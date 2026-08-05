/*
 *
 *  *
 *  **    Copyright 2015, The LimeIME Open Source Project
 *  **
 *  **    Project Url: http://github.com/lime-ime/limeime/
 *  **                 http://android.toload.net/
 *  **
 *  **    This program is free software: you can redistribute it and/or modify
 *  **    it under the terms of the GNU General Public License as published by
 *  **    the Free Software Foundation, either version 3 of the License, or
 *  **    (at your option) any later version.
 *  *
 *  **    This program is distributed in the hope that it will be useful,
 *  **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  **    GNU General Public License for more details.
 *  *
 *  **    You should have received a copy of the GNU General Public License
 *  **    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *
 */

package net.toload.main.hd;

import android.content.Context;
import android.content.Intent;

import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import net.toload.main.hd.R;
import net.toload.main.hd.data.ImObj;
import net.toload.main.hd.data.KeyboardObj;
import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.global.LIME;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.LIMEUtilities;
import net.toload.main.hd.global.SmartSelectionManager;
import net.toload.main.hd.limedb.LimeDB;

public class SearchServer {

    private static final boolean DEBUG = false;
    private static final String TAG = "LIME.SearchServer";
    private static final int MAX_CACHE_ENTRIES = 512;
    // Jeremy '12,6,9 make run-time suggestion phrase
    private static final boolean doRunTimeSuggestion = true;

    // Jeremy '12,5,1 shared single LIMEDB object
    // Jeremy '12,4,6 Combine updatedb and quierydb into db,
    // Jeremy '12,4,7 move db open/close back to LimeDB
    private static final boolean dumpRunTimeSuggestion = false;
    private static LimeDB dbadapter = null;
    private static List<Mapping> scorelist = null;
    // Jeremy '15,6,2 preserve the exact match mapping with the code user typed.
    private static List<List<Pair<Mapping, String>>> suggestionLoL;
    private static Stack<Pair<Mapping, String>> bestSuggestionStack;
    // Jeremy '15,6,21
    private static int maxCodeLength = 4;
    private static volatile boolean mResetCache;
    private static List<List<Mapping>> LDPhraseListArray = null;
    private static List<Mapping> LDPhraseList = null;
    private static String tablename = "";
    private static boolean isPhysicalKeyboardPressed; // Sync to LIMEService and LIMEDB
    // Jeremy '11,6,10
    private static boolean hasNumberMapping;
    private static boolean hasSymbolMapping;
    private static Map<String, List<Mapping>> cache = null;
    private static Map<String, List<Mapping>> engcache = null;
    private static Map<String, List<Mapping>> emojicache = null;
    private static Map<String, String> keynamecache = null;
    /**
     * Store the mapping of typing code and mapped code from getMappingByCode on db
     * Jeremy '12,6,5
     */
    private static Map<String, List<String>> coderemapcache = null;
    // Jeremy '15,6,8 TODO resolved: related phrases are now cached across keystrokes
    private static Map<String, List<Mapping>> relatedcache = null; // word -> related phrase list
    private static Map<String, Mapping> relatedPhraseCache = null; // pword\0cword -> Mapping (null = no record)
    // 大易連打的查詢/結果快取(key 皆含 tablename)
    private static Map<String, List<Mapping>> topWordsCache = null;   // code -> exact 前幾名字(空清單=查過無此碼)
    private static Map<String, Boolean> prefixCache = null;           // code -> hasCodeOrPrefix
    private static Map<String, List<String>> phraseWordsCache = null; // 縮碼_詞長 -> 詞
    private static Map<String, Boolean> pairCache = null;             // code\0word -> 是否對映
    private static Map<String, List<Mapping>> segmentCache = null;    // 碼串 -> 切分候選結果
    private static final java.util.concurrent.atomic.AtomicBoolean prefetchRunning =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    // deprecated and using exact match stack to get real code length now. Jerey
    // '15,6,2
    // private static List<Pair<Integer, Integer>> codeLengthMap = new
    // LinkedList<>();
    private static String lastEnglishWord = null;
    private static boolean noSuggestionsForLastEnglishWord = false;
    private static String lastCommittedChar = null;
    private final LIMEPreferenceManager mLIMEPref;
    // Jeremy '11,6,6
    private final HashMap<String, String> selKeyMap = new HashMap<>();
    // '11,8,1 renamed from updateuserdict()
    List<Mapping> scorelistSnapshot = null;
    private Context mContext = null;

    public SearchServer(Context context) {
        this.mContext = context;

        mLIMEPref = new LIMEPreferenceManager(mContext.getApplicationContext());
        if (dbadapter == null)
            dbadapter = new LimeDB(mContext);
        initialCache();
    }

    public static void resetCache(boolean resetCache) {
        mResetCache = resetCache;
    }

    /**
     * 自建關聯字管理變更資料後呼叫:清除聯想詞相關快取,
     * 讓同 process 的輸入法服務立即反映刪除/清空結果
     */
    public static void clearRelatedCaches() {
        if (relatedcache != null)
            relatedcache.clear();
        if (relatedPhraseCache != null)
            relatedPhraseCache.clear();
        if (dbadapter != null)
            dbadapter.clearRelatedScoreCache();
    }

    public String hanConvert(String input) {
        return dbadapter.hanConvert(input, mLIMEPref.getHanCovertOption());
    }

    public String getTablename() {
        return tablename;
    }

    // The char committed before lastCommittedChar — used to de-pollute
    // context learning when the commit path updates lastCommittedChar to the
    // just-picked word before recordSelection() runs.
    private static String prevBeforeLastCommittedChar = null;

    public static void setLastCommittedChar(String c) {
        if (c != null && !c.equals(lastCommittedChar)) {
            prevBeforeLastCommittedChar = lastCommittedChar;
        }
        lastCommittedChar = c;
    }

    public static String getLastCommittedChar() {
        return lastCommittedChar;
    }

    public void setTablename(String table, boolean numberMapping, boolean symbolMapping) {
        if (DEBUG)
            Log.i(TAG, "SearchService.setTablename()");

        dbadapter.setTablename(table);
        tablename = table;
        hasNumberMapping = numberMapping;
        hasSymbolMapping = symbolMapping;

        // run prefetch on first keys thread to feed the data into cache first for
        // better response on large table. Jeremy '15, 6,7
        if (cache.get(cacheKey("a")) == null) { // no cache records present. do prefetch now. '15,6,7
            prefetchCache(numberMapping, symbolMapping);
        }

        // Jeremy '15,6,21 set max code length
        if (tablename.startsWith("dayi")) {
            maxCodeLength = 4;
        } else {
            maxCodeLength = 4;
        }
    }

