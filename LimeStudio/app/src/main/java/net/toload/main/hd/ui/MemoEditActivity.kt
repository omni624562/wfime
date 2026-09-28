package net.toload.main.hd.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.toload.main.hd.DBServer
import net.toload.main.hd.R

// 備忘錄新增後通知備忘錄面板重新讀取清單（面板只建立一次，與這個 Activity 在同一個程序）
internal object MemoChangeSignal {
    var version by mutableIntStateOf(0)
        private set

    fun notifyChanged() {
        version++
    }
}

/**
 * 新增備忘錄的小視窗。備忘錄面板在輸入法視窗裡：開 Compose Dialog 會因沒有視窗 token 而
 * BadTokenException，面板裡的輸入框也收不到鍵盤輸入，所以「+」改開這個獨立的 Activity，
 * 輸入框是一般 app 視窗，可以直接用 WFIME 打字。
 */
class MemoEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MemoEditScreen(onDone = { finish() }) }
    }
}

@Composable
private fun MemoEditScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val panelColors = keyboardPanelColors()
    val accentColor = panelColors.accent
    val secondaryTextColor = panelColors.secondary
    var memoInputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        // 半透明遮罩：點外面取消（與原本 Dialog 的 onDismissRequest 相同）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = panelColors.dialogCard),
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
                    text = stringResource(R.string.memo_add_title),
                    color = panelColors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = memoInputText,
                    onValueChange = { memoInputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text(text = stringResource(R.string.memo_input_hint), color = secondaryTextColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = panelColors.text,
                        unfocusedTextColor = panelColors.text,
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
                    TextButton(onClick = onDone) {
                        Text(text = stringResource(R.string.dialog_cancel), color = secondaryTextColor)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (memoInputText.isNotBlank()) {
                                DBServer(context).insertMemo(memoInputText, 0)
                                MemoChangeSignal.notifyChanged()
                                Toast.makeText(context, context.getString(R.string.memo_saved), Toast.LENGTH_SHORT).show()
                                onDone()
                            } else {
                                Toast.makeText(context, context.getString(R.string.memo_input_required), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(text = stringResource(R.string.memo_save), color = panelColors.onAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 開啟時輸入框取得焦點並叫出鍵盤，可直接打字
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
