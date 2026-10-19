package net.toload.main.hd.ui

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.toload.main.hd.Lime
import net.toload.main.hd.R
import net.toload.main.hd.ui.compose.theme.LimeTheme

/**
 * 分享文字匯入對話框（ImportDialog）的內容：大易、注音並排，下面是關聯字庫與取消。
 * 照舊版：沒載入的輸入法（以及分享的文字只有一個字時的關聯字庫）半透明、斜體、不能按；載入的輸入法用粗體。
 */
object ImportDialogContent {
    @JvmStatic
    fun createView(
        context: Context,
        dayiEnabled: Boolean,
        phoneticEnabled: Boolean,
        relatedEnabled: Boolean,
        onDayi: Runnable,
        onPhonetic: Runnable,
        onRelated: Runnable,
        onCancel: Runnable
    ): View = ComposeView(context).apply {
        setContent {
            LimeTheme {
                // 對話框寬度由按鈕的自然寬度決定（舊版視窗也是 wrap_content）
                // 同 MaterialButton：按鈕上下固定各留 4dp（insetTop／insetBottom）
                Column(Modifier.width(IntrinsicSize.Max).padding(10.dp)) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        Row(Modifier.fillMaxWidth()) {
                            ImportButton(stringResource(R.string.im_dayi), dayiEnabled, dayiEnabled,
                                Modifier.weight(1f)) { onDayi.run() }
                            ImportButton(stringResource(R.string.im_phonetic), phoneticEnabled, phoneticEnabled,
                                Modifier.weight(1f)) { onPhonetic.run() }
                        }
                        ImportButton(stringResource(R.string.related), relatedEnabled, false,
                            Modifier.fillMaxWidth()) { onRelated.run() }
                        ImportButton(stringResource(R.string.dialog_cancel), true, false,
                            Modifier.fillMaxWidth()) { onCancel.run() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportButton(text: String, enabled: Boolean, bold: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        // 不能按的照舊版再減半透明度
        modifier = modifier.padding(vertical = 4.dp)
            .alpha(if (enabled) Lime.NORMAL_ALPHA_VALUE else Lime.HALF_ALPHA_VALUE)
    ) {
        Text(
            text,
            style = materialButtonTextStyle(),
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (enabled) null else FontStyle.Italic
        )
    }
}
