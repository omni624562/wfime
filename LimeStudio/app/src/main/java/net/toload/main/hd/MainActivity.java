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
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
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


    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
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
        setContentView(R.layout.activity_main);

        // Set up Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(null);

        // Hide toolbar/appbar completely on tablet to reclaim screen space
        boolean isTablet = this.getResources().getConfiguration().screenWidthDp >= 600;
        if (isTablet) {
            com.google.android.material.appbar.AppBarLayout appBar = findViewById(R.id.app_bar);
            if (appBar != null) {
                appBar.setVisibility(android.view.View.GONE);
            }
        }

        handler = new MainActivityHandler(this);

        progress = new LoadingDialogHelper(this);
        progress.setMax(100);
        progress.setCancelable(false);

        this.mLIMEPref = new LIMEPreferenceManager(this);


        // initial imlist
        initialImList();



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

        // Load Compose Settings Screen directly as requested by the user
        android.widget.FrameLayout container = findViewById(R.id.container);
        if (container != null) {
            container.removeAllViews();
            android.view.View settingsView = ComposeBridge.INSTANCE.createSettingsView(this, this);
            if (settingsView != null) {
                container.addView(settingsView);
            }
        }

        mTitle = this.getResources().getString(R.string.action_preference);
        restoreActionBar();
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



    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            throw new AssertionError();
        boolean isTablet = this.getResources().getConfiguration().screenWidthDp >= 600;
        actionBar.setDisplayShowTitleEnabled(!isTablet);
        if (!isTablet) {
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_help) {
            // Show help dialog
            net.toload.main.hd.ui.HelpDialog helpDialog = new net.toload.main.hd.ui.HelpDialog();
            helpDialog.show(getSupportFragmentManager(), "helpDialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
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
