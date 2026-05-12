/*
 *
 *  *
 *  **    Copyright 2015, The LimeIME Open Source Project
 *  **
 *  **    Project Url: http://github.com/lime-ime/limeime/
 *  **                 http://android.toload.net/
 *  **
 *  **    This program is free software: you can redistribute it and/or modify
 *  **    it under the terms of the GNU General Public License as published by
 *  **    the Free Software Foundation, either version 3 of the License, or
 *  **    (at your option) any later version.
 *  *
 *  **    This program is distributed in the hope that it will be useful,
 *  **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  **    GNU General Public License for more details.
 *  *
 *  **    You should have received a copy of the GNU General Public License
 *  **    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *
 */

package nan.toload.main.hd.ui

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import nan.toload.main.hd.R

/**
 * Helper class for displaying Material3 LoadingDialog from Java code.
 * Replacement for deprecated ProgressDialog.
 *
 * 從 Java 程式碼顯示 Material3 LoadingDialog 的輔助類別。
 * 替代已棄用的 ProgressDialog。
 *
 * Usage from Java:
 * ```java
 * LoadingDialogHelper dialog = new LoadingDialogHelper(context);
 * dialog.setMessage("Loading...");
 * dialog.show();
 * dialog.setProgress(0.5f);  // For determinate progress (0.0 to 1.0)
 * dialog.dismiss();
 * ```
 */
class LoadingDialogHelper(private val context: Context) {

    private var dialog: Dialog? = null
    private val messageState = mutableStateOf("")
    private val progressState = mutableStateOf<Float?>(null)
    private var isShowing = false
    
    private var lifecycleOwner: LifecycleOwner? = findLifecycleOwner(context)
    private var savedStateRegistryOwner: SavedStateRegistryOwner? = findSavedStateRegistryOwner(context)
    private var viewModelStoreOwner: ViewModelStoreOwner? = findViewModelStoreOwner(context)

    init {
        createDialog()
    }

    private fun findLifecycleOwner(context: Context): LifecycleOwner? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is LifecycleOwner) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    private fun findSavedStateRegistryOwner(context: Context): SavedStateRegistryOwner? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is SavedStateRegistryOwner) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    private fun findViewModelStoreOwner(context: Context): ViewModelStoreOwner? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is ViewModelStoreOwner) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    private fun createDialog() {
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)

            val composeView = ComposeView(context).apply {
                // Set ViewTree owners if available.
                // This prevents "ViewTreeLifecycleOwner not found" when showing the dialog.
                lifecycleOwner?.let { setViewTreeLifecycleOwner(it) }
                savedStateRegistryOwner?.let { setViewTreeSavedStateRegistryOwner(it) }
                viewModelStoreOwner?.let { setViewTreeViewModelStoreOwner(it) }

                setContent {
                    LoadingDialog(
                        message = messageState.value,
                        progress = progressState.value
                    )
                }
            }

            setContentView(composeView)
        }
    }

    /**
     * Set ViewTree owners explicitly. Useful for Service contexts (like LIMEService)
     * where the context itself doesn't implement these interfaces.
     */
    fun setOwners(
        lifecycleOwner: LifecycleOwner?,
        savedStateRegistryOwner: SavedStateRegistryOwner?,
        viewModelStoreOwner: ViewModelStoreOwner?
    ) {
        this.lifecycleOwner = lifecycleOwner
        this.savedStateRegistryOwner = savedStateRegistryOwner
        this.viewModelStoreOwner = viewModelStoreOwner
        
        // Re-create dialog if it was already initialized to apply new owners to its ComposeView
        createDialog()
    }

    /**
     * Set the message to display in the dialog.
     * 設定要在對話框中顯示的訊息。
     */
    fun setMessage(message: String) {
        messageState.value = message
    }

    /**
     * Set the progress value for determinate progress indicator.
     * Pass null for indeterminate (spinner) style.
     *
     * 設定確定進度指示器的進度值。
     * 傳遞 null 表示不確定（spinner）樣式。
     *
     * @param progress Progress value from 0.0 to 1.0, or null for indeterminate
     *                 進度值從 0.0 至 1.0，或 null 表示不確定
     */
    fun setProgress(progress: Float?) {
        progressState.value = progress
    }

    /**
     * Set the progress value from 0 to 100.
     * 設定從 0 至 100 的進度值。
     */
    fun setProgress(progress: Int) {
        progressState.value = progress / 100f
    }

    /**
     * Set to indeterminate (spinner) style.
     * 設定為不確定（spinner）樣式。
     */
    fun setIndeterminate(indeterminate: Boolean) {
        if (indeterminate) {
            progressState.value = null
        } else if (progressState.value == null) {
            progressState.value = 0f
        }
    }

    /**
     * Show the loading dialog.
     * 顯示載入對話框。
     */
    fun show() {
        if (!isShowing) {
            dialog?.show()
            isShowing = true
        }
    }

    /**
     * Dismiss the loading dialog.
     * 關閉載入對話框。
     */
    fun dismiss() {
        if (isShowing) {
            dialog?.dismiss()
            isShowing = false
        }
    }

    /**
     * Check if the dialog is currently showing.
     * 檢查對話框是否正在顯示。
     */
    fun isShowing(): Boolean {
        return isShowing
    }

    /**
     * Set maximum value for progress (for API compatibility).
     * This is only used for converting setProgress(int) calls.
     *
     * 設定進度的最大值（用於 API 相容性）。
     * 僅用於轉換 setProgress(int) 呼叫。
     */
    fun setMax(max: Int) {
        // This is kept for API compatibility with ProgressDialog
        // The actual implementation uses 0.0-1.0 range
        // 保留此方法以與 ProgressDialog API 相容
        // 實際實作使用 0.0-1.0 範圍
    }

    /**
     * Set whether the dialog is cancelable (for API compatibility).
     * LoadingDialog is always non-cancelable.
     *
     * 設定對話框是否可取消（用於 API 相容性）。
     * LoadingDialog 始終不可取消。
     */
    fun setCancelable(cancelable: Boolean) {
        // This is kept for API compatibility with ProgressDialog
        // LoadingDialog is always non-cancelable
        // 保留此方法以與 ProgressDialog API 相容
        // LoadingDialog 始終不可取消
        dialog?.setCancelable(cancelable)
    }
}
