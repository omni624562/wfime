package nan.toload.main.hd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nan.toload.main.hd.R
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
        "🕒" to 0, // Recent
        "😀" to 1,
        "🐻" to 2,
        "🍔" to 3,
        "💡" to 4
    )

    // Load Colors from Resources
    val backgroundColor = colorResource(id = R.color.keyboard_background_dark)
    val searchBarColor = colorResource(id = R.color.functional_key_background_dark) // Lighter than bg
    val accentColor = colorResource(id = R.color.color_common_green_hl)
    val textColor = colorResource(id = R.color.foreground_dark)
    val secondaryTextColor = colorResource(id = R.color.second_foreground_dark)

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    .clip(RoundedCornerShape(20.dp)) // Round pill shape
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

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            containerColor = backgroundColor,
            contentColor = accentColor,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedCategoryIndex]),
                    color = accentColor,
                    height = 3.dp
                )
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
                           fontSize = 24.sp, // Larger tab icons
                           color = if (selected) accentColor else secondaryTextColor.copy(alpha = 0.7f)
                       )
                   }
                }
            }
        }

        // Grid
        val emojiList = remember(selectedCategoryIndex, searchText) {
            if (searchText.isNotEmpty()) {
                 val all = EmojiData.SMILEYS + EmojiData.ANIMALS + EmojiData.FOOD + EmojiData.OBJECTS
                 all.filter { true } 
                 EmojiData.getListByCategory(categories[selectedCategoryIndex].second).toList()
            } else {
                EmojiData.getListByCategory(categories[selectedCategoryIndex].second).toList()
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp), // Spacing between rows
            horizontalArrangement = Arrangement.spacedBy(4.dp) // Spacing between cols
        ) {
            items(emojiList) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = true, color = accentColor.copy(alpha = 0.3f)), // Softer ripple
                            onClick = { onEmojiClick(emoji) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 30.sp) // Larger Emojis
                }
            }
        }
    }
}
