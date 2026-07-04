package net.toload.main.hd.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import net.toload.main.hd.data.Emoji
import net.toload.main.hd.data.EmojiData
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
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        EmojiData.initialize(context) // Load emojis from JSON once
    }

    // Recent Emojis State
    var recentEmojis by remember { mutableStateOf(loadRecentEmojis(context)) }
    var selectedCategoryIndex by remember { mutableIntStateOf(if (recentEmojis.isEmpty()) 1 else 0) }

    // 膚色偏好(base emoji -> 膚色修飾字元);點擊直接用偏好膚色,長按重選並記住
    var tonePrefs by remember { mutableStateOf(loadTonePrefs(context)) }

    // 搜尋模式(英文關鍵字;輸入用內建迷你鍵盤,因為選擇器本身就在輸入法視窗內)
    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Categories (using Material Icons to match Gboard style)
    val categories: List<ImageVector> = listOf(
        Icons.Filled.Schedule,            // Recent
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

    // Helper to add to recent (同時記錄使用頻率供聯想排序)。
    // 以基底字元儲存:膚色變體去重,顯示時再套用膚色偏好
    fun addToRecent(emoji: String) {
        val base = EmojiData.baseOf(emoji)
        val newList = (listOf(base) + recentEmojis.filter { it != base }).take(30)
        recentEmojis = newList
        saveRecentEmojis(context, newList)
        net.toload.main.hd.data.EmojiUsageTracker.record(context, base)
    }

    val pagerState = rememberPagerState(initialPage = selectedCategoryIndex, pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    // Sync selectedCategoryIndex with pager state for tab highlighting
    LaunchedEffect(pagerState.currentPage) {
        selectedCategoryIndex = pagerState.currentPage
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            // Apply bottom padding to the whole column so the bottom bar sits ABOVE the system nav bar
            .padding(bottom = bottomPaddingDp.dp) 
    ) {
        // Top Bar: Category Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp) 
                .background(backgroundColor) // Solid background
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 搜尋(英文關鍵字)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        searchMode = true
                        searchQuery = ""
                    }
                    .padding(horizontal = 2.dp)
                    .weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜尋",
                    tint = if (searchMode) iconColor else secondaryTextColor,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(bottom = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(
                            if (searchMode) accentColor else Color.Transparent,
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
            categories.forEachIndexed { index, icon: ImageVector ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            searchMode = false
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
                        tint = if (!searchMode && selectedCategoryIndex == index) iconColor else secondaryTextColor,
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
                                if (!searchMode && selectedCategoryIndex == index) accentColor
                                else Color.Transparent,
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }

        if (searchMode) {
            // 搜尋模式:查詢列 + 結果格 + 迷你鍵盤
            EmojiSearchPane(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClose = { searchMode = false },
                tonePrefs = tonePrefs,
                onToneSelected = { base, tone ->
                    tonePrefs = saveTonePref(context, tonePrefs, base, tone)
                },
                onEmojiClick = { char ->
                    onEmojiClick(char)
                    addToRecent(char)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f) // Fill available space
                .fillMaxWidth()
        ) { page ->
            // Read directly (no remember) so Compose reacts when EmojiData.initialize() completes
            val emojisForPage = if (page == 0) {
                // 回查完整資訊,讓最近使用的項目也能長按選膚色
                recentEmojis.map { stored ->
                    val base = EmojiData.baseOf(stored)
                    EmojiData.lookup(base) ?: Emoji(base, emptyList(), false)
                }
            } else {
                EmojiData.getListByCategory(page)
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(
                    items = emojisForPage,
                    key = { index, emoji -> "${emoji.char}_$index" }
                ) { _, emoji ->
                    EmojiGridItem(
                        emoji = emoji,
                        preferredTone = tonePrefs[emoji.char],
                        onToneSelected = { tone ->
                            tonePrefs = saveTonePref(context, tonePrefs, emoji.char, tone)
                        },
                        onEmojiClick = { char ->
                            onEmojiClick(char)
                            addToRecent(char)
                        }
                    )
                }
            }
        }
        }

        // Custom Bottom Bar: ABC (left) | ⌫ (right)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp), 
            color = bottomBarColor
        ) {
            Column {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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

                    // Backspace ⌫(搜尋模式時改刪搜尋字,不動目標欄位)
                    IconButton(
                        onClick = {
                            if (searchMode) {
                                if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1)
                            } else {
                                onBackspaceClick()
                            }
                        },
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
}

@Composable
fun EmojiGridItem(
    emoji: Emoji,
    onEmojiClick: (String) -> Unit,
    preferredTone: String? = null,
    onToneSelected: (String?) -> Unit = {}
) {
    var showSkinTonePopup by remember { mutableStateOf(false) }

    // 有記住的膚色偏好時,格子直接顯示偏好膚色,點擊也送出偏好膚色
    val displayChar = if (emoji.hasSkinTone && !preferredTone.isNullOrEmpty())
        EmojiData.applySkinTone(emoji.char, preferredTone)
    else
        emoji.char

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(emoji, displayChar) {
                detectTapGestures(
                    onTap = { onEmojiClick(displayChar) },
                    onLongPress = {
                        if (emoji.hasSkinTone) {
                            showSkinTonePopup = true
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = displayChar, fontSize = 30.sp)

        if (emoji.hasSkinTone) {
            Canvas(modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(8.dp)
            ) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width, size.height) 
                    lineTo(size.width, 0f)          
                    lineTo(0f, size.height)         
                    close()
                }
                drawPath(path, color = Color.LightGray.copy(alpha = 0.8f))
            }
        }

        if (showSkinTonePopup) {
            SkinTonePopup(
                baseEmoji = emoji.char,
                onDismiss = { showSkinTonePopup = false },
                onSkinToneSelected = { variant ->
                    // 記住這次選的膚色,之後點擊直接套用;選回原色則清除偏好
                    val tone = EmojiData.SKIN_TONES.firstOrNull { variant.contains(it) }
                    onToneSelected(tone)
                    onEmojiClick(variant)
                    showSkinTonePopup = false
                }
            )
        }
    }
}

/**
 * 搜尋面板:查詢列 + 結果格 + 迷你 QWERTY。
 * 選擇器位於輸入法視窗內,無法呼叫輸入法自己輸入,故內建英文迷你鍵盤;
 * 比對對象為 CLDR 英文關鍵字(emojis.json 的 keywords)。
 */
@Composable
fun EmojiSearchPane(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    tonePrefs: Map<String, String>,
    onToneSelected: (String, String?) -> Unit,
    onEmojiClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val results = remember(query) { searchEmojis(query) }

    // 接著實體鍵盤時隱藏迷你鍵盤(輸入走硬體按鍵橋接),拔掉後自動恢復
    val hasHardKeyboard = LocalConfiguration.current.hardKeyboardHidden ==
            android.content.res.Configuration.HARDKEYBOARDHIDDEN_NO

    // 讓實體鍵盤也能輸入搜尋:註冊橋接,LIMEService.onKeyDown 會把
    // a-z/空白/倒退/Esc 餵進來(用 rememberUpdatedState 避免閉包吃到舊 query)
    val currentQuery = rememberUpdatedState(query)
    val currentOnQueryChange = rememberUpdatedState(onQueryChange)
    val currentOnClose = rememberUpdatedState(onClose)
    DisposableEffect(Unit) {
        EmojiSearchBridge.activate(
            onChar = { c -> currentOnQueryChange.value(currentQuery.value + c) },
            onBackspace = {
                if (currentQuery.value.isNotEmpty())
                    currentOnQueryChange.value(currentQuery.value.dropLast(1))
            },
            onClose = { currentOnClose.value() }
        )
        onDispose { EmojiSearchBridge.deactivate() }
    }

    Column(modifier = modifier) {
        // 查詢列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    query.isNotEmpty() -> query
                    hasHardKeyboard -> "用實體鍵盤輸入英文關鍵字…"
                    else -> "輸入英文關鍵字…"
                },
                color = if (query.isEmpty()) Color(0xFF9E9E9E) else Color.White,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "關閉搜尋",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 結果格
        Box(modifier = Modifier.weight(1f)) {
            if (query.isNotEmpty() && results.isEmpty()) {
                Text(
                    text = "沒有符合的表情符號",
                    color = Color(0xFF9E9E9E),
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                ) {
                    itemsIndexed(
                        items = results,
                        key = { index, emoji -> "${emoji.char}_$index" }
                    ) { _, emoji ->
                        EmojiGridItem(
                            emoji = emoji,
                            preferredTone = tonePrefs[emoji.char],
                            onToneSelected = { tone -> onToneSelected(emoji.char, tone) },
                            onEmojiClick = onEmojiClick
                        )
                    }
                }
            }
        }

        // 迷你 QWERTY(英文);接實體鍵盤時不顯示
        if (!hasHardKeyboard) {
            MiniQwerty(
                onKey = { c -> onQueryChange(query + c) }
            )
        }
    }
}

private fun searchEmojis(query: String): List<Emoji> {
    if (query.isBlank()) return emptyList()
    val q = query.lowercase()
    val all = listOf(
        EmojiData.SMILEYS, EmojiData.PEOPLE, EmojiData.ANIMALS_NATURE,
        EmojiData.FOOD_DRINK, EmojiData.TRAVEL_PLACES, EmojiData.ACTIVITIES,
        EmojiData.OBJECTS, EmojiData.SYMBOLS, EmojiData.FLAGS
    )
    // 前綴命中排前,一般包含次之
    val prefix = mutableListOf<Emoji>()
    val contains = mutableListOf<Emoji>()
    for (category in all) {
        for (emoji in category) {
            when {
                emoji.keywords.any { it.startsWith(q) } -> prefix.add(emoji)
                emoji.keywords.any { it.contains(q) } -> contains.add(emoji)
            }
            if (prefix.size >= 60) return prefix
        }
    }
    return (prefix + contains).take(60)
}

@Composable
private fun MiniQwerty(
    onKey: (Char) -> Unit
) {
    val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F1F1F))
            .padding(vertical = 2.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // 三排鍵寬一致:以 10 鍵為基準,較短的排兩側留空置中
                val sidePad = (10 - row.length) / 2f
                if (sidePad > 0f) Spacer(modifier = Modifier.weight(sidePad))
                row.forEach { c ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF3A3A3A))
                            .clickable { onKey(c) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = c.toString(), color = Color.White, fontSize = 22.sp)
                    }
                }
                if (sidePad > 0f) Spacer(modifier = Modifier.weight(sidePad))
            }
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
private const val KEY_TONE_PREFS = "skin_tone_prefs"

