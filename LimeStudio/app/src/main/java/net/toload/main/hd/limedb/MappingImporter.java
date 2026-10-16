package net.toload.main.hd.limedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.toload.main.hd.Lime;
import net.toload.main.hd.R;
import net.toload.main.hd.data.ChineseSymbol;
import net.toload.main.hd.data.Im;
import net.toload.main.hd.data.ImObj;
import net.toload.main.hd.data.Keyboard;
import net.toload.main.hd.data.KeyboardObj;
import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.data.Related;
import net.toload.main.hd.data.Word;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.LIMEProgressListener;
import net.toload.main.hd.global.LIMEUtilities;

/**
 * 字碼表的匯入（文字檔、.db 字庫、自訂輸入法）與學習資料備份表。
 * 從 LimeDB 原樣搬出，LimeDB 對應的公開方法只轉呼叫這裡（鎖仍在 LimeDB 的 synchronized 上）。
 */
final class MappingImporter {
    private static final boolean DEBUG = LimeDB.DEBUG;
    private static final String TAG = LimeDB.TAG;

    private final LimeDB owner;

    MappingImporter(LimeDB owner) {
        this.owner = owner;
    }

    public boolean importBackupDb(File sourcedbfile, String imtype) {
        if (!owner.checkDBConnection())
            return false;

        String validatedImType = LimeDB.validateTableName(imtype);
        // Reset IM Info
        owner.deleteAll(validatedImType);
        LimeDB.db.execSQL("delete from " + Lime.DB_IM + " where " + Lime.DB_IM_COLUMN_CODE + "=?",
                new Object[] { validatedImType });

        owner.holdDBConnection();

        // Load data from DB File
        LimeDB.db.execSQL("attach database ? as sourceDB", new Object[] { sourcedbfile.getAbsolutePath() });
        LimeDB.db.execSQL("insert into " + validatedImType + " select * from sourceDB." + Lime.DB_TABLE_CUSTOM);
        LimeDB.db.execSQL("update sourceDB." + Lime.DB_IM + " set " + Lime.DB_IM_COLUMN_CODE + "=?",
                new Object[] { validatedImType });
        LimeDB.db.execSQL("insert into " + Lime.DB_IM + " select * from sourceDB." + Lime.DB_IM);
        LimeDB.db.execSQL("detach database sourceDB");
        owner.unHoldDBConnection();
        owner.userStore().absorbIm(validatedImType);
        return true;
    }

    public int importDb(String sourcedbfile, String imtype) {
        if (!owner.checkDBConnection())
            return -1;

        String validatedImType = LimeDB.validateTableName(imtype);
        owner.deleteAll(validatedImType);
        owner.holdDBConnection();
        try {
            LimeDB.db.execSQL("attach database ? as sourceDB", new Object[] { sourcedbfile });

            // Identify source table name - it might match imtype or be a generic 'phonetic' or 'dayi'
            String sourceTable = null;
            String[] possibleSourceTables = {validatedImType, "phonetic", "dayi", Lime.DB_TABLE_CUSTOM};

            for (String table : possibleSourceTables) {
                Cursor cursor = LimeDB.db.rawQuery("SELECT name FROM sourceDB.sqlite_master WHERE type='table' AND name=?",
                        new String[] { table });
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        sourceTable = cursor.getString(0);
                    }
                    cursor.close();
                }
                if (sourceTable != null) break;
            }

            if (sourceTable == null) {
                Log.e(TAG, "importDb: No valid source table found in " + sourcedbfile);
                LimeDB.db.execSQL("detach database sourceDB");
                return -1;
            }

            Log.d(TAG, "importDb: Importing from source table '" + sourceTable + "' to '" + validatedImType + "'");
            LimeDB.db.execSQL("insert into " + validatedImType + " select * from sourceDB." + sourceTable);

