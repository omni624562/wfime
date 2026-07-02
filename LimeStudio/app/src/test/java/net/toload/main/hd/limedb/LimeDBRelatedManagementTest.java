/*
 *
 *  *
 *  **    Copyright 2015, The LimeIME Open Source Project
 *  **
 *  **    Project Url: http://github.com/lime-ime/limeime/
 *  **                 http://android.toload.net/
 *  **
 *  **    This program is free software: you can redistribute it and/or modify
 *  **    it under the terms of the GNU General Public License as published by
 *  **    the Free Software Foundation, either version 3 of the License, or
 *  **    (at your option) any later version.
 *  *
 *  **    This program is distributed in the hope that it will be useful,
 *  **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  **    GNU General Public License for more details.
 *  *
 *  **    You should have received a copy of the GNU General Public License
 *  **    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *
 */

package net.toload.main.hd.limedb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.data.Related;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * 自建關聯字管理的資料層測試。
 *
 * 四種列型:
 * - 純自建:score>0、basescore NULL(addOrUpdateRelatedPhraseRecord 寫入的型態)
 * - 內建被用過:score>0、basescore>0(刪除時只歸零 score,保留字典資料)
 * - 內建未用過:score=0、basescore>0(管理清單不顯示、清除時不受影響)
 * - 頻率計數列:cword NULL(永遠不碰)
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LimeDBRelatedManagementTest {

    private LimeDB limeDb;
    private SQLiteDatabase db;

    private long pureUserId;      // 詹→詠 score 3
    private long bumpedBuiltInId; // 詹→事府 score 5, basescore 15
    private long untouchedBuiltInId; // 詹→森 score 0, basescore 5
    private long counterId;       // cword NULL 計數列

    private long insertRow(String pword, String cword, Integer score, Integer basescore) {
        ContentValues cv = new ContentValues();
        cv.put("pword", pword);
        if (cword != null)
            cv.put("cword", cword);
        if (score != null)
            cv.put("score", score);
        if (basescore != null)
            cv.put("basescore", basescore);
        return db.insert("related", null, cv);
    }

    private int rowCount(String selection) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM related" +
                (selection == null ? "" : " WHERE " + selection), null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    private int scoreOf(long id) {
        Cursor cursor = db.rawQuery("SELECT score FROM related WHERE _id = " + id, null);
        cursor.moveToFirst();
        int score = cursor.getInt(0);
        cursor.close();
        return score;
    }

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        limeDb = new LimeDB(context);
        db = limeDb.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS related (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "pword text, cword text, " +
                "score INTEGER NOT NULL DEFAULT 0, " +
                "basescore INTEGER DEFAULT 0)");
        db.delete("related", null, null);

        pureUserId = insertRow("詹", "詠", 3, null);
        bumpedBuiltInId = insertRow("詹", "事府", 5, 15);
        untouchedBuiltInId = insertRow("詹", "森", 0, 5);
        counterId = insertRow("詹", null, 7, null);
    }

    // =========================================================================
    // getUserLearnedRelated / countUserLearnedRelated
    // =========================================================================

    @Test
    public void testList_ReturnsOnlyUserScoredRowsWithCword_SortedByScoreDesc() {
        List<Related> list = limeDb.getUserLearnedRelated(null, 500);
        assertEquals(2, list.size());
        assertEquals("事府", list.get(0).getCword()); // score 5
        assertEquals("詠", list.get(1).getCword()); // score 3
        assertEquals(2, limeDb.countUserLearnedRelated());
    }

    @Test
    public void testList_BuiltInFlagDistinguishable() {
        List<Related> list = limeDb.getUserLearnedRelated(null, 500);
        assertTrue("內建被用過的列 basescore 應 > 0", list.get(0).getBasescore() > 0);
        assertEquals("純自建列 basescore 應為 0(NULL 讀為 0)", 0, list.get(1).getBasescore());
    }

    @Test
    public void testList_FilterMatchesPwordOrCword() {
        insertRow("今", "天", 2, null);
        assertEquals(1, limeDb.getUserLearnedRelated("天", 500).size());
        assertEquals(1, limeDb.getUserLearnedRelated("今", 500).size());
        assertEquals(2, limeDb.getUserLearnedRelated("詹", 500).size());
    }

    @Test
    public void testList_LikeMetacharactersTreatedLiterally() {
        insertRow("50%", "折", 2, null);
        insertRow("a_b", "x", 1, null);
        insertRow("axb", "y", 1, null);

        // "%" 不可當萬用字元:只命中 50% 那筆
        List<Related> percent = limeDb.getUserLearnedRelated("50%", 500);
        assertEquals(1, percent.size());
        assertEquals("折", percent.get(0).getCword());

        // "_" 不可當單字元萬用:只命中 a_b,不可命中 axb
        List<Related> underscore = limeDb.getUserLearnedRelated("a_b", 500);
        assertEquals(1, underscore.size());
        assertEquals("x", underscore.get(0).getCword());
    }

    @Test
    public void testList_RespectsLimit() {
        for (int i = 0; i < 10; i++)
            insertRow("字" + i, "詞" + i, i + 1, null);
        assertEquals(5, limeDb.getUserLearnedRelated(null, 5).size());
    }

    // =========================================================================
    // removeOrResetUserRelated
    // =========================================================================

    @Test
    public void testDelete_PureUserRow_RemovesRow() {
        limeDb.removeOrResetUserRelated(pureUserId, false);
        assertEquals(0, rowCount("_id = " + pureUserId));
        assertEquals(1, limeDb.countUserLearnedRelated());
    }

    @Test
    public void testDelete_BuiltInRow_ResetsScoreKeepsRow() {
        limeDb.removeOrResetUserRelated(bumpedBuiltInId, true);
        assertEquals(1, rowCount("_id = " + bumpedBuiltInId));
        assertEquals(0, scoreOf(bumpedBuiltInId));
        assertEquals(1, limeDb.countUserLearnedRelated());
    }

    // =========================================================================
    // restoreUserRelated(復原)
    // =========================================================================

    @Test
    public void testRestore_PureUserRow_ReinsertsWithScore() {
        limeDb.removeOrResetUserRelated(pureUserId, false);

        Related snapshot = new Related();
        snapshot.setId((int) pureUserId);
        snapshot.setPword("詹");
        snapshot.setCword("詠");
        snapshot.setUserscore(3);
        snapshot.setBasescore(0);
        limeDb.restoreUserRelated(snapshot);

        List<Related> list = limeDb.getUserLearnedRelated("詠", 500);
        assertEquals(1, list.size());
        assertEquals(3, list.get(0).getUserscore());
        assertEquals("復原的純自建列 basescore 應維持空/0", 0, list.get(0).getBasescore());
    }

    @Test
    public void testRestore_BuiltInRow_RestoresScore() {
        limeDb.removeOrResetUserRelated(bumpedBuiltInId, true);
        assertEquals(0, scoreOf(bumpedBuiltInId));

        Related snapshot = new Related();
        snapshot.setId((int) bumpedBuiltInId);
        snapshot.setPword("詹");
        snapshot.setCword("事府");
        snapshot.setUserscore(5);
        snapshot.setBasescore(15);
        limeDb.restoreUserRelated(snapshot);

        assertEquals(5, scoreOf(bumpedBuiltInId));
        assertEquals(1, rowCount("_id = " + bumpedBuiltInId));
    }

    // =========================================================================
    // clearUserLearnedRelated(全部清除)
    // =========================================================================

    @Test
    public void testClearAll_DeletesPureUser_ResetsBuiltIn_KeepsCounters() {
        limeDb.clearUserLearnedRelated();

        // 純自建列刪除
        assertEquals(0, rowCount("_id = " + pureUserId));
        // 內建被用過:保留、score 歸零
        assertEquals(1, rowCount("_id = " + bumpedBuiltInId));
        assertEquals(0, scoreOf(bumpedBuiltInId));
        // 內建未用過:原封不動
        assertEquals(1, rowCount("_id = " + untouchedBuiltInId));
        // cword NULL 計數列:原封不動(score 仍為 7)
        assertEquals(1, rowCount("_id = " + counterId));
        assertEquals(7, scoreOf(counterId));

        assertEquals(0, limeDb.countUserLearnedRelated());
        assertTrue(limeDb.getUserLearnedRelated(null, 500).isEmpty());
    }

    @Test
    public void testCounterRow_NeverListed() {
        List<Related> list = limeDb.getUserLearnedRelated(null, 500);
        for (Related r : list)
            assertFalse("cword NULL 計數列不可出現在清單", r.getCword() == null);
    }
}
