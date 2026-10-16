package net.toload.main.hd.keyboard

/**
 * 長按與連續刪除的計時。PointerTracker、PointerDispatcher 只透過這個介面要求計時，
 * 不直接碰 Android 的 Handler；鍵盤 View 用 UIHandler 實作，單元測試可以換成假的計時器（藍圖 A4-2）。
 */
interface KeyTimers {
    fun startKeyRepeatTimer(delay: Long, keyIndex: Int, tracker: PointerTracker)
    fun cancelKeyRepeatTimer()
    fun isInKeyRepeat(): Boolean
    fun startLongPressTimer(delay: Long, keyIndex: Int, tracker: PointerTracker)
    fun cancelLongPressTimer()
    fun cancelKeyTimers()
}
