package net.toload.main.hd;

import static org.junit.Assert.assertEquals;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 大易同碼字：智慧選字沒有記錄時，看關聯字表「前一字後面接哪個字」，比目前第一個常接 2 倍以上才提到第一。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DayiRelatedPairTest {

    // ---- pickByNextCharWeights（純邏輯）----

    private static Mapping exact(String w) {
        Mapping m = new Mapping();
        m.setWord(w);
        m.setExactMatchToCodeRecord();
        return m;
    }

    private static Mapping composing(String code) {
        Mapping m = new Mapping();
        m.setWord(code);
        m.setComposingCodeRecord();
        return m;
    }

    private static Map<String, Integer> weights(Object... kv) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    @Test
    public void promotesWhenClearlyMoreCommon() {
        List<Mapping> l = new ArrayList<>();
        l.add(composing("x"));
        l.add(exact("又"));
        l.add(exact("水"));
        assertEquals(2, SearchServer.pickByNextCharWeights(l, weights("水", 50)));
        assertEquals(2, SearchServer.pickByNextCharWeights(l, weights("水", 20, "又", 10)));
    }

    @Test
    public void keepsOrderWhenNotClearlyBetter() {
        List<Mapping> l = new ArrayList<>();
        l.add(exact("又"));
        l.add(exact("水"));
        assertEquals(-1, SearchServer.pickByNextCharWeights(l, weights("水", 19, "又", 10))); // 不到 2 倍
        assertEquals(-1, SearchServer.pickByNextCharWeights(l, weights("又", 30, "水", 20))); // 第一個本來就最多
        assertEquals(-1, SearchServer.pickByNextCharWeights(l, weights("水", 1)));            // 權重太小（至少 2）
        assertEquals(-1, SearchServer.pickByNextCharWeights(l, weights()));
        assertEquals(-1, SearchServer.pickByNextCharWeights(l, weights("木", 99)));           // 不是候選
    }

    @Test
    public void ignoresNonExactRecords() {
        List<Mapping> l = new ArrayList<>();
        l.add(exact("又"));
        Mapping partial = new Mapping();
        partial.setWord("水");
        partial.setPartialMatchToCodeRecord();
        l.add(partial);
        assertEquals(-1, SearchServer.pickByNextCharWeights(l, weights("水", 99)));
    }

    // ---- 經過 SearchServer 的實際查詢 ----

    private Context context;
    private SearchServer searchServer;

    private void insertWord(SQLiteDatabase db, String code, String word, int basescore) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        cv.put("word", word);
        cv.put("score", 0);
        cv.put("basescore", basescore);
        db.insert("dayi", null, cv);
    }

    private void insertRelated(SQLiteDatabase db, String pword, String cword, int basescore, int score) {
        ContentValues cv = new ContentValues();
        cv.put("pword", pword);
        cv.put("cword", cword);
        cv.put("basescore", basescore);
        cv.put("score", score);
        db.insert("related", null, cv);
    }

    private void waitPrefetch() throws Exception {
        Field f = SearchServer.class.getDeclaredField("prefetchRunning");
        f.setAccessible(true);
        AtomicBoolean running = (AtomicBoolean) f.get(null);
        for (int i = 0; i < 200 && running.get(); i++) Thread.sleep(50);
    }

    private String firstWordAfter(String prev, String code) {
        SearchServer.setLastCommittedChar(prev);
        for (Mapping m : searchServer.getMappingByCode(code, true))
            if (!m.isComposingCodeRecord()) return m.getWord();
        return null;
    }

    @Before
    public void setup() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        // SearchServer 的 LimeDB 是 static，會沿用前一個測試的 context 與設定檔；清掉讓這個測試的開關設定生效
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
        insertWord(db, "x", "又", 300);
        insertWord(db, "x", "水", 200);
        insertRelated(db, "停", "水", 50, 0);      // 內建：停水
        insertRelated(db, "喝", "水果", 0, 1);     // 使用者學過一次：喝水果 → 水（1×10，不乘 10 就不到 2）
        insertRelated(db, "我", "又", 5, 0);
        LIMEPreferenceManager pref = new LIMEPreferenceManager(context);
        pref.setParameter("dayi_smart_selection", true);
        pref.setParameter("similiar_enable", true);
        pref.setParameter("candidate_suggestion", true);
        SmartSelectionManager.getInstance(context).clearData();
        SearchServer.clearRelatedCaches();
        searchServer = new SearchServer(context);
        searchServer.setTablename("dayi", true, true);
        waitPrefetch();
        // 預先查詢會用上一個測試留下的前一字，先把它算好的搭配權重清掉
        SearchServer.clearRelatedCaches();
    }

    @Test
    public void relatedPairPicksTheNextChar() {
        assertEquals("水", firstWordAfter("停", "x"));
        assertEquals("水", firstWordAfter("喝", "x"));   // 使用者學過的權重 ×10
        assertEquals("又", firstWordAfter("我", "x"));   // 本來第一個就是常接的
        assertEquals("又", firstWordAfter("的", "x"));   // 沒有搭配資料：照字庫順序
    }

    @Test
    public void smartSelectionStillWins() {
        SmartSelectionManager.getInstance(context).recordSelection("x", "又", "停");
        assertEquals("又", firstWordAfter("停", "x"));
    }

    @Test
    public void respectsSwitches() {
        LIMEPreferenceManager pref = new LIMEPreferenceManager(context);
        pref.setParameter("dayi_smart_selection", false);
        assertEquals("smartOff", "又", firstWordAfter("停", "x"));   // 關掉智慧選字：完全照字庫順序
        pref.setParameter("dayi_smart_selection", true);
        pref.setParameter("similiar_enable", false);
        assertEquals("dictOff", "水", firstWordAfter("停", "x"));   // 「啟用關聯字典」只管顯示，挑字照樣用內建的「停水」
        assertEquals("dictOffUser", "水", firstWordAfter("喝", "x"));
        pref.setParameter("candidate_suggestion", false);
        assertEquals("bothOffDict", "水", firstWordAfter("停", "x"));
        assertEquals("bothOffUser", "又", firstWordAfter("喝", "x"));   // 關掉自建關聯字：使用者學過的不算
        pref.setParameter("similiar_enable", true);
        assertEquals("userOff", "又", firstWordAfter("喝", "x"));
    }
}
