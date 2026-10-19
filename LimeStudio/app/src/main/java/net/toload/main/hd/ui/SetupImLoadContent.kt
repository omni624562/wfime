package net.toload.main.hd.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import net.toload.main.hd.ui.compose.theme.LimeTheme
import java.util.function.Consumer

/**
 * 載入／移除輸入法、匯入關聯字庫對話框（SetupImLoadDialog）的內容：
 * 上面是「還原學習資料」或「備份學習資料」勾選框（依情況只顯示一個或都不顯示），下面一排滿寬的按鈕。
 * 勾選狀態由 SetupImLoadDialog 保存（載入結束時才讀，旋轉畫面也要留著），這裡只顯示並回報變更。
 */
object SetupImLoadContent {
    class Action(val label: String, val onClick: Runnable)

    @JvmStatic
    fun createView(
        context: Context,
        restoreLabel: String?,
        restoreChecked: Boolean,
        onRestoreChanged: Consumer<Boolean>,
        backupLabel: String?,
        backupChecked: Boolean,
        onBackupChanged: Consumer<Boolean>,
        actions: List<Action>
    ): View = ComposeView(context).apply {
        setContent {
            LimeTheme {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    if (restoreLabel != null) LearningCheckBox(restoreLabel, restoreChecked, onRestoreChanged)
                    if (backupLabel != null) LearningCheckBox(backupLabel, backupChecked, onBackupChanged)
                    // 同 MaterialButton：按鈕上下固定各留 4dp（insetTop／insetBottom），兩行的按鈕之間也有間隔
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        for (action in actions) {
                            Button(
                                onClick = { action.onClick.run() },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                contentPadding = PaddingValues(10.dp)  // 舊版 android:padding="10dp"
                            ) {
                                Text(action.label, textAlign = TextAlign.Center, style = materialButtonTextStyle())
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 按鈕文字照舊版 MaterialButton（TextView）：字型本身的行高、含字型上下留白，兩行時高度才一樣 */
@Composable
internal fun materialButtonTextStyle(): TextStyle = LocalTextStyle.current.copy(
    lineHeight = TextUnit.Unspecified,
    platformStyle = PlatformTextStyle(includeFontPadding = true)
)

@Composable
private fun LearningCheckBox(label: String, initial: Boolean, onChanged: Consumer<Boolean>) {
    var checked by remember { mutableStateOf(initial) }
    // 同舊版 CheckBox：高 48dp、置中、點方框或文字都能切換
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Row(
            Modifier.toggleable(value = checked, role = Role.Checkbox) { checked = it; onChanged.accept(it) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Checkbox(checked = checked, onCheckedChange = null)
            }
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
