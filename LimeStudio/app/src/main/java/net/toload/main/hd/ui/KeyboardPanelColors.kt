package net.toload.main.hd.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 表情符號面板（EmojiPicker）與備忘錄面板（MemoPanel）的配色：
// 深色沿用原本的深灰；淺色用 Material3 淺色色票（colors_material3.xml），與候選列（CandidateView）一致
internal data class KeyboardPanelColors(
    val background: Color,       // 面板底色
    val bottomBar: Color,        // 底部 ABC／退格列、搜尋用小鍵盤底、外框（導覽列區）底色
    val accent: Color,           // 選中分類底線、備忘錄的貼上／新增／置頂、輸入框框線與游標、儲存鈕底
    val onAccent: Color,         // 儲存鈕文字
    val icon: Color,             // 選中的分類圖示、備忘錄返回鈕
    val secondary: Color,        // 未選中分類、ABC、退格、提示文字
    val text: Color,             // 一般文字（搜尋字、小鍵盤按鍵、備忘錄內容與標題）
    val divider: Color,          // 分隔線
    val skinToneMark: Color,     // 可選膚色的表情符號右下角三角
    val keyBackground: Color,    // 搜尋用小鍵盤的按鍵
    val popupBackground: Color,  // 膚色選擇浮窗
    val card: Color,             // 備忘錄卡片
    val pinnedCard: Color,       // 置頂的備忘錄卡片
    val dialogCard: Color,       // 新增備忘錄對話框
    val delete: Color            // 刪除鈕
)

internal val DarkKeyboardPanelColors = KeyboardPanelColors(
    background = Color(0xFF2B2B2B),
    bottomBar = Color(0xFF1F1F1F),
    accent = Color(0xFF4CAF50),
    onAccent = Color.Black,
    icon = Color(0xFFE2E2E2),
    secondary = Color(0xFF9E9E9E),
    text = Color.White,
    divider = Color.White.copy(alpha = 0.1f),
    skinToneMark = Color.LightGray.copy(alpha = 0.8f),
    keyBackground = Color(0xFF3A3A3A),
    popupBackground = Color(0xFF2F2F2F),
    card = Color(0xFF383838),
    pinnedCard = Color(0xFF203A2B),
    dialogCard = Color(0xFF2E2E2E),
    delete = Color(0xFFE57373)
)

internal val LightKeyboardPanelColors = KeyboardPanelColors(
    background = Color(0xFFF0F1EC),              // md_theme_inverseOnSurface（與候選列相同）
    bottomBar = Color(0xFFFBFDF8),               // md_theme_surface（與鍵盤底色相同）
    accent = Color(0xFF006C4C),                  // md_theme_primary
    onAccent = Color(0xFFFFFFFF),                // md_theme_onPrimary
    icon = Color(0xFF191C1A),                    // md_theme_onSurface
    secondary = Color(0xFF707972),               // md_theme_outline
    text = Color(0xFF191C1A),                    // md_theme_onSurface
    divider = Color.Black.copy(alpha = 0.1f),
    skinToneMark = Color(0xFF707972).copy(alpha = 0.8f), // md_theme_outline @ 80%
    keyBackground = Color(0xFFDBE5DD),           // md_theme_surfaceVariant（與鍵盤按鍵相同）
    popupBackground = Color(0xFFDBE5DD),         // md_theme_surfaceVariant
    card = Color(0xFFFBFDF8),                    // md_theme_surface
    pinnedCard = Color(0xFFCFE9D9),              // md_theme_secondaryContainer
    dialogCard = Color(0xFFFBFDF8),              // md_theme_surface
    delete = Color(0xFFBA1A1A)                   // md_theme_error
)

@Composable
internal fun keyboardPanelColors(): KeyboardPanelColors =
    if (isSystemInDarkTheme()) DarkKeyboardPanelColors else LightKeyboardPanelColors
