/*
 * Copyright 2024 The LimeIME Open Source Project
 */

package net.toload.main.hd;

import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import net.toload.main.hd.keyboard.LIMEMetaKeyKeyListener;

public class HardKeyHelper {

    private static final String TAG = "HardKeyHelper";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    public static boolean translateKeyDown(LIMEService service, int keyCode, KeyEvent event) {
        // move to HandleCharacter '10, 3,26
        // mMetaState = LIMEMetaKeyKeyListener.handleKeyDown(mMetaState,
        // keyCode, event);
        // mMetaState =
        // LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);

        service.hasPhysicalKeyPressed = true;

        // If user use the physical keyboard then not fixed the candidate view
        service.mFixedCandidateViewOn = false;

        // hide softkeyboard. Jeremy '12,5,8
        // Should not hide inputView or the candidateView cannot be shown in first
        // stroke. Jeremy '15,6,1
        /*
         * if (mInputView != null && mInputView.isShown() &&
         * mLIMEPref.getAutoHideSoftKeyboard()) {
         * mInputView.closing();
         * requestHideSelf(0);
         * }
         */

        if (DEBUG)
            Log.i(TAG, "translateKeyDown() LIMEMetaKeyKeyListener.getMetaState(mMetaState) = "
                    + Integer.toHexString(LIMEMetaKeyKeyListener.getMetaState(service.mMetaState))
                    + ", event.getMetaState()" + Integer.toHexString(event.getMetaState()));

        // Jeremy '12,5,28 after honeycomb use the metastate sent form KeyEvent to
        // proces the shift/cap_lock etc...

        int metaState = LIMEMetaKeyKeyListener.getMetaState(service.mMetaState);

        int c = event.getUnicodeChar(metaState);

        InputConnection ic = service.getCurrentInputConnection();

        if (c == 0 || ic == null) {
            return false;
        }

        // Compact code by Jeremy '10, 3, 27
        if (keyCode == 59) { // Translate shift as -1
            c = -1;
        }
        if (c != -1 && (c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        service.onKey(c, null);
        return true;
    }
}
