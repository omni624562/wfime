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

package net.toload.main.hd.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.toload.main.hd.DBServer;
import net.toload.main.hd.Lime;
import net.toload.main.hd.R;
import net.toload.main.hd.data.Word;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.LIMEProgressListener;
import net.toload.main.hd.global.LIMEUtilities;
import net.toload.main.hd.limedb.LimeDB;
import net.toload.main.hd.limesettings.LIMESelectFileRecyclerAdapter;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */

/**
 * A placeholder fragment containing a simple view.
 */
public class SetupImLoadDialog extends DialogFragment {

    static final Comparator<File> SORT_FILENAME = new Comparator<File>() {
        public int compare(File e1, File e2) {
            return e2.getName().compareTo(e1.getName());
        }
    };
    private static final String IM_TYPE = "IM_TYPE";
    // IM Log Tag
    private final String TAG = "SetupImLoadDialog";
    // Default
    Button btnSetupImDialogCustom;
    Button btnSetupImDialogLoad1;
    Button btnSetupImDialogLoad2;
    Button btnSetupImDialogLoad3;
    Button btnSetupImDialogLoad4;
    Button btnSetupImDialogLoad5;
    Button btnSetupImDialogLoad6;
    Button btnSetupImDialogCancel;
    CheckBox chkSetupImBackupLearning;
    CheckBox chkSetupImRestoreLearning;
    List<File> flist;
    // Basic
    private SetupImHandler handler;
    private ConnectivityManager connManager;
    private String imtype = null;
    private int imcount = 0;
    private SetupImLoadDialog frgdialog;
    private LimeDB datasource;
    private DBServer DBSrv = null;
    private Activity activity;
    private LIMEPreferenceManager mLIMEPref;
    // Select File
    private LIMESelectFileRecyclerAdapter adapter;
    private RecyclerView listview;
    private LinearLayout toplayout;
    private Thread loadthread;

    public SetupImLoadDialog() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SetupImLoadDialog newInstance(String imtype, SetupImHandler handler) {
        SetupImLoadDialog frg = new SetupImLoadDialog();
        Bundle args = new Bundle();
        args.putString(IM_TYPE, imtype);
        frg.setArguments(args);
        frg.setHandler(handler);
        return frg;
    }

