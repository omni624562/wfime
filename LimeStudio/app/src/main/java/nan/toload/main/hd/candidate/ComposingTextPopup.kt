/*
 * Copyright 2024 The LimeIME Open Source Project
 * 
 * Floating popup window for displaying composing text (組字),
 * similar to Gboard's inline suggestion display.
 * 
 * Uses traditional View instead of Compose to avoid ViewTreeLifecycleOwner issues.
 */

package nan.toload.main.hd.candidate

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import nan.toload.main.hd.R
import nan.toload.main.hd.global.LIMEPreferenceManager

/**
 * A floating popup window that displays the composing text (組字/字根).
 * This is shown above the keyboard/candidate bar and floats independently,
 * allowing the app content to be visible around it.
 */
class ComposingTextPopup(private val context: Context) {
    
    private var popupWindow: PopupWindow? = null
    private var textView: TextView? = null
    private var composingText: String = ""
    private var isShowing = false
    
    // Preference manager for font size
    private val mLIMEPref: LIMEPreferenceManager by lazy {
        LIMEPreferenceManager(context)
    }
    
    // Base font size from resources
    private val baseFontSizePx: Float by lazy {
        context.resources.getDimension(R.dimen.candidate_font_size)
    }
    
    init {
        createPopupWindow()
    }
    
    private fun createPopupWindow() {
        // Create styled TextView
        textView = TextView(context).apply {
            // Set text color (light blue)
            setTextColor(Color.parseColor("#4FC3F7"))
            
            // Apply font size from preference
            val fontSizeScale = mLIMEPref.fontSize
            val scaledFontSizePx = baseFontSizePx * fontSizeScale
            setTextSize(TypedValue.COMPLEX_UNIT_PX, scaledFontSizePx)
            
            // Set padding
            val paddingH = dpToPx(10)
            val paddingV = dpToPx(4)
            setPadding(paddingH, paddingV, paddingH, paddingV)
            
            // Set background with rounded corners
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#505050"))
                cornerRadius = dpToPx(4).toFloat()
            }
            
            // Single line
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
        }
        
        popupWindow = PopupWindow(
            textView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            // Make popup non-focusable so input continues to work
            isFocusable = false
            // Allow touches to pass through
            isTouchable = false
            // Transparent background for the popup window itself
            setBackgroundDrawable(null)
            // Enable drawing outside bounds if needed
            isClippingEnabled = false
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
    
    /**
     * Update the composing text and show/hide the popup accordingly.
     */
    fun updateComposingText(text: String) {
        composingText = text
        textView?.text = text
        if (text.isEmpty()) {
            hide()
        }
    }
    
    /**
     * Show the popup window anchored to the given view (typically the keyboard/candidate view).
     * @param anchor The view to anchor the popup to
     * @param xOffset Horizontal offset from anchor's left edge
     * @param yOffset Vertical offset (positive = below anchor top)
     */
    fun show(anchor: View, xOffset: Int = 8, yOffset: Int = 8) {
        if (composingText.isEmpty()) return
        
        try {
            if (!isShowing && popupWindow != null && anchor.windowToken != null) {
                // Show above the anchor view (at the top of the keyboard area)
                popupWindow?.showAtLocation(
                    anchor,
                    Gravity.BOTTOM or Gravity.START,
                    xOffset,
                    anchor.height + yOffset
                )
                isShowing = true
            } else if (isShowing) {
                // Update position if already showing
                popupWindow?.update(
                    xOffset,
                    anchor.height + yOffset,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        } catch (e: Exception) {
            // Window token might not be valid yet
            android.util.Log.w("ComposingTextPopup", "Failed to show popup: ${e.message}")
        }
    }
    
    /**
     * Hide the popup window.
     */
    fun hide() {
        if (isShowing) {
            try {
                popupWindow?.dismiss()
            } catch (e: Exception) {
                android.util.Log.w("ComposingTextPopup", "Failed to hide popup: ${e.message}")
            }
            isShowing = false
        }
    }
    
    /**
     * Check if the popup is currently visible.
     */
    fun isShowing(): Boolean = isShowing
    
    /**
     * Clean up resources when the IME is destroyed.
     */
    fun destroy() {
        hide()
        popupWindow = null
        textView = null
    }
}
