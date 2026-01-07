package nan.toload.main.hd

import android.content.Context
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import nan.toload.main.hd.ui.EmojiPicker
import android.view.KeyEvent

object ComposeBridge {
    fun createEmojiPickerView(context: Context, service: LIMEService): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                EmojiPicker(
                    onEmojiClick = { emoji -> 
                        service.onText(emoji) 
                    },
                    onBackClick = { 
                        service.closeEmojiPicker() 
                    },
                    onBackspaceClick = { 
                        // Simulate Backspace
                         service.handleComposeBackspace()
                    }
                )
            }
        }
    }
}
