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
import android.content.SharedPreferences;
import android.os.Build;
import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.List;

import nan.toload.main.hd.Lime;
import nan.toload.main.hd.data.Im;

public class LIMEPreferenceManager {

    private final Context ctx;
    // Cached SharedPreferences instance — getDefaultSharedPreferences() is thread-safe
    // and Android internally caches it, but calling it 80+ times per keystroke still
    // incurs unnecessary method-call overhead and discourages adding per-field caching later.
    private final SharedPreferences sp;

    public LIMEPreferenceManager(Context context) {
        this.ctx = context;
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getTableTotalRecords(String table) {
        table = preProcessTableName(table);

        String records = sp.getString(table + "total_record", "");
        if (records.equals("")) {
            SharedPreferences ssp = ctx.getSharedPreferences(table + "total_record", 0);
            records = ssp.getString(table + "total_record", "");
            if (!records.equals(""))
                setTableTotalRecords(table, records);
        }
        return records;
    }

    public void setTableTotalRecords(String table, String records) {
        table = preProcessTableName(table);
        sp.edit().putString(table + "total_record", records).apply();
    }

    public String getTableVersion(String table) {
        table = preProcessTableName(table);

        String version = sp.getString(table + "mapping_version", "");
        if (version.equals("")) {
            SharedPreferences ssp = ctx.getSharedPreferences(table + "mapping_version", 0);
            version = ssp.getString(table + "mapping_version", "");
            if (!version.equals(""))
                setTableVersion(table, version);
        }
        return version;
    }

    public void setTableVersion(String table, String version) {
        table = preProcessTableName(table);
        sp.edit().putString(table + "mapping_version", version).apply();
    }

    public String getTableMappingFilename(String table) {
        table = preProcessTableName(table);
        return sp.getString(table + "mapping_file", "");
    }

    public void setTableMappingFilename(String table, String filename) {
        table = preProcessTableName(table);
        sp.edit().putString(table + "mapping_file", filename).apply();
    }

    public String getTableMappingTempFilename(String table) {
        table = preProcessTableName(table);
        return sp.getString(table + "mapping_file_temp", "");
    }

    public void setTableTempMappingFilename(String table, String filename) {
        table = preProcessTableName(table);
        sp.edit().putString(table + "mapping_file_temp", filename).apply();
    }

    public String getTotalUserdictRecords() {

        String records = sp.getString("total_userdict_record", "0");
        if (records.equals("0")) {
            SharedPreferences ssp = ctx.getSharedPreferences("total_userdict_record", 0);
            records = ssp.getString("total_userdict_record", "0");
            if (records.equals("0"))
                setTotalUserdictRecords(records);
        }
        return records;

    }

    public void setTotalUserdictRecords(String records) {

        sp.edit().putString("total_userdict_record", records).apply();
    }

    // Removed @Deprecated methods:
    // - getDatabaseOnHold() - never called
    // - holdDatabaseCoonection() - never called (only in comments)

    public boolean getLanguageMode() {

        return sp.getString("language_mode", "no").equals("yes");
    }

    public void setLanguageMode(boolean englishOnly) {

        String loadingStatus = englishOnly ? "yes" : "no";

        sp.edit().putString("language_mode", loadingStatus).apply();

    }

    public int getMappingFileImportLines() {
        return Integer.parseInt(sp.getString("mapping_import_line", "0"));
    }

    public void setMappingFileImportLines(int lines) {
        sp.edit().putString("mapping_import_line", String.valueOf(lines)).apply();
    }

    public String getRerverseLookupTable(String table) {
        if (table.equals("phonetic")) {
            return sp.getString("bpmf_im_reverselookup", "none");
        } else {
            return sp.getString(table + "_im_reverselookup", "none");
        }
    }

    public boolean getFixedCandidateViewDisplay() {
        return true;
    }

    public boolean getDisableSoftwareKeyboard() {
        return sp.getBoolean("disable_software_keyboard", false);
    }

    public boolean getLearnRelatedWord() {
        return sp.getBoolean("candidate_suggestion", true);
    }

    public boolean getLearnPhrase() {
        return sp.getBoolean("learn_phrase", true);
    }

    public boolean getDisablePhysicalSelKeyOption() {
        return sp.getBoolean("disable_physical_selkey_option", false);
    }

    public boolean getEnglishPrediction() {
        return sp.getBoolean("english_dictionary_enable", true);
    }

    public boolean getPhysicalKeyboardEnable() {
        return sp.getBoolean("physical_keyboard_enable", true);
    }

    public boolean getEnglishPredictionOnPhysicalKeyboard() {
        return sp.getBoolean("english_dictionary_physical_keyboard", false);
    }

    public boolean getSortSuggestions() {

        return sp.getBoolean("learning_switch", true);
    }

    public boolean getCandidateSuggestionPunctutation() {
        return sp.getBoolean("candidate_suggestion_punctuation", true);
    }

    public boolean getPhysicalKeyboardSortSuggestions() {

        return sp.getBoolean("physical_keyboard_sort", true);
    }

    public boolean getSimiliarEnable() {
        return sp.getBoolean("similiar_enable", true);
    }

    public boolean getSelectDefaultOnSliding() {

        return sp.getBoolean("candidate_switch", true);
    }

    public boolean getVibrateOnKeyPressed() {

        return sp.getBoolean("vibrate_on_keypress", false);
    }

    public boolean getSoundOnKeyPressed() {

        return sp.getBoolean("sound_on_keypress", false);
    }

    public boolean getEmojiMode() {
        // Jeremy '16,7,30 Emoji support is limited before API 16
        return sp.getBoolean("enable_emoji", Build.VERSION.SDK_INT >= 27);
    }

    public Integer getEmojiDisplayPosition() {
        return Integer.parseInt(sp.getString("enable_emoji_position", "3"));
    }

    public boolean getReverseLookupNotify() {
        return sp.getBoolean("reverse_lookup_notify", true);
    }

    public boolean getHanConvertNotify() {
        return sp.getBoolean("han_convert_notify", false);
    }

    public boolean getPersistentLanguageMode() {
        return sp.getBoolean("persistent_language_mode", false);
    }

    public boolean getShowNumberRowInEnglish() {

        return sp.getBoolean("number_row_in_english", true);
    }

    public void syncIMActivatedState(List<Im> imlist) {
        HashMap<String, String> imhm = new HashMap<String, String>();
        for (Im i : imlist) {
            imhm.put(i.getCode(), i.getCode());
        }

        StringBuilder sb = new StringBuilder();
        if (imhm.get(Lime.IM_CUSTOM) != null)   { sb.append("0"); }
        if (imhm.get(Lime.IM_DAYI) != null)      { if (sb.length() > 0) sb.append(";"); sb.append("5"); }
        if (imhm.get(Lime.IM_PHONETIC) != null)  { if (sb.length() > 0) sb.append(";"); sb.append("6"); }
        if (imhm.get(Lime.IM_EZ) != null)        { if (sb.length() > 0) sb.append(";"); sb.append("7"); }

        setIMActivatedState(sb.toString());
    }

    public String getIMActivatedState() {
        return sp.getString("keyboard_state", "0;1;2;3;4;5;6;7;8;9;10;11;12");
    }

    public void setIMActivatedState(String state) {
        sp.edit().putString("keyboard_state", String.valueOf(state)).apply();
    }

    public String getActiveIM() {
        return sp.getString("keyboard_list", "phonetic");
    }

    public void setActiveIM(String activeIM) {
        sp.edit().putString("keyboard_list", String.valueOf(activeIM)).apply();
    }

    public boolean getThreerowRemapping() {

        return sp.getBoolean("three_rows_remapping", false);
    }

    public String getPhysicalKeyboardType() {

        return sp.getString("physical_keyboard_type", "normal_keyboard");
    }

    public int getAutoCommitValue() {
        return Integer.parseInt(sp.getString("auto_commit", "0"));
    }

    public String getPhoneticKeyboardType() {

        return sp.getString("phonetic_keyboard_type", "standard");
    }

    public boolean getAutoCaptalization() {

        return sp.getBoolean("auto_cap", true);
    }

    public boolean getQuickFixes() {

        return sp.getBoolean("quick_fixes", true);
    }

    public boolean getAutoComplete() {

        return sp.getBoolean("auto_complete", true);
    }

    public boolean getDisablePhysicalSelkey() {

        return sp.getBoolean("disable_physical_selkey", false);
    }

    public Integer getHanCovertOption() {

        return Integer.parseInt(sp.getString("han_convert_option", "0"));
    }

    public void setHanCovertOption(int value) {

        sp.edit().putString("han_convert_option", String.valueOf(value)).apply();

    }

    public Integer getSelkeyOption() {

        return Integer.parseInt(sp.getString("selkey_option", "0"));
    }

    public Integer getSimilarCodeCandidates() {
        return Integer.parseInt(sp.getString("similiar_list", "20"));
    }

    public float getFontSize() {
        return Float.parseFloat(sp.getString("font_size", "1"));

    }

    public float getKeyboardSize() {
        return Float.parseFloat(sp.getString("keyboard_size", "1"));

    }

    public boolean getSmartChineseInput() {
        return sp.getBoolean("smart_chinese_input", false);
    }

    public boolean getAutoChineseSymbol() {

        return sp.getBoolean("auto_chinese_symbol", false);
    }

    public Integer getVibrateLevel() {
        return Integer.parseInt(sp.getString("vibrate_level", "40"));
    }

    public boolean getShowNumberKeypard() {

        return sp.getBoolean("display_number_keypads", false);
    }

    public boolean getAllowNumberMapping() {
        return sp.getBoolean("accept_number_index", false);
    }

    public boolean getAllowSymoblMapping() {
        return sp.getBoolean("accept_symbol_index", false);
    }

    public boolean getSwitchEnglishModeHotKey() {
        return sp.getBoolean("switch_english_mode", false);
    }

    public boolean getShiftSwitchEnglishMode() {
        return sp.getBoolean("switch_english_mode_shift", true);
    }

    public boolean getAutoHideSoftKeyboard() {
        return sp.getBoolean("hide_software_keyboard_typing_with_physical", true);

    }

    public int getShowArrowKeys() {
        return Integer.parseInt(sp.getString("show_arrow_key", "0"));

    }

    public void setShowArrowKeys(int mode) {
        sp.edit().putString("show_arrow_key", Integer.toString(mode)).apply();

    }

    public int getSplitKeyboard() {
        return Integer.parseInt(sp.getString("split_keyboard_mode", "0"));
    }

    public void setSplitKeyboard(int mode) {
        sp.edit().putString("split_keyboard_mode", Integer.toString(mode)).apply();

    }

    public int getKeyboardTheme() {
        return Integer.parseInt(sp.getString("keyboard_theme", "0"));
    }

    public boolean getResetCacheFlag(boolean defaultvalue) {
        return sp.getBoolean("searchsrv_reset_cache", defaultvalue);
    }

    public void setResetCacheFlag(boolean value) {
        sp.edit().putBoolean("searchsrv_reset_cache", value).apply();
    }

    /*
     * INT Parameter SET/GET
     */
    public void setParameter(String label, int value) {
        sp.edit().putInt(label, value).apply();
    }

    public int getParameterInt(String label) {
        return sp.getInt(label, 0);
    }

    public int getParameterInt(String label, int defaultvalue) {
        return sp.getInt(label, defaultvalue);
    }

    /*
     * LONG Parameter SET/GET
     */
    public long getParameterLong(String label, long defaultvalue) {
        return sp.getLong(label, defaultvalue);
    }

    public long getParameterLong(String label) {
        return sp.getLong(label, 0);
    }

    public void setParameter(String label, long value) {
        sp.edit().putLong(label, value).apply();
    }

    /*
     * String Parameter SET/GET
     */
    public void setParameter(String label, String value) {
        sp.edit().putString(label, value).apply();
    }

    public String getParameterString(String label) {
        return sp.getString(label, "");
    }

    public String getParameterString(String label, String defaultstring) {
        return sp.getString(label, defaultstring);
    }

    /*
     * Boolean Parameter SET/GET
     */
    public void setParameter(String label, boolean value) {
        sp.edit().putBoolean(label, value).apply();
    }

    public boolean getParameterBoolean(String label) {
        return sp.getBoolean(label, false);
    }

    public boolean getParameterBoolean(String label, boolean defaultvalue) {
        try {
            return sp.getBoolean(label, defaultvalue);
        } catch (Exception e) {
            return defaultvalue;
        }
    }

    private String preProcessTableName(String table) {
        if (table.endsWith("_") || table.equals("")) {
            return table; // processed already.
        } else if (table.equals("phonetic")) {
            return "bpmf_";
        } else if (table.equals("mapping") || table.equals("lime") || table.equals("phone")) {
            return "";
        } else {
            return table + "_";
        }
    }

}
