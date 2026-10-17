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
 * 學習：選字加分、新增字碼，以及從 user.db 套回學習資料。
 * 從 LimeDB 原樣搬出，LimeDB 對應的公開方法只轉呼叫這裡（鎖仍在 LimeDB 的 synchronized 上）。
 */
final class LearningStore {
    private static final boolean DEBUG = LimeDB.DEBUG;
    private static final String TAG = LimeDB.TAG;

    private final LimeDB owner;

    LearningStore(LimeDB owner) {
        this.owner = owner;
    }

    /**
     * Add new mapping into current table
     */
    // Jeremy '11, 7, 31 add new phrase mapping into current table (for LD phrase
    // learning).
    public void addOrUpdateMappingRecord(String table, String code, String word, int score) {
        // String code = preProcessingRemappingCode(raw_code); //Jeremy '12,6,4 the code
        // is build from mapping.getcode() should not do remap again.
        if (DEBUG)
            Log.i(TAG, "addOrUpdateMappingRecord(), code = '" + code + "'. word=" + word + ", score =" + score);
        // Jeremy '12,4,17 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return;

        try {
            // 表名來自呼叫端(例如還原學習紀錄時的輸入法代碼),會串進 SQL,先驗證
            table = LimeDB.validateTableName(table);
            Mapping munit = owner.isMappingExistOnDB(LimeDB.db, table, code, word);
            ContentValues cv = new ContentValues();

            if (munit == null) {
                if (code.length() > 0 && word.length() > 0) {

                    cv.put(LimeDB.FIELD_CODE, code);
                    if (table.equals("phonetic")) {
                        String noToneCode = code.replaceAll("[ 3467]", "");
                        cv.put(LimeDB.FIELD_NO_TONE_CODE, noToneCode);// Jeremy '12,6,1, add missing space
                    }
                    cv.put(LimeDB.FIELD_WORD, word);
                    cv.put(LimeDB.FIELD_SCORE, (score == -1) ? 1 : score);
                    LimeDB.db.insert(table, null, cv);

                    if (DEBUG)
                        Log.i(TAG, "addOrUpdateMappingRecord(): mapping does not exist, new record inserted");
                }

            } else {// the item exist in preload related database.

                int newScore = (score == -1) ? munit.getScore() + 1 : score;
                cv.put(LimeDB.FIELD_SCORE, newScore);
                LimeDB.db.update(table, cv, LimeDB.FIELD_ID + " = ?", new String[] { munit.getId() });
                if (DEBUG)
                    Log.i(TAG, "addOrUpdateMappingRecord(): mapping exist, update score on existing record; score:"
                            + score);
            }
            owner.userStore().syncMapping(table, code, word);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Add score to the mapping item
     */
    public void addScore(Mapping srcunit) {

        // Jeremy '12,4,17 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return;

        // Jeremy '11,7,31 even selected from realted list, udpate the corresponding
        // score in im table.
        // Jeremy '11,6,12 Id=null denotes selection from related list in im table
        // Jeremy '11,9,8 query highest score first. Erase relatedlist if new score is
        // not highest.
        try {

            if (srcunit != null && srcunit.getWord() != null &&
                    !srcunit.getWord().trim().equals("")) {

                if (DEBUG)
                    Log.i(TAG, "addScore(): addScore on word:" + srcunit.getWord());

                if (srcunit.isRelatedPhraseRecord()) {

                    int score;
                    if (owner.relatedscore.get(srcunit.getId()) == null) {
                        score = srcunit.getScore() + 1;
                        owner.relatedscore.put(srcunit.getId(), score);
                    } else {
                        score = owner.relatedscore.get(srcunit.getId()) + 1;
                        owner.relatedscore.put(srcunit.getId(), score);
                    }

                    ContentValues cv = new ContentValues();
                    cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, score);
                    LimeDB.db.update(Lime.DB_RELATED, cv, LimeDB.FIELD_ID + " = ?", new String[] { srcunit.getId() });
                    owner.userStore().syncRelatedById(Long.parseLong(srcunit.getId()));

                    // Log.i("TAG RELATED B", srcunit.getId() + " : Related ADD Score :" + score);

                } else {
                    int newScore = srcunit.getScore() + 1;
                    if (srcunit.getCode() != null && !srcunit.getCode().trim().equals("")) {
                        try {
                            Cursor maxCursor = LimeDB.db.rawQuery("SELECT MAX(" + LimeDB.FIELD_SCORE + ") FROM " + owner.tablename 
                                    + " WHERE " + LimeDB.FIELD_CODE + " = ?", new String[] { srcunit.getCode() });
                            if (maxCursor != null) {
                                if (maxCursor.moveToFirst()) {
                                    int maxScore = maxCursor.getInt(0);
                                    if (maxScore >= newScore) {
                                        newScore = maxScore + 1;
                                    }
                                }
                                maxCursor.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    ContentValues cv = new ContentValues();
                    cv.put(LimeDB.FIELD_SCORE, newScore);
                    // Jeremy 11',7,29 update according to word instead of ID, may have multiple
                    // records mathing word but with diff code/id
                    LimeDB.db.update(owner.tablename, cv, LimeDB.FIELD_WORD + " = ?", new String[] { srcunit.getWord() });
                    owner.userStore().syncMappingWord(owner.tablename, srcunit.getWord());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 匯入字庫後把 user.db 裡這個輸入法學過的字碼套回:字庫已有的列取較高分,沒有的整列補上。
     * 回傳套回筆數;user.db 還沒搬過回傳 -1(呼叫端改用舊的 table_user 備份表)。
     */
    public int restoreLearnedFromUserData(String imtype) {
        if (!owner.checkDBConnection())
            return -1;
        String table = LimeDB.validateTableName(imtype);
        List<Word> learned = owner.userStore().learnedMapping(table);
        if (learned == null)
            return -1;
        LimeDB.db.beginTransaction();
        try {
            for (Word w : learned) {
                LimeDB.db.execSQL("UPDATE " + table + " SET " + LimeDB.FIELD_SCORE + " = MAX(" + LimeDB.FIELD_SCORE + ", ?) WHERE "
                        + LimeDB.FIELD_CODE + " = ? AND " + LimeDB.FIELD_WORD + " = ?",
                        new Object[] { w.getScore(), w.getCode(), w.getWord() });
                if (!isMappingExist(table, w.getCode(), w.getWord())) {
                    ContentValues cv = new ContentValues();
                    cv.put(LimeDB.FIELD_CODE, w.getCode());
                    if (table.equals("phonetic"))
                        cv.put(LimeDB.FIELD_NO_TONE_CODE, w.getCode().replaceAll("[ 3467]", ""));
                    cv.put(LimeDB.FIELD_WORD, w.getWord());
                    cv.put(LimeDB.FIELD_SCORE, w.getScore());
                    LimeDB.db.insert(table, null, cv);
                }
            }
            LimeDB.db.setTransactionSuccessful();
        } finally {
            LimeDB.db.endTransaction();
        }
        return learned.size();
    }

    /**
     * 匯入「我的資料」後套回:已載入的輸入法(表裡有字)套回字碼,關聯字全部套回
     * (已有的取較高分,沒有的補上,補上的是自建關聯字)。還沒載入的輸入法之後匯入時再套回。
     */
    public void restoreAllFromUserData() {
        if (!owner.checkDBConnection())
            return;
        for (String im : new String[] { Lime.DB_TABLE_DAYI, Lime.DB_TABLE_PHONETIC, Lime.DB_TABLE_CUSTOM }) {
            if (owner.countMapping(im) > 0)
                restoreLearnedFromUserData(im);
        }
        List<String[]> learned = owner.userStore().learnedRelated();
        if (learned == null)
            return;
        int added = 0;
        LimeDB.db.beginTransaction();
        try {
            for (String[] r : learned) {
                LimeDB.db.execSQL("UPDATE " + Lime.DB_RELATED + " SET " + LimeDB.FIELD_DIC_score + " = MAX(" + LimeDB.FIELD_DIC_score
                        + ", ?) WHERE " + LimeDB.FIELD_DIC_pword + " = ? AND " + LimeDB.FIELD_DIC_cword + " = ?",
                        new Object[] { Integer.parseInt(r[2]), r[0], r[1] });
                if (owner.isRelatedPhraseExistOnDB(LimeDB.db, r[0], r[1]) == null) {
                    ContentValues cv = new ContentValues();
                    cv.put(Lime.DB_RELATED_COLUMN_PWORD, r[0]);
                    cv.put(Lime.DB_RELATED_COLUMN_CWORD, r[1]);
                    cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, Integer.parseInt(r[2]));
                    LimeDB.db.insert(Lime.DB_RELATED, null, cv);
                    added++;
                }
            }
            LimeDB.db.setTransactionSuccessful();
        } finally {
            LimeDB.db.endTransaction();
        }
        owner.relatedscore.clear();
        if (added > 0) {
            int dictotal = Integer.parseInt(owner.mLIMEPref.getTotalUserdictRecords());
            owner.mLIMEPref.setTotalUserdictRecords(String.valueOf(dictotal + added));
        }
    }

    boolean isMappingExist(String table, String code, String word) {
        try (Cursor c = LimeDB.db.rawQuery("SELECT 1 FROM " + table + " WHERE " + LimeDB.FIELD_CODE + " = ? AND "
                + LimeDB.FIELD_WORD + " = ? LIMIT 1", new String[] { code, word })) {
            return c.moveToFirst();
        }
    }
}
