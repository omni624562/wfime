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
 * 注音查詢與字根反查:
 * 精確碼的候選要全部列出(不能只到 15 個)、聲調在中間的注音照樣查得到詞、
 * 字根反查開啟時打大易查注音、打注音查大易,關閉時不查。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SearchServerPhoneticAndReverseLookupTest {

    private Context context;
    private SearchServer searchServer;

    private void insert(SQLiteDatabase db, String table, String code, String word, int basescore) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        cv.put("code3r", code.replaceAll("[3467 ]", ""));
        cv.put("word", word);
        cv.put("score", 0);
        cv.put("basescore", basescore);
        db.insert(table, null, cv);
    }

    // setTablename 會在背景預先查詢 a~z,等它跑完再測,避免兩邊同時讀寫快取
    private void waitPrefetch() throws Exception {
        Field f = SearchServer.class.getDeclaredField("prefetchRunning");
        f.setAccessible(true);
        AtomicBoolean running = (AtomicBoolean) f.get(null);
        for (int i = 0; i < 200 && running.get(); i++) Thread.sleep(50);
    }

    private void useTable(String table) throws Exception {
        searchServer.setTablename(table, true, true);
        waitPrefetch();
    }

    // 碼完全等於 code 的候選(查 u4 時也會合併碼較短的 u 的結果,那些不算)
    private List<String> exactWords(String code) {
        List<String> words = new ArrayList<>();
        for (Mapping m : searchServer.getMappingByCode(code, true))
            if (m.isExactMatchToCodeRecord() && code.equals(m.getCode())) words.add(m.getWord());
        return words;
    }

    private List<String> allWords(String code) {
        List<String> words = new ArrayList<>();
        for (Mapping m : searchServer.getMappingByCode(code, true))
            if (!m.isComposingCodeRecord()) words.add(m.getWord());
        return words;
    }

    @Before
    public void setup() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        // SearchServer 的 LimeDB 是 static,會沿用前一個測試的 context 與設定檔;
        // 清掉讓這個測試的「字根反查」設定生效
        Field adapter = SearchServer.class.getDeclaredField("dbadapter");
        adapter.setAccessible(true);
        adapter.set(null, null);
        LimeDB limeDb = new LimeDB(context);
        // 每個測試的資料目錄都是新的:重開共用連線,不沿用前一個測試留下的連線
        limeDb.openDBConnection(true);
        SQLiteDatabase db = limeDb.getWritableDatabase();
        for (String t : new String[]{"dayi", "phonetic"}) {
            db.execSQL("DROP TABLE IF EXISTS " + t);
            db.execSQL("CREATE TABLE " + t + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                    + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        }
        // ㄧˋ(u4)放 30 個字,超過原本的 15 個上限
        for (int i = 0; i < 30; i++)
            insert(db, "phonetic", "u4", String.valueOf((char) ('一' + i * 7)), 1000 - i);
        insert(db, "phonetic", "u", "衣", 2000);
        insert(db, "phonetic", "b4", "日", 900);
        insert(db, "phonetic", "j4", "信", 800);
        // 詞的碼存成去掉聲調的形式(code3r 相同)
        insert(db, "phonetic", "usu", "意你", 100);
        insert(db, "dayi", "d", "日", 500);
        insert(db, "dayi", "a1", "信", 300);

        new LIMEPreferenceManager(context).setParameter("reverse_lookup", false);
        searchServer = new SearchServer(context);
    }

    @Test
    public void allExactMatchesListed_NotCappedAt15() throws Exception {
        useTable("phonetic");
        List<String> words = exactWords("u4");
        assertEquals(30, words.size());
        // 精確碼的字全部排在碼較短的字(衣)前面
        List<String> all = allWords("u4");
        assertTrue(all.indexOf("衣") > all.indexOf(words.get(words.size() - 1)));
    }

    @Test
    public void toneInTheMiddleStillFindsPhrase() throws Exception {
        useTable("phonetic");
        assertTrue(allWords("u4su3").contains("意你"));
    }

    @Test
    public void reverseLookupOff_ReturnsNull() throws Exception {
        useTable("dayi");
        assertNull(searchServer.getCodeListStringFromWord("日"));
    }

    @Test
    public void reverseLookupOn_DayiShowsChuyin() throws Exception {
        new LIMEPreferenceManager(context).setParameter("reverse_lookup", true);
        useTable("dayi");
        assertEquals("日=ㄖˋ", searchServer.getCodeListStringFromWord("日"));
    }

    @Test
    public void reverseLookupOn_ChuyinShowsDayiRoots() throws Exception {
        new LIMEPreferenceManager(context).setParameter("reverse_lookup", true);
        useTable("phonetic");
        assertEquals("信=人言", searchServer.getCodeListStringFromWord("信"));
    }

    @Test
    public void reverseLookupOn_NoDataReturnsNull() throws Exception {
        new LIMEPreferenceManager(context).setParameter("reverse_lookup", true);
        useTable("dayi");
        assertNull(searchServer.getCodeListStringFromWord("龘"));
    }
}
