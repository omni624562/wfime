package nan.toload.main.hd;

import static nan.toload.main.hd.LIMEService.*;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;

import nan.toload.main.hd.data.ChineseSymbol;
import nan.toload.main.hd.keyboard.LIMEBaseKeyboard;
import nan.toload.main.hd.keyboard.LIMEMetaKeyKeyListener;

class PhysicalKeyHandler {
    private final LIMEService service;

    PhysicalKeyHandler(LIMEService service) {
        this.service = service;
    }

    boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        // Clean code by jeremy '11,8,22
        if (service.DEBUG)
            Log.i(service.TAG, "OnKeyDown():keyCode:" + keyCode
                    + ", mComposing = " + service.mComposing
                    + ", hasMenuPress = " + service.hasMenuPress
                    + ", hasCtrlPress = " + service.hasCtrlPress
                    + ", isCtrlPressed = " + event.isCtrlPressed()
                    + ", hasShiftPress = " + service.hasShiftPress
                    + ", onlyShiftPress = " + service.onlyShiftPress
                    + ", hasWinPress = " + service.hasWinPress
                    + ", event.getEventTime() -  event.getDownTime()" + (event.getEventTime() - event.getDownTime())
                    + ", event.getRepeatCount()" + event.getRepeatCount()
                    + ", event.getMetaState()" + Integer.toHexString(event.getMetaState()));

        service.mKeydownEvent = new KeyEvent(event);
        // Record key pressed time and set key processed flags(key down, for physical
        // keys)
        // Jeremy '11,8,22 using getRepeatCount from event to set processed flags
        if (event.getRepeatCount() == 0) {// !keydown) {
            // keyPressTime = System.currentTimeMillis();
            // keydown = true;
            service.hasKeyProcessed = false;
            service.hasMenuProcessed = false; // only do this on first keydown event
            service.hasEnterProcessed = false;
            service.hasSpaceProcessed = false;
            service.hasSymbolEntered = false;
            // Jeremy '15,5,30 for physical keyboard
            service.onlyShiftPress = false;
            service.lastKeyCtrl = false;
            service.spaceKeyPress = false;
        }

