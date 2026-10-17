package net.toload.main.hd.limedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * 鍵盤資訊查詢：鍵盤代碼用綁定參數，不再串進 SQL（含單引號的代碼也查得到）。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LimeDBKeyboardInfoTest {

    private LimeDB limeDb;

    private static void keyboard(SQLiteDatabase db, String code, String desc) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        cv.put("desc", desc);
        db.insert("keyboard", null, cv);
    }

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        limeDb = new LimeDB(context);
        limeDb.openDBConnection(true);
        SQLiteDatabase db = limeDb.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS keyboard");
        db.execSQL("CREATE TABLE keyboard (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, desc text)");
        keyboard(db, "phonetic", "注音");
        keyboard(db, "o'k", "含引號");
    }

    @Test
    public void findsKeyboardByCode() {
        assertEquals("注音", limeDb.getKeyboardInfo("phonetic", "desc"));
    }

    @Test
    public void unknownOrNullCodeReturnsNull() {
        assertNull(limeDb.getKeyboardInfo("nothere", "desc"));
        assertNull(limeDb.getKeyboardInfo(null, "desc"));
    }

    @Test
    public void codeWithQuoteIsFound() {
        // 舊寫法把代碼串進 SQL，單引號會變成語法錯誤（被吞掉後回傳 null）
        assertEquals("含引號", limeDb.getKeyboardInfo("o'k", "desc"));
    }
}
