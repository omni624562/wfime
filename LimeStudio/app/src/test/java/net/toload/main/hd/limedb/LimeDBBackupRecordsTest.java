package net.toload.main.hd.limedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.data.Word;
import net.toload.main.hd.Lime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * listBackupRecords:IM 載入時還原學習資料所讀的備份表(table + "_user")。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LimeDBBackupRecordsTest {

    private LimeDB limeDb;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        limeDb = new LimeDB(context);
        SQLiteDatabase db = limeDb.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS dayi_user");
        db.execSQL("CREATE TABLE dayi_user (" +
                Lime.DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Lime.DB_COLUMN_CODE + " text, " +
                Lime.DB_COLUMN_WORD + " text, " +
                Lime.DB_COLUMN_RELATED + " text, " +
                Lime.DB_COLUMN_SCORE + " INTEGER, " +
                Lime.DB_COLUMN_BASESCORE + " INTEGER)");
        insertBackup(db, "ab", "詹", 3);
        insertBackup(db, "cd", "詠", 5);
    }

    private void insertBackup(SQLiteDatabase db, String code, String word, int score) {
        ContentValues cv = new ContentValues();
        cv.put(Lime.DB_COLUMN_CODE, code);
        cv.put(Lime.DB_COLUMN_WORD, word);
        cv.put(Lime.DB_COLUMN_SCORE, score);
        db.insert("dayi_user", null, cv);
    }

    @Test
    public void listBackupRecords_readsAllRowsOfUserTable() {
        Cursor cursor = limeDb.listBackupRecords("dayi");
        List<Word> list = Word.getList(cursor);

        assertEquals(2, list.size());
        assertEquals("ab", list.get(0).getCode());
        assertEquals("詹", list.get(0).getWord());
        assertEquals(3, list.get(0).getScore());
        assertEquals("cd", list.get(1).getCode());
        assertEquals(5, list.get(1).getScore());
    }

    @Test
    public void listBackupRecords_rejectsInjectedTableName() {
        assertThrows(IllegalArgumentException.class,
                () -> limeDb.listBackupRecords("dayi; DROP TABLE dayi_user --"));
    }

    // backupUserRecords:移除 IM 前把 score>0 的學習資料備份到 table + "_user"

    private SQLiteDatabase createCustomTable() {
        SQLiteDatabase db = limeDb.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS custom");
        db.execSQL("DROP TABLE IF EXISTS custom_user");
        db.execSQL("CREATE TABLE custom (" +
                Lime.DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Lime.DB_COLUMN_CODE + " text, " +
                Lime.DB_COLUMN_WORD + " text, " +
                Lime.DB_COLUMN_RELATED + " text, " +
                Lime.DB_COLUMN_SCORE + " INTEGER, " +
                Lime.DB_COLUMN_BASESCORE + " INTEGER)");
        insertMapping(db, "a", "甲", 2);
        insertMapping(db, "b", "乙", 0);
        insertMapping(db, "c", "丙", 4);
        return db;
    }

    private void insertMapping(SQLiteDatabase db, String code, String word, int score) {
        ContentValues cv = new ContentValues();
        cv.put(Lime.DB_COLUMN_CODE, code);
        cv.put(Lime.DB_COLUMN_WORD, word);
        cv.put(Lime.DB_COLUMN_SCORE, score);
        db.insert("custom", null, cv);
    }

    @Test
    public void backupUserRecords_createsUserTableWhenAbsent() {
        createCustomTable();

        assertEquals(2, limeDb.backupUserRecords("custom"));
        List<Word> list = Word.getList(limeDb.listBackupRecords("custom"));
        assertEquals(2, list.size());
        assertEquals("c", list.get(0).getCode()); // order by score desc
        assertEquals(4, list.get(0).getScore());
        assertEquals("a", list.get(1).getCode());
        assertEquals(2, list.get(1).getScore());
    }

    @Test
    public void backupUserRecords_replacesExistingUserTable() {
        SQLiteDatabase db = createCustomTable();
        limeDb.backupUserRecords("custom");

        // 學習資料改變後再次備份:舊的 custom_user 必須被整張取代
        db.execSQL("UPDATE custom SET " + Lime.DB_COLUMN_SCORE + " = 0 WHERE " + Lime.DB_COLUMN_CODE + " = 'a'");
        db.execSQL("UPDATE custom SET " + Lime.DB_COLUMN_SCORE + " = 6 WHERE " + Lime.DB_COLUMN_CODE + " = 'b'");

        assertEquals(2, limeDb.backupUserRecords("custom"));
        List<Word> list = Word.getList(limeDb.listBackupRecords("custom"));
        assertEquals(2, list.size());
        assertEquals("b", list.get(0).getCode());
        assertEquals(6, list.get(0).getScore());
        assertEquals("c", list.get(1).getCode());
        assertEquals(4, list.get(1).getScore());
    }
}
