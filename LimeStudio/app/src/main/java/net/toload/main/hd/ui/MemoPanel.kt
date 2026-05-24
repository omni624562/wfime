package net.toload.main.hd.ui

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import net.toload.main.hd.DBServer
import net.toload.main.hd.limedb.MemoObj

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoPanel(
    dbServer: DBServer,
    onMemoClick: (String) -> Unit,
    onBackClick: () -> Unit,
    bottomPaddingDp: Int = 0
) {
    val context = LocalContext.current
    val memos = remember { mutableStateListOf<MemoObj>() }

    // Helper to reload memos from SQLite
    fun reloadMemos() {
        memos.clear()
        memos.addAll(dbServer.getMemos())
    }

    // Load initial list
    LaunchedEffect(Unit) {
        reloadMemos()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var memoInputText by remember { mutableStateOf("") }

    // Styles & Theme
    val backgroundColor = Color(0xFF2B2B2B)
    val cardBackgroundColor = Color(0xFF383838)
    val pinnedCardBackgroundColor = Color(0xFF203A2B)
    val accentColor = Color(0xFF4CAF50) // Neon green accent
    val iconColor = Color(0xFFE2E2E2)
    val secondaryTextColor = Color(0xFF9E9E9E)
    val bottomBarColor = Color(0xFF1F1F1F)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(bottom = bottomPaddingDp.dp)
    ) {
        // 1. Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = iconColor
                )
            }

            Text(
                text = "常用備忘錄",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Capsule Button: Paste from clipboard
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clipData = clipboard?.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text?.toString() ?: ""
                        if (text.isNotBlank()) {
                            dbServer.insertMemo(text, 0)
                            reloadMemos()
                            Toast.makeText(context, "已自剪貼簿新增備忘錄", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "剪貼簿中無文字內容", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "剪貼簿為空", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .height(32.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "貼上新增",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Manual Add Icon Button
            IconButton(
                onClick = {
                    memoInputText = ""
                    showAddDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "手動新增",
                    tint = accentColor
                )
            }
        }

        // Horizontal Divider
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

        // 2. Memo Cards List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (memos.isEmpty()) {
                // Empty state placeholder
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = secondaryTextColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "尚無常用備忘錄",
                        color = secondaryTextColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "點擊右上角按鈕貼上或新增",
                        color = secondaryTextColor.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = memos,
                        key = { it.id }
                    ) { memo ->
                        val isPinned = memo.pinned == 1
                        val cardBg = if (isPinned) pinnedCardBackgroundColor else cardBackgroundColor
                        val borderStroke = if (isPinned) BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)) else null

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = borderStroke,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onMemoClick(memo.content) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Content text
                                Text(
                                    text = memo.content,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Pin Button
                                IconButton(
                                    onClick = {
                                        val newPinnedStatus = if (isPinned) 0 else 1
                                        dbServer.updateMemoPin(memo.id, newPinnedStatus)
                                        reloadMemos()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = if (isPinned) "取消置頂" else "置頂",
                                        tint = if (isPinned) accentColor else secondaryTextColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete Button
                                IconButton(
                                    onClick = {
                                        dbServer.deleteMemo(memo.id)
                                        reloadMemos()
                                        Toast.makeText(context, "備忘錄已刪除", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "刪除",
                                        tint = Color(0xFFE57373),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Custom Bottom Navigation Bar (matches EmojiPicker layout)
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
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                }
            }
        }
    }

    // 4. Custom Manual Add Dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2E2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "新增常用備忘錄",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = memoInputText,
                        onValueChange = { memoInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = { Text(text = "輸入欲儲存的文字內容...", color = secondaryTextColor) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = secondaryTextColor.copy(alpha = 0.5f),
                            cursorColor = accentColor
                        ),
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showAddDialog = false }
                        ) {
                            Text(text = "取消", color = secondaryTextColor)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (memoInputText.isNotBlank()) {
                                    dbServer.insertMemo(memoInputText, 0)
                                    reloadMemos()
                                    showAddDialog = false
                                    Toast.makeText(context, "備忘錄已儲存", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "請輸入內容", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text(text = "儲存", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
