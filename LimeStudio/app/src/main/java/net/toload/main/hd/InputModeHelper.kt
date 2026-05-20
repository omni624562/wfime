/*
 * Copyright 2024 The LimeIME Open Source Project
 */

package net.toload.main.hd

import android.view.inputmethod.EditorInfo
import net.toload.main.hd.global.LIMEPreferenceManager

class InputModeHelper {

    class InputModeConfig {
        @JvmField var isEnglishOnly: Boolean = false
        @JvmField var isPredictionOn: Boolean = false
        @JvmField var isCompletionOn: Boolean = false
        @JvmField var keyboardMode: Int = LIMEKeyboardSwitcher.MODE_TEXT
        @JvmField var isPhone: Boolean = false
        @JvmField var isNumber: Boolean = false
        @JvmField var isDateTime: Boolean = false
        @JvmField var isIM: Boolean = false
    }

    companion object {
        @JvmStatic
        fun determineInputMode(
            attribute: EditorInfo,
            prefs: LIMEPreferenceManager,
            imeOptions: Int,
            persistentLanguageMode: Boolean
        ): InputModeConfig {
            val config = InputModeConfig()

            // Defaults
            config.isPredictionOn = true
            config.isCompletionOn = false
            config.isEnglishOnly = false

            when (attribute.inputType and EditorInfo.TYPE_MASK_CLASS) {
                EditorInfo.TYPE_CLASS_NUMBER -> { // 0x02
                    config.isEnglishOnly = true
                    config.isNumber = true
                    config.keyboardMode = LIMEKeyboardSwitcher.MODE_NUMBER
                }
                EditorInfo.TYPE_CLASS_DATETIME -> { // 0x04
                    config.isEnglishOnly = true
                    config.isDateTime = true
                    config.keyboardMode = LIMEKeyboardSwitcher.MODE_TEXT
                }
                EditorInfo.TYPE_CLASS_PHONE -> { // 0x03
                    config.isEnglishOnly = true
                    config.isPhone = true
                    config.keyboardMode = LIMEKeyboardSwitcher.MODE_PHONE
                }
                EditorInfo.TYPE_CLASS_TEXT -> { // 0x01
                    val variation = attribute.inputType and EditorInfo.TYPE_MASK_VARIATION

                    if (variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                        config.isPredictionOn = false
                    }

                    if ((attribute.inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                        config.isPredictionOn = false
                    }

                    if ((attribute.inputType and EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                        config.isPredictionOn = false
                        // Note: completionOn logic usually depends on isFullscreenMode context in Service,
                        // but we can set a flag here and let Service override if needed.
                        config.isCompletionOn = true
                    }

                    if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    ) {
                        config.isPredictionOn = false
                        config.isEnglishOnly = true
                        config.keyboardMode = LIMEKeyboardSwitcher.MODE_EMAIL
                    } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                    ) {
                        config.isEnglishOnly = true
                        config.isPredictionOn = false
                        config.keyboardMode = LIMEKeyboardSwitcher.MODE_EMAIL
                    } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_URI) {
                        config.isPredictionOn = false
                        config.isEnglishOnly = true
                        config.keyboardMode = LIMEKeyboardSwitcher.MODE_URL
                    } else if (variation == EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                        config.isEnglishOnly = false
                        config.isIM = true
                        config.keyboardMode = LIMEKeyboardSwitcher.MODE_IM
                    } else {
                        // Default text
                        handleDefaultText(config, prefs, persistentLanguageMode)
                    }
                }
                else -> {
                    handleDefaultText(config, prefs, persistentLanguageMode)
                }
            }

            return config
        }

        private fun handleDefaultText(
            config: InputModeConfig,
            prefs: LIMEPreferenceManager,
            persistentLanguageMode: Boolean
        ) {
            if (persistentLanguageMode) {
                config.isEnglishOnly = prefs.languageMode
                if (config.isEnglishOnly) {
                    config.isPredictionOn = true
                    config.keyboardMode = LIMEKeyboardSwitcher.MODE_TEXT
                } else {
                    config.isEnglishOnly = false
                    // Normal Chinese mode, effectively
                }
            } else {
                config.isEnglishOnly = false
            }
        }
    }
}
