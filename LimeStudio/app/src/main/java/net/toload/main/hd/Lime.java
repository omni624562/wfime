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

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Core constants for LIME IME application.
 * Contains database schema definitions, IM types, and utility methods.
 * 
 * Note: For storage paths and download URLs, see
 * {@link net.toload.main.hd.global.LIME}
 */
public class Lime {

    private static final String TAG = "Lime";

    // ==================== Database Settings ====================
    public static final String DATABASE_NAME = "lime.db";
    public static final String DATABASE_EXT = ".db";
    public static final String DATABASE_BACKUP_NAME = "backup.zip";
    public static final String DATABASE_CLOUD_TEMP = "cloudtemp.zip";

    /**
     * Get the database folder path using app-specific storage.
     * Use this instead of the deprecated hardcoded folder.
     *
     * @param context Application context
     * @return Path to database folder
     */
    public static String getDatabaseDeviceFolder(Context context) {
        File dbDir = context.getDatabasePath(DATABASE_NAME).getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs();
        }
        return dbDir != null ? dbDir.getAbsolutePath() : context.getFilesDir().getAbsolutePath();
    }

    // ==================== News ====================
    public static final String LIME_NEWS_CONTENT = "lime_news_content";
    public static final String LIME_NEWS_CONTENT_URL = "https://github.com/omni624562/ime/raw/main/Resources/Message/content.html";

    // ==================== File Constants ====================
    public static final String separator = java.io.File.separator;
    public static final String DATABASE_IM_TEMP = "temp";
    public static final String DATABASE_IM_TEMP_EXT = "zip";

    // ==================== Download URLs ====================
    // Primary: GitHub (reliable)
    public static final String DATABASE_CLOUD_URL_BASED = "https://github.com/omni624562/ime/raw/main/Database/";

    // Phonetic IM downloads
    public static final String DATABASE_CLOUD_IM_PHONETIC = DATABASE_CLOUD_URL_BASED + "phonetic.zip";
    public static final String DATABASE_CLOUD_IM_PHONETIC_BIG5 = DATABASE_CLOUD_URL_BASED + "phoneticbig5.zip";
    public static final String DATABASE_CLOUD_IM_PHONETICCOMPLETE = DATABASE_CLOUD_URL_BASED + "phoneticcomplete.zip";
    public static final String DATABASE_CLOUD_IM_PHONETICCOMPLETE_BIG5 = DATABASE_CLOUD_URL_BASED + "phoneticcompletebig5.zip";
    public static final String DATABASE_CLOUD_IM_PHONETIC_KEYBOARD = "phonetic";

    // Dayi IM downloads
    public static final String DATABASE_CLOUD_IM_DAYI = DATABASE_CLOUD_URL_BASED + "dayi.zip";
    public static final String DATABASE_CLOUD_IM_DAYIUNI = DATABASE_CLOUD_URL_BASED + "dayiuni.zip";
    public static final String DATABASE_CLOUD_IM_DAYIUNIP = DATABASE_CLOUD_URL_BASED + "dayiunip.zip";
    public static final String DATABASE_CLOUD_IM_DAYI_KEYBOARD = "dayi";

    // HS IM downloads
    public static final String DATABASE_CLOUD_IM_HS = DATABASE_CLOUD_URL_BASED + "hs.zip";
    public static final String DATABASE_CLOUD_IM_HS_V1 = DATABASE_CLOUD_URL_BASED + "hs1.zip";
    public static final String DATABASE_CLOUD_IM_HS_V2 = DATABASE_CLOUD_URL_BASED + "hs2.zip";
    public static final String DATABASE_CLOUD_IM_HS_V3 = DATABASE_CLOUD_URL_BASED + "hs3.zip";

    // ==================== Database Tables ====================
    public static final String DB_TABLE_CUSTOM = "custom";
    public static final String DB_TABLE_DAYI = "dayi";
    public static final String DB_TABLE_PHONETIC = "phonetic";
    public static final String DB_TABLE_EZ = "ez";

    // ==================== IM Type Names ====================
    public static final String IM_CUSTOM = "custom";
    public static final String IM_DAYI = "dayi";
    public static final String IM_DAYIUNI = "dayiuni";
    public static final String IM_DAYIUNIP = "dayiunip";
    public static final String IM_PHONETIC = "phonetic";
    public static final String IM_PHONETIC_BIG5 = "phonetic_big5";
    public static final String IM_PHONETIC_ADV = "phonetic_adv";
    public static final String IM_PHONETIC_ADV_BIG5 = "phonetic_adv_big5";
    public static final String IM_EZ = "ez";
    public static final String IM_HS = "hs";
    public static final String IM_HS_V1 = "hs_v1";
    public static final String IM_HS_V2 = "hs_v2";
    public static final String IM_HS_V3 = "hs_v3";

    // ==================== Database Columns ====================
    public static final String DB_COLUMN_ID = "_id";
    public static final String DB_COLUMN_CODE = "code";
    public static final String DB_COLUMN_CODE3R = "code3r";
    public static final String DB_COLUMN_WORD = "word";
    public static final String DB_COLUMN_RELATED = "related";
    public static final String DB_COLUMN_SCORE = "score";
    public static final String DB_COLUMN_BASESCORE = "basescore";

    // ==================== IM Table Schema ====================
    public static final String DB_IM = "im";
    public static final String DB_IM_COLUMN_ID = "_id";
    public static final String DB_IM_COLUMN_CODE = "code";
    public static final String DB_IM_COLUMN_TITLE = "title";
    public static final String DB_IM_COLUMN_DESC = "desc";
    public static final String DB_IM_COLUMN_KEYBOARD = "keyboard";
    public static final String DB_IM_COLUMN_DISABLE = "disable";
    public static final String DB_IM_COLUMN_SELKEY = "selkey";
    public static final String DB_IM_COLUMN_ENDKEY = "endkey";
    public static final String DB_IM_COLUMN_SPACESTYLE = "spacestyle";

    // ==================== Related Table Schema ====================
    public static final String DB_RELATED = "related";
    public static final String DB_RELATED_COLUMN_ID = "_id";
    public static final String DB_RELATED_COLUMN_PWORD = "pword";
    public static final String DB_RELATED_COLUMN_CWORD = "cword";
    public static final String DB_RELATED_COLUMN_BASESCORE = "basescore";
    public static final String DB_RELATED_COLUMN_USERSCORE = "score";

    // ==================== Keyboard Table Schema ====================
    public static final String DB_KEYBOARD = "keyboard";
    public static final String DB_KEYBOARD_COLUMN_ID = "_id";
    public static final String DB_KEYBOARD_COLUMN_CODE = "code";
    public static final String DB_KEYBOARD_COLUMN_NAME = "name";
    public static final String DB_KEYBOARD_COLUMN_DESC = "desc";
    public static final String DB_KEYBOARD_COLUMN_TYPE = "type";
    public static final String DB_KEYBOARD_COLUMN_IMAGE = "image";
    public static final String DB_KEYBOARD_COLUMN_IMKB = "imkb";
    public static final String DB_KEYBOARD_COLUMN_IMSHIFTKB = "imshiftkb";
    public static final String DB_KEYBOARD_COLUMN_ENGKB = "engkb";
    public static final String DB_KEYBOARD_COLUMN_ENGSHIFTKB = "engshiftkb";
    public static final String DB_KEYBOARD_COLUMN_SYMBOLKB = "symbolkb";
    public static final String DB_KEYBOARD_COLUMN_SYMBOLSHIFTKB = "symbolshiftkb";
    public static final String DB_KEYBOARD_COLUMN_DEFAULTKB = "defaultkb";
    public static final String DB_KEYBOARD_COLUMN_DEFAULTSHIFTKB = "defaultshiftkb";
    public static final String DB_KEYBOARD_COLUMN_EXTENDEDKB = "extendedkb";
    public static final String DB_KEYBOARD_COLUMN_EXTENDEDSHIFTKB = "extendedshiftkb";
    public static final String DB_KEYBOARD_COLUMN_DISABLE = "disable";
    public static final String DB_TOTAL_COUNT = "count";

    // ==================== IM Type Fields ====================
    public static final String IM_TYPE_NAME = "name";
    public static final String IM_TYPE_SOURCE = "source";
    public static final String IM_TYPE_AMOUNT = "amount";
    public static final String IM_TYPE_IMPORT = "import";
    public static final String IM_TYPE_KEYBOARD = "keyboard";
    public static final String IM_TYPE_SELKEY = "selkey";
    public static final String IM_TYPE_ENDKEY = "endkey";
    public static final String IM_TYPE_SPACESTYLE = "spacestyle";

    // ==================== Display Settings ====================
    public static final int IM_MANAGE_DISPLAY_AMOUNT = 100;
    public static final float HALF_ALPHA_VALUE = .5f;
    public static final float NORMAL_ALPHA_VALUE = 1f;

    // ==================== Preference Keys ====================
    public static final String DB_CHECK_RELATED_USERSCORE = "db_user_score_check";
    public static final String DATABASE_DOWNLOAD_STATUS = "database_download_status";

    // ==================== Backup/Restore ====================
    public static final String BACKUP = "backup";
    public static final String RESTORE = "restore";
    public static final String LOCAL = "LOCAL";
    public static final String DEVICE = "device";

    // ==================== File Types ====================
    public static final String SHARE_TYPE_TXT = "text/plain";
    public static final String SHARE_TYPE_ZIP = "application/zip";
    public static final String IMPORT_TEXT = "import_text";
    public static final String SUPPORT_FILE_EXT_TXT = "txt";
    public static final String SUPPORT_FILE_EXT_LIME = "lime";
    public static final String SUPPORT_FILE_EXT_LIMEDB = "limedb";
    public static final String SUPPORT_FILE_EXT_CIN = "cin";
    public static final String EXPORT_FILENAME_RELATED = "lime.related";

    // ==================== Emoji Settings ====================
    public static final int EMOJI_EN = 1;
    public static final int EMOJI_TW = 2;
    public static final int EMOJI_CN = 3;
    public static final String EMOJI_FIELD_TAG = "tag";
    public static final String EMOJI_FIELD_VALUE = "value";

    // ==================== Payment (Legacy) ====================
    public static final String PAYMENT_FLAG = "PAYMENT_FLAG";
    public static final int PAYMENT_REQUEST_CODE = 1001;

    // ==================== Utility Methods ====================

    /**
     * Get the database folder path using app-specific storage.
     *
     * @param context Application context
     * @return Path to database folder
     */
    public static String getDatabaseFolder(Context context) {
        File dbDir = new File(context.getFilesDir(), "databases");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        return dbDir.getAbsolutePath();
    }

    /**
     * Format a number with thousands separators.
     *
     * @param number The number to format
     * @return Formatted string
     */
    public static String format(int number) {
        try {
            DecimalFormat df = new DecimalFormat("###,###,###,###,###,###,##0");
            return df.format(number);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to format number: " + e.getMessage());
            return "0";
        }
    }

    /**
     * Escape special characters in SQL value strings.
     * Note: Prefer using parameterized queries instead of this method.
     *
     * @param value The value to escape
     * @return Escaped string safe for SQL
     */
    public static String formatSqlValue(String value) {
        if (value != null) {
            value = value.replace("\"", "\"\"");
            value = value.replace("'", "\\'");
            return value;
        } else {
            return "";
        }
    }
}
