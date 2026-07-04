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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 * Overhauled with a gorgeous, high-end dashboard user interface.
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
        // 平板雙欄旗艦模式 (Premium Master-Detail Dashboard)
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
                // 左側導覽列 Master Panel
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(
                            width = (0.5).dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                ) {
                    // Premium Brand Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "麥田輸入法",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "設定主控台 v${net.toload.main.hd.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // 導覽選項 (Navigation Options)
                    val menuItems = listOf(
                        Triple(0, "載入輸入法對照表", Icons.Default.CloudDownload),
                        Triple(1, "虛擬鍵盤與回饋", Icons.Default.Keyboard),
                        Triple(2, "輸入法引擎設定", Icons.Default.Tune),
                        Triple(3, "字根與對照表管理", Icons.Default.Storage)
                    )

                    menuItems.forEach { (index, title, icon) ->
                        val isSelected = selectedIndex == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else
                                        Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 3.dp, height = 18.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activity?.showHelpDialog() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "使用說明與教學",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activity?.performReset() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "重置設定值",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 右側詳細內容 Detail Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    Crossfade(targetState = selectedIndex, label = "settings_crossfade") { page ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
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
        // 手機極簡美觀單欄模式
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
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
                    PreferenceCategory(title = stringResource(R.string.im))
                    ImSettingsSection(uiState, viewModel)
                }

                // Mapping Category
                item {
                    PreferenceCategory(title = stringResource(R.string.mapping))
                    MappingSettingsSection(uiState, viewModel)
                }
            }
        }
    }
}

/**
 * Download & Import Card Overhauled with high-tech status indicators
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.setup_im_download),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.setup_im_download_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 注音載入按鈕
                Button(
                    onClick = { activity?.downloadPhonetic() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isPhoneticImported)
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    border = if (uiState.isPhoneticImported) BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    ) else null
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (uiState.isPhoneticImported) Color(0xFF00E676) else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isPhoneticImported) "注音 (已載入)" else "載入注音對照表",
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isPhoneticImported)
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // 大易載入按鈕
                Button(
                    onClick = { activity?.downloadDayi() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isDayiImported)
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    border = if (uiState.isDayiImported) BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    ) else null
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (uiState.isDayiImported) Color(0xFF00E676) else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isDayiImported) "大易 (已載入)" else "載入大易對照表",
                            fontWeight = FontWeight.Bold,
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
}

/**
 * Keyboard preferences section grouped in gorgeous sub-cards
 */
