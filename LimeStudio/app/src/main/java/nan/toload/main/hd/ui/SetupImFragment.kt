/*
 * Copyright 2024 The LimeIME Open Source Project
 */

package nan.toload.main.hd.ui

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nan.toload.main.hd.DBServer
import nan.toload.main.hd.Lime
import nan.toload.main.hd.R
import nan.toload.main.hd.data.Im
import nan.toload.main.hd.global.LIMEPreferenceManager
import nan.toload.main.hd.global.LIMEUtilities
import nan.toload.main.hd.limedb.LimeDB
import java.util.HashMap

class SetupImFragment : Fragment() {

    // IM Log Tag
    private val TAG = "SetupImFragment"
    
    // UI State
    private var uiState by mutableStateOf(SetupUiState())

    private var imlist: List<Im>? = null
    
    // Basic
    private lateinit var handler: SetupImHandler
    private lateinit var progress: ProgressDialog
    private var connManager: ConnectivityManager? = null
    private lateinit var datasource: LimeDB
    private lateinit var DBSrv: DBServer
    private lateinit var activityRef: Activity
    private lateinit var mLIMEPref: LIMEPreferenceManager

    data class SetupUiState(
        val version: String = "0.0",
        val showSystemSettings: Boolean = true,
        val showImPicker: Boolean = false,
        val showPermissionGrant: Boolean = false,
        val showImList: Boolean = false,
        val isBackupRestoreEnabled: Boolean = false,
        val isImportEnabled: Boolean = false,
        val customTableImportLabel: String? = null,
        val isCustomTableImported: Boolean = false, // To control bold/italic
        val isPhoneticImported: Boolean = false,
        val isDayiImported: Boolean = false
    )

