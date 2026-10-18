package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import kotlinx.coroutines.ExecutorsKt;

/**
 * 查詢執行器:新查詢取消舊查詢、只套用最新結果、套用在「主執行緒」、背景工作依序執行、
 * 被取消的工作收到中斷、可以等最新工作做完。
 */
public class LatestQueryRunnerTest {

    private ExecutorService work;
    private ExecutorService main;
    private LatestQueryRunner runner;
    private final AtomicReference<Thread> mainThread = new AtomicReference<>();

    @Before
    public void setup() throws Exception {
        work = Executors.newSingleThreadExecutor();
        main = Executors.newSingleThreadExecutor();
        main.submit(() -> mainThread.set(Thread.currentThread())).get();
        runner = new LatestQueryRunner(ExecutorsKt.from(work), ExecutorsKt.from(main));
    }

    @After
    public void tearDown() {
        runner.shutdown();
        work.shutdownNow();
        main.shutdownNow();
    }

    // 等主執行緒把已排隊的工作做完
    private void drainMain() throws Exception {
        for (int i = 0; i < 3; i++) main.submit(() -> { }).get();
    }

    private void onMain(Runnable r) throws Exception {
        main.submit(r).get();
    }

    @Test
    public void appliesResultOnMainThread() throws Exception {
        AtomicReference<Thread> appliedOn = new AtomicReference<>();
        AtomicReference<String> result = new AtomicReference<>();
        onMain(() -> runner.submit(() -> "A", r -> {
            result.set(r);
            appliedOn.set(Thread.currentThread());
        }));
        assertTrue(runner.awaitWork(5000));
        drainMain();
        assertEquals("A", result.get());
        assertEquals(mainThread.get(), appliedOn.get());
    }

    @Test
    public void newerQueryReplacesOlderEvenIfOlderFinishesLater() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        List<String> applied = new CopyOnWriteArrayList<>();
        AtomicBoolean olderFinished = new AtomicBoolean();
        onMain(() -> runner.submit(() -> {
            started.countDown();
            try {
                release.await();       // 舊查詢卡住(例如慢的資料庫查詢)
            } catch (InterruptedException e) {
                // 被新查詢取消:照樣算完,結果也不該被套用
            }
            olderFinished.set(true);
            return "old";
        }, applied::add));
        // 等舊查詢真的在背景執行緒開始才送新查詢；否則電腦忙時舊查詢還沒開始就被取消、根本不會跑
        assertTrue(started.await(5, TimeUnit.SECONDS));
        onMain(() -> runner.submit(() -> "new", applied::add));
        release.countDown();
        assertTrue(runner.awaitWork(5000));
        drainMain();
        assertTrue(olderFinished.get());
        assertEquals(Collections.singletonList("new"), applied);
    }

    @Test
    public void cancelledWorkIsInterrupted() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        onMain(() -> runner.submit(() -> {
            started.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
            return "old";
        }, r -> { }));
        assertTrue(started.await(5, TimeUnit.SECONDS));
        onMain(() -> runner.submit(() -> "new", r -> { }));
        assertTrue(runner.awaitWork(5000));
        assertTrue(interrupted.get());
    }

    @Test
    public void workRunsOneAtATimeInOrder() throws Exception {
        List<Integer> order = new CopyOnWriteArrayList<>();
        AtomicBoolean overlap = new AtomicBoolean();
        AtomicBoolean busy = new AtomicBoolean();
        for (int i = 0; i < 5; i++) {
            final int n = i;
            onMain(() -> runner.submit(() -> {
                if (!busy.compareAndSet(false, true)) overlap.set(true);
                order.add(n);
                busy.set(false);
                return n;
            }, r -> { }));
        }
        assertTrue(runner.awaitWork(5000));
        work.submit(() -> { }).get();
        assertFalse(overlap.get());
        // 被取代的查詢可能在開始前就被取消;有跑的照提交順序
        for (int i = 1; i < order.size(); i++) assertTrue(order.get(i - 1) < order.get(i));
        assertEquals(Integer.valueOf(4), order.get(order.size() - 1));
    }

    @Test
    public void cancelDropsResultAfterWorkFinished() throws Exception {
        CountDownLatch mainBlocked = new CountDownLatch(1);
        CountDownLatch unblockMain = new CountDownLatch(1);
        List<String> applied = new CopyOnWriteArrayList<>();
        // 背景工作等主執行緒忙碌後才做完,結果只能排隊等套用;這時取消
        onMain(() -> runner.submit(() -> {
            mainBlocked.await();
            return "A";
        }, applied::add));
        main.submit(() -> {
            mainBlocked.countDown();
            try {
                unblockMain.await();
            } catch (InterruptedException ignored) {
            }
        });
        assertTrue(mainBlocked.await(5, TimeUnit.SECONDS));
        assertTrue(runner.awaitWork(5000));
        runner.cancel();
        unblockMain.countDown();
        drainMain();
        assertTrue(applied.isEmpty());
    }

    @Test
    public void awaitWorkTimesOutWhileWorkRuns() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        onMain(() -> runner.submit(() -> {
            release.await();
            return "A";
        }, r -> { }));
        assertFalse(runner.awaitWork(50));
        release.countDown();
        assertTrue(runner.awaitWork(5000));
        assertTrue(new LatestQueryRunner(ExecutorsKt.from(work), ExecutorsKt.from(main)).awaitWork(10));
    }

    @Test
    public void queryCancelledBeforeStartDoesNotBlockAwait() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        onMain(() -> runner.submit(() -> {
            // 前一個查詢佔住背景執行緒(例如不理會中斷的資料庫查詢)
            while (true) {
                try {
                    release.await();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
            return "A";
        }, r -> { }));
        List<String> applied = new CopyOnWriteArrayList<>();
        onMain(() -> runner.submit(() -> "B", applied::add));
        onMain(runner::cancel);                    // B 還在排隊就被取消
        assertTrue(runner.awaitWork(5000));
        release.countDown();
        drainMain();
        assertTrue(applied.isEmpty());
    }

    @Test
    public void workExceptionDoesNotBreakLaterQueries() throws Exception {
        List<String> applied = new CopyOnWriteArrayList<>();
        onMain(() -> runner.submit(() -> {
            throw new IllegalStateException("db error");
        }, r -> applied.add("bad")));
        assertTrue(runner.awaitWork(5000));
        drainMain();
        onMain(() -> runner.submit(() -> "ok", applied::add));
        assertTrue(runner.awaitWork(5000));
        drainMain();
        assertEquals(Collections.singletonList("ok"), applied);
    }
}
