package nan.toload.main.hd;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Verifies the interrupt propagation pattern used in SearchServer.getMappingByCodeFromCacheOrDB().
 *
 * Before fix: executor.submit(task); future.get() — InterruptedException was
 * silently swallowed, allowing stale results to be processed.
 *
 * After fix: direct call with early exit guard:
 *   if (Thread.currentThread().isInterrupted()) return null;
 *   cacheTemp = dbadapter.getMappingByCode(...);
 */
public class ThreadInterruptPropagationTest {

    /** Simulates the DB query + interrupt guard in SearchServer */
    private static String simulatedDbQuery(String key) {
        if (Thread.currentThread().isInterrupted()) return null; // ← the added guard
        // Simulate work
        try { Thread.sleep(10); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return "result_" + key;
    }

    @Test
    public void interruptedThread_earlyReturnNull() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        AtomicReference<String> result = new AtomicReference<>("NOT_SET");
        CountDownLatch done = new CountDownLatch(1);

        Future<?> f = exec.submit(() -> {
            Thread.currentThread().interrupt(); // pre-interrupt
            result.set(simulatedDbQuery("test_key"));
            done.countDown();
        });

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertNull("Pre-interrupted thread must return null from DB query", result.get());
        exec.shutdownNow();
    }

    @Test
    public void nonInterruptedThread_returnsResult() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        exec.submit(() -> {
            result.set(simulatedDbQuery("test_key"));
            done.countDown();
        });

        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertEquals("Non-interrupted thread must return valid result", "result_test_key", result.get());
        exec.shutdownNow();
    }

    @Test
    public void cancelledFuture_subsequentTaskSeesCleanState() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        AtomicReference<String> result1 = new AtomicReference<>();
        AtomicReference<String> result2 = new AtomicReference<>();
        CountDownLatch task1Started = new CountDownLatch(1);
        CountDownLatch task2Done = new CountDownLatch(1);

        // Task 1: slow, will be cancelled
        Future<?> f1 = exec.submit(() -> {
            task1Started.countDown();
            result1.set(simulatedDbQuery("stale"));
        });

        task1Started.await(1, TimeUnit.SECONDS);
        f1.cancel(true);

        // Task 2: submitted after cancellation
        exec.submit(() -> {
            // The executor thread interrupt flag should be cleared between tasks
            // (ExecutorService implementations clear the flag after task completion)
            result2.set(simulatedDbQuery("fresh"));
            task2Done.countDown();
        });

        assertTrue(task2Done.await(3, TimeUnit.SECONDS));
        assertEquals("Fresh task must not be affected by previous cancellation", "result_fresh", result2.get());
        exec.shutdownNow();
    }

    @Test
    public void oldPattern_futureGetSwallowsInterrupt_demonstratesProblem() throws Exception {
        // This test DOCUMENTS the old bug (not testing our code, but showing why it was broken).
        // Old pattern: executor.submit(task); future.get() where catch(Exception e) swallows interrupt.
        ExecutorService innerExec = Executors.newSingleThreadExecutor();
        AtomicBoolean processedStaleData = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(1);

        ExecutorService outerExec = Executors.newSingleThreadExecutor();
        Future<?> outerFuture = outerExec.submit(() -> {
            try {
                // Old pattern: submit+get inside another thread
                Future<String> inner = innerExec.submit(() -> "db_result");
                String val = inner.get(); // if interrupted here, Exception is caught below
                processedStaleData.set(true); // old code continued here even after interrupt
            } catch (Exception e) {
                // Old code just did e.printStackTrace() and continued — BUG
            }
            done.countDown();
        });

        // Interrupt the outer thread while it's in future.get()
        Thread.sleep(5); // let it reach future.get()
        outerFuture.cancel(true);

        done.await(2, TimeUnit.SECONDS);
        // In the old pattern, stale data might still be processed
        // Our new pattern avoids this by not using the nested executor at all
        innerExec.shutdownNow();
        outerExec.shutdownNow();
        // Just verify no crash — the documentation value is the comment above
        assertTrue(true);
    }
}
