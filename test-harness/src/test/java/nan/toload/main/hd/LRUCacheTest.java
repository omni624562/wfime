package nan.toload.main.hd;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

/**
 * Verifies the LRU cache eviction pattern used in SearchServer.
 *
 * SearchServer uses:
 *   Collections.synchronizedMap(new LinkedHashMap<K,V>(maxSize, 0.75f, true) {
 *       protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
 *           return size() > maxSize;
 *       }
 *   })
 *
 * Where maxSize = 512 (MAX_CACHE_ENTRIES).
 */
public class LRUCacheTest {

    private static final int MAX = 8; // small value for fast tests

    private static <K, V> Map<K, V> makeLRU(int maxSize) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        });
    }

    @Test
    public void cache_evictsEldestWhenFull() {
        Map<String, String> cache = makeLRU(MAX);
        for (int i = 0; i < MAX + 1; i++) {
            cache.put("k" + i, "v" + i);
        }
        // First entry must be evicted
        assertFalse("Eldest entry must be evicted when cache exceeds MAX", cache.containsKey("k0"));
        assertEquals("Cache must not exceed MAX entries", MAX, cache.size());
    }

    @Test
    public void cache_accessPromotesEntry() {
        Map<String, String> cache = makeLRU(MAX);
        for (int i = 0; i < MAX; i++) {
            cache.put("k" + i, "v" + i);
        }
        // Access "k0" to promote it to MRU position
        cache.get("k0");

        // Fill one more — evicts LRU (now "k1", since "k0" was accessed)
        cache.put("k_new", "v_new");

        assertTrue("Accessed entry must survive eviction", cache.containsKey("k0"));
        assertFalse("LRU entry after access promotion must be evicted", cache.containsKey("k1"));
    }

    @Test
    public void cache_belowCapacity_noEviction() {
        Map<String, String> cache = makeLRU(MAX);
        for (int i = 0; i < MAX; i++) {
            cache.put("k" + i, "v" + i);
        }
        assertEquals("No eviction until capacity exceeded", MAX, cache.size());
        for (int i = 0; i < MAX; i++) {
            assertTrue(cache.containsKey("k" + i));
        }
    }

    @Test
    public void cache_put_overwrite_doesNotGrow() {
        Map<String, String> cache = makeLRU(MAX);
        for (int i = 0; i < MAX; i++) {
            cache.put("k0", "v" + i); // same key
        }
        assertEquals("Overwriting same key must not increase size", 1, cache.size());
    }

    @Test
    public void cache_clear_resetsToEmpty() {
        Map<String, String> cache = makeLRU(MAX);
        for (int i = 0; i < MAX; i++) {
            cache.put("k" + i, "v" + i);
        }
        cache.clear();
        assertEquals(0, cache.size());
        cache.put("after_clear", "val");
        assertEquals(1, cache.size());
    }

    @Test
    public void cache_threadSafe_concurrentPuts() throws Exception {
        Map<String, String> cache = makeLRU(512); // matches MAX_CACHE_ENTRIES
        int threads = 8;
        int perThread = 100;

        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            workers.add(new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    cache.put("t" + tid + "_k" + i, "v");
                    cache.get("t" + tid + "_k" + i);
                }
            }));
        }
        workers.forEach(Thread::start);
        for (Thread w : workers) w.join(5000);

        assertTrue("Cache must not exceed MAX_CACHE_ENTRIES under concurrent access",
                cache.size() <= 512);
    }
}
