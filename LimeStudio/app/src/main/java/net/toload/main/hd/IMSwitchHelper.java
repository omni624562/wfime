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

import android.content.res.Resources;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import net.toload.main.hd.global.LIMEPreferenceManager;

/**
 * Helper class for managing Input Method (IM) switching logic.
 * Extracted from LIMEService to improve modularity.
 * 
 * This class handles:
 * - Building and maintaining the activated IM list
 * - Switching between activated IMs
 * - IM selection by index
 */
public class IMSwitchHelper {

    private static final String TAG = "IMSwitchHelper";
    private static final boolean DEBUG = false;

    private final LIMEPreferenceManager mLIMEPref;
    private final Resources mResources;

    // Activated IM lists
    private final List<String> activatedIMNameList = new ArrayList<>();
    private final List<String> activatedIMShortNameList = new ArrayList<>();
    private final List<String> activatedIMList = new ArrayList<>();

    // Current active IM
    private String activeIM;

    // Cached state to avoid rebuilding when unchanged
    private String mIMActivatedState = "";

    public IMSwitchHelper(LIMEPreferenceManager limePref, Resources resources) {
        this.mLIMEPref = limePref;
        this.mResources = resources;
        this.activeIM = limePref != null ? limePref.getActiveIM() : "";
    }

    /**
     * Get the current active IM code.
     */
    public String getActiveIM() {
        return activeIM;
    }

    /**
     * Get the short name of the currently active IM.
     */
    public String getActiveIMShortName() {
        buildActivatedIMList();
        for (int i = 0; i < activatedIMList.size(); i++) {
            if (activeIM.equals(activatedIMList.get(i))) {
                return activatedIMShortNameList.get(i);
            }
        }
        return "";
    }

    /**
     * Set the current active IM code.
     */
    public void setActiveIM(String im) {
        this.activeIM = im;
        mLIMEPref.setActiveIM(im);
    }

    /**
     * Get the list of activated IM names (for display).
     */
    public List<String> getActivatedIMNameList() {
        return activatedIMNameList;
    }

    /**
     * Get the list of activated IM short names.
     */
    public List<String> getActivatedIMShortNameList() {
        return activatedIMShortNameList;
    }

    /**
     * Get the list of activated IM codes.
     */
    public List<String> getActivatedIMList() {
        return activatedIMList;
    }

    /**
     * Check if the activated IM list is empty.
     */
    public boolean isActivatedListEmpty() {
        return activatedIMList.isEmpty();
    }

