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

import net.toload.main.hd.ui.LoadingDialogHelper;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.util.List;

import net.toload.main.hd.data.Im;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.limedb.LimeDB;
import net.toload.main.hd.ui.ImportDialog;
import net.toload.main.hd.ui.SetupImHandler;
import net.toload.main.hd.ui.SetupImLoadDialog;
import android.os.RemoteException;

public class MainActivity extends AppCompatActivity {
    public static final String ARG_ADD_WORD = "arg_add_word";
    private static final int STORAGE_PERMISSION_CODE = 0;
    // private static final int STORAGE_PERMISSION_CODE = android.permission.;


    // private CharSequence mCode;

    private LimeDB datasource;
    private List<Im> imlist;

    private LIMEPreferenceManager mLIMEPref;

    // Material3 loading dialog (replacement for deprecated ProgressDialog)
    private LoadingDialogHelper progress;
    private MainActivityHandler handler;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            refreshImportStatus();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable Dynamic Colors for Material You (Android 12+)
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this.getApplication());

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);

        handler = new MainActivityHandler(this);

        progress = new LoadingDialogHelper(this);
        progress.setMax(100);
        progress.setCancelable(false);

        this.mLIMEPref = new LIMEPreferenceManager(this);


        // initial imlist
        initialImList();

        // 自動備份剛還原時，先把學習資料套回（輸入法服務還沒啟動前就開設定的情況）
        new Thread(() -> net.toload.main.hd.limedb.UserDataBackup.applyRestoredIfNeeded(getApplicationContext()),
                "UserDataRestore").start();



        // Handle Import Text from other application
        // 只在第一次建立時處理；旋轉、切換語言等重建 Activity 時 intent 不變，不重複處理（已顯示的對話框由系統自動還原）
        Intent intent = getIntent();
        String action = savedInstanceState == null ? intent.getAction() : null;
        String type = getIntent().getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(getIntent());
            }
        }




        // Handle back button press using OnBackPressedDispatcher (Android 13+
        // predictive back support)
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // 設定首頁（Compose）：手機有標題列與「使用說明」選單，平板不顯示標題列
        boolean isTablet = this.getResources().getConfiguration().screenWidthDp >= 600;
        android.view.View settingsView = ComposeBridge.INSTANCE.createSettingsView(this, this, !isTablet,
                this::showHelpDialog);
        if (settingsView != null) {
            setContentView(settingsView);
        }
    }



    void handleSendText(Intent intent) {
        String importtext = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (importtext != null && !importtext.isEmpty()) {
            androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ImportDialog dialog = ImportDialog.newInstance(importtext);
            dialog.show(ft, "importdialog");
        }
    }

    public void initialImList() {
        if (datasource != null) return;
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            LimeDB db = new LimeDB(this);
            List<Im> list = db.getIm(null, Lime.IM_TYPE_NAME);
            runOnUiThread(() -> {
                datasource = db;
                imlist = list;
            });
        });
    }



    public void showToastMessage(String msg, int length) {
        Toast toast = Toast.makeText(this, msg, length);
        toast.show();
    }

    public void showProgress() {
        if (!progress.isShowing()) {
            progress.show();
        }
    }

    public void cancelProgress() {
        if (progress.isShowing()) {
            progress.dismiss();
        }
    }

    public void updateProgress(int value) {
        if (progress.isShowing()) {
            progress.setProgress(value);
        }
    }

    public void updateProgress(String value) {
        if (progress.isShowing()) {
            progress.setMessage(value);
        }
    }

    public void initialDefaultPreference() {
    }

    public void downloadPhonetic() {
        showSetupImLoadDialog(Lime.DB_TABLE_PHONETIC);
    }

    public void downloadDayi() {
        showSetupImLoadDialog(Lime.DB_TABLE_DAYI);
    }

    public void importRelated() {
        showSetupImLoadDialog(Lime.DB_RELATED);
    }

    private void showSetupImLoadDialog(String imtype) {
        androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SetupImHandler setupImHandler = new SetupImHandler(this);
        SetupImLoadDialog dialog = SetupImLoadDialog.newInstance(imtype, setupImHandler);
        dialog.show(ft, "loadimdialog");
    }

    public void showProgress(boolean spinnerStyle, String message) {
        if (progress.isShowing()) {
            progress.dismiss();
        }
        progress.setIndeterminate(spinnerStyle);
        if (message != null) {
            progress.setMessage(message);
        }
        if (!spinnerStyle) {
            progress.setProgress(0);
        }
        progress.show();
    }

    public void setProgressIndeterminate(boolean flag) {
        progress.setIndeterminate(flag);
    }

    public void finishProgress(String imtype) {
        cancelProgress();
        refreshImportStatus();
    }

    public void resetImTable(String imtable, boolean backuplearning) {
        try {
            if (backuplearning) {
                datasource.backupUserRecords(imtable);
            } else {
                // 不保留學習紀錄:user.db 裡這個輸入法學過的字碼也一起清掉
                net.toload.main.hd.limedb.UserDataStore.get(this).clearIm(imtable);
            }
            DBServer dbSrv = new DBServer(this);
            dbSrv.resetMapping(imtable);
            refreshImportStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshImportStatus() {
        runOnUiThread(() -> {
            try {
                net.toload.main.hd.ui.compose.settings.SettingsViewModel settingsViewModel =
                        new androidx.lifecycle.ViewModelProvider(this,
                                new net.toload.main.hd.ui.compose.settings.SettingsViewModelFactory(this))
                                .get(net.toload.main.hd.ui.compose.settings.SettingsViewModel.class);
                if (settingsViewModel != null) {
                    settingsViewModel.refreshImportStatus();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void showHelpDialog() {
        runOnUiThread(() -> {
            try {
                net.toload.main.hd.ui.HelpDialog helpDialog = new net.toload.main.hd.ui.HelpDialog();
                helpDialog.show(getSupportFragmentManager(), "helpDialog");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
