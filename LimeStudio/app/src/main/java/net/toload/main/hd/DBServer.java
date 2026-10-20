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

package net.toload.main.hd;

import android.content.Context;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.toload.main.hd.data.KeyboardObj;
import net.toload.main.hd.data.Word;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.LIMEProgressListener;
import net.toload.main.hd.global.LIMEUtilities;
import net.toload.main.hd.limedb.LimeDB;
import net.toload.main.hd.limedb.MemoObj;

//Jeremy '12,5,1 renamed from DBServer and change from service to ordinary class.
public class DBServer {
    private static final boolean DEBUG = false;
    private static final String TAG = "LIME.DBServer";
    protected static LimeDB datasource = null; // static LIMEDB for shared LIMEDB between DBServer instances
    protected static LIMEPreferenceManager mLIMEPref = null;
    protected static Context appContext = null;
    private static String loadingTablename = ""; // Jeremy '12,6,2 change all variable to static for all instances to
                                                 // share these variables

    public DBServer(Context context) {
        DBServer.appContext = context.getApplicationContext();
        mLIMEPref = new LIMEPreferenceManager(appContext);
        if (datasource == null)
            datasource = new LimeDB(appContext);
    }

    public static void resetCache() {
        SearchServer.resetCache(true);
    }

    public void loadMapping(String filename, String tablename, LIMEProgressListener progressListener)
            {

        File sourcefile = new File(filename);
        loadMapping(sourcefile, tablename, progressListener);
    }

    public void loadMapping(File sourcefile, String tablename, LIMEProgressListener progressListener)
            {
        if (DEBUG)
            Log.i(TAG, "loadMapping() on " + loadingTablename);

        loadingTablename = tablename;

        datasource.setFinish(false);
        datasource.setFilename(sourcefile);

        // showNotificationMessage(appContext.getText(R.string.lime_setting_notification_loading)
        // + "");
        datasource.loadFileV2(tablename, progressListener);
        // datasource.close();

        // Reset for SearchSrv
        resetCache();
    }

    public void resetMapping(final String tablename) {

        if (DEBUG)
            Log.i(TAG, "resetMapping() on " + loadingTablename);

        datasource.deleteAll(tablename);

        // Reset cache in SearchSrv
        resetCache();
    }

    public boolean importBackupRelatedDb(File sourcedb) {
        return datasource.importBackupRelatedDb(sourcedb);
    }

    public boolean importBackupDb(File sourcedb, String imtype) {
        return datasource.importBackupDb(sourcedb, imtype);
    }

    public int importMapping(File compressedSourceDB, String imtype) {

        List<Word> results = null;

        // String sourcedbfile = LIME.LIME_SDCARD_FOLDER + imtype;
        List<String> unzipFilePaths = new ArrayList<>();
        try {
            unzipFilePaths = LIMEUtilities.unzip(compressedSourceDB.getAbsolutePath(),
                    new File(appContext.getCacheDir(), "limehd").getAbsolutePath(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // decompressFile(compressedSourceDB, LIME.LIME_SDCARD_FOLDER, imtype, true);
        String targetDbPath = null;
        for (String path : unzipFilePaths) {
            if (path.toLowerCase(java.util.Locale.ROOT).endsWith(".db")) {
                targetDbPath = path;
                break;
            }
        }

        if (targetDbPath == null) {
            // TODO: Process exception here.
            return -1;
        } else {
            int count = datasource.importDb(targetDbPath, imtype);
            // 解壓出的檔案匯入後即不再需要
            for (String path : unzipFilePaths)
                new File(path).delete();
            resetCache();
            return count;
        }
    }

    public String getImInfo(String im, String field) {
        // if (datasource == null) {loadLimeDB();}
        return datasource.getImInfo(im, field);
    }

    public String getKeyboardInfo(String keyboardCode, String field) {
        // if (datasource == null) {loadLimeDB();}
        return datasource.getKeyboardInfo(keyboardCode, field);
    }

    public void removeImInfo(String im, String field)
            {
        // if (datasource == null) {loadLimeDB();}
        datasource.removeImInfo(im, field);

    }

    public void resetImInfo(String im) {
        // if (datasource == null) {loadLimeDB();}
        datasource.resetImInfo(im);

    }

    public void setImInfo(String im, String field, String value)
            {
        // if (datasource == null) {loadLimeDB();}
        datasource.setImInfo(im, field, value);

    }

    public void setIMKeyboard(String im, String value,
            String keyboard) {

        datasource.setIMKeyboard(im, value, keyboard);
    }

    public String getKeyboardCode(String im)
            {
        // if (datasource == null) {loadLimeDB();}
        return datasource.getKeyboardCode(im);
    }

    public void compressFile(File sourceFile, String targetFolder, String targetFile) {
        if (DEBUG)
            Log.i(TAG, "compressFile(), srouce = " + sourceFile.toString() + "" +
                    ", target = " + targetFolder + "/" + targetFile);
        try {
            final int BUFFER = 2048;

            File targetFolderObj = new File(targetFolder);
            if (!targetFolderObj.exists()) {
                targetFolderObj.mkdirs();
            }

            File OutputFile = new File(targetFolderObj.getAbsolutePath() + File.separator + targetFile);
            OutputFile.delete();

            byte[] data = new byte[BUFFER];

            try (FileInputStream fi = new FileInputStream(sourceFile);
                 BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
                 FileOutputStream dest = new FileOutputStream(OutputFile);
                 ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest))) {
                ZipEntry entry = new ZipEntry(sourceFile.getAbsolutePath());
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);
                }
            }

            // Log.i("ART","compress Output File:"+OutputFile.getAbsolutePath() + " / " +
            // OutputFile.length());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check the consistency of phonetic keyboard setting in preference and db.
     * Jeremy '12,6,8
     */
    public void checkPhoneticKeyboardSetting() {
        datasource.checkPhoneticKeyboardSetting();
    }

    public void renameTableName(String source, String target) {
        if (datasource != null) {
            datasource.renameTableName(source, target);
        }
    }

    /**
     * Jeremy '12,7,6 get keyboard object from table name
     *
     * @param table
     * @return
     */
    public KeyboardObj getKeyboardObj(String table) {
        KeyboardObj kobj = null;
        if (datasource != null)
            kobj = datasource.getKeyboardObj(table);
        return kobj;
    }

    public long insertMemo(String content, int pinned) {
        if (datasource != null) {
            return datasource.insertMemo(content, pinned);
        }
        return -1;
    }

    public List<MemoObj> getMemos() {
        if (datasource != null) {
            return datasource.getMemos();
        }
        return new ArrayList<>();
    }

    public int deleteMemo(int id) {
        if (datasource != null) {
            return datasource.deleteMemo(id);
        }
        return -1;
    }

    public int updateMemoPin(int id, int pinned) {
        if (datasource != null) {
            return datasource.updateMemoPin(id, pinned);
        }
        return -1;
    }

    public int updateMemoContent(int id, String content) {
        if (datasource != null) {
            return datasource.updateMemoContent(id, content);
        }
        return -1;
    }

    public boolean isDatabseOnHold() {
        return datasource.isDatabseOnHold();
    }

}
