package net.toload.main.hd.limedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import net.toload.main.hd.Lime;
import net.toload.main.hd.R;
import net.toload.main.hd.data.ChineseSymbol;
import net.toload.main.hd.data.Im;
import net.toload.main.hd.data.ImObj;
import net.toload.main.hd.data.Keyboard;
import net.toload.main.hd.data.KeyboardObj;
import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.data.Related;
import net.toload.main.hd.data.Word;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.LIMEProgressListener;
import net.toload.main.hd.global.LIMEUtilities;

/**
 * 關聯字（related 表）：查詢、學習、自建關聯字管理與匯入。
 * 從 LimeDB 原樣搬出，LimeDB 對應的公開方法只轉呼叫這裡（鎖仍在 LimeDB 的 synchronized 上）。
 */
final class RelatedStore {
    private static final boolean DEBUG = LimeDB.DEBUG;
    private static final String TAG = LimeDB.TAG;

    private final LimeDB owner;

    RelatedStore(LimeDB owner) {
        this.owner = owner;
    }

    /**
     * 清除所有自建關聯字:純自建列(basescore 空)刪除、內建列(basescore>0)使用次數歸零。
     * 不動 cword IS NULL 的頻率計數列(見 addOrUpdateRelatedPhraseRecord)與內建字典本體。
     */
    public void clearUserLearnedRelated() {
        if (!owner.checkDBConnection())
            return;
        owner.mLIMEPref.setTotalUserdictRecords("0");
        try {
            LimeDB.db.beginTransaction();
            ContentValues cv = new ContentValues();
            cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, 0);
            LimeDB.db.update(Lime.DB_RELATED, cv,
                    LimeDB.FIELD_DIC_score + " > 0 AND " + Lime.DB_RELATED_COLUMN_BASESCORE + " > 0 AND "
                            + LimeDB.FIELD_DIC_cword + " IS NOT NULL",
                    null);
            LimeDB.db.delete(Lime.DB_RELATED,
                    LimeDB.FIELD_DIC_score + " > 0 AND (" + Lime.DB_RELATED_COLUMN_BASESCORE + " IS NULL OR "
                            + Lime.DB_RELATED_COLUMN_BASESCORE + " <= 0) AND "
                            + LimeDB.FIELD_DIC_cword + " IS NOT NULL",
                    null);
            LimeDB.db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LimeDB.db.endTransaction();
        }
        owner.relatedscore.clear();
        owner.userStore().resyncAllRelated();
    }

    /**
     * 自建關聯字管理:列出使用者學習過的關聯字(score>0),依使用次數排序。
     * filter 為空時列出全部;否則以 pword/cword LIKE 過濾(% _ \ 跳脫為字面值)。
     */
    public List<Related> getUserLearnedRelated(String filter, int limit) {
        List<Related> result = new ArrayList<>();
        if (!owner.checkDBConnection())
            return result;
        String selection = LimeDB.FIELD_DIC_score + " > 0 AND " + LimeDB.FIELD_DIC_cword + " IS NOT NULL";
        String[] selectionArgs = null;
        if (filter != null && !filter.trim().isEmpty()) {
            String pattern = "%" + filter.trim()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            selection += " AND (" + LimeDB.FIELD_DIC_pword + " LIKE ? ESCAPE '\\' OR "
                    + LimeDB.FIELD_DIC_cword + " LIKE ? ESCAPE '\\')";
            selectionArgs = new String[] { pattern, pattern };
        }
        try {
            Cursor cursor = LimeDB.db.query(Lime.DB_RELATED, null, selection, selectionArgs, null, null,
                    LimeDB.FIELD_DIC_score + " DESC, " + LimeDB.FIELD_DIC_pword + " ASC", String.valueOf(limit));
            if (cursor != null)
                result = Related.getList(cursor); // getList() 會關閉 cursor
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 自建關聯字管理:自建關聯字總數(score>0,排除頻率計數列)
     */
    public int countUserLearnedRelated() {
        if (!owner.checkDBConnection())
            return 0;
        Cursor cursor = null;
        try {
            cursor = LimeDB.db.rawQuery("SELECT COUNT(*) FROM " + Lime.DB_RELATED + " WHERE "
                    + LimeDB.FIELD_DIC_score + " > 0 AND " + LimeDB.FIELD_DIC_cword + " IS NOT NULL", null);
            if (cursor.moveToFirst())
                return cursor.getInt(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return 0;
    }

    /**
     * 自建關聯字管理:刪除單筆。內建列(builtIn)只把使用次數歸零保留字典資料;
     * 純自建列直接刪除並遞減自建字數統計。
     */
    public void removeOrResetUserRelated(long id, boolean builtIn) {
        if (!owner.checkDBConnection())
            return;
        String[] pair = owner.userStore().relatedPair(id);
        try {
            if (builtIn) {
                ContentValues cv = new ContentValues();
                cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, 0);
                LimeDB.db.update(Lime.DB_RELATED, cv, LimeDB.FIELD_ID + " = ?", new String[] { String.valueOf(id) });
            } else {
                LimeDB.db.delete(Lime.DB_RELATED, LimeDB.FIELD_ID + " = ?", new String[] { String.valueOf(id) });
                int dictotal = Integer.parseInt(owner.mLIMEPref.getTotalUserdictRecords());
                if (dictotal > 0)
                    owner.mLIMEPref.setTotalUserdictRecords(String.valueOf(dictotal - 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        owner.relatedscore.remove(String.valueOf(id));
        if (pair != null)
            owner.userStore().syncRelated(pair[0], pair[1]);
    }

    /**
     * 自建關聯字管理:復原剛刪除的單筆(Snackbar 復原用)。
     * 內建列還原使用次數;純自建列重新插入(basescore 留空以維持自建判別)。
     */
    public void restoreUserRelated(Related item) {
        if (item == null || !owner.checkDBConnection())
            return;
        try {
            ContentValues cv = new ContentValues();
            if (item.getBasescore() > 0) {
                cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, item.getUserscore());
                LimeDB.db.update(Lime.DB_RELATED, cv, LimeDB.FIELD_ID + " = ?",
                        new String[] { String.valueOf(item.getId()) });
            } else {
                cv.put(Lime.DB_RELATED_COLUMN_PWORD, item.getPword());
                cv.put(Lime.DB_RELATED_COLUMN_CWORD, item.getCword());
                cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, item.getUserscore());
                LimeDB.db.insert(Lime.DB_RELATED, null, cv);
                int dictotal = Integer.parseInt(owner.mLIMEPref.getTotalUserdictRecords());
                owner.mLIMEPref.setTotalUserdictRecords(String.valueOf(dictotal + 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        owner.relatedscore.remove(String.valueOf(item.getId()));
        owner.userStore().syncRelated(item.getPword(), item.getCword());
    }

    public int addOrUpdateRelatedPhraseRecord(String pword, String cword) {

        // Jeremy '12,4,17 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return -1;

        // Jeremy '11,6,12
        // Return if not learing related words and cword is not null (recording word
        // frequency in IM relatedlist field)
        if (!owner.mLIMEPref.getLearnRelatedWord() && cword != null)
            return -1;

        // Remove all the chinese symbols from the related words
        if (owner.mLIMEPref.getLearnRelatedWord() && cword != null) {
            try {
                // Remove Punctutation
                String[] chinesesymbols = ChineseSymbol.chineseSymbols.split("\\|");
                for (String s : chinesesymbols) {
                    cword = cword.replace(s, "");
                    if (cword.isEmpty()) {
                        return -1;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int dictotal = Integer.parseInt(owner.mLIMEPref.getTotalUserdictRecords());

        if (DEBUG)
            Log.i(TAG,
                    "addOrUpdateRelatedPhraseRecord(): pword:" + pword + " cword:" + cword + "dictotoal:" + dictotal);

        int score = 1;

        ContentValues cv = new ContentValues();
        try {
            Mapping munit = this.isRelatedPhraseExistOnDB(LimeDB.db, pword, cword);

            if (munit == null) {
                cv.put(Lime.DB_RELATED_COLUMN_PWORD, pword);
                cv.put(Lime.DB_RELATED_COLUMN_CWORD, cword);
                // cv.put(Lime.DB_RELATED_COLUMN_SCORE, score); leave this field null so as we
                // can distinguish records learned from user. Jeremy '15,6,3
                cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, score);
                LimeDB.db.insert(Lime.DB_RELATED, null, cv);
                dictotal++;
                owner.mLIMEPref.setTotalUserdictRecords(String.valueOf(dictotal));
                if (DEBUG)
                    Log.i(TAG, "addOrUpdateRelatedPhraseRecord(): new record, dictotal:" + dictotal);
            } else {// the item exist in preload related database.
                if (owner.relatedscore.get(munit.getId()) == null) {
                    score = munit.getScore() + 1;
                    owner.relatedscore.put(munit.getId(), score);
                } else {
                    score = owner.relatedscore.get(munit.getId()) + 1;
                    owner.relatedscore.put(munit.getId(), score);
                }
                cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, score);
                LimeDB.db.update(Lime.DB_RELATED, cv, LimeDB.FIELD_ID + " = ?", new String[] { munit.getId() });

                // Log.i("TAG RELATED A", munit.getId() + " : Related ADD Score :" + score);

                if (DEBUG)
                    Log.i(TAG, "addOrUpdateRelatedPhraseRecord():update score on existing record; score:" + score);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        owner.userStore().syncRelated(pword, cword);

        return score;

    }

    /**
     * Get dictionary database contents
     */
    public List<Mapping> getRelatedPhrase(String pword) {
        if (DEBUG)
            Log.i(TAG, "getRelatedPhrase()");

        List<Mapping> result = new LinkedList<>();

        // 兩開關獨立運作:「啟用關聯字典」顯示內建字典列(basescore>0),
        // 「啟動自建關聯字」顯示使用者學習過的列(score>0),各自可單獨開啟
        final boolean dictionaryEnabled = owner.mLIMEPref.getSimiliarEnable();
        final boolean userLearnedEnabled = owner.mLIMEPref.getLearnRelatedWord();

        if (dictionaryEnabled || userLearnedEnabled) {

            String originFilter = "";
            if (!dictionaryEnabled)
                originFilter = " and " + Lime.DB_RELATED_COLUMN_USERSCORE + " > 0";
            else if (!userLearnedEnabled)
                originFilter = " and " + Lime.DB_RELATED_COLUMN_BASESCORE + " > 0";

            if (pword != null && !pword.trim().equals("")) {

                Cursor cursor;

                // Jeremy '11,8.23 remove group by condition to avoid sorting ordr
                // Jeremy '11,8,1 add group by cword to remove duplicate items.
                // Jeremy '11,6,12, Add constraint on cword is not null (cword =null is for
                // recoding im related list selected count).
                // Jeremy '12,12,21 Add limitClause to limit candidates in only 1 page first.
                // to do 2 stage query.
                // Jeremy '14,12,38 Add query on word length > 1 to include last character into
                // query
                String limitClause = LimeDB.INITIAL_RESULT_LIMIT;

                if (pword.length() > 1) {

                    String last = pword.substring(pword.length() - 1);

                    String selectString = "SELECT " + LimeDB.FIELD_ID + ", " + LimeDB.FIELD_DIC_pword + ", " + LimeDB.FIELD_DIC_cword + ", "
                            + Lime.DB_RELATED_COLUMN_BASESCORE + ", " + Lime.DB_RELATED_COLUMN_USERSCORE
                            + ", length(" + LimeDB.FIELD_DIC_pword + ") as len FROM " + Lime.DB_RELATED + " where ("
                            + LimeDB.FIELD_DIC_pword + " = ?"
                            + " or " + LimeDB.FIELD_DIC_pword + " = ?"
                            + " and " + LimeDB.FIELD_DIC_cword + " is not null"
                            + ")" + originFilter
                            + " order by len desc, " + Lime.DB_RELATED_COLUMN_USERSCORE + " desc, "
                            + Lime.DB_RELATED_COLUMN_BASESCORE + " desc ";

                    selectString += " limit " + limitClause;

                    if (DEBUG)
                        Log.i(TAG, "getRelatedPhrase() selectString = " + selectString);

                    try {
                        cursor = LimeDB.db.rawQuery(selectString, new String[] { pword, last });
                    } catch (SQLiteException sqe) {
                        if (DEBUG)
                            sqe.getStackTrace();

                        cursor = null;
                    }

                } else {
                    cursor = LimeDB.db.query(Lime.DB_RELATED, null, LimeDB.FIELD_DIC_pword + " = ?"
                            + " and " + LimeDB.FIELD_DIC_cword + " is not null " + originFilter, new String[] { pword }, null, null,
                            Lime.DB_RELATED_COLUMN_USERSCORE + " DESC, "
                                    + Lime.DB_RELATED_COLUMN_BASESCORE + " DESC",
                            limitClause);
                }
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {

                            int rsize = 0;
                            do {
                                Mapping munit = new Mapping();
                                munit.setId(cursor.getString(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_ID)));
                                munit.setPword(cursor.getString(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_PWORD)));
                                munit.setCode("");
                                munit.setWord(cursor.getString(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_CWORD)));
                                munit.setScore(cursor.getInt(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_USERSCORE)));
                                munit.setBasescore(
                                        cursor.getInt(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_BASESCORE)));
                                munit.setRelatedPhraseRecord();
                                result.add(munit);
                                rsize++;
                            } while (cursor.moveToNext());
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
        return result;
    }

    public boolean importBackupRelatedDb(File sourcedbfile) {
        if (!owner.checkDBConnection())
            return false;

        // 匯入會整張取代 related:先保存學習過(score>0)的關聯字,匯入後套回
        List<ContentValues> learned = new ArrayList<>();
        try (Cursor cursor = LimeDB.db.query(Lime.DB_RELATED,
                new String[] { LimeDB.FIELD_DIC_pword, LimeDB.FIELD_DIC_cword, LimeDB.FIELD_DIC_score, Lime.DB_RELATED_COLUMN_BASESCORE },
                LimeDB.FIELD_DIC_score + " > 0", null, null, null, null)) {
            while (cursor.moveToNext()) {
                ContentValues cv = new ContentValues();
                cv.put(LimeDB.FIELD_DIC_pword, cursor.getString(0));
                cv.put(LimeDB.FIELD_DIC_cword, cursor.getString(1));
                cv.put(LimeDB.FIELD_DIC_score, cursor.getInt(2));
                if (!cursor.isNull(3))
                    cv.put(Lime.DB_RELATED_COLUMN_BASESCORE, cursor.getInt(3));
                learned.add(cv);
            }
        }

        // Reset IM Info
        owner.deleteAll(Lime.DB_RELATED);

        owner.holdDBConnection();

        // Load data from DB File
        LimeDB.db.execSQL("attach database ? as sourceDB", new Object[] { sourcedbfile.getAbsolutePath() });
        LimeDB.db.execSQL("insert into " + Lime.DB_RELATED + " select * from sourceDB." + Lime.DB_RELATED);
        LimeDB.db.execSQL("detach database sourceDB");
        restoreLearnedRelated(learned);
        owner.unHoldDBConnection();
        owner.userStore().resyncAllRelated();
        return true;
    }

    // 新字庫已有同一組 pword/cword 者只套回 score,沒有者整列補回
    void restoreLearnedRelated(List<ContentValues> learned) {
        LimeDB.db.beginTransaction();
        try {
            for (ContentValues cv : learned) {
                String pword = cv.getAsString(LimeDB.FIELD_DIC_pword);
                String cword = cv.getAsString(LimeDB.FIELD_DIC_cword);
                ContentValues score = new ContentValues();
                score.put(LimeDB.FIELD_DIC_score, cv.getAsInteger(LimeDB.FIELD_DIC_score));
                int updated = (cword == null)
                        ? LimeDB.db.update(Lime.DB_RELATED, score, LimeDB.FIELD_DIC_pword + " = ? AND " + LimeDB.FIELD_DIC_cword + " IS NULL",
                                new String[] { pword })
                        : LimeDB.db.update(Lime.DB_RELATED, score, LimeDB.FIELD_DIC_pword + " = ? AND " + LimeDB.FIELD_DIC_cword + " = ?",
                                new String[] { pword, cword });
                if (updated == 0)
                    LimeDB.db.insert(Lime.DB_RELATED, null, cv);
            }
            LimeDB.db.setTransactionSuccessful();
        } finally {
            LimeDB.db.endTransaction();
        }
    }

    /*
     * Backup learned user scores and phrases from the specified table to the backup
     * table.
     * Jeremy '15,5,21
     */
    public void deleteRelatedPhrase(String pword, String cword) {
        if (!owner.checkDBConnection())
            return;
        LimeDB.db.delete(Lime.DB_RELATED, LimeDB.FIELD_DIC_pword + " = ? AND " + LimeDB.FIELD_DIC_cword + " = ?", new String[] { pword, cword });
        owner.userStore().syncRelated(pword, cword);
    }

    /**
     * Check if usesr dictionary record exists
     */
    public Mapping isRelatedPhraseExist(String pword, String cword) {

        long startTime = 0;
        if (DEBUG || LimeDB.probePerformance) {
            startTime = System.currentTimeMillis();
            Log.i(TAG, "isRelatedPhraseExist(): pword='" + pword + ", cword=" + cword);
        }
        if (!owner.checkDBConnection())
            return null;
        Mapping munit = null;

        // SQLiteDatabase db = this.getSqliteDb(true);
        try {
            munit = isRelatedPhraseExistOnDB(LimeDB.db, pword, cword);

        } catch (Exception e) {

            e.printStackTrace();
        }

        if (DEBUG || LimeDB.probePerformance) {

            Log.i(TAG, "isRelatedPhraseExist(): time elapsed = " + (System.currentTimeMillis() - startTime));
        }

        return munit;
    }

    /**
     * Jeremy '12/4/16 core of isUserDictExist()
     */
    Mapping isRelatedPhraseExistOnDB(SQLiteDatabase db, String pword, String cword) {

        Mapping munit = null;
        if (pword != null && !pword.trim().equals("")) {
            Cursor cursor;

            if (cword == null || cword.trim().equals("")) {
                cursor = db.query(Lime.DB_RELATED, null, LimeDB.FIELD_DIC_pword + " = ?"
                        + " AND " + LimeDB.FIELD_DIC_cword + " IS NULL", new String[]{pword}, null, null, null, null);
            } else {
                cursor = db.query(Lime.DB_RELATED, null, LimeDB.FIELD_DIC_pword + " = ?"
                        + " AND " + LimeDB.FIELD_DIC_cword + " = ?", new String[]{pword, cword}, null, null, null, null);
            }

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        munit = new Mapping();
                        munit.setId(cursor.getString(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_ID)));
                        munit.setPword(cursor.getString(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_PWORD)));
                        munit.setWord(cursor.getString(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_CWORD)));
                        munit.setBasescore(cursor.getInt(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_BASESCORE)));
                        munit.setScore(cursor.getInt(cursor.getColumnIndexOrThrow(Lime.DB_RELATED_COLUMN_USERSCORE)));
                        munit.setRelatedPhraseRecord();

                    }
                } finally {
                    cursor.close();
                }
            }

        }
        return munit;
    }
}
