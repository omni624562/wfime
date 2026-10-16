package net.toload.main.hd.limedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.global.SmartSelectionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 「我的資料」匯出／匯入:換到新手機後學過的字、關聯字、備忘錄、智慧選字紀錄都回來;
 * 匯入是合併(兩邊都留、取較高分),重複匯入不重複;不對的檔案不動任何資料。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UserDataBackupTest {

    private Context context;
    private LimeDB limeDb;
    private SQLiteDatabase db;

    private void insert(String table, String code, String word, int score) {
        ContentValues cv = new ContentValues();
        cv.put("code", code);
        cv.put("word", word);
        cv.put("score", score);
        db.insert(table, null, cv);
    }

    private void related(String pword, String cword, int score, Integer basescore) {
        ContentValues cv = new ContentValues();
        cv.put("pword", pword);
        cv.put("cword", cword);
        cv.put("score", score);
        if (basescore != null) cv.put("basescore", basescore);
        db.insert("related", null, cv);
    }

    private static List<String> rows(SQLiteDatabase d, String sql) {
        List<String> out = new ArrayList<>();
        try (Cursor c = d.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < c.getColumnCount(); i++) sb.append(c.getString(i)).append('|');
                out.add(sb.toString());
            }
        }
        return out;
    }

    private List<String> limeLearned() {
        List<String> out = rows(db, "SELECT 'dayi', code, word, score FROM dayi WHERE score > 0 ORDER BY code, word");
        out.addAll(rows(db, "SELECT pword, cword, score FROM related WHERE score > 0 ORDER BY pword, cword"));
        return out;
    }

    private static void resetSmartSelection() throws Exception {
        Field f = SmartSelectionManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    // 字庫:日 曰 前 都是 0 分;關聯字 日→本 是內建
    private void freshDictionary() {
        for (String t : new String[]{"dayi", "phonetic"}) {
            db.execSQL("DROP TABLE IF EXISTS " + t);
            db.execSQL("CREATE TABLE " + t + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, code text, code3r text, "
                    + "word text, related text, score INTEGER NOT NULL DEFAULT 0, basescore INTEGER DEFAULT 0)");
        }
        db.execSQL("DROP TABLE IF EXISTS dayi_user");
        insert("dayi", "d", "日", 0);
        insert("dayi", "d", "曰", 0);
        insert("dayi", "ujn", "前", 0);
        db.execSQL("DELETE FROM related");
        related("日", "本", 0, 10);
        db.execSQL("DELETE FROM memo");
    }

    // 模擬換到一支新手機:字庫重裝、沒有 user.db、沒有智慧選字紀錄
    private void newPhone() throws Exception {
        UserDataStore.resetSharedForTest();
        context.getDatabasePath(UserDataStore.FILE_NAME).delete();
        freshDictionary();
        SmartSelectionManager.getInstance(context).clearData();
        resetSmartSelection();
        new File(context.getFilesDir(), "dayi_smart_sel.json").delete();
    }

    private byte[] export() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertNotNull(UserDataBackup.exportData(context, out));
        return out.toByteArray();
    }

    @Before
    public void setup() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        UserDataStore.resetSharedForTest();
        resetSmartSelection();
        new File(context.getFilesDir(), "dayi_smart_sel.json").delete();
        limeDb = new LimeDB(context);
        limeDb.openDBConnection(true);
        db = limeDb.getWritableDatabase();
        freshDictionary();
        context.getDatabasePath(UserDataStore.FILE_NAME).delete();
        // 舊手機學過的東西
        db.execSQL("UPDATE dayi SET score = 4 WHERE word = '曰'");
        insert("dayi", "nh12", "詹智", 2);                 // 自己學的新詞
        db.execSQL("UPDATE related SET score = 3 WHERE pword = '日'");
        related("我", "們", 1, null);                       // 自建關聯字
        ContentValues memo = new ContentValues();
        memo.put("content", "地址");
        memo.put("pinned", 1);
        memo.put("created_at", 1700000000000L);
        db.insert("memo", null, memo);
        SmartSelectionManager.getInstance(context).recordSelection("d", "曰", "日");
    }

    @After
    public void tearDown() throws Exception {
        UserDataStore.resetSharedForTest();
        resetSmartSelection();
    }

    @Test
    public void exportThenImportOnNewPhoneBringsEverythingBack() throws Exception {
        List<String> learnedBefore = limeLearned();
        byte[] file = export();
        List<String> names = new ArrayList<>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(file))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) names.add(e.getName());
        }
        assertEquals(Arrays.asList("manifest.json", "user.db", "smart_selection.json"), names);

        newPhone();
        assertTrue(limeLearned().isEmpty());
        UserDataBackup.Result r = UserDataBackup.importData(context, new ByteArrayInputStream(file));
        assertNotNull(r);
        assertEquals(2, r.mapping);
        assertEquals(2, r.related);
        assertEquals(1, r.memo);
        assertEquals(1, r.smart);
        assertEquals(learnedBefore, limeLearned());                       // lime.db 的分數與新詞都回來
        assertEquals(1, rows(db, "SELECT 1 FROM related WHERE pword='我' AND cword='們' AND (basescore IS NULL OR basescore <= 0)").size());
        List<MemoObj> memos = limeDb.getMemos();
        assertEquals(1, memos.size());
        assertEquals("地址", memos.get(0).getContent());
        assertEquals(1, SmartSelectionManager.getInstance(context).getContextCount("d", "曰", "日"));
    }

    @Test
    public void importingSameFileTwiceAddsNothing() throws Exception {
        byte[] file = export();
        newPhone();
        assertNotNull(UserDataBackup.importData(context, new ByteArrayInputStream(file)));
        List<String> once = limeLearned();
        UserDataBackup.Result again = UserDataBackup.importData(context, new ByteArrayInputStream(file));
        assertNotNull(again);
        assertEquals(0, again.mapping + again.related + again.memo);
        assertEquals(once, limeLearned());
        assertEquals(1, limeDb.getMemos().size());
        assertEquals(1, SmartSelectionManager.getInstance(context).getContextCount("d", "曰", "日"));
        // 總次數也不累加
        assertEquals(1, new org.json.JSONObject(SmartSelectionManager.getInstance(context).exportJson())
                .getJSONObject("d").getJSONObject("曰").getInt("count"));
    }

    @Test
    public void mergeKeepsBothSidesAndHigherScore() throws Exception {
        byte[] file = export();                     // 舊手機:曰 4、詹智 2、日→本 3、我→們 1
        newPhone();
        assertNotNull(new UserDataStore(context).migrateIfNeeded(limeDb));
        // 新手機自己也學了:曰 9(比較高)、前 1(舊手機沒有)、詹智 1 與日→本 1(比較低)
        limeDb.addOrUpdateMappingRecord("dayi", "d", "曰", 9);
        limeDb.addOrUpdateMappingRecord("dayi", "ujn", "前", 1);
        limeDb.addOrUpdateMappingRecord("dayi", "nh12", "詹智", 1);   // 比檔案裡的 2 低
        limeDb.addOrUpdateRelatedPhraseRecord("日", "本");
        assertNotNull(UserDataBackup.importData(context, new ByteArrayInputStream(file)));
        assertEquals(Arrays.asList("dayi|d|曰|9|", "dayi|nh12|詹智|2|", "dayi|ujn|前|1|", "我|們|1|", "日|本|3|"),
                limeLearned());
    }

    @Test
    public void imNotLoadedYetIsSkippedButKeptInUserDb() throws Exception {
        byte[] file = export();
        newPhone();
        db.execSQL("DELETE FROM dayi");                     // 新手機還沒載入大易
        assertNotNull(UserDataBackup.importData(context, new ByteArrayInputStream(file)));
        assertEquals(0, rows(db, "SELECT 1 FROM dayi").size());
        assertEquals(2, new UserDataStore(context).learnedMapping("dayi").size());
    }

    private static byte[] zip(String[] names, byte[][] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            for (int i = 0; i < names.length; i++) {
                z.putNextEntry(new ZipEntry(names[i]));
                z.write(data[i]);
                z.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    public void invalidFilesAreRejectedWithoutTouchingData() throws Exception {
        assertNotNull(new UserDataStore(context).migrateIfNeeded(limeDb));
        List<String> before = limeLearned();
        // 不是 zip、zip 裡沒有 user.db、user.db 不是資料庫
        assertNull(UserDataBackup.importData(context, new ByteArrayInputStream("hello".getBytes())));
        assertNull(UserDataBackup.importData(context, new ByteArrayInputStream(
                zip(new String[]{"manifest.json"}, new byte[][]{"{}".getBytes()}))));
        assertNull(UserDataBackup.importData(context, new ByteArrayInputStream(
                zip(new String[]{"user.db"}, new byte[][]{"garbage".getBytes()}))));
        // 沒有搬遷完成標記的資料庫
        File half = new File(context.getCacheDir(), "half.db");
        half.delete();
        SQLiteDatabase h = SQLiteDatabase.openOrCreateDatabase(half, null);
        UserDataStore.createSchema(h);
        h.close();
        assertNull(UserDataBackup.importData(context, new ByteArrayInputStream(
                zip(new String[]{"user.db"}, new byte[][]{java.nio.file.Files.readAllBytes(half.toPath())}))));
        assertEquals(before, limeLearned());
    }

    @Test
    public void zipPathsOutsideAreIgnored() throws Exception {
        byte[] good = export();
        // 在正常的資料檔前面加一個想寫到外面的項目
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out);
             ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(good))) {
            z.putNextEntry(new ZipEntry("../../evil.txt"));
            z.write("x".getBytes());
            z.closeEntry();
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                z.putNextEntry(new ZipEntry(e.getName()));
                byte[] buf = new byte[8192];
                for (int n; (n = in.read(buf)) > 0; ) z.write(buf, 0, n);
                z.closeEntry();
            }
        }
        newPhone();
        assertNotNull(UserDataBackup.importData(context, new ByteArrayInputStream(out.toByteArray())));
        assertFalse(new File(context.getCacheDir().getParentFile(), "evil.txt").exists());
        assertFalse(new File(context.getCacheDir(), "../../evil.txt").exists());
    }
}
