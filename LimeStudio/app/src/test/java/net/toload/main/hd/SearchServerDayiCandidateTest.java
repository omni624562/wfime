package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.limedb.LimeDB;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 大易候選規則:只列碼完全相符的字,再加三碼(4 碼字取第 1、2 碼加最後一碼,只列 Big5 字)。
 * 碼比打的還短的字、延伸碼(碼比打的還長)都不列。資料取自實際大易字庫的例子。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SearchServerDayiCandidateTest {

    private SearchServer searchServer;

    private void insert(SQLiteDatabase db, String table, String code, String word, int score, int basescore) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        cv.put("word", word);
        cv.put("score", score);
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

    // 候選字(不含原碼那一筆)
    private List<String> words(String code) {
        List<String> words = new ArrayList<>();
        for (Mapping m : searchServer.getMappingByCode(code, true, false))
            if (!m.isComposingCodeRecord()) words.add(m.getWord());
        return words;
    }

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        LimeDB limeDb = new LimeDB(context);
        // 每個測試的資料目錄都是新的:重開共用連線,不沿用前一個測試留下的連線
        limeDb.openDBConnection(true);
        SQLiteDatabase db = limeDb.getWritableDatabase();
        for (String t : new String[]{"dayi", "phonetic"}) {
            db.execSQL("DROP TABLE IF EXISTS " + t);
            db.execSQL("CREATE TABLE " + t + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                    + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        }
        insert(db, "dayi", "n", "刀", 0, 50);
        insert(db, "dayi", "nh", "鴛", 0, 40);
        insert(db, "dayi", "nh1", "詹", 20, 138);
        insert(db, "dayi", "nh81", "詹", 0, 138);
        insert(db, "dayi", "nhm1", "鮞", 0, 0);
        insert(db, "dayi", "nhm1", "鲕", 0, 0); // 簡體,不在 Big5
        insert(db, "dayi", "nhr1", "鰤", 0, 0);
        insert(db, "dayi", "nh12vdbxe", "詹智丞", 1, 0); // 學過的詞
        insert(db, "dayi", "ujn", "前", 8, 16068);
        insert(db, "dayi", "ujn", "剪", 0, 357);
        insert(db, "dayi", "ujjn", "蒯", 0, 0);
        insert(db, "dayi", "ujnn", "剪", 0, 0);
        insert(db, "dayi", "ujny", "煎", 0, 0);
        insert(db, "dayi", "ujnb", "翦", 0, 0);
        insert(db, "dayi", "ujy", "煎", 0, 0);
        insert(db, "dayi", "d", "日", 0, 500);
        insert(db, "dayi", "d", "曰", 0, 100);
        insert(db, "dayi", "d9", "是", 0, 900);
        insert(db, "dayi", "dfmu", "曀", 0, 0);
        insert(db, "dayi", "a.na", "佽", 0, 0);
        insert(db, "phonetic", "j", "甲", 1, 0);
        insert(db, "phonetic", "ji", "丙", 1, 0);
        insert(db, "phonetic", "jio", "丁", 1, 0);
        db.execSQL("UPDATE phonetic SET code3r = code");

        searchServer = new SearchServer(context);
        searchServer.setTablename("dayi", false, false);
        waitPrefetch();
    }

    @Test
    public void exactMatchAndThreeCode_LikeDesktopDayi() {
        assertEquals(Arrays.asList("詹", "鮞", "鰤"), words("nh1"));
        assertEquals(Arrays.asList("前", "剪", "蒯"), words("ujn"));
    }

    @Test
    public void noShorterCodesNoLongerCodes() {
        // 碼為 d 的字才列;d9 的 是 是延伸碼
        assertEquals(Arrays.asList("日", "曰"), words("d"));
        // 煎 只在 ujy(及完整 4 碼 ujny)
        assertEquals(Arrays.asList("煎"), words("ujy"));
        assertEquals(Arrays.asList("煎"), words("ujny"));
    }

    @Test
    public void threeCodeOnlyAndNoMatch() {
        // 曀 的三碼是 dfu(dfmu 取 1+2+尾);dfm 不是任何字的碼
        assertEquals(Arrays.asList("曀"), words("dfu"));
        assertTrue(words("dfm").isEmpty());
        assertEquals(Arrays.asList("佽"), words("a.a"));
    }

    @Test
    public void threeCodeRecordsAfterExactMatches() {
        List<Mapping> list = searchServer.getMappingByCode("nh1", true, false);
        List<Boolean> exact = new ArrayList<>();
        for (Mapping m : list)
            if (!m.isComposingCodeRecord()) exact.add(m.isExactMatchToCodeRecord());
        assertEquals(Arrays.asList(true, false, false), exact);
    }

    @Test
    public void otherInputMethodsUnchanged() throws Exception {
        searchServer.setTablename("phonetic", false, false);
        waitPrefetch();
        // 注音仍列碼較短與延伸碼的字
        List<String> words = words("ji");
        assertTrue(words.contains("丙"));
        assertTrue(words.contains("丁"));
        assertTrue(words.contains("甲"));
    }
}
