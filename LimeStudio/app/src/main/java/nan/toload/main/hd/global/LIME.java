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

package nan.toload.main.hd.global;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Global constants for LIME IME.
 * 
 * Note: Remote download URLs have been removed as the services (googlecode.com,
 * openfoundry.org)
 * are deprecated or no longer functional. All input method databases should be
 * bundled locally
 * in the assets or Database folder.
 * 
 * 注意：遠端下載 URL 已移除，因為相關服務 (googlecode.com, openfoundry.org) 已棄用或不再運作。
 * 所有輸入法資料庫應打包在 assets 或 Database 資料夾中。
 */
public class LIME {

    // ==================== Database Source Files ====================
    // These are the local database file names bundled with the app
    public static final String DATABASE_SOURCE_DAYI = "dayi.cin";
    public static final String DATABASE_SOURCE_PHONETIC = "phonetic.lime";
    public static final String DATABASE_SOURCE_PHONETIC_CNS = "bopomofo.cin";
    public static final String DATABASE_SOURCE_PHONETICADV = "phonetic_adv_CJK.lime";
    public static final String DATABASE_SOURCE_CJ = "cj.lime";
    public static final String DATABASE_SOURCE_CJ_CNS = "cangjie.cin";
    public static final String DATABASE_SOURCE_CJ5 = "cj5.lime";
    public static final String DATABASE_SOURCE_ECJ = "ecj.lime";
    public static final String DATABASE_SOURCE_SCJ = "scj.lime";
    public static final String DATABASE_SOURCE_ARRAY = "array.lime";
    public static final String DATABASE_SOURCE_ARRAY10 = "array10.lime";
    public static final String DATABASE_SOURCE_WB = "stroke5.cin";
    public static final String DATABASE_SOURCE_EZ = "ez.lime";
    public static final String DATABASE_SOURCE_PINYIN_BIG5 = "pinyinbig5.cin";
    public static final String DATABASE_SOURCE_PINYIN_GB = "pinyin.cin";
    public static final String DATABASE_SOURCE_PINYIN_LIME = "pinyin_CJK.cin";
    public static final String DATABASE_SOURCE_CJ_LIME = "cj_CJK.lime";
    public static final String DATABASE_SOURCE_ECJ_LIME = "ecj_CJK.lime";
    public static final String DATABASE_SOURCE_PHONETIC_LIME = "phonetic_CJK.lime";
    public static final String DATABASE_SOURCE_FILENAME = "lime.zip";
    public static final String DATABASE_SOURCE_FILENAME_EMPTY = "empty.zip";

    // ==================== Database Paths ====================
    public static final String DATABASE_RELATIVE_FOLDER = "/databases";
    public static final String DATABASE_NAME = "lime.db";
    public static final String DATABASE_JOURNAL = "lime.db-journal";
    public static final String DATABASE_BACKUP_NAME = "backup.zip";
    public static final String DATABASE_JOURNAL_BACKUP_NAME = "backupJournal.zip";
    public static final String SHARED_PREFS_BACKUP_NAME = "shared_prefs.bak";
    public static final String DATABASE_CLOUD_TEMP = "cloudtemp.zip";

    // ==================== Download Status ====================
    public static final String DOWNLOAD_START = "download_start";
    public static final String DATABASE_DOWNLOAD_STATUS = "database_download_status";

    // ==================== IM Status Keys ====================
    public static final String IM_CJ_STATUS = "im_cj_status";
    public static final String IM_SCJ_STATUS = "im_scj_status";
    public static final String IM_PHONETIC_STATUS = "im_phonetic_status";
    public static final String IM_DAYI_STATUS = "im_dayi_status";
    public static final String IM_CUSTOM_STATUS = "im_custom_status";
    public static final String IM_EZ_STATUS = "im_ez_status";
    public static final String IM_MAPPING_FILENAME = "im_mapping_filename";
    public static final String IM_MAPPING_VERSION = "im_mapping_version";
    public static final String IM_MAPPING_TOTAL = "im_mapping_total";
    public static final String IM_MAPPING_DATE = "im_mapping_date";

    // ==================== Preferences ====================
    public static final String CANDIDATE_SUGGESTION = "candidate_suggestion";
    public static final String TOTAL_USERDICT_RECORD = "total_userdict_record";
    public static final String LEARNING_SWITCH = "learning_switch";

    // ==================== Cache Settings ====================
    public final static int SEARCHSRV_RESET_CACHE_SIZE = 256;
    public final static int LIMEDB_CACHE_SIZE = 1024;

    // ==================== Package Info ====================
    public static String PACKAGE_NAME;

    /**
     * Get the root folder for LIME data using app-specific storage.
     * Note: This is a legacy method that uses deprecated API.
     * Consider using context.getFilesDir() instead.
     * 
     * @return Path to LIME data root folder
     * @deprecated Use getLimeDataRootFolder(Context) instead
     */
    @Deprecated
    public static String getLimeDataRootFolder() {
        return Environment.getDataDirectory() + "/data/" + LIME.PACKAGE_NAME;
    }

    /**
     * Get the LIME database folder path.
     * 
     * @return Path to LIME database folder
     * @deprecated Use getLIMEDatabaseFolder(Context) instead
     */
    @Deprecated
    public static String getLIMEDatabaseFolder() {
        return Environment.getDataDirectory() + "/data/" + LIME.PACKAGE_NAME + LIME.DATABASE_RELATIVE_FOLDER;
    }

    /**
     * Get the root folder for LIME data using app-specific storage (recommended).
     * 
     * @param context Application context
     * @return Path to LIME data root folder
     */
    public static String getLimeDataRootFolder(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    /**
     * Get the LIME database folder path using app-specific storage (recommended).
     * 
     * @param context Application context
     * @return Path to LIME database folder
     */
    public static String getLIMEDatabaseFolder(Context context) {
        File dbDir = new File(context.getFilesDir(), "databases");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        return dbDir.getAbsolutePath();
    }

    /**
     * Get the external storage folder for LIME (for backup/restore).
     * Uses app-specific external storage which doesn't require permissions on API
     * 19+.
     * 
     * @param context Application context
     * @return Path to external LIME folder, or null if not available
     */
    public static String getExternalLimeFolder(Context context) {
        File externalDir = context.getExternalFilesDir(null);
        if (externalDir != null) {
            return externalDir.getAbsolutePath();
        }
        return null;
    }
}
