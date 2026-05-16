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

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * Dialog fragment for MultiListPreference.
 * This class handles the dialog UI for multi-selection list preferences
 * using the AndroidX Preference library's Fragment-based dialog system.
 *
 * Usage:
 * In your PreferenceFragmentCompat, override onDisplayPreferenceDialog() and
 * call:
 * MultiListPreferenceDialogFragmentCompat.newInstance(preference.getKey())
 */
public class MultiListPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private static final String TAG = "MultiListPrefDialog";
    private static final boolean DEBUG = false;

    /** Key for saving/restoring the selection state */
    private static final String SAVE_STATE_VALUES = "MultiListPreferenceDialogFragmentCompat.values";

    /** Current selection state (mutable copy during dialog interaction) */
    private boolean[] mCurrentState;

    /**
     * Creates a new instance of the dialog fragment.
     *
     * @param key The preference key
     * @return A new instance of MultiListPreferenceDialogFragmentCompat
     */
    public static MultiListPreferenceDialogFragmentCompat newInstance(String key) {
        final MultiListPreferenceDialogFragmentCompat fragment = new MultiListPreferenceDialogFragmentCompat();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Gets the MultiListPreference associated with this dialog.
     */
    private MultiListPreference getMultiListPreference() {
        return (MultiListPreference) getPreference();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Initialize from preference
            MultiListPreference preference = getMultiListPreference();
            CharSequence[] entries = preference.getEntries();

            if (entries != null) {
                // Try to restore from persisted string first
                String persistedValue = preference.getPersistedValue(null);

                if (persistedValue != null && !persistedValue.isEmpty()) {
                    // Parse persisted value
                    mCurrentState = new boolean[entries.length];
                    String[] indices = persistedValue.split(MultiListPreference.CHOICE_DELIMITER);
                    for (String indexStr : indices) {
                        try {
                            int index = Integer.parseInt(indexStr.trim());
                            if (index >= 0 && index < mCurrentState.length) {
                                mCurrentState[index] = true;
                            }
                        } catch (NumberFormatException e) {
                            if (DEBUG) {
                                Log.w(TAG, "onCreate(): Invalid index in persisted value: " + indexStr);
                            }
                        }
                    }
                    if (DEBUG) {
                        Log.d(TAG, "onCreate(): Restored state from persisted value: " + persistedValue);
                    }
                } else {
                    // Fall back to preference value or default
                    boolean[] value = preference.getValue();
                    mCurrentState = value != null ? value.clone() : new boolean[entries.length];
                }
            }
        } else {
            // Restore from saved instance state (configuration change)
            mCurrentState = savedInstanceState.getBooleanArray(SAVE_STATE_VALUES);
        }

        if (DEBUG) {
            Log.d(TAG, "onCreate(): state initialized, length=" +
                    (mCurrentState != null ? mCurrentState.length : "null"));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBooleanArray(SAVE_STATE_VALUES, mCurrentState);
    }

    @Override
    protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        MultiListPreference preference = getMultiListPreference();
        CharSequence[] entries = preference.getEntries();

        if (entries == null || mCurrentState == null) {
            if (DEBUG) {
                Log.e(TAG, "onPrepareDialogBuilder(): entries or state is null");
            }
            return;
        }

        // Ensure state array matches entries length
        if (mCurrentState.length != entries.length) {
            if (DEBUG) {
                Log.w(TAG, "onPrepareDialogBuilder(): state length mismatch, resizing");
            }
            boolean[] newState = new boolean[entries.length];
            System.arraycopy(mCurrentState, 0, newState, 0,
                    Math.min(mCurrentState.length, entries.length));
            mCurrentState = newState;
        }

        builder.setMultiChoiceItems(entries, mCurrentState,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (DEBUG) {
                            Log.d(TAG, "Item " + which + " clicked: " + isChecked);
                        }
                        mCurrentState[which] = isChecked;
                    }
                });

        // Make dialog non-cancelable on touch outside (user must press button)
        builder.setCancelable(false);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (DEBUG) {
            Log.d(TAG, "onDialogClosed(): positiveResult=" + positiveResult);
        }

        if (positiveResult && mCurrentState != null) {
            MultiListPreference preference = getMultiListPreference();

            // Check if at least one item is selected
            boolean hasSelection = false;
            for (boolean selected : mCurrentState) {
                if (selected) {
                    hasSelection = true;
                    break;
                }
            }

            // If nothing selected, default to first item
            if (!hasSelection) {
                Toast.makeText(requireContext(),
                        MultiListPreference.USING_DEFAULT,
                        Toast.LENGTH_SHORT).show();
                mCurrentState[0] = true; // Default to first item
            }

            if (DEBUG) {
                Log.d(TAG, "onDialogClosed(): Saving state");
            }

            // Notify change listeners and persist if allowed
            if (preference.callChangeListener(mCurrentState)) {
                preference.setValueAndPersist(mCurrentState);
            }
        }
    }

    /**
     * Convert boolean array to delimited string of indices.
     * Example: [true, false, true, false] -> "0;2"
     *
     * @param state Boolean array representing selection state
     * @return Semicolon-delimited string of selected indices
     */
    private String booleanArrayToDelimitedString(boolean[] state) {
        if (state == null)
            return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < state.length; i++) {
            if (state[i]) {
                if (sb.length() > 0) {
                    sb.append(MultiListPreference.CHOICE_DELIMITER);
                }
                sb.append(i);
            }
        }
        return sb.toString();
    }
}
