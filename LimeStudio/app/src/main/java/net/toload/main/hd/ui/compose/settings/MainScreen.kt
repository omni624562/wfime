package net.toload.main.hd.ui.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import net.toload.main.hd.R

/**
 * 設定首頁（MainActivity）：手機上面是標題列（「麥田輸入法」＋右上角選單「使用說明」），平板不顯示標題列。
 * 取代原本的 activity_main.xml（CoordinatorLayout＋MaterialToolbar），位置照舊：手機的標題列延伸到狀態列底下，
 * 設定內容只避開左右的系統列、下面延伸到導覽列底下；平板的設定內容從螢幕最上面開始。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SettingsViewModel, showTopBar: Boolean, onHelp: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (showTopBar) {
            var menuExpanded by remember { mutableStateOf(false) }
            TopAppBar(
                title = { Text(stringResource(R.string.action_preference)) },
                actions = {
                    Box {
                        // 同 MaterialToolbar 的溢出選單鈕：36dp 寬、圓形水波紋
                        Box(
                            Modifier.size(width = 36.dp, height = 48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 20.dp),
                                    role = Role.Button
                                ) { menuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options)
                            )
                        }
                        // 同 AppCompat 選單：蓋在選單鈕上、至少 196dp 寬、文字左邊留 16dp
                        // （Compose 選單離視窗上緣至少 48dp，所以比舊版低約 16dp）
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            offset = DpOffset(0.dp, (-48).dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_help), style = MaterialTheme.typography.bodyLarge) },
                                onClick = { menuExpanded = false; onHelp() },
                                modifier = Modifier.widthIn(min = 196.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            )
                        }
                    }
                }
            )
        }
        Box(Modifier.weight(1f).windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))) {
            SettingsScreen(viewModel = viewModel)
        }
    }
}
