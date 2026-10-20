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

public class LimeDB extends LimeSQLiteOpenHelper {

    public final static String FIELD_ID = "_id";
    public final static String FIELD_CODE = "code";
    public final static String FIELD_WORD = "word";
    public final static String FIELD_RELATED = Lime.DB_RELATED;

    // Jeremy '15, 6, 1 between search clause without using related column for
    // better sorting order.
    public final static String FIELD_SCORE = "score";
    public final static String FIELD_BASESCORE = "basescore"; // jeremy '11,9,8 base frequency got from han converter
                                                              // when table loading.
    public final static String FIELD_NO_TONE_CODE = "code3r";
    public final static String FIELD_DIC_pword = "pword";
    public final static String FIELD_DIC_cword = "cword";
    public final static String FIELD_DIC_score = "score";
    static final boolean DEBUG = false;
    static final String TAG = "LIMEDB";
    private final static int DATABASE_VERSION = 103;
    // Jeremy '11,8,5
    // TODO: should set INITIAL_RESULT_LIMIT according to screen size.
    final static String INITIAL_RESULT_LIMIT = "15";
    final static int COMPOSING_CODE_LENGTH_LIMIT = 16; // Jeremy '12,5,30 changed from 12 to 16 because of
                                                               // improved performance using binary tree.
    final static int DUALCODE_COMPOSING_LIMIT = 16; // Jeremy '12,5,30 changed from 7 to 16 because of improved
                                                            // performance using binary tree.
    // for keyToChar
    final static String DAYI_KEY = "1234567890qwertyuiopasdfghjkl;zxcvbnm,./";
    final static String DAYI_CHAR = "言|牛|目|四|王|門|田|米|足|金|石|山|一|工|糸|火|艸|木|口|耳|人|革|日|土|手|鳥|月|立|女|虫|心|水|鹿|禾|馬|魚|雨|力|舟|竹";
    final static String BPMF_KEY = "1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik,9ol.0p;/-";
    final static String BPMF_CHAR = "ㄅ|ㄆ|ㄇ|ㄈ|ㄉ|ㄊ|ㄋ|ㄌ|ˇ|ㄍ|ㄎ|ㄏ|ˋ|ㄐ|ㄑ|ㄒ|ㄓ|ㄔ|ㄕ|ㄖ|ˊ|ㄗ|ㄘ|ㄙ|˙|ㄧ|ㄨ|ㄩ|ㄚ|ㄛ|ㄜ|ㄝ|ㄞ|ㄟ|ㄠ|ㄡ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ";
    final static String SHIFTED_NUMBERIC_KEY = "!@#$%^&*()";
    final static String SHIFTED_NUMBERIC_KEY_REMAP = "1234567890";
    final static String SHIFTED_SYMBOL_KEY = "<>?_:+\"";
    final static String SHIFTED_SYMBOL_KEY_REMAP = ",./-;='";


    // ==================== Security: Table Name Validation ====================
    // SQL Injection Prevention: Whitelist of valid table names
    // See SECURITY_ANALYSIS.md for detailed security analysis
    private static final java.util.Set<String> VALID_TABLE_NAMES = new java.util.HashSet<>(java.util.Arrays.asList(
        // Main IM tables
        Lime.DB_TABLE_CUSTOM,
        Lime.DB_TABLE_DAYI,
        Lime.DB_TABLE_PHONETIC,
        Lime.IM_PHONETIC_BIG5,
        Lime.IM_PHONETIC_ADV,
        Lime.IM_PHONETIC_ADV_BIG5,
        // System tables
        Lime.DB_IM,
        Lime.DB_RELATED,
        Lime.DB_KEYBOARD,
        "memo",
        // Backup tables (with _user suffix)
        "custom_user", "dayi_user", "phonetic_user"
    ));

    /**
     * Validates table name to prevent SQL injection attacks.
     *
     * Security: This method provides critical SQL injection protection by validating
     * table names against a whitelist before use in SQL queries.
     *
     * @param tableName The table name to validate
     * @return true if the table name is valid and safe to use
     * @throws IllegalArgumentException if the table name is invalid
     */
    private static boolean isValidTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        String cleanedName = tableName.trim().toLowerCase(Locale.ROOT);

        // First check whitelist
        if (VALID_TABLE_NAMES.contains(cleanedName)) {
            return true;
        }

        // Allow pattern: valid_name_user (user backup tables)
        if (cleanedName.endsWith("_user")) {
            String baseName = cleanedName.substring(0, cleanedName.length() - 5);
            if (VALID_TABLE_NAMES.contains(baseName)) {
                return true;
            }
        }

        // Additional pattern validation for safety
        // Allow only alphanumeric and underscores, max 64 chars
        if (!cleanedName.matches("^[a-z][a-z0-9_]{0,63}$")) {
            throw new IllegalArgumentException(
                "Invalid table name format: " + tableName +
                " (must be alphanumeric with underscores, max 64 chars)"
            );
        }

