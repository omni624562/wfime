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

package nan.toload.main.hd;

// TODO: Replace ProgressDialog with Material3 components (deprecated since API 26)
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nan.toload.main.hd.data.Im;
import nan.toload.main.hd.global.LIME;
import nan.toload.main.hd.global.LIMEPreferenceManager;
import nan.toload.main.hd.limedb.LimeDB;
import nan.toload.main.hd.ui.ImportDialog;
import nan.toload.main.hd.ui.ManageImFragment;
import nan.toload.main.hd.ui.ManageRelatedFragment;
import nan.toload.main.hd.ui.SetupImFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    public static final String ARG_ADD_WORD = "arg_add_word";
    private static final int STORAGE_PERMISSION_CODE = 0;
    // private static final int STORAGE_PERMISSION_CODE = android.permission.;

    /**
     * ccc
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    // private CharSequence mCode;

    private LimeDB datasource;
    private List<Im> imlist;

    private ConnectivityManager connManager;
    private LIMEPreferenceManager mLIMEPref;

    // TODO: Replace ProgressDialog with Material3 AlertDialog + CircularProgressIndicator
    // ProgressDialog is deprecated since API 26, but replacement requires custom layout
    @SuppressWarnings("deprecation")
    private ProgressDialog progress;
    private MainActivityHandler handler;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SetupImFragment ImFragment = (SetupImFragment) getSupportFragmentManager().findFragmentByTag("SetupImFragment");
        if (ImFragment == null)
            return;
        if (hasFocus && ImFragment.isVisible())
            ImFragment.initialbutton();

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

        handler = new MainActivityHandler(this);

        progress = new ProgressDialog(this);
        progress.setMax(100);
        progress.setCancelable(false);

        connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        this.mLIMEPref = new LIMEPreferenceManager(this);

        LIME.PACKAGE_NAME = getApplicationContext().getPackageName();

        // initial imlist
        initialImList();

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);
        // mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                findViewById(R.id.drawer_layout));

        // Handle Import Text from other application
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = getIntent().getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(getIntent());
            }
        } else if (Intent.ACTION_VIEW.equals(action) && type != null) {
            String scheme = intent.getScheme();
            ContentResolver resolver = getContentResolver();

            if (ContentResolver.SCHEME_CONTENT.equals(scheme)
                    || ContentResolver.SCHEME_FILE.equals(scheme)
                    || scheme.equals("http") || scheme.equals("https")) {
                Uri uri = intent.getData();
                String fileName = getContentName(resolver, uri);
                if (fileName == null) {
                    fileName = uri.getLastPathSegment();
                }
                InputStream input = null;
                try {
                    input = resolver.openInputStream(uri);
                } catch (FileNotFoundException e) {
                    Log.e("MainActivity", "Failed to open input stream: " + e.getMessage());
                }
                // Use cache directory instead of deprecated external storage
                String importFilepath = getCacheDir().getAbsolutePath() + File.separator + fileName;
                InputStreamToFile(input, importFilepath);
                showToastMessage("Got file " + importFilepath, Toast.LENGTH_SHORT);
            }

        }

        String versionstr = "";
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionstr = "v" + pInfo.versionName + " - " + pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("MainActivity", "Package not found: " + e.getMessage());
        }

        String cversion = mLIMEPref.getParameterString("current_version", "");
        if (cversion == null || cversion.isEmpty() || !cversion.equals(versionstr)) {
            mLIMEPref.setParameter("current_version", versionstr);
        }

        // Daniel: check if it's for adding new words
        if (getIntent() != null && getIntent().getStringExtra(ARG_ADD_WORD) != null) {
            String table = getIntent().getStringExtra(ARG_ADD_WORD);
            showImeAddWordDialog(table);
        }

        // Handle back button press using OnBackPressedDispatcher (Android 13+ predictive back support)
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void showImeAddWordDialog(String table) {
        for (int i = 0; i < imlist.size(); i++) {
            String im = imlist.get(i).getCode();
            if (im.equals(table)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, ManageImFragment.newInstance(i, table, true),
                                "ManageImFragment_" + table)
                        .addToBackStack("ManageImFragment_" + table)
                        .commit();
                break;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getStringExtra(ARG_ADD_WORD) != null) {
            String table = intent.getStringExtra(ARG_ADD_WORD);
            showImeAddWordDialog(table);
        }
    }

    private String getContentName(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor == null)
            return null;
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            return cursor.getString(nameIndex);
        } else {
            cursor.close();
            return null;
        }
    }

    private void InputStreamToFile(InputStream in, String file) {
        try {
            OutputStream out = new FileOutputStream(new File(file));

            int size;
            byte[] buffer = new byte[102400];

            while ((size = in.read(buffer)) != -1) {
                out.write(buffer, 0, size);
            }

            out.close();
        } catch (Exception e) {
            Log.e("MainActivity", "InputStreamToFile exception: " + e.getMessage());
        }
    }

    void handleSendText(Intent intent) {
        String importtext = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (importtext != null && !importtext.isEmpty()) {
            android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
            ImportDialog dialog = ImportDialog.newInstance(importtext);
            dialog.show(ft, "importdialog");
        }
    }

    public void initialImList() {
        if (datasource == null) {
            datasource = new LimeDB(this);
            imlist = datasource.getIm(null, Lime.IM_TYPE_NAME);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (position == 0) {

            fragmentManager.beginTransaction()
                    .replace(R.id.container, SetupImFragment.newInstance(position), "SetupImFragment")
                    .addToBackStack("SetupImFragment")
                    .commit();
        } else if (position == 1) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, ManageRelatedFragment.newInstance(position), "ManageRelatedFragment")
                    .addToBackStack("ManageRelatedFragment")
                    .commit();
        } else {
            if (imlist == null || imlist.isEmpty()) {
                imlist = datasource.getIm(null, Lime.IM_TYPE_NAME);
            }

            if (!imlist.isEmpty()) {
                int number = position - 2;
                String table = imlist.get(number).getCode();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, ManageImFragment.newInstance(position, table, false),
                                "ManageImFragment_" + table)
                        .addToBackStack("ManageImFragment_" + table)
                        .commit();
            }
        }
    }

    public void onSectionAttached(int number) {
        if (imlist == null) {
            initialImList();
        }
        if (number == 0) {
            mTitle = this.getResources().getString(R.string.default_menu_initial);
            // mCode = "initial";
        } else if (number == 1) {
            mTitle = this.getResources().getString(R.string.default_menu_related);
            // mCode = "related";
        } else {
            int position = number - 2;
            if (position >= 0 && position < imlist.size()) {
                mTitle = imlist.get(position).getDesc();
            }
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            throw new AssertionError();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
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
        if (!progress.isShowing()) {
            progress.setProgress(value);
        }
    }

    public void updateProgress(String value) {
        if (progress.isShowing()) {
            progress.setMessage(value);
        }
    }

    public void shareTo(String filepath, String type) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType(type);

        File target = new File(filepath);
        Uri targetfile = Uri.fromFile(target);
        sharingIntent.putExtra(Intent.EXTRA_STREAM, targetfile);

        sharingIntent.putExtra(Intent.EXTRA_TEXT, target.getName());
        startActivity(Intent.createChooser(sharingIntent, target.getName()));
    }

    public void initialDefaultPreference() {
    }
}
