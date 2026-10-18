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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
                    int codeLen = code.length();
                    // 組字碼一律用綁定參數；順序同 SQL 裡 ? 出現的順序（先 exactmatch，再 where 條件）
                    java.util.List<String> args = new java.util.ArrayList<>();
                    args.add(code);

                    String limitClause = LimeDB.INITIAL_RESULT_LIMIT;
                    // 候選列已沒有「捲到底載入更多」,精確碼的字要一次全部查出來;否則注音 ㄧˋ
                    // 這類同音字多的碼只看得到前 15 個。精確碼不超過上限時照舊
                    int exactCount = countExactMatches(codeCol, code);
                    if (exactCount > Integer.parseInt(limitClause))
                        limitClause = String.valueOf(exactCount);

                    // Jeremy '15, 6, 1 between search clause without using related column for
                    // better sorting order.
                    selectClause = expandBetweenSearchClause(codeCol, code, args);
                    String exactMatchCondition = " (" + codeCol + " = ?) ";
                    sortClause = "( exactmatch = 1 and ( score > 0 or  basescore >0) and length(word)=1) desc, exactmatch desc,"
                            + " (length(" + codeCol + ") >= " + codeLen + " ) desc, "
                            + "(length(" + codeCol + ") <= " + ((codeLen > 5) ? 5 : codeLen) + " )*length(" + codeCol
                            + ") desc, ";

                    if (sort)
                        sortClause += " score desc, basescore desc, ";
                    sortClause += "_id asc";

                    String selectString = "select _id, code, code3r, word, score, basescore, " + exactMatchCondition
                            + " as exactmatch  ";

                    selectString += " from " + LimeDB.validateTableName(db.tablename) + " where word is not null and " + selectClause + " order by "
                            + sortClause
                            + " limit " + limitClause;
                    cursor = LimeDB.db.rawQuery(selectString, args.toArray(new String[0]));

                    if (LimeDB.DEBUG)
                        Log.i(LimeDB.TAG, "getMappingByCode() between search select string:" + selectString);

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

    private int countExactMatches(String codeCol, String code) {
        try (Cursor c = LimeDB.db.rawQuery("select count(*) from " + LimeDB.validateTableName(db.tablename) + " where word is not null and "
                + codeCol + " = ?", new String[]{code})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /*
     * Jeremy '15,5,1 expand the search clause to include cod = abc, ab, c
     * descending
     */
    /** 前綴與範圍條件；條件裡的碼用 ?，值依序加進 args。 */
    private String expandBetweenSearchClause(String searchColumn, String code, java.util.List<String> args) {

        StringBuilder selectClause = new StringBuilder();

        int len = code.length();
        int end = (len > 5) ? 6 : len;

        if (len > 1) {
            for (int j = 0; j < end - 1; j++) {
                selectClause.append(searchColumn).append("= ? or ");
                args.add(code.substring(0, j + 1));
            }
        }
        // if(fuzzySearch) code = (len>2) ? code.substring(0,2) : code;
        char[] chArray = code.toCharArray();
        chArray[code.length() - 1]++;
        String nextCode = new String(chArray);
        selectClause.append(" (").append(searchColumn).append(" >= ? and ").append(searchColumn)
                .append(" < ?) ");
        args.add(code);
        args.add(nextCode);
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
