package net.toload.main.hd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.limedb.LimeDB;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

/**
 * 大易連打：純字母 3 碼以上時，只有打的字母是英文字典裡單字的開頭才繼續組成英文（不自動上字）。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EnglishWordPrefixTest {

    private SearchServer searchServer;

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        // SearchServer 的 LimeDB 與快取是 static，清掉以免沿用前一個測試的資料
        Field adapter = SearchServer.class.getDeclaredField("dbadapter");
        adapter.setAccessible(true);
        adapter.set(null, null);
        Field cache = SearchServer.class.getDeclaredField("englishPrefixCache");
        cache.setAccessible(true);
        cache.set(null, null);
        LimeDB limeDb = new LimeDB(context);
        limeDb.openDBConnection(true);
        SQLiteDatabase db = limeDb.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS dictionary");
        db.execSQL("CREATE VIRTUAL TABLE dictionary USING fts3(word)");
        for (String w : new String[] { "book", "sorry", "abed", "the" })
            db.execSQL("INSERT INTO dictionary (word) VALUES (?)", new Object[] { w });
        searchServer = new SearchServer(context);
    }

    @Test
    public void wordPrefixesAreEnglish() {
        assertTrue(searchServer.isEnglishWordPrefix("boo"));
        assertTrue(searchServer.isEnglishWordPrefix("book"));      // 完整單字也算
        assertTrue(searchServer.isEnglishWordPrefix("sorr"));
        assertTrue(searchServer.isEnglishWordPrefix("abe"));
        assertTrue(searchServer.isEnglishWordPrefix("BoO"));       // 大小寫不分
    }

    @Test
    public void dayiCodeRunsAreNotEnglish() {
        assertFalse(searchServer.isEnglishWordPrefix("abev"));     // 今天 abe＋ev
        assertFalse(searchServer.isEnglishWordPrefix("akjx"));     // 停水
        assertFalse(searchServer.isEnglishWordPrefix("books"));    // 字典沒有的字
        assertFalse(searchServer.isEnglishWordPrefix(""));
    }
}
