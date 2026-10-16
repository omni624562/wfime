package net.toload.main.hd.limedb

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import net.toload.main.hd.global.LIMEPreferenceManager

/**
 * Android 自動備份：要備份哪些檔由 res/xml 的規則決定（只有 user.db 與智慧選字紀錄）。
 * 這裡只多做兩件事：設定關掉時什麼都不備份；備份期間鎖住 user.db 讓檔案一致。
 * 還原完成後留下標記，下次啟動時把還原回來的學習資料套回 lime.db。
 */
class UserDataBackupAgent : BackupAgent() {

    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) {
        // 只用整份檔案備份（fullBackupOnly），不用 key-value 備份
    }

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) {
    }

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (!LIMEPreferenceManager(this).autoBackupUserData) {
            Log.i(TAG, "auto backup turned off, skipping")
            return
        }
        UserDataStore.runLocked { super.onFullBackup(data) }
    }

    override fun onRestoreFinished() {
        LIMEPreferenceManager(this).setUserDataRestored(true)
    }

    companion object {
        private const val TAG = "UserDataBackupAgent"
    }
}