        switch (keyCode) {
            // Jeremy '11,5,29 Bypass search and menu combination keys.
            case KeyEvent.KEYCODE_MENU:

                service.hasMenuPress = true;
                break;
            // Add by Jeremy '10, 3, 29. DPAD selection on candidate view
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Log.i("ART","select:"+1);
                if (service.hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    service.mCandidateView.selectNext();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Log.i("ART","select:"+2);
                if (service.hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    service.mCandidateView.selectPrev();
                    return true;
                }
                break;
            // Jeremy '11,8,28 for expanded canddiateviewi
            case KeyEvent.KEYCODE_DPAD_UP:
                // Log.i("ART","select:"+2);
                if (service.hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    service.mCandidateView.selectPrevRow();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Log.i("ART","select:"+2);
                if (service.hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    service.mCandidateView.selectNextRow();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                // Log.i("ART","select:"+3);
                if (service.hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    service.pickHighlightedCandidate();
                    return true;
                }
                break;
            // Add by Jeremy '10,3,26, process metakey with
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                service.hasShiftPress = true;
                service.onlyShiftPress = true;
                service.mMetaState = LIMEMetaKeyKeyListener.handleKeyDown(service.mMetaState, keyCode, event);
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                service.mMetaState = LIMEMetaKeyKeyListener.handleKeyDown(service.mMetaState, keyCode, event);
                break;
            case MY_KEYCODE_CTRL_LEFT:
            case MY_KEYCODE_CTRL_RIGHT:
                service.hasCtrlPress = true;
                service.lastKeyCtrl = true;
                break;
            case MY_KEYCODE_WINDOWS_START:
                service.hasWinPress = true;
                break;
            case MY_KEYCODE_ESC:
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.

                if (event.getRepeatCount() == 0) {
                    // Jeremy '24,1,7: Handle emoji view back
                    if (service.mEmojiKeyboardView != null && service.mEmojiKeyboardView.getVisibility() == View.VISIBLE) {
                        service.closeEmojiPicker();
                        return true;
                    }

                    if (service.mInputView != null && service.mInputView.handleBack()) {
                        Log.i(service.TAG, "KEYCODE_BACK mInputView handled the backed key");
                        return true;
                    }
                    // Jeremy '12,4,8 rewrite the logic here
                    // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                    // TODO: need to recheck here.
                    else if (!service.mEnglishOnly
                            && service.hasCandidatesShown
                            && (service.mComposing.length() > 0
                                    || (service.selectedCandidate != null && !service.selectedCandidate.isComposingCodeRecord()
                                            && !service.hasChineseSymbolCandidatesShown))) {
                        if (service.DEBUG)
                            Log.i(service.TAG, "KEYCODE_BACK clearcomposing only.");
                        service.clearComposing(false);
                        return true;
                    } else if (!service.mEnglishOnly && service.hasCandidatesShown) { // Jeremy '12,6,13
                        service.hideCandidateView();
                        return true;
                    }

                }
                if (service.DEBUG)
                    Log.i(service.TAG, "KEYCODE_BACK return to super.");

                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                service.hasPhysicalKeyPressed = true;
                service.onKey(LIMEBaseKeyboard.KEYCODE_DELETE, null);
                return true;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these, if return
                // false from takeSelectedSuggestion().
                // Process enter for candidate view selection in OnKeyUp() to block
                // the real enter afterward.
                // return false;
                // Log.i("ART", "physical keyboard:"+ keyCode);
                service.mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(service.mMetaState);
                service.setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState();
                if (!service.mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                    if (service.hasCandidatesShown) { // Replace isCandidateShown() with hasCandidatesShown by Jeremy '12,5,6
                        // To block a real enter after suggestion selection. We have to
                        // return true in OnKeyUp();
                        if (service.pickHighlightedCandidate()) {
                            service.hasEnterProcessed = true;
                            return true;
                        } else {
                            service.hideCandidateView();
                            break;
                        }
                    }
                } else if (// mLIMEPref.getEnglishPrediction() &&
                service.mPredictionOn && service.mLIMEPref.getEnglishPredictionOnPhysicalKeyboard()) {
                    service.resetTempEnglishWord();
                    service.updateEnglishPrediction();
                    break;
                } else // Jeremy '12',7,1 bug fixed on english mode enter not functioning in chrome
                    break;

                /*
                 * case MY_KEYCODE_ESC:
                 * //Jeremy '11,9,7 treat esc as back key
                 * //Jeremy '11,8,14
                 * clearComposing();
                 * InputConnection ic=getCurrentInputConnection();
                 * if(ic!=null) ic.commitText("", 0);
                 * return true;
                 */

            case KeyEvent.KEYCODE_SPACE:
                service.spaceKeyPress = true;
                service.hasQuickSwitch = service.mLIMEPref.getSwitchEnglishModeHotKey();
                // If user enable Quick Switch Mode control then check if has
                // Shift+Space combination
                // '11,5,13 Jeremy added Ctrl-space switch chi/eng
                // '11,6,18 Jeremy moved from on_KEY_UP
                // '12,4,29 Jeremy add hasWinPress + space to switch chi/eng (earth key on zippy
                // keyboard)
                // '12,5,8 Jeremy add send the space key to onKey with translatekeydown for
                // candidate processing if it's not switching chi/eng
                if ((service.hasQuickSwitch && service.hasShiftPress) || service.hasCtrlPress || service.hasMenuPress || service.hasWinPress
                        || event.isCtrlPressed()) {
                    if (!service.hasWinPress)
                        service.switchChiEng(); // Jeremy '12,5,20 move hasWinPress to winstartkey in onkeyUp()
                    if (service.hasMenuPress)
                        service.hasMenuProcessed = true;
                    service.hasSpaceProcessed = true;
                    return true;
                } else
                    return service.translateKeyDown(keyCode, event);

            case MY_KEYCODE_SWITCH_CHARSET: // experia pro earth key
            case 1000: // milestone chi/eng key
                service.switchChiEng();
                break;
            case KeyEvent.KEYCODE_SYM:
            case KeyEvent.KEYCODE_AT:
                // Jeremy '11,8,22 use begintime and eventtime in event to see if long-pressed
                // or not.
                if (!service.hasKeyProcessed
                        && event.getRepeatCount() > 0
                        && event.getEventTime() - event.getDownTime() > service.mLongPressKeyTimeout) {
                    // && System.currentTimeMillis() - keyPressTime > mLongPressKeyTimeout){
                    service.switchChiEng();
                    service.hasKeyProcessed = true;
                }
                return true;
            case KeyEvent.KEYCODE_TAB: // Jeremy '12.6,22 Force bypassing tab processing to super if not on milestone 2
                                       // with alt on (alt+tab = ~ on milestone2)
                if (!(LIMEMetaKeyKeyListener.getMetaState(service.mMetaState,
                        LIMEMetaKeyKeyListener.META_ALT_ON) > 0
                        && service.mLIMEPref.getPhysicalKeyboardType().equals("milestone2")))
                    break;
            default:
                if (!(service.hasCtrlPress || event.isCtrlPressed() || service.hasMenuPress)) {
                    if (service.translateKeyDown(keyCode, event)) {
                        if (service.DEBUG)
                            Log.i(service.TAG, "Onkeydown():tranlatekeydown:true");
                        return true;
                    }
                }

        }

        if ((service.hasCtrlPress || service.hasMenuPress) && !service.mEnglishOnly) { // Jeremy '12,4,29 use mEnglishOnly instead of onIM
            int primaryKey = event.getUnicodeChar(LIMEMetaKeyKeyListener.getMetaState(service.mMetaState));
            char t = (char) primaryKey;

            if (service.hasCtrlPress && // Only working with ctrl Jeremy '11,8,22
                    service.mCandidateList != null && service.mCandidateList.size() > 0
                    && service.mCandidateView != null && service.hasCandidatesShown) {
                switch (keyCode) {
                    case 8:
                        service.pickCandidateManually(0);
                        return true;
                    case 9:
                        service.pickCandidateManually(1);
                        return true;
                    case 10:
                        service.pickCandidateManually(2);
                        return true;
                    case 11:
                        service.pickCandidateManually(3);
                        return true;
                    case 12:
                        service.pickCandidateManually(4);
                        return true;
                    case 13:
                        service.pickCandidateManually(5);
                        return true;
                    case 14:
                        service.pickCandidateManually(6);
                        return true;
                    case 15:
                        service.pickCandidateManually(7);
                        return true;
                    case 16:
                        service.pickCandidateManually(8);
                        return true;
                    case 7:
                        service.pickCandidateManually(9);
                        return true;
                }
            }
            if ((service.mComposing == null || service.mComposing.length() == 0)) {
                // Jeremy '11,8,21. Ctrl-/ to fetch full-shaped chinese symbols1 in
                // candidateview.
                if (t == '/') {
                    if (service.hasMenuPress)
                        service.hasMenuProcessed = true;
                    service.updateChineseSymbol();
                    return true;
                }
                // 27.May.2011 Art : when user click Ctrl + Symbol or number then send Chinese
                // Symobl Characters
                String s = ChineseSymbol.getSymbol(t);
                if (s != null) {
                    service.clearSuggestions();
                    service.getCurrentInputConnection().commitText(s, 0);
                    service.hasSymbolEntered = true;
                    if (service.hasMenuPress)
                        service.hasMenuProcessed = true;
                    return true;

                }
            }
        }

        return service.superOnKeyDown(keyCode, event);
    }

    void setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState() {
        InputConnection ic = service.getCurrentInputConnection();
        if (ic != null) {
            int clearStatesFlags = 0;
            if (LIMEMetaKeyKeyListener.getMetaState(service.mMetaState,
                    LIMEMetaKeyKeyListener.META_ALT_ON) == 0)
                clearStatesFlags += KeyEvent.META_ALT_ON;
            if (LIMEMetaKeyKeyListener.getMetaState(service.mMetaState,
                    LIMEMetaKeyKeyListener.META_SHIFT_ON) == 0)
                clearStatesFlags += KeyEvent.META_SHIFT_ON;
            if (LIMEMetaKeyKeyListener.getMetaState(service.mMetaState,
                    LIMEMetaKeyKeyListener.META_SYM_ON) == 0)
                clearStatesFlags += KeyEvent.META_SYM_ON;
            ic.clearMetaKeyStates(clearStatesFlags);
        }
    }

    boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (service.DEBUG)
            Log.i(service.TAG, "OnKeyUp():keyCode:" + keyCode
                    + ", mComposing = " + service.mComposing
                    + ", hasCtrlPress:" + service.hasCtrlPress
                    + ", hasWinPress:" + service.hasWinPress
                    + ", hasShiftPress = " + service.hasShiftPress
                    + ", event.getEventTime() -  event.getDownTime()" + (event.getEventTime() - event.getDownTime())

            );

        switch (keyCode) {
            // Jeremy '11,5,29 Bypass search and menu keys.
            // case KeyEvent.KEYCODE_SEARCH:
            // hasSearchPress = false;
            // if(hasSearchProcessed) return true;
            // break;
            case KeyEvent.KEYCODE_CAPS_LOCK:
                // Modified by Art 20130607
                // to switch the cap lock mode
                service.toggleCapsLock();
            case KeyEvent.KEYCODE_MENU:
                service.hasMenuPress = false;
                if (service.hasMenuProcessed)
                    return true;
                break;
            // */------------------------------------------------------------------------
            // Modified by Jeremy '10, 3,12
            // keep track of alt state with mHasAlt.
            // Modified '10, 3, 24 for bug fix and alt-lock implementation
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                service.hasShiftPress = false;
                service.mMetaState = LIMEMetaKeyKeyListener.handleKeyUp(service.mMetaState, keyCode, event);
                // '11,8,28 Jeremy popup keyboard picker instead of nextIM when onIM
                // '11,5,14 Jeremy ctrl-shift switch to next available keyboard;
                // '11,5,24 blocking switching if full-shape symbol
                if (!service.hasSymbolEntered && !service.mEnglishOnly && (service.hasMenuPress || service.hasCtrlPress)) {
                    // nextActiveKeyboard(true);
                    service.showIMPicker(); // Jeremy '11,8,28
                    if (service.hasMenuPress) {
                        service.hasMenuProcessed = true;
                        service.hasMenuPress = false;
                    }
                    service.mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(service.mMetaState);
                    service.setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState();
                    return true;
                } else if (service.mLIMEPref.getShiftSwitchEnglishMode() && service.onlyShiftPress) {
                    service.switchChiEng();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
                service.mMetaState = LIMEMetaKeyKeyListener.handleKeyUp(service.mMetaState, keyCode, event);
                break;
            case MY_KEYCODE_CTRL_LEFT:
            case MY_KEYCODE_CTRL_RIGHT:
                service.hasCtrlPress = false;
                break;
            case MY_KEYCODE_WINDOWS_START:
                if (service.hasSpaceProcessed) // Jeremy '12,5,20 long press to show IM picker, switch chi/eng otherwise for
                                       // the win+space or earth key on zippy
                    if (event.getEventTime() - event.getDownTime() > service.mLongPressKeyTimeout)
                        service.showIMPicker();
                    else
                        service.switchChiEng();
                service.hasWinPress = false;
                break;
            case KeyEvent.KEYCODE_ENTER:
                // Add by Jeremy '10, 3 ,29. Pick selected selection if candidates
                // shown.
                // Does not block real enter after select the suggestion. !! need
                // fix here!!
                // Let the underlying text editor always handle these, if return
                // false from takeSelectedSuggestion().

                if (service.hasEnterProcessed) {
                    return true;
                }
                // Jeremy '10, 4, 12 bug fix on repeated enter.
                break;

            case KeyEvent.KEYCODE_SYM:
            case KeyEvent.KEYCODE_AT:
                if (service.hasKeyProcessed) { // (keyPressTime != 0
                    // && System.currentTimeMillis() - keyPressTime > 700) {
                    // switchChiEng(); // Jeremy '11,8,15 moved to onKeyDown()
                    return true;
                } else if (LIMEMetaKeyKeyListener.getMetaState(service.mMetaState,
                        LIMEMetaKeyKeyListener.META_SHIFT_ON) > 0 && !service.mEnglishOnly
                        && !service.mLIMEPref.getPhysicalKeyboardType().equals("xperiapro")) {
                    // alt-@ is conflict with symbol input thus altered to shift-@ Jeremy '11,8,15
                    // alt-@ switch to next active keyboard.
                    // nextActiveKeyboard(true);
                    service.showIMPicker(); // Jeremy '11,8,28
                    service.mMetaState = LIMEMetaKeyKeyListener.adjustMetaAfterKeypress(service.mMetaState);
                    service.setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState();
                    return true;
                    // Long press physical @ key to swtich chn/eng
                } else if ((!service.mEnglishOnly || service.mPredictionOn)
                        && service.translateKeyDown(keyCode, event)) {
                    return true;
                } else {
                    service.translateKeyDown(keyCode, event);
                    service.superOnKeyDown(keyCode, service.mKeydownEvent);
                }
                break;

            case KeyEvent.KEYCODE_SPACE:
                // Jeremy move the chi/eng switching to on_KEY_UP '11,6,18

                if (!service.spaceKeyPress && service.lastKeyCtrl) { // missing space down event when ctrl-space is pressed
                    service.switchChiEng();
                    return true;
                }

                if (service.hasSpaceProcessed)
                    return true;
            default:

        }
        // Update metakeystate of IC maintained by MetaKeyKeyListerner
        // setInputConnectionMetaStateAsCurrentMetaKeyKeyListenerState(); moved to OnKey
        // by jeremy '12,6,13

        if (service.DEBUG)
            Log.i(service.TAG, "OnKeyUp():keyCode:" + keyCode
                    + ";hasCtrlPress:" + service.hasCtrlPress
                    + ";hasWinPress:" + service.hasWinPress
                    + ", event.getEventTime() -  event.getDownTime()" + (event.getEventTime() - event.getDownTime())
                    + " call super.onKeyUp()");

        return service.superOnKeyUp(keyCode, event);
    }
}
