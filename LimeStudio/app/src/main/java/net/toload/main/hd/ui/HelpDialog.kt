package net.toload.main.hd.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import net.toload.main.hd.ui.compose.theme.LimeTheme

class HelpDialog : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make it fullscreen on tablet/dialog
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LimeTheme {
                    HelpDialogContent(onDismiss = { dismiss() })
                }
            }
        }
    }
}

@Composable
fun HelpDialogContent(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        Triple(0, "✨ 旗艦功能", Icons.Default.AutoAwesome),
        Triple(1, "⌨️ 鍵盤教學", Icons.Default.Keyboard),
        Triple(2, "📋 更新日誌", Icons.Default.List)
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "使用說明與教學",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs (Segmented Control style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (index, title, icon) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Content Area with elegant fade transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> TabFlagshipFeatures()
                    1 -> TabKeyboardTutorial()
                    2 -> TabUpdateLog()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Action Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "開始使用麥田輸入法",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TabFlagshipFeatures() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FeatureItem(
                title = "大易實體鍵盤左側翻頁面板",
                description = "專為實體外接鍵盤設計，將「▲ (上頁) / ▼ (下頁)」翻頁控制列移至最左側。實現「右手打字、左手翻頁」的完美分工，按鈕隨頁數動態亮起，大幅降低手部移動疲勞與誤觸率。",
                icon = Icons.Default.Keyboard,
                accentColor = Color(0xFF00E676)
            )
        }
        item {
            FeatureItem(
                title = "全面屏沉浸式設定主控台",
                description = "拋棄傳統 XML 頂部標題贅肉，讓設定畫面完美直通頂部邊緣，結合極簡左側導覽列與磨砂玻璃漸層背景，釋放超過 15% 以上的閱讀空間，展現極致高貴設計感。",
                icon = Icons.Default.Fullscreen,
                accentColor = Color(0xFF3A86FF)
            )
        }
        item {
            FeatureItem(
                title = "AI 智慧選字呼吸指示燈",
                description = "大易智慧選字模組升級為「AI 智慧核心」，搭載全新狀態呼吸燈。啟用時會亮起科技綠色，並微亮渲染整張卡片，以動態科技美學呈現系統運算狀態。",
                icon = Icons.Default.AutoAwesome,
                accentColor = Color(0xFF8338EC)
            )
        }
    }
}

@Composable
fun FeatureItem(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun TabKeyboardTutorial() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TutorialCard(
                title = "💡 遇到異常時如何排查？",
                steps = listOf(
                    "若輸入法運作異常、字對照表遺失，請點擊左側導覽面板最底部的「重置設定值」按鍵，恢復原廠設定即可解決 95% 以上問題。",
                    "安裝新版對照表後，請確保「啟用大易輸入法」與「啟用注音輸入法」開關處於啟動狀態。"
                )
            )
        }
        item {
            TutorialCard(
                title = "🎹 實體鍵盤（外接藍牙/磁吸）操作流",
                steps = listOf(
                    "打字時，右手於實體鍵盤正常輸入字碼與選字數字鍵。",
                    "需要翻頁時，左手直接點選平板最左側螢幕邊緣的「▲ (前一頁)」與「▼ (後一頁)」進行翻頁，極具效率。",
                    "系統預設支援實體鍵盤直接送出字詞，亦可於設定內開啟「實體鍵盤優先排序」確保常用字前排顯示。"
                )
            )
        }
    }
}

@Composable
fun TutorialCard(title: String, steps: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TabUpdateLog() {
    val logs = listOf(
        UpdateLogItem(
            version = "v1.4.0-tablet",
            date = "2026.06.01",
            changes = listOf(
                "大易實體外接鍵盤左側翻頁優化 (▲/▼)。",
                "平板雙欄全面屏沉浸式設定主控台升級，隱藏 XML AppBarLayout。",
                "大易智慧選字核心 AI 卡片改裝與綠/灰狀態呼吸燈導入。",
                "整合使用說明與重置功能至導覽列底部。"
            ),
            isFlagship = true
        ),
        UpdateLogItem(
            version = "v1.3.1",
            date = "2026.02.28",
            changes = listOf(
                "簡化架構，移除舊用語音輸入功能按鍵與 VoiceInputActivity。",
                "為 LimeDB、IMSwitchHelper、OptionsDialogHelper 新增 66 項全數通過之單元測試案例。"
            )
        ),
        UpdateLogItem(
            version = "v1.3.0",
            date = "2026.02.05",
            changes = listOf(
                "修復繪文字鍵盤 (Emoji Picker) 切換中斷與輸入法崩潰問題。",
                "最佳化輸入法開啟穩定度與系統整合。"
            )
        ),
        UpdateLogItem(
            version = "v1.2.0",
            date = "2026.01.03",
            changes = listOf(
                "全面導入 Material Design 3 設計語言，支援動態色彩與全屏顯示。",
                "修復側邊選單導致的隨機程式崩潰 Bug。",
                "版本格式更動為日期格式 (yyyy.MM.dd) 以利識別。"
            )
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(logs) { log ->
            LogCard(log)
        }
    }
}

data class UpdateLogItem(
    val version: String,
    val date: String,
    val changes: List<String>,
    val isFlagship: Boolean = false
)

@Composable
fun LogCard(log: UpdateLogItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (log.isFlagship)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (log.isFlagship)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.version,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (log.isFlagship) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = log.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            log.changes.forEach { change ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        color = if (log.isFlagship) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = change,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
