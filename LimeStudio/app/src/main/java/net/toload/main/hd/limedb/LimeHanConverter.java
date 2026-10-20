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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Art Hung
 */
public class LimeHanConverter extends SQLiteOpenHelper {

    private final static boolean DEBUG = false;
    private final static String TAG = "LimeHanConverter";

    private final static String DATABASE_NAME = "hanconvertv2.db";
    private final static int DATABASE_VERSION = 59;

    private static final String FIELD_CODE = "code";
    private static final String FIELD_WORD = "word";
    private static final String FIELD_SCORE = "score";

    public LimeHanConverter(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Count total amount of specific table
     * <p>
     * <p>
     * public int countMapping(String table) {
     * if(DEBUG)
     * Log.i(TAG,"countMapping() on table:" + table);
     * <p>
     * try {
     * SQLiteDatabase db = this.getReadableDatabase();
     * <p>
     * Cursor cursor = db.rawQuery("SELECT * FROM " + table, null);
     * if(cursor ==null) return 0;
     * int total = cursor.getCount();
     * cursor.close();
     * if(DEBUG)
     * Log.i(TAG, "countMapping" + "Table," + table + ": " + total);
     * return total;
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * return 0;
     * }
     * /**
     * Create SQLite Database and create related tables
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // ignore error when create tables
    }

    /**
     * Upgrade current database
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public int getBaseScore(String input) {
        if (DEBUG)
            Log.i(TAG, "getBaseScore()");
        int score = 0;
        if (input != null && !input.equals("")) {
            Cursor cursor;

            try {
                SQLiteDatabase db = this.getReadableDatabase();

                cursor = db.query("TCSC", null, FIELD_CODE + " = ?",
                        new String[] { input }, null, null, null, null);
                if (cursor.moveToFirst()) {
                    int scoreColumn = cursor.getColumnIndex(FIELD_SCORE);
                    score = cursor.getInt(scoreColumn);
                } else if (input.length() > 1)
                    score = 1; // phase has default score = 1

                // cursor.deactivate();
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return score;
    }

    /**
     * 簡體字：簡轉繁表（SCTC）裡轉成繁體會變、而且不在 Big5 的字（例如 论、旧）；
     * 丑、后、里這類 Big5 也有的字不算。讀不到回空集合。
     */
    public Set<String> getSimplifiedChars() {
        Set<String> result = new HashSet<>();
        try (Cursor cursor = this.getReadableDatabase().query("SCTC", new String[] { FIELD_CODE, FIELD_WORD },
                null, null, null, null, null)) {
            CharsetEncoder big5 = Charset.forName("Big5").newEncoder();
            while (cursor.moveToNext()) {
                String simplified = cursor.getString(0);
                if (simplified != null && !simplified.isEmpty() && !simplified.equals(cursor.getString(1))
                        && !big5.canEncode(simplified))
                    result.add(simplified);
            }
        } catch (Exception e) {
            Log.e(TAG, "getSimplifiedChars(): " + e.getMessage());
        }
        return result;
    }

}
