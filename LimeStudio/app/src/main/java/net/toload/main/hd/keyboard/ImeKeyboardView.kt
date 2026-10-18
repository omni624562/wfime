package net.toload.main.hd.keyboard

import android.view.View

/**
 * 輸入法服務（LIMEService、LIMEKeyboardSwitcher）對鍵盤本體需要的操作。
 * 由 Compose 鍵盤 ComposeKeyboardView 實作（藍圖 A4-1；舊的自繪鍵盤 LIMEKeyboardBaseView 在 A4-7 移除）。
 */
interface ImeKeyboardView {
    fun setOnKeyboardActionListener(listener: OnKeyboardActionListener?)
    fun setKeyboard(keyboard: LIMEBaseKeyboard?)
    fun getKeyboard(): LIMEBaseKeyboard?
    fun setShifted(shifted: Boolean): Boolean
    fun isShifted(): Boolean
    fun hasDistinctMultitouch(): Boolean
    fun setHardwareAcceleratedDrawingEnabled(enabled: Boolean)

    /** 輸入法視窗收起或換欄位時呼叫：關掉預覽與長按小鍵盤。 */
    fun closing()

    /** 返回鍵：有開著的長按小鍵盤就關掉並回傳 true。 */
    fun handleBack(): Boolean

    /** 鍵盤本身的 View（放進輸入法視窗、顯示／隱藏、取得 window token）。 */
    fun asView(): View
}
