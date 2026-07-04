package net.toload.main.hd.ui

/**
 * emoji 選擇器搜尋模式與 LIMEService 硬體鍵盤的橋接。
 * 搜尋面板組成時註冊 callback,LIMEService.onKeyDown 在搜尋模式
 * 作用中時把 a-z/空白/倒退/Esc 直接餵給搜尋框(不進 IME 組字管線)。
 */
object EmojiSearchBridge {

    @Volatile
    var isActive: Boolean = false
        private set

    private var onChar: ((Char) -> Unit)? = null
    private var onBackspace: (() -> Unit)? = null
    private var onClose: (() -> Unit)? = null

    fun activate(onChar: (Char) -> Unit, onBackspace: () -> Unit, onClose: () -> Unit) {
        this.onChar = onChar
        this.onBackspace = onBackspace
        this.onClose = onClose
        isActive = true
    }

    fun deactivate() {
        isActive = false
        onChar = null
        onBackspace = null
        onClose = null
    }

    @JvmStatic
    fun sendChar(c: Char) {
        onChar?.invoke(c)
    }

    @JvmStatic
    fun sendBackspace() {
        onBackspace?.invoke()
    }

    @JvmStatic
    fun sendClose() {
        onClose?.invoke()
    }
}
