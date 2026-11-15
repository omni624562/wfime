package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.SmartSelectionManager;
import net.toload.main.hd.limedb.LimeDB;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 連續打幾個字的碼再按空白，整串碼剛好是某個單字的碼（一下 ee9＝征）：
 * 切成常用字比整串的字可能 100 倍以上才把切開的詞排第一；選過那個字或它本身常用就不切。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DayiSplitOverWholeTest {

    private SQLiteDatabase db;
    private SearchServer searchServer;

    private void insertWord(String code, String word, int score, int basescore) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        cv.put("word", word);
        cv.put("score", score);
        cv.put("basescore", basescore);
        db.insert("dayi", null, cv);
    }

    private void waitPrefetch() throws Exception {
        Field f = SearchServer.class.getDeclaredField("prefetchRunning");
        f.setAccessible(true);
        AtomicBoolean running = (AtomicBoolean) f.get(null);
        for (int i = 0; i < 200 && running.get(); i++) Thread.sleep(50);
    }

    private void start() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        new LIMEPreferenceManager(context).setParameter("dayi_smart_selection", true);
        SmartSelectionManager.getInstance(context).clearData();
        searchServer = new SearchServer(context);
        searchServer.setTablename("dayi", true, true);
        waitPrefetch();
        SearchServer.clearRelatedCaches();
        SearchServer.setLastCommittedChar(null);
    }

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Field adapter = SearchServer.class.getDeclaredField("dbadapter");
        adapter.setAccessible(true);
        adapter.set(null, null);
        LimeDB limeDb = new LimeDB(context);
        limeDb.openDBConnection(true);
        db = limeDb.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS dayi");
        db.execSQL("CREATE TABLE dayi (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        insertWord("e", "一", 0, 200000);
        insertWord("e9", "下", 0, 80000);
        insertWord("v", "大", 0, 90000);
        insertWord("vm,", "稼", 0, 100);
    }

    @Test
    public void rareWholeCharLosesToCommonSplit() throws Exception {
        insertWord("ee9", "征", 0, 50);
        start();
        Mapping m = searchServer.getSplitOverWholeMapping("ee9");
        assertEquals("一下", m.getWord());
        assertEquals("ee9", m.getCode());
        assertTrue(m.isSegmentedPhraseRecord());
    }

    @Test
    public void keepsCharUserPickedBefore() throws Exception {
        insertWord("ee9", "征", 1, 50);       // 使用者在這個碼選過「征」
        start();
        assertNull(searchServer.getSplitOverWholeMapping("ee9"));
    }

    @Test
    public void keepsCommonWholeChar() throws Exception {
        insertWord("ee9", "征", 0, 60000);    // 本身就是常用字：切開不會明顯更可能
        start();
        assertNull(searchServer.getSplitOverWholeMapping("ee9"));
    }

    @Test
    public void noSplitWithoutCommonSegments() throws Exception {
        insertWord("ee9", "征", 0, 50);
        start();
        assertNull(searchServer.getSplitOverWholeMapping("vm,"));   // m, 沒有字，切不成常用字
        assertNull(searchServer.getSplitOverWholeMapping("e"));     // 一碼不切
    }
}
