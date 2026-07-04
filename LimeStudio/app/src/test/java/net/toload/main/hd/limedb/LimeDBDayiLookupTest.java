package net.toload.main.hd.limedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.data.Mapping;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * 大易連打新查詢的資料層測試:getTopWordByExactCode / hasCodeOrPrefix。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LimeDBDayiLookupTest {

    private LimeDB limeDb;
    private SQLiteDatabase db;

    private void insertRow(String code, String word, int score, int basescore) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        if (word != null)
            cv.put("word", word);
        cv.put("score", score);
        cv.put("basescore", basescore);
        db.insert("dayi", null, cv);
    }

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        limeDb = new LimeDB(context);
        db = limeDb.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS dayi (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "code text, word text, " +
                "score INTEGER NOT NULL DEFAULT 0, " +
                "basescore INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_dayi_code ON dayi(code)");
        db.delete("dayi", null, null);

        insertRow("nh1", "明", 5, 10);
        insertRow("nh1", "盟", 9, 3);
        insertRow("2vd", "天", 2, 8);
        insertRow("ab", null, 7, 7); // word NULL 列應被排除
    }

    // ===================== getTopWordByExactCode =====================

    @Test
    public void testTopWord_HighestScoreFirst() {
        Mapping m = limeDb.getTopWordByExactCode("dayi", "nh1");
        assertNotNull(m);
        assertEquals("盟", m.getWord()); // score 9 > 5
        assertEquals(9, m.getScore());
    }

    @Test
    public void testTopWord_BasescoreTieBreak() {
        insertRow("xy", "甲", 3, 1);
        insertRow("xy", "乙", 3, 9);
        Mapping m = limeDb.getTopWordByExactCode("dayi", "xy");
        assertNotNull(m);
        assertEquals("乙", m.getWord()); // 同 score,basescore 9 > 1
    }

    @Test
    public void testTopWord_NoRowReturnsNull() {
        assertNull(limeDb.getTopWordByExactCode("dayi", "zzz"));
    }

    @Test
    public void testTopWord_NullWordRowExcluded() {
        assertNull(limeDb.getTopWordByExactCode("dayi", "ab"));
    }

    @Test
    public void testTopWord_InvalidTableRejected() {
        assertNull(limeDb.getTopWordByExactCode("dayi; DROP TABLE dayi", "nh1"));
    }

    // ===================== hasCodeOrPrefix =====================

    @Test
    public void testPrefix_ExactHit() {
        assertTrue(limeDb.hasCodeOrPrefix("dayi", "nh1"));
    }

    @Test
    public void testPrefix_TruePrefixHit() {
        assertTrue(limeDb.hasCodeOrPrefix("dayi", "n"));
        assertTrue(limeDb.hasCodeOrPrefix("dayi", "nh"));
        assertTrue(limeDb.hasCodeOrPrefix("dayi", "2"));
    }

    @Test
    public void testPrefix_NoExtensionReturnsFalse() {
        assertFalse(limeDb.hasCodeOrPrefix("dayi", "nh12"));
        assertFalse(limeDb.hasCodeOrPrefix("dayi", "z"));
    }

    @Test
    public void testPrefix_QuoteCharacterSafe() {
        insertRow("a'b", "丙", 1, 1);
        assertTrue(limeDb.hasCodeOrPrefix("dayi", "a'"));
        assertNotNull(limeDb.getTopWordByExactCode("dayi", "a'b"));
    }
}
