package net.toload.main.hd;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

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
 * 大易連打自動上字：整串碼沒有字或首選是罕用字時，前段上成常用字、尾巴留給下一個字（或整串切開）。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DayiAutoCommitSplitTest {

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
        insertWord(db, "akj", "停", 2460);
        insertWord(db, "akjx", "儱", 0);       // 罕用字：停＋水連打成 akjx
        insertWord(db, "x", "水", 8953);
        insertWord(db, "ab", "今", 6807);
        insertWord(db, "ev", "天", 27164);
        insertWord(db, "e", "一", 120245);
        insertWord(db, "abex", "罕", 0);       // 讓 abe 是某個碼的開頭
        insertWord(db, "nb", "通", 5000);
        insertWord(db, "m0", "週", 3000);
        insertWord(db, "ei", "末", 2000);
        // 英文與數字的例子：book＋s 的尾巴 k＋s、2026＋a 的尾巴 6＋a 都接得上
        insertWord(db, "b", "目", 500);
        insertWord(db, "o", "口", 500);
        insertWord(db, "ks", "辛", 1500);
        insertWord(db, "2", "丁", 500);
        insertWord(db, "0", "乙", 500);
        insertWord(db, "6a", "某", 500);
        // 碼接成常用字的例子：e＋w 接成 ew 的「徒」（字頻 20，剛好算常用），一、起都很常用
        insertWord(db, "w", "起", 100000);
        insertWord(db, "ew", "徒", 20);
        insertWord(db, "eq", "好", 2000);
        insertWord(db, "q", "去", 100000);
        insertWord(db, "wx", "來", 50000);
        // 選過切分候選學到的詞：碼是整串、字頻 0
        insertWord(db, "ewx", "一起來", 0);
        db.execSQL("DROP TABLE IF EXISTS dictionary");
        db.execSQL("CREATE VIRTUAL TABLE dictionary USING fts3(word)");
        db.execSQL("INSERT INTO dictionary (word) VALUES ('book')");
        LIMEPreferenceManager pref = new LIMEPreferenceManager(context);
        pref.setParameter("dayi_smart_selection", true);
        SmartSelectionManager.getInstance(context).clearData();
        searchServer = new SearchServer(context);
        searchServer.setTablename("dayi", true, true);
        waitPrefetch();
        SearchServer.clearRelatedCaches();
        SearchServer.setLastCommittedChar(null);
    }

    @Test
    public void carriesTailToNextChar() {
        // 今(ab)＋天(ev)：打到 abe 再按 v 接不上 → 上「今」、留 e（e＋v＝天）
        assertArrayEquals(new String[] { "今", "e" }, searchServer.splitForAutoCommit("abe", 'v'));
        // 週(m0)＋末(ei)
        assertArrayEquals(new String[] { "週", "e" }, searchServer.splitForAutoCommit("m0e", 'i'));
    }

    @Test
    public void splitsWholeWhenNoTailFits() {
        // 停水＋通：akjx 的首選「儱」是罕用字，尾巴都接不上 n → 整串切成「停水」
        assertArrayEquals(new String[] { "停水", "" }, searchServer.splitForAutoCommit("akjx", 'n'));
    }

    @Test
    public void keepsCurrentBehaviorOtherwise() {
        assertNull(searchServer.splitForAutoCommit("akj", 'n'));    // 首選是常用字：照舊
        assertNull(searchServer.splitForAutoCommit("qqqq", 'n'));   // 純字母又沒有字：照舊（英文）
        assertNull(searchServer.splitForAutoCommit("bob", 'z'));    // 純字母沒有字、尾巴接不上：不整串切成「目口目」
        assertNull(searchServer.splitForAutoCommit("x", 'n'));      // 只有一碼
    }

    @Test
    public void splitsCommonMergeOnlyWhenClearlyMoreLikely() {
        // 一＋起 比「徒」可能得多（字頻比遠超過 100 倍），尾巴 w＋z 接不上 → 整串切開
        assertArrayEquals(new String[] { "一起", "" }, searchServer.splitForAutoCommit("ew", 'z'));
        // 一＋去 比「好」可能，但差不到 100 倍 → 照舊上「好」
        assertNull(searchServer.splitForAutoCommit("eq", 'z'));
        // 尾巴 w＋x 是完整的碼（來）：上「一」、留 w
        assertArrayEquals(new String[] { "一", "w" }, searchServer.splitForAutoCommit("ew", 'x'));
    }

    @Test
    public void keepsLearnedPhrase() {
        // 整串碼是學過的詞（字頻 0）：不當罕用字切成「一來」
        assertNull(searchServer.splitForAutoCommit("ewx", 'z'));
    }

    @Test
    public void keepsEnglishWordsAndNumbers() {
        // 已經當英文單字在打（4 個字母以上、是字典裡單字的開頭）：不切成「目口口」＋k
        assertNull(searchServer.splitForAutoCommit("book", 's'));
        // 4 位以上的數字串：不切成「丁乙丁」＋6
        assertNull(searchServer.splitForAutoCommit("2026", 'a'));
    }
}
