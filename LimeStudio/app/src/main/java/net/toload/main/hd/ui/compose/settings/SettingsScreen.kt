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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.toload.main.hd.R

/**
 * Main settings screen with all preference categories.
 *
 * Displays preferences organized into three categories:
 * - Keyboard
 * - Input Method (IM)
 * - Mapping
 *
 * @param viewModel SettingsViewModel managing preference state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_preference)) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Keyboard Category
            item {
                PreferenceCategory(title = stringResource(R.string.keyboard))
            }

            // Keyboard Theme
            item {
                val themeEntries = stringArrayResource(R.array.keyboard_themes_values)
                val themeLabels = stringArrayResource(R.array.keyboard_themes_options)
                ListPreference(
                    title = stringResource(R.string.keyboard_themes),
                    summary = themeLabels.getOrNull(themeEntries.indexOf(uiState.keyboardTheme))
                        ?: themeLabels.firstOrNull() ?: "",
                    selectedValue = uiState.keyboardTheme,
                    entries = themeEntries.toList(),
                    labels = themeLabels.toList(),
                    onValueSelected = { viewModel.setKeyboardTheme(it) },
                    dialogTitle = stringResource(R.string.keyboard_themes)
                )
            }

            // Enable Emoji
            item {
                SwitchPreference(
                    title = stringResource(R.string.enable_emoji),
                    summary = stringResource(R.string.enable_emoji_summary),
                    checked = uiState.enableEmoji,
                    onCheckedChange = { viewModel.setEnableEmoji(it) }
                )
            }

            // Emoji Position (dependent on enable_emoji)
            item {
                val emojiPosEntries = stringArrayResource(R.array.emoji_display_position)
                val emojiPosLabels = stringArrayResource(R.array.emoji_display_position_title)
                ListPreference(
                    title = stringResource(R.string.enable_emoji_position),
                    summary = emojiPosLabels.getOrNull(emojiPosEntries.indexOf(uiState.emojiPosition))
                        ?: emojiPosLabels.firstOrNull() ?: "",
                    selectedValue = uiState.emojiPosition,
                    entries = emojiPosEntries.toList(),
                    labels = emojiPosLabels.toList(),
                    onValueSelected = { viewModel.setEmojiPosition(it) },
                    dialogTitle = stringResource(R.string.enable_emoji_position_dialog_title),
                    enabled = uiState.enableEmoji
                )
            }

            // Persistent Language Mode
            item {
                SwitchPreference(
                    title = stringResource(R.string.persistent_language_mode),
                    summary = stringResource(R.string.persistent_language_mode_summary),
                    checked = uiState.persistentLanguageMode,
                    onCheckedChange = { viewModel.setPersistentLanguageMode(it) }
                )
            }

            // Number Row in English
            item {
                SwitchPreference(
                    title = stringResource(R.string.number_row_in_english),
                    summary = stringResource(R.string.number_row_in_english_summary),
                    checked = uiState.numberRowInEnglish,
                    onCheckedChange = { viewModel.setNumberRowInEnglish(it) }
                )
            }

            // Hide Software Keyboard with Physical
            item {
                SwitchPreference(
                    title = stringResource(R.string.hide_software_keyboard_typing_with_physical),
                    summary = stringResource(R.string.hide_software_keyboard_typing_with_physical_summary),
                    checked = uiState.hideSoftwareKeyboardWithPhysical,
                    onCheckedChange = { viewModel.setHideSoftwareKeyboardWithPhysical(it) }
                )
            }

            // Show Arrow Keys
            item {
                val arrowKeyEntries = stringArrayResource(R.array.show_arrow_keys_values)
                val arrowKeyLabels = stringArrayResource(R.array.show_arrow_keys_options)
                ListPreference(
                    title = stringResource(R.string.show_arrow_keys),
                    summary = arrowKeyLabels.getOrNull(arrowKeyEntries.indexOf(uiState.showArrowKey))
                        ?: arrowKeyLabels.firstOrNull() ?: "",
                    selectedValue = uiState.showArrowKey,
                    entries = arrowKeyEntries.toList(),
                    labels = arrowKeyLabels.toList(),
                    onValueSelected = { viewModel.setShowArrowKey(it) },
                    dialogTitle = stringResource(R.string.show_arrow_keys)
                )
            }

            // Split Keyboard Mode
            item {
                val splitKbEntries = stringArrayResource(R.array.split_keyboard_values)
                val splitKbLabels = stringArrayResource(R.array.split_keyboard_options)
                ListPreference(
                    title = stringResource(R.string.split_keyboard),
                    summary = splitKbLabels.getOrNull(splitKbEntries.indexOf(uiState.splitKeyboardMode))
                        ?: splitKbLabels.firstOrNull() ?: "",
                    selectedValue = uiState.splitKeyboardMode,
                    entries = splitKbEntries.toList(),
                    labels = splitKbLabels.toList(),
                    onValueSelected = { viewModel.setSplitKeyboardMode(it) },
                    dialogTitle = stringResource(R.string.split_keyboard)
                )
            }

            // Keyboard Size
            item {
                val sizeEntries = stringArrayResource(R.array.five_size_scale_values)
                val sizeLabels = stringArrayResource(R.array.five_size_scale_options)
                ListPreference(
                    title = stringResource(R.string.keyboard_size),
                    summary = sizeLabels.getOrNull(sizeEntries.indexOf(uiState.keyboardSize))
                        ?: sizeLabels.firstOrNull() ?: "",
                    selectedValue = uiState.keyboardSize,
                    entries = sizeEntries.toList(),
                    labels = sizeLabels.toList(),
                    onValueSelected = { viewModel.setKeyboardSize(it) },
                    dialogTitle = stringResource(R.string.keyboard_size)
                )
            }

            // Font Size
            item {
                val fontSizeEntries = stringArrayResource(R.array.five_size_scale_values)
                val fontSizeLabels = stringArrayResource(R.array.five_size_scale_options)
                ListPreference(
                    title = stringResource(R.string.font_size),
                    summary = fontSizeLabels.getOrNull(fontSizeEntries.indexOf(uiState.fontSize))
                        ?: fontSizeLabels.firstOrNull() ?: "",
                    selectedValue = uiState.fontSize,
                    entries = fontSizeEntries.toList(),
                    labels = fontSizeLabels.toList(),
                    onValueSelected = { viewModel.setFontSize(it) },
                    dialogTitle = stringResource(R.string.font_size)
                )
            }

            // Vibrate on Keypress
            item {
                SwitchPreference(
                    title = stringResource(R.string.vibrate_on_keypress),
                    checked = uiState.vibrateOnKeypress,
                    onCheckedChange = { viewModel.setVibrateOnKeypress(it) }
                )
            }

            // Vibrate Level
            item {
                val vibrateLevelEntries = stringArrayResource(R.array.vibrate_level_values)
                val vibrateLevelLabels = stringArrayResource(R.array.vibrate_level_options)
                ListPreference(
                    title = stringResource(R.string.vibrate_level),
                    summary = vibrateLevelLabels.getOrNull(vibrateLevelEntries.indexOf(uiState.vibrateLevel))
                        ?: vibrateLevelLabels.firstOrNull() ?: "",
                    selectedValue = uiState.vibrateLevel,
                    entries = vibrateLevelEntries.toList(),
                    labels = vibrateLevelLabels.toList(),
                    onValueSelected = { viewModel.setVibrateLevel(it) },
                    dialogTitle = stringResource(R.string.vibrate_level)
                )
            }

            // Sound on Keypress
            item {
                SwitchPreference(
                    title = stringResource(R.string.sound_on_keypress),
                    checked = uiState.soundOnKeypress,
                    onCheckedChange = { viewModel.setSoundOnKeypress(it) }
                )
            }

            // Switch English Mode
            item {
                SwitchPreference(
                    title = stringResource(R.string.switch_english_mode_1),
                    summary = stringResource(R.string.switch_english_mode_summary_1),
                    checked = uiState.switchEnglishMode,
                    onCheckedChange = { viewModel.setSwitchEnglishMode(it) }
                )
            }

            // Switch English Mode Shift
            item {
                SwitchPreference(
                    title = stringResource(R.string.switch_english_mode_2),
                    summary = stringResource(R.string.switch_english_mode_summary_2),
                    checked = uiState.switchEnglishModeShift,
                    onCheckedChange = { viewModel.setSwitchEnglishModeShift(it) }
                )
            }

            // IM Category
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PreferenceCategory(title = stringResource(R.string.im))
            }

            // Smart Chinese Input
            item {
                SwitchPreference(
                    title = stringResource(R.string.preference_smart_chinese_input),
                    summary = stringResource(R.string.preference_smart_chinese_input_message),
                    checked = uiState.smartChineseInput,
                    onCheckedChange = { viewModel.setSmartChineseInput(it) }
                )
            }

            // Auto Chinese Symbol
            item {
                SwitchPreference(
                    title = stringResource(R.string.auto_chinese_symbol),
                    summary = stringResource(R.string.auto_chinese_symbol_summary),
                    checked = uiState.autoChineseSymbol,
                    onCheckedChange = { viewModel.setAutoChineseSymbol(it) }
                )
            }

            // Disable Physical Selkey
            item {
                SwitchPreference(
                    title = stringResource(R.string.disable_physical_selkey_option),
                    checked = uiState.disablePhysicalSelkey,
                    onCheckedChange = { viewModel.setDisablePhysicalSelkey(it) }
                )
            }

            // Auto Commit
            item {
                val autoCommitEntries = stringArrayResource(R.array.auto_commit_values)
                val autoCommitLabels = stringArrayResource(R.array.auto_commit_labels)
                ListPreference(
                    title = stringResource(R.string.auto_commit),
                    summary = autoCommitLabels.getOrNull(autoCommitEntries.indexOf(uiState.autoCommit))
                        ?: autoCommitLabels.firstOrNull() ?: "",
                    selectedValue = uiState.autoCommit,
                    entries = autoCommitEntries.toList(),
                    labels = autoCommitLabels.toList(),
                    onValueSelected = { viewModel.setAutoCommit(it) },
                    dialogTitle = stringResource(R.string.auto_commit_summary)
                )
            }

            // Selkey Option
            item {
                val selkeyEntries = stringArrayResource(R.array.selkey_options_values)
                val selkeyLabels = stringArrayResource(R.array.selkey_options)
                ListPreference(
                    title = stringResource(R.string.selkey_option_list),
                    summary = selkeyLabels.getOrNull(selkeyEntries.indexOf(uiState.selkeyOption))
                        ?: selkeyLabels.firstOrNull() ?: "",
                    selectedValue = uiState.selkeyOption,
                    entries = selkeyEntries.toList(),
                    labels = selkeyLabels.toList(),
                    onValueSelected = { viewModel.setSelkeyOption(it) },
                    dialogTitle = stringResource(R.string.selkey_option_list)
                )
            }

            // Phonetic Keyboard Type
            item {
                val phoneticEntries = stringArrayResource(R.array.phonetic_keyboard_type_values)
                val phoneticLabels = stringArrayResource(R.array.phonetic_keyboard_type)
                ListPreference(
                    title = stringResource(R.string.phonetic_keyboard),
                    summary = phoneticLabels.getOrNull(phoneticEntries.indexOf(uiState.phoneticKeyboardType))
                        ?: phoneticLabels.firstOrNull() ?: "",
                    selectedValue = uiState.phoneticKeyboardType,
                    entries = phoneticEntries.toList(),
                    labels = phoneticLabels.toList(),
                    onValueSelected = { viewModel.setPhoneticKeyboardType(it) },
                    dialogTitle = stringResource(R.string.phonetic_keyboard)
                )
            }

            // Physical Keyboard Type
            item {
                val physicalKbEntries = stringArrayResource(R.array.physical_keyboard_options_values)
                val physicalKbLabels = stringArrayResource(R.array.physical_keyboard_options)
                ListPreference(
                    title = stringResource(R.string.physical_keyboard),
                    summary = physicalKbLabels.getOrNull(physicalKbEntries.indexOf(uiState.physicalKeyboardType))
                        ?: physicalKbLabels.firstOrNull() ?: "",
                    selectedValue = uiState.physicalKeyboardType,
                    entries = physicalKbEntries.toList(),
                    labels = physicalKbLabels.toList(),
                    onValueSelected = { viewModel.setPhysicalKeyboardType(it) },
                    dialogTitle = stringResource(R.string.physical_keyboard)
                )
            }

            // Reverse Lookup Notify
            item {
                SwitchPreference(
                    title = stringResource(R.string.reverse_lookup_notify),
                    checked = uiState.reverseLookupNotify,
                    onCheckedChange = { viewModel.setReverseLookupNotify(it) }
                )
            }

            // Mapping Category
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PreferenceCategory(title = stringResource(R.string.mapping))
            }

            // Similar List
            item {
                val similiarEntries = stringArrayResource(R.array.similiar_codes)
                ListPreference(
                    title = stringResource(R.string.similiar_list),
                    summary = uiState.similiarList,
                    selectedValue = uiState.similiarList,
                    entries = similiarEntries.toList(),
                    labels = similiarEntries.toList(),
                    onValueSelected = { viewModel.setSimiliarList(it) },
                    dialogTitle = stringResource(R.string.similiar_list)
                )
            }

            // Similar Enable
            item {
                SwitchPreference(
                    title = stringResource(R.string.similiar_enable_switch),
                    summary = stringResource(R.string.similiar_enable_switch_summary),
                    checked = uiState.similiarEnable,
                    onCheckedChange = { viewModel.setSimiliarEnable(it) }
                )
            }

            // English Dictionary Enable
            item {
                SwitchPreference(
                    title = stringResource(R.string.enable_english_dictionary),
                    summary = stringResource(R.string.enable_english_dictionary_summary),
                    checked = uiState.englishDictionaryEnable,
                    onCheckedChange = { viewModel.setEnglishDictionaryEnable(it) }
                )
            }

            // English Dictionary Physical Keyboard (dependent)
            item {
                SwitchPreference(
                    title = stringResource(R.string.enable_english_dictionary_physical_keyboard),
                    summary = stringResource(R.string.enable_english_dictionary_physical_keyboard_summary),
                    checked = uiState.englishDictionaryPhysicalKeyboard,
                    onCheckedChange = { viewModel.setEnglishDictionaryPhysicalKeyboard(it) },
                    enabled = uiState.englishDictionaryEnable
                )
            }

            // Candidate Switch
            item {
                SwitchPreference(
                    title = stringResource(R.string.candidate_switch),
                    summary = stringResource(R.string.candidate_switch_summary),
                    checked = uiState.candidateSwitch,
                    onCheckedChange = { viewModel.setCandidateSwitch(it) }
                )
            }

            // Candidate Suggestion
            item {
                SwitchPreference(
                    title = stringResource(R.string.candidate_suggestion),
                    summary = stringResource(R.string.candidate_suggestion_summary),
                    checked = uiState.candidateSuggestion,
                    onCheckedChange = { viewModel.setCandidateSuggestion(it) }
                )
            }

            // Learn Phrase
            item {
                SwitchPreference(
                    title = stringResource(R.string.learn_phrase),
                    summary = stringResource(R.string.learn_phrase_summary),
                    checked = uiState.learnPhrase,
                    onCheckedChange = { viewModel.setLearnPhrase(it) }
                )
            }

            // Learning Switch
            item {
                SwitchPreference(
                    title = stringResource(R.string.learning_switch),
                    summary = stringResource(R.string.learning_switch_summary),
                    checked = uiState.learningSwitch,
                    onCheckedChange = { viewModel.setLearningSwitch(it) }
                )
            }

            // Physical Keyboard Sort
            item {
                SwitchPreference(
                    title = stringResource(R.string.physical_keyboard_sort),
                    summary = stringResource(R.string.physical_keyboard_sort_summary),
                    checked = uiState.physicalKeyboardSort,
                    onCheckedChange = { viewModel.setPhysicalKeyboardSort(it) }
                )
            }

            // Accept Number Index
            item {
                SwitchPreference(
                    title = stringResource(R.string.accept_number_index),
                    summary = stringResource(R.string.accept_number_index_summary),
                    checked = uiState.acceptNumberIndex,
                    onCheckedChange = { viewModel.setAcceptNumberIndex(it) }
                )
            }

            // Accept Symbol Index
            item {
                SwitchPreference(
                    title = stringResource(R.string.accept_symbol_index),
                    summary = stringResource(R.string.accept_symbol_index_summary),
                    checked = uiState.acceptSymbolIndex,
                    onCheckedChange = { viewModel.setAcceptSymbolIndex(it) }
                )
            }
        }
    }
}
