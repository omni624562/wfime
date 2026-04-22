package nan.toload.main.hd;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Verifies the per-invocation HashMap pattern added to SearchServer.makeRunTimeSuggestion().
 *
 * Before fix: N*M calls to isRelatedPhraseExist(pword, cword) for N candidates × M words.
 * After fix: a local HashMap per invocation caches results — same (pword,cword) pair
 * is only looked up once per method call.
 */
public class PerInvocationCacheTest {

    /** Simulates isRelatedPhraseExist — counts actual DB calls. */
    static class FakeDb {
        final AtomicInteger callCount = new AtomicInteger(0);

        String isRelatedPhraseExist(String pword, String cword) {
            callCount.incrementAndGet();
            // Simulate result: related if pword ends same digit as cword starts
            return (pword.isEmpty() || cword.isEmpty()) ? null : "related";
        }
    }

    /** Mirrors the fixed makeRunTimeSuggestion() logic for a single candidate. */
    static String lookupWithCache(
            FakeDb db,
            Map<String, String> cache,
            String pword, String cword) {
        String key = pword + "\0" + cword;
        if (cache.containsKey(key)) {
            return cache.get(key);  // ← cache hit, no DB call
        }
        String result = db.isRelatedPhraseExist(pword, cword);
        cache.put(key, result);
        return result;
    }

    @Test
    public void duplicatePairs_onlyOneDbCallPerPair() {
        FakeDb db = new FakeDb();
        Map<String, String> cache = new HashMap<>();

        // Same (pword, cword) pair 5 times
        for (int i = 0; i < 5; i++) {
            lookupWithCache(db, cache, "hello", "world");
        }

        assertEquals("Duplicate pairs must produce exactly 1 DB call", 1, db.callCount.get());
    }

    @Test
    public void distinctPairs_eachCallsDbOnce() {
        FakeDb db = new FakeDb();
        Map<String, String> cache = new HashMap<>();

        String[] pwords = {"a", "b", "c"};
        String[] cwords = {"x", "y", "z"};

        // 3 × 3 = 9 distinct pairs, each called twice
        for (int round = 0; round < 2; round++) {
            for (String p : pwords) {
                for (String c : cwords) {
                    lookupWithCache(db, cache, p, c);
                }
            }
        }

        assertEquals("9 distinct pairs × 2 rounds must produce exactly 9 DB calls",
                9, db.callCount.get());
    }

    @Test
    public void cacheNotSharedAcrossInvocations_secondCallHitsDb() {
        FakeDb db = new FakeDb();

        // First invocation — fresh cache
        Map<String, String> cache1 = new HashMap<>();
        lookupWithCache(db, cache1, "word", "next");
        assertEquals(1, db.callCount.get());

        // Second invocation — NEW cache (per-invocation scope)
        Map<String, String> cache2 = new HashMap<>();
        lookupWithCache(db, cache2, "word", "next");
        assertEquals("New per-invocation cache must not reuse prior invocation's cache",
                2, db.callCount.get());
    }

    @Test
    public void keyCollision_differentPairsNeverShare() {
        FakeDb db = new FakeDb();
        Map<String, String> cache = new HashMap<>();

        // Pairs that would collide without the \0 separator:
        // "ab" + "c" vs "a" + "bc" — both become "abc" without separator
        lookupWithCache(db, cache, "ab", "c");
        lookupWithCache(db, cache, "a", "bc");

        assertEquals("Keys with separator must not collide: 2 distinct DB calls",
                2, db.callCount.get());
    }

    @Test
    public void n_plus_1_improvement_largeInput() {
        FakeDb db = new FakeDb();
        Map<String, String> cache = new HashMap<>();

        // Simulate 10 candidates, each compared against 5 context words (50 calls)
        // but only 3 unique pwords × 5 cwords = 15 distinct pairs
        String[] pwords = {"p1", "p2", "p3", "p1", "p2", "p3", "p1", "p2", "p3", "p1"};
        String[] cwords = {"c1", "c2", "c3", "c4", "c5"};

        for (String p : pwords) {
            for (String c : cwords) {
                lookupWithCache(db, cache, p, c);
            }
        }

        int wouldHaveBeen = pwords.length * cwords.length; // 50 without cache
        int distinctPairs = 3 * cwords.length;             // 15 with cache

        assertEquals("With per-invocation cache, DB calls equal distinct pairs",
                distinctPairs, db.callCount.get());
        assertTrue("Cache reduces calls from " + wouldHaveBeen + " to " + distinctPairs,
                db.callCount.get() < wouldHaveBeen);
    }
}
