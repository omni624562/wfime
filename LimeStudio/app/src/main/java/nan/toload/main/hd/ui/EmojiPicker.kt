package nan.toload.main.hd.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import nan.toload.main.hd.data.Emoji
import nan.toload.main.hd.data.EmojiData

@Composable
fun EmojiPicker(
    onEmojiClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // Default to Smileys
    val context = LocalContext.current

    // Recent Emojis State
    var recentEmojis by remember { mutableStateOf(loadRecentEmojis(context)) }

    // Categories (icons only - simple design)
    val categories = listOf(
        "😀", // Smileys
        "👋", // People
        "🐻", // Animals
        "🍔", // Food
        "🚗", // Travel
        "💡", // Objects
        "🏆", // Activities
        "🔣", // Symbols
        "🏳️"  // Flags
    )

    // Dark Theme Colors
    val backgroundColor = Color(0xFF2B2B2B)
    val accentColor = Color(0xFF4CAF50) // Green underline
    val secondaryTextColor = Color(0xFF9E9E9E)
    val bottomBarColor = Color(0xFF1F1F1F)

    // Helper to add to recent
    fun addToRecent(emoji: String) {
        val newList = (listOf(emoji) + recentEmojis.filter { it != emoji }).take(30)
        recentEmojis = newList
        saveRecentEmojis(context, newList)
    }

    // Get display emojis based on category
    val displayEmojis = remember(selectedCategoryIndex, recentEmojis) {
        val categoryId = selectedCategoryIndex + 1 // Categories are 1-indexed in EmojiData
        EmojiData.getListByCategory(categoryId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(backgroundColor)
            .padding(bottom = 10.dp)  // Space between content and navigation bar
    ) {
        // Top Bar: Category Icons with green underline indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEachIndexed { index, icon ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedCategoryIndex = index }
                        .padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = icon,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    // Green underline for selected category
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .background(
                                if (selectedCategoryIndex == index) accentColor
                                else Color.Transparent,
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )
                }
            }
        }

        // Emoji Grid (scrollable) - 6 columns like reference
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(displayEmojis) { emoji ->
                EmojiGridItem(
                    emoji = emoji,
                    onEmojiClick = { char ->
                        onEmojiClick(char)
                        addToRecent(char)
                    }
                )
            }
        }

        // Bottom Bar: ABC (left) | spacer | ⌫ (right)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            color = bottomBarColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ABC - Back to keyboard
                TextButton(
                    onClick = onBackClick,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = "ABC",
                        color = secondaryTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Backspace ⌫
                IconButton(
                    onClick = onBackspaceClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(
                        text = "⌫",
                        fontSize = 24.sp,
                        color = secondaryTextColor
                    )
                }
            }
        }

        // Black separator bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.Black)
        )

        // Space for system IME navigation bar (⬇️ 🌐)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1A1A1A))  // Slightly darker than bottom bar
        )
    }
}

@Composable
fun EmojiGridItem(
    emoji: Emoji,
    onEmojiClick: (String) -> Unit
) {
    var showSkinTonePopup by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onEmojiClick(emoji.char) },
                    onLongPress = {
                        if (emoji.hasSkinTone) {
                            showSkinTonePopup = true
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji.char, fontSize = 28.sp)

        if (showSkinTonePopup) {
            SkinTonePopup(
                baseEmoji = emoji.char,
                onDismiss = { showSkinTonePopup = false },
                onSkinToneSelected = { variant ->
                    onEmojiClick(variant)
                    showSkinTonePopup = false
                }
            )
        }
    }
}

@Composable
fun SkinTonePopup(
    baseEmoji: String,
    onDismiss: () -> Unit,
    onSkinToneSelected: (String) -> Unit
) {
    val variants = remember(baseEmoji) {
        val list = mutableListOf(baseEmoji)
        list.addAll(EmojiData.SKIN_TONES.map { tone -> baseEmoji + tone })
        list
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetPx = with(density) { androidx.compose.ui.unit.IntOffset(0, (-60).dp.roundToPx()) }

    Popup(
        alignment = Alignment.TopCenter,
        offset = offsetPx,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFF2F2F2F), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            variants.forEach { variant ->
                Text(
                    text = variant,
                    fontSize = 30.sp,
                    modifier = Modifier.clickable { onSkinToneSelected(variant) }
                )
            }
        }
    }
}

// Persistence Helpers
private const val PREF_NAME = "emoji_prefs"
private const val KEY_RECENT = "recent_emojis"

fun loadRecentEmojis(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_RECENT, "") ?: ""
    return if (saved.isEmpty()) emptyList() else saved.split(",")
}

fun saveRecentEmojis(context: Context, emojis: List<String>) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_RECENT, emojis.joinToString(",")).apply()
}
