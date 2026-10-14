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

package net.toload.main.hd.limedb;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import net.toload.main.hd.data.Mapping;

/**
 * LimeQueryEngine — 打字熱路徑查詢,自 LimeDB 抽離(重構二期)。
 * 方法本體逐字搬移,行為不變;共用狀態仍存於 LimeDB,一律經 db 存取。
 */
class LimeQueryEngine {

    private final LimeDB db;

    // 以下欄位/快取只有熱路徑查詢方法使用,自 LimeDB 隨方法移入
    private final HashMap<String, HashMap<String, String>> keysDefMap = new HashMap<>();
    private final HashMap<String, HashMap<String, String>> keysReMap = new HashMap<>();
    private final HashMap<String, HashMap<String, String>> keysDualMap = new HashMap<>();
    private String lastCode = "";
    private String lastValidDualCodeList = "";
    // Jeremy '11,6,16 keep the soft/physical keyboard flag from getmapping()
    private boolean isPhysicalKeyboardPressed = false;

    LimeQueryEngine(LimeDB db) {
        this.db = db;
    }

    /*
     * Rewrite by Jeremy 11,6,4. Supporting array and dayi now.
     * Covert composing codes into composing text (reading string).
     *
     */
    // 查詢執行緒與背景預取會同時呼叫 getMappingByCode/keyToKeyname,這些方法共用 lastCode、
    // lastValidDualCodeList 與按鍵對照表:同步執行,避免字根顯示拼到別次查詢的結果
    synchronized String keyToKeyname(String code, String table, Boolean composingText) {
        // Jeremy '11,8,30
        if (composingText && code.length() > LimeDB.COMPOSING_CODE_LENGTH_LIMIT)
            return code;

        String keyboardtype = db.mLIMEPref.getPhysicalKeyboardType();
        String phonetickeyboardtype = db.mLIMEPref.getPhoneticKeyboardType();
        String keytable = table;

        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "keyToKeyname():code:" + code +
                    " lastValidDualCodeList=" + lastValidDualCodeList +
                    " table:" + table + " tablename:" + db.tablename +
                    " isPhysicalKeybaordPressed:" + isPhysicalKeyboardPressed +
                    " keyboardtype: " + keyboardtype +
                    " composingText:" + composingText);

