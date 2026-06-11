package net.toload.main.hd.global;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SmartSelectionManager {
    private static final String TAG = "SmartSelectionManager";
    private static final String FILE_NAME = "dayi_smart_sel.json";
    // Cap total code-word pairs so memory, file size and load time stay bounded
    // under long-term use; oldest-used entries are evicted first.
    private static final int MAX_TOTAL_ENTRIES = 4096;
    private static final long SAVE_INTERVAL_MS = 60000;
    private static SmartSelectionManager instance;

    private final Context context;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // Memory Cache: Code -> Word -> CandidateStats
    private final Map<String, Map<String, CandidateStats>> statsMap = new HashMap<>();
    private int totalEntries = 0;

    private boolean isDirty = false;
    private long lastSaveTime = 0;
    private boolean saveScheduled = false;

    public static class CandidateStats {
        public int count;
        public long last;
        public final Map<String, Integer> prev = new HashMap<>(); // prevChar -> count

        public CandidateStats(int count, long last) {
            this.count = count;
            this.last = last;
        }
    }

    private SmartSelectionManager(Context context) {
        this.context = context.getApplicationContext();
        loadData();
    }

    public static synchronized SmartSelectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SmartSelectionManager(context);
        }
        return instance;
    }

    /**
     * Flush pending data only if the singleton was ever created; avoids
     * loading the data file just to write nothing on service teardown.
     */
    public static synchronized void flushIfLoaded() {
        if (instance != null) {
            instance.flush();
        }
    }

    public synchronized void recordSelection(String code, String word, String prevChar) {
        if (code == null || word == null) return;
        code = code.trim().toLowerCase();
        if (code.isEmpty() || word.isEmpty()) return;

        Map<String, CandidateStats> wordStats = statsMap.get(code);
        if (wordStats == null) {
            wordStats = new HashMap<>();
            statsMap.put(code, wordStats);
        }

        CandidateStats stats = wordStats.get(word);
        if (stats == null) {
            stats = new CandidateStats(0, 0);
            wordStats.put(word, stats);
            totalEntries++;
            if (totalEntries > MAX_TOTAL_ENTRIES) {
                evictOldestEntry();
            }
        }

        stats.count++;
        stats.last = System.currentTimeMillis();

        if (prevChar != null && !prevChar.isEmpty()) {
            int prevCount = stats.prev.containsKey(prevChar) ? stats.prev.get(prevChar) : 0;
            stats.prev.put(prevChar, prevCount + 1);

            // Cap prev context at 32 entries
            if (stats.prev.size() > 32) {
                String oldestOrLeastKey = null;
                int minVal = Integer.MAX_VALUE;
                for (Map.Entry<String, Integer> entry : stats.prev.entrySet()) {
                    if (entry.getValue() < minVal) {
                        minVal = entry.getValue();
                        oldestOrLeastKey = entry.getKey();
                    }
                }
                if (oldestOrLeastKey != null) {
                    stats.prev.remove(oldestOrLeastKey);
                }
            }
        }

        isDirty = true;
        scheduleSave();
    }

    // Caller must hold the monitor (called from recordSelection only).
    private void evictOldestEntry() {
        String oldestCode = null;
        String oldestWord = null;
        long oldestLast = Long.MAX_VALUE;
        for (Map.Entry<String, Map<String, CandidateStats>> codeEntry : statsMap.entrySet()) {
            for (Map.Entry<String, CandidateStats> wordEntry : codeEntry.getValue().entrySet()) {
                if (wordEntry.getValue().last < oldestLast) {
                    oldestLast = wordEntry.getValue().last;
                    oldestCode = codeEntry.getKey();
                    oldestWord = wordEntry.getKey();
                }
            }
        }
        if (oldestCode != null) {
            Map<String, CandidateStats> words = statsMap.get(oldestCode);
            words.remove(oldestWord);
            if (words.isEmpty()) {
                statsMap.remove(oldestCode);
            }
            totalEntries--;
        }
    }

    // Caller must hold the monitor. Drops the oldest-used entries in one
    // O(n log n) pass instead of evicting one by one.
    private void trimToCap() {
        java.util.List<long[]> ages = new java.util.ArrayList<>(totalEntries); // [last, index]
        java.util.List<String[]> keys = new java.util.ArrayList<>(totalEntries); // [code, word]
        for (Map.Entry<String, Map<String, CandidateStats>> codeEntry : statsMap.entrySet()) {
            for (Map.Entry<String, CandidateStats> wordEntry : codeEntry.getValue().entrySet()) {
                ages.add(new long[] { wordEntry.getValue().last, keys.size() });
                keys.add(new String[] { codeEntry.getKey(), wordEntry.getKey() });
            }
        }
        ages.sort(java.util.Comparator.comparingLong(a -> a[0]));
        int toRemove = totalEntries - MAX_TOTAL_ENTRIES;
        for (int i = 0; i < toRemove; i++) {
            String[] key = keys.get((int) ages.get(i)[1]);
            Map<String, CandidateStats> words = statsMap.get(key[0]);
            if (words != null && words.remove(key[1]) != null) {
                totalEntries--;
                if (words.isEmpty()) {
                    statsMap.remove(key[0]);
                }
            }
        }
    }

    public synchronized CandidateStats getStats(String code, String word) {
        if (code == null || word == null) return null;
        code = code.trim().toLowerCase();
        Map<String, CandidateStats> wordStats = statsMap.get(code);
        if (wordStats == null) return null;
        return wordStats.get(word);
    }

    /**
     * Compute the ranking score for one candidate under the manager's lock,
     * so callers never touch mutable CandidateStats internals unsynchronized.
     */
    public synchronized double getScore(String code, String word, String prevChar,
            boolean recentEnabled, boolean contextEnabled, long now) {
        if (code == null || word == null) return 0.0;
        Map<String, CandidateStats> wordStats = statsMap.get(code.trim().toLowerCase());
        if (wordStats == null) return 0.0;
        CandidateStats stats = wordStats.get(word);
        if (stats == null) return 0.0;

        double score = stats.count;

        if (contextEnabled && prevChar != null && !prevChar.isEmpty()) {
            Integer prevCount = stats.prev.get(prevChar);
            if (prevCount != null) {
                score += prevCount * 2.0;
            }
        }

        if (recentEnabled) {
            double ageDays = (double) (now - stats.last) / (1000.0 * 60.0 * 60.0 * 24.0);
            if (ageDays < 0) ageDays = 0;
            score += 3.0 / (1.0 + ageDays / 7.0);
        }

        return score;
    }

    public synchronized void clearData() {
        statsMap.clear();
        totalEntries = 0;
        isDirty = false;
        saveScheduled = false;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                File file = new File(context.getFilesDir(), FILE_NAME);
                if (file.exists()) {
                    file.delete();
                }
            }
        });
    }

    private synchronized void scheduleSave() {
        if (saveScheduled) return;
        saveScheduled = true;

        long delay = SAVE_INTERVAL_MS - (System.currentTimeMillis() - lastSaveTime);
        if (delay < 0) delay = 0;
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                flushInternal();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public void flush() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                flushInternal();
            }
        });
    }

    private void flushInternal() {
        // Snapshot under the lock, then serialize and write outside it so
        // candidate ranking is never blocked by file I/O.
        Map<String, Map<String, CandidateStats>> snapshot;
        synchronized (this) {
            saveScheduled = false;
            if (!isDirty) return;
            isDirty = false;

            snapshot = new HashMap<>();
            for (Map.Entry<String, Map<String, CandidateStats>> entry : statsMap.entrySet()) {
                Map<String, CandidateStats> words = new HashMap<>();
                for (Map.Entry<String, CandidateStats> wordEntry : entry.getValue().entrySet()) {
                    CandidateStats src = wordEntry.getValue();
                    CandidateStats copy = new CandidateStats(src.count, src.last);
                    copy.prev.putAll(src.prev);
                    words.put(wordEntry.getKey(), copy);
                }
                snapshot.put(entry.getKey(), words);
            }
        }

        JSONObject root = new JSONObject();
        try {
            for (Map.Entry<String, Map<String, CandidateStats>> entry : snapshot.entrySet()) {
                String code = entry.getKey();
                JSONObject wordsObj = new JSONObject();
                for (Map.Entry<String, CandidateStats> wordEntry : entry.getValue().entrySet()) {
                    String word = wordEntry.getKey();
                    CandidateStats stats = wordEntry.getValue();

                    JSONObject statsObj = new JSONObject();
                    statsObj.put("count", stats.count);
                    statsObj.put("last", stats.last);

                    JSONObject prevObj = new JSONObject();
                    for (Map.Entry<String, Integer> prevEntry : stats.prev.entrySet()) {
                        prevObj.put(prevEntry.getKey(), prevEntry.getValue());
                    }
                    statsObj.put("prev", prevObj);

                    wordsObj.put(word, statsObj);
                }
                root.put(code, wordsObj);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error building JSON to save", e);
            return;
        }

        File file = new File(context.getFilesDir(), FILE_NAME);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
            synchronized (this) {
                lastSaveTime = System.currentTimeMillis();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing smart selection data", e);
            synchronized (this) {
                isDirty = true; // retry on next save cycle
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private synchronized void loadData() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            int offset = 0;
            while (offset < bytes.length) {
                int read = fis.read(bytes, offset, bytes.length - offset);
                if (read < 0) break;
                offset += read;
            }
            String jsonStr = new String(bytes, 0, offset, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonStr);

            Iterator<String> codes = root.keys();
            while (codes.hasNext()) {
                String code = codes.next();
                JSONObject wordsObj = root.optJSONObject(code);
                if (wordsObj == null) continue;

                Map<String, CandidateStats> wordStats = new HashMap<>();
                Iterator<String> words = wordsObj.keys();
                while (words.hasNext()) {
                    String word = words.next();
                    JSONObject statsObj = wordsObj.optJSONObject(word);
                    if (statsObj == null) continue;

                    int count = statsObj.optInt("count", 0);
                    long last = statsObj.optLong("last", 0);
                    CandidateStats stats = new CandidateStats(count, last);

                    JSONObject prevObj = statsObj.optJSONObject("prev");
                    if (prevObj != null) {
                        Iterator<String> prevs = prevObj.keys();
                        while (prevs.hasNext()) {
                            String prevChar = prevs.next();
                            int prevCount = prevObj.optInt(prevChar, 0);
                            stats.prev.put(prevChar, prevCount);
                        }
                    }
                    wordStats.put(word, stats);
                    totalEntries++;
                }
                statsMap.put(code, wordStats);
            }
            // Trim down to the cap in case an older oversized file is loaded.
            if (totalEntries > MAX_TOTAL_ENTRIES) {
                trimToCap();
                isDirty = true;
                scheduleSave();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading smart selection data. Starting fresh.", e);
            // Clean up potentially corrupt file
            file.delete();
            statsMap.clear();
            totalEntries = 0;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }
}