    companion object {
        private const val ARG_SECTION_NUMBER = "section_number"

        @JvmStatic
        fun newInstance(sectionNumber: Int): SetupImFragment {
            return SetupImFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityRef = requireActivity()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        datasource = LimeDB(activityRef)
        handler = SetupImHandler(this)
        
        progress = ProgressDialog(activityRef)
        progress.max = 100
        progress.setCancelable(false)

        DBSrv = DBServer(activityRef)
        mLIMEPref = LIMEPreferenceManager(activityRef)

        connManager = activityRef.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        updateVersionInfo()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    SetupScreen()
                }
            }
        }
    }

    @Composable
    fun SetupScreen() {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .verticalScroll(scrollState)
        ) {
            
            // Version
            Text(
                text = uiState.version,
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.bodyMedium
            )

            // Setup Wizard Section
            if (!uiState.showImList) {
                SetupWizardSection()
            }

            // IM List Section (Database, Import, Download)
            if (uiState.showImList) {
               ImListSection()
            }
        }
    }

    @Composable
    fun SetupWizardSection() {
        Column {
            if (uiState.showSystemSettings) {
                Text(
                    text = stringResource(R.string.setup_im_system_settings),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.setup_im_system_settings_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { LIMEUtilities.showInputMethodSettingsPage(activityRef.applicationContext) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Text(stringResource(R.string.setup_im_wizard_nextstep))
                }
            }

            if (uiState.showImPicker) {
                 Text(
                    text = stringResource(R.string.setup_im_system_impicker_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                 Button(
                    onClick = { 
                        LIMEUtilities.showInputMethodPicker(activityRef.applicationContext)
                        // Invalidate? Compose state should verify on resume
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Text(stringResource(R.string.setup_im_system_selectLIME))
                }
            }

            if (uiState.showPermissionGrant) {
                Text(
                    text = stringResource(R.string.setup_im_grant_permission),
                    style = MaterialTheme.typography.bodyMedium
                )
                 Button(
                    onClick = { 
                        // Modern Android file access ready
                        Log.d(TAG, "File access ready for modern Android")
                        // Enable buttons locally for immediate feedback (though check() will override)
                         uiState = uiState.copy(
                            isBackupRestoreEnabled = true,
                            isImportEnabled = true
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Text(stringResource(R.string.setup_im_grant_permission_button))
                }
            }
        }
    }
    
    @Composable
    fun ImListSection() {
        Column {
            // Database Management
            Text(
                text = stringResource(R.string.setup_im_database),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                text = stringResource(R.string.setup_im_database_description),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showAlertDialog(Lime.BACKUP, Lime.LOCAL, getString(R.string.l3_initial_backup_confirm)) },
                    enabled = uiState.isBackupRestoreEnabled,
                    modifier = Modifier.weight(1f).padding(end = 5.dp)
                ) {
                    Text(stringResource(R.string.setup_im_backup_local))
                }
                Button(
                    onClick = { showAlertDialog(Lime.RESTORE, Lime.LOCAL, getString(R.string.l3_initial_restore_confirm)) },
                    enabled = uiState.isBackupRestoreEnabled,
                    modifier = Modifier.weight(1f).padding(start = 5.dp)
                ) {
                    Text(stringResource(R.string.setup_im_restore_local))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Import
            Text(
                text = stringResource(R.string.setup_im_import),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.setup_im_import_description),
                style = MaterialTheme.typography.bodyMedium
            )
             Row(modifier = Modifier.fillMaxWidth()) {
                val customLabel = uiState.customTableImportLabel ?: stringResource(R.string.setup_im_import_standard)
                Button(
                    onClick = { 
                        val ft = parentFragmentManager.beginTransaction()
                        val dialog = SetupImLoadDialog.newInstance(Lime.DB_TABLE_CUSTOM, handler)
                        dialog.show(ft, "loadimdialog") 
                    },
                    enabled = uiState.isImportEnabled,
                    modifier = Modifier.weight(1f).padding(end = 5.dp)
                ) {
                    Text(
                        text = customLabel, 
                        fontWeight = if(uiState.isCustomTableImported) FontWeight.Normal else FontWeight.Bold
                        // Alpha handling is usually done via enabled state or color, sticking to bold vs normal for now
                    )
                }
                Button(
                    onClick = { 
                         val ft = parentFragmentManager.beginTransaction()
                        val dialog = SetupImLoadDialog.newInstance(Lime.DB_RELATED, handler)
                        dialog.show(ft, "loadimdialog")
                    },
                    enabled = uiState.isImportEnabled,
                    modifier = Modifier.weight(1f).padding(start = 5.dp)
                ) {
                    Text(stringResource(R.string.setup_im_import_related))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Download (Pre-built tables)
            Text(
                text = stringResource(R.string.setup_im_download),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.setup_im_download_description),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { 
                        val ft = parentFragmentManager.beginTransaction()
                        val dialog = SetupImLoadDialog.newInstance(Lime.DB_TABLE_PHONETIC, handler)
                        dialog.show(ft, "loadimdialog") 
                    },
                    enabled = uiState.isImportEnabled, // Assuming same permission logic
                    modifier = Modifier.weight(1f).padding(end = 5.dp)
                ) {
                     Text(
                        text = stringResource(R.string.im_phonetic), 
                        fontWeight = if(uiState.isPhoneticImported) FontWeight.Normal else FontWeight.Bold
                    )
                }
                Button(
                    onClick = { 
                           val ft = parentFragmentManager.beginTransaction()
                        val dialog = SetupImLoadDialog.newInstance(Lime.DB_TABLE_DAYI, handler)
                        dialog.show(ft, "loadimdialog") 
                    },
                    enabled = uiState.isImportEnabled, // Assuming same logic
                    modifier = Modifier.weight(1f).padding(start = 5.dp)
                ) {
                     Text(
                        text = stringResource(R.string.im_dayi), 
                        fontWeight = if(uiState.isDayiImported) FontWeight.Normal else FontWeight.Bold
                    )
                }
            }

        }
    }


    private fun updateVersionInfo() {
        try {
            val pInfo = activityRef.packageManager.getPackageInfo(activityRef.packageName, 0)
            uiState = uiState.copy(version = "v${pInfo.versionName} - ${pInfo.versionCode}")
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }


    override fun onPause() {
        super.onPause()
        imlist?.let {
            if (it.isNotEmpty()) {
                mLIMEPref.syncIMActivatedState(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initialbutton()
    }

    // Public API for Handler
    fun initialbutton() {
        val check = HashMap<String, String>()
        
        // Load Menu Item
        if (!DBSrv.isDatabseOnHold) {
            try {
                imlist = datasource.getIm(null, Lime.IM_TYPE_NAME)
                imlist?.forEach { im ->
                    check[im.code] = im.desc
                }
                
                // Update Sync
                imlist?.let { mLIMEPref.syncIMActivatedState(it) }
                
                // State Logic
                val context = activityRef.applicationContext
                val isLimeEnabled = LIMEUtilities.isLIMEEnabled(context)
                val isLimeActive = LIMEUtilities.isLIMEActive(context)
                
                // Permission logic (simplified for modern Android)
                // In original code, it checked "if (true)" for > API 23 permissions, so effectively always true/granted
                // unless legacy logic applied. We'll stick to true.
                val hasPermission = true 
                
                var showSettings = false
                var showImPicker = false
                var showGrant = false
                var showList = false
                
                if (isLimeEnabled) {
                     showSettings = false
                     showList = true
                     
                     if (isLimeActive) {
                         // Active and Ready
                         showImPicker = false
                     } else {
                         // Enabled but not selected
                         showImPicker = true
                     }
                     
                     showGrant = !hasPermission
                } else {
                    showSettings = true
                    showImPicker = false
                    showGrant = false
                    showList = false
                }
                
                val buttonsEnabled = if(isLimeEnabled) {
                    if(isLimeActive) true 
                    else hasPermission
                } else false


                // Check import status for UI Styling
                val customLabel = check[Lime.DB_TABLE_CUSTOM]
                val isPhonetic = check[Lime.DB_TABLE_PHONETIC] != null
                val isDayi = check[Lime.DB_TABLE_DAYI] != null
                
                uiState = uiState.copy(
                    showSystemSettings = showSettings,
                    showImPicker = showImPicker,
                    showPermissionGrant = showGrant,
                    showImList = showList,
                    isBackupRestoreEnabled = buttonsEnabled,
                    isImportEnabled = buttonsEnabled,
                    customTableImportLabel = customLabel,
                    isCustomTableImported = customLabel != null,
                    isPhoneticImported = isPhonetic,
                    isDayiImported = isDayi
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun showProgress(spinnerStyle: Boolean, message: String?) {
        if (progress.isShowing) progress.dismiss()

        progress = ProgressDialog(activityRef)
        progress.setCancelable(false)
        progress.setProgressStyle(if (spinnerStyle) ProgressDialog.STYLE_SPINNER else ProgressDialog.STYLE_HORIZONTAL)
        message?.let { progress.setMessage(it) }
        if (!spinnerStyle) progress.progress = 0

        progress.show()
    }

    fun cancelProgress() {
        if (progress.isShowing) {
            progress.dismiss()
            handler.initialImButtons()
        }
    }

    fun setProgressIndeterminate(flag: Boolean) {
        progress.isIndeterminate = flag
    }

    fun updateProgress(value: Int) {
        progress.progress = value
    }

    fun updateProgress(value: String) {
        progress.setMessage(value)
    }

    fun showToastMessage(msg: String, length: Int) {
        Toast.makeText(activityRef, msg, length).show()
    }

    fun updateCustomButton() {
        // Original code updated text. Compose triggers state update on initialbutton() refresh usually.
        // Or we can manually trigger a localized refresh if needed.
        // For simplicity, just re-run initialbutton logic if the handler calls this
        initialbutton()
    }

    fun resetImTable(imtable: String, backuplearning: Boolean) {
        try {
            if (backuplearning) {
                datasource.backupUserRecords(imtable)
            }
            DBSrv.resetMapping(imtable)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun finishProgress(imtype: String?) {
        cancelProgress()
    }
    
    fun showAlertDialog(action: String?, type: String, message: String) {
         MaterialAlertDialogBuilder(activityRef)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.dialog_confirm)) { dialog, id ->
                if (action != null) {
                    if (action.equals(Lime.BACKUP, ignoreCase = true)) {
                        if (type.equals(Lime.LOCAL, ignoreCase = true)) {
                            // backupLocalDrive(); // Was commented out in original
                        }
                    } else if (action.equals(Lime.RESTORE, ignoreCase = true)) {
                        if (type.equals(Lime.LOCAL, ignoreCase = true)) {
                            // restoreLocalDrive(); // Was commented out in original
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { dialog, id -> }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Payment functionality removed
    }

}
