package nan.toload.main.hd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nan.toload.main.hd.data.EmojiData

@Composable
fun EmojiPicker(
    onEmojiClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(1) } // Default to Smileys
    var searchText by remember { mutableStateOf("") }

    val categories = listOf(
        "🕒" to 0, // Recent (Placeholder logic needed)
        "😀" to 1,
        "🐻" to 2,
        "🍔" to 3,
        "💡" to 4
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202124))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFA0A0A0)
                )
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(Color(0xFF303134), MaterialTheme.shapes.extraLarge)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchText.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF9AA0A6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search emojis", color = Color(0xFF9AA0A6), fontSize = 14.sp)
                    }
                }
                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    textStyle = TextStyle(color = Color(0xFFE8EAED), fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(0xFF8AB4F8)),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            IconButton(onClick = onBackspaceClick) {
                 // Use a vector icon or text for delete
                 // Assuming we don't have resource ID easy access here, use text or standard icon
                 Icon(
                     imageVector = Icons.Default.Close, // Placeholder for Backspace/Delete which usually looks like <-x
                     contentDescription = "Delete",
                     tint = Color(0xFFA0A0A0)
                 )
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = Color(0xFF202124),
            contentColor = Color(0xFF8AB4F8),
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                    color = Color(0xFF8AB4F8)
                )
            }
        ) {
            categories.forEachIndexed { index, (icon, categoryId) ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    text = { Text(icon, fontSize = 20.sp) }, // Use Emoji as tab icon
                    selectedContentColor = Color(0xFF8AB4F8),
                    unselectedContentColor = Color(0xFF9AA0A6)
                )
            }
        }

        // Grid
        // Filter data based on search or category
        val emojiList = remember(selectedCategoryIndex, searchText) {
            if (searchText.isNotEmpty()) {
                // Naive search: filter all? For now, search in current category or all
                // Searching all requires flattening.
                // Let's just return filtered current category for simplicity, or flattened list.
                // Doing flattened search:
                val all = EmojiData.SMILEYS + EmojiData.ANIMALS + EmojiData.FOOD + EmojiData.OBJECTS
                all.filter { it.contains(searchText) || true } // 'true' because emojis don't have names in list currently
                // NOTE: EmojiData only has strings. Can't search by name unless we map emoji to name.
                // So search is useless without metadata. 
                // Return current category.
                EmojiData.getListByCategory(categories[selectedCategoryIndex].second).toList()
            } else {
                EmojiData.getListByCategory(categories[selectedCategoryIndex].second).toList()
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(emojiList) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onEmojiClick(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }
    }
}
