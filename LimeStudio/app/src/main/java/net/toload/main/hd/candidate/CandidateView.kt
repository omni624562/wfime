/*
 * Copyright 2024 The LimeIME Open Source Project
 */

package net.toload.main.hd.candidate

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.toload.main.hd.LIMEService
import net.toload.main.hd.R
import net.toload.main.hd.data.Mapping
import net.toload.main.hd.global.LIMEPreferenceManager

open class CandidateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.LIMECandidateView
) : FrameLayout(context, attrs, defStyle) {

    private var mService: LIMEService? = null
    private var suggestions by mutableStateOf<List<Mapping>>(emptyList())
    private var selectedIndex by mutableIntStateOf(-1)
    private var _composingText by mutableStateOf("")
    private var _rawKeycode by mutableStateOf("") // Raw keycode like "nh1"
    // Cached font size scale — read from SharedPreferences once, updated via updateFontSize()
    // Stored as Compose state so CandidateRow recomposes automatically when it changes
    private var _fontSizeScale by mutableFloatStateOf(1f)
    
    // Custom lifecycle and recomposer for Compose support in InputMethodService
    private val lifecycleOwner = IMELifecycleOwner()
    private val coroutineScope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
    private var recomposer: Recomposer? = null
    
    // Compatibility Fields exposed to CandidateViewContainer (Java)
    @JvmField var mColorBackground: Int = 0
    @JvmField var mColorNormalText: Int = 0
    @JvmField var mDrawableExpandButton: Drawable? = null
    @JvmField var mDrawableSuggestHighlight: Drawable? = null
    @JvmField var mDrawableSymbolInput: Drawable? = null
    @JvmField var mDrawableCloseButton: Drawable? = null
    
    // Internal fields for theme
    private var mColorComposingText: Int = 0
    private var mColorComposingBackground: Int = 0
    private var mColorNormalTextHighlight: Int = 0
    private var mColorComposingCode: Int = 0
    private var mColorComposingCodeHighlight: Int = 0 
    private var mColorSpacer: Int = 0
    private var mColorSelKey: Int = 0
    private var mColorSelKeyShifted: Int = 0
    
    private var embeddedComposingView: TextView? = null
    private var composeView: ComposeView? = null
    
    // Preference manager for font size setting
    private val mLIMEPref: LIMEPreferenceManager by lazy {
        LIMEPreferenceManager(context)
    }
    
    // Base font size from resources (18sp)
    private val baseCandidateFontSizePx: Float by lazy {
        context.resources.getDimension(R.dimen.candidate_font_size)
    }

    init {
        // Read font size preference once at construction; refreshed by updateFontSize()
        _fontSizeScale = mLIMEPref.fontSize

        // Load styles from R.styleable.LIMECandidateView
         val a = context.theme.obtainStyledAttributes(
            attrs, R.styleable.LIMECandidateView, defStyle, R.style.LIMECandidateView
        )

        try {
            mDrawableSuggestHighlight = a.getDrawable(R.styleable.LIMECandidateView_suggestHighlight)
            mDrawableSymbolInput = a.getDrawable(R.styleable.LIMECandidateView_voiceInputIcon)
            mDrawableExpandButton = a.getDrawable(R.styleable.LIMECandidateView_ExpandButtonIcon)
            mDrawableCloseButton = a.getDrawable(R.styleable.LIMECandidateView_closeButtonIcon)
            
            mColorBackground = a.getColor(
                R.styleable.LIMECandidateView_candidateBackground,
                ContextCompat.getColor(context, R.color.third_background_light)
            )
            mColorComposingText = a.getColor(
                R.styleable.LIMECandidateView_composingTextColor,
                ContextCompat.getColor(context, R.color.second_foreground_light)
            )
             mColorComposingBackground = a.getColor(
                 R.styleable.LIMECandidateView_composingBackgroundColor,
                 ContextCompat.getColor(context, R.color.composing_background_light)
             )
            mColorNormalText = a.getColor(
                R.styleable.LIMECandidateView_candidateNormalTextColor,
                ContextCompat.getColor(context, R.color.foreground_light)
            )
            mColorNormalTextHighlight = a.getColor(
                R.styleable.LIMECandidateView_candidateNormalTextHighlightColor,
                ContextCompat.getColor(context, R.color.foreground_light)
            )
            // ... other colors
            
        } finally {
            a.recycle()
        }

        // Create custom Recomposer that doesn't rely on ViewTreeLifecycleOwner
        recomposer = Recomposer(AndroidUiDispatcher.Main)
        coroutineScope.launch {
            recomposer?.runRecomposeAndApplyChanges()
        }
        
        try {
            composeView = ComposeView(context).apply {
                // Use WRAP_CONTENT for height to respect minHeight from XML
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                // Set our custom recomposer directly - bypasses ViewTree lookup
                setParentCompositionContext(recomposer)
                // Also set lifecycle owners on this view for any other components that need it
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setContent {
                    CandidateRow()
                }
            }
            composeView?.let { 
                addView(it)
                Log.d("CANDIDATE_VIEW", "Successfully added composeView to CandidateView")
            }
        } catch (e: Exception) {
            Log.e("CANDIDATE_VIEW", "FAILED to initialize ComposeView in CandidateView: ${e.message}", e)
        }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    
    override fun onDetachedFromWindow() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onDetachedFromWindow()
    }
    
    fun destroy() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        recomposer?.cancel()
        coroutineScope.cancel()
    }

    fun setService(service: LIMEService) {
        mService = service
    }

    fun setSuggestions(
        suggestions: List<Mapping>?,
        completions: Boolean,
        typedWordValid: Boolean,
        haveMinimalSuggestion: Boolean
    ) {
        this.suggestions = suggestions?.toList() ?: emptyList()
        this.selectedIndex = -1
    }

    fun setSuggestions(
        suggestions: List<Mapping>?,
        showNumber: Boolean,
        displaySelKey: Any
    ) {
         setSuggestions(suggestions, showNumber, true, true)
    }

    fun setSuggestions(suggestions: List<Mapping>?, selectedIndex: Int) {
        this.suggestions = suggestions?.toList() ?: emptyList()
        this.selectedIndex = selectedIndex
    }

    fun setSuggestions(
        suggestions: List<Mapping>?,
        reset: Boolean
    ) {
        setSuggestions(suggestions, false, false, false)
    }
    
    fun clear() {
        suggestions = emptyList()
        selectedIndex = -1
        _composingText = ""
        _rawKeycode = ""
    }
    
    fun setRawKeycode(keycode: String) {
        _rawKeycode = keycode
    }
    
    fun setEmbeddedComposingView(view: TextView?) {
        this.embeddedComposingView = view
    }
    
    override fun computeHorizontalScrollRange(): Int {
        // Rough estimate to satisfy Container logic
        // If list is empty, return 0. If not, return something larger than width to force button show?
        // Actually Container logic: availableWidth < neededWidth -> show button.
        // For prototype, let's just assume if we have suggestions, we might need scrolling.
        return if (suggestions.isNotEmpty()) 10000 else 0
    }
    
    fun isCandidateExpanded(): Boolean {
        return false // Prototype does not support expanded view yet
    }
    
    fun isEmpty(): Boolean {
        return suggestions.isEmpty()
    }
    
    fun showCandidatePopup() {
        // Prototype: just log or do nothing. Original showed a popup window.
        // Should we implement the popup? 
        // mService?.doVibrateSound(0)
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun CandidateRow() {
        // Stable Color — remembered so the object is not re-created on every recomposition
        val gboardDark = remember { Color(0xFF2B2B2B) }
        val scrollState = rememberScrollState()

        // Font size only recalculates when _fontSizeScale or density changes
        val density = LocalDensity.current
        val candidateFontSize = remember(density, _fontSizeScale) {
            with(density) { (baseCandidateFontSizePx * _fontSizeScale).toSp() }
        }
        
        // Reset scroll position when suggestions change
        LaunchedEffect(suggestions) {
            scrollState.scrollTo(0)
        }
        
        // Check if we're at the end of scroll
        val isAtEnd = remember {
            derivedStateOf {
                scrollState.value >= scrollState.maxValue
            }
        }
        
        // Use BoxWithConstraints to handle infinite width constraints gracefully
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp) // Fixed height to prevent filling screen
                .background(gboardDark)
        ) {
            // Only render the scrollable content if we have a finite maximum width.
            // This prevents the "infinity maximum width constraints" crash when measured with MeasureSpec.UNSPECIFIED.
            if (constraints.maxWidth != androidx.compose.ui.unit.Constraints.Infinity) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show raw keycode as first item (if not empty)
                    if (_rawKeycode.isNotEmpty()) {
                        RawKeycodeItem(
                            keycode = _rawKeycode,
                            fontSize = candidateFontSize,
                            onClick = {
                                // Input raw keycode directly
                                mService?.commitTyped(_rawKeycode)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    suggestions.forEachIndexed { index, mapping ->
                        CandidateItem(
                            mapping = mapping,
                            isSelected = index == selectedIndex,
                            fontSize = candidateFontSize,
                            onClick = {
                                mService?.pickCandidateManually(index)
                                selectedIndex = index
                            },
                            onLongClick = {
                                if (mapping.isRelatedPhraseRecord()) {
                                    mService?.removeCandidateManually(index)
                                }
                            }
                        )
                    }
                }
                
                // Fade edge indicator on right side when more content is available
                if (suggestions.isNotEmpty() && !isAtEnd.value) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(24.dp)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        gboardDark
                                    )
                                )
                            )
                    )
                }
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun CandidateItem(
        mapping: Mapping,
        isSelected: Boolean,
        fontSize: androidx.compose.ui.unit.TextUnit,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        // Gboard-style: light text on dark background
        val textColor = if (isSelected) Color(0xFF4FC3F7) else Color.White  // Light blue when selected
        val fontWeight = if (mapping.isHighLighted == true) FontWeight.Bold else FontWeight.Normal

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mapping.word ?: "",
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1
            )
        }
    }

    @Composable
    fun RawKeycodeItem(
        keycode: String,
        fontSize: androidx.compose.ui.unit.TextUnit,
        onClick: () -> Unit
    ) {
        // Distinct style: bordered box with different background
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onClick)
                .background(
                    color = Color(0xFF3A3A3A), // Slightly lighter than gboardDark
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 12.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = keycode,
                color = Color(0xFF80DEEA), // Cyan color to distinguish from candidates
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }

    // Required Stub methods
    override fun setScrollX(x: Int) {}
    fun updateFontSize() {
        _fontSizeScale = mLIMEPref.fontSize
    }
    fun setTouchX(x: Int) {}
    fun takeSuggestionAt(x: Int) {}
    fun onTouchReal(event: android.view.MotionEvent): Boolean = false
    
    fun setComposingText(text: String) {
        _composingText = text
    }
    fun setTransparentCandidateView(transparent: Boolean) {}
    fun startSymbolInput() {}
    fun forceHide() {
        clear()
    }
    
    fun selectNext() {
        if (suggestions.isNotEmpty()) {
            selectedIndex = (selectedIndex + 1).coerceAtMost(suggestions.size - 1)
        }
    }
    
    fun selectPrev() {
         if (suggestions.isNotEmpty()) {
            selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
        }
    }
    
    fun selectNextRow() {
         // Prototype: equivalent to next page or jump 10?
         selectNext()
    }
    
    fun selectPrevRow() {
        selectPrev()
    }
    
    fun takeSelectedSuggestion(): Boolean {
        if (selectedIndex >= 0 && selectedIndex < suggestions.size) {
            mService?.pickCandidateManually(selectedIndex)
            return true
        }
        return false
    }
}

/**
 * Custom LifecycleOwner and SavedStateRegistryOwner for Compose support in InputMethodService.
 * InputMethodService doesn't implement LifecycleOwner, so we need to provide our own.
 */
private class IMELifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val mViewModelStore = ViewModelStore()
    
    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }
    
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = mViewModelStore
    
    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