        if (isPhysicalKeyboardPressed) {
            if (composingText && table.equals("phonetic")) {// doing composing popup
                keytable = table + keyboardtype + phonetickeyboardtype;
            } else if (composingText)
                keytable = table + keyboardtype;
        } else if (composingText && db.tablename.equals("phonetic")) {
            keytable = table + phonetickeyboardtype;
        }
        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "keyToKeyname():keytable:" + keytable);

        if (composingText) {// building composing text and get dual mapped codes

            if (!code.equals(lastCode)) {
                // unsynchronized cache. do the preprocessing again.
                // preProcessingForExtraQueryConditions(preProcessingRemappingCode(code));
                getMappingByCode(code, false);
            }
            // String dualCodeList = lastValidDualCodeList;
            if (lastValidDualCodeList != null) {
                if (LimeDB.DEBUG)
                    Log.i(LimeDB.TAG, "keyToKeyname():lastValidDualCodeList:" + lastValidDualCodeList +
                            " table:" + table + " tablename:" + db.tablename);
                // code = dualCodeList;
                if (db.tablename.equals("phonetic")) {
                    keytable = "phonetic";
                    keyboardtype = "normal_keyboard";
                    phonetickeyboardtype = "standard";
                }
                if (db.tablename.startsWith("dayi")) {
                    keytable = "dayi";
                    keyboardtype = "normal_keyboard";
                }

            }
        }

        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "keyToKeyname():code:" + code +
                    " table:" + table + " tablename:" + db.tablename + " keytable:" + keytable);

        if (keysDefMap.get(keytable) == null
                || keysDefMap.get(keytable).size() == 0) {

            String keyString, keynameString;
            // Jeremy 11,6,4 Load keys and keynames from im table.
            keyString = db.getImInfo(table, "imkeys");
            keynameString = db.getImInfo(table, "imkeynames");



            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG, "keyToKeyname(): load from db: imkeys:keyString=" + keyString + ", imkeynames="
                        + keynameString);

            if (table.startsWith("phonetic") || table.startsWith("dayi") ||
                    keyString.equals("") || keynameString.equals("")) {
                if (table.startsWith("phonetic")) {
                    keyString = LimeDB.BPMF_KEY;
                    keynameString = LimeDB.BPMF_CHAR;
                } else if (table.startsWith("dayi")) {
                    keyString = LimeDB.DAYI_KEY;
                    keynameString = LimeDB.DAYI_CHAR;
                }
            }
            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG,
                        "keyToKeyname():keyboardtype:" + keyboardtype + " phonetickeyboardtype:" + phonetickeyboardtype
                                +
                                " composing?:" + composingText +
                                " keyString:" + keyString + " keynameString:" + keynameString);
            if (keyString != null && keyString.length() > 0) {
                HashMap<String, String> keyMap = new HashMap<>();

                String[] charlist = keynameString.split("\\|");


                // Ignore the exception of key name mapping.
                try {
                    for (int i = 0; i < keyString.length(); i++) {
                        keyMap.put(keyString.substring(i, i + 1), charlist[i]);
                    }
                } catch (Exception ignored) {
                }

                keyMap.put("|", "|"); // put the seperator for multi-code display
                keysDefMap.put(keytable, keyMap);
            }

        }

        // Starting doing key to keyname conversion ------------------------------------
        if (keysDefMap.get(keytable) == null
                || keysDefMap.get(keytable).size() == 0) {
            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG, "keyToKeyname():nokeysDefMap found!!");
            return code;

        } else {
            if (composingText &&
                    (lastValidDualCodeList != null)) // Jeremy '11,10,6 bug fixed on rmapping returning orignal code.
                code = lastValidDualCodeList;
            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG, "keyToKeyname():lastValidDualCodeList=" + lastValidDualCodeList);

            String result = "";
            HashMap<String, String> keyMap = keysDefMap.get(keytable);
            // do the real conversion
            for (int i = 0; i < code.length(); i++) {
                String c = keyMap.get(code.substring(i, i + 1));
                if (c != null)
                    result = result + c;
            }
            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG, "keyToKeyname():returning:" + result);

            if (result.equals("")) {
                return code;
            } else {
                return result;
            }
        }

    }

    /**
     * Retrieve matched records
     */
    synchronized List<Mapping> getMappingByCode(String code, boolean softKeyboard) {

        String codeorig = code;

        long startTime = 0;
        if (LimeDB.DEBUG || LimeDB.probePerformance) {
            startTime = System.currentTimeMillis();
            Log.i(LimeDB.TAG,
                    "getMappingByCode(): code='" + code + ", table=" + db.tablename);
        }

        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!db.checkDBConnection())
            return null;

        boolean sort;
        if (softKeyboard)
            sort = db.mLIMEPref.getSortSuggestions();
        else
            sort = db.mLIMEPref.getPhysicalKeyboardSortSuggestions();
        isPhysicalKeyboardPressed = !softKeyboard;

        // Add by Jeremy '10, 3, 27. Extension on multi table query.
        lastCode = code;
        lastValidDualCodeList = null; // reset the lastValidDualCodeList
        List<Mapping> result = null;

        // Two-steps query with code pre-processing. Jeremy '11,6,15
        // Step.1 Code re-mapping.
        code = preProcessingRemappingCode(code);
        code = code.toLowerCase(Locale.US); // Jeremy '12,4,1 moved from SearchService.getMappingByCode();
        // Step.2 Build extra getMappingByCode conditions. (e.g. dualcode remap)
        Pair<String, String> extraConditions = preProcessingForExtraQueryConditions(code);
        String extraSelectClause = "";
        String extraExactMatchClause = "";
        if (extraConditions != null) {
            extraSelectClause = extraConditions.first;
            extraExactMatchClause = extraConditions.second;
        }
        // Jeremy '11,6,11 separated suggestions sorting option for physical keyboard

        try {
            if (!code.equals("")) {

                Cursor cursor = null;
                try {

                    // Jeremy '11,8,2 Query noToneCode instead of code for code contains no tone
                    // symbols
                    // Jeremy '12,6,5 rewrite to consistent with expanddualcode
                    // Jeremy '15,6,6 always search no tone code for phonetic. The db will be
                    // upgraded in onUprade if code3r is not present

                    String codeCol = LimeDB.FIELD_CODE;

                    final boolean tonePresent = code.matches(".+[3467 ].*"); // Tone symbols present in any locoation
                                                                             // except the first character
                    final boolean toneNotLast = code.matches(".+[3467 ].+"); // Tone symbols present in any locoation
                                                                             // except the first and last character

                    if (db.tablename.equals("phonetic")) {
                        if (tonePresent) {
                            // LD phrase if tone symbols present but not in last character or in last
                            // character but the length > 4
                            // (phonetic combinations never has length >4)
                            if (toneNotLast || (code.length() > 4))
                                code = code.replaceAll("[3467 ]", "");

                        } else { // no tone symbols present, check NoToneCode column
                            codeCol = LimeDB.FIELD_NO_TONE_CODE;
                        }
                        code = code.trim();
                    }

                    String selectClause;
                    String sortClause;
                    String escapedCode = code.replace("'", "''");
                    int codeLen = code.length();

                    String limitClause = LimeDB.INITIAL_RESULT_LIMIT;
                    // 候選列已沒有「捲到底載入更多」,精確碼的字要一次全部查出來;否則注音 ㄧˋ
                    // 這類同音字多的碼只看得到前 15 個。精確碼不超過上限時照舊
                    int exactCount = countExactMatches(codeCol, code, extraExactMatchClause);
                    if (exactCount > Integer.parseInt(limitClause))
                        limitClause = String.valueOf(exactCount);

                    // Jeremy '15, 6, 1 between search clause without using related column for
                    // better sorting order.
                    // if(betweenSearch){
                    selectClause = expandBetweenSearchClause(codeCol, code) + extraSelectClause;
                    String exactMatchCondition = " (" + codeCol + " ='" + escapedCode + "' " + extraExactMatchClause
                            + ") ";
                    sortClause = "( exactmatch = 1 and ( score > 0 or  basescore >0) and length(word)=1) desc, exactmatch desc,"
                            + " (length(" + codeCol + ") >= " + codeLen + " ) desc, "
                            + "(length(" + codeCol + ") <= " + ((codeLen > 5) ? 5 : codeLen) + " )*length(" + codeCol
                            + ") desc, ";

                    if (sort)
                        sortClause += " score desc, basescore desc, ";
                    sortClause += "_id asc";

                    String selectString = "select _id, code, code3r, word, score, basescore, " + exactMatchCondition
                            + " as exactmatch  ";

                    selectString += " from " + db.tablename + " where word is not null and " + selectClause + " order by "
                            + sortClause
                            + " limit " + limitClause;
                    cursor = LimeDB.db.rawQuery(selectString, null);

                    if (LimeDB.DEBUG)
                        Log.i(LimeDB.TAG, "getMappingByCode() between search select string:" + selectString);
                    /*
                     * }
                     * else{
                     * selectClause = codeCol + " = '" + escapedCode + "' " + extraSelectClause;
                     * if (sort)
                     * sortClause = FIELD_SCORE + " DESC, +" + FIELD_BASESCORE + " DESC, " +
                     * "_id ASC";
                     * else
                     * sortClause = "_id ASC";
                     * cursor = db.query(tablename, null, selectClause, null, null, null,
                     * sortClause, limitClause);
                     * if (DEBUG)
                     * Log.i(TAG, "getMappingByCode(): code = '" + code + "' selectClause=" +
                     * selectClause);
                     *
                     * }
                     */

                    // Jeremy '11,8,5 limit initial getMappingByCode to limited records
                    // Jeremy '11,6,15 Using getMappingByCode with preprocessed code and extra
                    // getMappingByCode conditions.

                    if (cursor != null) {
                        result = buildQueryResult(code, codeorig, cursor);
                    }

                } catch (SQLiteException e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (LimeDB.DEBUG || LimeDB.probePerformance) {
            Log.i(LimeDB.TAG, "getMappingByCode() time elapsed = " + (System.currentTimeMillis() - startTime));
        }

        return result;
    }

    private int countExactMatches(String codeCol, String code, String extraExactMatchClause) {
        try (Cursor c = LimeDB.db.rawQuery("select count(*) from " + db.tablename + " where word is not null and ("
                + codeCol + " = ? " + extraExactMatchClause + ")", new String[]{code})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /*
     * Jeremy '15,5,1 expand the search clause to include cod = abc, ab, c
     * descending
     */
    private String expandBetweenSearchClause(String searchColumn, String code) {

        StringBuilder selectClause = new StringBuilder();

        int len = code.length();
        int end = (len > 5) ? 6 : len;

        if (len > 1) {
            for (int j = 0; j < end - 1; j++) {
                selectClause.append(searchColumn).append("= '")
                        .append(code.substring(0, j + 1).replace("'", "''")).append("' or ");
            }
        }
        // if(fuzzySearch) code = (len>2) ? code.substring(0,2) : code;
        char[] chArray = code.toCharArray();
        chArray[code.length() - 1]++;
        String nextCode = new String(chArray);
        selectClause.append(" (").append(searchColumn).append(" >= '")
                .append(code.replace("'", "''")).append("' and ").append(searchColumn)
                .append(" <'").append(nextCode.replace("'", "''")).append("') ");
        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "expandBetweenSearchClause() selectClause: " + selectClause);
        return selectClause.toString();
    }

    synchronized String preProcessingRemappingCode(String code) {
        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "preProcessingRemappingCode(): tablename = " + db.tablename + " , code=" + code);
        if (code != null) {
            String keyboardtype = db.mLIMEPref.getPhysicalKeyboardType();
            String phonetickeyboardtype = db.mLIMEPref.getPhoneticKeyboardType();
            String keyString = "", keyRemapString = "";
            String newcode = code;
            String remaptable = db.tablename;

            // Build cached hashmap remapping table name
            if (isPhysicalKeyboardPressed) {
                if (db.tablename.equals("phonetic"))
                    remaptable = db.tablename + keyboardtype + phonetickeyboardtype;
                else
                    remaptable = db.tablename + keyboardtype;
            } else if (db.tablename.equals("phonetic"))
                remaptable = db.tablename + phonetickeyboardtype;

            // Build cached hashmap remapping table if it's not exist
            if (keysReMap.get(remaptable) == null
                    || keysReMap.get(remaptable).size() == 0) {

                if (!isPhysicalKeyboardPressed
                        && (db.tablename.startsWith("dayi") || db.tablename.equals("phonetic"))) {
                    keyString = LimeDB.SHIFTED_NUMBERIC_KEY + LimeDB.SHIFTED_SYMBOL_KEY;
                    keyRemapString = LimeDB.SHIFTED_NUMBERIC_KEY_REMAP + LimeDB.SHIFTED_SYMBOL_KEY_REMAP;
                }

                if (LimeDB.DEBUG)
                    Log.i(LimeDB.TAG, "preProcessingRemappingCode(): keyString=\"" + keyString + "\";keyRemapString=\""
                            + keyRemapString + "\"");

                if (!keyString.equals("")) {
                    HashMap<String, String> reMap = new HashMap<>();

                    for (int i = 0; i < keyString.length(); i++) {
                        reMap.put(keyString.substring(i, i + 1), keyRemapString.substring(i, i + 1));
                    }
                    keysReMap.put(remaptable, reMap);
                }
            }

            // Do the remapping here using the cached remapping table

            // if(keysReMap.get(remaptable)==null
            // || keysReMap.get(remaptable).size()==0){
            // return code; //Jeremy '12,5,21 need to do escape. should not return here.
            // }
            // else{
            if (keysReMap.get(remaptable) != null
                    && keysReMap.get(remaptable).size() != 0) {
                HashMap<String, String> reMap = keysReMap.get(remaptable);

                newcode = "";
                String c;

                for (int i = 0; i < code.length(); i++) {
                    String s = code.substring(i, i + 1);
                    c = reMap.get(s);
                    if (c != null)
                        newcode = newcode + c;
                    else
                        newcode = newcode + s;
                }
            }

            // Process the escape characters of getMappingByCode
            // newcode = newcode.replace("'", "''"); // Jeremy '12,7,7 do the code
            // escaped before getMappingByCode.
            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG, "preProcessingRemappingCode():newcode=" + newcode);
            return newcode;
        } else
            return "";
    }

    // Jeremy '12,4,5 add db parameter because db open/closed is handled in
    // searchservice now.
    private Pair<String, String> preProcessingForExtraQueryConditions(String code) {
        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "preProcessingForExtraQueryConditions(): code = '" + code
                    + "', isPhysicalKeyboardPressed=" + isPhysicalKeyboardPressed);

        if (code != null) {
            String keyboardtype = db.mLIMEPref.getPhysicalKeyboardType();
            String phonetickeyboardtype = db.mLIMEPref.getPhoneticKeyboardType();
            String dualcode;
            String dualKey = "";
            String dualKeyRemap = "";
            String remaptable = db.tablename;
            if (isPhysicalKeyboardPressed) {
                if (db.tablename.equals("phonetic"))
                    remaptable = db.tablename + keyboardtype + phonetickeyboardtype;
                else
                    remaptable = db.tablename + keyboardtype;
            } else if (db.tablename.equals("phonetic")) {
                remaptable = db.tablename + phonetickeyboardtype;
            }

            if (keysDualMap.get(remaptable) == null
                    || keysDualMap.get(remaptable).size() == 0) {
                HashMap<String, String> reMap = new HashMap<>();
                if (LimeDB.DEBUG)
                    Log.i(LimeDB.TAG, "preProcessingForExtraQueryConditions(): dualKey=" + dualKey + " dualKeyRemap="
                            + dualKeyRemap);
                for (int i = 0; i < dualKey.length(); i++) {
                    String key = dualKey.substring(i, i + 1);
                    String value = dualKeyRemap.substring(i, i + 1);
                    reMap.put(key, value);
                    reMap.put(value, value);
                }
                keysDualMap.put(remaptable, reMap);
            }
            // do real precessing now
            if (keysDualMap.get(remaptable) == null
                    || keysDualMap.get(remaptable).size() == 0) {
                LimeDB.codeDualMapped = false;
                dualcode = code;
            } else {
                LimeDB.codeDualMapped = true;
                HashMap<String, String> reMap = keysDualMap.get(remaptable);
                dualcode = "";
                // testing if code contains dual mapped characters.
                for (int i = 0; i < code.length(); i++) {
                    String c = reMap.get(code.substring(i, i + 1));
                    if (c != null)
                        dualcode = dualcode + c;
                }
                if (LimeDB.DEBUG)
                    Log.i(LimeDB.TAG, "preProcessingForExtraQueryConditions(): dualcode=" + dualcode);

            }
            // Jeremy '11,8,12 if phonetic has tone symbol in the middle do the
            // expanddualcode
            if (!dualcode.equalsIgnoreCase(code)
                    || !code.equalsIgnoreCase(lastCode) // '11,8,18 Jeremy
                    || (db.tablename.equals("phonetic") && code.matches(".+[ 3467].+"))) {
                return expandDualCode(code, remaptable);
            }
        }
        return null;
    }

    private HashSet<String> buildDualCodeList(String code, String keytablename) {

        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "buildDualCodeList(): code:" + code + ", keytablename=" + keytablename);

        HashMap<String, String> codeDualMap = keysDualMap.get(keytablename);
        HashSet<String> treeDualCodeList = new HashSet<>();

        if (codeDualMap != null && codeDualMap.size() > 0) {

            // Jeremy '12,6,4
            SparseArray<List<String>> treemap = new SparseArray<>();
            for (int i = 0; i < code.length(); i++) {
                if (LimeDB.DEBUG)
                    Log.i(LimeDB.TAG, "buildDualCodeList() level : " + i);

                List<String> levelnMap = new LinkedList<>();
                List<String> lastLevelMap;
                if (i == 0) {
                    lastLevelMap = new LinkedList<>();
                    lastLevelMap.add(code);
                } else
                    lastLevelMap = treemap.get(i - 1);

                String c;
                String n;

                if (lastLevelMap == null || (lastLevelMap.size() == 0)) {
                    if (LimeDB.DEBUG)
                        Log.i(LimeDB.TAG, "buildDualCodeList() level : " + i + " ended because last level map is empty");
                    continue;
                }
                if (LimeDB.DEBUG)
                    Log.i(LimeDB.TAG, "buildDualCodeList() level : " + i + " lastlevelmap size = " + lastLevelMap.size());
                for (String entry : lastLevelMap) {
                    if (LimeDB.DEBUG)
                        Log.i(LimeDB.TAG, "buildDualCodeList() level : " + i + ", entry = " + entry);

                    if (entry.length() == 1)
                        c = entry;
                    else
                        c = entry.substring(i, i + 1);

                    boolean codeMapped = false;
                    do {
                        if (LimeDB.DEBUG)
                            Log.i(LimeDB.TAG, "buildDualCodeList() newCode = '" + entry
                                    + "' blacklistKey = '" + db.cacheKey(entry.substring(0, i + 1) + "%")
                                    + "' blacklistValue = "
                                    + LimeDB.blackListCache.get(db.cacheKey(entry.substring(0, i + 1) + "%")));

                        if (entry.length() == 1 && !levelnMap.contains(entry)) {
                            if (LimeDB.blackListCache.get(db.cacheKey(entry)) == null)
                                treeDualCodeList.add(entry);
                            levelnMap.add(entry);
                            if (LimeDB.DEBUG)
                                Log.i(LimeDB.TAG, "buildDualCodeList() entry.length()==1 new code = '" + entry
                                        + "' added. treeDualCodeList.size = " + treeDualCodeList.size());
                            codeMapped = true;

                        } else if ((entry.length() > 1 && !levelnMap.contains(entry))
                                && LimeDB.blackListCache.get(db.cacheKey(entry.substring(0, i + 1) + "%")) == null) {
                            if (LimeDB.blackListCache.get(db.cacheKey(entry)) == null)
                                treeDualCodeList.add(entry);
                            levelnMap.add(entry);
                            if (LimeDB.DEBUG)
                                Log.i(LimeDB.TAG, "buildDualCodeList() new code = '" + entry
                                        + "' added. treeDualCodeList.size = " + treeDualCodeList.size());
                            codeMapped = true;

                        } else if (codeDualMap.get(c) != null && !codeDualMap.get(c).equals(c)) {
                            n = codeDualMap.get(c);
                            String newCode;

                            if (entry.length() == 1)
                                newCode = n;
                            else if (i == 0)
                                newCode = n + entry.substring(1);
                            else if (i == entry.length() - 1)
                                newCode = entry.substring(0, entry.length() - 1) + n;
                            else
                                newCode = entry.substring(0, i) + n
                                        + entry.substring(i + 1);
                            if (LimeDB.DEBUG)
                                Log.i(LimeDB.TAG, "buildDualCodeList() newCode = '" + newCode
                                        + "' blacklistKey = '" + db.cacheKey(newCode)
                                        + "' blacklistValue = " + LimeDB.blackListCache.get(db.cacheKey(newCode))
                                        + "' blacklistKey = '" + db.cacheKey(newCode.substring(0, i + 1) + "%")
                                        + "' blacklistValue = "
                                        + LimeDB.blackListCache.get(db.cacheKey(newCode.substring(0, i + 1) + "%")));

                            if (newCode.length() == 1 && !levelnMap.contains(newCode)) {
                                if (LimeDB.blackListCache.get(db.cacheKey(newCode)) == null)
                                    treeDualCodeList.add(newCode);
                                levelnMap.add(newCode);
                                if (LimeDB.DEBUG)
                                    Log.i(LimeDB.TAG,
                                            "buildDualCodeList() newCode.length()==1 treeDualCodeList new code = '"
                                                    + newCode
                                                    + "' added. treeDualCodeList.size = " + treeDualCodeList.size());
                                codeMapped = true;
                            } else if ((newCode.length() > 1 && !levelnMap.contains(newCode))
                                    && LimeDB.blackListCache.get(db.cacheKey(newCode.substring(0, i + 1) + "%")) == null) {
                                levelnMap.add(newCode);

                                if (LimeDB.blackListCache.get(db.cacheKey(newCode)) == null)
                                    treeDualCodeList.add(newCode);
                                if (LimeDB.DEBUG)
                                    Log.i(LimeDB.TAG, "buildDualCodeList() treeDualCodeList new code = '" + newCode
                                            + ", c = " + c
                                            + ", n = " + n
                                            + "' added. treeDualCodeList.size = " + treeDualCodeList.size());

                                codeMapped = true;

                            } else if (LimeDB.DEBUG)
                                Log.i(LimeDB.TAG,
                                        "buildDualCodeList()  blacklisted code = '" + newCode.substring(0, i + 1) + "%"
                                                + "'");

                            c = n;
                        } else {
                            if (LimeDB.DEBUG)
                                Log.i(LimeDB.TAG, "buildDualCodeList() level : " + i
                                        + " ended. treeDualCodeList.size = " + treeDualCodeList.size());
                            codeMapped = false;
                        }

                    } while (codeMapped);
                    treemap.put(i, levelnMap);

                }
            }

            // Jeremy '11,8,12 added for continuous typing.
            if (db.tablename.equals("phonetic")) {
                HashSet<String> tempList = new HashSet<>(treeDualCodeList);
                for (String iterator_code : tempList) {
                    if (iterator_code.matches(".+[ 3467].+")) { // regular expression mathes tone in the middle
                        String newCode = iterator_code.replaceAll("[3467 ]", "");
                        // Jeremy '12,6,3 look-up the blacklist cache before add to the list.
                        if (LimeDB.DEBUG)
                            Log.i(LimeDB.TAG, "buildDualCodeList(): processing no tone code :" + newCode);
                        if (newCode.length() > 0
                                && !treeDualCodeList.contains(newCode)
                                && !checkBlackList(db.cacheKey(newCode), false)) {
                            treeDualCodeList.add(newCode);
                            if (LimeDB.DEBUG)
                                Log.i(LimeDB.TAG, "buildDualCodeList(): no tone code added:" + newCode);

                        }
                    }
                }
            }

        }

        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "buildDualCodeList(): treeDualCodeList.size()=" + treeDualCodeList.size());
        return treeDualCodeList;

    }

    /**
     * Jeremy '12,6,4 check black list on code , code + wildcard and reduced code
     * with wildcard
     *
     * @param code blacklist query code
     * @return true if the cod is black listed
     */
    private boolean checkBlackList(String code, Boolean wildCardOnly) {
        Boolean isBlacklisted = false;
        if (code.length() < LimeDB.DUALCODE_NO_CHECK_LIMIT) { // code too short, add anyway
            isBlacklisted = false;
            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG, "buildDualCodeList(): code too short add without check code=" + code);
        } else if (!wildCardOnly && LimeDB.blackListCache.get(db.cacheKey(code)) != null) { // the code is blacklisted
            isBlacklisted = true;
            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG, "buildDualCodeList(): black listed code:" + code);
            /*
             * }else if(blackListCache.get(cacheKey(code+"%")) != null){ //the code with
             * wildcard is blacklisted
             * if(DEBUG)
             * Log.i(TAG, "buildDualCodeList(): check black list code:"+ code
             * +
             * ", blackListCache.get(cacheKey(codeToCheck+%))="+blackListCache.get(cacheKey(
             * code+"%")));
             * isBlacklisted = true;
             * if(DEBUG)
             * Log.i(TAG, "buildDualCodeList(): black listed code:"+ code+"%");
             */
        } else {
            for (int i = LimeDB.DUALCODE_NO_CHECK_LIMIT - 1; i <= code.length(); i++) {
                String codeToCheck = code.substring(0, i) + "%";
                if (LimeDB.blackListCache.get(db.cacheKey(codeToCheck)) != null) {
                    isBlacklisted = true;
                    if (LimeDB.DEBUG)
                        Log.i(LimeDB.TAG, "buildDualCodeList(): black listed code:" + codeToCheck);
                    break;
                }

            }

        }
        return isBlacklisted;
    }

    private Pair<String, String> expandDualCode(String code, String keytablename) {

        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "expandDualCode() code=" + code + ", keytablename = " + keytablename);

        HashSet<String> dualCodeList = buildDualCodeList(code, keytablename);
        String selectClause = "";
        String exactMatchClause = "";
        String validDualCodeList = "";

        if (dualCodeList != null) {
            final boolean NOCheckOnExpand = code.length() < LimeDB.DUALCODE_NO_CHECK_LIMIT;
            final boolean searchNoToneCode = db.tablename.equals("phonetic");

            for (String dualcode : dualCodeList) {
                if (LimeDB.DEBUG)
                    Log.i(LimeDB.TAG, "expandDualCode(): processing dual code = '" + dualcode + "'" + ". result = "
                            + selectClause);

                String noToneCode = dualcode;
                String codeCol = LimeDB.FIELD_CODE;
                String[] col = { codeCol };

                if (db.tablename.equals("phonetic")) {
                    final boolean tonePresent = dualcode.matches(".+[3467 ].*"); // Tone symbols present in any
                                                                                 // locoation except the first character
                    final boolean toneNotLast = dualcode.matches(".+[3467 ].+"); // Tone symbols present in any
                                                                                 // locoation except the first and last
                                                                                 // character

                    if (searchNoToneCode) { // noToneCode (phonetic combination without tones) is present
                        if (tonePresent) {
                            // LD phrase if tone symbols present but not in last character or in last
                            // character but the length > 4 (phonetic combinations never has length >4)
                            if (toneNotLast || (dualcode.length() > 4))
                                noToneCode = dualcode.replaceAll("[3467 ]", "");

                        } else { // no tone symbols present, check noToneCode column
                            codeCol = LimeDB.FIELD_NO_TONE_CODE;
                        }
                    } else if (tonePresent && (toneNotLast || (dualcode.length() > 4))) // LD phrase and no noToneCode
                                                                                        // column present
                        noToneCode = dualcode.replaceAll("[3467 ]", "");
                }
                // do escape code for codes
                String queryCode = dualcode.trim().replace("'", "''");
                String queryNoToneCode = noToneCode.trim().replace("'", "''");

                if (queryCode.length() == 0)
                    continue;

                if (NOCheckOnExpand) {
                    if (!dualcode.equals(code)) {
                        // result = result + " OR " + codeCol + "= '" + queryCode + "'";
                        selectClause += " or (" + expandBetweenSearchClause(codeCol, dualcode) + ") ";
                        exactMatchClause += " or " + codeCol + " ='" + queryCode + "' ";
                    }
                } else {
                    // Jeremy '11,8, 26 move valid code list building to buildqueryresult to avoid
                    // repeat query.
                    Cursor cursor = null;
                    try {
                        String selectValidCodeClause = codeCol + " = '" + queryCode + "'";
                        if (!dualcode.equals(noToneCode)) { // code with tones. should strip tone symbols and add to the
                                                            // select condition.
                            selectValidCodeClause = LimeDB.FIELD_CODE + " = '" + queryCode + "' OR " + LimeDB.FIELD_NO_TONE_CODE
                                    + " = '" + queryNoToneCode + "'";
                        }

                        if (LimeDB.DEBUG)
                            Log.i(LimeDB.TAG, "expandDualCode() selectClause for exactmatch = " + selectValidCodeClause);

                        cursor = LimeDB.db.query(db.tablename, col, selectValidCodeClause, null, null, null, null, "1");
                        if (cursor != null) {
                            if (cursor.moveToFirst()) { // fist entry exist, the code is valid.
                                if (LimeDB.DEBUG)
                                    Log.i(LimeDB.TAG, "expandDualCode()  code = '" + dualcode + "' is valid code");
                                if (validDualCodeList.equals(""))
                                    validDualCodeList = dualcode;
                                else
                                    validDualCodeList = validDualCodeList + "|" + dualcode;
                                if (!dualcode.equals(code)) {
                                    // result = result + " OR " + codeCol + "= '" + queryCode + "'";
                                    selectClause += " or (" + expandBetweenSearchClause(codeCol, dualcode) + ") ";
                                    exactMatchClause += " or (" + codeCol + " ='" + queryCode + "') ";
                                }
                            } else { // the code is not valid, keep it in the black list cache. Jeremy '12,6,3

                                char[] charray = dualcode.toCharArray();
                                charray[queryCode.length() - 1]++;
                                String nextcode = new String(charray);
                                nextcode = nextcode.replace("'", "''");

                                selectValidCodeClause = codeCol + " > '" + queryCode + "' AND " + codeCol + " < '"
                                        + nextcode + "'";

                                if (!dualcode.equals(noToneCode)) { // code with tones. should strip tone symbols and
                                                                    // add to the select condition.
                                    charray = queryNoToneCode.toCharArray();
                                    charray[noToneCode.length() - 1]++;
                                    String nextNoToneCode = new String(charray);
                                    nextNoToneCode = nextNoToneCode.replace("'", "''");
                                    selectValidCodeClause = "(" + codeCol + " > '" + queryCode + "' AND " + codeCol
                                            + " < '" + nextcode + "') "
                                            + "OR (" + codeCol + " > '" + queryNoToneCode + "' AND " + codeCol + " < '"
                                            + nextNoToneCode + "')";

                                }
                                cursor.close();
                                if (LimeDB.DEBUG)
                                    Log.i(LimeDB.TAG, "expandDualCode() dualcode = '" + dualcode + "' noToneCode = '"
                                            + noToneCode + "' selectValidCodeClause for no exact match = "
                                            + selectValidCodeClause);

                                cursor = LimeDB.db.query(db.tablename, col, selectValidCodeClause,
                                        null, null, null, null, "1");

                                if (cursor == null || !cursor.moveToFirst()) { // code* returns no valid records add the
                                                                               // code with wildcard to blacklist
                                    LimeDB.blackListCache.put(db.cacheKey(dualcode + "%"), true);
                                    if (LimeDB.DEBUG)
                                        Log.i(LimeDB.TAG,
                                                " expandDualCode() blackList wildcard code added, code = " + dualcode + "%"
                                                        + ", cachekey = :" + db.cacheKey(dualcode + "%")
                                                        + ", black list size = " + LimeDB.blackListCache.size()
                                                        + ", blackListCache.get() = "
                                                        + LimeDB.blackListCache.get(db.cacheKey(dualcode + "%")));

                                } else { // only add the code to black list
                                    LimeDB.blackListCache.put(db.cacheKey(dualcode), true);
                                    if (LimeDB.DEBUG)
                                        Log.i(LimeDB.TAG, " expandDualCode() blackList code added, code = " + dualcode);
                                }

                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }

                }
            }

            if (validDualCodeList.equals(""))
                lastValidDualCodeList = null;
            else
                lastValidDualCodeList = validDualCodeList;

        }

        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "expandDualCode(): result:" + selectClause + " validDualCodeList:" + validDualCodeList);
        return new Pair<>(selectClause, exactMatchClause);

    }

    /**
     * Process search results
     */
    // 原為 LimeDB 的 synchronized 方法;本方法不做 DB I/O(僅讀取已開啟的 Cursor),
    // 且外層 getMappingByCode 的 rawQuery 原本就在鎖外執行,故不再以 synchronized (db) 包覆
    private List<Mapping> buildQueryResult(String query_code, String codeorig, Cursor cursor) {

        long startTime = 0;
        if (LimeDB.DEBUG || LimeDB.probePerformance) {
            startTime = System.currentTimeMillis();
            Log.i(LimeDB.TAG, "buildQueryResult()");
        }

        List<Mapping> result = new ArrayList<>();

        HashSet<String> duplicateCheck = new HashSet<>();
        HashSet<String> validCodeMap = new HashSet<>(); // Jeremy '11,8,26
        int rsize = 0;
        // jeremy '11,8,30 reset lastVaidDualCodeList first.
        final boolean buildValidCodeList = lastValidDualCodeList == null;

        boolean searchNoToneColumn = db.tablename.equals("phonetic")
                && !query_code.matches(".+[3467 ].*");
        if (LimeDB.DEBUG)
            Log.i(LimeDB.TAG, "buildQueryResutl(): cursor.getCount()=" + cursor.getCount()
                    + ". lastValidDualCodeList = " + lastValidDualCodeList);
        if (cursor.moveToFirst()) {

            int idColumn = cursor.getColumnIndex(LimeDB.FIELD_ID);
            int codeColumn = cursor.getColumnIndex(LimeDB.FIELD_CODE);
            int noToneCodeColumn = cursor.getColumnIndex(LimeDB.FIELD_NO_TONE_CODE); // Jeremy '12,5,31 renamed from noToneCode
                                                                              // Column
            int wordColumn = cursor.getColumnIndex(LimeDB.FIELD_WORD);
            int scoreColumn = cursor.getColumnIndex(LimeDB.FIELD_SCORE);
            int baseScoreColumn = cursor.getColumnIndex(LimeDB.FIELD_BASESCORE);
            int relatedColumn = cursor.getColumnIndex(LimeDB.FIELD_RELATED);
            int exactMatchColumn = cursor.getColumnIndex("exactmatch");
            // HashMap<String, String> relatedMap = new HashMap<>();

            int sLimit = db.mLIMEPref.getSimilarCodeCandidates();
            int sCount = 0;
            if (LimeDB.DEBUG)
                Log.i(LimeDB.TAG, "buildQueryResult(): code=" + query_code + ", similar code limit=" + sLimit);

            do {
                String word = cursor.getString(wordColumn);
                // skip if word is null
                if (word == null || word.trim().equals(""))
                    continue;
                String code = cursor.getString(codeColumn);
                Mapping m = new Mapping();
                m.setCode(code);
                m.setCodeorig(codeorig);
                m.setWord(word);
                m.setId(cursor.getString(idColumn));
                m.setScore(cursor.getInt(scoreColumn));
                m.setBasescore(cursor.getInt(baseScoreColumn));

                // String relatedlist = (betweenSearch)?null: cursor.getString(relatedColumn);

                Boolean exactMatch = cursor.getString(exactMatchColumn).equals("1"); // Jeremy '15,6,3 new exact match
                                                                                     // virtual column built in query
                                                                                     // time.
                // m.setHighLighted((betweenSearch) && !exactMatch);//Jeremy '12,5,30 exact
                // match, not from related list

                // Jeremy 15,6,3 new exact or partial record type
                if (exactMatch)
                    m.setExactMatchToCodeRecord();
                else
                    m.setPartialMatchToCodeRecord();

                // Jeremy '11,8,26 build valid code map
                // jeremy '11,8,30 add limit for valid code words for composing display
                if (buildValidCodeList) {
                    String noToneCode = cursor.getString(noToneCodeColumn);
                    if (searchNoToneColumn && noToneCode != null
                            && noToneCode.trim().length() == query_code.replaceAll("[3467 ]", "").trim().length()
                            && validCodeMap.size() < LimeDB.DUALCODE_COMPOSING_LIMIT)
                        validCodeMap.add(noToneCode);
                    else if (code != null && code.length() == query_code.length())
                        validCodeMap.add(code);
                }



                // related list always null in between search mode. Jeremy
                // '15,6,3----------------
                /*
                 * if ( relatedlist != null && relatedMap.get(code) == null) {
                 * relatedMap.put(code, relatedlist);
                 * if (DEBUG)
                 * Log.i(TAG, "buildQueryResult() build relatedmap on code = '" + code +
                 * "' relatedlist = " + relatedlist);
                 *
                 * }
                 */
                // -----------------------------------------------------------------------------------------------

                if (duplicateCheck.add(m.getWord())) {
                    result.add(m);

                    if (m.isPartialMatchToCodeRecord()) {
                        sCount++;
                        if (sCount > sLimit)
                            break;
                    }
                }
                rsize++;
                if (LimeDB.DEBUG)
                    Log.i(LimeDB.TAG, "buildQueryResult():  current code = " + m.getCode() + ", current word =" + m.getWord()
                            + ", similar code count=" + sCount + ", record counts" + rsize);
            } while (cursor.moveToNext());

            // Jeremy '11,8,26 build valid code map
            if (buildValidCodeList && validCodeMap.size() > 0) {
                for (String validCode : validCodeMap) {
                    if (LimeDB.DEBUG)
                        Log.i(LimeDB.TAG, "buildQueryResult(): buildValidCodeList: valicode=" + validCode);
                    if (lastValidDualCodeList == null)
                        lastValidDualCodeList = validCode;
                    else
                        lastValidDualCodeList = lastValidDualCodeList + "|" + validCode;
                }
            }

        }

        // Add full shaped punctuation symbol to the third place , and .
        if (query_code.length() == 1) {

            if ((query_code.equals(",") || query_code.equals("<")) && duplicateCheck.add("，")) {
                Mapping temp = new Mapping();
                temp.setCode(query_code);
                temp.setWord("，");
                if (result.size() > 3)
                    result.add(3, temp);
                else
                    result.add(temp);
            }
            if ((query_code.equals(".") || query_code.equals(">")) && duplicateCheck.add("。")) {
                Mapping temp = new Mapping();
                temp.setCode(query_code);
                temp.setWord("。");
                if (result.size() > 3)
                    result.add(3, temp);
                else
                    result.add(temp);
            }
        }

        if (LimeDB.DEBUG || LimeDB.probePerformance)
            Log.i(LimeDB.TAG, "buildQueryResult():query_code:" + query_code + " query_code.length:" + query_code.length()
                    + " result.size=" + result.size() + " query size:" + rsize + ", time elapsed = "
                    + (System.currentTimeMillis() - startTime));
        return result;

    }

}