            // Update IM info if present in source
            Cursor imCursor = LimeDB.db.rawQuery("SELECT name FROM sourceDB.sqlite_master WHERE type='table' AND name=?",
                    new String[] { Lime.DB_IM });
            if (imCursor != null) {
                if (imCursor.moveToFirst()) {
                    LimeDB.db.execSQL("delete from " + Lime.DB_IM + " where code = ?", new Object[] { validatedImType });
                    // We need to ensure we only insert the relevant IM info if there are multiple
                    LimeDB.db.execSQL("insert into " + Lime.DB_IM + " select * from sourceDB." + Lime.DB_IM + " where code = ?",
                            new Object[] { sourceTable });
                    // Update the code in our DB to match the requested imtype if it was different in source
                    LimeDB.db.execSQL("update " + Lime.DB_IM + " set code = ? where code = ?",
                            new Object[] { validatedImType, sourceTable });
                }
                imCursor.close();
            }

            LimeDB.db.execSQL("detach database sourceDB");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                LimeDB.db.execSQL("detach database sourceDB");
            } catch (Exception ex) {
                // ignore
            }
            return -1;
        } finally {
            owner.unHoldDBConnection();
        }

        owner.userStore().absorbIm(validatedImType);
        return owner.countMapping(validatedImType);
    }

    public int backupUserRecords(final String table) {
        if (!owner.checkDBConnection())
            return -1;
        String backupTableName = table + "_user";

        String selectString = "select * from " + table +
                " where " + LimeDB.FIELD_WORD + " is not null and " +
                LimeDB.FIELD_SCORE + " >0 order by " + LimeDB.FIELD_SCORE + " desc";
        Cursor cursor = LimeDB.db.rawQuery(selectString, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.close();
            try {
                // 不用 DROP TABLE IF EXISTS:表不存在時該語句被判為唯讀並進入 statement cache,
                // 同程序內表存在後再執行會沿用唯讀旗標而被派到 WAL 唯讀連線,導致 drop 失敗
                if (owner.hasTable(backupTableName))
                    LimeDB.db.execSQL("drop table " + backupTableName);
            } catch (Exception e) {
                Log.i(TAG, "Remove the table " + backupTableName);
            }
            LimeDB.db.execSQL("create table " + backupTableName + " as " + selectString);
        }

        return owner.countMapping(backupTableName);
    }

    /**
     * 讀取學習資料備份表(table + "_user")的所有記錄;表名經 LimeDB.validateTableName 驗證。
     */
    public Cursor listBackupRecords(String table) {
        if (!owner.checkDBConnection())
            return null;
        return LimeDB.db.query(LimeDB.validateTableName(table + "_user"), null, null, null, null, null, null);
    }

    /**
     * Jeremy '11,9,8 loadFile() with basescore got from hanconverter
     */
    public void loadFileV2(final String table, final LIMEProgressListener progressListener) {

        if (DEBUG)
            Log.i(TAG, "loadFileV2()");
        // Jeremy '12,5,1 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection()) {
            progressListener.onError(-1, "Database is not avaiable. Please try to do it later");
            return;
        }

        owner.finish = false;
        owner.progressPercentageDone = 0;
        owner.count = 0;
        if (owner.loadingMappingThread != null) {
            // owner.threadAborted = true;
            while (owner.loadingMappingThread.isAlive()) {
                Log.d(TAG, "loadFile():waiting for last loading owner.loadingMappingThread stopped...");
                SystemClock.sleep(1000);
            }
            owner.loadingMappingThread = null;
        }

        owner.loadingMappingThread = new Thread() {

            public void run() {

                String delimiter_symbol = "";

                // Reset Database Table
                // SQLiteDatabase db = getSqliteDb(false);
                if (DEBUG)
                    Log.i(TAG, "loadFileV2 owner.loadingMappingThread starting...");

                try {
                    if (owner.countMapping(table) > 0)
                        LimeDB.db.delete(table, null, null);

                    if (table.equals("phonetic")) {
                        if (DEBUG)
                            Log.i(TAG, "loadfile(), build code3r index.");
                        LimeDB.db.execSQL("CREATE INDEX IF NOT EXISTS phonetic_idx_code3r ON phonetic(code3r)");

                    }
                } catch (Exception e1) {
                    e1.printStackTrace();

                }

                owner.resetImInfo(table);
                boolean isCinFormat = false;

                String imname = "";
                String line;
                String endkey = "";
                String selkey = "";
                String spacestyle = "";
                String imkeys = "";
                String imkeynames = "";

                // Check if source file is .cin format
                if (owner.filename.getName().toLowerCase(Locale.US).endsWith(".cin")) {
                    isCinFormat = true;
                }

                // Base on first 100 line to identify the Delimiter
                try {
                    // Prepare Source File
                    FileReader fr = new FileReader(owner.filename);
                    BufferedReader buf = new BufferedReader(fr);
                    int i = 0;
                    List<String> templist = new ArrayList<>();
                    while ((line = buf.readLine()) != null
                            && !isCinFormat) {
                        templist.add(line);
                        if (i >= 100) {
                            break;
                        } else {
                            i++;
                        }
                    }
                    delimiter_symbol = identifyDelimiter(templist);
                    templist.clear();
                    buf.close();
                    fr.close();
                } catch (Exception ignored) {
                    progressListener.onError(-1, "Source file reading error.");
                }

                // HashSet<String> codeList = new HashSet<>();

                // db = getSqliteDb(false);

                // Jeremy '12,4,10 db will locked after beginTrasaction();
                // owner.mLIMEPref.holdDatabaseCoonection(true);
                // Jeremy '15,5,23 new database on hold mechanism.
                owner.holdDBConnection();
                LimeDB.db.beginTransaction();

                try {
                    // Prepare Source File
                    owner.progressStatus = owner.mContext.getResources().getText(R.string.setup_load_migrate_import).toString();
                    long fileLength = owner.filename.length();
                    long processedLength = 0;
                    FileReader fr = new FileReader(owner.filename);
                    BufferedReader buf = new BufferedReader(fr);
                    boolean firstline = true;
                    boolean inChardefBlock = false;
                    boolean inKeynameBlock = false;
                    // String precode = "";

                    while ((line = buf.readLine()) != null && !owner.threadAborted) {
                        processedLength += line.getBytes().length + 2; // +2 for the eol mark.
                        owner.progressPercentageDone = (int) ((float) processedLength / (float) fileLength * 100);

                        // Log.i(TAG, line + " / " + delimiter_symbol.equals(" ") + " / " +
                        // line.indexOf(delimiter_symbol));
                        // if(DEBUG)
                        // Log.i(TAG, "loadFile():loadFile()"+ owner.progressPercentageDone +"% processed"
                        // + ". processedLength:" + processedLength + ". fileLength:" + fileLength + ",
                        // owner.threadAborted=" + owner.threadAborted);
                        if (owner.progressPercentageDone > 99)
                            owner.progressPercentageDone = 99;

                        if (delimiter_symbol.equals(" ") && line.indexOf(delimiter_symbol) == -1) {
                            continue;
                        }

                        if (delimiter_symbol.equals(" ")) {
                            line = line.replaceAll("     ", " ");
                            line = line.replaceAll("    ", " ");
                            line = line.replaceAll("   ", " ");
                            line = line.replaceAll("  ", " ");
                        }

                        if (line.length() < 3) {
                            continue;
                        }

                        /*
                         * If source is cin format start from the tag %chardef
                         * begin until %chardef end
                         */
                        if (isCinFormat) {
                            if (!(inChardefBlock || inKeynameBlock)) {
                                // Modified by Jeremy '10, 3, 28. Some .cin have
                                // double space between $chardef and begin or
                                // end
                                if (line != null
                                        && line.trim().toLowerCase(Locale.US).startsWith("%chardef")
                                        && line.trim().toLowerCase(Locale.US).endsWith("begin")) {
                                    inChardefBlock = true;
                                }
                                if (line != null
                                        && line.trim().toLowerCase(Locale.US).startsWith("%keyname")
                                        && line.trim().toLowerCase(Locale.US).endsWith("begin")) {
                                    inKeynameBlock = true;
                                }
                                // Add by Jeremy '10, 3 , 27
                                // use %cname as mapping_version of .cin
                                // Jeremy '11,6,5 add selkey, endkey and spacestyle support
                                if (!(line.trim().toLowerCase(Locale.US).startsWith("%cname")
                                        || line.trim().toLowerCase(Locale.US).startsWith("%selkey")
                                        || line.trim().toLowerCase(Locale.US).startsWith("%endkey")
                                        || line.trim().toLowerCase(Locale.US).startsWith("%spacestyle"))) {
                                    continue;
                                }
                            }
                            if (line != null
                                    && line.trim().toLowerCase(Locale.US).startsWith("%keyname")
                                    && line.trim().toLowerCase(Locale.US).endsWith("end")) {
                                inKeynameBlock = false;
                                continue;
                            }
                            if (line != null
                                    && line.trim().toLowerCase(Locale.US).startsWith("%chardef")
                                    && line.trim().toLowerCase(Locale.US).endsWith("end")) {
                                break;
                            }
                        }

                        // Check if file contain BOM MARK at file header
                        if (firstline) {
                            byte[] srcstring = line.getBytes();
                            if (srcstring.length > 3) {
                                if (srcstring[0] == -17 && srcstring[1] == -69
                                        && srcstring[2] == -65) {
                                    byte[] tempstring = new byte[srcstring.length - 3];
                                    // int a = 0;
                                    System.arraycopy(srcstring, 3, tempstring, 0, srcstring.length - 3);
                                    line = new String(tempstring);
                                }
                            }
                            firstline = false;
                        } else if (line == null || line.trim().equals("") || line.length() < 3) {
                            continue;
                        }

                        try {

                            int source_score = 0, source_basescore = 0;
                            String code = null, word = null;
                            if (isCinFormat) {
                                if (line.contains("\t")) {
                                    try {
                                        code = line.split("\t")[0];
                                        word = line.split("\t")[1];
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    try {
                                        // Simply ignore error and try to load score and basescore values
                                        source_score = Integer.parseInt(line.split("\t")[2]);
                                        source_basescore = Integer.parseInt(line.split("\t")[3]);
                                    } catch (Exception ignored) {
                                    }
                                } else if (line.contains(" ")) {
                                    try {
                                        code = line.split(" ")[0];
                                        word = line.split(" ")[1];
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    try {
                                        // Simply ignore error and try to load score and basescore values
                                        source_score = Integer.parseInt(line.split(" ")[2]);
                                        source_basescore = Integer.parseInt(line.split(" ")[3]);
                                    } catch (Exception ignored) {
                                    }
                                }
                            } else {
                                if (delimiter_symbol.equals("|")) {
                                    try {
                                        code = line.split("\\|")[0];
                                        word = line.split("\\|")[1];
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    try {
                                        // Simply ignore error and try to load score and basescore values
                                        source_score = Integer.parseInt(line.split("\\|")[2]);
                                        source_basescore = Integer.parseInt(line.split("\\|")[3]);
                                    } catch (Exception ignored) {
                                    }
                                } else {
                                    try {
                                        code = line.split(delimiter_symbol)[0];
                                        word = line.split(delimiter_symbol)[1];
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    try {
                                        // Simply ignore error and try to load score and basescore values
                                        source_score = Integer.parseInt(line.split(delimiter_symbol)[2]);
                                        source_basescore = Integer.parseInt(line.split(delimiter_symbol)[3]);
                                    } catch (Exception ignored) {
                                    }
                                }

                            }
                            if (code == null || code.trim().equals("")) {
                                continue;
                            } else {
                                code = code.trim();
                            }
                            if (word == null || word.trim().equals("")) {
                                continue;
                            } else {
                                word = word.trim();
                            }
                            if (code.toLowerCase(Locale.US).contains("@version@")) {
                                imname = word.trim();
                                continue;
                            } else if (code.toLowerCase(Locale.US).contains("%cname")) {
                                imname = word.trim();
                                continue;
                            } else if (code.toLowerCase(Locale.US).contains("%selkey")) {
                                selkey = word.trim();
                                if (DEBUG)
                                    Log.i(TAG, "loadfile(): selkey:" + selkey);
                                continue;
                            } else if (code.toLowerCase(Locale.US).contains("%endkey")) {
                                endkey = word.trim();
                                if (DEBUG)
                                    Log.i(TAG, "loadfile(): endkey:" + endkey);
                                continue;
                            } else if (code.toLowerCase(Locale.US).contains("%spacestyle")) {
                                spacestyle = word.trim();
                                continue;
                            } else {
                                code = code.toLowerCase(Locale.US);
                            }

                            if (inKeynameBlock) { // Jeremy '11,6,5 preserve keyname blocks here.
                                imkeys = imkeys + code.toLowerCase(Locale.US).trim();
                                String c = word.trim();
                                if (!c.equals("")) {
                                    if (imkeynames.equals(""))
                                        imkeynames = c;
                                    else
                                        imkeynames = imkeynames + "|" + c;
                                }

                            } else {
                                owner.count++;
                                ContentValues cv = new ContentValues();
                                cv.put(LimeDB.FIELD_CODE, code);

                                if (table.equals("phonetic")) {
                                    cv.put(LimeDB.FIELD_NO_TONE_CODE, code.replaceAll("[3467 ]", ""));
                                }
                                cv.put(LimeDB.FIELD_WORD, word);
                                cv.put(LimeDB.FIELD_SCORE, source_score);
                                if (source_basescore == 0) {
                                    source_basescore = owner.getBaseScore(word);
                                }
                                cv.put(LimeDB.FIELD_BASESCORE, source_basescore);
                                // if(DEBUG) Log.i(TAG, "loadfilev2():code="+code+", word="+word+",
                                // basescore="+basescore);
                                LimeDB.db.insert(table, null, cv);
                            }

                        } catch (StringIndexOutOfBoundsException ignored) {
                        }
                    }

                    buf.close();
                    fr.close();

                    LimeDB.db.setTransactionSuccessful();
                } catch (Exception e) {

                    Log.i(TAG, "Error : " + e);
                    owner.setImInfo(table, "amount", "0");
                    owner.setImInfo(table, "source", "Failed!!!");
                    e.printStackTrace();
                    progressListener.onError(-1, "Table file import failed!");
                } finally {
                    if (DEBUG)
                        Log.i(TAG, "loadfile(): main import loop final section");
                    LimeDB.db.endTransaction();
                    // owner.mLIMEPref.holdDatabaseCoonection(false); // Jeremy '12,4,10 reset
                    // mapping_loading status
                    owner.unHoldDBConnection();

                }
                // 文字檔可能自帶分數:收進 user.db
                owner.userStore().absorbIm(table);

                // TODO: do phrase table learning here.
                // Fill IM information into the IM Table
                if (!owner.threadAborted) {
                    if (!owner.threadAborted)
                        owner.progressPercentageDone = 100;
                    owner.finish = true;

                    owner.setImInfo(table, "source", owner.filename.getName());
                    if (imname == null || imname.isEmpty()) {
                        owner.setImInfo(table, "name", owner.filename.getName());
                    } else {
                        owner.setImInfo(table, "name", imname);
                    }
                    owner.setImInfo(table, "amount", String.valueOf(owner.count));
                    owner.setImInfo(table, "import", new Date().toString()); // Jeremy '12,4,21 toLocaleString() is deprecated

                    if (DEBUG)
                        Log.i("limedb:loadfile()", "Fianlly section: source:"
                                + owner.getImInfo(table, "source") + " amount:" + owner.getImInfo(table, "amount"));

                    // If user download from LIME Default IM SET then fill in related information
                    if (owner.filename.getName().equals("phonetic.lime") || owner.filename.getName().equals("phonetic_adv.lime")) {
                        owner.setImInfo("phonetic", "selkey", "123456789");
                        owner.setImInfo("phonetic", "endkey", "3467'[]\\=<>?:\"{}|~!@#$%^&*()_+");
                        owner.setImInfo("phonetic", "imkeys",
                                ",-./0123456789;abcdefghijklmnopqrstuvwxyz'[]\\=<>?:\"{}|~!@#$%^&*()_+");
                        owner.setImInfo("phonetic", "imkeynames",
                                "ㄝ|ㄦ|ㄡ|ㄥ|ㄢ|ㄅ|ㄉ|ˇ|ˋ|ㄓ|ˊ|˙|ㄚ|ㄞ|ㄤ|ㄇ|ㄖ|ㄏ|ㄎ|ㄍ|ㄑ|ㄕ|ㄘ|ㄛ|ㄨ|ㄜ|ㄠ|ㄩ|ㄙ|ㄟ|ㄣ|ㄆ|ㄐ|ㄋ|ㄔ|ㄧ|ㄒ|ㄊ|ㄌ|ㄗ|ㄈ|、|「|」|＼|＝|，|。|？|：|；|『|』|│|～|！|＠|＃|＄|％|︿|＆|＊|（|）|－|＋");
                    }
                    if (true) {
                        if (!selkey.equals(""))
                            owner.setImInfo(table, "selkey", selkey);
                        if (!endkey.equals(""))
                            owner.setImInfo(table, "endkey", endkey);
                        if (!spacestyle.equals(""))
                            owner.setImInfo(table, "spacestyle", spacestyle);
                        if (!imkeys.equals(""))
                            owner.setImInfo(table, "imkeys", imkeys);
                        if (!imkeynames.equals(""))
                            owner.setImInfo(table, "imkeynames", imkeynames);
                    }
                    if (DEBUG)
                        Log.i(TAG, "loadfilev2():update IM info: imkeys:" + imkeys + " imkeynames:" + imkeynames);

                    // Prepare and Setup the Keyboard of the IM

                    // If there is no keyboard assigned for current input method then use default
                    // keyboard layout
                    // String keyboard = owner.getImInfo(table, "keyboard");
                    // if(keyboard == null || keyboard.equals("")){
                    // owner.setImInfo(table, "keyboard", "lime");
                    // '11,5,23 by Jeremy: Preset keyboard info. by owner.tablename
                    KeyboardObj kobj = owner.getKeyboardObj(table);
                    if (table.equals("phonetic")) {
                        kobj = owner.getKeyboardObj("phonetic");
                    } else if (table.startsWith("dayi")) {
                        kobj = owner.getKeyboardObj("dayi");
                    } else if (kobj == null) { // Jeremy '12,5,21 chose english with number keyboard if the optione is
                                               // on for default keyboard.
                        if (owner.mLIMEPref.getParameterBoolean("number_row_in_english", true)) {
                            kobj = owner.getKeyboardObj("limenum");
                        } else {
                            kobj = owner.getKeyboardObj("lime");
                        }
                    }
                    owner.setIMKeyboard(table, kobj.getDescription(), kobj.getCode());
                }

                // finishing

            }
        };

        Thread reportProgressThread = new Thread() {
            public void run() {

                long interval = progressListener.progressInterval();
                while (owner.loadingMappingThread.isAlive()) {
                    SystemClock.sleep(interval);
                    progressListener.onProgress(owner.progressPercentageDone, 0, owner.progressStatus);
                }
                owner.progressPercentageDone = 100;
                progressListener.onPostExecute(true, null, 0);

            }

        };

        owner.threadAborted = false;
        owner.loadingMappingThread.start();
        reportProgressThread.start();
    }

    /**
     * Identify the delimiter of the source file
     *
     * @param src text format table string
     */
    public String identifyDelimiter(List<String> src) {

        int commaCount = 0;
        int tabCount = 0;
        int pipeCount = 0;
        int spaceCount = 0;

        for (String line : src) {
            if (line.contains("\t")) {
                tabCount++;
            }
            if (line.contains(",")) {
                commaCount++;
            }
            if (line.contains("|")) {
                pipeCount++;
            }
            if (line.contains(" ")) {
                spaceCount++;
            }
        }
        if (commaCount >= tabCount && commaCount >= pipeCount && commaCount >= spaceCount) {
            return ",";
        } else if (tabCount >= commaCount && tabCount >= pipeCount && tabCount >= spaceCount) {
            return "\t";
        } else if (pipeCount >= tabCount && pipeCount >= commaCount && pipeCount >= spaceCount) {
            return "|";
        } else if (spaceCount >= tabCount && spaceCount >= commaCount && spaceCount >= pipeCount) {
            return " ";
        }

        return " ";
    }
}
