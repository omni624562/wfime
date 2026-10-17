package net.toload.main.hd.limedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.data.Related;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/**
 * 搬遷之後的雙寫:LimeDB 每個學習資料寫入點寫完 lime.db,user.db 要跟著變成同樣的現況
 * (等於從 lime.db 重新搬一次的結果);備忘錄只存 user.db;同步失敗會標 dirty、下次啟動重抄。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UserDataStoreSyncTest {

    private Context context;
    private LimeDB limeDb;
    private SQLiteDatabase db;
    private UserDataStore store;

    private long insert(String table, String code, String code3r, String word, int score) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        if (code3r != null) cv.put("code3r", code3r);
        cv.put("word", word);
        cv.put("score", score);
        return db.insert(table, null, cv);
    }

    private long related(String pword, String cword, int score, Integer basescore) {
        ContentValues cv = new ContentValues();
        cv.put("pword", pword);
        cv.put("cword", cword);
        cv.put("score", score);
        if (basescore != null) cv.put("basescore", basescore);
        return db.insert("related", null, cv);
    }

    private static List<String> rows(SQLiteDatabase d, String sql, String... args) {
        List<String> out = new ArrayList<>();
        try (Cursor c = d.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < c.getColumnCount(); i++) sb.append(c.getString(i)).append('|');
                out.add(sb.toString());
            }
        }
        return out;
    }

    private SQLiteDatabase openUserDb() {
        File f = context.getDatabasePath(UserDataStore.FILE_NAME);
        return SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
    }

    private List<String> userMapping() {
        SQLiteDatabase u = openUserDb();
        try {
            return rows(u, "SELECT im, code, code3r, word, score FROM learned_mapping ORDER BY im, code, word");
        } finally {
            u.close();
        }
    }

    private List<String> userRelated() {
        SQLiteDatabase u = openUserDb();
        try {
            return rows(u, "SELECT pword, cword, score FROM learned_related ORDER BY pword, cword");
        } finally {
            u.close();
        }
    }

    // 從 lime.db 現況獨立算出「重新搬一次」應有的學習資料(主表與 *_user 取較高分)
    private List<String> expectedMapping() {
        TreeMap<String, String[]> m = new TreeMap<>();
        for (String im : new String[]{"dayi", "phonetic"}) {
            for (String t : new String[]{im, im + "_user"}) {
                if (rows(db, "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", t).isEmpty())
                    continue;
                try (Cursor c = db.rawQuery("SELECT code, code3r, word, score FROM " + t
                        + " WHERE score > 0 AND code IS NOT NULL AND word IS NOT NULL", null)) {
                    while (c.moveToNext()) {
                        String key = im + "\u0001" + c.getString(0) + "\u0001" + c.getString(2);
                        String[] old = m.get(key);
                        int score = c.getInt(3);
                        if (old == null)
                            m.put(key, new String[]{im, c.getString(0), c.getString(1), c.getString(2), String.valueOf(score)});
                        else if (score > Integer.parseInt(old[4]))
                            old[4] = String.valueOf(score);
                    }
                }
            }
        }
        List<String> out = new ArrayList<>();
        for (String[] r : m.values()) out.add(r[0] + "|" + r[1] + "|" + r[2] + "|" + r[3] + "|" + r[4] + "|");
        return out;
    }

    private List<String> expectedRelated() {
        return rows(db, "SELECT pword, cword, MAX(score) FROM related WHERE score > 0 AND pword IS NOT NULL "
                + "AND cword IS NOT NULL GROUP BY pword, cword ORDER BY pword, cword");
    }

    private void assertMirrorsLime() {
        assertEquals(expectedMapping(), userMapping());
        assertEquals(expectedRelated(), userRelated());
    }

    private static Mapping word(String code, String w, int score) {
        Mapping m = new Mapping();
        m.setCode(code);
        m.setWord(w);
        m.setScore(score);
        m.setExactMatchToCodeRecord();
        return m;
    }

    private Mapping relatedUnit(long id) {
        try (Cursor c = db.rawQuery("SELECT pword, cword, score FROM related WHERE _id = ?",
                new String[]{String.valueOf(id)})) {
            c.moveToFirst();
            Mapping m = new Mapping();
            m.setId(String.valueOf(id));
            m.setPword(c.getString(0));
            m.setWord(c.getString(1));
            m.setScore(c.getInt(2));
            m.setRelatedPhraseRecord();
            return m;
        }
    }

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        UserDataStore.resetSharedForTest();
        limeDb = new LimeDB(context);
        limeDb.openDBConnection(true);
        db = limeDb.getWritableDatabase();
        for (String t : new String[]{"dayi", "phonetic", "dayi_user"}) {
            db.execSQL("DROP TABLE IF EXISTS " + t);
            db.execSQL("CREATE TABLE " + t + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                    + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        }
        db.execSQL("DELETE FROM related");
        db.execSQL("DELETE FROM memo");
        insert("dayi", "d", null, "日", 0);
        insert("dayi", "d", null, "曰", 3);
        insert("dayi", "dd", null, "曰", 0);   // 同一個字的另一個碼
        insert("dayi", "ujn", null, "前", 0);
        insert("dayi_user", "a1", null, "信", 7);
        insert("phonetic", "u4", "u", "意", 0);
        related("日", "本", 2, 10);    // 內建、學過
        related("日", "期", 0, 5);     // 內建、沒學過
        related("我", "們", 1, null);  // 自建
        ContentValues memo = new ContentValues();
        memo.put("content", "舊備忘");
        memo.put("pinned", 0);
        memo.put("created_at", 1700000000000L);
        db.insert("memo", null, memo);
        context.getDatabasePath(UserDataStore.FILE_NAME).delete();
        limeDb.setTablename("dayi");
        store = UserDataStore.get(context);
    }

    @After
    public void tearDown() {
        UserDataStore.resetSharedForTest();
    }

    @Test
    public void addScoreUpdatesEveryCodeOfTheWord() {
        assertNotNull(store.migrateIfNeeded(limeDb));
        limeDb.addScore(word("d", "曰", 3));
        // addScore 依字更新:d 與 dd 兩個碼都變成新分數
        assertEquals(Arrays.asList("dayi|a1|null|信|7|", "dayi|d|null|曰|4|", "dayi|dd|null|曰|4|"), userMapping());
        assertMirrorsLime();
    }

    @Test
    public void addOrUpdateMappingRecordInsertsAndRaises() {
        assertNotNull(store.migrateIfNeeded(limeDb));
        limeDb.addOrUpdateMappingRecord("dayi", "nh12", "詹智", -1);          // 新詞
        limeDb.addOrUpdateMappingRecord("dayi", "ujn", "前", -1);             // 既有、從 0 升到 1
        limeDb.addOrUpdateMappingRecord("phonetic", "u4", "意", 6);           // 指定分數、帶 code3r
        assertMirrorsLime();
        assertTrue(userMapping().contains("dayi|nh12|null|詹智|1|"));
        assertTrue(userMapping().contains("phonetic|u4|u|意|6|"));
    }

    // 用 lime.db 同結構建一份來源字庫檔(只有一張表),rows 為 {code, code3r, word, score}
    private File sourceDb(String name, String srcTable, String likeTable, String[][] rows) {
        File src = new File(context.getCacheDir(), name);
        src.delete();
        db.execSQL("ATTACH DATABASE ? AS s", new Object[]{src.getPath()});
        db.execSQL("CREATE TABLE s." + srcTable + " AS SELECT * FROM main." + likeTable + " WHERE 0");
        for (String[] r : rows)
            db.execSQL("INSERT INTO s." + srcTable + " (code, code3r, word, score) VALUES (?, ?, ?, ?)",
                    new Object[]{r[0], r[1], r[2], Integer.parseInt(r[3])});
        db.execSQL("DETACH DATABASE s");
        return src;
    }

    private List<String> limeLearned(String table) {
        return rows(db, "SELECT code, code3r, word, score FROM " + table + " WHERE score > 0 ORDER BY code, word");
    }

    @Test
    public void importDictionaryThenRestoreFromUserDb() {
        insert("dayi", "nh12", null, "詹智", 2);              // 自己學的新詞
        assertNotNull(store.migrateIfNeeded(limeDb));
        // 換一份新字庫:原本的字都在但分數 0,新詞沒有;另有一筆字庫自帶分數
        File src = sourceDb("dayi_new.db", "dayi", "dayi", new String[][]{
                {"d", null, "日", "0"}, {"d", null, "曰", "0"}, {"ujn", null, "前", "0"}, {"x", null, "新", "5"}});
        assertTrue(limeDb.importDb(src.getPath(), "dayi") > 0);
        assertEquals(Collections.singletonList("x|null|新|5|"), limeLearned("dayi"));
        assertTrue(userMapping().contains("dayi|x|null|新|5|"));   // 字庫自帶分數收進 user.db

        assertEquals(4, limeDb.restoreLearnedFromUserData("dayi"));
        // 已有的列套回分數、沒有的(新學詞、舊備份表的字)整列補上
        assertEquals(java.util.Arrays.asList("a1|null|信|7|", "d|null|曰|3|", "nh12|null|詹智|2|", "x|null|新|5|"),
                limeLearned("dayi"));
        assertEquals(1, rows(db, "SELECT 1 FROM dayi WHERE code = 'd' AND word = '曰'").size()); // 不重複插入
        assertMirrorsLime();
    }

    @Test
    public void restoreKeepsHigherDictionaryScoreAndFillsCode3r() {
        insert("phonetic", "u4", "u", "意", 3);
        insert("phonetic", "u4", "u", "義", 1);
        assertNotNull(store.migrateIfNeeded(limeDb));
        File src = sourceDb("phonetic_new.db", "phonetic", "phonetic", new String[][]{{"u4", "u", "義", "6"}});
        assertTrue(limeDb.importDb(src.getPath(), "phonetic") > 0);
        assertEquals(2, limeDb.restoreLearnedFromUserData("phonetic"));
        // 字庫分數較高的不降回去;補回的列算出 code3r
        assertEquals(java.util.Arrays.asList("u4|u|意|3|", "u4|u|義|6|"), limeLearned("phonetic"));
        assertMirrorsLime();
    }

    @Test
    public void customDbImportAbsorbsScores() {
        db.execSQL("DROP TABLE IF EXISTS custom");
        db.execSQL("CREATE TABLE custom AS SELECT * FROM dayi WHERE 0");
        assertNotNull(store.migrateIfNeeded(limeDb));
        File src = sourceDb("custom_new.db", "custom", "custom", new String[][]{{"ab", null, "甲", "4"}});
        db.execSQL("ATTACH DATABASE ? AS s", new Object[]{src.getPath()});
        db.execSQL("CREATE TABLE s.im AS SELECT * FROM main.im WHERE 0");
        db.execSQL("DETACH DATABASE s");
        assertTrue(limeDb.importBackupDb(src, "custom"));
        assertTrue(userMapping().contains("custom|ab|null|甲|4|"));
    }

    @Test
    public void textFileImportAbsorbsScores() throws Exception {
        assertNotNull(store.migrateIfNeeded(limeDb));
        File txt = new File(context.getCacheDir(), "dayi_new.lime");
        try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(txt),
                java.nio.charset.StandardCharsets.UTF_8)) {
            w.write("d|曰|0|0\nx|新|5|0\n");
        }
        limeDb.setFilename(txt);
        limeDb.loadFileV2("dayi", new net.toload.main.hd.global.LIMEProgressListener() {
            @Override
            public void onProgress(long a, long b, String s) {
            }
        });
        // 載入在背景執行緒:等 user.db 收到匯入檔自帶的分數
        long until = System.currentTimeMillis() + 15000;
        while (!userMapping().contains("dayi|x|null|新|5|") && System.currentTimeMillis() < until)
            Thread.sleep(100);
        assertTrue(userMapping().contains("dayi|x|null|新|5|"));
    }

    @Test
    public void snapshotFromD11IsUpgradedOnce() {
        assertNotNull(store.migrateIfNeeded(limeDb));
        // 模擬 D1-1 版留下的 user.db:沒有雙寫標記;之後的學習、刪除、備忘錄都只進了 lime.db
        File f = context.getDatabasePath(UserDataStore.FILE_NAME);
        UserDataStore.resetSharedForTest();
        SQLiteDatabase u = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        assertEquals(1, rows(u, "SELECT 1 FROM meta WHERE key = 'dual_write'").size()); // 新搬遷直接標記
        u.delete("meta", "key = 'dual_write'", null);
        u.close();
        insert("dayi", "x", null, "新", 2);
        db.execSQL("DELETE FROM related WHERE pword = '我'");
        ContentValues memo = new ContentValues();
        memo.put("content", "D1-1 期間寫的");
        memo.put("pinned", 0);
        memo.put("created_at", 1710000000000L);
        db.insert("memo", null, memo);

        store = UserDataStore.get(context);
        store.migrateIfNeeded(limeDb);
        assertMirrorsLime();
        List<String> memos = new ArrayList<>();
        for (MemoObj m : limeDb.getMemos()) memos.add(m.getContent());
        assertEquals(Arrays.asList("D1-1 期間寫的", "舊備忘"), memos);

        // 標記過就不再重抄:之後只在 user.db 的備忘錄要留著
        limeDb.insertMemo("只在 user.db", 0);
        store.migrateIfNeeded(limeDb);
        assertEquals(3, limeDb.getMemos().size());
        insert("dayi", "y", null, "另", 3);                  // 直接寫 lime.db、沒經過同步
        store.migrateIfNeeded(limeDb);
        assertTrue(!userMapping().contains("dayi|y|null|另|3|"));
    }

    @Test
    public void restoreBeforeMigrationFallsBack() {
        assertEquals(-1, limeDb.restoreLearnedFromUserData("dayi"));
    }

    @Test
    public void backupTableHigherScoreIsKept() {
        insert("dayi", "a1", null, "信", 0);                  // 主表有、沒學過;舊備份表是 7 分
        assertNotNull(store.migrateIfNeeded(limeDb));
        limeDb.addOrUpdateMappingRecord("dayi", "a1", "信", -1);  // 主表變 1 分
        assertTrue(userMapping().contains("dayi|a1|null|信|7|")); // 和重新搬一次一樣取較高分
        assertMirrorsLime();
    }

    @Test
    public void relatedWritesFollowLime() {
        assertNotNull(store.migrateIfNeeded(limeDb));
        limeDb.addOrUpdateRelatedPhraseRecord("你", "好");   // 新增自建
        limeDb.addOrUpdateRelatedPhraseRecord("日", "期");   // 內建從 0 升到 1
        limeDb.addOrUpdateRelatedPhraseRecord("日", "本");   // 內建加分
        assertMirrorsLime();
        assertEquals(Arrays.asList("你|好|1|", "我|們|1|", "日|期|1|", "日|本|3|"), userRelated());

        long builtIn = Long.parseLong(rows(db, "SELECT _id FROM related WHERE pword='日' AND cword='本'").get(0).replace("|", ""));
        limeDb.addScore(relatedUnit(builtIn));               // 從關聯字清單選字
        assertMirrorsLime();
        assertEquals(Arrays.asList("你|好|1|", "我|們|1|", "日|期|1|", "日|本|4|"), userRelated());

        // 自建關聯字管理:內建歸零、自建刪除,復原後回來
        long own = Long.parseLong(rows(db, "SELECT _id FROM related WHERE pword='我' AND cword='們'").get(0).replace("|", ""));
        Related ownItem = Related.getList(db.rawQuery("SELECT * FROM related WHERE _id = " + own, null)).get(0);
        Related builtInItem = Related.getList(db.rawQuery("SELECT * FROM related WHERE _id = " + builtIn, null)).get(0);
        limeDb.removeOrResetUserRelated(own, false);
        limeDb.removeOrResetUserRelated(builtIn, true);
        assertMirrorsLime();
        assertEquals(Arrays.asList("你|好|1|", "日|期|1|"), userRelated());
        limeDb.restoreUserRelated(ownItem);
        limeDb.restoreUserRelated(builtInItem);
        assertMirrorsLime();

        limeDb.deleteRelatedPhrase("你", "好");             // 候選列長按刪除
        assertMirrorsLime();
        limeDb.clearUserLearnedRelated();
        assertEquals(Collections.emptyList(), userRelated());
        assertMirrorsLime();
    }

    @Test
    public void importBackupRelatedDbResyncs() throws Exception {
        assertNotNull(store.migrateIfNeeded(limeDb));
        File src = new File(context.getCacheDir(), "related_src.db");
        src.delete();
        // 用 lime.db 的 related 結構建一份來源檔:一筆新的已學關聯字、一筆和既有同組的內建列
        db.execSQL("ATTACH DATABASE ? AS s", new Object[]{src.getPath()});
        db.execSQL("CREATE TABLE s.related AS SELECT * FROM main.related WHERE 0");
        db.execSQL("INSERT INTO s.related (pword, cword, basescore, score) VALUES ('天', '氣', 3, 4), ('日', '本', 10, 0)");
        db.execSQL("DETACH DATABASE s");
        assertTrue(limeDb.importBackupRelatedDb(src));
        assertMirrorsLime();
        assertTrue(userRelated().contains("天|氣|4|"));
        assertTrue(userRelated().contains("日|本|2|"));      // 學過的分數套回
    }

    @Test
    public void notMigratedYetSkipsThenMigrationCopies() {
        limeDb.addScore(word("d", "曰", 3));                 // 還沒搬:只寫 lime.db
        assertTrue(!context.getDatabasePath(UserDataStore.FILE_NAME).exists());
        assertNotNull(store.migrateIfNeeded(limeDb));
        assertMirrorsLime();
        assertTrue(userMapping().contains("dayi|d|null|曰|4|"));
    }

    @Test
    public void clearImRemovesOnlyThatIm() {
        insert("phonetic", "u4", "u", "義", 2);
        assertNotNull(store.migrateIfNeeded(limeDb));
        store.clearIm("dayi");
        assertEquals(Collections.singletonList("phonetic|u4|u|義|2|"), userMapping());
    }

    @Test
    public void memoLivesInUserDbAfterMigration() {
        // 第一次用備忘錄就會先搬遷,舊備忘錄帶過去
        long id = limeDb.insertMemo("新備忘", 0);
        assertTrue(id > 0);
        assertTrue(store.isMigrated());
        List<String> lime = rows(db, "SELECT content FROM memo");
        assertEquals(Collections.singletonList("舊備忘|"), lime);   // lime.db 不再寫入
        limeDb.updateMemoPin((int) id, 1);
        limeDb.updateMemoContent((int) id, "改過");
        List<MemoObj> memos = limeDb.getMemos();
        assertEquals(2, memos.size());
        assertEquals("改過", memos.get(0).getContent());            // 置頂排前面
        assertEquals("舊備忘", memos.get(1).getContent());
        assertEquals(1, limeDb.deleteMemo(memos.get(1).getId()));
        assertEquals(1, limeDb.getMemos().size());
        assertEquals(lime, rows(db, "SELECT content FROM memo"));
    }

    @Test
    public void failedSyncMarksDirtyAndNextStartResyncs() {
        assertNotNull(store.migrateIfNeeded(limeDb));
        File f = context.getDatabasePath(UserDataStore.FILE_NAME);
        SQLiteDatabase u = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        u.execSQL("CREATE TRIGGER fail BEFORE INSERT ON learned_mapping BEGIN SELECT RAISE(ABORT, 'boom'); END");
        u.close();
        limeDb.addOrUpdateMappingRecord("dayi", "nh12", "詹智", -1);
        u = openUserDb();
        try {
            assertEquals(1, rows(u, "SELECT 1 FROM meta WHERE key = 'dirty'").size());
        } finally {
            u.close();
        }
        u = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        u.execSQL("DROP TRIGGER fail");
        u.close();
        // 下次啟動:以 lime.db 為準重抄,備忘錄不動
        limeDb.insertMemo("留著", 0);
        UserDataStore.resetSharedForTest();
        store = UserDataStore.get(context);
        store.migrateIfNeeded(limeDb);
        assertMirrorsLime();
        u = openUserDb();
        try {
            assertEquals(0, rows(u, "SELECT 1 FROM meta WHERE key = 'dirty'").size());
            assertEquals(2, rows(u, "SELECT 1 FROM memo").size());
        } finally {
            u.close();
        }
    }

    @Test
    public void randomOperationsKeepUserDbEqualToFreshMigration() {
        assertNotNull(store.migrateIfNeeded(limeDb));
        Random r = new Random(20261015L);
        String[] codes = {"d", "dd", "ujn", "a1", "x"};
        String[] words = {"日", "曰", "前", "信", "新"};
        String[] ps = {"日", "我", "你"};
        String[] cs = {"本", "期", "們", "好"};
        for (int i = 0; i < 300; i++) {
            switch (r.nextInt(6)) {
                case 0:
                    limeDb.addScore(word(codes[r.nextInt(codes.length)], words[r.nextInt(words.length)], r.nextInt(5)));
                    break;
                case 1:
                    limeDb.addOrUpdateMappingRecord("dayi", codes[r.nextInt(codes.length)], words[r.nextInt(words.length)],
                            r.nextBoolean() ? -1 : r.nextInt(9));
                    break;
                case 2:
                    limeDb.addOrUpdateRelatedPhraseRecord(ps[r.nextInt(ps.length)], cs[r.nextInt(cs.length)]);
                    break;
                case 3:
                    limeDb.deleteRelatedPhrase(ps[r.nextInt(ps.length)], cs[r.nextInt(cs.length)]);
                    break;
                case 4: {
                    List<Related> learned = limeDb.getUserLearnedRelated("", 100);
                    if (!learned.isEmpty()) {
                        Related it = learned.get(r.nextInt(learned.size()));
                        limeDb.removeOrResetUserRelated(it.getId(), it.getBasescore() > 0);
                    }
                    break;
                }
                default: {
                    List<String> ids = rows(db, "SELECT _id FROM related WHERE cword IS NOT NULL");
                    if (ids.isEmpty())
                        break;
                    long id = Long.parseLong(ids.get(r.nextInt(ids.size())).replace("|", ""));
                    limeDb.addScore(relatedUnit(id));
                }
            }
            assertMirrorsLime();
        }
    }
}