@Composable
fun KeyboardSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Group 1: 智慧功能與表情
        PreferenceCardGroup(title = "智慧輸入與表情符號", icon = Icons.Default.AutoAwesome) {
            SwitchPreference(
                title = stringResource(R.string.enable_emoji),
                summary = stringResource(R.string.enable_emoji_summary),
                checked = uiState.enableEmoji,
                onCheckedChange = { viewModel.setEnableEmoji(it) }
            )

            if (uiState.enableEmoji) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.persistent_language_mode),
                summary = stringResource(R.string.persistent_language_mode_summary),
                checked = uiState.persistentLanguageMode,
                onCheckedChange = { viewModel.setPersistentLanguageMode(it) }
            )
        }

        // Group 2: 鍵盤配置與尺寸
        PreferenceCardGroup(title = "鍵盤配置與尺寸調整", icon = Icons.Default.Keyboard) {
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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.number_row_in_english),
                summary = stringResource(R.string.number_row_in_english_summary),
                checked = uiState.numberRowInEnglish,
                onCheckedChange = { viewModel.setNumberRowInEnglish(it) }
            )
        }

        // Group 3: 鍵盤互動與回饋
        PreferenceCardGroup(title = "按鍵回饋與實體鍵盤互動", icon = Icons.Default.VolumeUp) {
            SwitchPreference(
                title = stringResource(R.string.vibrate_on_keypress),
                checked = uiState.vibrateOnKeypress,
                onCheckedChange = { viewModel.setVibrateOnKeypress(it) }
            )

            if (uiState.vibrateOnKeypress) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
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
                    dialogTitle = stringResource(R.string.vibrate_level),
                    enabled = uiState.vibrateOnKeypress
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.sound_on_keypress),
                checked = uiState.soundOnKeypress,
                onCheckedChange = { viewModel.setSoundOnKeypress(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.hide_software_keyboard_typing_with_physical),
                summary = stringResource(R.string.hide_software_keyboard_typing_with_physical_summary),
                checked = uiState.hideSoftwareKeyboardWithPhysical,
                onCheckedChange = { viewModel.setHideSoftwareKeyboardWithPhysical(it) }
            )
        }

        // Group 4: 英數快速切換與功能鍵
        PreferenceCardGroup(title = "快捷鍵與英數輸入行為", icon = Icons.Default.Extension) {
            SwitchPreference(
                title = stringResource(R.string.switch_english_mode_1),
                summary = stringResource(R.string.switch_english_mode_summary_1),
                checked = uiState.switchEnglishMode,
                onCheckedChange = { viewModel.setSwitchEnglishMode(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.switch_english_mode_2),
                summary = stringResource(R.string.switch_english_mode_summary_2),
                checked = uiState.switchEnglishModeShift,
                onCheckedChange = { viewModel.setSwitchEnglishModeShift(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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

        // 快捷鍵速查:把散落在程式裡的隱藏快捷鍵公開給使用者
        PreferenceCardGroup(title = "快捷鍵速查 | Shortcuts", icon = Icons.Default.Info) {
            val shortcuts = listOf(
                "🌐 地球鍵" to "循環切換 英文 → 大易 → 注音;長按開啟系統輸入法選單",
                "雙擊 Shift" to "鎖定大寫(再按一次解除)",
                "大易快速選字(實體鍵盤)" to "Space 選第 1 個候選;' [ ] - \\ 選第 2~6 個;Ctrl+1~9 選第 1~9 個",
                "大易全形標點(實體鍵盤)" to "Shift+, . / 1 ; 輸出 ，。?!:;「=」前綴+標點輸出 、;:",
                "中英切換(實體鍵盤)" to "Shift+Space 或 Ctrl+Space",
                "候選列(實體鍵盤)" to "方向鍵移動;PgUp/PgDn 或 ↑↓ 翻頁;Enter 選取"
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                shortcuts.forEachIndexed { i, (key, desc) ->
                    if (i > 0)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * IM preferences section grouped beautifully with glowing AI features
 */
@Composable
fun ImSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Group 1: 輸入法引擎開關
        PreferenceCardGroup(title = "輸入法啟用管理", icon = Icons.Default.Tune) {
            SwitchPreference(
                title = stringResource(R.string.enable_dayi),
                summary = if (uiState.isDayiImported) {
                    if (uiState.enableDayi) stringResource(R.string.enable_dayi_summary_on) else stringResource(R.string.enable_dayi_summary_off)
                } else {
                    stringResource(R.string.enable_dayi_summary_disabled)
                },
                checked = uiState.enableDayi && uiState.isDayiImported,
                onCheckedChange = { viewModel.setEnableDayi(it) },
                enabled = uiState.isDayiImported
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.enable_phonetic),
                summary = if (uiState.isPhoneticImported) {
                    if (uiState.enablePhonetic) stringResource(R.string.enable_phonetic_summary_on) else stringResource(R.string.enable_phonetic_summary_off)
                } else {
                    stringResource(R.string.enable_phonetic_summary_disabled)
                },
                checked = uiState.enablePhonetic && uiState.isPhoneticImported,
                onCheckedChange = { viewModel.setEnablePhonetic(it) },
                enabled = uiState.isPhoneticImported
            )
        }

        // Group 2: 大易智慧選字設定 (Premium AI Engine Panel)
        if (uiState.isDayiImported) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .border(
                        width = 1.dp,
                        color = if (uiState.dayiSmartSelection)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.dayiSmartSelection)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.06f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (uiState.dayiSmartSelection) Color(0xFF00E676) else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "大易智慧選字核心 (AI 學習模型)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (uiState.dayiSmartSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = if (uiState.dayiSmartSelection)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )

                    SwitchPreference(
                        title = "啟用智慧選字",
                        summary = "依前一個字預測,最多把一個候選提到第 1 位;其餘候選維持固定順序(肌肉記憶安全)",
                        checked = uiState.dayiSmartSelection,
                        onCheckedChange = { viewModel.setDayiSmartSelection(it) },
                        color = Color.Transparent
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    SwitchPreference(
                        title = "連打模式（自動上字）",
                        summary = "打到無法接續的字根時自動送出首選字。注意：開啟後無法直接輸入英文單字",
                        checked = uiState.dayiAutoCompose,
                        onCheckedChange = { viewModel.setDayiAutoCompose(it) },
                        color = Color.Transparent
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    Spacer(modifier = Modifier.height(4.dp))
                    var showClearDialog by remember { mutableStateOf(false) }

                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = "清除智慧選字資料",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        supportingContent = { Text("清除您在大易輸入法下的所有個人習慣統計資料") },
                        modifier = Modifier.clickable { showClearDialog = true }
                    )

                    if (showClearDialog) {
                        AlertDialog(
                            onDismissRequest = { showClearDialog = false },
                            title = { Text("確認清除智慧學習資料", fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(24.dp),
                            text = { Text("您確定要清除大易輸入法的所有智慧選字學習習慣嗎？此動作無法復原。") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.clearDayiSmartSelectionData()
                                        showClearDialog = false
                                    }
                                ) {
                                    Text("確定清除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearDialog = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Group 3: 輸入行為與輔助
        PreferenceCardGroup(title = "輸入行為與輔助設定", icon = Icons.Default.Build) {
            SwitchPreference(
                title = stringResource(R.string.auto_chinese_symbol),
                summary = stringResource(R.string.auto_chinese_symbol_summary),
                checked = uiState.autoChineseSymbol,
                onCheckedChange = { viewModel.setAutoChineseSymbol(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.disable_physical_selkey_option),
                checked = uiState.disablePhysicalSelkey,
                onCheckedChange = { viewModel.setDisablePhysicalSelkey(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.reverse_lookup_notify),
                checked = uiState.reverseLookupNotify,
                onCheckedChange = { viewModel.setReverseLookupNotify(it) }
            )
        }
    }
}

/**
 * Mapping preferences section grouped elegantly
 */
@Composable
fun MappingSettingsSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Group 1: 聯想字與同音字輔助
        PreferenceCardGroup(title = "聯想與同音字輔助對照", icon = Icons.Default.Translate) {
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

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.similiar_enable_switch),
                summary = stringResource(R.string.similiar_enable_switch_summary),
                checked = uiState.similiarEnable,
                onCheckedChange = { viewModel.setSimiliarEnable(it) }
            )
        }

        // Group 2: 英文關聯字典
        PreferenceCardGroup(title = "英文輸入聯想助手", icon = Icons.Default.Info) {
            SwitchPreference(
                title = stringResource(R.string.enable_english_dictionary),
                summary = stringResource(R.string.enable_english_dictionary_summary),
                checked = uiState.englishDictionaryEnable,
                onCheckedChange = { viewModel.setEnglishDictionaryEnable(it) }
            )

            if (uiState.englishDictionaryEnable) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                SwitchPreference(
                    title = stringResource(R.string.enable_english_dictionary_physical_keyboard),
                    summary = stringResource(R.string.enable_english_dictionary_physical_keyboard_summary),
                    checked = uiState.englishDictionaryPhysicalKeyboard,
                    onCheckedChange = { viewModel.setEnglishDictionaryPhysicalKeyboard(it) },
                    enabled = uiState.englishDictionaryEnable
                )
            }
        }

        // Group 3: 智慧學習與候選字過濾
        PreferenceCardGroup(title = "候選字行為與智慧學習", icon = Icons.Default.Storage) {
            SwitchPreference(
                title = stringResource(R.string.candidate_switch),
                summary = stringResource(R.string.candidate_switch_summary),
                checked = uiState.candidateSwitch,
                onCheckedChange = { viewModel.setCandidateSwitch(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.candidate_suggestion),
                summary = stringResource(R.string.candidate_suggestion_summary),
                checked = uiState.candidateSuggestion,
                onCheckedChange = { viewModel.setCandidateSuggestion(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            var showRelatedManager by remember { mutableStateOf(false) }
            ClickablePreference(
                title = stringResource(R.string.related_manager_entry),
                summary = stringResource(R.string.related_manager_entry_summary),
                onClick = { showRelatedManager = true }
            )
            if (showRelatedManager) {
                RelatedWordManagerDialog(
                    viewModel = viewModel,
                    onDismiss = { showRelatedManager = false }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.learn_phrase),
                summary = stringResource(R.string.learn_phrase_summary),
                checked = uiState.learnPhrase,
                onCheckedChange = { viewModel.setLearnPhrase(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.learning_switch),
                summary = stringResource(R.string.learning_switch_summary),
                checked = uiState.learningSwitch,
                onCheckedChange = { viewModel.setLearningSwitch(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.physical_keyboard_sort),
                summary = stringResource(R.string.physical_keyboard_sort_summary),
                checked = uiState.physicalKeyboardSort,
                onCheckedChange = { viewModel.setPhysicalKeyboardSort(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.accept_number_index),
                summary = stringResource(R.string.accept_number_index_summary),
                checked = uiState.acceptNumberIndex,
                onCheckedChange = { viewModel.setAcceptNumberIndex(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            SwitchPreference(
                title = stringResource(R.string.accept_symbol_index),
                summary = stringResource(R.string.accept_symbol_index_summary),
                checked = uiState.acceptSymbolIndex,
                onCheckedChange = { viewModel.setAcceptSymbolIndex(it) }
            )
        }
    }
}
