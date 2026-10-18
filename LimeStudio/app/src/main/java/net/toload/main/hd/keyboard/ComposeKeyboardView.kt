package net.toload.main.hd.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.util.AttributeSet
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.toload.main.hd.ComposeLifecycleOwner
import net.toload.main.hd.R
import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key

/**
 * Compose 版螢幕鍵盤（藍圖 A4-3～A4-7）。舊的 LIMEKeyboardView 已移除（A4-7），一律用這個鍵盤；
 * 版面寫在 inputcandidate.xml（id 為 keyboard）。
 *
 * 鍵盤模型（LIMEBaseKeyboard）、觸控判斷（PointerDispatcher、PointerTracker）、畫鍵（KeyPainter）
 * 與舊鍵盤共用：鍵先畫進一張點陣圖（同舊鍵盤），再由 Compose 的 Canvas 貼上，外觀與舊鍵盤相同。
 * 長按小鍵盤（MiniKeyboardPopup）與滑動手勢（SwipeGestures）也與舊鍵盤共用；長按開出的小鍵盤本身
 * 也是這個類別（keyboard_popup.xml，A4-7②），由 setupAsMiniKeyboard 切成小鍵盤模式。
 * TalkBack：每個鍵在畫面上的位置放一個無障礙節點，說出鍵名，點兩下（或抬起輸入）送出該鍵（A4-5）。
 */
class ComposeKeyboardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs), ImeKeyboardView, PointerTracker.UIProxy {

    @JvmField internal val painter = KeyPainter()
    @JvmField internal var keyHysteresisDistance = 0f
    @JvmField internal var verticalCorrection = 0f
    private var popupLayout = 0
    private var previewLayout = 0
    private var backgroundDimAmount = 0f

