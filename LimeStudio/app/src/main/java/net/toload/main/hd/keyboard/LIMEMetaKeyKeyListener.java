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

package net.toload.main.hd.keyboard;

import android.text.method.MetaKeyKeyListener;
import android.view.KeyEvent;

public abstract class LIMEMetaKeyKeyListener extends MetaKeyKeyListener {


    public static final int META_SELECTING = 1 << 16;
    private static final int LOCKED_SHIFT = 8;
    public static final int META_CAP_LOCKED = KeyEvent.META_SHIFT_ON << LOCKED_SHIFT;
    public static final int META_ALT_LOCKED = KeyEvent.META_ALT_ON << LOCKED_SHIFT;
    public static final int META_SYM_LOCKED = KeyEvent.META_SYM_ON << LOCKED_SHIFT;
    //private static final Object CAP = new NoCopySpan.Concrete();
    //private static final Object ALT = new NoCopySpan.Concrete();
    //private static final Object SYM = new NoCopySpan.Concrete();
    //private static final Object SELECTING = new NoCopySpan.Concrete();


    /**
     * Call this method after you handle a keypress so that the meta
     * state will be reset to unshifted (if it is not still down)
     * or primed to be reset to unshifted (once it is released).  Takes
     * the current state, returns the new state.
     */
    public static long adjustMetaAfterKeypress(long state) {
        return MetaKeyKeyListener.adjustMetaAfterKeypress(state);
    }

    /**
     * Handles presses of the meta keys.
     */
    public static long handleKeyDown(long state, int keyCode, KeyEvent event) {
        return MetaKeyKeyListener.handleKeyDown(state, keyCode, event);
    }

    /**
     * Handles release of the meta keys.
     */
    public static long handleKeyUp(long state, int keyCode, KeyEvent event) {
        return MetaKeyKeyListener.handleKeyUp(state, keyCode, event);
    }


}
