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
 * 輸入法資訊（im 表）與鍵盤設定（keyboard 表）的讀寫。
 * 從 LimeDB 原樣搬出，LimeDB 對應的公開方法只轉呼叫這裡（鎖仍在 LimeDB 的 synchronized 上）。
 */
final class ImInfoStore {
    private static final boolean DEBUG = LimeDB.DEBUG;
    private static final String TAG = LimeDB.TAG;

    private final LimeDB owner;

    ImInfoStore(LimeDB owner) {
        this.owner = owner;
    }

    /**
     *
     */
    public void resetImInfo(String im) {
        // Jeremy '12,5,1
        if (!owner.checkDBConnection())
            return;
        LimeDB.db.execSQL("DELETE FROM im WHERE code=?", new Object[] { im });

    }

    /**
     *
     */
    public String getImInfo(String im, String field) {
        // Jeremy '12,5,1 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return "";

        String iminfo = "";
        try {
            // String value = "";
            String selectString = "SELECT * FROM im WHERE code=? AND title=?";

            Cursor cursor = LimeDB.db.rawQuery(selectString, new String[] { im, field });

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int descCol = cursor.getColumnIndex("desc");
                    iminfo = cursor.getString(descCol);
                }
                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return iminfo;
    }

    /**
     *
     */
    public void removeImInfo(String im, String field) {
        if (DEBUG)
            Log.i(TAG, "removeImInfo()");
        if (!owner.checkDBConnection())
            return;
        try {
            removeImInfoOnDB(LimeDB.db, im, field);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Jeremy '12,6,7 for working with OnUpgrade() before db is created
     */
    void removeImInfoOnDB(SQLiteDatabase dbin, String im, String field) {
        if (DEBUG)
            Log.i(TAG, "removeImInfoOnDB()");
        dbin.execSQL("DELETE FROM im WHERE code=? AND title=?", new Object[] { im, field });

    }

    /**
     *
     */
    public void setImInfo(String im, String field, String value) {
        // Jeremy '12,4,17 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return;

        ContentValues cv = new ContentValues();
        cv.put("code", im);
        cv.put("title", field);
        cv.put("desc", value);

        removeImInfo(im, field);

        LimeDB.db.insert("im", null, cv);

    }

    public List<Im> getImList(String code) {

        if (!owner.checkDBConnection())
            return null;

        List<Im> result = null;
        try {
            // SQLiteDatabase db = this.getSqliteDb(true);
            Cursor cursor = LimeDB.db.query("im", null, Lime.DB_IM_COLUMN_CODE + " = ?", new String[] { code }, null, null,
                    "code ASC", null);
            result = Im.getList(cursor);
            cursor.close();
        } catch (Exception e) {
            Log.i(TAG, "getIm(): Cannot get IM : " + e);
        }
        return result;
    }

    public List<ImObj> getImList() {
        if (DEBUG)
            Log.i(TAG, "getIMList()");
        // Jeremy '12,5,1 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return null;

        List<ImObj> result = new LinkedList<>();
        try {
            // SQLiteDatabase db = this.getSqliteDb(true);
            Cursor cursor = LimeDB.db.query("im", null, null, null, null, null, "code ASC", null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                        if (title.equals("keyboard")) {
                            ImObj kobj = new ImObj();
                            kobj.setCode(cursor.getString(cursor.getColumnIndexOrThrow("code")));
                            String kb = cursor.getString(cursor.getColumnIndexOrThrow("keyboard"));
                            if (kb != null) {
                                if (kb.contains("dayi")) {
                                    kb = "dayi";
                                } else if (kb.contains("phonetic") || kb.contains("hsu") || kb.contains("et26") || kb.contains("et41")) {
                                    kb = "phonetic";
                                }
                            }
                            kobj.setKeyboard(kb);
                            result.add(kobj);
                        }
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }

        } catch (Exception e) {
            Log.i(TAG, "getImList(): Cannot get IM List : " + e);
        }
        return result;
    }

    public KeyboardObj getKeyboardObj(String keyboard) {

        // Jeremy '12,5,1 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return null;

        if (keyboard == null || keyboard.equals(""))
            return null;
        KeyboardObj kobj = null;

        try {
            Cursor cursor = LimeDB.db.query("keyboard", null, LimeDB.FIELD_CODE + " = ?", new String[]{keyboard}, null, null,
                    null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    kobj = new KeyboardObj();
                    kobj.setCode(cursor.getString(cursor.getColumnIndexOrThrow("code")));
                    kobj.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                    kobj.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("desc")));
                    kobj.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                    kobj.setImage(cursor.getString(cursor.getColumnIndexOrThrow("image")));
                    kobj.setImkb(cursor.getString(cursor.getColumnIndexOrThrow("imkb")));
                    kobj.setImshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("imshiftkb")));
                    kobj.setEngkb(cursor.getString(cursor.getColumnIndexOrThrow("engkb")));
                    kobj.setEngshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("engshiftkb")));
                    kobj.setSymbolkb(cursor.getString(cursor.getColumnIndexOrThrow("symbolkb")));
                    kobj.setSymbolshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("symbolshiftkb")));
                    kobj.setDefaultkb(cursor.getString(cursor.getColumnIndexOrThrow("defaultkb")));
                    kobj.setDefaultshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("defaultshiftkb")));
                    kobj.setExtendedkb(cursor.getString(cursor.getColumnIndexOrThrow("extendedkb")));
                    kobj.setExtendedshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("extendedshiftkb")));
                }

                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return kobj;
    }

    public String getKeyboardInfo(String keyboardCode, String field) {
        if (DEBUG)
            Log.i(TAG, "getKeyboardInfo()");
        if (!owner.checkDBConnection())
            return null;
        String info = null;
        try {
            info = getKeyboardInfoOnDB(LimeDB.db, keyboardCode, field);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;

    }

    /**
     * Jeremy '12,6,7 for working with OnUpgrade() before db is created
     */
    String getKeyboardInfoOnDB(SQLiteDatabase dbin, String keyboardCode, String field) {
        if (DEBUG)
            Log.i(TAG, "getKeyboardInfoOnDB()");

        String info = null;
        // 綁定參數不能是 null；舊寫法的 code = 'null' 本來就查不到
        if (keyboardCode == null)
            return null;

        Cursor cursor = dbin.query("keyboard", null, LimeDB.FIELD_CODE + " = ?", new String[] { keyboardCode }, null, null, null,
                null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                info = cursor.getString(cursor.getColumnIndexOrThrow(field));
            }
            cursor.close();
        }
        if (DEBUG)
            Log.i(TAG, "getKeyboardInfoOnDB() info = " + info);

        return info;
    }

    public List<KeyboardObj> getKeyboardList() {

        // Jeremy '12,5,1 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return null;

        List<KeyboardObj> result = new LinkedList<>();
        try {
            // SQLiteDatabase db = this.getSqliteDb(true);
            Cursor cursor = LimeDB.db.query("keyboard", null, null, null, null, null, "name ASC", null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        KeyboardObj kobj = new KeyboardObj();
                        kobj.setCode(cursor.getString(cursor.getColumnIndexOrThrow("code")));
                        kobj.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                        kobj.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("desc")));
                        kobj.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                        kobj.setImage(cursor.getString(cursor.getColumnIndexOrThrow("image")));
                        kobj.setImkb(cursor.getString(cursor.getColumnIndexOrThrow("imkb")));
                        kobj.setImshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("imshiftkb")));
                        kobj.setEngkb(cursor.getString(cursor.getColumnIndexOrThrow("engkb")));
                        kobj.setEngshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("engshiftkb")));
                        kobj.setSymbolkb(cursor.getString(cursor.getColumnIndexOrThrow("symbolkb")));
                        kobj.setSymbolshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("symbolshiftkb")));
                        kobj.setDefaultkb(cursor.getString(cursor.getColumnIndexOrThrow("defaultkb")));
                        kobj.setDefaultshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("defaultshiftkb")));
                        kobj.setExtendedkb(cursor.getString(cursor.getColumnIndexOrThrow("extendedkb")));
                        kobj.setExtendedshiftkb(cursor.getString(cursor.getColumnIndexOrThrow("extendedshiftkb")));
                        result.add(kobj);
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void setIMKeyboard(String im, String value,
            String keyboard) {
        if (DEBUG)
            Log.i(TAG, "setIMKeyboard() im=" + im + " value= " + value + " keyboard= " + keyboard);
        if (!owner.checkDBConnection())
            return;
        try {
            setIMKeyboardOnDB(LimeDB.db, im, value, keyboard);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Jeremy '12,6,7 for working with OnUpgrade() before db is created
     */
    void setIMKeyboardOnDB(SQLiteDatabase dbin, String im, String value, String keyboard) {
        if (DEBUG)
            Log.i(TAG, "setIMKeyboardOnDB()");
        ContentValues cv = new ContentValues();
        cv.put("code", im);
        cv.put("title", "keyboard");
        cv.put("desc", value);
        cv.put("keyboard", keyboard);

        removeImInfoOnDB(dbin, im, "keyboard");

        dbin.insert("im", null, cv);
    }

    public String getKeyboardCode(String im) {
        // Jeremy '12,5,1 !owner.checkDBConnection() when db is restoring or replaced.
        if (!owner.checkDBConnection())
            return "";

        Cursor cursor = null;
        try {
            String selectString = "SELECT * FROM im WHERE code=? AND title='keyboard'";
            // SQLiteDatabase db = this.getSqliteDb(true);

            cursor = LimeDB.db.rawQuery(selectString, new String[] { im });

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                int descCol = cursor.getColumnIndex("keyboard");

                return cursor.getString(descCol);
            }

        } catch (Exception ignored) {
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return "";
    }

    public List<Keyboard> getKeyboard() {
        List<Keyboard> result = new ArrayList<>();
        if (!owner.checkDBConnection())
            return result;

        // Filter to only show Dayi English and Phonetic keyboards
        String selection = Lime.DB_KEYBOARD_COLUMN_CODE + " = ? OR " + Lime.DB_KEYBOARD_COLUMN_CODE + " = ?";
        String[] selectionArgs = { "dayi", "phonetic" };
        Cursor cursor = LimeDB.db.query(Lime.DB_KEYBOARD, null, selection, selectionArgs,
                null, null, Lime.DB_KEYBOARD_COLUMN_NAME + " ASC");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Keyboard r = Keyboard.get(cursor);
            result.add(r);
            cursor.moveToNext();
        }
        cursor.close();

        return result;
    }

    public List<Im> getIm(String code, String type) {

        List<Im> result = new ArrayList<>();
        if (!owner.checkDBConnection())
            return result;

        Cursor cursor;
        String query = null;
        if (code != null && code.length() > 1) {
            query = Lime.DB_IM_COLUMN_CODE + "='" + code + "'";
        }
        if (type != null && type.length() > 1) {
            if (query != null) {
                query += " AND ";
            } else {
                query = "";
            }

            query += " " + Lime.DB_IM_COLUMN_TITLE + "='" + type + "'";
        }

        cursor = LimeDB.db.query(Lime.DB_IM,
                null, query,
                null, null, null, Lime.DB_IM_COLUMN_DESC + " ASC");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Im r = Im.get(cursor);
            result.add(r);
            cursor.moveToNext();
        }
        cursor.close();

        return result;
    }

    public Keyboard getImKeyboard(String code) {
        if (!owner.checkDBConnection())
            return null;

        Cursor cursor = LimeDB.db.query(Lime.DB_IM, null,
                Lime.DB_IM_COLUMN_CODE + " = ? AND " + Lime.DB_IM_COLUMN_TITLE + " = ?",
                new String[]{code, Lime.IM_TYPE_KEYBOARD}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String keyboardCode = cursor.getString(cursor.getColumnIndexOrThrow(Lime.DB_IM_COLUMN_KEYBOARD));
            cursor.close();

            // Get the full keyboard object
            List<Keyboard> keyboards = getKeyboard();
            for (Keyboard kb : keyboards) {
                if (kb.getCode().equals(keyboardCode)) {
                    return kb;
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }
}
