package net.toload.main.hd.limedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * user.db 第一次搬遷:只搬使用者資料(score > 0 的字碼列、舊備份表、關聯字、備忘錄),
 * lime.db 一筆不動;搬過不再搬;資料庫鎖定中不搬;半套的檔案下次會重來。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UserDataStoreTest {

    private Context context;
    private LimeDB limeDb;
    private SQLiteDatabase db;

    private void insert(String table, String code, String code3r, String word, int score) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        if (code3r != null) cv.put("code3r", code3r);
        cv.put("word", word);
        cv.put("score", score);
        db.insert(table, null, cv);
    }

    private void related(String pword, String cword, int score) {
        ContentValues cv = new ContentValues();
        cv.put("pword", pword);
        cv.put("cword", cword);
        cv.put("score", score);
        db.insert("related", null, cv);
    }

    private static List<String> rows(SQLiteDatabase d, String sql) {
        List<String> out = new ArrayList<>();
        try (Cursor c = d.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < c.getColumnCount(); i++) sb.append(c.getString(i)).append('|');
                out.add(sb.toString());
            }
        }
        return out;
    }

    private List<String> limeSnapshot() {
        List<String> all = new ArrayList<>();
        for (String t : new String[]{"dayi", "phonetic", "dayi_user", "memo"})
            all.addAll(rows(db, "SELECT * FROM " + t + " ORDER BY _id"));
        all.addAll(rows(db, "SELECT * FROM related WHERE score > 0 ORDER BY _id"));
        return all;
    }

    private SQLiteDatabase openUserDb() {
        File f = context.getDatabasePath(UserDataStore.FILE_NAME);
        return SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        limeDb = new LimeDB(context);
        // 每個測試的資料目錄都是新的:重開共用連線,不沿用前一個測試留下的連線
        limeDb.openDBConnection(true);
        db = limeDb.getWritableDatabase();
        for (String t : new String[]{"dayi", "phonetic", "dayi_user"}) {
            db.execSQL("DROP TABLE IF EXISTS " + t);
            db.execSQL("CREATE TABLE " + t + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                    + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        }
        db.execSQL("DELETE FROM related WHERE score > 0");
        db.execSQL("DELETE FROM memo");
        // 字庫原本的列 score = 0,不算使用者資料
        insert("dayi", "d", null, "日", 0);
        insert("dayi", "d", null, "曰", 3);              // 學過
        insert("dayi", "nh12vdbxe", null, "詹智丞", 1);  // 自己新學的詞
        insert("dayi", "ujn", null, "前", 2);
        insert("dayi", "ujn", null, "前", 5);            // 同一組重複:保留較高分
        insert("phonetic", "u4", "u", "意", 4);
        insert("phonetic", "u4", "u", "義", 0);
        // 舊版重新匯入時留下的備份表:主表沒有的列要收,主表有的取較高分
        insert("dayi_user", "a1", null, "信", 7);
        insert("dayi_user", "d", null, "曰", 9);
        related("日", "本", 2);
        related("我", "們", 1);
        ContentValues memo = new ContentValues();
        memo.put("content", "地址");
        memo.put("pinned", 1);
        memo.put("created_at", 1700000000000L);
        db.insert("memo", null, memo);
        context.getDatabasePath(UserDataStore.FILE_NAME).delete();
    }

    @Test
    public void migratesOnlyUserDataAndLeavesLimeDbUntouched() {
        List<String> before = limeSnapshot();
        UserDataStore.Counts counts = new UserDataStore(context).migrateIfNeeded(limeDb);
        assertNotNull(counts);
        assertEquals(before, limeSnapshot());

        assertEquals(5, counts.mapping);
        assertEquals(2, counts.related);
        assertEquals(1, counts.memo);
        SQLiteDatabase u = openUserDb();
        try {
            List<String> mapping = rows(u, "SELECT im, code, code3r, word, score FROM learned_mapping ORDER BY im, code, word");
            List<String> expected = new ArrayList<>();
            expected.add("dayi|a1|null|信|7|");
            expected.add("dayi|d|null|曰|9|");
            expected.add("dayi|nh12vdbxe|null|詹智丞|1|");
            expected.add("dayi|ujn|null|前|5|");
            expected.add("phonetic|u4|u|意|4|");
            assertEquals(expected, mapping);
            assertEquals(java.util.Arrays.asList("我|們|1|", "日|本|2|"),
                    rows(u, "SELECT pword, cword, score FROM learned_related ORDER BY pword"));
            assertEquals(java.util.Collections.singletonList("地址|1|1700000000000|"),
                    rows(u, "SELECT content, pinned, created_at FROM memo"));
            assertEquals(1, rows(u, "SELECT value FROM meta WHERE key = 'migrated_at'").size());
            assertEquals(UserDataStore.SCHEMA_VERSION, u.getVersion());
        } finally {
            u.close();
        }
        assertTrue(new UserDataStore(context).isMigrated());
    }

    @Test
    public void secondStartSkips() {
        UserDataStore store = new UserDataStore(context);
        assertNotNull(store.migrateIfNeeded(limeDb));
        insert("dayi", "x", null, "新", 3); // 之後才學的,D1-1 不負責同步
        assertNull(store.migrateIfNeeded(limeDb));
        SQLiteDatabase u = openUserDb();
        try {
            assertEquals(0, rows(u, "SELECT * FROM learned_mapping WHERE word = '新'").size());
        } finally {
            u.close();
        }
    }

    @Test
    public void skipsWhileDatabaseOnHold() {
        limeDb.holdDBConnection();
        try {
            assertNull(new UserDataStore(context).migrateIfNeeded(limeDb));
            assertFalse(context.getDatabasePath(UserDataStore.FILE_NAME).exists());
        } finally {
            limeDb.unHoldDBConnection();
        }
    }

    @Test
    public void leftoverTmpAndUnfinishedUserDbAreRedone() throws Exception {
        File target = context.getDatabasePath(UserDataStore.FILE_NAME);
        File tmp = new File(target.getPath() + ".tmp");
        try (FileOutputStream o = new FileOutputStream(tmp)) {
            o.write("garbage".getBytes());
        }
        // 沒有完成標記的 user.db(例如上次改名前就中斷)
        SQLiteDatabase half = SQLiteDatabase.openOrCreateDatabase(target, null);
        half.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)");
        half.close();
        UserDataStore store = new UserDataStore(context);
        assertFalse(store.isMigrated());
        assertNotNull(store.migrateIfNeeded(limeDb));
        assertTrue(store.isMigrated());
        assertFalse(tmp.exists());
    }

    @Test
    public void emptyUserDataStillMarksMigrated() {
        db.execSQL("UPDATE dayi SET score = 0");
        db.execSQL("UPDATE phonetic SET score = 0");
        db.execSQL("DELETE FROM dayi_user");
        db.execSQL("UPDATE related SET score = 0");
        db.execSQL("DELETE FROM memo");
        UserDataStore store = new UserDataStore(context);
        UserDataStore.Counts counts = store.migrateIfNeeded(limeDb);
        assertNotNull(counts);
        assertEquals(0, counts.mapping + counts.related + counts.memo);
        assertTrue(store.isMigrated());
    }
}
