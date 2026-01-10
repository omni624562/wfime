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

package nan.toload.main.hd.limesettings;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import nan.toload.main.hd.DBServer;
import nan.toload.main.hd.R;
import nan.toload.main.hd.SearchServer;
import nan.toload.main.hd.data.KeyboardObj;
import nan.toload.main.hd.global.LIMEPreferenceManager;

public class LIMEPreferenceHC extends androidx.appcompat.app.AppCompatActivity {

    private SearchServer SearchSrv = null;

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.SearchSrv != null)
            this.SearchSrv.initialCache();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Create simple frame layout for fragment
        setContentView(R.layout.activity_settings_m3);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_preference);
        }

        this.SearchSrv = new SearchServer(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new PrefsFragment())
                    .commit();
        }
    }

    public static class PrefsFragment extends androidx.preference.PreferenceFragmentCompat
            implements OnSharedPreferenceChangeListener {
        private final boolean DEBUG = false;
        private final String TAG = "LIMEPreferenceHC";
        private Context ctx = null;
        private DBServer DBSrv = null;
        private LIMEPreferenceManager mLIMEPref = null;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preference, rootKey);

            ctx = requireContext();
            mLIMEPref = new LIMEPreferenceManager(ctx);
            DBSrv = new DBServer(ctx);
        }

        @Override
        public void onDisplayPreferenceDialog(@androidx.annotation.NonNull androidx.preference.Preference preference) {
            // Handle MultiListPreference with custom dialog fragment
            if (preference instanceof MultiListPreference) {
                final androidx.fragment.app.DialogFragment dialogFragment = MultiListPreferenceDialogFragmentCompat
                        .newInstance(preference.getKey());
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(),
                        "MultiListPreferenceDialogFragmentCompat");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (DEBUG)
                Log.i(TAG, "onSharedPreferenceChanged(), key:" + key);

            if (key.equals("phonetic_keyboard_type")) {
                String selectedPhoneticKeyboardType = mLIMEPref.getPhoneticKeyboardType();
                try {
                    KeyboardObj kobj = DBSrv.getKeyboardObj("phonetic");

                    if (selectedPhoneticKeyboardType.equals("standard")) {
                        kobj = DBSrv.getKeyboardObj("phonetic");
                    } else if (selectedPhoneticKeyboardType.equals("eten")) {
                        kobj = DBSrv.getKeyboardObj("phoneticet41");
                    } else if (selectedPhoneticKeyboardType.equals("eten26")) {
                        if (mLIMEPref.getParameterBoolean("number_row_in_english", false)) {
                            kobj = DBSrv.getKeyboardObj("limenum");
                        } else {
                            kobj = DBSrv.getKeyboardObj("lime");
                        }
                    } else if (selectedPhoneticKeyboardType.equals("eten26_symbol")) {
                        kobj = DBSrv.getKeyboardObj("et26");
                    } else if (selectedPhoneticKeyboardType.equals("hsu")) { // Jeremy '12,7,6 Add HSU english keyboard
                                                                             // support
                        if (mLIMEPref.getParameterBoolean("number_row_in_english", false)) {
                            kobj = DBSrv.getKeyboardObj("limenum");
                        } else {
                            kobj = DBSrv.getKeyboardObj("lime");
                        }
                    } else if (selectedPhoneticKeyboardType.equals("hsu_symbol")) {
                        kobj = DBSrv.getKeyboardObj("hsu");
                    }
                    if (kobj != null && DBSrv != null) {
                        DBSrv.setIMKeyboard("phonetic", kobj.getDescription(), kobj.getCode());
                    }

                    if (DEBUG && DBSrv != null)
                        Log.i(TAG, "onSharedPreferenceChanged() PhoneticIMInfo.kyeboard:" +
                                DBSrv.getImInfo("phonetic", "keyboard"));
                } catch (RemoteException e) {
                    Log.i(TAG, "onSharedPreferenceChanged(), WriteIMinfo for selected phonetic keyboard failed!!");
                    e.printStackTrace();
                }
            }
            BackupManager backupManager = new BackupManager(ctx);
            backupManager.dataChanged(); // Jeremy '12,4,29 call backup manager to backup the changes.
        }
    }
}
