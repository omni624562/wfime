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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmartSelectionManager {
    private static final String TAG = "SmartSelectionManager";
    private static final String FILE_NAME = "dayi_smart_sel.json";
    private static SmartSelectionManager instance;

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // Memory Cache: Code -> Word -> CandidateStats
    private final Map<String, Map<String, CandidateStats>> statsMap = new HashMap<>();
    
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

    public synchronized CandidateStats getStats(String code, String word) {
        if (code == null || word == null) return null;
        code = code.trim().toLowerCase();
        Map<String, CandidateStats> wordStats = statsMap.get(code);
        if (wordStats == null) return null;
        return wordStats.get(word);
    }

    public synchronized void clearData() {
        statsMap.clear();
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

        long now = System.currentTimeMillis();
        long elapsedSinceLastSave = now - lastSaveTime;

        if (elapsedSinceLastSave >= 60000) {
            // Write immediately in background
            saveScheduled = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    flushInternal();
                }
            });
        } else {
            // Schedule a write for the future
            saveScheduled = true;
            final long delay = 60000 - elapsedSinceLastSave;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            flushInternal();
                        }
                    });
                }
            }).start();
        }
    }

    public void flush() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                flushInternal();
            }
        });
    }

    private synchronized void flushInternal() {
        if (!isDirty) {
            saveScheduled = false;
            return;
        }

        JSONObject root = new JSONObject();
        try {
            for (Map.Entry<String, Map<String, CandidateStats>> entry : statsMap.entrySet()) {
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
            saveScheduled = false;
            return;
        }

        File file = new File(context.getFilesDir(), FILE_NAME);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(root.toString().getBytes(StandardCharsets.UTF_8));
            isDirty = false;
            lastSaveTime = System.currentTimeMillis();
        } catch (IOException e) {
            Log.e(TAG, "Error writing smart selection data", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            saveScheduled = false;
        }
    }

    private synchronized void loadData() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            String jsonStr = new String(bytes, StandardCharsets.UTF_8);
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
                }
                statsMap.put(code, wordStats);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading smart selection data. Starting fresh.", e);
            // Clean up potentially corrupt file
            file.delete();
            statsMap.clear();
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
