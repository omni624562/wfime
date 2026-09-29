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

package net.toload.main.hd.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import net.toload.main.hd.Lime;

public class Word {

    private int id;
    private String code;
    private String code3r;
    private String word;
    private String related;
    private int score;
    private int basescore;

    @SuppressLint("Range")
    public static Word get(Cursor cursor) {
        Word record = new Word();
        record.setId(cursor.getInt(cursor.getColumnIndex(Lime.DB_COLUMN_ID)));
        record.setCode(cursor.getString(cursor.getColumnIndex(Lime.DB_COLUMN_CODE)));
        //record.setCode3r(cursor.getString(cursor.getColumnIndex(Lime.DB_COLUMN_CODE3R)));  Jeremy '15,6,6 may not present in old db.
        record.setWord(cursor.getString(cursor.getColumnIndex(Lime.DB_COLUMN_WORD)));
        record.setRelated(cursor.getString(cursor.getColumnIndex(Lime.DB_COLUMN_RELATED)));
        record.setScore(cursor.getInt(cursor.getColumnIndex(Lime.DB_COLUMN_SCORE)));
        record.setBasescore(cursor.getInt(cursor.getColumnIndex(Lime.DB_COLUMN_BASESCORE)));
        return record;
    }

    public static List<Word> getList(Cursor cursor) {
        List<Word> list = new ArrayList<Word>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(get(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public static ContentValues getContentValues(String table, Word record) {
        ContentValues cv = new ContentValues();
        cv.put(Lime.DB_COLUMN_CODE, record.getCode());
        if (table.equals("phonetic"))
            cv.put(Lime.DB_COLUMN_CODE3R, record.getCode().replaceAll("[ 3467]", "")); //Jeremy '15,6,6. remove 3467 tone keys from code as code3r
        cv.put(Lime.DB_COLUMN_WORD, record.getWord());
        cv.put(Lime.DB_COLUMN_RELATED, record.getRelated() == null ? "" : record.getRelated());
        cv.put(Lime.DB_COLUMN_SCORE, record.getScore());
        cv.put(Lime.DB_COLUMN_BASESCORE, record.getBasescore());
        return cv;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setCode3r(String code3r) {
        this.code3r = code3r;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getRelated() {
        return related;
    }

    public void setRelated(String related) {
        this.related = related;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getBasescore() {
        return basescore;
    }

    public void setBasescore(int basescore) {
        this.basescore = basescore;
    }

}
