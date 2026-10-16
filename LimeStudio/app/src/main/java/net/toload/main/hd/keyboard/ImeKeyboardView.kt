package net.toload.main.hd.keyboard

import android.view.View

/**
 * 輸入法服務（LIMEService、LIMEKeyboardSwitcher）對鍵盤本體需要的操作。
 * 目前由舊的自繪鍵盤 LIMEKeyboardBaseView 實作；之後的 Compose 鍵盤實作同一個介面，
 * 服務那邊不用跟著改（藍圖 A4-1）。
 */
interface ImeKeyboardView {
    fun setOnKeyboardActionListener(listener: LIMEKeyboardBaseView.OnKeyboardActionListener?)
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
