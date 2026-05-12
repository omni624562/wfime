/*
 * Copyright 2024 The LimeIME Open Source Project
 * Licensed under GPLv3 — see LICENSE for details.
 */

package nan.toload.main.hd;

import android.util.Log;
import android.view.inputmethod.InputConnection;

import java.util.Locale;

import nan.toload.main.hd.data.Mapping;
import nan.toload.main.hd.global.LIMEUtilities;

/**
 * Handles text composition, candidate commit, and character input logic
 * extracted from LIMEService to improve code organisation.
 *
 * All state (mComposing, selectedCandidate, …) remains in LIMEService;
 * this class accesses them through the package-private {@code service} reference.
 */
class TextCompositionManager {

    private final LIMEService service;

    TextCompositionManager(LIMEService service) {
        this.service = service;
    }

    // -------------------------------------------------------------------------
    // clearComposing
    // -------------------------------------------------------------------------

    void clearComposing(boolean forceClearComposing) {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "clearComposing()");
        try {
            if (service.mComposing != null && service.mComposing.length() > 0)
                service.mComposing.setLength(0);
            if (service.mCandidateList != null)
                service.mCandidateList.clear();

            if (forceClearComposing) {
                InputConnection ic = service.getCurrentInputConnection();
                if (ic != null) {
                    ic.setComposingText("", 0);
                    ic.finishComposingText();
                }
            }

            service.selectedCandidate = null;
            service.clearSuggestions();
        } catch (Exception e) {
            Log.e(LIMEService.TAG, "Error clearing composing: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // commitTyped (InputConnection overload)
    // -------------------------------------------------------------------------

    void commitTyped(InputConnection ic) {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "commitTyped()");
        if (service.selectedCandidate == null)
            return;
        try {
            if ((service.mComposing.length() > 0
                    || !service.selectedCandidate.isComposingCodeRecord())
                    && !LIMEUtilities.isUnicodeSurrogate(service.selectedCandidate.getWord())) {

                if (!service.mEnglishOnly
                        || !service.selectedCandidate.isComposingCodeRecord()
                        || !service.selectedCandidate.isEnglishSuggestionRecord()) {
                    if (service.selectedCandidate != null && service.selectedCandidate.getWord() != null
                            && !service.selectedCandidate.getWord().equals("")) {

                        int firstMatchedLength = 1;

                        if (service.selectedCandidate.getCode() == null
                                || service.selectedCandidate.getCode().equals("")) {
                            firstMatchedLength = 1;
                        }

                        String wordToCommit = service.selectedCandidate.getWord();

                        if (service.selectedCandidate != null
                                && service.selectedCandidate.getCode() != null
                                && service.selectedCandidate.getWord() != null) {
                            if (service.selectedCandidate.getCode()
                                    .toLowerCase(Locale.US)
                                    .equals(service.selectedCandidate.getWord().toLowerCase(Locale.US))) {
                                firstMatchedLength = 1;
                            }
                        }

                        if (LIMEService.DEBUG)
                            Log.i(LIMEService.TAG, "commitTyped() committed Length=" + firstMatchedLength);

                        if (service.mLIMEPref.getHanCovertOption() == 0) {
                            if (ic != null)
                                ic.commitText(wordToCommit, firstMatchedLength);
                        } else {
                            if (service.mLIMEPref.getHanConvertNotify()) {
                                java.util.Calendar now = java.util.Calendar.getInstance();
                                long nowvalue = now.getTimeInMillis();
                                long storevalue = service.mLIMEPref.getParameterLong("han_notify_interval", 0);
                                if (nowvalue - storevalue > 60000) {
                                    if (service.mLIMEPref.getHanCovertOption() == 1) {
                                        android.widget.Toast.makeText(service, nan.toload.main.hd.R.string.han_convert_ts,
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    } else if (service.mLIMEPref.getHanCovertOption() == 2) {
                                        android.widget.Toast.makeText(service, nan.toload.main.hd.R.string.han_convert_st,
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                }
                                service.mLIMEPref.setParameter("han_notify_interval", now.getTimeInMillis());
                            }
                            if (ic != null)
                                ic.commitText(service.SearchSrv.hanConvert(wordToCommit), firstMatchedLength);
                        }

                        if (service.currentSoftKeyboard.contains("wb")
                                || service.selectedCandidate.isEmojiRecord()
                                || service.selectedCandidate.isChinesePunctuationSymbolRecord()) {
                            clearComposing(true);
                        }

                        boolean composingNotFinish = false;
                        int committedCodeLength = service.SearchSrv.getRealCodeLength(
                                service.selectedCandidate, service.mComposing.toString());

                        if (LIMEService.DEBUG)
                            Log.i(LIMEService.TAG, "commitTyped(): committedCodeLength = " + committedCodeLength);

                        if (service.mComposing.length() > service.selectedCandidate.getCode().length()) {
                            composingNotFinish = true;
                        }

                        boolean shouldUpdateCandidates = false;
                        if (composingNotFinish) {
                            if (service.LDComposingBuffer.length() == 0) {
                                service.LDComposingBuffer = service.mComposing.toString();
                                if (LIMEService.DEBUG)
                                    Log.i(LIMEService.TAG, "commitTyped():starting LD process, LDBuffer="
                                            + service.LDComposingBuffer
                                            + ". just committed code= '" + service.selectedCandidate.getCode() + "'");
                                service.SearchSrv.addLDPhrase(service.selectedCandidate, false);
                            } else {
                                if (LIMEService.DEBUG)
                                    Log.i(LIMEService.TAG, "commitTyped():Continuous LD process, LDBuffer='"
                                            + service.LDComposingBuffer
                                            + "'. just committed code=" + service.selectedCandidate.getCode());
                                service.SearchSrv.addLDPhrase(service.selectedCandidate, false);
                            }
                            service.mComposing = service.mComposing.delete(0, committedCodeLength);
                            if (LIMEService.DEBUG)
                                Log.i(LIMEService.TAG, "commitTyped(): trimmed mComposing = '"
                                        + service.mComposing + "', + mComposing.length = "
                                        + service.mComposing.length());

                            if (!service.mComposing.toString().equals(" ")) {
                                if (service.mComposing.toString().startsWith(" "))
                                    service.mComposing = service.mComposing.deleteCharAt(0);
                                if (LIMEService.DEBUG)
                                    Log.i(LIMEService.TAG, "commitTyped(): new mComposing:'" + service.mComposing + "'");
                                if (service.mComposing.length() > 0) {
                                    shouldUpdateCandidates = true;
                                }
                            }
                        } else {
                            if (service.LDComposingBuffer.length() > 0) {
                                if (LIMEService.DEBUG)
                                    Log.i(LIMEService.TAG, "commitTyped():Ending LD process, LDBuffer="
                                            + service.LDComposingBuffer
                                            + ". just committed code=" + service.selectedCandidate.getCode());
                                service.LDComposingBuffer = "";
                                service.SearchSrv.addLDPhrase(service.selectedCandidate, true);
                            } else if (service.LDComposingBuffer.length() > 0) {
                                if (LIMEService.DEBUG)
                                    Log.i(LIMEService.TAG, "commitTyped():LD process interrupted, LDBuffer="
                                            + service.LDComposingBuffer
                                            + ". just committed code=" + service.selectedCandidate.getCode());
                                service.LDComposingBuffer = "";
                                service.SearchSrv.addLDPhrase(null, true);
                            }
                        }

                        if (shouldUpdateCandidates) {
                            service.updateCandidates();
                        } else {
                            service.committedCandidate = new Mapping(service.selectedCandidate);
                            service.selectedCandidate = null;
                            clearComposing(false);
                            service.updateRelatedPhrase(false);

                            if (service.committedCandidate != null
                                    && service.committedCandidate.getWord() != null) {
                                service.SearchSrv.learnRelatedPhraseAndUpdateScore(service.committedCandidate);
                                service.SearchSrv.getCodeListStringFromWord(service.committedCandidate.getWord());
                            }
                        }

                    } else {
                        if (ic != null)
                            ic.commitText(service.mComposing, service.mComposing.length());
                    }
                } else {
                    if (ic != null) {
                        ic.commitText(service.mComposing, service.mComposing.length());
                        if (!service.mEnglishOnly)
                            clearComposing(false);
                    }
                }

            } else if (LIMEUtilities.isUnicodeSurrogate(service.selectedCandidate.getWord())) {
                ic.commitText(service.selectedCandidate.getWord(), 1);
                clearComposing(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // handleCharacter
    // -------------------------------------------------------------------------

    void handleCharacter(int primaryCode) {
        if (LIMEService.DEBUG)
            Log.i(LIMEService.TAG, "handleCharacter():primaryCode:" + primaryCode
                    + ", metaState = " + service.mMetaState
                    + ", hasPhysicalKeyPressed = " + service.hasPhysicalKeyPressed
                    + ", currentSoftKeyboard=" + service.currentSoftKeyboard);

        if (service.hasPhysicalKeyPressed
                && (service.mCandidateView != null && service.hasCandidatesShown)) {
            if (service.handleSelkey(primaryCode)) {
                service.updateShiftKeyState(service.getCurrentInputEditorInfo());
                if (LIMEService.DEBUG)
                    Log.i(LIMEService.TAG, "handleCharacter() sel key found return now");
                return;
            }
        }

        if (!service.mEnglishOnly) {
            InputConnection ic = service.getCurrentInputConnection();

            if (LIMEService.DEBUG)
                Log.i(LIMEService.TAG, "HandleCharacter():"
                        + " ic != null:" + (ic != null)
                        + " isValidLetter:" + service.isValidLetter(primaryCode)
                        + " isValidDigit:" + service.isValidDigit(primaryCode)
                        + " isValidSymbol:" + service.isValidSymbol(primaryCode)
                        + " hasSymbolMapping:" + service.hasSymbolMapping
                        + " hasNumberMapping:" + service.hasNumberMapping
                        + " mEnglishOnly:" + service.mEnglishOnly);

            if ((!service.hasSymbolMapping) && (primaryCode == ',' || primaryCode == '.')) {
                service.mComposing.append((char) primaryCode);
                service.getComposingDisplayString(service.mComposing.toString());
                service.updateCandidates();
            } else if (!service.hasSymbolMapping && !service.hasNumberMapping
                    && (service.isValidLetter(primaryCode)
                            || (primaryCode == LIMEService.MY_KEYCODE_SPACE
                                    && service.activeIM.equals("phonetic")))
                    && !service.mEnglishOnly) {
                service.mComposing.append((char) primaryCode);
                service.getComposingDisplayString(service.mComposing.toString());
                service.updateCandidates();
            } else if (!service.hasSymbolMapping && service.hasNumberMapping
                    && (service.isValidLetter(primaryCode) || service.isValidDigit(primaryCode))
                    && !service.mEnglishOnly) {
                service.mComposing.append((char) primaryCode);
                service.getComposingDisplayString(service.mComposing.toString());
                service.updateCandidates();
            } else if (service.hasSymbolMapping && !service.hasNumberMapping
                    && (service.isValidLetter(primaryCode) || service.isValidSymbol(primaryCode)
                            || (primaryCode == LIMEService.MY_KEYCODE_SPACE
                                    && service.activeIM.equals("phonetic")))
                    && !service.mEnglishOnly) {
                service.mComposing.append((char) primaryCode);
                service.getComposingDisplayString(service.mComposing.toString());
                service.updateCandidates();
            } else if (service.hasSymbolMapping && service.hasNumberMapping
                    && (service.isValidSymbol(primaryCode)
                            || (primaryCode == LIMEService.MY_KEYCODE_SPACE
                                    && service.activeIM.equals("phonetic"))
                            || service.isValidLetter(primaryCode)
                            || service.isValidDigit(primaryCode))
                    && !service.mEnglishOnly) {
                service.mComposing.append((char) primaryCode);
                service.getComposingDisplayString(service.mComposing.toString());
                service.updateCandidates();
            } else {
                if (LIMEService.DEBUG)
                    Log.i(LIMEService.TAG, "handleCharacter() fallback case: primaryCode="
                            + primaryCode + " char=" + (char) primaryCode);
                if (service.hasCandidatesShown) {
                    if (!service.pickHighlightedCandidate()) {
                        if (ic != null)
                            ic.commitText(String.valueOf((char) primaryCode), 1);
                    }
                } else {
                    if (ic != null)
                        ic.commitText(String.valueOf((char) primaryCode), 1);
                }
                service.finishComposing();
            }

        } else {
            if (LIMEService.DEBUG)
                Log.i(LIMEService.TAG, "handleCharacter() english only mode without prediction, committext = "
                        + (char) primaryCode);
            if (service.isInputViewShown()) {
                if (service.mInputView.isShifted()) {
                    primaryCode = Character.toUpperCase(primaryCode);
                }
            }

            if (service.mLIMEPref.getEnglishPrediction() && service.mPredictionOn
                    && !service.mKeyboardSwitcher.isSymbols()
                    && (!service.hasPhysicalKeyPressed
                            || service.mLIMEPref.getEnglishPredictionOnPhysicalKeyboard())) {
                if (Character.isLetter((char) primaryCode)) {
                    service.tempEnglishWord.append((char) primaryCode);
                    service.updateEnglishPrediction();
                } else {
                    service.resetTempEnglishWord();
                    service.updateEnglishPrediction();
                }
            }

            service.getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
        }

        if (!(!service.hasPhysicalKeyPressed && service.hasDistinctMultitouch))
            service.updateShiftKeyState(service.getCurrentInputEditorInfo());
    }
}