/** 膚色偏好:base emoji -> 膚色修飾字元(🏻🏼🏽🏾🏿) */
fun loadTonePrefs(context: Context): Map<String, String> {
    val saved = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_TONE_PREFS, "") ?: ""
    if (saved.isEmpty()) return emptyMap()
    return saved.split(",").mapNotNull { entry ->
        val parts = entry.split("|")
        if (parts.size == 2) parts[0] to parts[1] else null
    }.toMap()
}

/** 更新單一 base emoji 的膚色偏好(tone 為 null 表示清除,回到原色)並回傳新 map */
fun saveTonePref(context: Context, current: Map<String, String>, base: String, tone: String?): Map<String, String> {
    val newMap = current.toMutableMap()
    if (tone.isNullOrEmpty()) newMap.remove(base) else newMap[base] = tone
    val serialized = newMap.entries.joinToString(",") { "${it.key}|${it.value}" }
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit().putString(KEY_TONE_PREFS, serialized).apply()
    return newMap
}

fun loadRecentEmojis(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val saved = prefs.getString(KEY_RECENT, "") ?: ""
    return if (saved.isEmpty()) emptyList() else saved.split(",")
}

fun saveRecentEmojis(context: Context, emojis: List<String>) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_RECENT, emojis.joinToString(",")).apply()
}
