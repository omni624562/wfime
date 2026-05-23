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
 *  */

package net.toload.main.hd.ui.compose.settings

import android.content.ContextWrapper
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.toload.main.hd.MainActivity
import net.toload.main.hd.R

@Composable
fun findMainActivity(): MainActivity? {
    var context = LocalContext.current
    while (context is ContextWrapper) {
        if (context is MainActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

/**
 * Main settings screen with responsive layout support.
 *
 * Automatically scales to a gorgeous Master-Detail dual-pane interface on tablets (width >= 600dp)
 * with animated transitions, while falling back gracefully to a classic list scroll on mobile phones.
 *
 * @param viewModel SettingsViewModel managing preference state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = findMainActivity()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        // 平板雙欄模式 (Master-Detail Pane Layout)
        var selectedIndex by rememberSaveable { mutableStateOf(0) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 左側導覽列 Master
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = "設定主控台 v1.3.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 導覽選項
                    val menuItems = listOf(
                        Triple(0, "載入輸入法", Icons.Default.List),
                        Triple(1, stringResource(R.string.keyboard), Icons.Default.Keyboard),
                        Triple(2, stringResource(R.string.im), Icons.Default.Settings),
                        Triple(3, stringResource(R.string.mapping), Icons.Default.List)
                    )

                    menuItems.forEach { (index, title, icon) ->
                        val isSelected = selectedIndex == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    else
                                        Color.Transparent
                                )
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 右側詳細內容 Detail
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Crossfade(targetState = selectedIndex, label = "settings_crossfade") { page ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            when (page) {
                                0 -> {
                                    item { PreferenceCategory(title = "載入對照表") }
                                    item { ImLoadingCard(activity, uiState, modifier = Modifier.padding(horizontal = 0.dp)) }
                                }
                                1 -> {
                                    item { PreferenceCategory(title = stringResource(R.string.keyboard)) }
                                    item { KeyboardSettingsSection(uiState, viewModel) }
                                }
                                2 -> {
                                    item { PreferenceCategory(title = stringResource(R.string.im)) }
                                    item { ImSettingsSection(uiState, viewModel) }
                                }
                                3 -> {
                                    item { PreferenceCategory(title = stringResource(R.string.mapping)) }
                                    item { MappingSettingsSection(uiState, viewModel) }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // 手機單欄滾動模式
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 載入輸入法
                item {
                    ImLoadingCard(activity, uiState)
                }

                // Keyboard Category
                item {
                    PreferenceCategory(title = stringResource(R.string.keyboard))
                    KeyboardSettingsSection(uiState, viewModel)
                }

                // IM Category
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PreferenceCategory(title = stringResource(R.string.im))
                    ImSettingsSection(uiState, viewModel)
                }

                // Mapping Category
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PreferenceCategory(title = stringResource(R.string.mapping))
                    MappingSettingsSection(uiState, viewModel)
                }
            }
        }
    }
}

/**
 * Download & Import Card
 */
@Composable
fun ImLoadingCard(
    activity: MainActivity?,
    uiState: SettingsUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.setup_im_download),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.setup_im_download_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { activity?.downloadPhonetic() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isPhoneticImported)
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (uiState.isPhoneticImported) "注音 (已載入)" else "載入注音",
                        fontWeight = if (uiState.isPhoneticImported) FontWeight.Normal else FontWeight.Bold,
                        color = if (uiState.isPhoneticImported)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                }
                Button(
                    onClick = { activity?.downloadDayi() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isDayiImported)
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (uiState.isDayiImported) "大易 (已載入)" else "載入大易",
                        fontWeight = if (uiState.isDayiImported) FontWeight.Normal else FontWeight.Bold,
                        color = if (uiState.isDayiImported)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Keyboard preferences section
 */
@Composable
fun KeyboardSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Enable Emoji
        SwitchPreference(
            title = stringResource(R.string.enable_emoji),
            summary = stringResource(R.string.enable_emoji_summary),
            checked = uiState.enableEmoji,
            onCheckedChange = { viewModel.setEnableEmoji(it) }
        )

        // Emoji Position (dependent on enable_emoji)
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

        // Persistent Language Mode
        SwitchPreference(
            title = stringResource(R.string.persistent_language_mode),
            summary = stringResource(R.string.persistent_language_mode_summary),
            checked = uiState.persistentLanguageMode,
            onCheckedChange = { viewModel.setPersistentLanguageMode(it) }
        )

        // Number Row in English
        SwitchPreference(
            title = stringResource(R.string.number_row_in_english),
            summary = stringResource(R.string.number_row_in_english_summary),
            checked = uiState.numberRowInEnglish,
            onCheckedChange = { viewModel.setNumberRowInEnglish(it) }
        )

        // Hide Software Keyboard with Physical
        SwitchPreference(
            title = stringResource(R.string.hide_software_keyboard_typing_with_physical),
            summary = stringResource(R.string.hide_software_keyboard_typing_with_physical_summary),
            checked = uiState.hideSoftwareKeyboardWithPhysical,
            onCheckedChange = { viewModel.setHideSoftwareKeyboardWithPhysical(it) }
        )

        // Show Arrow Keys
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

        // Split Keyboard Mode
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

        // Keyboard Size
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

        // Font Size
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

        // Vibrate on Keypress
        SwitchPreference(
            title = stringResource(R.string.vibrate_on_keypress),
            checked = uiState.vibrateOnKeypress,
            onCheckedChange = { viewModel.setVibrateOnKeypress(it) }
        )

        // Vibrate Level
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

        // Sound on Keypress
        SwitchPreference(
            title = stringResource(R.string.sound_on_keypress),
            checked = uiState.soundOnKeypress,
            onCheckedChange = { viewModel.setSoundOnKeypress(it) }
        )

        // Switch English Mode
        SwitchPreference(
            title = stringResource(R.string.switch_english_mode_1),
            summary = stringResource(R.string.switch_english_mode_summary_1),
            checked = uiState.switchEnglishMode,
            onCheckedChange = { viewModel.setSwitchEnglishMode(it) }
        )

        // Switch English Mode Shift
        SwitchPreference(
            title = stringResource(R.string.switch_english_mode_2),
            summary = stringResource(R.string.switch_english_mode_summary_2),
            checked = uiState.switchEnglishModeShift,
            onCheckedChange = { viewModel.setSwitchEnglishModeShift(it) }
        )
    }
}

/**
 * IM preferences section
 */
@Composable
fun ImSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Auto Chinese Symbol
        SwitchPreference(
            title = stringResource(R.string.auto_chinese_symbol),
            summary = stringResource(R.string.auto_chinese_symbol_summary),
            checked = uiState.autoChineseSymbol,
            onCheckedChange = { viewModel.setAutoChineseSymbol(it) }
        )

        // Disable Physical Selkey
        SwitchPreference(
            title = stringResource(R.string.disable_physical_selkey_option),
            checked = uiState.disablePhysicalSelkey,
            onCheckedChange = { viewModel.setDisablePhysicalSelkey(it) }
        )

        // Auto Commit
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

        // Selkey Option
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

        // Phonetic Keyboard Type
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

        // Physical Keyboard Type
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

        // Reverse Lookup Notify
        SwitchPreference(
            title = stringResource(R.string.reverse_lookup_notify),
            checked = uiState.reverseLookupNotify,
            onCheckedChange = { viewModel.setReverseLookupNotify(it) }
        )
    }
}

