package nan.toload.main.hd.ui

import android.content.Context
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPicker(
    onEmojiClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    bottomPaddingDp: Int = 0
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // Default to Smileys
    val context = LocalContext.current
    EmojiData.initialize(context) // Load emojis from JSON

    // Recent Emojis State
    var recentEmojis by remember { mutableStateOf(loadRecentEmojis(context)) }

    // Categories (using Material Icons to match Gboard style)
    val categories: List<ImageVector> = listOf(
        Icons.Filled.Mood,                // Smileys
        Icons.Filled.EmojiPeople,         // People
        Icons.Filled.EmojiNature,         // Animals & Nature (Bee + Flower)
        Icons.Filled.EmojiFoodBeverage,   // Food (Cup)
        Icons.Filled.EmojiTransportation, // Travel (Building + Car)
        Icons.Filled.EmojiEvents,         // Activities (Trophy)
        Icons.Filled.EmojiObjects,        // Objects (Lightbulb)
        Icons.Filled.EmojiSymbols,        // Symbols (Music + Percent)
        Icons.Filled.EmojiFlags           // Flags
    )

    // Dark Theme Colors
    val backgroundColor = Color(0xFF2B2B2B)
    val accentColor = Color(0xFF4CAF50) // Green underline
    val iconColor = Color(0xFFE2E2E2) // Light gray/white for icons
    val secondaryTextColor = Color(0xFF9E9E9E)
    val bottomBarColor = Color(0xFF1F1F1F)

    // Helper to add to recent
    fun addToRecent(emoji: String) {
        val newList = (listOf(emoji) + recentEmojis.filter { it != emoji }).take(30)
        recentEmojis = newList
        saveRecentEmojis(context, newList)
    }

    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    // Sync selectedCategoryIndex with pager state for tab highlighting
    LaunchedEffect(pagerState.currentPage) {
        selectedCategoryIndex = pagerState.currentPage
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(backgroundColor)
            // Apply bottom padding to the whole column so the bottom bar sits ABOVE the system nav bar
            .padding(bottom = bottomPaddingDp.dp) 
    ) {
        // Top Bar: Category Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) 
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEachIndexed { index, icon: ImageVector ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { 
                            selectedCategoryIndex = index
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                        .padding(horizontal = 2.dp)
                        .weight(1f) // Distribute space evenly
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selectedCategoryIndex == index) iconColor else secondaryTextColor,
                        modifier = Modifier
                            .size(24.dp) 
                            .padding(bottom = 4.dp) 
                    )
                    // Green underline for selected category
                    Box(
                        modifier = Modifier
                            .width(24.dp) 
                            .height(2.dp)
                            .background(
                                if (selectedCategoryIndex == index) accentColor
                                else Color.Transparent,
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }

        // Emoji Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            // Get display emojis for this specific page
            val emojisForPage = remember(page, recentEmojis) {
                EmojiData.getListByCategory(page + 1) // Categories are 1-indexed
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(emojisForPage) { emoji ->
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

        // Custom Bottom Bar: ABC (left) | ⌫ (right)
        // This will sit at the bottom of the Column (content area), 
        // but ABOVE the system padding we added to the Column.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
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
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "ABC",
                        color = secondaryTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Backspace ⌫
                IconButton(
                    onClick = onBackspaceClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "⌫",
                        fontSize = 20.sp,
                        color = secondaryTextColor
                    )
                }
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
            .clip(RoundedCornerShape(8.dp)) // Changed from CircleShape to visible corners for indicator
            .pointerInput(emoji) {
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
        Text(text = emoji.char, fontSize = 30.sp)

        // Visual indicator for emojis with skin tone support (small triangle in bottom-right)
        if (emoji.hasSkinTone) {
            Canvas(modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(8.dp) // Increased size slightly for visibility
            ) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width, size.height) // Bottom right
                    lineTo(size.width, 0f)          // Top right
                    lineTo(0f, size.height)         // Bottom left
                    close()
                }
                // Use LightGray with higher alpha for better visibility on dark background
                drawPath(path, color = Color.LightGray.copy(alpha = 0.8f))
            }
        }

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
        list.addAll(EmojiData.SKIN_TONES.map { tone -> EmojiData.applySkinTone(baseEmoji, tone) })
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