        // If pattern is valid but not in whitelist, log warning but allow
        // This allows for future table additions while maintaining security
        try {
            Log.w(TAG, "Table name not in whitelist but matches pattern: " + tableName);
        } catch (RuntimeException ignored) {
            // Some JVM unit test environments do not mock android.util.Log.
        }
        return true;
    }

    /**
     * Validates and sanitizes table name for use in SQL queries.
     *
     * @param tableName The table name to validate
     * @return The validated table name (trimmed and lowercased)
     * @throws IllegalArgumentException if the table name is invalid
     */
    static String validateTableName(String tableName) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        return tableName.trim().toLowerCase(Locale.ROOT);
    }

    static final boolean probePerformance = false;
    static SQLiteDatabase db = null; // Jeremy '12,5,1 add static modifier. Shared db instance for dbserver and
                                             // searchserver
    // private final static Boolean fuzzySearch = false;
    // hold database connection when database is in maintainable. Jeremy '15,5,23
    private static boolean databaseOnHold = false;
    // 本次鎖定期間是否已提示過「資料庫載入中」,避免每次查詢都跳 Toast
    private static volatile boolean holdNoticeShown = false;
    final LIMEPreferenceManager mLIMEPref;
    final Context mContext;
    // Cache for Related Score
    // Bounded LRU — the IME process is long-lived (foreground service), so an
    // unbounded map would grow monotonically. Evicted entries are recomputed from
    // the USERSCORE column via isRelatedPhraseExistOnDB(), so eviction is lossless.
    final java.util.Map<String, Integer> relatedscore = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, Integer>(Lime.LIMEDB_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, Integer> eldest) {
                    return size() > Lime.LIMEDB_CACHE_SIZE;
                }
            });
    File filename = null;
    String tablename = "custom";
    int count = 0;
    // Jeremy '15,5,23 for new progress listener progress status update
    int progressPercentageDone = 0;
    String progressStatus;
    // private Map<String, String> codeDualMap = new HashMap<String, String>();
    // private int ncount = 0;
    boolean finish = false;
    // private boolean relatedfinish = false;
    // Db loading loadingMappingThread.
    Thread loadingMappingThread = null;
    boolean threadAborted = false;
    // Han and Emoji Databases
    private LimeHanConverter hanConverter;
    private EmojiConverter emojiConverter;
    // 重構二期:打字熱路徑查詢引擎,見 LimeQueryEngine
    private final LimeQueryEngine queryEngine = new LimeQueryEngine(this);
    // 輸入法資訊(im 表)與鍵盤設定(keyboard 表),見 ImInfoStore
    private final ImInfoStore imInfo = new ImInfoStore(this);
    // 關聯字(related 表)的查詢、學習與管理,見 RelatedStore
    private final RelatedStore related = new RelatedStore(this);
    // 字碼表匯入與學習資料備份表,見 MappingImporter
    private final MappingImporter importer = new MappingImporter(this);
    // 學習(選字加分、新增字碼、從 user.db 套回),見 LearningStore
    private final LearningStore learning = new LearningStore(this);

    /*
     * Initialize LIME database, Context and LIMEPreferenceManager
     */
    public LimeDB(Context context) {

        super(context.getApplicationContext(), Lime.DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context.getApplicationContext();

        mLIMEPref = new LIMEPreferenceManager(mContext);

        // Jeremy '12,4,7 open DB connection in constructor
        // 不強制重開:db 是整個程序共用的 static 連線,重開會先關掉它,讓其他執行緒進行中的查詢 crash
        // (換掉資料庫檔案的流程會自己呼叫 openDBConnection(true))
        openDBConnection(false);

    }

    public void setFinish(boolean value) {
        this.finish = value;
    }

    /*
     * For DBService to set the filename to be load to database
     */
    public void setFilename(File filename) {
        this.filename = filename;
    }

    public String getTablename() {
        return this.tablename;
    }

    /**
     * Create SQLite Database and create related tables
     */
    // Jeremy'12,4,7 on OnCreate now. db is always preloaded.
    // @Override
    // public void onCreate(SQLiteDatabase dbin) {
    // Start from 3.0v no need to create internal database
    // }

    /*
     * Update Database Schema
     *
     * @see
     * android.database.sqlite.SQLit eOpenHelper#onUpgrade(android.database.sqlite
     * .SQLiteDatabase, int, int)
     */

    /*
     * For LIMEService to setup tablename for further word mapping query
     */
    public void setTablename(String tablename) {
        this.tablename = tablename;
        if (DEBUG) {
            Log.i(TAG, "settTableName(), tablename:" + tablename + " this.tablename:"
                    + this.tablename);
        }
    }

    /**
     * Jeremy '15,6,6 left only oldVersion <80
     * Do upgrade here if db version is not up to date.
     */
    @Override
    public void onUpgrade(SQLiteDatabase dbin, int oldVersion, int newVersion) {

        Log.i(TAG, "OnUpgrade() db old version = " + oldVersion + ", new version = " + newVersion);

        if (oldVersion < 102) {
            // Add code column index on main IM tables for faster prefix range queries
            String[] imTables = {"phonetic", "custom", "dayi"};
            for (String tbl : imTables) {
                try {
                    execSQL(dbin, "CREATE INDEX IF NOT EXISTS idx_" + tbl + "_code ON " + tbl + " (code)");
                } catch (Exception ignored) {
                }
            }
        }

        if (oldVersion < 103) {
            // Add word column index so addScore() UPDATE ... WHERE word = ? doesn't
            // full-scan the IM table while holding the LimeDB monitor
            String[] imTables = {"phonetic", "custom", "dayi"};
            for (String tbl : imTables) {
                try {
                    execSQL(dbin, "CREATE INDEX IF NOT EXISTS idx_" + tbl + "_word ON " + tbl + " (word)");
                } catch (Exception ignored) {
                }
            }
        }

        if (oldVersion < 101) {
            long startTime = System.currentTimeMillis();
            // create index on related (cword) for better perfomance when making run-time
            // suggestion checking related phrases. Jeremy '15,7,17

            try {
                String CREATE_INDEX = "CREATE INDEX related_idx_cword "
                        + "on " + Lime.DB_RELATED + " (" + Lime.DB_RELATED_COLUMN_CWORD + "); ";

                execSQL(dbin, CREATE_INDEX);
            } catch (Exception ignored) {
            }

            if (oldVersion < 100) {

                Cursor cursor = dbin.query("sqlite_master", null, "type='index' and name = 'phonetic_idx_code3r'",
                        null, null, null, null);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        Log.i(TAG, "OnUpgrade(), NoToneCodeI index is exist!!");
                    } else {
                        Log.i(TAG, "OnUpgrade()  creating phonetic code3r column and index.");
                        execSQL(dbin, "alter table phonetic add column 'code3r'");
                        execSQL(dbin, "create index 'phonetic_idx_code3r' on phonetic (code3r)");
                    }
                    cursor.close();
                }
                cursor = dbin.query("phonetic", null, "code3r='ru'", null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        Log.i(TAG, "OnUpgrade(), NoToneCode column has valid data!!");
                    } else {
                        Log.i(TAG, "OnUpgrade()  update phonetic code3r data from trimmed code.");
                        execSQL(dbin, "update phonetic set code3r=trim(code,'3467')");
                    }
                    cursor.close();
                }
                long endTime = System.currentTimeMillis();
                Log.i(TAG,
                        "OnUpgrade() build phonetic code3r finished.  Elapsed time = " + (endTime - startTime) + "ms.");

                // Update Related table
                if (oldVersion > 78) {
                    try {

                        String BACKUP_OLD_RELATED = "ALTER TABLE " + Lime.DB_RELATED + " RENAME TO " + Lime.DB_RELATED
                                + "_old";
                        execSQL(dbin, BACKUP_OLD_RELATED);

                        String CREATE_NEW_TABLE = "";

                        CREATE_NEW_TABLE += "CREATE TABLE " + Lime.DB_RELATED + " ("
                                + Lime.DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + Lime.DB_RELATED_COLUMN_PWORD + " text, "
                                + Lime.DB_RELATED_COLUMN_CWORD + " text, "
                                + Lime.DB_RELATED_COLUMN_BASESCORE + " INTEGER, "
                                + Lime.DB_RELATED_COLUMN_USERSCORE + " INTEGER DEFAULT 0  NOT NULL)";

                        execSQL(dbin, CREATE_NEW_TABLE);

                        try {
                            String CREATE_INDEX = "";
                            CREATE_INDEX += "CREATE INDEX related_idx_pword "
                                    + "ON " + Lime.DB_RELATED + "(" + Lime.DB_RELATED_COLUMN_PWORD + "); ";

                            execSQL(dbin, CREATE_INDEX);
                        } catch (Exception e) {
                            // ignore index creation error
                        }

                        String MIGRATE_DATA = "";
                        MIGRATE_DATA += "INSERT INTO " + Lime.DB_RELATED + "("
                                + Lime.DB_RELATED_COLUMN_PWORD + ", "
                                + Lime.DB_RELATED_COLUMN_CWORD + ", "
                                + Lime.DB_RELATED_COLUMN_USERSCORE + ","
                                + Lime.DB_RELATED_COLUMN_BASESCORE + ")";
                        MIGRATE_DATA += "SELECT " + Lime.DB_RELATED_COLUMN_PWORD + ", "
                                + Lime.DB_RELATED_COLUMN_CWORD + ", user_score, score  FROM " + Lime.DB_RELATED
                                + "_old";

                        execSQL(dbin, MIGRATE_DATA);

                        String DROP_OLD_TABLE = "DROP TABLE " + Lime.DB_RELATED + "_old";
                        execSQL(dbin, DROP_OLD_TABLE);

                        // Download and restore related DB

                    } catch (SQLiteException e) {
                        e.printStackTrace();
                    }
                } else {
                    String add_column = "ALTER TABLE " + Lime.DB_RELATED + " ADD ";
                    add_column += Lime.DB_RELATED_COLUMN_BASESCORE + " INTEGER";
                    execSQL(dbin, add_column);
                }

            }
        }
    }

    /**
     * Check the consistency of phonetic keyboard setting in preference and db.
     * Jeremy '12,6,8
     */
    public void checkPhoneticKeyboardSetting() {
        if (!checkDBConnection())
            return;
        try {
            checkPhoneticKeyboardSettingOnDB(db);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param dbin sqlite database object
     */
    private void checkPhoneticKeyboardSettingOnDB(SQLiteDatabase dbin) {
        setIMKeyboardOnDB(dbin, "phonetic",
                getKeyboardInfoOnDB(dbin, "phonetic", "desc"), "phonetic");
    }

    /*
     * Calling from onUpgrade with SquliteDataabase object to upgrade.
     */
    private void execSQL(SQLiteDatabase dbin, String command) {

        try {
            dbin.execSQL(command);

        } catch (Exception e) {
            Log.w(TAG, "Ignore all possible exceptions~");
        }
    }

    // Jeremy '12,4,7
    public boolean openDBConnection(boolean force_reload) {
        if (DEBUG) {
            Log.i(TAG, "openDBConnection(), force_reload = " + force_reload);
            if (db != null)
                Log.i(TAG, "db.isOpen()" + db.isOpen());
        }

        if (!force_reload && db != null && db.isOpen()) {
            return true;
        } else {

            // Reset related phrsae score cache
            if (relatedscore != null)
                relatedscore.clear();

            if (force_reload) {
                try {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            db = this.getWritableDatabase();
            databaseOnHold = false;
            return db != null && db.isOpen();
        }
    }

    /**
     * Jeremy '12,5,1 checkDBconnection try to openDBconection if db is not open.
     * Return true if the db connection is valid, return false if dbconnection is
     * not valid
     *
     * @return return true if db connection is ready.
     */
    synchronized boolean checkDBConnection() {
        // Jeremy '12,5,1 mapping loading. db is locked
        if (DEBUG)
            Log.i(TAG, "checkDBConnection()");

        if (databaseOnHold) { // mapping loading in progress, database is not available for query
            if (DEBUG)
                Log.i(TAG, "checkDBConnection() : mapping loading ");
            // 原本以 Looper.prepare()+Looper.loop() 在任意 thread 顯示 Toast,但 loop 永不返回,
            // 呼叫的 thread 會永遠卡住;改為丟到主執行緒顯示(每次鎖定只提示一次)並直接回傳 false
            if (!holdNoticeShown) {
                holdNoticeShown = true;
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext,
                        mContext.getText(R.string.l3_database_loading), Toast.LENGTH_SHORT).show());
            }
            return false;
        } else
            return openDBConnection(false);

    }

    /**
     * Base on given table name to remove records
     */
    public void deleteAll(String table) {

        if (DEBUG)
            Log.i(TAG, "deleteAll()");
        if (loadingMappingThread != null) {
            threadAborted = true;
            while (loadingMappingThread.isAlive()) {
                Log.d(TAG, "deleteAll():waiting for loadingMappingThread stopped...");
                SystemClock.sleep(1000);
            }
        }

        if (countMapping(table) > 0)
            db.delete(table, null, null);

        finish = false;
        resetImInfo(table);
        // mLIMEPref.setParameter("im_loading", false);
        // mLIMEPref.setParameter("im_loading_table", "");

    }

    public synchronized void clearUserLearnedRelated() {
        related.clearUserLearnedRelated();
    }

    // 使用者資料正本 user.db:學習資料寫完 lime.db 後同步過去
    UserDataStore userStore() {
        return UserDataStore.get(mContext);
    }

    /**
     * 供 SearchServer 於自建關聯字變更後清除 score 快取
     */
    public void clearRelatedScoreCache() {
        relatedscore.clear();
    }

    public List<Related> getUserLearnedRelated(String filter, int limit) {
        return related.getUserLearnedRelated(filter, limit);
    }

    public int countUserLearnedRelated() {
        return related.countUserLearnedRelated();
    }

    public synchronized void removeOrResetUserRelated(long id, boolean builtIn) {
        related.removeOrResetUserRelated(id, builtIn);
    }

    public synchronized void restoreUserRelated(Related item) {
        related.restoreUserRelated(item);
    }

    /**
     * Count total amount of specific table
     *
     * @return return 0 if db is not ready, the table is not available or with 0
     *         mapping records.
     */
    public int countMapping(String table) {
        if (DEBUG)
            Log.i(TAG, "countMapping() on table:" + table);

        if (!checkDBConnection())
            return 0;
            
        // Check if table exists first to avoid "no such table" log errors
        if (!hasTable(table)) {
            return 0;
        }

        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + validateTableName(table), null);
            if (cursor == null)
                return 0;
            int total = cursor.getCount();
            if (DEBUG)
                Log.i(TAG, "countMapping" + "Table," + table + ": " + total);
            return total;
        } catch (Exception e) {
            Log.e(TAG, "Error counting mapping: " + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return 0;
    }

    public int getCount() {
        return count;
    }

    /**
     * Return the score after add or updated. Jeremy '12,6,7
     */

    public synchronized int addOrUpdateRelatedPhraseRecord(String pword, String cword) {
        return related.addOrUpdateRelatedPhraseRecord(pword, cword);
    }

    public synchronized void addOrUpdateMappingRecord(String code, String word) {
        addOrUpdateMappingRecord(this.tablename, code, word, -1);
    }

    public synchronized void addOrUpdateMappingRecord(String table, String code, String word, int score) {
        learning.addOrUpdateMappingRecord(table, code, word, score);
    }

    public synchronized void addScore(Mapping srcunit) {
        learning.addScore(srcunit);
    }
    // Add by jeremy '10, 4, 1. For reverse lookup

    /**
     * Jeremy '12,6,7 for phrase learning to get code from word
     */
    public List<Mapping> getMappingByWord(String keyword, String table) {

        if (DEBUG)
            Log.i(TAG, "getMappingByWord():tablename:" + table + "  keyworad:" + keyword);

        if (!checkDBConnection())
            return null;

        List<Mapping> result = new LinkedList<>();

        try {

            if (keyword != null && !keyword.trim().equals("")) {
                Cursor cursor;
                cursor = db.query(table, null, FIELD_WORD + " = ?", new String[] { keyword },
                        null, null, FIELD_SCORE + " DESC", null);
                if (DEBUG)
                    Log.i(TAG, "getMappingByWord():tablename:" + table + "  keyworad:"
                            + keyword + "  cursor.getCount:"
                            + cursor.getCount());

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            int idColumn = cursor.getColumnIndex(FIELD_ID);
                            int codeColumn = cursor.getColumnIndex(FIELD_CODE);
                            int wordColumn = cursor.getColumnIndex(FIELD_WORD);
                            int scoreColumn = cursor.getColumnIndex(FIELD_SCORE);
                            Mapping munit = new Mapping();
                            munit.setId(cursor.getString(idColumn));
                            munit.setCode(cursor.getString(codeColumn));
                            munit.setWord(cursor.getString(wordColumn));
                            munit.setExactMatchToWordRecord();
                            munit.setScore(cursor.getInt(scoreColumn));
                            result.add(munit);

                        } while (cursor.moveToNext());

                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }

        if (DEBUG)
            Log.i(TAG, "getMappingByWord() Result.size() = " + result.size());

        return result;
    }

    /**
     * Reverse lookup on keyword.
     */
    public String getCodeListStringByWord(String keyword) {

        if (!checkDBConnection())
            return null;

        String table = mLIMEPref.getRerverseLookupTable(tablename);

        if (table.equals("none")) {
            return null;
        }

        String result = "";
        try {

            if (keyword != null && !keyword.trim().equals("")) {
                Cursor cursor;
                cursor = db.query(validateTableName(table), null, FIELD_WORD + " = ?", new String[] { keyword },
                        null, null, null, null);
                if (DEBUG)
                    Log.i(TAG, "getRmapping():tablename:" + table + "  keyworad:"
                            + keyword + "  cursor.getCount:"
                            + cursor.getCount());

                if (cursor != null) {

                    if (cursor.moveToFirst()) {
                        int codeColumn = cursor.getColumnIndex(FIELD_CODE);
                        int wordColumn = cursor.getColumnIndex(FIELD_WORD);
                        result = cursor.getString(wordColumn) + "="
                                + keyToKeyname(cursor.getString(codeColumn), table, false);
                        if (DEBUG)
                            Log.i(TAG, "getRmapping():Code:"
                                    + cursor.getString(codeColumn));

                        while (cursor.moveToNext()) {
                            result = result
                                    + "; "
                                    + keyToKeyname(cursor.getString(codeColumn),
                                            table, false);
                            if (DEBUG)
                                Log.i(TAG, "getRmapping():Code:"
                                        + cursor.getString(codeColumn));

                        }
                    }

                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }

        if (DEBUG)
            Log.i(TAG, "getRmapping() Result:" + result);

        return result;
    }

    /*
     * Rewrite by Jeremy 11,6,4. Supporting array and dayi now.
     * Covert composing codes into composing text (reading string).
     *
     */
    public String keyToKeyname(String code, String table, Boolean composingText) {
        // 重構二期:本體移至 LimeQueryEngine
        return queryEngine.keyToKeyname(code, table, composingText);
    }

    /**
     * Retrieve matched records
     */
    public List<Mapping> getMappingByCode(String code, boolean softKeyboard) {
        // 重構二期:本體移至 LimeQueryEngine
        return queryEngine.getMappingByCode(code, softKeyboard);
    }

    public String preProcessingRemappingCode(String code) {
        // 重構二期:本體移至 LimeQueryEngine
        return queryEngine.preProcessingRemappingCode(code);
    }



    /**
     * Jeremy '12,6,3 Build unique cache key for black list cache.
     */

    String cacheKey(String code) {

        return tablename + "_" + code;
    }


    /**
     * @return Cursor for
     *
     *         public Cursor getDictionaryAll() {
     *         //Jeremy '12,5,1 !checkDBConnection() when db is restoring or
     *         replaced.
     *         if (!checkDBConnection()) return null;
     * 
     *         Cursor cursor;
     *         cursor = db.query("dictionary", null, null, null, null, null, null,
     *         null);
     *         return cursor;
     *         }
     */

    /** 這個字後面接各個字的權重（關聯字表，大易連打挑同碼字用），見 RelatedStore.getNextCharWeights */
    public java.util.Map<String, Integer> getRelatedNextCharWeights(String pword) {
        if (!checkDBConnection()) return new java.util.HashMap<>();
        return related.getNextCharWeights(pword);
    }

    public List<Mapping> getRelatedPhrase(String pword) {
        return related.getRelatedPhrase(pword);
    }

    public boolean importBackupRelatedDb(File sourcedbfile) {
        return related.importBackupRelatedDb(sourcedbfile);
    }

    private void restoreLearnedRelated(List<ContentValues> learned) {
        related.restoreLearnedRelated(learned);
    }

    public boolean importBackupDb(File sourcedbfile, String imtype) {
        return importer.importBackupDb(sourcedbfile, imtype);
    }

    public int importDb(String sourcedbfile, String imtype) {
        return importer.importDb(sourcedbfile, imtype);
    }

    public void deleteRelatedPhrase(String pword, String cword) {
        related.deleteRelatedPhrase(pword, cword);
    }

    public int backupUserRecords(final String table) {
        return importer.backupUserRecords(table);
    }

        public boolean checkBackuptable(String table) {
        String backupTableName = table + "_user";
        
        if (!checkDBConnection() || !hasTable(backupTableName)) {
            return false;
        }

        try {
            Cursor cursor = db.rawQuery("select COUNT(*) as total from " + validateTableName(backupTableName), null);

            cursor.moveToFirst();

            int total = cursor.getInt(cursor.getColumnIndexOrThrow("total"));
            cursor.close();
            
            if (total > 0) {
                Log.i("LIME", "Total size :" + total);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized int restoreLearnedFromUserData(String imtype) {
        return learning.restoreLearnedFromUserData(imtype);
    }

    public synchronized void restoreAllFromUserData() {
        learning.restoreAllFromUserData();
    }

    private boolean isMappingExist(String table, String code, String word) {
        return learning.isMappingExist(table, code, word);
    }

    public Cursor listBackupRecords(String table) {
        return importer.listBackupRecords(table);
    }

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName Name of the table to check
     * @return true if the table exists
     */
    public boolean hasTable(String tableName) {
        if (!checkDBConnection()) return false;
        
        Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            new String[]{tableName}
        );
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return exists;
    }

    public synchronized void loadFileV2(final String table, final LIMEProgressListener progressListener) {
        importer.loadFileV2(table, progressListener);
    }

    public String identifyDelimiter(List<String> src) {
        return importer.identifyDelimiter(src);
    }

    /**
     * Check if the specific mapping exists in current table
     */
    Mapping isMappingExistOnDB(SQLiteDatabase db, String table, String code, String word)
            {
        if (DEBUG)
            Log.i(TAG, "isMappingExistOnDB(), code = '" + code + "'");
        Mapping munit = null;
        if (code != null && code.trim().length() > 0) {

            Cursor cursor;
            if (word == null || word.trim().length() == 0) {
                cursor = db.query(table, null, FIELD_CODE + " = ?",
                        new String[]{code}, null, null, null, null);
            } else {
                cursor = db.query(table, null, FIELD_CODE + " = ?" + " AND " + FIELD_WORD + " = ?",
                        new String[]{code, word}, null, null, null, null);
            }
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    munit = new Mapping();
                    int idColumn = cursor.getColumnIndex(FIELD_ID);
                    int codeColumn = cursor.getColumnIndex(FIELD_CODE);
                    int wordColumn = cursor.getColumnIndex(FIELD_WORD);
                    int scoreColumn = cursor.getColumnIndex(FIELD_SCORE);
                    // int relatedColumn = cursor.getColumnIndex(FIELD_RELATED);

                    munit.setId(cursor.getString(idColumn));
                    munit.setCode(cursor.getString(codeColumn));
                    munit.setWord(cursor.getString(wordColumn));
                    munit.setScore(cursor.getInt(scoreColumn));
                    // munit.setHighLighted(cursor.getString(relatedColumn));
                    // munit.setHighLighted(false);
                    munit.setExactMatchToCodeRecord();
                    if (DEBUG)
                        Log.i(TAG, "isMappingExistOnDB(), mapping is exist");
                } else if (DEBUG)
                    Log.i(TAG, "isMappingExistOnDB(), mapping is not exist");

                cursor.close();
            }

        }
        return munit;
    }

    /**
     * 取 exact code 分數最高的字(供大易連打切分/自動上字);無此碼回 null。
     * 參數化查詢,走 idx_&lt;table&gt;_code 索引,可於主執行緒單筆呼叫。
     */
    public Mapping getTopWordByExactCode(String table, String code) {
        List<Mapping> list = getTopWordsByExactCode(table, code, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 取 exact code 分數最高的前 limit 個字(供切分的智慧選字挑字);無此碼回空清單。
     */
    /**
     * 單字字頻總和：每個字取各碼中最大的 basescore 加總，再加字數（每字至少算 1）。
     * 大易連打算「某字出現的機率」用；查不到回 0。
     */
    public long getCharFrequencyTotal(String table) {
        if (!checkDBConnection()) return 0;
        try (Cursor cursor = db.rawQuery("SELECT sum(m) + count(*) FROM (SELECT max(" + FIELD_BASESCORE + ") AS m FROM "
                + validateTableName(table) + " WHERE length(" + FIELD_WORD + ") = 1 GROUP BY " + FIELD_WORD + ")", null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Mapping> getTopWordsByExactCode(String table, String code, int limit) {
        List<Mapping> results = new LinkedList<>();
        if (!checkDBConnection()) return results;
        if (table == null || !table.matches("[a-z0-9_]+")) return results;
        if (code == null || code.isEmpty() || limit <= 0) return results;

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT " + FIELD_WORD + ", " + FIELD_SCORE + ", " + FIELD_BASESCORE + " FROM " + table
                            + " WHERE " + FIELD_CODE + " = ? AND " + FIELD_WORD + " IS NOT NULL"
                            + " ORDER BY " + FIELD_SCORE + " DESC, " + FIELD_BASESCORE + " DESC, "
                            + FIELD_ID + " ASC LIMIT " + limit,
                    new String[]{code});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Mapping m = new Mapping();
                    m.setCode(code);
                    m.setWord(cursor.getString(0));
                    m.setScore(cursor.getInt(1));
                    m.setBasescore(cursor.getInt(2));
                    results.add(m);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * 大易三碼:3 碼 abc 對應的 4 碼單字(碼為 ab?c,即第 1、2 碼加最後一碼);無則空清單。
     * 以碼的範圍查詢走索引。
     */
    public List<Mapping> getDayiThreeCodeWords(String table, String code, int limit) {
        List<Mapping> results = new LinkedList<>();
        if (!checkDBConnection()) return results;
        if (table == null || !table.matches("[a-z0-9_]+")) return results;
        if (code == null || code.length() != 3 || limit <= 0) return results;

        String prefix = code.substring(0, 2);
        String upperBound = prefix.substring(0, 1) + (char) (prefix.charAt(1) + 1);
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT " + FIELD_ID + ", " + FIELD_CODE + ", " + FIELD_WORD + ", " + FIELD_SCORE + ", "
                            + FIELD_BASESCORE + " FROM " + table
                            + " WHERE " + FIELD_CODE + " >= ? AND " + FIELD_CODE + " < ?"
                            + " AND length(" + FIELD_CODE + ") = 4 AND substr(" + FIELD_CODE + ", 4, 1) = ?"
                            + " AND length(" + FIELD_WORD + ") = 1"
                            + " ORDER BY " + FIELD_SCORE + " DESC, " + FIELD_BASESCORE + " DESC, "
                            + FIELD_ID + " ASC LIMIT " + limit,
                    new String[]{prefix, upperBound, code.substring(2)});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Mapping m = new Mapping();
                    m.setId(cursor.getString(0));
                    m.setCode(cursor.getString(1));
                    m.setWord(cursor.getString(2));
                    m.setScore(cursor.getInt(3));
                    m.setBasescore(cursor.getInt(4));
                    m.setPartialMatchToCodeRecord();
                    results.add(m);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * 依碼與詞長取詞(供切分的詞庫優先比對;大易內建詞碼為每字「首碼+末碼」的縮碼)。
     */
    public List<String> getWordsByCodeAndLength(String table, String code, int wordLength, int limit) {
        List<String> results = new LinkedList<>();
        if (!checkDBConnection()) return results;
        if (table == null || !table.matches("[a-z0-9_]+")) return results;
        if (code == null || code.isEmpty() || wordLength <= 0 || limit <= 0) return results;

        try {
            // wordLength 為 int 直接內插:rawQuery 參數以 TEXT 綁定,
            // length(word)=? 會變成整數比文字而永遠不成立
            Cursor cursor = db.rawQuery(
                    "SELECT " + FIELD_WORD + " FROM " + table
                            + " WHERE " + FIELD_CODE + " = ? AND length(" + FIELD_WORD + ") = " + wordLength
                            + " ORDER BY " + FIELD_SCORE + " DESC, " + FIELD_BASESCORE + " DESC, "
                            + FIELD_ID + " ASC LIMIT " + limit,
                    new String[]{code});
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String w = cursor.getString(0);
                    if (w != null) results.add(w);
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * 此碼是否對映到此字(供切分的詞庫比對逐字驗證)。
     */
    public boolean codeMapsToWord(String table, String code, String word) {
        if (!checkDBConnection()) return false;
        if (table == null || !table.matches("[a-z0-9_]+")) return false;
        if (code == null || code.isEmpty() || word == null || word.isEmpty()) return false;

        try {
            Cursor cursor = db.rawQuery(
                    "SELECT 1 FROM " + table
                            + " WHERE " + FIELD_CODE + " = ? AND " + FIELD_WORD + " = ? LIMIT 1",
                    new String[]{code, word});
            if (cursor != null) {
                boolean exists = cursor.moveToFirst();
                cursor.close();
                return exists;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * code 是否為某字的 exact code 或任何碼的前綴(供大易連打模式判斷可否接續)。
     * 以字典序範圍查詢(末字元 +1 為上界)走索引,SELECT 1 ... LIMIT 1。
     */
    public boolean hasCodeOrPrefix(String table, String codePrefix) {
        if (!checkDBConnection()) return false;
        if (table == null || !table.matches("[a-z0-9_]+")) return false;
        if (codePrefix == null || codePrefix.isEmpty()) return false;

        String upperBound = codePrefix.substring(0, codePrefix.length() - 1)
                + (char) (codePrefix.charAt(codePrefix.length() - 1) + 1);
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT 1 FROM " + table
                            + " WHERE " + FIELD_CODE + " >= ? AND " + FIELD_CODE + " < ? LIMIT 1",
                    new String[]{codePrefix, upperBound});
            if (cursor != null) {
                boolean exists = cursor.moveToFirst();
                cursor.close();
                return exists;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Mapping isRelatedPhraseExist(String pword, String cword) {
        return related.isRelatedPhraseExist(pword, cword);
    }

    Mapping isRelatedPhraseExistOnDB(SQLiteDatabase db, String pword, String cword) {
        return related.isRelatedPhraseExistOnDB(db, pword, cword);
    }

    public synchronized void resetImInfo(String im) {
        imInfo.resetImInfo(im);
    }

    public String getImInfo(String im, String field) {
        return imInfo.getImInfo(im, field);
    }

    public synchronized void removeImInfo(String im, String field) {
        imInfo.removeImInfo(im, field);
    }

    private void removeImInfoOnDB(SQLiteDatabase dbin, String im, String field) {
        imInfo.removeImInfoOnDB(dbin, im, field);
    }

    public synchronized void setImInfo(String im, String field, String value) {
        imInfo.setImInfo(im, field, value);
    }

    public List<Im> getImList(String code) {
        return imInfo.getImList(code);
    }

    public List<ImObj> getImList() {
        return imInfo.getImList();
    }

    public KeyboardObj getKeyboardObj(String keyboard) {
        return imInfo.getKeyboardObj(keyboard);
    }

    public String getKeyboardInfo(String keyboardCode, String field) {
        return imInfo.getKeyboardInfo(keyboardCode, field);
    }

    private String getKeyboardInfoOnDB(SQLiteDatabase dbin, String keyboardCode, String field) {
        return imInfo.getKeyboardInfoOnDB(dbin, keyboardCode, field);
    }

    public List<KeyboardObj> getKeyboardList() {
        return imInfo.getKeyboardList();
    }

    public synchronized void setIMKeyboard(String im, String value,
            String keyboard) {
        imInfo.setIMKeyboard(im, value, keyboard);
    }

    private void setIMKeyboardOnDB(SQLiteDatabase dbin, String im, String value, String keyboard) {
        imInfo.setIMKeyboardOnDB(dbin, im, value, keyboard);
    }

    public String getKeyboardCode(String im) {
        return imInfo.getKeyboardCode(im);
    }

    /** 英文字典裡有沒有以 prefix 開頭的單字（prefix 本身是完整單字也算）；資料庫鎖定時回 true（照舊當英文） */
    public boolean hasEnglishWordPrefix(String prefix) {
        if (!checkDBConnection())
            return true;
        // FTS 前綴查詢，prefix 以 ? 綁定
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM dictionary WHERE word MATCH ? LIMIT 1",
                new String[]{prefix + "*"})) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            return true;
        }
    }

    public List<String> getEnglishSuggestions(String word) {

        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return null;

        List<String> result = new ArrayList<>();
        try {
            // String value = "";
            int ssize = mLIMEPref.getSimilarCodeCandidates();
            char[] sourcechars = word.toCharArray();
            // 參數化:word 為使用者輸入,不可串接進 SQL(FTS MATCH 的前綴查詢以 ? 綁定)
            String selectString = "SELECT word FROM dictionary WHERE word MATCH ? AND word <> ?"
                    + " ORDER BY word ASC LIMIT " + ssize + ";";
            Cursor cursor = db.rawQuery(selectString, new String[]{word + "*", word});
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        String w = cursor.getString(cursor.getColumnIndexOrThrow("word"));
                        if (w != null && !w.equals("")) {
                            result.add(w);
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    public List<Mapping> emojiConvert(String source, int emoji) {
        checkEmojiDB();
        return emojiConverter.convert(source, emoji);
    }

    /**
     * Jeremy '11,9,8 get basescore of word store in hanconverter
     */
    public int getBaseScore(String input) {
        checkHanDB();
        return hanConverter.getBaseScore(input);

    }

    // APK 內 res/raw/emoji.db 的資料版本;更新資料時遞增,裝置端會重新複製
    // (產生器見 tools/emoji/gen_emoji.py)
    // v2: 2026-07 以 Unicode 17.0 + CLDR 48 重建 en/tw 表
    // v3: 2026-07 cn 表改以 CLDR zh 重生(原為 LimeIME 舊資料)
    // v4: 2026-10 Unicode 18.0 + CLDR 49 beta3 重建(新增 9 個 emoji)
    private final static int EMOJI_DB_VERSION = 4;

    private void checkEmojiDB() {
        if (emojiConverter == null) {

            File emojiDBFile = mContext.getDatabasePath("emoji.db");
            if (!emojiDBFile.getParentFile().exists()) {
                emojiDBFile.getParentFile().mkdirs();
            }

            if (!emojiDBFile.exists()
                    || mLIMEPref.getParameterInt("emojiDBVersion", 1) != EMOJI_DB_VERSION) {
                if (emojiDBFile.exists())
                    emojiDBFile.delete();
                LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.emoji), emojiDBFile);
                mLIMEPref.setParameter("emojiDBVersion", EMOJI_DB_VERSION);
            }

            emojiConverter = new EmojiConverter(mContext);
        }
    }

    private void checkHanDB() {
        if (hanConverter == null) {

            // Jeremy '11,9,8 update handconverdb to v2 with base score in TCSC table
            File hanDBFile = mContext.getDatabasePath("hanconvert.db");
            if (hanDBFile.exists())
                hanDBFile.delete();
            
            File hanDBV2File = mContext.getDatabasePath("hanconvertv2.db");
            if (!hanDBV2File.getParentFile().exists()) {
                hanDBV2File.getParentFile().mkdirs();
            }

            if (DEBUG)
                Log.i(TAG, "LimeDB: checkHanDB(): hanDBV2Filepaht:" + hanDBV2File.getAbsolutePath());

            if (!hanDBV2File.exists())
                LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.hanconvertv2), hanDBV2File);
            else { // Jeremy '11,9,14 copy the db file if it's newer.
                if (mLIMEPref.getParameterLong("hanDBDate") != hanDBV2File.lastModified())
                    LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.hanconvertv2), hanDBV2File);
            }

            hanConverter = new LimeHanConverter(mContext);
        }
    }

    /**
     * This is the method to rename the table name in database
     */
    public boolean renameTableName(String source, String target) {
        if (!checkDBConnection())
            return false;

        try {
            // ALTER TABLE foo RENAME TO bar (表名經白名單/格式驗證,防注入)
            db.execSQL("ALTER TABLE " + validateTableName(source)
                    + " RENAME TO " + validateTableName(target));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Method from DataSource

    public void beginTransaction() {
        if (db != null && db.isOpen()) {
            db.beginTransaction();
        }
    }

    public void endTransaction() {
        if (db != null && db.isOpen()) {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    /**
     * 取得表格內的所有記錄
     */
    public Cursor list(String table) {
        Cursor cursor = null;
        if (db != null && db.isOpen()) {
            cursor = db.query(table, null, null, null, null, null, null);
        }
        return cursor;
    }

    public List<Keyboard> getKeyboard() {
        return imInfo.getKeyboard();
    }

    public List<Im> getIm(String code, String type) {
        return imInfo.getIm(code, type);
    }

    public Word getWord(String code, long id) {
        if (!checkDBConnection())
            return null;
        Word w;
        Cursor cursor;

        String query = Lime.DB_COLUMN_ID + " = '" + id + "' ";

        cursor = db.query(code,
                null, query,
                null, null, null, null);

        cursor.moveToFirst();
        w = Word.get(cursor);
        cursor.close();
        return w;
    }

    public Keyboard getImKeyboard(String code) {
        return imInfo.getImKeyboard(code);
    }

    public Related getRelated(long id) {
        if (!checkDBConnection())
            return null;
        Related w;
        Cursor cursor;

        String query = Lime.DB_RELATED_COLUMN_ID + " = '" + id + "' ";

        cursor = db.query(Lime.DB_RELATED,
                null, query,
                null, null, null, null);

        cursor.moveToFirst();
        w = Related.get(cursor);
        cursor.close();

        return w;
    }

    public int count(String table) {

        if (!checkDBConnection())
            return 0;
        int total;

        Cursor cursor;
        String query = "SELECT COUNT(*) as count FROM " + table;
        cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        total = cursor.getInt(cursor.getColumnIndexOrThrow(Lime.DB_TOTAL_COUNT));
        cursor.close();

        return total;

    }

    public void insert(String table, ContentValues cv) {
        if (!checkDBConnection())
            return;
        db.insert(table, null, cv);

    }

    // Hold database connection to prevent further transactions when database is in
    // maintenance. Jeremy '15,5,23
    public void holdDBConnection() {
        holdNoticeShown = false;
        databaseOnHold = true;
    }

    public void unHoldDBConnection() {
        databaseOnHold = false;
    }

    public boolean isDatabseOnHold() {
        return databaseOnHold;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS memo (" +
                       "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                       "content TEXT NOT NULL, " +
                       "pinned INTEGER DEFAULT 0, " +
                       "created_at INTEGER);");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create/ensure memo table", e);
        }
    }

    // 備忘錄搬到 user.db 之後只存那裡;還沒搬過才寫 lime.db(之後搬遷會一起複製)
    public long insertMemo(String content, int pinned) {
        Long stored = userStore().insertMemo(this, content, pinned);
        if (stored != null)
            return stored;
        if (!checkDBConnection())
            return -1;
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        cv.put("pinned", pinned);
        cv.put("created_at", System.currentTimeMillis());
        return db.insert("memo", null, cv);
    }

    public List<MemoObj> getMemos() {
        List<MemoObj> stored = userStore().getMemos(this);
        if (stored != null)
            return stored;
        List<MemoObj> list = new ArrayList<>();
        if (!checkDBConnection())
            return list;
        Cursor cursor = null;
        try {
            cursor = db.query("memo", null, null, null, null, null, "pinned DESC, created_at DESC");
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex("_id");
                int contentIdx = cursor.getColumnIndex("content");
                int pinnedIdx = cursor.getColumnIndex("pinned");
                int createdIdx = cursor.getColumnIndex("created_at");
                do {
                    MemoObj obj = new MemoObj();
                    obj.setId(cursor.getInt(idIdx));
                    obj.setContent(cursor.getString(contentIdx));
                    obj.setPinned(cursor.getInt(pinnedIdx));
                    obj.setCreatedAt(cursor.getLong(createdIdx));
                    list.add(obj);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "getMemos error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public int deleteMemo(int id) {
        Integer stored = userStore().deleteMemo(this, id);
        if (stored != null)
            return stored;
        if (!checkDBConnection())
            return -1;
        return db.delete("memo", "_id = ?", new String[]{String.valueOf(id)});
    }

    public int updateMemoPin(int id, int pinned) {
        Integer stored = userStore().updateMemoPin(this, id, pinned);
        if (stored != null)
            return stored;
        if (!checkDBConnection())
            return -1;
        ContentValues cv = new ContentValues();
        cv.put("pinned", pinned);
        return db.update("memo", cv, "_id = ?", new String[]{String.valueOf(id)});
    }

    public int updateMemoContent(int id, String content) {
        Integer stored = userStore().updateMemoContent(this, id, content);
        if (stored != null)
            return stored;
        if (!checkDBConnection())
            return -1;
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        return db.update("memo", cv, "_id = ?", new String[]{String.valueOf(id)});
    }
}
