/*
 * Copyright 2024 The LimeIME Open Source Project
 * Licensed under GPLv3 — see LICENSE for details.
 */

package nan.toload.main.hd;

import android.util.Log;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.InputConnection;

import java.util.LinkedList;
import java.util.List;

import nan.toload.main.hd.data.Mapping;

/**
 * Handles candidate view display, suggestion population, and candidate
 * selection logic extracted from LIMEService.
 */
class CandidateController {

    private final LIMEService service;

    CandidateController(LIMEService service) {
        this.service = service;
    }

    // -------------------------------------------------------------------------
    // setSuggestions
    // -------------------------------------------------------------------------

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

            if ((!service.mFixedCandidateViewOn || service.hasPhysicalKeyPressed)
                    && service.mCandidateView != service.mCandidateViewStandAlone) {
                service.mCandidateViewInInputView.clear();
                service.mCandidateView = service.mCandidateViewStandAlone;
                if (service.hasPhysicalKeyPressed) {
                    InputConnection ic = service.getCurrentInputConnection();
                    if (ic != null && service.mPredictionOn)
                        ic.setComposingText("", 0);
                    service.mInputView.closing();
                    service.requestHideSelf(0);
                    if (service.mComposing.length() > 1)
                        service.mComposing.delete(0, service.mComposing.length() - 1);
                    service.updateCandidates();
                }
            } else if ((service.mFixedCandidateViewOn || !service.hasPhysicalKeyPressed) &&
                    service.mCandidateView != service.mCandidateViewInInputView) {
                service.mCandidateViewStandAlone.clear();
                service.hideCandidateView();
                service.mCandidateView = service.mCandidateViewInInputView;
                if (service.mCandidateViewStandAlone != null)
                    service.mCandidateViewStandAlone.setEmbeddedComposingView(null);
            }
            if (!service.mFixedCandidateViewOn || service.hasPhysicalKeyPressed)
                service.showCandidateView();

            service.hasCandidatesShown = true;
            service.hasMappingList = true;

            if (service.mCandidateView != null) {
                service.mCandidateList = (LinkedList<Mapping>) suggestions;
                try {
                    if (suggestions.size() > 1 && suggestions.get(1).isExactMatchToCodeRecord()) {
                        service.selectedCandidate = suggestions.get(1);
                    } else if (suggestions.size() > 0) {
                        service.selectedCandidate = suggestions.get(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                service.mCandidateView.setSuggestions(suggestions, showNumber, diplaySelkey);
                if (LIMEService.DEBUG)
                    Log.i(LIMEService.TAG, "setSuggestion(): mCandidateList.size: "
                            + service.mCandidateList.size()
                            + ", mComposing = " + service.mComposing);
            }
        } else {
            if (LIMEService.DEBUG)
                Log.i(LIMEService.TAG, "setSuggestion() with list=null");
            service.hasMappingList = false;
            service.clearSuggestions();
        }
    }

    // -------------------------------------------------------------------------
    // pickCandidateManually
    // -------------------------------------------------------------------------

    void pickCandidateManually(int index) {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "pickCandidateManually():"
                    + "Pick up candidate at index : " + index);

        if (service.mCandidateList != null && index >= service.mCandidateList.size()) {
            return;
        }

        if (service.mCandidateList != null && service.mCandidateList.size() > 0) {
            service.selectedCandidate = service.mCandidateList.get(index);
        }

        InputConnection ic = service.getCurrentInputConnection();

        if (service.mCompletionOn && service.mCompletions != null && index >= 0
                && service.selectedCandidate.isPartialMatchToCodeRecord()
                && index < service.mCompletions.length) {
            CompletionInfo ci = service.mCompletions[index];
            if (ic != null)
                ic.commitCompletion(ci);
            if (LIMEService.DEBUG)
                Log.i(LIMEService.TAG, "pickSuggestionManually():mCompletionOn:" + service.mCompletionOn);

        } else if ((service.mComposing.length() > 0
                || (service.selectedCandidate != null && !service.selectedCandidate.isComposingCodeRecord()))
                && !service.mEnglishOnly) {
            service.commitTyped(ic);
        } else if (service.mLIMEPref.getEnglishPrediction() && service.tempEnglishList != null
                && service.tempEnglishList.size() > 0) {

            if (service.tempEnglishList.get(index).isEmojiRecord()) {
                if (ic != null)
                    ic.commitText(service.tempEnglishList.get(index).getWord() + " ", 0);
            } else {
                if (ic != null)
                    ic.commitText(
                            service.tempEnglishList.get(index).getWord()
                                    .substring(service.tempEnglishWord.length()) + " ",
                            0);
            }

            service.resetTempEnglishWord();
            service.clearSuggestions();
        }

        if (service.currentSoftKeyboard.contains("wb")) {
            if (ic != null && service.mPredictionOn)
                ic.setComposingText("", 0);
        }
    }
}
