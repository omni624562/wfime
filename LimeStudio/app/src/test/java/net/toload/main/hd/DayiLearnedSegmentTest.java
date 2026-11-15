package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.SmartSelectionManager;
import net.toload.main.hd.limedb.LimeDB;
import net.toload.main.hd.limedb.UserDataStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 純字母碼串（原碼排第一）：選過切分的中文，同碼串再打時中文排第一（不用重開輸入法）；
 * 之後又選原碼，就忘掉學過的切分、原碼排回第一。字庫內建的詞與字母數字混合的碼不受影響。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DayiLearnedSegmentTest {

    private Context context;
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

    // 學習／忘記在背景執行緒做，排一個空工作等它們做完
    private void waitBackground() throws Exception {
        Field f = SearchServer.class.getDeclaredField("backgroundExecutor");
        f.setAccessible(true);
        ((ExecutorService) f.get(null)).submit(() -> { }).get();
    }

    /** 按空白會上的字：候選列第一個不是原碼的字；只有原碼回 null */
    private String spacePick(String code) {
        for (Mapping m : searchServer.getMappingByCode(code, true, false))
            if (!m.isComposingCodeRecord()) return m.getWord();
        return null;
    }

    private int count(SQLiteDatabase d, String sql, String... args) {
        try (Cursor c = d.rawQuery(sql, args)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    private int userDbCount(String code) {
        SQLiteDatabase u = SQLiteDatabase.openDatabase(context.getDatabasePath(UserDataStore.FILE_NAME).getPath(),
                null, SQLiteDatabase.OPEN_READONLY);
        try {
            return count(u, "SELECT count(*) FROM learned_mapping WHERE im = 'dayi' AND code = ?", code);
        } finally {
            u.close();
        }
    }

    @Before
    public void setup() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        Field adapter = SearchServer.class.getDeclaredField("dbadapter");
        adapter.setAccessible(true);
        adapter.set(null, null);
        LimeDB limeDb = new LimeDB(context);
        limeDb.openDBConnection(true);
        db = limeDb.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS dayi");
        db.execSQL("CREATE TABLE dayi (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        insertWord("ab", "今", 0, 3000);
        insertWord("ev", "天", 0, 5000);
        insertWord("abcd", "測試", 0, 100);   // 字庫內建的詞（有字頻）
        insertWord("1d", "舊", 0, 100);
        insertWord("v5", "我", 0, 9000);
        insertWord("a6", "們", 0, 4000);
        insertWord("va", "我們", 2, 0);      // 用縮碼（首碼＋末碼）學來的詞
        context.getDatabasePath(UserDataStore.FILE_NAME).delete();
        limeDb.setTablename("dayi");
        assertNotNull(UserDataStore.get(context).migrateIfNeeded(limeDb));
        new LIMEPreferenceManager(context).setParameter("dayi_smart_selection", true);
        SmartSelectionManager.getInstance(context).clearData();
        searchServer = new SearchServer(context);
        searchServer.setTablename("dayi", true, true);
        waitPrefetch();
        SearchServer.clearRelatedCaches();
        SearchServer.setLastCommittedChar(null);
    }

    @After
    public void tearDown() {
        UserDataStore.resetSharedForTest();
    }

    @Test
    public void learnedSegmentComesFirstRightAway() throws Exception {
        assertNull(spacePick("abev"));                       // 還沒學：只有原碼（切分候選另外排在原碼後面）
        assertEquals("今天", searchServer.getSegmentedPhraseMappings("abev").get(0).getWord());
        searchServer.learnSegmentedPhrase("abev", "今天");
        waitBackground();
        assertEquals("今天", spacePick("abev"));             // 同一次使用中再打就是中文，不必重開輸入法
        assertEquals(1, userDbCount("abev"));
    }

    @Test
    public void pickingRawCodeForgetsLearnedSegment() throws Exception {
        searchServer.learnSegmentedPhrase("abev", "今天");
        waitBackground();
        assertEquals("今天", spacePick("abev"));
        searchServer.forgetSegmentedPhrase("abev");
        waitBackground();
        assertNull(spacePick("abev"));                       // 原碼又排第一
        assertEquals(0, count(db, "SELECT count(*) FROM dayi WHERE code = 'abev'"));
        assertEquals(0, userDbCount("abev"));
    }

    @Test
    public void forgetKeepsDictionaryWordsAndMixedCodes() throws Exception {
        searchServer.forgetSegmentedPhrase("abcd");          // 字庫內建的詞不刪
        waitBackground();
        assertEquals("測試", spacePick("abcd"));
        searchServer.learnSegmentedPhrase("1dab", "舊今");
        waitBackground();
        searchServer.forgetSegmentedPhrase("1dab");          // 字母數字混合的碼本來中文就排第一，不處理
        waitBackground();
        assertEquals("舊今", spacePick("1dab"));
        searchServer.forgetSegmentedPhrase("va");            // 縮碼學來的詞切不成「我們」，不是學過的切分，不刪
        waitBackground();
        assertEquals("我們", spacePick("va"));
    }
}
