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

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.List;

import nan.toload.main.hd.DBServer;
import nan.toload.main.hd.Lime;
import nan.toload.main.hd.R;
import nan.toload.main.hd.data.Im;
import nan.toload.main.hd.global.LIMEPreferenceManager;
import nan.toload.main.hd.global.LIMEUtilities;
import nan.toload.main.hd.limedb.LimeDB;

//admob
//google drive
/*  vpon import
import com.vpadn.ads.VpadnAdRequest;
import com.vpadn.ads.VpadnAdSize;
import com.vpadn.ads.VpadnBanner;
*/
// admob import

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */

/**
 * A placeholder fragment containing a simple rootView.
 */
public class SetupImFragment extends Fragment {

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    // IM Log Tag
    private final String TAG = "SetupImFragment";
    // Debug Flag
    private final boolean DEBUG = false;
    Button btnSetupImSystemSettings;
    Button btnSetupImSystemIMPicker;
    Button btnSetupImGrantPermission;

    // Activate LIME IM
    // Custom Import
    Button btnSetupImImportStandard;
    Button btnSetupImImportRelated;
    // Default IME
    Button btnSetupImPhonetic;
    Button btnSetupImDayi;
    // Backup Restore
    Button btnSetupImBackupLocal;
    Button btnSetupImRestoreLocal;
    List<Im> imlist;
    TextView txtVersion;
    // Basic
    private SetupImHandler handler;
    private ProgressDialog progress;
    private ConnectivityManager connManager;
    private View rootView;
    private LimeDB datasource;
    private DBServer DBSrv = null;
    private Activity activity;

    private LIMEPreferenceManager mLIMEPref;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static SetupImFragment newInstance(int sectionNumber) {
        SetupImFragment frg = new SetupImFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        frg.setArguments(args);
        return frg;
    }

    @Override
    public void onPause() {
        super.onPause();

        // Update IM pick up list items
        if (imlist != null && imlist.size() > 0) {
            mLIMEPref.syncIMActivatedState(imlist);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*
         * if(vpadnBanner != null){
         * vpadnBanner.destroy();
         * vpadnBanner = null;
         * }
         */
    }

    @Override
    public void onResume() {
        super.onResume();
        initialbutton();
    }

    public void showProgress(boolean spinnerStyle, String message) {
        if (progress.isShowing())
            progress.dismiss();

        progress = new ProgressDialog(activity);
        progress.setCancelable(false);
        progress.setProgressStyle(spinnerStyle ? ProgressDialog.STYLE_SPINNER : ProgressDialog.STYLE_HORIZONTAL);
        if (message != null)
            progress.setMessage(message);
        if (!spinnerStyle)
            progress.setProgress(0);

        progress.show();
    }

    public void cancelProgress() {
        if (progress.isShowing()) {
            progress.dismiss();
            handler.initialImButtons();
        }
    }

    public void setProgressIndeterminate(boolean flag) {
        progress.setIndeterminate(flag);
    }

    public void updateProgress(int value) {
        progress.setProgress(value);
    }

    public void updateProgress(String value) {
        if (progress != null) {
            progress.setMessage(value);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        datasource = new LimeDB(this.getActivity());

        handler = new SetupImHandler(this);

        activity = getActivity();

        progress = new ProgressDialog(activity);
        progress.setMax(100);
        progress.setCancelable(false);

        DBSrv = new DBServer(activity);
        mLIMEPref = new LIMEPreferenceManager(activity);

        connManager = (ConnectivityManager) SetupImFragment.this.activity.getSystemService(
                Context.CONNECTIVITY_SERVICE);

        rootView = inflater.inflate(R.layout.fragment_setup_im, container, false);

        btnSetupImSystemSettings = rootView.findViewById(R.id.btnSetupImSystemSetting);
        btnSetupImSystemIMPicker = rootView.findViewById(R.id.btnSetupImSystemIMPicker);
        btnSetupImGrantPermission = rootView.findViewById(R.id.btnSetupImGrantPermission);
        btnSetupImImportStandard = rootView.findViewById(R.id.btnSetupImImportStandard);
        btnSetupImImportRelated = rootView.findViewById(R.id.btnSetupImImportRelated);
        btnSetupImPhonetic = rootView.findViewById(R.id.btnSetupImPhonetic);
        btnSetupImDayi = rootView.findViewById(R.id.btnSetupImDayi);

        // Backup and Restore Setting
        btnSetupImBackupLocal = rootView.findViewById(R.id.btnSetupImBackupLocal);
        btnSetupImRestoreLocal = rootView.findViewById(R.id.btnSetupImRestoreLocal);

        btnSetupImBackupLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(Lime.BACKUP, Lime.LOCAL, getResources().getString(R.string.l3_initial_backup_confirm));
            }
        });

        btnSetupImRestoreLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog(Lime.RESTORE, Lime.LOCAL,
                        getResources().getString(R.string.l3_initial_restore_confirm));
            }
        });

        PackageInfo pInfo;
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String versionstr = "v" + pInfo.versionName + " - " + pInfo.versionCode;
            txtVersion = rootView.findViewById(R.id.txtVersion);
            txtVersion.setText(versionstr);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return rootView;
    }

    public void initialbutton() {
        HashMap<String, String> check = new HashMap<>();

        // Load Menu Item
        if (!DBSrv.isDatabseOnHold()) {
            try {
                // datasource.open();
                imlist = datasource.getIm(null, Lime.IM_TYPE_NAME);
                for (int i = 0; i < imlist.size(); i++) {
                    check.put(imlist.get(i).getCode(), imlist.get(i).getDesc());
                }

                // Update IM pick up list items
                mLIMEPref.syncIMActivatedState(imlist);

                Context ctx = getActivity().getApplicationContext();
                if (LIMEUtilities.isLIMEEnabled(getActivity().getApplicationContext())) { // LIME is activated in system
                    btnSetupImSystemSettings.setVisibility(View.GONE);
                    rootView.findViewById(R.id.setup_im_system_settings_description).setVisibility(View.GONE);
                    rootView.findViewById(R.id.SetupImList).setVisibility(View.VISIBLE);
                    if (LIMEUtilities.isLIMEActive(getActivity().getApplicationContext()) &&
                            true) { // LIME is activated, also the active Keyboard, and write storage permission is
                                    // grated
                        btnSetupImSystemIMPicker.setVisibility(View.GONE);
                        rootView.findViewById(R.id.Setup_Wizard).setVisibility(View.GONE);
                        btnSetupImBackupLocal.setEnabled(true);
                        btnSetupImRestoreLocal.setEnabled(true);
                        btnSetupImImportStandard.setEnabled(true);
                        btnSetupImImportRelated.setEnabled(true);
                    } else // LIME is activated, but not active keyboard
                    {
                        if (LIMEUtilities.isLIMEActive(getActivity().getApplicationContext())) {
                            btnSetupImSystemIMPicker.setVisibility(View.GONE);
                            rootView.findViewById(R.id.setup_im_system_impicker_description).setVisibility(View.GONE);
                        } else {
                            btnSetupImSystemIMPicker.setVisibility(View.VISIBLE);
                            rootView.findViewById(R.id.setup_im_system_impicker_description)
                                    .setVisibility(View.VISIBLE);
                        }
                        // Check permission for > API 23
                        if (true) {
                            rootView.findViewById(R.id.setup_im_grant_permission).setVisibility((View.GONE));
                            btnSetupImGrantPermission.setVisibility(View.GONE);
                            btnSetupImBackupLocal.setEnabled(true);
                            btnSetupImRestoreLocal.setEnabled(true);
                            btnSetupImImportStandard.setEnabled(true);
                            btnSetupImImportRelated.setEnabled(true);
                        } else {
                            rootView.findViewById(R.id.setup_im_grant_permission).setVisibility((View.VISIBLE));
                            btnSetupImGrantPermission.setVisibility(View.VISIBLE);
                            btnSetupImBackupLocal.setEnabled(false);
                            btnSetupImRestoreLocal.setEnabled(false);
                            btnSetupImImportStandard.setEnabled(false);
                            btnSetupImImportRelated.setEnabled(false);
                        }

                    }
                } else {
                    btnSetupImSystemSettings.setVisibility(View.VISIBLE);
                    rootView.findViewById(R.id.setup_im_system_settings_description).setVisibility(View.VISIBLE);
                    btnSetupImSystemIMPicker.setVisibility(View.GONE);
                    rootView.findViewById(R.id.setup_im_grant_permission).setVisibility((View.GONE));
                    btnSetupImGrantPermission.setVisibility(View.GONE);
                    rootView.findViewById(R.id.setup_im_system_impicker_description).setVisibility(View.GONE);
                    rootView.findViewById(R.id.SetupImList).setVisibility(View.GONE);
                }

                btnSetupImGrantPermission.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Modern Android - file access is handled through document picker
                        Log.d(TAG, "File access ready for modern Android");
                        // Enable file operations
                        btnSetupImBackupLocal.setEnabled(true);
                        btnSetupImRestoreLocal.setEnabled(true);
                        btnSetupImImportStandard.setEnabled(true);
                        btnSetupImImportRelated.setEnabled(true);
                    }
                });

                btnSetupImSystemSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LIMEUtilities.showInputMethodSettingsPage(getActivity().getApplicationContext());
                    }
                });

                btnSetupImSystemIMPicker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LIMEUtilities.showInputMethodPicker(getActivity().getApplicationContext());
                        rootView.invalidate();
                    }
                });

                if (check.get(Lime.DB_TABLE_CUSTOM) != null) {
                    btnSetupImImportStandard.setAlpha(Lime.HALF_ALPHA_VALUE);
                    btnSetupImImportStandard.setText(check.get(Lime.DB_TABLE_CUSTOM));
                    btnSetupImImportStandard.setTypeface(null, Typeface.ITALIC);
                } else {
                    btnSetupImImportStandard.setAlpha(Lime.NORMAL_ALPHA_VALUE);
                    btnSetupImImportStandard.setTypeface(null, Typeface.BOLD);
                }

                btnSetupImImportStandard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        SetupImLoadDialog dialog = SetupImLoadDialog.newInstance(Lime.DB_TABLE_CUSTOM, handler);
                        dialog.show(ft, "loadimdialog");

                    }
                });

                // User can always load new related table ...
                btnSetupImImportRelated.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        SetupImLoadDialog dialog = SetupImLoadDialog.newInstance(Lime.DB_RELATED, handler);
                        dialog.show(ft, "loadimdialog");

                    }
                });

                if (check.get(Lime.DB_TABLE_PHONETIC) != null) {
                    btnSetupImPhonetic.setAlpha(Lime.HALF_ALPHA_VALUE);
                    btnSetupImPhonetic.setTypeface(null, Typeface.ITALIC);
                } else {
                    btnSetupImPhonetic.setAlpha(Lime.NORMAL_ALPHA_VALUE);
                    btnSetupImPhonetic.setTypeface(null, Typeface.BOLD);
                }

                btnSetupImPhonetic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        SetupImLoadDialog dialog = SetupImLoadDialog.newInstance(Lime.DB_TABLE_PHONETIC, handler);
                        dialog.show(ft, "loadimdialog");
                    }
                });

                if (check.get(Lime.DB_TABLE_DAYI) != null) {
                    btnSetupImDayi.setAlpha(Lime.HALF_ALPHA_VALUE);
                    btnSetupImDayi.setTypeface(null, Typeface.ITALIC);
                } else {
                    btnSetupImDayi.setAlpha(Lime.NORMAL_ALPHA_VALUE);
                    btnSetupImDayi.setTypeface(null, Typeface.BOLD);
                }

                btnSetupImDayi.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        SetupImLoadDialog dialog = SetupImLoadDialog.newInstance(Lime.DB_TABLE_DAYI, handler);
                        dialog.show(ft, "loadimdialog");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new MaterialAlertDialogBuilder(getActivity())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showAlertDialog(final String action, final String type, String message) {

        new MaterialAlertDialogBuilder(activity)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.dialog_confirm),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (action != null) {
                                    if (action.equalsIgnoreCase(Lime.BACKUP)) {
                                        if (type.equalsIgnoreCase(Lime.LOCAL)) {
                                            // backupLocalDrive();
                                        }
                                    } else if (action.equalsIgnoreCase(Lime.RESTORE)) {
                                        if (type.equalsIgnoreCase(Lime.LOCAL)) {
                                            // restoreLocalDrive();
                                        }
                                    }
                                }
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                .show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Payment functionality removed
    }

    public void showToastMessage(String msg, int length) {
        Toast toast = Toast.makeText(activity, msg, length);
        toast.show();
    }

    public void updateCustomButton() {
        btnSetupImImportStandard.setText(getResources().getString(R.string.setup_im_load_standard));
    }

    public void resetImTable(String imtable, boolean backuplearning) {
        try {
            if (backuplearning) {
                datasource.backupUserRecords(imtable);
            }
            DBSrv.resetMapping(imtable);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void finishProgress(final String imtype) {
        cancelProgress();
    }

    private void showExplanation(String title,
            String message,
            final String permission,
            final int permissionRequestCode) {
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                })
                .show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(getActivity(),
                new String[] { permissionName }, permissionRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        // Modern Android - permissions handled through document picker
        // No explicit permission handling needed for file access
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}