/**
 * Mapping preferences section
 */
@Composable
fun MappingSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Similar List
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

        // Similar Enable
        SwitchPreference(
            title = stringResource(R.string.similiar_enable_switch),
            summary = stringResource(R.string.similiar_enable_switch_summary),
            checked = uiState.similiarEnable,
            onCheckedChange = { viewModel.setSimiliarEnable(it) }
        )

        // English Dictionary Enable
        SwitchPreference(
            title = stringResource(R.string.enable_english_dictionary),
            summary = stringResource(R.string.enable_english_dictionary_summary),
            checked = uiState.englishDictionaryEnable,
            onCheckedChange = { viewModel.setEnglishDictionaryEnable(it) }
        )

        // English Dictionary Physical Keyboard (dependent)
        SwitchPreference(
            title = stringResource(R.string.enable_english_dictionary_physical_keyboard),
            summary = stringResource(R.string.enable_english_dictionary_physical_keyboard_summary),
            checked = uiState.englishDictionaryPhysicalKeyboard,
            onCheckedChange = { viewModel.setEnglishDictionaryPhysicalKeyboard(it) },
            enabled = uiState.englishDictionaryEnable
        )

        // Candidate Switch
        SwitchPreference(
            title = stringResource(R.string.candidate_switch),
            summary = stringResource(R.string.candidate_switch_summary),
            checked = uiState.candidateSwitch,
            onCheckedChange = { viewModel.setCandidateSwitch(it) }
        )

        // Candidate Suggestion
        SwitchPreference(
            title = stringResource(R.string.candidate_suggestion),
            summary = stringResource(R.string.candidate_suggestion_summary),
            checked = uiState.candidateSuggestion,
            onCheckedChange = { viewModel.setCandidateSuggestion(it) }
        )

        // Learn Phrase
        SwitchPreference(
            title = stringResource(R.string.learn_phrase),
            summary = stringResource(R.string.learn_phrase_summary),
            checked = uiState.learnPhrase,
            onCheckedChange = { viewModel.setLearnPhrase(it) }
        )

        // Learning Switch
        SwitchPreference(
            title = stringResource(R.string.learning_switch),
            summary = stringResource(R.string.learning_switch_summary),
            checked = uiState.learningSwitch,
            onCheckedChange = { viewModel.setLearningSwitch(it) }
        )

        // Physical Keyboard Sort
        SwitchPreference(
            title = stringResource(R.string.physical_keyboard_sort),
            summary = stringResource(R.string.physical_keyboard_sort_summary),
            checked = uiState.physicalKeyboardSort,
            onCheckedChange = { viewModel.setPhysicalKeyboardSort(it) }
        )

        // Accept Number Index
        SwitchPreference(
            title = stringResource(R.string.accept_number_index),
            summary = stringResource(R.string.accept_number_index_summary),
            checked = uiState.acceptNumberIndex,
            onCheckedChange = { viewModel.setAcceptNumberIndex(it) }
        )

        // Accept Symbol Index
        SwitchPreference(
            title = stringResource(R.string.accept_symbol_index),
            summary = stringResource(R.string.accept_symbol_index_summary),
            checked = uiState.acceptSymbolIndex,
            onCheckedChange = { viewModel.setAcceptSymbolIndex(it) }
        )
    }
}
