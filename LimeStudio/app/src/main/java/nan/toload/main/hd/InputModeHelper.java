/*
 * Copyright 2024 The LimeIME Open Source Project
 */

package nan.toload.main.hd;

import android.view.inputmethod.EditorInfo;
import nan.toload.main.hd.global.LIMEPreferenceManager;

public class InputModeHelper {

    public static class InputModeConfig {
        public boolean isEnglishOnly;
        public boolean isPredictionOn;
        public boolean isCompletionOn;
        public int keyboardMode = LIMEKeyboardSwitcher.MODE_TEXT;
        public boolean isPhone = false;
        public boolean isNumber = false;
        public boolean isDateTime = false;
        public boolean isIM = false;
    }

    public static InputModeConfig determineInputMode(EditorInfo attribute, LIMEPreferenceManager prefs,
            int imeOptions, boolean persistentLanguageMode) {
        InputModeConfig config = new InputModeConfig();

        // Defaults
        config.isPredictionOn = true;
        config.isCompletionOn = false;
        config.isEnglishOnly = false;

        switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER: // 0x02
                config.isEnglishOnly = true;
                config.isNumber = true;
                config.keyboardMode = LIMEKeyboardSwitcher.MODE_TEXT; // Passed to setKeyboardMode as text but with
                                                                      // isNumber=true
                break;
            case EditorInfo.TYPE_CLASS_DATETIME: // 0x04
                config.isEnglishOnly = true;
                config.isDateTime = true;
                config.keyboardMode = LIMEKeyboardSwitcher.MODE_TEXT;
                break;
            case EditorInfo.TYPE_CLASS_PHONE: // 0x03
                config.isEnglishOnly = true;
                config.isPhone = true;
                config.keyboardMode = LIMEKeyboardSwitcher.MODE_PHONE;
                break;
            case EditorInfo.TYPE_CLASS_TEXT: // 0x01
                int variation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    config.isPredictionOn = false;
                }

                if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                    config.isPredictionOn = false;
                }

                if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    config.isPredictionOn = false;
                    // Note: completionOn logic usually depends on isFullscreenMode context in
                    // Service,
                    // but we can set a flag here and let Service override if needed, or pass
                    // isFullscreen
                    // For now, let's treat it as a suggestion state.
                    config.isCompletionOn = true;
                }

                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    config.isPredictionOn = false;
                    config.isEnglishOnly = true;
                    config.keyboardMode = LIMEKeyboardSwitcher.MODE_EMAIL; // Maps to previous MODE_EMAIL usage for
                                                                           // password in switch
                    // Actually original code reused MODE_EMAIL for password? Let's check original.
                    // Original: MODE_EMAIL passed.
                } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                    config.isEnglishOnly = true;
                    config.isPredictionOn = false;
                    config.keyboardMode = LIMEKeyboardSwitcher.MODE_EMAIL;
                } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                    config.isPredictionOn = false;
                    config.isEnglishOnly = true;
                    config.keyboardMode = LIMEKeyboardSwitcher.MODE_URL;
                } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                    config.isEnglishOnly = false;
                    config.isIM = true;
                    config.keyboardMode = LIMEKeyboardSwitcher.MODE_IM;
                } else {
                    // Default text
                    handleDefaultText(config, prefs, persistentLanguageMode);
                }
                break;
            default:
                handleDefaultText(config, prefs, persistentLanguageMode);
        }

        return config;
    }

    private static void handleDefaultText(InputModeConfig config, LIMEPreferenceManager prefs,
            boolean persistentLanguageMode) {
        if (persistentLanguageMode) {
            config.isEnglishOnly = prefs.getLanguageMode();
            if (config.isEnglishOnly) {
                config.isPredictionOn = true;
                config.keyboardMode = LIMEKeyboardSwitcher.MODE_TEXT;
            } else {
                config.isEnglishOnly = false;
                // Normal Chinese mode, effectively
            }
        } else {
            config.isEnglishOnly = false;
        }
    }
}
