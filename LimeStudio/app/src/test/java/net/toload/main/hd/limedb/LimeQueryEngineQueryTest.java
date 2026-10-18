package net.toload.main.hd.limedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 主查詢 getMappingByCode（組字碼用綁定參數）：精確碼排最前，另含較短的前綴碼與延伸碼；
 * 碼含單引號也查得到。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LimeQueryEngineQueryTest {

    private LimeDB limeDb;

    private static void row(SQLiteDatabase db, String code, String word) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        cv.put("word", word);
        db.insert("custom", null, cv);
    }

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        limeDb = new LimeDB(context);
        limeDb.openDBConnection(true);
        SQLiteDatabase db = limeDb.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS custom");
        db.execSQL("CREATE TABLE custom (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        row(db, "a", "甲");
        row(db, "ab", "乙");
        row(db, "abc", "丙");
        row(db, "abd", "丁");
        row(db, "ac", "庚");
        row(db, "b", "戊");
        row(db, "o'k", "引");
        limeDb.setTablename("custom");
    }

    private List<String> words(String code) {
        List<String> out = new ArrayList<>();
        List<Mapping> list = limeDb.getMappingByCode(code, true);
        if (list != null)
            for (Mapping m : list)
                if (!m.isComposingCodeRecord())
                    out.add(m.getWord());
        return out;
    }

    @Test
    public void exactCodeFirstThenPrefixAndExtensions() {
        List<String> w = words("ab");
        assertEquals(w.toString(), "乙", w.get(0));
        assertTrue(w.toString(), w.contains("甲"));      // 較短的前綴碼 a
        assertTrue(w.toString(), w.contains("丙"));      // 延伸碼 abc
        assertTrue(w.toString(), w.contains("丁"));      // 延伸碼 abd
        assertFalse(w.toString(), w.contains("庚"));     // ac 不在 ab 範圍內
        assertFalse(w.toString(), w.contains("戊"));
    }

    @Test
    public void exactMatchFlagComesFromQuery() {
        // 精確碼的字標成 exact match，其他標成 partial match（取自查詢裡的 exactmatch 欄）
        for (Mapping m : limeDb.getMappingByCode("ab", true)) {
            if ("乙".equals(m.getWord()))
                assertTrue(m.isExactMatchToCodeRecord());
            if ("丙".equals(m.getWord()))
                assertTrue(m.isPartialMatchToCodeRecord());
        }
    }

    @Test
    public void singleKeyCode() {
        List<String> w = words("b");
        assertEquals(w.toString(), "戊", w.get(0));
        assertFalse(w.toString(), w.contains("甲"));
    }

    @Test
    public void codeWithQuoteIsFound() {
        assertEquals("[引]", words("o'k").toString());
    }
}