    /**
     * Get the index of the current active IM in the activated list.
     * Returns 0 if not found.
     */
    public int getCurrentIMIndex() {
        for (int i = 0; i < activatedIMList.size(); i++) {
            if (activeIM.equals(activatedIMList.get(i))) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Build/refresh the activated IM list from preferences and resources.
     */
    public void buildActivatedIMList() {
        CharSequence[] items = mResources.getStringArray(R.array.keyboard);
        CharSequence[] shortNames = mResources.getStringArray(R.array.keyboardShortname);
        CharSequence[] codes = mResources.getStringArray(R.array.keyboard_codes);

        String pIMActiveState = mLIMEPref.getIMActivatedState();

        if (DEBUG)
            Log.i(TAG, "buildActivatedIMList(): pIMActiveState='" + pIMActiveState + "', mIMActivatedState='"
                    + mIMActivatedState + "'");

        if (pIMActiveState.trim().isEmpty()) {
            // Set default activated input methods: dayi (0) and phonetic (1)
            pIMActiveState = "0;1;";
            mLIMEPref.setIMActivatedState(pIMActiveState);
            if (DEBUG)
                Log.i(TAG, "Set default pIMActiveState: " + pIMActiveState);
        }

        if (!(mIMActivatedState.length() > 0 && mIMActivatedState.equals(pIMActiveState))) {
            if (DEBUG)
                Log.i(TAG, "Rebuilding list (cache miss)");
            mIMActivatedState = pIMActiveState;
            String[] s = pIMActiveState.split(";");

            activatedIMNameList.clear();
            activatedIMList.clear();
            activatedIMShortNameList.clear();

            for (String value : s) {
                if (value.isEmpty())
                    continue;
                int index = Integer.parseInt(value);

                if (index < items.length) {
                    activatedIMNameList.add(items[index].toString());
                    activatedIMShortNameList.add(shortNames[index].toString());
                    activatedIMList.add(codes[index].toString());
                    if (DEBUG)
                        Log.i(TAG, "buildActivatedIMList(): [" + index + "] = "
                                + codes[index].toString() + " ;" + shortNames[index].toString());
                } else {
                    if (DEBUG)
                        Log.i(TAG, "Invalid index " + index + " >= items.length " + items.length);
                }
            }

            // If list is still empty after parsing, reset to defaults
            if (activatedIMList.isEmpty()) {
                if (DEBUG)
                    Log.i(TAG, "List empty after parsing, resetting to defaults 0;1");
                pIMActiveState = "0;1;";
                mIMActivatedState = pIMActiveState;
                mLIMEPref.setIMActivatedState(pIMActiveState);

                // Add default IMs
                for (int i = 0; i < Math.min(2, codes.length); i++) {
                    activatedIMNameList.add(items[i].toString());
                    activatedIMShortNameList.add(shortNames[i].toString());
                    activatedIMList.add(codes[i].toString());
                    if (DEBUG)
                        Log.i(TAG, "Added default IM: [" + i + "] = " + codes[i].toString());
                }
            }
        } else {
            if (DEBUG)
                Log.i(TAG, "Skipping rebuild (cache hit)");

            // If list is empty despite cache hit, force rebuild
            if (activatedIMList.isEmpty()) {
                if (DEBUG)
                    Log.i(TAG, "List empty despite cache hit, forcing rebuild");
                mIMActivatedState = ""; // Clear cache to force rebuild on next call
                buildActivatedIMList(); // Recursive call to rebuild
                return;
            }
        }

        if (DEBUG)
            Log.i(TAG, "current active IM: " + activeIM);

        // Check if the selected keyboard is in active keyboard list
        boolean matched = false;
        for (int i = 0; i < activatedIMList.size(); i++) {
            if (activeIM.equals(activatedIMList.get(i))) {
                if (DEBUG)
                    Log.i(TAG, "buildActivatedIMList(): activatedIM[" + i + "] matches current active IM: " + activeIM);
                matched = true;
                break;
            }
        }

        if (!matched) {
            // If the selected keyboard is not in the active keyboard list,
            // set the keyboard to the first active keyboard
            if (!activatedIMList.isEmpty()) {
                activeIM = activatedIMList.get(0);
                mLIMEPref.setActiveIM(activeIM);
            } else {
                // Set default to first available input method if list is empty
                if (codes.length > 0) {
                    activeIM = codes[0].toString();
                    mLIMEPref.setActiveIM(activeIM);
                }
            }
        }
    }

    /**
     * Switch to the next (or previous) activated IM.
     * 
     * @param forward true for next IM, false for previous IM
     * @return the name of the newly active IM, or empty string if switch failed
     */
    public String switchToNextActivatedIM(boolean forward) {
        if (DEBUG)
            Log.i(TAG, "switchToNextActivatedIM()");

        buildActivatedIMList();

        if (DEBUG)
            Log.i(TAG, "activatedIMList.size()=" + activatedIMList.size() + ", contents: " + activatedIMList);

        if (activatedIMList.isEmpty()) {
            return "";
        }

        String activeIMName = "";
        for (int i = 0; i < activatedIMList.size(); i++) {
            if (activeIM.equals(activatedIMList.get(i))) {
                if (i == activatedIMList.size() - 1 && forward) {
                    activeIM = activatedIMList.get(0);
                    activeIMName = activatedIMNameList.get(0);
                } else if (i == 0 && !forward) {
                    activeIM = activatedIMList.get(activatedIMList.size() - 1);
                    activeIMName = activatedIMNameList.get(activatedIMList.size() - 1);
                } else {
                    int nextIndex = i + (forward ? 1 : -1);
                    activeIM = activatedIMList.get(nextIndex);
                    activeIMName = activatedIMNameList.get(nextIndex);
                }
                break;
            }
        }

        mLIMEPref.setActiveIM(activeIM);
        return activeIMName;
    }

    /**
     * Select an IM by its position in the activated list.
     * 
     * @param position index in the activated IM list
     */
    public void selectIMByPosition(int position) {
        if (DEBUG)
            Log.i(TAG, "selectIMByPosition() position = " + position);

        if (position >= 0 && position < activatedIMList.size()) {
            activeIM = activatedIMList.get(position);
            mLIMEPref.setActiveIM(activeIM);
        }
    }
}