    private void prefetchCache(boolean numberMapping, boolean symbolMapping) {
        if (DEBUG)
            Log.i(TAG, "prefetchCache() on table :" + tablename);

        String keys = "abcdefghijklmnoprstuvwxyz";
        if (numberMapping)
            keys += "01234567890";
        if (symbolMapping)
            keys += ",./;";
        final String finalKeys = keys;

        // Run on the shared background executor instead of spawning raw
        // threads; the flag prevents overlapping prefetch runs.
        if (!prefetchRunning.compareAndSet(false, true))
            return;

        backgroundExecutor.execute(new Runnable() {
            public void run() {
                try {
                    long startime = System.currentTimeMillis();
                    for (int i = 0; i < finalKeys.length(); i++) {
                        String key = finalKeys.substring(i, i + 1);
                        try {
                            // bypass run-time suggestion for prefetch queries
                            getMappingByCode(key, true, false, true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error in prefetch: " + e.getMessage());
                        }
                    }
                    Log.i(TAG, "prefetchCache() on table :" + tablename + " finished.  Elapsed time = "
                            + (System.currentTimeMillis() - startime) + " ms.");
                } finally {
                    prefetchRunning.set(false);
                }
            }
        });
    }

    public List<Mapping> getRelatedPhrase(String word, boolean getAllRecords) {
        Map<String, List<Mapping>> localCache = relatedcache;
        if (localCache == null)
            return dbadapter.getRelatedPhrase(word, getAllRecords);

        // 開關狀態影響查詢結果,須納入快取 key 避免切換設定後吃到舊結果
        String key = word + "\0" + getAllRecords
                + "\0" + mLIMEPref.getSimiliarEnable() + mLIMEPref.getLearnRelatedWord();
        List<Mapping> cached = localCache.get(key);
        if (cached != null)
            return cached;

        List<Mapping> result = dbadapter.getRelatedPhrase(word, getAllRecords);
        if (result != null)
            localCache.put(key, result);
        return result;
    }

    /*
     * return longest common substring with recursive method.
     */

    // Add by jeremy '10, 4,1
    public void getCodeListStringFromWord(final String word) {

        String result = dbadapter.getCodeListStringByWord(word);
        if (result != null && !result.equals("")) {
            LIMEUtilities.showNotification(
                    mContext, true, mContext.getText(R.string.ime_setting), result,
                    new Intent(mContext, MainActivity.class));

            if (mLIMEPref.getReverseLookupNotify()) {
                Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
            }
        }

    }

    private String cacheKey(String code) {
        String key;

        // Jeremy '11,6,17 Seperate physical keyboard cache with keybaordtype
        if (isPhysicalKeyboardPressed) {
            if (tablename.equals("phonetic")) {
                key = mLIMEPref.getPhysicalKeyboardType() + dbadapter.getTablename()
                        + mLIMEPref.getPhoneticKeyboardType() + code;
            } else {
                key = mLIMEPref.getPhysicalKeyboardType() + dbadapter.getTablename() + code;
            }
        } else {
            if (tablename.equals("phonetic"))
                key = dbadapter.getTablename() + mLIMEPref.getPhoneticKeyboardType() + code;
            else
                key = dbadapter.getTablename() + code;
        }
        return key;
    }

    /*
     * Get mapping list from cache or from db if it's not in cache. Separated from
     * getMappingByCode() Jeremy '15,6,8
     */

    /*
     * Jeremy '15,7,12 synchronized the method called from LIMEService only
     */
    public void deleteRelatedPhrase(String pword, String cword) {
        dbadapter.deleteRelatedPhrase(pword, cword);
    }

    public List<Mapping> getMappingByCode(String code, boolean softkeyboard, boolean getAllRecords)
            {
        return getMappingByCode(code, softkeyboard, getAllRecords, false);
    }

        public List<Mapping> emojiConvert(String code, int type) {
        if (code != null) {
            if (emojicache == null) {
                // Late init must use the same bounded LRU as initialCache(),
                // not an unbounded map that bypasses eviction
                emojicache = newLruMap(MAX_CACHE_ENTRIES);
            }
            String cacheKey = code + "_" + type;
            List<Mapping> results = emojicache.get(cacheKey);
            if (results != null) {
                return results;
            } else {
                // Log.i("EMOJI :" , "Run search emoji ...");
                results = dbadapter.emojiConvert(code, type);
                if (results != null) {
                    emojicache.put(cacheKey, results);
                }
                return results;
            }
        }
        return null;
    }

    public List<Mapping> getMappingByCode(String code, boolean softkeyboard, boolean getAllRecords,
            boolean prefetchCache)
            {
        if (DEBUG || dumpRunTimeSuggestion)
            Log.i(TAG, "getMappingByCode(): code=" + code);
        // Check if system need to reset cache

        // check reset cache with local variable instead of reading from shared
        // preference for better performance
        if (mResetCache) {
            initialCache();
            mResetCache = false;
        }

        // codeLengthMap.clear();//Jeremy '12,6,2 reset the codeLengthMap

        List<Mapping> result = new LinkedList<>();
        if (code != null) {
            // clear mappingidx when user switching between softkeyboard and hard keyboard.
            // Jeremy '11,6,11
            if (isPhysicalKeyboardPressed == softkeyboard)
                isPhysicalKeyboardPressed = !softkeyboard;

            // Jeremy '11,9, 3 remove cached keyname when request full records
            if (getAllRecords && keynamecache.get(cacheKey(code)) != null)
                keynamecache.remove(cacheKey(code));

            int size = code.length();

            // boolean hasMore = false;

            // 12,6,4 Jeremy. Ascending a ab abc... looking up db if the cache is not exist
            // '15,6,4 Jeremy. Do exact search only in between search mode (1 time only).
            // Descending loop to collect suggestions for current code and its prefixes
            // For Dayi 3-code, we only collect matches for the full code to enable auto-commit
            String currentLoopCode = code;
            java.util.HashSet<String> seenWords = new java.util.HashSet<>();
            boolean isDayiOverLimit = tablename.startsWith("dayi") && code.length() > 4;
            while (!isDayiOverLimit && currentLoopCode != null && currentLoopCode.length() > 0) {
                String loopCacheKey = cacheKey(currentLoopCode);
                List<Mapping> loopCacheTemp = cache.get(loopCacheKey);

                if (loopCacheTemp == null) {
                    try {
                        loopCacheTemp = dbadapter.getMappingByCode(currentLoopCode, !isPhysicalKeyboardPressed, getAllRecords);
                        if (loopCacheTemp != null) {
                            cache.put(loopCacheKey, loopCacheTemp);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error refreshing cache in loop: " + e.getMessage());
                    }
                }

                if (loopCacheTemp != null && !loopCacheTemp.isEmpty()) {
                    // Only add if it's not already in the result list (avoid duplicates)
                    for (Mapping m : loopCacheTemp) {
                        if (seenWords.add(m.getWord())) {
                            result.add(m);
                        }
                    }
                    // For Dayi 3-code, we STOP after the first successful match level (exact match only)
                    if (tablename.startsWith("dayi")) {
                        break;
                    }
                }

                currentLoopCode = currentLoopCode.substring(0, currentLoopCode.length() - 1);
            }

            // Finally add the raw composing code at the beginning
            // Jeremy '24,1,7: Don't add raw code for Dayi to prevent things like "./" showing up
            // BUT allow it if it's alphanumeric or common Dayi radicals (like ",./") so users see a reaction
            if (!tablename.startsWith("dayi") || code.matches("[A-Za-z0-9,./]+")) {
                Mapping self = new Mapping();
                self.setWord(code);
                self.setCode(code);
                self.setComposingCodeRecord();
                result.add(0, self);
            }
        }
        if (DEBUG)
            Log.i(TAG, "getMappingByCode() result.size()=" + result.size());

        if (tablename.startsWith("dayi") && mLIMEPref.getDayiSmartSelectionEnabled() && result.size() > 1) {
            // 純上下文預測(肌肉記憶安全):只看「前一個字之後,這個碼選過
            // 哪個字」的記錄,把最有把握的一個候選提到第 1 位;其餘候選
            // 維持字庫原始順序,不做任何全域重排。
            try {
                final SmartSelectionManager manager = SmartSelectionManager.getInstance(mContext);
                final String prevChar = lastCommittedChar;
                if (prevChar != null && !prevChar.isEmpty()) {
                    int bestIndex = -1;
                    int bestCount = 0;
                    for (int i = 0; i < result.size(); i++) {
                        Mapping m = result.get(i);
                        int c = manager.getContextCount(m.getCode(), m.getWord(), prevChar);
                        if (c > bestCount) { // strictly greater: ties keep the earlier (DB-order) candidate
                            bestCount = c;
                            bestIndex = i;
                        }
                    }
                    if (bestIndex > 0) {
                        result.add(0, result.remove(bestIndex));
                        if (DEBUG)
                            Log.d(TAG, "getMappingByCode() context prediction promoted index "
                                    + bestIndex + " (count=" + bestCount + ")");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error applying Dayi context prediction", e);
            }
        } else if (!tablename.startsWith("dayi")
                && lastCommittedChar != null && lastCommittedChar.length() > 0 && result.size() > 1) {
            try {
                List<Mapping> related = getRelatedPhrase(lastCommittedChar, false);
                if (related != null && !related.isEmpty()) {
                    final java.util.HashSet<String> relatedChars = new java.util.HashSet<>();
                    for (Mapping m : related) {
                        if (m.getWord() != null && m.getWord().length() > 1) {
                            relatedChars.add(m.getWord().substring(1, 2));
                        }
                    }
                    if (!relatedChars.isEmpty()) {
                        java.util.Collections.sort(result, new java.util.Comparator<Mapping>() {
                            public int compare(Mapping m1, Mapping m2) {
                                boolean b1 = relatedChars.contains(m1.getWord());
                                boolean b2 = relatedChars.contains(m2.getWord());
                                if (b1 == b2) return 0;
                                return b1 ? -1 : 1;
                            }
                        });
                        if (DEBUG)
                            Log.i(TAG, "getMappingByCode() Context-aware sorting applied for char: " + lastCommittedChar);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private List<Mapping> getMappingByCodeFromCacheOrDB(String queryCode, Boolean getAllRecords) {
        String cacheKey = cacheKey(queryCode);
        List<Mapping> cacheTemp = cache.get(cacheKey);

        if (DEBUG)
            Log.i(TAG, " getMappingByCode() check if cached exist on code = '" + queryCode + "'");

        if (cacheTemp == null) {
            // 25/Jul/2011 by Art
            // Just ignore error when something wrong with the result set
            try {
                if (Thread.currentThread().isInterrupted()) return null;
                cacheTemp = dbadapter.getMappingByCode(queryCode, !isPhysicalKeyboardPressed, getAllRecords);
                if (cacheTemp != null) {
                    cache.put(cacheKey, cacheTemp);
                }
                // Jeremy '12,6,5 check if need to update code remap cache
                if (cacheTemp != null && cacheTemp != null
                        && cacheTemp.size() > 0 && cacheTemp.get(0) != null
                        && cacheTemp.get(0).isExactMatchToCodeRecord()) {
                    String remappedCode = cacheTemp.get(0).getCode();
                    if (!queryCode.equals(remappedCode)) {
                        List<String> codeList = coderemapcache.get(remappedCode);
                        String key = cacheKey(remappedCode);
                        if (codeList == null) {
                            List<String> newlist = new LinkedList<>();
                            newlist.add(remappedCode); // put self in the list
                            newlist.add(queryCode);
                            coderemapcache.put(key, newlist);
                            if (DEBUG)
                                Log.i(TAG, "getMappingByCode() build new remap code = '"
                                        + remappedCode + "' to code = '" + queryCode + "'"
                                        + " coderemapcache.size()=" + coderemapcache.size());
                        } else {
                            codeList.add(queryCode);
                            coderemapcache.remove(key);
                            coderemapcache.put(key, codeList);
                            if (DEBUG)
                                Log.i(TAG, "getMappingByCode() remappedCode: add new remap code = '" + remappedCode
                                        + "' to code = '" + queryCode + "'");
                        }

                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cacheTemp;

    }

    /**
     * 大易連打切分:把長碼串切成合法碼序列還原中文詞候選(最多 3 個,最佳在前)。
     * 優先序:記住的切分(整串碼學過) → 詞庫優先＋智慧選字挑字 → 其他切法逐段取高分。
     * 切不出回空清單。跑在候選查詢執行緒;整體結果進 segmentCache。
     */
    public List<Mapping> getSegmentedPhraseMappings(String rawCode) {
        List<Mapping> results = new LinkedList<>();
        if (rawCode == null || !tablename.startsWith("dayi")) return results;
        String code = rawCode.toLowerCase(Locale.US);

        if (segmentCache == null) segmentCache = newLruMap(MAX_CACHE_ENTRIES);
        String key = tablename + "_" + code;
        List<Mapping> cached = segmentCache.get(key);
        if (cached != null) return cached;

        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();

        // 1. 記住的切分:整串碼曾被學過(選過切分候選會寫回 code=全碼串)
        List<Mapping> learned = cachedTopWords(code);
        if (!learned.isEmpty() && learned.get(0).getWord() != null
                && learned.get(0).getWord().length() > 1) {
            addSegmentedResult(results, seen, code, learned.get(0).getWord());
        }

        List<List<String>> segLists = net.toload.main.hd.data.DayiCodeSegmenter
                .segmentCodes(code, this::cachedTopByExactCode, 3);
        if (!segLists.isEmpty()) {
            // 2. 最佳切法:詞庫優先＋智慧選字上下文挑字
            addSegmentedResult(results, seen, code, refineSegmentWords(segLists.get(0)));
            // 3. 多解:各切法逐段取最高分字(與上面不同者才收),最多 3 個候選
            for (List<String> segs : segLists) {
                if (results.size() >= 3) break;
                addSegmentedResult(results, seen, code, plainSegmentWords(segs));
            }
        }

        segmentCache.put(key, results);
        return results;
    }

    /** 使用者選了切分候選:學成 code=全碼串 的對映(再選會加分),並失效相關快取 */
    public void learnSegmentedPhrase(String rawCode, String word) {
        if (rawCode == null || rawCode.isEmpty() || word == null || word.isEmpty()
                || !tablename.startsWith("dayi")) return;
        final String code = rawCode.toLowerCase(Locale.US);
        final String key = tablename + "_" + code;
        backgroundExecutor.execute(() -> {
            try {
                dbadapter.addOrUpdateMappingRecord(code, word);
                if (topWordsCache != null) topWordsCache.remove(key);
                if (segmentCache != null) segmentCache.remove(key);
            } catch (Exception e) {
                Log.e(TAG, "learnSegmentedPhrase failed: " + e.getMessage());
            }
        });
    }

    /**
     * 大易連打模式:code 是否可接續(為某字 exact code 或任何碼的前綴)。
     * 主執行緒同步呼叫;單筆索引查詢＋快取。DB 未就緒時回 true(退回一般行為)。
     */
    public boolean canExtendCode(String rawCode) {
        if (rawCode == null || rawCode.isEmpty() || !tablename.startsWith("dayi")) return true;
        String code = rawCode.toLowerCase(Locale.US);
        if (prefixCache == null) prefixCache = newLruMap(MAX_CACHE_ENTRIES);
        String key = tablename + "_" + code;
        Boolean cached = prefixCache.get(key);
        if (cached != null) return cached;
        boolean result = dbadapter.hasCodeOrPrefix(tablename, code);
        prefixCache.put(key, result);
        return result;
    }

    /**
     * 大易連打模式:取 exact code 首選字(主執行緒同步 fallback);無則 null。
     */
    public Mapping getTopExactMapping(String rawCode) {
        if (rawCode == null || rawCode.isEmpty() || !tablename.startsWith("dayi")) return null;
        return cachedTopByExactCode(rawCode.toLowerCase(Locale.US));
    }

    // ---- 切分內部工具(皆經 LRU 快取) ----

    private Mapping cachedTopByExactCode(String code) {
        List<Mapping> list = cachedTopWords(code);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<Mapping> cachedTopWords(String code) {
        if (topWordsCache == null) topWordsCache = newLruMap(MAX_CACHE_ENTRIES);
        String key = tablename + "_" + code;
        List<Mapping> cached = topWordsCache.get(key);
        if (cached != null) return cached;
        List<Mapping> result = dbadapter.getTopWordsByExactCode(tablename, code, 5);
        topWordsCache.put(key, result);
        return result;
    }

    private List<String> cachedWordsByCodeAndLength(String code, int wordLength) {
        if (phraseWordsCache == null) phraseWordsCache = newLruMap(MAX_CACHE_ENTRIES);
        String key = tablename + "_" + code + "_" + wordLength;
        List<String> cached = phraseWordsCache.get(key);
        if (cached != null) return cached;
        List<String> result = dbadapter.getWordsByCodeAndLength(tablename, code, wordLength, 3);
        phraseWordsCache.put(key, result);
        return result;
    }

    private boolean cachedCodeMapsToWord(String code, String word) {
        if (pairCache == null) pairCache = newLruMap(MAX_CACHE_ENTRIES);
        String key = tablename + "_" + code + "\0" + word;
        Boolean cached = pairCache.get(key);
        if (cached != null) return cached;
        boolean result = dbadapter.codeMapsToWord(tablename, code, word);
        pairCache.put(key, result);
        return result;
    }

    private void addSegmentedResult(List<Mapping> results, java.util.Set<String> seen,
            String code, String words) {
        if (words == null || words.isEmpty() || words.equals(code) || !seen.add(words)) return;
        Mapping m = new Mapping();
        m.setWord(words);
        m.setCode(code);
        m.setSegmentedPhraseRecord();
        results.add(m);
    }

    /** 逐段取最高分字 */
    private String plainSegmentWords(List<String> segs) {
        StringBuilder sb = new StringBuilder();
        for (String segCode : segs) {
            Mapping top = cachedTopByExactCode(segCode);
            if (top == null || top.getWord() == null) return null;
            sb.append(top.getWord());
        }
        return sb.toString();
    }

    /**
     * 詞庫優先＋智慧選字挑字:
     * - 相鄰段落先組縮碼(每字首碼+末碼)查詞庫,詞命中且逐字驗證通過就整段採用
     * - 其餘單字段落用智慧選字上下文(前一字)挑字,無記錄時取最高分
     */
    private String refineSegmentWords(List<String> segs) {
        StringBuilder sb = new StringBuilder();
        boolean smart = mLIMEPref.getDayiSmartSelectionEnabled();
        SmartSelectionManager smartMgr = smart ? SmartSelectionManager.getInstance(mContext) : null;
        String prevChar = lastCommittedChar;
        int n = segs.size();
        int i = 0;
        while (i < n) {
            boolean matched = false;
            for (int len = Math.min(4, n - i); len >= 2 && !matched; len--) {
                StringBuilder abbr = new StringBuilder();
                for (int j = 0; j < len; j++)
                    abbr.append(net.toload.main.hd.data.DayiCodeSegmenter.abbreviate(segs.get(i + j)));
                for (String phrase : cachedWordsByCodeAndLength(abbr.toString(), len)) {
                    if (phrase.length() != len) continue;
                    boolean ok = true;
                    for (int j = 0; j < len && ok; j++)
                        ok = cachedCodeMapsToWord(segs.get(i + j), String.valueOf(phrase.charAt(j)));
                    if (ok) {
                        sb.append(phrase);
                        prevChar = String.valueOf(phrase.charAt(len - 1));
                        i += len;
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                List<Mapping> tops = cachedTopWords(segs.get(i));
                if (tops.isEmpty() || tops.get(0).getWord() == null) return null;
                String chosen = tops.get(0).getWord();
                if (smartMgr != null && prevChar != null && !prevChar.isEmpty()) {
                    int bestCount = 0;
                    for (Mapping m : tops) {
                        if (m.getWord() == null) continue;
                        int c = smartMgr.getContextCount(segs.get(i), m.getWord(), prevChar);
                        if (c > bestCount) {
                            bestCount = c;
                            chosen = m.getWord();
                        }
                    }
                }
                sb.append(chosen);
                if (!chosen.isEmpty())
                    prevChar = String.valueOf(chosen.charAt(chosen.length() - 1));
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * get real code length
     */
    int getRealCodeLength(final Mapping selectedMapping, String currentCode) {
        if (DEBUG)
            Log.i(TAG, "getRealCodeLength()");

        String code = selectedMapping.getCode();
        int realCodeLen = code.length();
        if (LimeDB.isCodeDualMapped()) { // abandon LD support for dual mapped codes. Jeremy '15,6,5
            realCodeLen = currentCode.length();
        } else {
            if (tablename.equals("phonetic")) {
                String selectedPhoneticKeyboardType = mLIMEPref.getParameterString("phonetic_keyboard_type",
                        "standard");
                String lcode = currentCode;
                if (selectedPhoneticKeyboardType.startsWith("eten")) {
                    lcode = dbadapter.preProcessingRemappingCode(currentCode);
                }
                String noToneCode = code.replaceAll("[3467 ]", "");
                if (code.equals(noToneCode)) {
                    realCodeLen = code.length();
                } else if (!lcode.startsWith(code) && lcode.startsWith(noToneCode)) {
                    realCodeLen = noToneCode.length();
                } else {
                    realCodeLen = currentCode.length(); // unexpected condition.
                }
            }
        }

        // remove elements in suggestionLoL with code length smaller than current code
        // length - submitted code length
        if (realCodeLen < currentCode.length()) {
            Iterator<List<Pair<Mapping, String>>> itl = suggestionLoL.iterator();
            while (itl.hasNext()) {
                List<Pair<Mapping, String>> lpe = itl.next();
                Iterator<Pair<Mapping, String>> it = lpe.iterator();
                while (it.hasNext()) {
                    Pair<Mapping, String> pe = it.next();
                    if (pe.second.length() > currentCode.length() - realCodeLen) {
                        it.remove();
                    }
                }
                if (lpe.isEmpty())
                    itl.remove();
            }
            Iterator<Pair<Mapping, String>> it = bestSuggestionStack.iterator();
            while (it.hasNext()) {
                Pair<Mapping, String> pe = it.next();
                if (pe.second.length() > currentCode.length() - realCodeLen) {
                    it.remove();
                }
            }
        }

        // learn ld phrase if the select mapping is run-time suggestion
        if (selectedMapping != null && selectedMapping.isRuntimeBuiltPhraseRecord() &&
                suggestionLoL != null && !suggestionLoL.isEmpty()) {

            final List<Pair<Mapping, String>> bestSuggestionList = new LinkedList<>(
                    suggestionLoL.get(suggestionLoL.size() - 1));
            final String selectedWord = selectedMapping.getWord();

            backgroundExecutor.execute(new Runnable() {
                public void run() {

                    if (!bestSuggestionList.isEmpty()) {
                        for (int j = 0; j < bestSuggestionList.size(); j++) {
                            // TODO:should learn QP code for phonetic table
                            if (selectedWord.startsWith(bestSuggestionList.get(j).first.getWord())) {
                                if (bestSuggestionList.get(j).first.getWord().length() > 8)
                                    break; // stop learning if word length > 8
                                dbadapter.addOrUpdateMappingRecord(bestSuggestionList.get(j).second,
                                        bestSuggestionList.get(j).first.getWord());
                                removeRemappedCodeCachedMappings(bestSuggestionList.get(j).second);
                            }

                            if ((DEBUG || dumpRunTimeSuggestion))// dump best suggestion list
                                Log.i(TAG, "getRealCodeLength() best suggestion list(" + j + "): word="
                                        + bestSuggestionList.get(j).first.getWord() + ", code="
                                        + bestSuggestionList.get(j).second);

                        }

                    }

                }
            });

        }

        return realCodeLen;
    }

    // Single shared worker for score/phrase learning DB writes. One thread keeps
    // submission order (selection score before finish-input learning) and stops
    // the old pattern of spawning 2+ short-lived threads on every selection.
    private static final java.util.concurrent.ExecutorService backgroundExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    private static <K, V> Map<K, V> newLruMap(int maxSize) {
        return Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        });
    }

    /**
     * This method is to initial/reset the cache of im.
     */
    public void initialCache() {
        try {
            clear();
        } catch (Exception e) {
            // e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        cache = newLruMap(MAX_CACHE_ENTRIES);
        engcache = newLruMap(MAX_CACHE_ENTRIES);
        emojicache = newLruMap(MAX_CACHE_ENTRIES);
        keynamecache = newLruMap(MAX_CACHE_ENTRIES);
        coderemapcache = newLruMap(MAX_CACHE_ENTRIES);
        relatedcache = newLruMap(MAX_CACHE_ENTRIES);
        relatedPhraseCache = newLruMap(MAX_CACHE_ENTRIES);
        topWordsCache = newLruMap(MAX_CACHE_ENTRIES);
        prefixCache = newLruMap(MAX_CACHE_ENTRIES);
        phraseWordsCache = newLruMap(MAX_CACHE_ENTRIES);
        pairCache = newLruMap(MAX_CACHE_ENTRIES);
        segmentCache = newLruMap(MAX_CACHE_ENTRIES);

        // initial exact match stack here
        suggestionLoL = new LinkedList<>();
        bestSuggestionStack = new Stack<>();
    }

    private void updateScoreCache(Mapping cachedMapping) {
        if (DEBUG)
            Log.i(TAG, "updateScoreCache(): code=" + cachedMapping.getCode());

        dbadapter.addScore(cachedMapping);
        // Selection may change related-table scores; drop cached related phrases
        // so the next lookup reflects the new ordering.
        if (relatedcache != null)
            relatedcache.clear();
        if (relatedPhraseCache != null)
            relatedPhraseCache.clear();
        // Jeremy '11,7,29 update cached here
        if (!cachedMapping.isRelatedPhraseRecord()) {
            String code = cachedMapping.getCode().toLowerCase(Locale.US);
            String cachekey = cacheKey(code);
            List<Mapping> cachedList = cache.get(cachekey);
            // null id denotes target is selected from the related list (not exact match)
            if ((cachedMapping.getId() == null || cachedMapping.isPartialMatchToCodeRecord()) // Jeremy '15,6,3 new
                                                                                              // record type to identify
                                                                                              // partial match
                    && cachedList != null && !cachedList.isEmpty()) {
                if (DEBUG)
                    Log.i(TAG, "updateScoreCache(): updating related list");
                if (cache.remove(cachekey) == null) {
                    removeRemappedCodeCachedMappings(code);
                }
                // non null id denotes target is in exact match result list.
            } else if ((cachedMapping.getId() != null || cachedMapping.isExactMatchToCodeRecord()) // Jeremy '15,6,3 new
                                                                                                   // record type to
                                                                                                   // identify exact
                                                                                                   // match
                    && cachedList != null && !cachedList.isEmpty()) {

                boolean sort;
                if (isPhysicalKeyboardPressed)
                    sort = mLIMEPref.getPhysicalKeyboardSortSuggestions();
                else
                    sort = mLIMEPref.getSortSuggestions();

                // 肌肉記憶安全規格:大易候選順序固定,不做使用頻率冒泡重排
                // (分數仍累計入 DB;唯一的順序調整是查詢時的純上下文單一提升)
                if (tablename != null && tablename.startsWith("dayi"))
                    sort = false;

                if (sort) { // Jeremy '12,5,22 do not update the order of exact match list if the sort
                            // option is off
                    int size = cachedList.size();
                    if (DEBUG)
                        Log.i(TAG, "updateScoreCache(): cachedList.size:" + size);
                    // update exact match cache
                    for (int j = 0; j < size; j++) {
                        Mapping cm = cachedList.get(j);
                        if (DEBUG)
                            Log.i(TAG, "updateScoreCache(): cachedList at :" + j + ". score=" + cm.getScore());
                        if (cachedMapping.getId().equals(cm.getId())) {
                            int score = cm.getScore() + 1;
                            if (DEBUG)
                                Log.i(TAG, "updateScoreCache(): cachedMapping found at :" + j + ". new score=" + score);
                            cm.setScore(score);
                            if (j > 0 && score > cachedList.get(j - 1).getScore()) {
                                cachedList.remove(j);
                                for (int k = 0; k < j; k++) {
                                    if (cachedList.get(k).getScore() <= score) {
                                        cachedList.add(k, cm);
                                        break;
                                    }
                                }

                            }
                            break;
                        }
                    }
                }
                // Jeremy '11,7,31
                // exact match score was changed, related list in similar codes should be
                // rebuild
                // (eg. d, de, and def for code, defg)
                updateSimilarCodeCache(code);

            } else {// Jeremy '12,6,5 code not in cache do removeRemappedCodeCachedMappings and
                    // removed cached items of ramped codes.

                removeRemappedCodeCachedMappings(code);
            }
        }

    }

    public void postFinishInput() {

        if (scorelistSnapshot == null)
            scorelistSnapshot = new LinkedList<>();
        else
            scorelistSnapshot.clear();

        if (DEBUG)
            Log.i(TAG, "postFinishInput(), creating offline updating thread");
        // Jeremy '11,7,31 The updating process takes some time. Run it on the
        // shared background worker.
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                // for thread-safe operation, duplicate local copy of scorelist and
                // LDphraselistarray
                // List<Mapping> localScorelist = new LinkedList<Mapping>();
                if (scorelist != null) {
                    scorelistSnapshot.addAll(scorelist);
                    scorelist.clear();
                }
                // Jeremy '11,7,28 combine to adduserdict and addscore
                // Jeremy '11,6,12 do adduserdict and add score if diclist.size > 0 and only
                // adduserdict if diclist.size >1
                // Jeremy '11,6,11, always learn scores, but sorted according preference options

                // Learn the consecutive two words as a related phrase).
                learnRelatedPhrase(scorelistSnapshot);

                ArrayList<List<Mapping>> localLDPhraseListArray = new ArrayList<>();
                if (LDPhraseListArray != null) {
                    localLDPhraseListArray.addAll(LDPhraseListArray);
                    LDPhraseListArray.clear();
                }

                // Learn LD Phrase
                learnLDPhrase(localLDPhraseListArray);

            }
        });

    }

    private void learnRelatedPhrase(List<Mapping> localScorelist) {
        if (localScorelist != null) {
            if (DEBUG)
                Log.i(TAG, "learnRelatedPhrase(), localScorelist.size=" + localScorelist.size());
            if (mLIMEPref.getLearnRelatedWord() && localScorelist.size() > 1) {
                // Learning below changes the related table; drop cached related phrases.
                if (relatedcache != null)
                    relatedcache.clear();
                if (relatedPhraseCache != null)
                    relatedPhraseCache.clear();
                for (int i = 0; i < localScorelist.size(); i++) {
                    Mapping unit = localScorelist.get(i);
                    if (unit == null) {
                        continue;
                    }
                    if (i + 1 < localScorelist.size()) {
                        Mapping unit2 = localScorelist.get((i + 1));
                        if (unit2 == null) {
                            continue;
                        }
                        if (unit.getWord() != null && !unit.getWord().equals("")

                                && unit2.getWord() != null && !unit2.getWord().equals("")

                                &&
                                (unit.isExactMatchToCodeRecord() || unit.isPartialMatchToCodeRecord()
                                        || unit.isRelatedPhraseRecord()) // use record type to identify records. Jeremy
                                                                         // '15,6,4

                                &&
                                (unit2.isExactMatchToCodeRecord() || unit2.isPartialMatchToCodeRecord()
                                        || unit.isRelatedPhraseRecord() || unit2.isChinesePunctuationSymbolRecord()
                                        || unit.isEmojiRecord() || unit2.isEmojiRecord())

                        // allow unit2 to be chinese punctuation symbols.
                        // && !unit.getCode().equals(unit.getWord())//Jeremy '12,6,13 avoid learning
                        // mixed mode english
                        // && !unit2.getCode().equals(unit2.getWord())
                        /// && unit2.getId() !=null
                        ) {

                            int score;

                            // if (unit.getId() != null && unit2.getId() != null) //Jeremy '12,7,2 eliminate
                            // learning english words.
                            score = dbadapter.addOrUpdateRelatedPhraseRecord(unit.getWord(), unit2.getWord());
                            if (DEBUG)
                                Log.i(TAG, "learnRelatedPhrase(), the return score = " + score);
                            // Jeremy '12,6,7 learn LD phrase if the score of userdic is > 20
                            if (score > 20 && mLIMEPref.getLearnPhrase()) {
                                addLDPhrase(unit, false);
                                addLDPhrase(unit2, true);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Jeremy '12,6,9 Rewrite to support word with more than 1 characters
     */

    private void learnLDPhrase(ArrayList<List<Mapping>> localLDPhraseListArray) {
        if (DEBUG)
            Log.i(TAG, "learnLDPhrase()");

        if (localLDPhraseListArray != null && localLDPhraseListArray.size() > 0) {
            if (DEBUG)
                Log.i(TAG, "learnLDPhrase(): LDPhrase learning, arraysize =" + localLDPhraseListArray.size());

            for (List<Mapping> phraselist : localLDPhraseListArray) {
                if (DEBUG)
                    Log.i(TAG, "learnLDPhrase(): LDPhrase learning, current list size =" + phraselist.size());
                if (phraselist.size() > 0 && phraselist.size() < 5) { // Jeremy '12,6,8 limit the phrase to have 4
                                                                      // chracters

                    String baseCode, LDCode = "", QPCode = "", baseWord;

                    Mapping unit1 = phraselist.get(0);

                    if (DEBUG)
                        Log.i(TAG, "learnLDPhrase(): unit1.getId() = " + unit1.getId()
                                + ", unit1.getCode() =" + unit1.getCode()
                                + ", unit1.getWord() =" + unit1.getWord());

                    if (unit1 == null || unit1.getWord().length() == 0
                            || unit1.getCode().equals(unit1.getWord())) // Jeremy '12,6,13 avoid learning mixed mode
                                                                        // english
                    {
                        break;
                    }

                    baseCode = unit1.getCode();
                    baseWord = unit1.getWord();

                    if (baseWord.length() == 1) {
                        if (unit1.getId() == null // Jeremy '12,6,7 break if id is null (selected from related list)
                                || unit1.isPartialMatchToCodeRecord() // Jeremy '15,6,3 new record identification
                                || unit1.getCode() == null // Jeremy '12,6,7 break if code is null (selected from
                                                           // related phrase)
                                || unit1.getCode().length() == 0
                                || unit1.isRelatedPhraseRecord()) {
                            List<Mapping> rMappingList = dbadapter.getMappingByWord(baseWord, tablename);
                            if (rMappingList.size() > 0)
                                baseCode = rMappingList.get(0).getCode();
                            else
                                break; // look-up failed, abandon.
                        }
                        if (baseCode != null && baseCode.length() > 0)
                            QPCode += baseCode.substring(0, 1);
                        else
                            break;// abandon the phrase learning process;

                        // if word length >0, lookup all codes and rebuild basecode and QPCode
                    } else if (baseWord.length() > 1 && baseWord.length() < 5) {
                        baseCode = "";
                        for (int i = 0; i < baseWord.length(); i++) {
                            String c = baseWord.substring(i, i + 1);
                            List<Mapping> rMappingList = dbadapter.getMappingByWord(c, tablename);
                            if (rMappingList.size() > 0) {
                                baseCode += rMappingList.get(0).getCode();
                                QPCode += rMappingList.get(0).getCode().substring(0, 1);
                            } else {
                                baseCode = ""; // r-lookup failed. abandon the phrase learning
                                break;
                            }
                        }
                    }

                    for (int i = 0; i < phraselist.size(); i++) {
                        if (i + 1 < phraselist.size()) {

                            Mapping unit2 = phraselist.get((i + 1));
                            if (unit2 == null || unit2.getWord().length() == 0 || unit2.isComposingCodeRecord()
                                    || unit2.isEnglishSuggestionRecord()) // Jeremy 15,6,4 exclude composing code
                            // || unit2.getCode().equals(unit2.getWord())) //Jeremy '12,6,13 avoid learning
                            // mixed mode english
                            {
                                break;
                            }

                            String word2 = unit2.getWord();
                            String code2 = unit2.getCode();
                            baseWord += word2;

                            if (word2.length() == 1 && baseWord.length() < 5) { // limit the phrase size to 4
                                if (unit2.getId() == null // Jeremy '12,6,7 break if id is null (selected from related
                                                          // phrase)
                                        || unit2.isPartialMatchToCodeRecord() // Jeremy '15,6,3 new record
                                                                              // identification
                                        || code2 == null // Jeremy '12,6,7 break if code is null (selected from
                                                         // relatedphrase)
                                        || code2.length() == 0
                                        || unit2.isRelatedPhraseRecord()) {
                                    List<Mapping> rMappingList = dbadapter.getMappingByWord(word2, tablename);
                                    if (rMappingList.size() > 0)
                                        code2 = rMappingList.get(0).getCode();
                                    else
                                        break;
                                }
                                if (code2 != null && code2.length() > 0) {
                                    baseCode += code2;
                                    QPCode += code2.substring(0, 1);
                                } else
                                    break; // abandon the phrase learning process;

                                // if word length >0, lookup all codes and rebuild basecode and QPCode
                            } else if (word2.length() > 1 && baseWord.length() < 5) {
                                for (int j = 0; j < word2.length(); j++) {
                                    String c = word2.substring(j, j + 1);
                                    List<Mapping> rMappingList = dbadapter.getMappingByWord(c, tablename);
                                    if (rMappingList.size() > 0) {
                                        baseCode += rMappingList.get(0).getCode();
                                        QPCode += rMappingList.get(0).getCode().substring(0, 1);
                                    } else // r-lookup failed. abandon the phrase learning
                                        break;
                                }
                            } else // abandon the learing process.
                                break;

                            if (DEBUG)
                                Log.i(TAG, "learnLDPhrase(): code1 = " + unit1.getCode()
                                        + ", code2 = '" + code2
                                        + "', word1 = " + unit1.getWord()
                                        + ", word2 = " + word2
                                        + ", basecode = '" + baseCode
                                        + "', baseWord = " + baseWord
                                        + ", QPcode = '" + QPCode
                                        + "'.");
                            if (i + 1 == phraselist.size() - 1) {// only learn at the end of the phrase word '12,6,8
                                if (tablename.equals("phonetic")) {// remove tone symbol in phonetic table
                                    LDCode = baseCode.replaceAll("[3467 ]", "").toLowerCase(Locale.US);
                                    QPCode = QPCode.toLowerCase(Locale.US);
                                    if (LDCode.length() > 1) {
                                        dbadapter.addOrUpdateMappingRecord(LDCode, baseWord);
                                        removeRemappedCodeCachedMappings(LDCode);
                                        updateSimilarCodeCache(LDCode);
                                    }
                                    if (QPCode.length() > 1) {
                                        dbadapter.addOrUpdateMappingRecord(QPCode, baseWord);
                                        removeRemappedCodeCachedMappings(QPCode);
                                        updateSimilarCodeCache(QPCode);
                                    }
                                } else if (baseCode.length() > 1) {
                                    baseCode = baseCode.toLowerCase(Locale.US);
                                    dbadapter.addOrUpdateMappingRecord(baseCode, baseWord);
                                    removeRemappedCodeCachedMappings(baseCode);
                                    updateSimilarCodeCache(baseCode);
                                }
                                if (DEBUG)
                                    Log.i(TAG, "learnLDPhrase(): LDPhrase learning, baseCode = '" + baseCode
                                            + "', LDCode = '" + LDCode + "', QPCode=" + QPCode + "'."
                                            + ", baseWord" + baseWord);

                            }

                        }
                    }
                }
            }

        }
    }

    /**
     *
     */
    private void removeRemappedCodeCachedMappings(String code) {
        if (DEBUG)
            Log.i(TAG, "removeRemappedCodeCachedMappings() on code ='" + code + "' coderemapcache.size="
                    + coderemapcache.size());
        List<String> codelist = coderemapcache.get(cacheKey(code));
        if (codelist != null) {
            for (String entry : codelist) {
                if (DEBUG)
                    Log.i(TAG, "removeRemappedCodeCachedMappings() remove code= '" + entry + "' from cache.");
                cache.remove(cacheKey(entry));
            }
        } else
            cache.remove(cacheKey(code)); // Jeremy '12,6,6 no remap. remove the code mapping from cache.
    }

    private void updateSimilarCodeCache(String code) {
        if (DEBUG)
            Log.i(TAG, "updateSimilarCodeCache(): code = '" + code + "'");
        String cachekey;
        List<Mapping> cachedList;// = cache.get(cachekey);
        int len = code.length();
        if (len > 5)
            len = 5; // Jeremy '12,6,7 change max backward level to 5.
        for (int k = 1; k < len; k++) {
            String key = code.substring(0, code.length() - k);
            cachekey = cacheKey(key);
            cachedList = cache.get(cachekey);
            if (DEBUG)
                Log.i(TAG, "updateSimilarCodeCache(): cachekey = '" + cachekey + "' cachedList == null :"
                        + (cachedList == null));
            if (cachedList != null) {
                cache.remove(cachekey);
            } else {
                if (DEBUG)
                    Log.i(TAG,
                            "updateSimilarCodeCache(): code not in cache. update to db only on code = '" + key + "'");
                removeRemappedCodeCachedMappings(key);
            }
            if (code.length() == 1)// prefetch if code length ==1
                try {
                    getMappingByCode(code, !isPhysicalKeyboardPressed, false, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }

        // Between-search results cached under longer codes can include this code's
        // records with the old score; drop those keys too (cache is capped at 512).
        String prefix = cacheKey(code);
        synchronized (cache) {
            java.util.Iterator<String> it = cache.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                if (key.length() > prefix.length() && key.startsWith(prefix)) {
                    it.remove();
                }
            }
        }
    }

    public String keyToKeyname(String code) {
        // Jeremy '11,6,21 Build cache according using cachekey

        String cacheKey = cacheKey(code);
        String result = keynamecache.get(cacheKey);
        if (result == null) {
            // loadDBAdapter(); openLimeDatabase();
            result = dbadapter.keyToKeyname(code, tablename, true);
            keynamecache.put(cacheKey, result);
        }
        return result;
    }

    /**
     * Renamed from addUserDict and pass parameter with mapping directly Jeremy
     * '12,6,5
     * Renamed to learnRelatedPhraseAndUpdateScore Jeremy '15,6,4
     */

    public void learnRelatedPhraseAndUpdateScore(Mapping updateMapping)
            // String id, String code, String word,
            // String pword, int score, boolean isDictionary)
            {
        if (DEBUG)
            Log.i(TAG, "learnRelatedPhraseAndUpdateScore() ");

        if (scorelist == null) {
            scorelist = new ArrayList<>();
        }

        // Temp final Mapping Object For updateMapping thread.
        if (updateMapping != null) {
            final Mapping updateMappingTemp = new Mapping(updateMapping);

            if (tablename.startsWith("dayi") && mLIMEPref.getDayiSmartSelectionEnabled()) {
                // If lastCommittedChar is already the picked word itself (the
                // commit path updated it first), the real context is the char
                // before it.
                String prevTmp = lastCommittedChar;
                if (prevTmp != null && prevTmp.equals(updateMappingTemp.getWord()))
                    prevTmp = prevBeforeLastCommittedChar;
                final String prev = prevTmp;
                backgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SmartSelectionManager.getInstance(mContext).recordSelection(
                                    updateMappingTemp.getCode(),
                                    updateMappingTemp.getWord(),
                                    prev
                            );
                        } catch (Exception e) {
                            Log.e(TAG, "Error recording selection in SmartSelectionManager", e);
                        }
                    }
                });
            }

            // Jeremy '11,6,11. Always update score and sort according to preferences.
            scorelist.add(updateMappingTemp);
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    updateScoreCache(updateMappingTemp);
                }
            });
        }
    }

    public void addLDPhrase(Mapping mapping, // String id, String code, String word, int score,
            boolean ending) {
        if (LDPhraseListArray == null)
            LDPhraseListArray = new ArrayList<>();
        if (LDPhraseList == null)
            LDPhraseList = new LinkedList<>();

        if (mapping != null) { // force interruped if mapping=null
            LDPhraseList.add(mapping);
        }

        if (ending) {
            if (LDPhraseList.size() > 1)
                LDPhraseListArray.add(LDPhraseList);
            LDPhraseList = new LinkedList<>();
        }

        if (DEBUG)
            Log.i(TAG, "addLDPhrase()"// +mapping.getCode() + ". id=" + mapping.getId()
                    + ". ending:" + ending
                    + ". LDPhraseListArray.size=" + LDPhraseListArray.size()
                    + ". LDPhraseList.size=" + LDPhraseList.size());

    }

    public List<KeyboardObj> getKeyboardList() {
        // if(dbadapter == null){dbadapter = new LimeDB(ctx);}
        return dbadapter.getKeyboardList();
    }

    public List<ImObj> getImList() {
        // if(dbadapter == null){dbadapter = new LimeDB(ctx);}
        return dbadapter.getImList();
    }

    public void clear() {
        if (scorelist != null) {
            scorelist.clear();
        }
        if (cache != null) {
            cache.clear();
        }
        if (engcache != null) {
            engcache.clear();
        }
        if (emojicache != null) {
            emojicache.clear();
        }
        if (keynamecache != null) {
            keynamecache.clear();
        }

        if (coderemapcache != null) {
            coderemapcache.clear();
        }
    }

    public List<Mapping> getEnglishSuggestions(String word) {

        long startTime = 0;
        if (DEBUG || dumpRunTimeSuggestion) {
            startTime = System.currentTimeMillis();
            Log.i(TAG, "getEnglishSuggestions()");
        }

        List<Mapping> result = new LinkedList<>();

        // Jeremy '15,7,16 return zero result if last query returns no result
        if (!(word.length() > 1 && lastEnglishWord != null && word.startsWith(lastEnglishWord)
                && noSuggestionsForLastEnglishWord)) {

            List<Mapping> cacheTemp = engcache.get(word);

            if (cacheTemp != null) {
                result.addAll(cacheTemp);
            } else {
                List<String> tempResult = dbadapter.getEnglishSuggestions(word);
                for (String u : tempResult) {
                    Mapping temp = new Mapping();
                    temp.setWord(u);
                    temp.setEnglishSuggestionRecord();
                    result.add(temp);
                }
                if (result.size() > 0) {
                    engcache.put(word, result);
                }
            }

            noSuggestionsForLastEnglishWord = result.isEmpty();
            lastEnglishWord = word;
        }

        if (DEBUG || dumpRunTimeSuggestion) {
            Log.i(TAG, "getEnglishSuggestions() time elapsed =" + (System.currentTimeMillis() - startTime));
        }

        return result;

    }

    /*
     * public boolean isImKeys(char c) {
     * if (imKeysMap.get(tablename) == null || imKeysMap.size() == 0) {
     * //if(dbadapter == null){dbadapter = new LimeDB(ctx);}
     * imKeysMap.put(tablename, dbadapter.getImInfo(tablename, "imkeys"));
     * }
     * String imkeys = imKeysMap.get(tablename);
     * return !(imkeys == null || imkeys.equals("")) && (imkeys.indexOf(c) >= 0);
     * }
     */
    public String getSelkey() {
        if (DEBUG)
            Log.i(TAG, "getSelkey():hasNumber:" + hasNumberMapping + "hasSymbol:" + hasSymbolMapping);
        String selkey;
        String table = tablename;
        if (tablename.equals("phonetic")) {
            table = tablename + mLIMEPref.getPhoneticKeyboardType();
        }
        if (selKeyMap.get(table) == null || selKeyMap.size() == 0) {
            // if(dbadapter == null){dbadapter = new LimeDB(ctx);}
            selkey = dbadapter.getImInfo(tablename, "selkey");
            if (DEBUG)
                Log.i(TAG, "getSelkey():selkey from db:" + selkey);
            boolean validSelkey = true;
            if (selkey != null && selkey.length() == 10) {
                for (int i = 0; i < 10; i++) {
                    if (Character.isLetter(selkey.charAt(i)) ||
                            (hasNumberMapping && Character.isDigit(selkey.charAt(i))))
                        validSelkey = false;

                }
            } else
                validSelkey = false;
            // Jeremy '11,6,19 Rewrite for IM has symbol mapping like ETEN
            if (!validSelkey || tablename.startsWith("dayi") || tablename.equals("phonetic")) {
                if (hasNumberMapping && hasSymbolMapping) {
                    if (tablename.startsWith("dayi")
                            || (tablename.equals("phonetic")
                                    && mLIMEPref.getPhoneticKeyboardType().equals("standard"))) {
                        selkey = " []-'^&*(";
                    } else {
                        selkey = "!@#$%^&*()";
                    }
                } else if (hasNumberMapping) {
                    selkey = "'[]-\\^&*()";
                } else {
                    selkey = "1234567890";
                }
            }
            if (DEBUG)
                Log.i(TAG, "getSelkey():selkey:" + selkey);
            selKeyMap.put(table, selkey);
        }
        return selKeyMap.get(table);
    }
}