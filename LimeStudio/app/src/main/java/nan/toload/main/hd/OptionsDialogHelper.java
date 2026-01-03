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

import android.content.res.Resources;
import android.util.DisplayMetrics;

import nan.toload.main.hd.global.LIMEPreferenceManager;
import nan.toload.main.hd.keyboard.LIMEKeyboard;

/**
 * Helper class for managing options dialog configuration.
 * Extracted from LIMEService to improve modularity.
 *
 * This class handles:
 * - Building options menu items based on current state
 * - Managing split keyboard state transitions
 * - Han convert option management
 */
public class OptionsDialogHelper {

    // Menu positions
    public static final int POS_SETTINGS = 0;
    public static final int POS_HANCONVERT = 1;
    public static final int POS_KEYBOARD = 2;
    public static final int POS_METHOD = 3;
    public static final int POS_SPLIT_KEYBOARD = 4;
    public static final int POS_VOICEINPUT = 5;

    private final LIMEPreferenceManager mLIMEPref;
    private final Resources mResources;

    public OptionsDialogHelper(LIMEPreferenceManager limePref, Resources resources) {
        this.mLIMEPref = limePref;
        this.mResources = resources;
    }

    /**
     * Check if the device is in landscape mode.
     */
    public boolean isLandscape() {
        DisplayMetrics dm = mResources.getDisplayMetrics();
        return dm.widthPixels > dm.heightPixels;
    }

    /**
     * Check if split keyboard option should be shown.
     * Split option is hidden in landscape mode when arrow keys are shown.
     *
     * @param showArrowKeys current show arrow keys setting
     * @return true if split option should be displayed
     */
    public boolean shouldShowSplitOption(int showArrowKeys) {
        return !(isLandscape() && showArrowKeys > 0);
    }

    /**
     * Get the split keyboard menu item text based on current state.
     *
     * @param splitKeyboardState current split keyboard state
     * @return resource ID for the menu item text
     */
    public int getSplitKeyboardMenuTextResId(int splitKeyboardState) {
        boolean isLandscape = isLandscape();
        if ((splitKeyboardState == LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY && isLandscape)
                || splitKeyboardState == LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS) {
            return R.string.merge_keyboard;
        }
        return R.string.split_keyboard;
    }

    /**
     * Toggle split keyboard state and return the new state.
     *
     * @param currentState current split keyboard state
     * @return new split keyboard state after toggle
     */
    public int toggleSplitKeyboard(int currentState) {
        boolean isLandscape = isLandscape();
        int newState;

        if (currentState == LIMEKeyboard.SPLIT_KEYBOARD_NEVER) {
            newState = isLandscape
                    ? LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY
                    : LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS;
        } else if (currentState == LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS) {
            newState = isLandscape
                    ? LIMEKeyboard.SPLIT_KEYBOARD_NEVER
                    : LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY;
        } else { // SPLIT_KEYBOARD_LANDSCAPD_ONLY
            newState = isLandscape
                    ? LIMEKeyboard.SPLIT_KEYBOARD_NEVER
                    : LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS;
        }

        mLIMEPref.setSplitKeyboard(newState);
        return newState;
    }

    /**
     * Get the current Han convert option.
     */
    public int getHanConvertOption() {
        return mLIMEPref.getHanCovertOption();
    }

    /**
     * Set the Han convert option.
     *
     * @param option the Han convert option to set
     */
    public void setHanConvertOption(int option) {
        mLIMEPref.setHanCovertOption(option);
    }

    /**
     * Get Han convert option items from resources.
     */
    public CharSequence[] getHanConvertOptions() {
        return mResources.getStringArray(R.array.han_convert_options);
    }

    /**
     * Configuration class for building the main options menu.
     */
    public static class OptionsMenuConfig {
        public final CharSequence[] items;
        public final boolean hasSplitOption;
        public final boolean isLandscape;

        public OptionsMenuConfig(CharSequence[] items, boolean hasSplitOption, boolean isLandscape) {
            this.items = items;
            this.hasSplitOption = hasSplitOption;
            this.isLandscape = isLandscape;
        }
    }

    /**
     * Build the options menu configuration.
     *
     * @param splitKeyboardState current split keyboard state
     * @param showArrowKeys      current show arrow keys setting
     * @return configuration for the options menu
     */
    public OptionsMenuConfig buildOptionsMenuConfig(int splitKeyboardState, int showArrowKeys) {
        boolean isLandscape = isLandscape();
        boolean hasSplitOption = shouldShowSplitOption(showArrowKeys);

        CharSequence itemSettings = mResources.getString(R.string.lime_setting_preference);
        CharSequence hanConvert = mResources.getString(R.string.han_convert_option_list);
        CharSequence itemSwitchIM = mResources.getString(R.string.keyboard_list);
        CharSequence itemSwitchSystemIM = mResources.getString(R.string.input_method);
        CharSequence itemVoiceInput = mResources.getString(R.string.voice_input);

        CharSequence itemSplitKeyboard = mResources.getString(
                getSplitKeyboardMenuTextResId(splitKeyboardState));

        CharSequence[] items;
        if (hasSplitOption) {
            items = new CharSequence[] {
                    itemSettings, hanConvert, itemSwitchIM,
                    itemSwitchSystemIM, itemSplitKeyboard, itemVoiceInput
            };
        } else {
            items = new CharSequence[] {
                    itemSettings, hanConvert, itemSwitchIM,
                    itemSwitchSystemIM, itemVoiceInput
            };
        }

        return new OptionsMenuConfig(items, hasSplitOption, isLandscape);
    }
}
