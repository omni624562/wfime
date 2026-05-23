/*
 *
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
 */

package net.toload.main.hd.ui.compose.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.toload.main.hd.global.LIMEPreferenceManager

/**
 * UI state for settings screen.
 *
 * Groups preferences by category for better organization.
 */
data class SettingsUiState(
    // Keyboard preferences
    val enableEmoji: Boolean = false,
    val emojiPosition: String = "3",
    val persistentLanguageMode: Boolean = false,
    val numberRowInEnglish: Boolean = true,
    val hideSoftwareKeyboardWithPhysical: Boolean = true,
    val showArrowKey: String = "0",
    val splitKeyboardMode: String = "0",
    val keyboardSize: String = "1",
    val fontSize: String = "1",
    val vibrateOnKeypress: Boolean = true,
    val vibrateLevel: String = "40",
    val soundOnKeypress: Boolean = false,
    val switchEnglishMode: Boolean = false,
    val switchEnglishModeShift: Boolean = true,

    // IM preferences
    val autoChineseSymbol: Boolean = false,
    val disablePhysicalSelkey: Boolean = false,
    val autoCommit: String = "0",
    val selkeyOption: String = "0",
    val phoneticKeyboardType: String = "standard",
    val physicalKeyboardType: String = "normal_keyboard",
    val reverseLookupNotify: Boolean = true,

    // Mapping preferences
    val similiarList: String = "20",
    val similiarEnable: Boolean = true,
    val englishDictionaryEnable: Boolean = true,
    val englishDictionaryPhysicalKeyboard: Boolean = false,
    val candidateSwitch: Boolean = true,
    val candidateSuggestion: Boolean = true,
    val learnPhrase: Boolean = true,
    val learningSwitch: Boolean = true,
    val physicalKeyboardSort: Boolean = true,
    val acceptNumberIndex: Boolean = false,
    val acceptSymbolIndex: Boolean = false,

    // IM loading status
    val isPhoneticImported: Boolean = false,
    val isDayiImported: Boolean = false,

    // IM activation status
    val enableDayi: Boolean = true,
    val enablePhonetic: Boolean = true
)

/**
 * ViewModel for managing settings state.
 *
 * Handles:
 * - Loading preferences from SharedPreferences
 * - Saving preference changes
 * - Providing reactive state to UI
 */