    private var keyboard: LIMEBaseKeyboard? = null
    private var keys: Array<Key>? = null
    private var keyDetector: KeyDetector = ProximityKeyDetector()
    // 長按開出的小鍵盤（setupAsMiniKeyboard）：沒有滑動手勢、不往下延伸蓋導覽列、長按只開小鍵盤
    private var isMiniKeyboard = false
    private var listener: OnKeyboardActionListener? = null
    private val trackers = ArrayList<PointerTracker>()
    private val timers = Timers()
    private val dispatcher = PointerDispatcher(timers) { id -> getPointerTracker(id) }
    private val distinctMultitouch = context.packageManager
        .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)
    private val keyRepeatInterval = resources.getInteger(R.integer.config_key_repeat_interval)
    private val keyHeight = resources.getDimensionPixelSize(R.dimen.key_height)
    private var oldPointerCount = 1

    private var bottomInset = 0
    private val gestureExclusionRect = Rect()
    private val gestureExclusionRects = listOf(gestureExclusionRect)

    // 鍵先畫進點陣圖；frame 改變時 Compose 重貼
    private var buffer: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    private var redrawAll = true
    private var frame by mutableIntStateOf(0)

    // TalkBack 用：目前的鍵與 Shift 狀態（換鍵盤或 Shift 時才重組無障礙節點）
    private var a11yKeys by mutableStateOf<List<Key>>(emptyList())
    private var a11yVersion by mutableIntStateOf(0)

    private val lifecycleOwner = ComposeLifecycleOwner().apply {
        performRestore(null)
        handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        handleLifecycleEvent(Lifecycle.Event.ON_START)
    }
    private var recomposer: Recomposer? = null
    private var recomposeScope: CoroutineScope? = null
    private val composeView = ComposeView(context)
    private val miniPopup: MiniKeyboardPopup
    private val swipeGestures: SwipeGestures

    init {
        readStyles(attrs)
        painter.finishStyle(0)
        miniPopup = MiniKeyboardPopup(context, object : MiniKeyboardPopup.Host {
            override fun view(): View = this@ComposeKeyboardView
            override fun getKeyboard(): LIMEBaseKeyboard? = this@ComposeKeyboardView.keyboard
            override fun getOnKeyboardActionListener(): OnKeyboardActionListener? =
                this@ComposeKeyboardView.listener
            override fun isShifted(): Boolean = this@ComposeKeyboardView.isShifted()
            // 同舊鍵盤：主題有按鍵預覽版面就算開啟
            override fun isPreviewEnabled(): Boolean = previewLayout != 0
            override fun invalidateAllKeys() = this@ComposeKeyboardView.invalidateAllKeys()
        }, popupLayout)
        swipeGestures = SwipeGestures(context, object : SwipeGestures.Host {
            override fun view(): View = this@ComposeKeyboardView
            override fun swipeLeft() { listener?.swipeLeft() }
            override fun swipeRight() { listener?.swipeRight() }
            override fun swipeUp() { listener?.swipeUp() }
            override fun swipeDown() { listener?.swipeDown() }
        })

        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        composeView.setContent {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION") frame
                    val bitmap = drawBuffer()
                    if (bitmap != null) drawImage(bitmap.asImageBitmap())
                }
                @Suppress("UNUSED_EXPRESSION") a11yVersion
                for (key in a11yKeys) {
                    val description = keyDescription(key)
                    Box(
                        Modifier
                            .offset { IntOffset(key.x + paddingLeft, key.y + paddingTop) }
                            .layout { measurable, _ ->
                                val placeable = measurable.measure(Constraints.fixed(key.width, key.height))
                                layout(key.width, key.height) { placeable.place(0, 0) }
                            }
                            .semantics {
                                contentDescription = description
                                role = Role.Button
                                onClick {
                                    typeKeyForAccessibility(key)
                                    true
                                }
                            }
                    )
                }
            }
        }
        addView(composeView)
    }

    // 刻意沿用舊鍵盤的 LIMEKeyboardBaseView 樣式屬性，兩個鍵盤讀同一套主題
    @SuppressLint("CustomViewStyleable")
    private fun readStyles(attrs: AttributeSet?) {
        if (attrs != null && attrs.styleAttribute != 0) {
            // 版面指定了樣式（小鍵盤 keyboard_popup.xml 的 ?attr/LIMEPopupKeyboardLayout）：同舊 LIMEKeyboardBaseView 建構子，
            // 版面屬性 > 版面樣式 > 主題的 LIMEKeyboardBaseView 樣式；背景與內距已由 View 建構子從同一個樣式讀到
            applyStyle(context.obtainStyledAttributes(attrs, R.styleable.LIMEKeyboardBaseView,
                R.attr.LIMEKeyboardBaseView, R.style.LIMEBaseKeyboard))
            return
        }
        // 主鍵盤：主題的 LIMEKeyboardBaseView 樣式，再疊上舊 input.xml（A4-7 已刪）原本的 style="?attr/LIMEKeyboardLayout"
        applyStyle(context.obtainStyledAttributes(null, R.styleable.LIMEKeyboardBaseView,
            R.attr.LIMEKeyboardBaseView, R.style.LIMEBaseKeyboard))
        val layoutStyle = TypedValue()
        if (context.theme.resolveAttribute(R.attr.LIMEKeyboardLayout, layoutStyle, true) && layoutStyle.resourceId != 0) {
            applyStyle(context.obtainStyledAttributes(layoutStyle.resourceId, R.styleable.LIMEKeyboardBaseView))
            val bg = context.obtainStyledAttributes(layoutStyle.resourceId, intArrayOf(android.R.attr.background))
            background = bg.getDrawable(0)
            bg.recycle()
        }
    }

    private fun applyStyle(a: TypedArray) {
        for (i in 0 until a.indexCount) {
            val attr = a.getIndex(i)
            if (painter.applyStyleAttribute(a, attr)) continue
            when (attr) {
                R.styleable.LIMEKeyboardBaseView_keyHysteresisDistance ->
                    keyHysteresisDistance = a.getDimensionPixelOffset(attr, 0).toFloat()
                R.styleable.LIMEKeyboardBaseView_verticalCorrection ->
                    verticalCorrection = a.getDimensionPixelOffset(attr, 0).toFloat()
                R.styleable.LIMEKeyboardBaseView_popupLayout -> popupLayout = a.getResourceId(attr, 0)
                R.styleable.LIMEKeyboardBaseView_keyPreviewLayout -> previewLayout = a.getResourceId(attr, 0)
                R.styleable.LIMEKeyboardBaseView_backgroundDimAmount -> backgroundDimAmount = a.getFloat(attr, 0.5f)
            }
        }
        a.recycle()
    }

    // ---- ImeKeyboardView ----

    override fun setOnKeyboardActionListener(listener: OnKeyboardActionListener?) {
        this.listener = listener
        for (tracker in trackers) tracker.setOnKeyboardActionListener(listener)
    }

    override fun setKeyboard(keyboard: LIMEBaseKeyboard?) {
        timers.cancelKeyTimers()
        this.keyboard = keyboard
        keys = keyDetector.setKeyboard(keyboard, -paddingLeft.toFloat(), -paddingTop + verticalCorrection)
        for (tracker in trackers) tracker.setKeyboard(keys, keyHysteresisDistance)
        requestLayout()
        invalidateAllKeys()
        computeProximityThreshold()
        miniPopup.clearCache()
        a11yKeys = keys?.toList() ?: emptyList()
        a11yVersion++
        painter.mLabelSplitCache.clear()
    }

    override fun getKeyboard(): LIMEBaseKeyboard? = keyboard

    override fun setShifted(shifted: Boolean): Boolean {
        if (keyboard?.setShifted(shifted) == true) {
            invalidateAllKeys()
            a11yVersion++
            return true
        }
        return false
    }

    override fun isShifted(): Boolean = keyboard?.isShifted == true

    override fun hasDistinctMultitouch(): Boolean = distinctMultitouch

    override fun setHardwareAcceleratedDrawingEnabled(enabled: Boolean) {
        // 鍵一律畫在點陣圖上，不需切換
    }

    override fun closing() {
        timers.cancelKeyTimers()
        miniPopup.dismissPopupKeyboard()
        buffer = null
        bufferCanvas = null
        miniPopup.clearCache()
    }

    override fun handleBack(): Boolean = miniPopup.handleBack()

    /**
     * 當長按開出的小鍵盤用（MiniKeyboardPopup 呼叫，要在 setKeyboard 之前）：換成小鍵盤的鍵偵測
     * （MiniKeyboardKeyDetector）、關掉滑動手勢、不往下延伸蓋導覽列
     * （同舊鍵盤把 mKeyDetector 換掉、mSwipeGestures 設 null）。
     */
    fun setupAsMiniKeyboard(slideAllowance: Float) {
        keyDetector = MiniKeyboardKeyDetector(slideAllowance)
        isMiniKeyboard = true
        bottomInset = 0
    }

    override fun asView(): View = this

    // ---- PointerTracker.UIProxy ----

    override fun invalidateKey(key: Key?) {
        if (key == null) return
        val canvas = bufferCanvas
        if (miniPopup.isMiniKeyboardOn()) {
            // 小鍵盤開著時整張重畫（含壓暗層，同舊鍵盤）
            redrawAll = true
        } else if (canvas != null && !redrawAll) {
            // 只重畫這一鍵的區域（同舊鍵盤的髒矩形重繪）
            canvas.save()
            canvas.clipRect(key.x + paddingLeft, key.y + paddingTop,
                key.x + key.width + paddingLeft, key.y + key.height + paddingTop)
            canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
            keyboard?.let { painter.drawKey(canvas, key, it, paddingLeft, paddingTop, ::adjustCase) }
            canvas.restore()
        }
        frame++
    }

    override fun showPreview(keyIndex: Int, tracker: PointerTracker?) {}

    // ---- TalkBack ----

    /** TalkBack 念出的鍵名：功能鍵用固定名稱，其他鍵念鍵上的字（多行標籤以空白隔開）。 */
    fun keyDescription(key: Key): String {
        val label = key.label?.toString()?.replace('\n', ' ')?.trim().orEmpty()
        val res = resources
        return when (key.codes[0]) {
            LIMEBaseKeyboard.KEYCODE_SPACE -> res.getString(R.string.key_a11y_space)
            LIMEBaseKeyboard.KEYCODE_DELETE -> res.getString(R.string.key_a11y_delete)
            LIMEBaseKeyboard.KEYCODE_SHIFT ->
                res.getString(if (isShifted()) R.string.key_a11y_shift_on else R.string.key_a11y_shift)
            LIMEBaseKeyboard.KEYCODE_MODE_CHANGE ->
                res.getString(if (label == "ABC") R.string.key_a11y_letters else R.string.key_a11y_symbols)
            KEYCODE_MORE_SYMBOLS -> res.getString(R.string.key_a11y_more_symbols)
            KEYCODE_NEXT_INPUT_METHOD -> res.getString(R.string.key_a11y_switch_im)
            KEYCODE_TO_ENGLISH -> res.getString(R.string.key_a11y_to_english)
            KEYCODE_TO_CHINESE -> res.getString(R.string.key_a11y_to_chinese)
            LIMEBaseKeyboard.KEYCODE_ENTER, LIMEBaseKeyboard.KEYCODE_DONE ->
                label.ifEmpty { res.getString(R.string.key_a11y_enter) }
            else -> when {
                label.isNotEmpty() -> label
                key.text != null -> key.text.toString()
                key.codes[0] > 0 -> String(Character.toChars(key.codes[0]))
                else -> ""
            }
        }
    }

    /** TalkBack 點兩下：與手指點一下送出的事件相同（PointerTracker 的按下、送鍵、放開）。 */
    private fun typeKeyForAccessibility(key: Key) {
        val l = listener ?: return
        val code = key.codes[0]
        l.onPress(code)
        if (key.text != null) {
            l.onText(key.text)
            l.onRelease(0)
        } else {
            l.onKey(code, key.codes, OnKeyboardActionListener.NOT_A_TOUCH_COORDINATE, OnKeyboardActionListener.NOT_A_TOUCH_COORDINATE)
            l.onRelease(code)
        }
    }

    // ---- 畫鍵 ----

    private fun invalidateAllKeys() {
        redrawAll = true
        frame++
    }

    private fun adjustCase(label: CharSequence?): CharSequence? {
        val kb = keyboard ?: return label
        if (kb.isShifted && label != null && label.length <= 3 && Character.isLowerCase(label[0])) {
            return label.toString().uppercase(java.util.Locale.ROOT)
        }
        return label
    }

    /** 需要時重畫整個鍵盤到點陣圖，回傳要貼上的點陣圖。 */
    private fun drawBuffer(): Bitmap? {
        val kb = keyboard ?: return null
        if (width <= 0 || height <= 0) return null
        var bmp = buffer
        if (bmp == null || bmp.width != width || bmp.height != height) {
            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            buffer = bmp
            bufferCanvas = Canvas(bmp)
            redrawAll = true
        }
        if (redrawAll) {
            val canvas = bufferCanvas!!
            canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR)
            for (key in keys ?: emptyArray()) {
                painter.drawKey(canvas, key, kb, paddingLeft, paddingTop, ::adjustCase)
            }
            // 小鍵盤開著時蓋一層壓暗
            if (miniPopup.isMiniKeyboardOn()) {
                painter.mPaint.color = (backgroundDimAmount * 0xFF).toInt() shl 24
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), painter.mPaint)
            }
            redrawAll = false
        }
        return bmp
    }

    private fun computeProximityThreshold() {
        val keys = keys ?: return
        if (keyboard == null) return
        val verticalGap = resources.getDimension(R.dimen.key_bottom_gap).toInt()
        var dimensionSum = 0
        for (key in keys) dimensionSum += minOf(key.width, key.height + verticalGap) + key.gap
        if (dimensionSum < 0 || keys.isEmpty()) return
        keyDetector.setProximityThreshold((dimensionSum * 1.4f / keys.size).toInt())
    }

    // ---- 版面 ----

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val kb = keyboard
        val w: Int
        val h: Int
        if (kb == null) {
            w = paddingLeft + paddingRight
            h = paddingTop + paddingBottom
        } else {
            var width = kb.minWidth + paddingLeft + paddingRight
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) width = MeasureSpec.getSize(widthMeasureSpec)
            w = width
            h = kb.height + paddingTop + paddingBottom
        }
        // 往下延伸蓋住導覽列／手勢區（同舊鍵盤）
        val height = h + bottomInset
        setMeasuredDimension(w, height)
        // ComposeView 要接上視窗才能量（需要 Recomposer）；接上後會再量一次
        if (composeView.isAttachedToWindow) {
            composeView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // 點陣圖蓋滿整個鍵盤（含內距），鍵的位置已加上內距
        composeView.layout(0, 0, right - left, bottom - top)
        if (bottomInset > 0) {
            gestureExclusionRect.set(0, height - bottomInset, width, height)
            systemGestureExclusionRects = gestureExclusionRects
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (isMiniKeyboard) return super.onApplyWindowInsets(insets)
        val navBarBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        val gestureBottom = insets.getInsets(WindowInsets.Type.systemGestures()).bottom
        bottomInset = maxOf(navBarBottom, gestureBottom)
        requestLayout()
        return super.onApplyWindowInsets(insets)
    }

    override fun onAttachedToWindow() {
        // 每次接上視窗建立自己的 Recomposer（輸入法服務沒有 Activity 的 lifecycle），離開視窗時結束
        val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)
        val r = Recomposer(AndroidUiDispatcher.Main)
        scope.launch { r.runRecomposeAndApplyChanges() }
        recomposer = r
        recomposeScope = scope
        composeView.setParentCompositionContext(r)
        super.onAttachedToWindow()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        requestApplyInsets()
    }

    override fun onDetachedFromWindow() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onDetachedFromWindow()
        closing()
        recomposer?.cancel()
        recomposeScope?.cancel()
        recomposer = null
        recomposeScope = null
    }

    // ---- 觸控 ----

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    // TalkBack 不經過這裡：每個鍵有自己的無障礙節點與點擊動作（見 setContent），所以不需要 performClick
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(me: MotionEvent): Boolean {
        if (keyboard == null) return true
        // 小鍵盤是 LIMEBaseKeyboard，沒有空白鍵拖曳
        val kb = keyboard as? LIMEKeyboard ?: return dispatchPointers(me)
        // 同 LIMEKeyboardView：按下時重設空白鍵拖曳；放開時空白鍵拖曳換輸入法
        if (me.action == MotionEvent.ACTION_DOWN) kb.keyReleased()
        if (me.action == MotionEvent.ACTION_UP) {
            val direction = kb.spaceDragDirection
            if (direction != 0) {
                listener?.onKey(if (direction == 1) KEYCODE_NEXT_IM else KEYCODE_PREV_IM,
                    null, 0, 0)
                me.action = MotionEvent.ACTION_CANCEL
                kb.keyReleased()
            }
        }
        return dispatchPointers(me)
    }

    /** 同 LIMEKeyboardBaseView.onTouchEvent。 */
    private fun dispatchPointers(me: MotionEvent): Boolean {
        val action = me.actionMasked
        val pointerCount = me.pointerCount
        val oldCount = oldPointerCount
        oldPointerCount = pointerCount
        if (!distinctMultitouch && pointerCount > 1 && oldCount > 1) return true

        // 滑動手勢（小鍵盤開著時不判斷；本身是小鍵盤時沒有滑動手勢）
        if (!isMiniKeyboard) swipeGestures.addMovement(me)
        if (!isMiniKeyboard && !miniPopup.isMiniKeyboardOn() && swipeGestures.onTouchEvent(me)) {
            timers.cancelKeyTimers()
            return true
        }
        // 只有一個鍵的小鍵盤開著時，觸控轉給小鍵盤
        if (miniPopup.forwardTouchEvent(me)) return true

        val index = me.actionIndex
        val ids = IntArray(pointerCount) { me.getPointerId(it) }
        val xs = IntArray(pointerCount) { me.getX(it).toInt() }
        val ys = IntArray(pointerCount) { me.getY(it).toInt() }
        return dispatcher.dispatch(action, pointerCount, oldCount, distinctMultitouch,
            me.getPointerId(index), me.getX(index).toInt(), me.getY(index).toInt(), me.eventTime, ids, xs, ys)
    }

    private fun getPointerTracker(id: Int): PointerTracker {
        for (i in trackers.size..id) {
            val tracker = PointerTracker(i, timers, keyDetector, this,
                resources.getInteger(R.integer.config_delay_before_key_repeat_start),
                resources.getInteger(R.integer.config_long_press_key_timeout),
                resources.getInteger(R.integer.config_multi_tap_key_timeout))
            keys?.let { tracker.setKeyboard(it, keyHysteresisDistance) }
            listener?.let { tracker.setOnKeyboardActionListener(it) }
            trackers.add(tracker)
        }
        return trackers[id]
    }

    /** 長按：同 LIMEKeyboardBaseView.openPopupIfRequired 與 LIMEKeyboardView.onLongPress。 */
    private fun onLongPress(keyIndex: Int, tracker: PointerTracker) {
        if (!miniPopup.hasPopupLayout()) return
        val key = tracker.getKey(keyIndex) ?: return
        val code = key.codes[0]
        val handled = when {
            // 小鍵盤長按只開小鍵盤（同舊 LIMEKeyboardBaseView.onLongPress）
            isMiniKeyboard -> miniPopup.open(key)
            code == LIMEBaseKeyboard.KEYCODE_DONE || code == -9 || code == -16 -> {
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                true
            }
            code == LIMEKeyboard.KEYCODE_SPACE
                    && Math.abs((keyboard as LIMEKeyboard).spaceDragDiff) < keyHeight / 5 -> {
                listener?.onKey(KEYCODE_SPACE_LONGPRESS, null, 0, 0)
                true
            }
            code == LIMEKeyboard.KEYCODE_MODE_CHANGE -> {
                listener?.onKey(KEYCODE_SYMBOL_KEYBOARD, null, 0, 0)
                true
            }
            else -> miniPopup.open(key)
        }
        if (handled) {
            miniPopup.setTrackerId(tracker.mPointerId)
            // 這個手指之後不會送出放開，在這裡收起按下狀態（同舊鍵盤）
            tracker.setAlreadyProcessed()
            dispatcher.queue().remove(tracker)
            key.pressed = false
            invalidateKey(key)
        }
    }

    /** 長按與連續刪除的計時（同 LIMEKeyboardBaseView.UIHandler）；離開視窗時 closing() 會清掉排程。 */
    private inner class Timers : KeyTimers {
        private var inKeyRepeat = false
        private val handler = Handler(Looper.getMainLooper()) { msg ->
            val tracker = msg.obj as PointerTracker
            when (msg.what) {
                MSG_REPEAT_KEY -> {
                    tracker.repeatKey(msg.arg1)
                    startKeyRepeatTimer(keyRepeatInterval.toLong(), msg.arg1, tracker)
                }
                MSG_LONGPRESS_KEY -> onLongPress(msg.arg1, tracker)
            }
            true
        }

        override fun startKeyRepeatTimer(delay: Long, keyIndex: Int, tracker: PointerTracker) {
            inKeyRepeat = true
            handler.sendMessageDelayed(handler.obtainMessage(MSG_REPEAT_KEY, keyIndex, 0, tracker), delay)
        }

        override fun cancelKeyRepeatTimer() {
            inKeyRepeat = false
            handler.removeMessages(MSG_REPEAT_KEY)
        }

        override fun isInKeyRepeat(): Boolean = inKeyRepeat

        override fun startLongPressTimer(delay: Long, keyIndex: Int, tracker: PointerTracker) {
            handler.removeMessages(MSG_LONGPRESS_KEY)
            handler.sendMessageDelayed(handler.obtainMessage(MSG_LONGPRESS_KEY, keyIndex, 0, tracker), delay)
        }

        override fun cancelLongPressTimer() {
            handler.removeMessages(MSG_LONGPRESS_KEY)
        }

        override fun cancelKeyTimers() {
            cancelKeyRepeatTimer()
            cancelLongPressTimer()
        }
    }

    companion object {
        // 鍵盤送給 LIMEService 的功能鍵碼（原本定義在舊鍵盤 LIMEKeyboardView）
        const val KEYCODE_OPTIONS = -100
        const val KEYCODE_SPACE_LONGPRESS = -102
        const val KEYCODE_NEXT_IM = -104
        const val KEYCODE_PREV_IM = -105
        const val KEYCODE_SYMBOL_KEYBOARD = -106

        private const val MSG_REPEAT_KEY = 3
        private const val MSG_LONGPRESS_KEY = 4

        // LIMEService 的功能鍵碼（鍵盤 XML 裡的 codes）
        private const val KEYCODE_TO_ENGLISH = -9
        private const val KEYCODE_TO_CHINESE = -10
        private const val KEYCODE_MORE_SYMBOLS = -15
        private const val KEYCODE_NEXT_INPUT_METHOD = -16
    }
}
