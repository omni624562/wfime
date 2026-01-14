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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // Default to Recent
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Recent Emojis State
    var recentEmojis by remember { mutableStateOf(loadRecentEmojis(context)) }

    // Categories (icons only for top bar)
    val categories = listOf(
        "🕒" to "最近使用的表情符號",
        "😀" to "表情符號",
        "👋" to "人物",
        "🐻" to "動物與自然",
        "🍔" to "食物與飲料",
        "🚗" to "旅遊與地點",
        "💡" to "物品",
        "🏳️" to "旗幟"
    )

    // Gboard-like Dark Theme Colors
    val backgroundColor = Color(0xFF2D2F31)
    val searchBarColor = Color(0xFF3C4043)
    val accentColor = Color(0xFF8AB4F8)
    val textColor = Color(0xFFE8EAED)
    val secondaryTextColor = Color(0xFF9AA0A6)
    val bottomBarColor = Color(0xFF2D2F31)
    val selectedCategoryBg = Color(0xFF3C4043)

    // Helper to add to recent
    fun addToRecent(emoji: String) {
        val newList = (listOf(emoji) + recentEmojis.filter { it != emoji }).take(30)
        recentEmojis = newList
        saveRecentEmojis(context, newList)
    }

    // Get display emojis based on search and category
    val displayEmojis = remember(selectedCategoryIndex, searchText, recentEmojis) {
        if (searchText.isNotEmpty()) {
            val query = searchText.lowercase()
            val allEmojis = (1..7).flatMap { EmojiData.getListByCategory(it) }
            allEmojis.filter { emoji ->
                emoji.keywords.any { it.contains(query) }
            }
        } else {
            if (selectedCategoryIndex == 0 && recentEmojis.isNotEmpty()) {
                recentEmojis.map { Emoji(it, emptyList()) }
            } else {
                val categoryId = if (selectedCategoryIndex == 0) 1 else selectedCategoryIndex
                EmojiData.getListByCategory(categoryId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()  // Fill available height like Gboard
            .background(backgroundColor)
            .navigationBarsPadding()  // Handle system navigation bar insets properly
    ) {
        // Top Bar: Back + Search + Category Icons (Gboard style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Search bar removed as per user request

            // Category Icons (horizontal scroll-like row)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEachIndexed { index, (icon, _) ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { selectedCategoryIndex = index }
                            .background(
                                if (selectedCategoryIndex == index) selectedCategoryBg
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // Category Title
        if (searchText.isEmpty()) {
            Text(
                text = categories[selectedCategoryIndex].second,
                color = secondaryTextColor,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
        }

        // Emoji Grid (scrollable)
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
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
        // Bottom bar removed as per user request
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