class SettingsViewModel(
    private val context: Context
) : ViewModel() {
    private val preferenceManager = LIMEPreferenceManager(context)
    private val limeDb = net.toload.main.hd.limedb.LimeDB(context)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    /**
     * Checks DB on IO thread and returns (isPhonetic, isDayi) import status.
     */
    private suspend fun fetchImportStatus(): Pair<Boolean, Boolean> = withContext(Dispatchers.IO) {
        val check = HashMap<String, String>()
        try {
            val imlist = limeDb.getIm(null, net.toload.main.hd.Lime.IM_TYPE_NAME)
            imlist?.forEach { im -> check[im.code] = im.desc }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val isPhonetic = check[net.toload.main.hd.Lime.DB_TABLE_PHONETIC] != null
        val isDayi = check[net.toload.main.hd.Lime.DB_TABLE_DAYI] != null
        isPhonetic to isDayi
    }

    /**
     * Refresh input method database import status (runs DB query off main thread).
     */
    fun refreshImportStatus() {
        viewModelScope.launch {
            val (isPhonetic, isDayi) = fetchImportStatus()
            _uiState.update { it.copy(isPhoneticImported = isPhonetic, isDayiImported = isDayi) }
        }
    }

    /**
     * Loads all preferences from SharedPreferences (DB query runs on IO thread).
     */
    private fun loadPreferences() {
        viewModelScope.launch {
            val (isPhonetic, isDayi) = fetchImportStatus()
            // Read SharedPreferences — fast enough to keep on main thread after IO completes.
            _uiState.update {
                SettingsUiState(
                    // Keyboard
                    enableEmoji = preferenceManager.getParameterBoolean("enable_emoji", false),
                    emojiPosition = preferenceManager.getParameterString("enable_emoji_position", "3"),
                    persistentLanguageMode = preferenceManager.getParameterBoolean("persistent_language_mode", false),
                    numberRowInEnglish = preferenceManager.getParameterBoolean("number_row_in_english", true),
                    hideSoftwareKeyboardWithPhysical = preferenceManager.getParameterBoolean("hide_software_keyboard_typing_with_physical", true),
                    showArrowKey = preferenceManager.getParameterString("show_arrow_key", "0"),
                    splitKeyboardMode = preferenceManager.getParameterString("split_keyboard_mode", "0"),
                    keyboardSize = preferenceManager.getParameterString("keyboard_size", "1"),
                    fontSize = preferenceManager.getParameterString("font_size", "1"),
                    vibrateOnKeypress = preferenceManager.getParameterBoolean("vibrate_on_keypress", true),
                    vibrateLevel = preferenceManager.getParameterString("vibrate_level", "40"),
                    soundOnKeypress = preferenceManager.getParameterBoolean("sound_on_keypress", false),
                    switchEnglishMode = preferenceManager.getParameterBoolean("switch_english_mode", false),
                    switchEnglishModeShift = preferenceManager.getParameterBoolean("switch_english_mode_shift", true),

                    // IM
                    autoChineseSymbol = preferenceManager.getParameterBoolean("auto_chinese_symbol", false),
                    disablePhysicalSelkey = preferenceManager.getParameterBoolean("disable_physical_selkey", false),
                    autoCommit = preferenceManager.getParameterString("auto_commit", "0"),
                    selkeyOption = preferenceManager.getParameterString("selkey_option", "0"),
                    phoneticKeyboardType = preferenceManager.getPhoneticKeyboardType(),
                    physicalKeyboardType = preferenceManager.getParameterString("physical_keyboard_type", "normal_keyboard"),
                    reverseLookupNotify = preferenceManager.getParameterBoolean("reverse_lookup_notify", true),

                    // Mapping
                    similiarList = preferenceManager.getParameterString("similiar_list", "20"),
                    similiarEnable = preferenceManager.getParameterBoolean("similiar_enable", true),
                    englishDictionaryEnable = preferenceManager.getParameterBoolean("english_dictionary_enable", true),
                    englishDictionaryPhysicalKeyboard = preferenceManager.getParameterBoolean("english_dictionary_physical_keyboard", false),
                    candidateSwitch = preferenceManager.getParameterBoolean("candidate_switch", true),
                    candidateSuggestion = preferenceManager.getParameterBoolean("candidate_suggestion", true),
                    learnPhrase = preferenceManager.getParameterBoolean("learn_phrase", true),
                    learningSwitch = preferenceManager.getParameterBoolean("learning_switch", true),
                    physicalKeyboardSort = preferenceManager.getParameterBoolean("physical_keyboard_sort", true),
                    acceptNumberIndex = preferenceManager.getParameterBoolean("accept_number_index", false),
                    acceptSymbolIndex = preferenceManager.getParameterBoolean("accept_symbol_index", false),
                    isPhoneticImported = isPhonetic,
                    isDayiImported = isDayi,
                    enableDayi = preferenceManager.getIMActivatedState().contains("0"),
                    enablePhonetic = preferenceManager.getIMActivatedState().contains("1")
                )
            }
        }
    }

    // Keyboard preference setters

    fun setEnableEmoji(value: Boolean) {
        preferenceManager.setParameter("enable_emoji", value)
        _uiState.update { it.copy(enableEmoji = value) }
    }

    fun setEmojiPosition(value: String) {
        preferenceManager.setParameter("enable_emoji_position", value)
        _uiState.update { it.copy(emojiPosition = value) }
    }

    fun setPersistentLanguageMode(value: Boolean) {
        preferenceManager.setParameter("persistent_language_mode", value)
        _uiState.update { it.copy(persistentLanguageMode = value) }
    }

    fun setNumberRowInEnglish(value: Boolean) {
        preferenceManager.setParameter("number_row_in_english", value)
        _uiState.update { it.copy(numberRowInEnglish = value) }
    }

    fun setHideSoftwareKeyboardWithPhysical(value: Boolean) {
        preferenceManager.setParameter("hide_software_keyboard_typing_with_physical", value)
        _uiState.update { it.copy(hideSoftwareKeyboardWithPhysical = value) }
    }

    fun setShowArrowKey(value: String) {
        preferenceManager.setParameter("show_arrow_key", value)
        _uiState.update { it.copy(showArrowKey = value) }
    }

    fun setSplitKeyboardMode(value: String) {
        preferenceManager.setParameter("split_keyboard_mode", value)
        _uiState.update { it.copy(splitKeyboardMode = value) }
    }

    fun setKeyboardSize(value: String) {
        preferenceManager.setParameter("keyboard_size", value)
        _uiState.update { it.copy(keyboardSize = value) }
    }

    fun setFontSize(value: String) {
        preferenceManager.setParameter("font_size", value)
        _uiState.update { it.copy(fontSize = value) }
    }

    fun setVibrateOnKeypress(value: Boolean) {
        preferenceManager.setParameter("vibrate_on_keypress", value)
        _uiState.update { it.copy(vibrateOnKeypress = value) }
    }

    fun setVibrateLevel(value: String) {
        preferenceManager.setParameter("vibrate_level", value)
        _uiState.update { it.copy(vibrateLevel = value) }
    }

    fun setSoundOnKeypress(value: Boolean) {
        preferenceManager.setParameter("sound_on_keypress", value)
        _uiState.update { it.copy(soundOnKeypress = value) }
    }

    fun setSwitchEnglishMode(value: Boolean) {
        preferenceManager.setParameter("switch_english_mode", value)
        _uiState.update { it.copy(switchEnglishMode = value) }
    }

    fun setSwitchEnglishModeShift(value: Boolean) {
        preferenceManager.setParameter("switch_english_mode_shift", value)
        _uiState.update { it.copy(switchEnglishModeShift = value) }
    }

    // IM preference setters

    fun setAutoChineseSymbol(value: Boolean) {
        preferenceManager.setParameter("auto_chinese_symbol", value)
        _uiState.update { it.copy(autoChineseSymbol = value) }
    }

    fun setDisablePhysicalSelkey(value: Boolean) {
        preferenceManager.setParameter("disable_physical_selkey", value)
        _uiState.update { it.copy(disablePhysicalSelkey = value) }
    }

    fun setAutoCommit(value: String) {
        preferenceManager.setParameter("auto_commit", value)
        _uiState.update { it.copy(autoCommit = value) }
    }

    fun setSelkeyOption(value: String) {
        preferenceManager.setParameter("selkey_option", value)
        _uiState.update { it.copy(selkeyOption = value) }
    }

    fun setPhoneticKeyboardType(value: String) {
        preferenceManager.setParameter("phonetic_keyboard_type", value)
        _uiState.update { it.copy(phoneticKeyboardType = value) }
    }

    fun setPhysicalKeyboardType(value: String) {
        preferenceManager.setParameter("physical_keyboard_type", value)
        _uiState.update { it.copy(physicalKeyboardType = value) }
    }

    fun setReverseLookupNotify(value: Boolean) {
        preferenceManager.setParameter("reverse_lookup_notify", value)
        _uiState.update { it.copy(reverseLookupNotify = value) }
    }

    // Mapping preference setters

    fun setSimiliarList(value: String) {
        preferenceManager.setParameter("similiar_list", value)
        _uiState.update { it.copy(similiarList = value) }
    }

    fun setSimiliarEnable(value: Boolean) {
        preferenceManager.setParameter("similiar_enable", value)
        _uiState.update { it.copy(similiarEnable = value) }
    }

    fun setEnglishDictionaryEnable(value: Boolean) {
        preferenceManager.setParameter("english_dictionary_enable", value)
        _uiState.update { it.copy(englishDictionaryEnable = value) }
    }

    fun setEnglishDictionaryPhysicalKeyboard(value: Boolean) {
        preferenceManager.setParameter("english_dictionary_physical_keyboard", value)
        _uiState.update { it.copy(englishDictionaryPhysicalKeyboard = value) }
    }

    fun setCandidateSwitch(value: Boolean) {
        preferenceManager.setParameter("candidate_switch", value)
        _uiState.update { it.copy(candidateSwitch = value) }
    }

    fun setCandidateSuggestion(value: Boolean) {
        preferenceManager.setParameter("candidate_suggestion", value)
        _uiState.update { it.copy(candidateSuggestion = value) }
    }

    fun setLearnPhrase(value: Boolean) {
        preferenceManager.setParameter("learn_phrase", value)
        _uiState.update { it.copy(learnPhrase = value) }
    }

    fun setLearningSwitch(value: Boolean) {
        preferenceManager.setParameter("learning_switch", value)
        _uiState.update { it.copy(learningSwitch = value) }
    }

    fun setPhysicalKeyboardSort(value: Boolean) {
        preferenceManager.setParameter("physical_keyboard_sort", value)
        _uiState.update { it.copy(physicalKeyboardSort = value) }
    }

    fun setAcceptNumberIndex(value: Boolean) {
        preferenceManager.setParameter("accept_number_index", value)
        _uiState.update { it.copy(acceptNumberIndex = value) }
    }

    fun setAcceptSymbolIndex(value: Boolean) {
        preferenceManager.setParameter("accept_symbol_index", value)
        _uiState.update { it.copy(acceptSymbolIndex = value) }
    }

    fun setEnableDayi(enable: Boolean) {
        val state = preferenceManager.getIMActivatedState()
        val hasDayi = state.contains("0")
        val hasPhonetic = state.contains("1")

        if (!enable && !hasPhonetic) {
            // 防呆：不能同時停用大易與注音，必須至少保留一個啟用的中文輸入法
            return
        }

        val newState = if (enable) {
            if (!hasDayi) {
                state + "0;"
            } else {
                state
            }
        } else {
            state.replace("0;", "").replace("0", "")
        }
        preferenceManager.setIMActivatedState(newState)
        loadPreferences()
    }

    fun setEnablePhonetic(enable: Boolean) {
        val state = preferenceManager.getIMActivatedState()
        val hasDayi = state.contains("0")
        val hasPhonetic = state.contains("1")

        if (!enable && !hasDayi) {
            // 防呆：不能同時停用大易與注音，必須至少保留一個啟用的中文輸入法
            return
        }

        val newState = if (enable) {
            if (!hasPhonetic) {
                state + "1;"
            } else {
                state
            }
        } else {
            state.replace("1;", "").replace("1", "")
        }
        preferenceManager.setIMActivatedState(newState)
        loadPreferences()
    }
}

/**
 * Factory for creating SettingsViewModel instances.
 */
class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