    public void setHandler(SetupImHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(android.content.DialogInterface dialog,
                    int keyCode, android.view.KeyEvent event) {
                if ((keyCode == android.view.KeyEvent.KEYCODE_BACK)) {
                    // To dismiss the fragment when the back-button is pressed.
                    dismiss();
                    return true;
                }
                // Otherwise, do nothing else
                else
                    return false;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        frgdialog = this;
        imtype = getArguments().getString(IM_TYPE);
        activity = getActivity();
        datasource = new LimeDB(activity);
        DBSrv = new DBServer(activity);
        mLIMEPref = new LIMEPreferenceManager(activity);

        connManager = (ConnectivityManager) activity.getSystemService(
                Context.CONNECTIVITY_SERVICE);

        imcount = datasource.count(imtype);

        View rootView = inflater.inflate(R.layout.fragment_dialog_im, container, false);

        chkSetupImBackupLearning = rootView.findViewById(R.id.chkSetupImBackupLearning);
        chkSetupImRestoreLearning = rootView.findViewById(R.id.chkSetupImRestoreLearning);

        btnSetupImDialogCustom = rootView.findViewById(R.id.btnSetupImDialogCustom);
        btnSetupImDialogLoad1 = rootView.findViewById(R.id.btnSetupImDialogLoad1);
        btnSetupImDialogLoad2 = rootView.findViewById(R.id.btnSetupImDialogLoad2);
        btnSetupImDialogLoad3 = rootView.findViewById(R.id.btnSetupImDialogLoad3);
        btnSetupImDialogLoad4 = rootView.findViewById(R.id.btnSetupImDialogLoad4);
        btnSetupImDialogLoad5 = rootView.findViewById(R.id.btnSetupImDialogLoad5);
        btnSetupImDialogLoad6 = rootView.findViewById(R.id.btnSetupImDialogLoad6);

        if (imtype.equalsIgnoreCase(Lime.DB_RELATED)) {

            getDialog().getWindow().setTitle(getResources().getString(R.string.setup_im_related_title));

            btnSetupImDialogCustom.setText(getResources().getString(R.string.setup_im_import_related_default));
            btnSetupImDialogCustom.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(activity)
                        .setMessage(
                                activity.getResources().getString(R.string.setup_im_import_related_default_confirm))
                        .setPositiveButton(activity.getResources().getString(R.string.dialog_confirm),
                                (dialog, which) -> {
                                    loadDefaultRelated();
                                    handler.initialImButtons();
                                    dismiss();
                                })
                        .setNegativeButton(activity.getResources().getString(R.string.dialog_cancel),
                                (dialog, which) -> dialog.dismiss())
                        .show();
            });

            btnSetupImDialogLoad1.setText(getResources().getString(R.string.setup_im_import_related));
            btnSetupImDialogLoad1.setOnClickListener(v -> {
                selectMappingFile();
                handler.initialImButtons();
                dismiss();
            });

            // btnSetupImDialogLoad1.setVisibility(View.GONE);
            btnSetupImDialogLoad2.setVisibility(View.GONE);
            btnSetupImDialogLoad3.setVisibility(View.GONE);
            btnSetupImDialogLoad4.setVisibility(View.GONE);
            btnSetupImDialogLoad5.setVisibility(View.GONE);
            btnSetupImDialogLoad6.setVisibility(View.GONE);
            chkSetupImBackupLearning.setVisibility(View.GONE);
            chkSetupImRestoreLearning.setVisibility(View.GONE);

        } else {

            // Display remove IM confirm dialog
            if (imcount > 0) {

                getDialog().getWindow().setTitle(getResources().getString(R.string.setup_im_dialog_title_remove));

                btnSetupImDialogLoad1.setText(getResources().getString(R.string.setup_im_dialog_remove));
                btnSetupImDialogLoad1.setOnClickListener(v -> {
                    new MaterialAlertDialogBuilder(activity)
                            .setMessage(activity.getResources()
                                    .getString(R.string.setup_im_dialog_remove_confirm_message))
                            .setPositiveButton(activity.getResources().getString(R.string.dialog_confirm),
                                    (dialog, which) -> {
                                        boolean backuplearning = chkSetupImBackupLearning.isChecked();
                                        handler.resetImTable(imtype, backuplearning);
                                        if (imtype.equals(Lime.DB_TABLE_CUSTOM)) {
                                            handler.updateCustomButton();
                                        }
                                        handler.initialImButtons();
                                        dismiss();
                                        frgdialog.dismiss();
                                    })
                            .setNegativeButton(activity.getResources().getString(R.string.dialog_cancel),
                                    (dialog, which) -> dialog.dismiss())
                            .show();
                });
                btnSetupImDialogLoad2.setVisibility(View.GONE);
                btnSetupImDialogLoad3.setVisibility(View.GONE);
                btnSetupImDialogLoad4.setVisibility(View.GONE);
                btnSetupImDialogLoad5.setVisibility(View.GONE);
                btnSetupImDialogLoad6.setVisibility(View.GONE);
                btnSetupImDialogCustom.setVisibility(View.GONE);

                chkSetupImBackupLearning.setVisibility(View.VISIBLE);
                chkSetupImRestoreLearning.setVisibility(View.GONE);

            } else {

                // Display Import IM dialog

                // Remove Backup Learning Data Checkbox
                chkSetupImBackupLearning.setVisibility(View.GONE);

                // Display Restore User Preference Checkbox
                chkSetupImRestoreLearning.setVisibility(View.VISIBLE);

                getDialog().getWindow().setTitle(getResources().getString(R.string.setup_im_dialog_title));

                // Check permission for > API 23
                btnSetupImDialogCustom.setEnabled(ContextCompat.checkSelfPermission(this.getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

                btnSetupImDialogCustom.setOnClickListener(v -> {
                    selectMappingFile();
                    handler.initialImButtons();
                    dismiss();
                });

                if (imtype.equals(Lime.DB_TABLE_PHONETIC)) {

                    btnSetupImDialogLoad1.setText(
                            getResources().getString(R.string.l3_im_download_from_phonetic_big5) + " (15,945)");
                    btnSetupImDialogLoad1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            downloadAndLoadIm(Lime.DB_TABLE_PHONETIC, Lime.IM_PHONETIC_BIG5);
                        }
                    });
                    btnSetupImDialogLoad2
                            .setText(getResources().getString(R.string.l3_im_download_from_phonetic) + " (34,838)");
                    btnSetupImDialogLoad2
                            .setOnClickListener(v -> downloadAndLoadIm(Lime.DB_TABLE_PHONETIC, Lime.IM_PHONETIC));
                    btnSetupImDialogLoad3.setText(
                            getResources().getString(R.string.l3_im_download_from_phonetic_adv_big5) + " (76,122)");
                    btnSetupImDialogLoad3.setOnClickListener(
                            v -> downloadAndLoadIm(Lime.DB_TABLE_PHONETIC, Lime.IM_PHONETIC_ADV_BIG5));
                    btnSetupImDialogLoad4
                            .setText(getResources().getString(R.string.l3_im_download_from_phonetic_adv) + " (95,029)");
                    btnSetupImDialogLoad4
                            .setOnClickListener(v -> downloadAndLoadIm(Lime.DB_TABLE_PHONETIC, Lime.IM_PHONETIC_ADV));

                    btnSetupImDialogLoad5.setVisibility(View.GONE);
                    btnSetupImDialogLoad6.setVisibility(View.GONE);
                } else if (imtype.equals(Lime.DB_TABLE_DAYI)) {

                    btnSetupImDialogLoad1
                            .setText(getResources().getString(R.string.setup_load_download_dayiuni) + " (27,198)");
                    btnSetupImDialogLoad1
                            .setOnClickListener(v -> downloadAndLoadIm(Lime.DB_TABLE_DAYI, Lime.IM_DAYIUNI));
                    btnSetupImDialogLoad2
                            .setText(getResources().getString(R.string.setup_load_download_dayiunip) + " (117,766)");
                    btnSetupImDialogLoad2
                            .setOnClickListener(v -> downloadAndLoadIm(Lime.DB_TABLE_DAYI, Lime.IM_DAYIUNIP));
                    btnSetupImDialogLoad3
                            .setText(getResources().getString(R.string.l3_im_download_from_dayi) + " (18,638)");
                    btnSetupImDialogLoad3.setOnClickListener(v -> downloadAndLoadIm(Lime.DB_TABLE_DAYI, Lime.IM_DAYI));
                    btnSetupImDialogLoad4.setVisibility(View.GONE);
                    btnSetupImDialogLoad5.setVisibility(View.GONE);
                    btnSetupImDialogLoad6.setVisibility(View.GONE);
                } else {
                    btnSetupImDialogLoad1.setVisibility(View.GONE);
                    btnSetupImDialogLoad2.setVisibility(View.GONE);
                    btnSetupImDialogLoad3.setVisibility(View.GONE);
                    btnSetupImDialogLoad4.setVisibility(View.GONE);
                    btnSetupImDialogLoad5.setVisibility(View.GONE);
                    btnSetupImDialogLoad6.setVisibility(View.GONE);
                }
            }
        }

        btnSetupImDialogCancel = rootView.findViewById(R.id.btnSetupImDialogCancel);
        btnSetupImDialogCancel.setOnClickListener(v -> dismiss());

        return rootView;
    }

    public void selectMappingFile() {

        final Dialog dialog = new Dialog(activity);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.target);
        dialog.setCancelable(false);
        Button button = dialog.findViewById(R.id.btn_loading_sync_cancel);
        button.setOnClickListener(v -> dialog.dismiss());

        listview = dialog.findViewById(R.id.listview_loading_target);
        listview.setHasFixedSize(true);
        listview.setLayoutManager(new LinearLayoutManager(activity));
        toplayout = dialog.findViewById(R.id.linearlayout_loading_confirm_top);

        // Use app-specific directory instead of deprecated external storage
        File startDir = activity.getExternalFilesDir(null);
        if (startDir == null)
            startDir = activity.getFilesDir();

        flist = getAvailableFiles(startDir.getAbsolutePath());
        adapter = new LIMESelectFileRecyclerAdapter(activity, flist, position -> {
            File f = flist.get(position);
            if (f.isDirectory()) {
                flist = getAvailableFiles(f.getAbsolutePath());
                adapter.updateItems(flist);
                createNavigationButtons(f);
            } else {
                getAvailableFiles(f.getAbsolutePath());
                dialog.dismiss();
            }
        });
        listview.setAdapter(adapter);

        createNavigationButtons(startDir);
        dialog.show();
    }

    private void createNavigationButtons(final File dir) {

        // Clean Top Area
        toplayout.removeAllViews();

        // Create Navigation Buttons
        String path = dir.getAbsolutePath();
        String[] pathlist = path.split("\\/");

        String pathconstruct = "/";
        if (pathlist.length > 0) {
            for (String p : pathlist) {
                if (!p.equals("") && !p.equals("/")) {
                    pathconstruct += p + "/";
                } else {
                    p = "/";
                }
                final String actpath = pathconstruct;
                Button b = new Button(activity);
                b.setText(p);
                b.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                b.setOnClickListener(arg0 -> {
                    createNavigationButtons(new File(actpath));
                    flist = getAvailableFiles(actpath);
                    adapter.updateItems(flist);
                });

                toplayout.addView(b);
            }
        } else {
            // final String actpath = pathconstruct;
            Button b = new Button(activity);
            b.setText("/");
            b.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            b.setOnClickListener(arg0 -> {
                createNavigationButtons(new File("/"));
                flist = getAvailableFiles("/");
                adapter.updateItems(flist);
            });
            toplayout.addView(b);
            flist = getAvailableFiles("/");
            adapter.updateItems(flist);
        }
    }

    public void downloadAndLoadIm(String code, String type) {

        boolean restorelearning = chkSetupImRestoreLearning.isChecked();

        NetworkCapabilities caps = connManager.getNetworkCapabilities(connManager.getActiveNetwork());
        boolean isConnected = caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        if (isConnected) {

            String url = null;

            if (type.equals(Lime.IM_DAYI)) {
                url = Lime.DATABASE_CLOUD_IM_DAYI;
            } else if (type.equals(Lime.IM_DAYIUNI)) {
                url = Lime.DATABASE_CLOUD_IM_DAYIUNI;
            } else if (type.equals(Lime.IM_DAYIUNIP)) {
                url = Lime.DATABASE_CLOUD_IM_DAYIUNIP;
            } else if (type.equals(Lime.IM_PHONETIC_BIG5)) {
                url = Lime.DATABASE_CLOUD_IM_PHONETIC_BIG5;
            } else if (type.equals(Lime.IM_PHONETIC_ADV_BIG5)) {
                url = Lime.DATABASE_CLOUD_IM_PHONETICCOMPLETE_BIG5;
            } else if (type.equals(Lime.IM_PHONETIC)) {
                url = Lime.DATABASE_CLOUD_IM_PHONETIC;
            } else if (type.equals(Lime.IM_PHONETIC_ADV)) {
                url = Lime.DATABASE_CLOUD_IM_PHONETICCOMPLETE;
            }
            loadthread = new Thread(new SetupImLoadRunnable(getActivity(), handler, code, type, url, restorelearning));
            loadthread.start();

            dismiss();
        } else {
            showToastMessage(getResources().getString(R.string.l3_tab_initial_error), Toast.LENGTH_LONG);
        }
    }

    public void showToastMessage(String msg, int length) {
        Toast toast = Toast.makeText(activity, msg, length);
        toast.show();
    }

    private List<File> getAvailableFiles(String path) {

        List<File> templist = new ArrayList<File>();
        List<File> list = new ArrayList<File>();
        File check = new File(path);

        if (check.exists() && check.isDirectory()) {

            for (File f : check.listFiles()) {
                if (f.canRead()) {
                    if (!f.isDirectory()) {
                        if (imtype.equalsIgnoreCase(Lime.DB_RELATED)) {
                            if ((f.getName().toLowerCase().startsWith(Lime.DB_RELATED) &&
                                    f.getName().toLowerCase().endsWith(Lime.SUPPORT_FILE_EXT_LIMEDB))) {
                                list.add(f);
                            }
                        } else {
                            if (f.getName().toLowerCase().endsWith(Lime.SUPPORT_FILE_EXT_TXT) ||
                                    (f.getName().toLowerCase().endsWith(Lime.SUPPORT_FILE_EXT_LIMEDB) &&
                                            f.getName().toLowerCase().startsWith(imtype))
                                    ||
                                    f.getName().toLowerCase().endsWith(Lime.SUPPORT_FILE_EXT_LIME) ||
                                    f.getName().toLowerCase().endsWith(Lime.SUPPORT_FILE_EXT_CIN)) {
                                list.add(f);
                            }
                        }
                    } else {
                        list.add(f);
                    }
                }
            }

            List<File> folders = new ArrayList<File>();
            List<File> files = new ArrayList<File>();
            for (File f : list) {
                if (f.isDirectory()) {
                    folders.add(f);
                } else {
                    files.add(f);
                }
            }

            List<File> result = new ArrayList<File>();
            Collections.sort(folders, SORT_FILENAME);
            Collections.reverse(folders);
            result.addAll(folders);
            Collections.sort(files, SORT_FILENAME);
            Collections.reverse(files);
            result.addAll(files);

            return result;

        } else {

            if (imtype.equalsIgnoreCase(Lime.DB_RELATED)) {
                loadDbRelatedMapping(check);
            } else {
                if (check.getName().toLowerCase().endsWith(Lime.SUPPORT_FILE_EXT_TXT) ||
                        check.getName().toLowerCase().endsWith(Lime.SUPPORT_FILE_EXT_LIME) ||
                        check.getName().toLowerCase().endsWith(Lime.SUPPORT_FILE_EXT_CIN)) {
                    loadMapping(check);
                } else {
                    loadDbMapping(check);
                }
            }

        }
        return templist;
    }

    public void loadDefaultRelated() {

        try {
            File relatedDbPath = activity.getDatabasePath("related.db");
            if (!relatedDbPath.getParentFile().exists()) {
                relatedDbPath.getParentFile().mkdirs();
            }
            if (relatedDbPath.exists())
                relatedDbPath.delete();

            LIMEUtilities.copyRAWFile(activity.getResources().openRawResource(R.raw.lime), relatedDbPath);

            DBSrv.importBackupRelatedDb(relatedDbPath);
            relatedDbPath.deleteOnExit();
            showToastMessage(activity.getResources().getString(R.string.setup_im_import_complete), Toast.LENGTH_LONG);
        } catch (Exception e) {
            e.printStackTrace();
            showToastMessage(activity.getResources().getString(R.string.error_import_db), Toast.LENGTH_LONG);
        }

    }

    public void loadDbRelatedMapping(File unit) {
        try {
            // unzip file
            List<String> unzipPaths = LIMEUtilities.unzip(unit.getAbsolutePath(),
                    activity.getCacheDir().getAbsolutePath(), true);
            if (unzipPaths.size() != 1) {
                showToastMessage(activity.getResources().getString(R.string.error_import_db), Toast.LENGTH_LONG);
            } else {
                File fileToImport = new File(unzipPaths.get(0));
                DBSrv.importBackupRelatedDb(fileToImport);
                fileToImport.deleteOnExit();
                showToastMessage(activity.getResources().getString(R.string.setup_im_import_complete),
                        Toast.LENGTH_LONG);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToastMessage(activity.getResources().getString(R.string.error_import_db), Toast.LENGTH_LONG);
        }
    }

    public void loadDbMapping(File unit) {

        try {
            // unzip file
            List<String> unzipPaths = LIMEUtilities.unzip(unit.getAbsolutePath(),
                    activity.getCacheDir().getAbsolutePath(), true);
            if (unzipPaths.size() != 1) {
                showToastMessage(activity.getResources().getString(R.string.error_import_db), Toast.LENGTH_LONG);
            } else {
                File fileToImport = new File(unzipPaths.get(0));
                DBSrv.importBackupDb(fileToImport.getAbsoluteFile(), imtype);
                fileToImport.deleteOnExit();
                showToastMessage(activity.getResources().getString(R.string.setup_im_import_complete),
                        Toast.LENGTH_LONG);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToastMessage(activity.getResources().getString(R.string.error_import_db), Toast.LENGTH_LONG);
        }
    }

    public void loadMapping(File unit) {

        handler.showProgress(false, activity.getResources().getString(R.string.setup_im_dialog_custom));

        try {
            DBSrv.loadMapping(unit.getAbsolutePath(), imtype, new LIMEProgressListener() {

                @Override
                public void onProgress(long percentageDone, long var2, String status) {
                    if (status != null && !status.isEmpty())
                        handler.updateProgress(status);
                    handler.updateProgress((int) percentageDone);
                }

                @Override
                public void onStatusUpdate(String status) {
                    if (status != null && !status.isEmpty())
                        handler.updateProgress(status);
                }

                @Override
                public void onError(int code, String source) {
                    if (source != null && !source.isEmpty())
                        showToastMessage(source, Toast.LENGTH_LONG);
                }

                @Override
                public void onPostExecute(boolean success, String status, int code) {

                    boolean restorelearning = chkSetupImRestoreLearning.isChecked();

                    if (restorelearning) {

                        handler.updateProgress(
                                activity.getResources().getString(R.string.setup_im_restore_learning_data));
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
                                Cursor cursorbackup = datasource.rawQuery("select * from " + backupTableName);
                                List<Word> backuplist = Word.getList(cursorbackup);
                                cursorbackup.close();

                                int progressvalue = 0;
                                int recordcount = 0;
                                int recordtotal = backuplist.size();

                                for (Word w : backuplist) {

                                    recordcount++;

                                    datasource.addOrUpdateMappingRecord(imtype, w.getCode(), w.getWord(), w.getScore());

                                    // Update Progress
                                    int progress = (int) ((double) recordcount / recordtotal * 90 + 10);

                                    if (progress != progressvalue) {
                                        progressvalue = progress;
                                        handler.updateProgress(progressvalue);
                                    }

                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            handler.updateProgress(100);
                        }
                    }

                    handler.cancelProgress();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}