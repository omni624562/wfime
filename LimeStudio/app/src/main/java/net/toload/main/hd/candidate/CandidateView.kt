/*
 * Copyright 2024 The LimeIME Open Source Project
 */

package net.toload.main.hd.candidate

import android.content.Context
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    private var currentPage by mutableIntStateOf(0)
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
    open fun setSuggestions(
        suggestions: List<Mapping>?,
        completions: Boolean,
        typedWordValid: Boolean,
        haveMinimalSuggestion: Boolean
    ) {
        this.suggestions = suggestions?.toList() ?: emptyList()
        this.selectedIndex = -1
        this.currentPage = 0
    }

    open fun setSuggestions(
        suggestions: List<Mapping>?,
        showNumber: Boolean,
        displaySelKey: Any
    ) {
         setSuggestions(suggestions, showNumber, true, true)
     }

    open fun setSuggestions(suggestions: List<Mapping>?, selectedIndex: Int) {
        this.suggestions = suggestions?.toList() ?: emptyList()
        this.selectedIndex = selectedIndex
        this.currentPage = 0
    }

    open fun setSuggestions(
        suggestions: List<Mapping>?,
        reset: Boolean
    ) {
        setSuggestions(suggestions, false, false, false)
    }
    
    open fun clear() {
        suggestions = emptyList()
        selectedIndex = -1
        currentPage = 0
        _composingText = ""
        _rawKeycode = ""
    }
    
    open fun setRawKeycode(keycode: String) {
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

    @Composable
    fun ToolbarRow(
        candidateFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 左側 Menu 圖標 (Apps)
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "Menu",
                tint = Color(0xFFB0BEC5),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(22.dp)
                    .height(22.dp)
            )

            // 功能圖標列
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 2. 智慧剪貼簿一鍵貼上
                IconButton(
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text
                                if (!text.isNullOrEmpty()) {
                                    mService?.currentInputConnection?.commitText(text.toString(), 1)
                                } else {
                                    Toast.makeText(context, "剪貼簿目前沒有文字內容", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "剪貼簿目前為空", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("TOOLBAR_DEBUG", "Failed to paste from clipboard: ${e.message}")
                        }
                    },
                    modifier = Modifier.width(36.dp).height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = Color.White
                    )
                }

                // 3. 表情符號面板
                IconButton(
                    onClick = {
                        mService?.toggleEmojiVisibility()
                    },
                    modifier = Modifier.width(36.dp).height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mood,
                        contentDescription = "Emoji",
                        tint = Color.White
                    )
                }

                // 4. 即時翻譯
                IconButton(
                    onClick = {
                        mService?.toggleTranslationMode(true)
                    },
                    modifier = Modifier.width(36.dp).height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate",
                        tint = Color.White
                    )
                }

                // 5. 設定主控台
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(context, net.toload.main.hd.MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("TOOLBAR_DEBUG", "Failed to start settings: ${e.message}")
                        }
                    },
                    modifier = Modifier.width(36.dp).height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
                }
            }
        }

    @Composable
    fun TranslationRow(
        translateQuery: String,
        translatedResult: String,
        candidateFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        val scrollState = rememberScrollState()
        val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
        val activeIM = mService?.activeIM
        val isDayi = activeIM?.startsWith("dayi") == true
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E)) // 極致暗黑色調
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 頂部列：如果 suggestions 為空，則顯示語言選擇列；若 suggestions 不為空，則顯示候選字列！
            if (suggestions.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回鍵
                    IconButton(
                        onClick = {
                            mService?.toggleTranslationMode(false)
                        },
                        modifier = Modifier.width(32.dp).height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 源語言膠囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F3E3E)) // 經典綠色
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "偵測語言",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 交換箭頭
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "交換語言功能已在規劃中！", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.width(32.dp).height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap",
                            tint = Color.Gray
                        )
                    }

                    // 目標語言膠囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F3E3E))
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "英文",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // 有候選字時，在頂部顯示橫向捲動的選字列！
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show raw keycode as first item (if not empty)
                    if (_rawKeycode.isNotEmpty()) {
                        RawKeycodeItem(
                            keycode = _rawKeycode,
                            fontSize = candidateFontSize,
                            onClick = {
                                mService?.commitTyped(_rawKeycode)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val pageSize = if (isPhysicalKeyboard && isDayi) 6 else suggestions.size
                    val startIndex = currentPage * pageSize
                    var endIndex = startIndex + pageSize
                    if (endIndex > suggestions.size) endIndex = suggestions.size
                    val visibleSuggestions = if (startIndex < suggestions.size) suggestions.subList(startIndex, endIndex) else emptyList()
                    val hasNextPage = endIndex < suggestions.size
                    val hasPrevPage = startIndex > 0

                    if (hasPrevPage && isDayi && isPhysicalKeyboard) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "◀", color = Color(0xFF80DEEA), fontSize = candidateFontSize)
                        }
                    }

                    visibleSuggestions.forEachIndexed { i, mapping ->
                        val actualIndex = startIndex + i
                        CandidateItem(
                            mapping = mapping,
                            index = actualIndex,
                            isSelected = actualIndex == selectedIndex,
                            fontSize = candidateFontSize,
                            onClick = {
                                mService?.pickCandidateManually(actualIndex)
                                selectedIndex = actualIndex
                            },
                            onLongClick = {
                                if (mapping.isRelatedPhraseRecord()) {
                                    mService?.removeCandidateManually(actualIndex)
                                }
                            }
                        )
                    }

                    if (hasNextPage && isDayi && isPhysicalKeyboard) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clickable { pageNext() }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "▶", color = Color(0xFF80DEEA), fontSize = candidateFontSize)
                        }
                    }
                }
            }

            // 2. 底部 Outlined 圓角翻譯輸入框
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF2A2A2A))
                    .border(1.dp, Color(0xFF00796B), RoundedCornerShape(22.dp)) // 翠綠色圓角邊框
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 文字查詢
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val translateCursorPosition by remember { LIMEService.translateCursorPositionState }
                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    val density = LocalDensity.current

                    // 1. 統一的觸控與渲染區域，填滿全部高度與可用寬度
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(translateQuery) {
                                detectTapGestures { offset ->
                                    val layoutResult = textLayoutResult
                                    if (layoutResult != null) {
                                        val clickedOffset = layoutResult.getOffsetForPosition(offset)
                                        mService?.updateTranslateCursorPosition(clickedOffset)
                                    } else {
                                        mService?.updateTranslateCursorPosition(0)
                                    }
                                }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (translateQuery.isEmpty()) {
                            Text(
                                text = "在這裡輸入要翻譯的內容",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            // 當輸入框為空時，依然渲染一個置左閃爍的翠綠游標，表示已準備好輸入！
                            val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(18.dp)
                                    .background(Color(0xFF00E676).copy(alpha = alpha))
                            )
                        } else {
                            // 渲染主要輸入文字，並取得 TextLayoutResult
                            Text(
                                text = translateQuery,
                                color = Color.White,
                                fontSize = 14.sp,
                                onTextLayout = { textLayoutResult = it },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 根據 TextLayoutResult 繪製自訂閃爍綠色游標
                            val layoutResult = textLayoutResult
                            if (layoutResult != null) {
                                val cursorIndex = translateCursorPosition.coerceIn(0, layoutResult.layoutInput.text.length)
                                val cursorRect = layoutResult.getCursorRect(cursorIndex)
                                val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "alpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = with(density) { cursorRect.left.toDp() },
                                            y = with(density) { cursorRect.top.toDp() }
                                        )
                                        .width(2.dp)
                                        .height(with(density) { cursorRect.height.toDp() })
                                        .background(Color(0xFF00E676).copy(alpha = alpha))
                                )
                            }
                        }
                    }

                    if (translatedResult.isNotEmpty()) {
                        Text(
                            text = " -> $translatedResult",
                            color = Color(0xFF80DEEA), // 亮青綠色
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // 最右側語音圖示
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = Color.Gray,
                    modifier = Modifier.width(20.dp).height(20.dp)
                )
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun CandidateRow() {
        val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
        val activeIM = mService?.activeIM
        val isDayi = activeIM?.startsWith("dayi") == true
        
        // 訂閱來自 LIMEService 的反應式即時翻譯狀態
        val isTranslationMode by remember { LIMEService.isTranslationModeState }
        val translateQuery by remember { LIMEService.translateQueryState }
        val translatedResult by remember { LIMEService.translatedResultState }
        
        // Stable Color — remembered so the object is not re-created on every recomposition
        val gboardDark = remember { Color(0xFF2B2B2B) }
        val scrollState = rememberScrollState()

        // Font size only recalculates when _fontSizeScale or density changes
        val density = LocalDensity.current
        val candidateFontSize = remember(density, _fontSizeScale, isDayi, isPhysicalKeyboard) {
            with(density) { 
                val base = if (isDayi && isPhysicalKeyboard) baseCandidateFontSizePx * 0.85f else baseCandidateFontSizePx
                (base * _fontSizeScale).toSp() 
            }
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
        // Determine base height based on input method and physical keyboard state
        val isDayiPhysical = isDayi && isPhysicalKeyboard
        val baseHeight = if (isDayiPhysical) 36 else 48
        
        // 即時翻譯模式時將高度拓寬為 96.dp，以容納雙層科技感控制面板
        val heightDp = if (isTranslationMode) 96.dp else (baseHeight * _fontSizeScale).coerceIn(32f, 60f).dp
        
        // Use BoxWithConstraints to handle infinite width constraints gracefully
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .background(gboardDark)
        ) {
            // Only render the scrollable content if we have a finite maximum width.
            // This prevents the "infinity maximum width constraints" crash when measured with MeasureSpec.UNSPECIFIED.
            if (constraints.maxWidth != androidx.compose.ui.unit.Constraints.Infinity) {
                if (isTranslationMode) {
                    TranslationRow(translateQuery, translatedResult, candidateFontSize)
                } else {
                    val isToolbarMode = suggestions.isEmpty() && _composingText.isEmpty() && _rawKeycode.isEmpty()
                    if (isToolbarMode) {
                        ToolbarRow(candidateFontSize)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .horizontalScroll(scrollState)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        // Show raw keycode as first item (if not empty)
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

                        val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
                        val activeIM = mService?.activeIM
                        val isDayi = activeIM?.startsWith("dayi") == true
                        val pageSize = if (isPhysicalKeyboard && isDayi) 6 else suggestions.size
                        
                        val startIndex = currentPage * pageSize
                        var endIndex = startIndex + pageSize
                        if (endIndex > suggestions.size) endIndex = suggestions.size
                        val visibleSuggestions = if (startIndex < suggestions.size) suggestions.subList(startIndex, endIndex) else emptyList()
                        val hasNextPage = endIndex < suggestions.size
                        val hasPrevPage = startIndex > 0

                        if (hasPrevPage && isDayi && isPhysicalKeyboard) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "◀", color = Color(0xFF80DEEA), fontSize = candidateFontSize)
                            }
                        }
                        
                        visibleSuggestions.forEachIndexed { i, mapping ->
                            val actualIndex = startIndex + i
                            CandidateItem(
                                mapping = mapping,
                                index = actualIndex,
                                isSelected = actualIndex == selectedIndex,
                                fontSize = candidateFontSize,
                                onClick = {
                                    mService?.pickCandidateManually(actualIndex)
                                    selectedIndex = actualIndex
                                },
                                onLongClick = {
                                    if (mapping.isRelatedPhraseRecord()) {
                                        mService?.removeCandidateManually(actualIndex)
                                    }
                                }
                            )
                        }

                        if (hasNextPage && isDayi && isPhysicalKeyboard) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clickable { pageNext() }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "▶", color = Color(0xFF80DEEA), fontSize = candidateFontSize)
                            }
                        }
                    }
                }
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

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun CandidateItem(
        mapping: Mapping,
        index: Int,
        isSelected: Boolean,
        fontSize: androidx.compose.ui.unit.TextUnit,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        // Gboard-style: light text on dark background
        val textColor = if (isSelected) Color(0xFF4FC3F7) else Color.White  // Light blue when selected
        val fontWeight = if (mapping.isHighLighted == true) FontWeight.Bold else FontWeight.Normal

        val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
        val activeIM = mService?.activeIM
        
        var displayText = mapping.word ?: ""
        if (isPhysicalKeyboard && !mapping.isEmojiRecord() && !mapping.isComposingCodeRecord()) {
            if (activeIM?.startsWith("dayi") == true) {
                val prefix = when (index % 6) {
                    0 -> "\u2423. "
                    1 -> "'. "
                    2 -> "[. "
                    3 -> "]. "
                    4 -> "-. "
                    5 -> "\\. "
                    else -> ""
                }
                displayText = prefix + displayText
            } else {
                val prefix = if (index < 9) "${index + 1}. " else if (index == 9) "0. " else ""
                displayText = prefix + displayText
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
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
                .padding(horizontal = 12.dp),
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
    
    open fun selectNext() {
        if (suggestions.isNotEmpty()) {
            selectedIndex++
            if (selectedIndex >= suggestions.size) {
                selectedIndex = 0
                currentPage = 0
            } else {
                val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
                val activeIM = mService?.activeIM
                val isDayi = activeIM?.startsWith("dayi") == true
                val pageSize = if (isPhysicalKeyboard && isDayi) 6 else suggestions.size
                if (pageSize > 0 && selectedIndex >= (currentPage + 1) * pageSize) {
                    currentPage++
                }
            }
        }
    }
    
    open fun selectPrev() {
         if (suggestions.isNotEmpty()) {
            selectedIndex--
            if (selectedIndex < 0) {
                selectedIndex = suggestions.size - 1
                val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
                val activeIM = mService?.activeIM
                val isDayi = activeIM?.startsWith("dayi") == true
                val pageSize = if (isPhysicalKeyboard && isDayi) 6 else suggestions.size
                if (pageSize > 0) {
                    currentPage = selectedIndex / pageSize
                }
            } else {
                val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
                val activeIM = mService?.activeIM
                val isDayi = activeIM?.startsWith("dayi") == true
                val pageSize = if (isPhysicalKeyboard && isDayi) 6 else suggestions.size
                if (pageSize > 0 && selectedIndex < currentPage * pageSize) {
                    currentPage--
                }
            }
        }
    }
    
    open fun selectNextRow() {
         // Prototype: equivalent to next page or jump 10?
         selectNext()
    }
    
    open fun selectPrevRow() {
        selectPrev()
    }

    fun getCurrentPageOffset(): Int {
        val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
        val activeIM = mService?.activeIM
        val isDayi = activeIM?.startsWith("dayi") == true
        val pageSize = if (isPhysicalKeyboard && isDayi) 6 else suggestions.size
        if (pageSize == 0) return 0
        return currentPage * pageSize
    }

    fun pageNext() {
        val isPhysicalKeyboard = mService?.hasPhysicalKeyPressed == true
        val activeIM = mService?.activeIM
        val isDayi = activeIM?.startsWith("dayi") == true
        val pageSize = if (isPhysicalKeyboard && isDayi) 6 else suggestions.size
        if (pageSize > 0 && (currentPage + 1) * pageSize < suggestions.size) {
            currentPage++
            selectedIndex = -1
        }
    }

    fun pagePrev() {
        if (currentPage > 0) {
            currentPage--
            selectedIndex = -1
        }
    }
    
    fun retrieveSelectedIndex(): Int {
        return selectedIndex
    }

    open fun takeSelectedSuggestion(): Boolean {
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
