/*
 * Copyright 2024 The LimeIME Open Source Project
 * Licensed under GPLv3 — see LICENSE for details.
 */

package net.toload.main.hd;

import android.content.res.Configuration;
import android.util.Log;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputConnection;

import java.util.LinkedList;
import java.util.List;

import net.toload.main.hd.data.Mapping;

/**
 * Handles candidate view display, suggestion population, and candidate
 * selection logic extracted from LIMEService.
 */
class CandidateController {

    private final LIMEService service;

    CandidateController(LIMEService service) {
        this.service = service;
    }

    void initCandidateView() {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "initCandidateView()");

        service.mCandidateViewHandler.showCandidateView();
        service.mCandidateViewHandler.hideCandidateView();
    }

    void showCandidateView() {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "showCandidateView()");
        if (service.hasPhysicalKeyPressed) {
            service.requestShowSelf(0);
        }

        Configuration config = service.getResources().getConfiguration();
        boolean isPhysicalKeyboardConnected = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
        boolean useFixedMode = service.mFixedCandidateViewOn || isPhysicalKeyboardConnected;

        if (!useFixedMode) {
            service.mCandidateViewHandler.showCandidateView();
        }
    }

    void hideCandidateView() {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "hideCandidateView()");
        if (service.mCandidateView != null)
            service.mCandidateView.clear();
        service.hasCandidatesShown = false;
        service.hasChineseSymbolCandidatesShown = false;
        if (service.mCandidateViewStandAlone == null || (!service.mCandidateViewStandAlone.isShown()))
            return; // escape if mCandidateViewStandAlone is not created or it's not shown '12,5,6,
                    // Jeremy

        service.mCandidateViewHandler.hideCandidateViewDelayed(LIMEService.DELAY_BEFORE_HIDE_CANDIDATE_VIEW);

    }

    void forceHideCandidateView() {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "forceHideCandidateView()");

        if (service.mComposing != null && service.mComposing.length() > 0)
            service.mComposing.setLength(0);

        service.selectedCandidate = null;
        // selectedIndex = 0;

        if (service.mCandidateList != null)
            service.mCandidateList.clear();

        if (service.mFixedCandidateViewOn) {
            service.mCandidateViewInInputView.forceHide();
        } else {
            hideCandidateView();
        }
    }

    void setSuggestions(List<Mapping> suggestions, boolean showNumber, String diplaySelkey) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            service.mMainHandler.post(() -> setSuggestions(suggestions, showNumber, diplaySelkey));
            return;
        }

        if (suggestions != null && suggestions.size() > 0) {

            if (LIMEService.DEBUG)
                Log.i(LIMEService.TAG, "setSuggestion():suggestions.size=" + suggestions.size()
                        + ", mComposing = " + service.mComposing
                        + ", mFixedCandidateViewOn:" + service.mFixedCandidateViewOn
                        + ", hasPhysicalKeyPressed:" + service.hasPhysicalKeyPressed);

            Configuration config = service.getResources().getConfiguration();
            boolean isPhysicalKeyboardConnected = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;

            if (isPhysicalKeyboardConnected) {
                // When physical keyboard is connected, we use the candidate view inside InputView
                // which is forced by updateInputViewContainer()
                if (service.mCandidateViewInInputView != null) {
                    service.mCandidateView = service.mCandidateViewInInputView;
                    if (service.mCandidateViewStandAlone != null) service.mCandidateViewStandAlone.clear();
                }

                // Ensure IME window is shown but don't force separate candidate window
                service.requestShowSelf(0);
                service.setCandidatesViewShown(false);
            } else if (!service.mFixedCandidateViewOn && service.mCandidateView != service.mCandidateViewStandAlone) {
                service.mCandidateViewInInputView.clear();
                service.mCandidateView = service.mCandidateViewStandAlone;
            } else if (service.mFixedCandidateViewOn && service.mCandidateView != service.mCandidateViewInInputView) {
                service.mCandidateViewStandAlone.clear();
                hideCandidateView();
                service.mCandidateView = service.mCandidateViewInInputView;
                if (service.mCandidateViewStandAlone != null)
                    service.mCandidateViewStandAlone.setEmbeddedComposingView(null);
            }

            showCandidateView();

            service.hasCandidatesShown = true; // Jeremy '15,6,1 move after hideCandidateView if candidateView is fixed.
            service.hasMappingList = true;

            if (service.mCandidateView != null) {
                service.mCandidateList = (LinkedList<Mapping>) suggestions;
                try {

                    // Default selection: first candidate, unless it is the raw
                    // composing-code record — then prefer the exact-match word
                    // after it. (Composing-code records are filtered from Chinese
                    // queries now; this guard keeps other paths correct.)
                    service.selectedCandidate = suggestions.get(0);
                    if (service.selectedCandidate.isComposingCodeRecord()
                            && suggestions.size() > 1 && suggestions.get(1).isExactMatchToCodeRecord()) {
                        service.selectedCandidate = suggestions.get(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                service.mCandidateView.setSuggestions(suggestions, showNumber, diplaySelkey);
                if (LIMEService.DEBUG)
                    Log.i(LIMEService.TAG, "setSuggestion(): mCandidateList.size: " + service.mCandidateList.size()
                            + ", mComposing = " + service.mComposing);
            }
        } else {
            if (LIMEService.DEBUG)
                Log.i(LIMEService.TAG, "setSuggestion() with list=null");
            service.hasMappingList = false;
            // Jeremy '11,8,15
            clearSuggestions();
        }

    }

    /**
     * Clear suggestions or candidates in candidate view.
     */
    void clearSuggestions() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            service.mMainHandler.post(this::clearSuggestions);
            return;
        }
        if (service.mCandidateView != null) {
            if (LIMEService.DEBUG)
                Log.i(LIMEService.TAG, "clearSuggestions(): "
                        + ", hasCandidatesShown:" + service.hasCandidatesShown);

            if (!service.mEnglishOnly && service.mLIMEPref.getAutoChineseSymbol() // Jeremy '12,4,29 use mEnglishOnly instead of onIM
                    && (service.hasCandidatesShown || service.mFixedCandidateViewOn)) { // Change isCandiateShown() to hasCandiatesShown
                service.mCandidateView.clear();
                if (service.hasCandidatesShown)
                    service.updateChineseSymbol(); // Jeremy '12.5,23 do not show chinesesymbol when init for fixed candidate
                                           // view.
            } else {
                service.mCandidateView.clear();
                hideCandidateView();
            }

        }
        service.hideComposingPopup();
    }

    // Push the root-name string (e.g. 木牛舟) into both candidate views for
    // the physical-keyboard fixed slot / tablet inline box. Posted to the
    // main thread because callers may run on the query executor.
    void updateComposingRootsDisplay(String roots) {
        service.mMainHandler.post(() -> {
            if (service.mCandidateViewStandAlone != null)
                service.mCandidateViewStandAlone.setComposingText(roots);
            if (service.mCandidateViewInInputView != null)
                service.mCandidateViewInInputView.setComposingText(roots);
        });
    }

    // Jeremy '12,5,11 add return value from mCandidate.takeselectedsuggestion()
    boolean pickHighlightedCandidate() {
        return service.mCandidateView != null && service.mCandidateView.takeSelectedSuggestion();
    }

    void pickCandidateManually(int index) {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "pickCandidateManually():"
                    + "Pick up candidate at index : " + index);

        // This is to prevent if user select the index more than the list
        if (service.mCandidateList != null && index >= service.mCandidateList.size()) {
            return;
        }

        if (service.mCandidateList != null && service.mCandidateList.size() > 0) {
            service.selectedCandidate = service.mCandidateList.get(index);
            // selectedIndex = index;
        }

        InputConnection ic = service.getCurrentInputConnection();

        if (service.mCompletionOn && service.mCompletions != null && index >= 0
                && service.selectedCandidate.isPartialMatchToCodeRecord()
                && index < service.mCompletions.length) { // user picked the completion suggestion item.
            CompletionInfo ci = service.mCompletions[index];
            if (ic != null)
                ic.commitCompletion(ci);
            if (LIMEService.DEBUG)
                Log.i(LIMEService.TAG, "pickSuggestionManually():mCompletionOn:" + service.mCompletionOn);

        } else if ((service.mComposing.length() > 0
                || (service.selectedCandidate != null && !service.selectedCandidate.isComposingCodeRecord()))
                && !service.mEnglishOnly) { // user picked candidates from composing candidate or related phrase candidates
            // Jeremy '12,4,29 use mEnglishOnly instead of onIM
            service.commitTyped(ic);
        } else if (service.mLIMEPref.getEnglishPrediction() && service.tempEnglishList != null
                && service.tempEnglishList.size() > 0) { // user picked English prediction suggestions

            // Log.i("EMOJI-commit-index:", index + "");
            // Log.i("EMOJI-commit:", tempEnglishList.size() + "");

            if (service.tempEnglishList.get(index).isEmojiRecord()) {
                if (ic != null)
                    ic.commitText(
                            service.tempEnglishList.get(index).getWord() + " ", 0);
            } else {
                if (ic != null)
                    ic.commitText(
                            service.tempEnglishList.get(index).getWord()
                                    .substring(service.tempEnglishWord.length())
                                    + " ",
                            0);
            }

            service.resetTempEnglishWord();

            clearSuggestions();

        }

        /*
        if (currentSoftKeyboard.contains("wb")) {
            if (ic != null && mPredictionOn)
                ic.setComposingText("", 0);
        }
        */

    }
}
