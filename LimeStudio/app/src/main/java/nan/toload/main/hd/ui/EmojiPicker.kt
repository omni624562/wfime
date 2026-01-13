package nan.toload.main.hd.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import nan.toload.main.hd.R
import nan.toload.main.hd.data.Emoji
import nan.toload.main.hd.data.EmojiData

@Composable
fun EmojiPicker(
    onEmojiClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(1) } // Default to Smileys
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    // Recent Emojis State
    var recentEmojis by remember { mutableStateOf(loadRecentEmojis(context)) }

    // Categories
    val categories = listOf(
        "🕒" to 0, // Recent
        "😀" to 1, // Smileys
        "👋" to 2, // People
        "🐻" to 3, // Animals
        "🍔" to 4, // Food
        "🚗" to 5, // Travel
        "💡" to 6  // Objects
    )

    // Colors (Gboard-like Dark Theme)
    val backgroundColor = Color(0xFF1F1F1F) 
    val searchBarColor = Color(0xFF2F2F2F)
    val accentColor = Color(0xFFA5D6A7) // Light Green
    val textColor = Color(0xFFE0E0E0)
    val secondaryTextColor = Color(0xFFB0B0B0)

    // Helper to add to recent
    fun addToRecent(emoji: String) {
        val newList = (listOf(emoji) + recentEmojis.filter { it != emoji }).take(30)
        recentEmojis = newList
        saveRecentEmojis(context, newList)
    }

    // Filter Emojis
    val displayEmojis = remember(selectedCategoryIndex, searchText, recentEmojis) {
        if (searchText.isNotEmpty()) {
            val query = searchText.lowercase()
            // Search across all main categories
            val allEmojis = (1..6).flatMap { EmojiData.getListByCategory(it) }
            allEmojis.filter { emoji ->
                emoji.keywords.any { it.contains(query) }
            }
        } else {
            if (selectedCategoryIndex == 0) {
                // Wrap recent strings into Emoji objects (no variants for now in history to keep simple)
                recentEmojis.map { char -> Emoji(char, emptyList()) }
            } else {
                EmojiData.getListByCategory(selectedCategoryIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp) // Enforce height to ensure visibility
            .background(backgroundColor)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = secondaryTextColor
                )
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(searchBarColor)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchText.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Search emoji",
                            color = secondaryTextColor,
                            fontSize = 14.sp
                        )
                    }
                }
                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(onClick = onBackspaceClick) {
                 Icon(
                     imageVector = Icons.Filled.Backspace,
                     contentDescription = "Delete",
                     tint = secondaryTextColor
                 )
            }
        }

        // Categories (Hidden if searching)
        if (searchText.isEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = backgroundColor,
                contentColor = accentColor,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedCategoryIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                            color = accentColor,
                            height = 3.dp
                        )
                    }
                },
                divider = {}
            ) {
                categories.forEachIndexed { index, (icon, categoryId) ->
                    val selected = selectedCategoryIndex == index
                    Tab(
                        selected = selected,
                        onClick = { selectedCategoryIndex = index },
                        selectedContentColor = accentColor,
                        unselectedContentColor = secondaryTextColor
                    ) {
                        Box(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = icon,
                                fontSize = 20.sp,
                                color = if (selected) accentColor else secondaryTextColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            state = remember(selectedCategoryIndex, searchText) { 
                 androidx.compose.foundation.lazy.grid.LazyGridState() 
            }
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
        offset = offsetPx, // Show above the finger
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

// Persistance Helpers
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
