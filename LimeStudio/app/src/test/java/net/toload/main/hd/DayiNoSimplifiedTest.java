package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 大易不列簡體字：候選與連打自動上字都去掉簡體字（用 APK 內的簡轉繁表判斷），
 * 「后」這類 Big5 也有的字照常列出。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DayiNoSimplifiedTest {

    private SearchServer searchServer;

    private void insertWord(SQLiteDatabase db, String code, String word, int basescore) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        cv.put("word", word);
        cv.put("score", 0);
        cv.put("basescore", basescore);
        db.insert("dayi", null, cv);
    }

    private void waitPrefetch() throws Exception {
        Field f = SearchServer.class.getDeclaredField("prefetchRunning");
        f.setAccessible(true);
        AtomicBoolean running = (AtomicBoolean) f.get(null);
        for (int i = 0; i < 200 && running.get(); i++) Thread.sleep(50);
    }

    private List<String> words(String code) {
        List<String> out = new ArrayList<>();
        for (Mapping m : searchServer.getMappingByCode(code, true, false))
            if (!m.isComposingCodeRecord()) out.add(m.getWord());
        return out;
    }

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Field adapter = SearchServer.class.getDeclaredField("dbadapter");
        adapter.setAccessible(true);
        adapter.set(null, null);
        LimeDB limeDb = new LimeDB(context);
        limeDb.openDBConnection(true);
        SQLiteDatabase db = limeDb.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS dayi");
        db.execSQL("CREATE TABLE dayi (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        db.execSQL("DELETE FROM related");
        insertWord(db, "1d", "旧", 5000);      // 簡體字排在前面
        insertWord(db, "1d", "舊", 100);
        insertWord(db, "a2,", "伤", 3000);     // 這個碼只有簡體字
        insertWord(db, "1x", "后", 3000);      // Big5 也有的字
        insertWord(db, "1x", "後", 100);
        new LIMEPreferenceManager(context).setParameter("dayi_smart_selection", true);
        SmartSelectionManager.getInstance(context).clearData();
        searchServer = new SearchServer(context);
        searchServer.setTablename("dayi", true, true);
        waitPrefetch();
        SearchServer.clearRelatedCaches();
        SearchServer.setLastCommittedChar(null);
    }

    @Test
    public void candidatesSkipSimplified() {
        List<String> w = words("1d");
        assertFalse(w.toString(), w.contains("旧"));
        assertEquals("舊", w.get(0));
        assertTrue(words("a2,").isEmpty());
    }

    @Test
    public void autoCommitTopSkipsSimplified() {
        assertEquals("舊", searchServer.getTopExactMapping("1d").getWord());
    }

    @Test
    public void keepsBig5CharsThatAlsoServeAsSimplified() {
        assertEquals("后", words("1x").get(0));
        assertEquals("后", searchServer.getTopExactMapping("1x").getWord());
    }
}
