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

package nan.toload.main.hd.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import nan.toload.main.hd.DBServer;
import nan.toload.main.hd.Lime;
import nan.toload.main.hd.R;
import nan.toload.main.hd.data.KeyboardObj;
import nan.toload.main.hd.data.Word;
import nan.toload.main.hd.global.LIMEPreferenceManager;
import nan.toload.main.hd.limedb.LimeDB;

public class SetupImLoadRunnable implements Runnable {
    private final static boolean DEBUG = false;
    private final static String TAG = "SetupImLoadRunnable";
    private final Activity activity;
    private final SetupImHandler handler;
    private final LimeDB datasource;
    private final LIMEPreferenceManager mLIMEPref;
    private final Context mContext;
    private final boolean restorePreference;
    // Global
    private String url = null;
    private String imtype = null;
    private String type = null;
    private DBServer dbsrv = null;

    public SetupImLoadRunnable(Activity activity, SetupImHandler handler, String imtype, String type, String url,
            boolean restorePreference) {
        this.handler = handler;
        this.imtype = imtype;
        this.type = type;
        this.url = url;
        this.activity = activity;
        this.dbsrv = new DBServer(activity);
        this.datasource = new LimeDB(activity);
        this.mLIMEPref = new LIMEPreferenceManager(activity);
        this.restorePreference = restorePreference;
        this.mContext = activity.getBaseContext();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public void run() {

        Looper.prepare();

        // Log.i("LIME", "showProgress Runnable:");
        handler.showProgress(false, activity.getResources().getString(R.string.setup_load_download));

        // Download DB File
        // handler.updateProgress(activity.getResources().getString(R.string.setup_load_download));
        File tempfile = downloadRemoteFile(mContext, url);

        if (tempfile == null || tempfile.length() < 100000) {
            // Primary download failed - log error and notify user
            Log.e(TAG, "Failed to download database from primary URL: " + url);
            handler.cancelProgress();
            return;
        }

        // Load DB
        handler.updateProgress(activity.getResources().getString(R.string.setup_load_migrate_load));
        dbsrv.importMapping(tempfile, imtype);

        mLIMEPref.setParameter("_table", "");
        // mLIMEPref.setResetCacheFlag(true);
        DBServer.resetCache();

        if (restorePreference) {
            handler.updateProgress(activity.getResources().getString(R.string.setup_im_restore_learning_data));
            handler.updateProgress(0);
            boolean check = datasource.checkBackuptable(imtype);
            handler.updateProgress(5);

            if (check) {

                String backupTableName = imtype + "_user";

                // check if user data backup table is present and have valid records
                int userRecordsCount = datasource.countMapping(backupTableName);
                handler.updateProgress(10);
                if (userRecordsCount == 0)
                    return;

                try {
                    // Load backuptable records
                    /*
                     * Cursor cursorsource = datasource.rawQuery("select * from " + imtype);
                     * List<Word> clist = Word.getList(cursorsource);
                     * cursorsource.close();
                     * 
                     * HashMap<String, Word> wordcheck = new HashMap<String, Word>();
                     * for(Word w : clist){
                     * String key = w.getCode() + w.getWord();
                     * wordcheck.put(key, w);
                     * }
                     * handler.updateProgress(20);
                     */
                    Cursor cursorbackup = datasource.rawQuery("select * from " + backupTableName);
                    List<Word> backuplist = Word.getList(cursorbackup);
                    cursorbackup.close();

                    int progressvalue = 0;
                    int recordcount = 0;
                    int recordtotal = backuplist.size();

                    for (Word w : backuplist) {

                        recordcount++;

                        datasource.addOrUpdateMappingRecord(imtype, w.getCode(), w.getWord(), w.getScore());
                        /*
                         * // update record
                         * String key = w.getCode() + w.getWord();
                         * 
                         * if(wordcheck.containsKey(key)){
                         * try{
                         * datasource.execSQL("update " + imtype + " set " + Lime.DB_COLUMN_SCORE +
                         * " = " + w.getScore()
                         * + " WHERE " + Lime.DB_COLUMN_CODE + " = '" + w.getCode() + "'"
                         * + " AND " + Lime.DB_COLUMN_WORD + " = '" + w.getWord() + "'"
                         * );
                         * }catch(Exception e){
                         * e.printStackTrace();
                         * }
                         * }else{
                         * try{
                         * Word temp = wordcheck.get(key);
                         * String insertsql = Word.getInsertQuery(imtype, temp);
                         * datasource.execSQL(insertsql);
                         * }catch(Exception e){
                         * e.printStackTrace();
                         * }
                         * }
                         */
                        // Update Progress
                        int progress = (int) ((double) recordcount / recordtotal * 90 + 10);

                        if (progress != progressvalue) {
                            progressvalue = progress;
                            handler.updateProgress(progressvalue);
                        }

                    }

                    // wordcheck.clear();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // datasource.restoreUserRecordsStep2(imtype);
                handler.updateProgress(100);
            }
        }

        handler.finishLoading(imtype);
        handler.initialImButtons();

    }

    public int migrateDb(File tempfile, String imtype) {

        List<Word> results = null;

        // Use app cache directory instead of deprecated external storage
        String cacheFolder = mContext.getCacheDir().getAbsolutePath() + File.separator;
        String sourcedbfile = cacheFolder + imtype;

        handler.updateProgress(activity.getResources().getString(R.string.setup_load_migrate_load));
        DBServer.decompressFile(tempfile, cacheFolder, imtype, true);
        SQLiteDatabase sourcedb = SQLiteDatabase.openDatabase(sourcedbfile, null, // SQLiteDatabase.OPEN_READWRITE |
                                                                                  // //redundant
                SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        results = loadWord(sourcedb, imtype);
        sourcedb.close();

        // Remove Imtype and related info
        try {
            dbsrv.resetMapping(imtype);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        int total = results.size();
        int c = 0;

        // datasource.open();
        datasource.beginTransaction();

        for (Word w : results) {
            c++;
            String insert = Word.getInsertQuery(imtype, w);
            datasource.add(insert);
            if (c % 100 == 0) {
                int p = (c * 100 / total);
                handler.updateProgress(
                        activity.getResources().getString(R.string.setup_load_migrate_import) + " " + p + "%");
            }
        }
        datasource.endTransaction();
        return results.size();

        // datasource.close();
        /*
         * try {
         * 
         * } catch (SQLException e) {
         * e.printStackTrace();
         * }
         */

        // return 0;
    }

    public List<Word> loadWord(SQLiteDatabase sourcedb, String code) {
        List<Word> result = new ArrayList<Word>();
        if (sourcedb != null && sourcedb.isOpen()) {
            Cursor cursor;
            String order = Lime.DB_COLUMN_CODE + " ASC";

            cursor = sourcedb.query(code, null, null, null, null, null, order);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Word r = Word.get(cursor);
                result.add(r);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return result;
    }

    // Removed @Deprecated methods:
    // - setImInfo() - use LimeDB.setImInfo() instead
    // - setIMKeyboard() - use LimeDB.setIMKeyboard() instead
    // - getKeyboardObj() - use LimeDB.getKeyboardObj() instead

    /*
     * Download Remote File
     */
    public File downloadRemoteFile(Context ctx, String url) {

        try {
            URL downloadUrl = new URL(url);
            URLConnection conn = downloadUrl.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();

            int size = conn.getContentLength();
            int downloadSize = 0;

            if (is == null) {
                throw new RuntimeException("stream is null");
            }

            File downloadFolder = ctx.getCacheDir();
            File downloadedFile = File.createTempFile(Lime.DATABASE_IM_TEMP, Lime.DATABASE_IM_TEMP_EXT, downloadFolder);
            downloadedFile.deleteOnExit();

            FileOutputStream fos;
            fos = new FileOutputStream(downloadedFile);

            byte[] buf = new byte[4096];
            do {
                int numread = is.read(buf);
                if (numread <= 0) {
                    break;
                }
                fos.write(buf, 0, numread);
                if (size > 0) {
                    downloadSize += 4096;
                    float percent = (float) downloadSize / (float) size;
                    percent *= 100;
                    handler.updateProgress((int) percent);
                }
                if (DEBUG)
                    Log.i(TAG, numread + "bytes download.");

            } while (true);

            is.close();

            return downloadedFile;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
