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

package net.toload.main.hd.limesettings;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import net.toload.main.hd.ComposeBridge;
import net.toload.main.hd.DBServer;
import net.toload.main.hd.R;
import net.toload.main.hd.SearchServer;
import net.toload.main.hd.data.KeyboardObj;
import net.toload.main.hd.global.LIMEPreferenceManager;

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

        // Create simple frame layout for Compose
        setContentView(R.layout.activity_settings_m3);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_preference);
        }

        this.SearchSrv = new SearchServer(this);

        // Use Compose Settings Screen instead of Fragment
        android.widget.FrameLayout container = findViewById(R.id.settings_container);
        if (container != null) {
            android.view.View settingsView = ComposeBridge.INSTANCE.createSettingsView(this, this);
            if (settingsView != null) {
                try {
                    container.addView(settingsView);
                } catch (Exception e) {
                    android.util.Log.e("LIME_PREF", "FAILED to add settingsView: " + e.getMessage(), e);
                }
            }
        }

        // Set up preference change listener for keyboard type handling
        setupPreferenceChangeListener();
    }

    /**
     * Set up preference change listener to handle phonetic keyboard type changes.
     * This logic was previously in PrefsFragment.onSharedPreferenceChanged()
     */
    private void setupPreferenceChangeListener() {
        LIMEPreferenceManager mLIMEPref = new LIMEPreferenceManager(this);
        DBServer DBSrv = new DBServer(this);

        SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key != null && key.equals("phonetic_keyboard_type")) {
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
                        } else if (selectedPhoneticKeyboardType.equals("hsu")) {
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
                    } catch (RemoteException e) {
                        Log.e("LIMEPreferenceHC", "Failed to update phonetic keyboard: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                // Trigger backup manager for preference changes
                BackupManager backupManager = new BackupManager(LIMEPreferenceHC.this);
                backupManager.dataChanged();
            }
        });
    }

    // PrefsFragment removed - now using Compose SettingsScreen
    // Preference change logic moved to setupPreferenceChangeListener()
}
