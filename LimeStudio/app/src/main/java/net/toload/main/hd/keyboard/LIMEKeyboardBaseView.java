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

/*
 * Jeremy '11,8,8
 * Derive from gingerbread Latin IME LatinKeyboardBaseView, 
 * modified to compatible with pre 2.2 devices, and disable
 * fling selection of popup minikeybaord on large screen.
 * 
 */

package net.toload.main.hd.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;

import net.toload.main.hd.R;
import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key;
import net.toload.main.hd.keyboard.PointerTracker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

@SuppressLint("UseSparseArrays")
public class LIMEKeyboardBaseView extends View implements PointerTracker.UIProxy, ImeKeyboardView {
    private static final String TAG = "LIMEKeyboardBaseView";
    private static final boolean DEBUG = false;

    public static final int NOT_A_TOUCH_COORDINATE = -1;

    public interface OnKeyboardActionListener {

        /**
         * Called when the user presses a key. This is sent before the
         * {@link #onKey} is called. For keys that repeat, this is only
         * called once.
         *
         * @param primaryCode the unicode of the key being pressed. If the touch is
         *                    not on a valid key, the value will be zero.
         */
        void onPress(int primaryCode);

        /**
         * Called when the user releases a key. This is sent after the
         * {@link #onKey} is called. For keys that repeat, this is only
         * called once.
         *
         * @param primaryCode the code of the key that was released
         */
        void onRelease(int primaryCode);

        /**
         * Send a key press to the listener.
         *
         * @param primaryCode this is the key that was pressed
         * @param keyCodes    the codes for all the possible alternative keys with
         *                    the primary code being the first. If the primary key
         *                    code is a single character such as an alphabet or
         *                    number or symbol, the alternatives will include other
         *                    characters that may be on the same key or adjacent
         *                    keys. These codes are useful to correct for
         *                    accidental presses of a key adjacent to the intended
         *                    key.
         * @param x           x-coordinate pixel of touched event. If onKey is not
         *                    called by onTouchEvent,
         *                    the value should be NOT_A_TOUCH_COORDINATE.
         * @param y           y-coordinate pixel of touched event. If onKey is not
         *                    called by onTouchEvent,
         *                    the value should be NOT_A_TOUCH_COORDINATE.
         */
        void onKey(int primaryCode, int[] keyCodes, int x, int y);

        /**
         * Sends a sequence of characters to the listener.
         *
         * @param text the sequence of characters to be displayed.
         */
        void onText(CharSequence text);

        /**
         * Called when user released a finger outside any key.
         */
        void onCancel();

        /**
         * Called when the user quickly moves the finger from right to
         * left.
         */
        void swipeLeft();

        /**
         * Called when the user quickly moves the finger from left to
         * right.
         */
        void swipeRight();

        /**
         * Called when the user quickly moves the finger from up to down.
         */
        void swipeDown();

        /**
         * Called when the user quickly moves the finger from down to up.
         */
        void swipeUp();
    }

    // themed context
    Context mContext;

    // Timing constants
    private final int mKeyRepeatInterval;

    // Miscellaneous constants
    /* package */ static final int NOT_A_KEY = -1;
    private static final int[] LONG_PRESSABLE_STATE_SET = { android.R.attr.state_long_pressable };

    // XML attribute
    private float mBackgroundDimAmount;
    private float mKeyHysteresisDistance;
    float mVerticalCorrection; // 長按小鍵盤（MiniKeyboardPopup）會改小鍵盤的值
    private int mPreviewOffset;
    private int mPreviewHeight;
    private int mPopupLayout;
    private int mSpacePreviewTopPadding;
    private int mPreviewTopPadding;

    private boolean mtHardwareAcceleratedDrawingEnabled = false;

    // Main keyboard
    private LIMEBaseKeyboard mKeyboard;
    private Key[] mKeys;
    // TODO this attribute should be gotten from Keyboard.
    private int mKeyboardVerticalGap;

    // Key preview popup
    private PopupWindow mPreviewPopup;
    private int mPreviewTextSizeLarge;
    private int[] mOffsetInWindow;
    private int mOldPreviewKeyIndex = NOT_A_KEY;
    private boolean mShowPreview = true;
    private boolean mShowTouchPoints = true;
    private int mPopupPreviewOffsetX;
    private int mPopupPreviewOffsetY;
    private int mWindowY;
    private final int mDelayBeforePreview;
    private final int mDelayAfterPreview;

    // Popup mini keyboard（長按小鍵盤，見 MiniKeyboardPopup）
    private View mMiniKeyboardParent;
    private final MiniKeyboardPopup mMiniPopup;

    // key preview animation
    private Animation mKeyPreviewFadeInAnimator;
    private Animation mKeyPreviewFadeOutAnimator;

    /**
     * Listener for {@link OnKeyboardActionListener}.
     */
    private OnKeyboardActionListener mKeyboardActionListener;

    private final ArrayList<PointerTracker> mPointerTrackers = new ArrayList<>();

    // TODO: Let the PointerTracker class manage this pointer queue

    private final boolean mHasDistinctMultitouch;
    private int mOldPointerCount = 1;

    protected KeyDetector mKeyDetector = new ProximityKeyDetector();

    // Swipe gesture detector（見 SwipeGestures；長按小鍵盤本身設為 null）
    SwipeGestures mSwipeGestures;

    // Drawing
    /**
     * Whether the keyboard bitmap needs to be redrawn before it's blitted. *
     */
    private boolean mDrawPending;
    /**
     * The dirty region in the keyboard bitmap
     */
    private final Rect mDirtyRect = new Rect();
    /**
     * invalidateKey 標記的單一鍵:onBufferDraw 只重畫此鍵區域(髒矩形)
     */
    private Key mInvalidatedKey;
    /**
     * The keyboard bitmap for faster updates
     */
    private Bitmap mBuffer;
    /**
     * Notes if the keyboard just changed, so that we could possibly reallocate the
     * mBuffer.
     */
    private boolean mKeyboardChanged;
    /**
     * The canvas for the above mutable keyboard bitmap
     */
    private Canvas mCanvas;

    private final UIHandler mHandler = new UIHandler(this);
    // 畫單一鍵（背景、標籤、字根、圖示）與鍵的樣式，Compose 鍵盤共用，見 KeyPainter
    final KeyPainter mPainter = new KeyPainter();
    private final PointerDispatcher mPointerDispatcher = new PointerDispatcher(mHandler, this::getPointerTracker);

    // private LIMEPreferenceManager mLIMEPref;

    static class UIHandler extends Handler implements KeyTimers {
        private static final int MSG_POPUP_PREVIEW = 1;
        private static final int MSG_DISMISS_PREVIEW = 2;
        private static final int MSG_REPEAT_KEY = 3;
        private static final int MSG_LONGPRESS_KEY = 4;
        private static final int MSG_SHOW_PREVIEW = 5;

        private boolean mInKeyRepeat;

        private final WeakReference<LIMEKeyboardBaseView> mLIMEKeyboardBaseViewWeakReference;

        public UIHandler(LIMEKeyboardBaseView keyboardBaseView) {
            super(android.os.Looper.getMainLooper());
            mLIMEKeyboardBaseViewWeakReference = new WeakReference<LIMEKeyboardBaseView>(keyboardBaseView);
        }

        @Override
        public void handleMessage(Message msg) {
            LIMEKeyboardBaseView mLIMEKeyboardBaseView = mLIMEKeyboardBaseViewWeakReference.get();
            if (mLIMEKeyboardBaseView == null)
                return;
            switch (msg.what) {
                case MSG_REPEAT_KEY: {
                    if (DEBUG)
                        Log.i(TAG, "handleMessage()  MSG_REPEAT_KEY");
                    final PointerTracker tracker = (PointerTracker) msg.obj;
                    tracker.repeatKey(msg.arg1);
                    startKeyRepeatTimer(mLIMEKeyboardBaseView.mKeyRepeatInterval, msg.arg1, tracker);
                    break;
                }
                case MSG_LONGPRESS_KEY: {
                    if (DEBUG)
                        Log.i(TAG, "handleMessage()  MSG_LONGPRESS_KEY");
                    final PointerTracker tracker = (PointerTracker) msg.obj;
                    mLIMEKeyboardBaseView.openPopupIfRequired(msg.arg1, tracker);
                    break;
                }
            }
        }

        @Override
        public void startKeyRepeatTimer(long delay, int keyIndex, PointerTracker tracker) {
            if (DEBUG)
                Log.i(TAG, "UIHandler.startKeyRepeatTimer() delay=" + delay + "keyIndex= " + keyIndex);
            mInKeyRepeat = true;
            sendMessageDelayed(obtainMessage(MSG_REPEAT_KEY, keyIndex, 0, tracker), delay);
        }

        @Override
        public void cancelKeyRepeatTimer() {
            if (DEBUG)
                Log.i(TAG, "UIHandler.cancelKeyRepeatTimer()");
            mInKeyRepeat = false;
            removeMessages(MSG_REPEAT_KEY);
        }

        @Override
        public boolean isInKeyRepeat() {
            if (DEBUG)
                Log.i(TAG, "UIHandler.isInKeyRepeat(): " + mInKeyRepeat);
            return mInKeyRepeat;
        }

        @Override
        public void startLongPressTimer(long delay, int keyIndex, PointerTracker tracker) {
            if (DEBUG)
                Log.i(TAG, "UIHandler.startLongPressTimer() delay=" + delay + "keyIndex= " + keyIndex);
            removeMessages(MSG_LONGPRESS_KEY);
            sendMessageDelayed(obtainMessage(MSG_LONGPRESS_KEY, keyIndex, 0, tracker), delay);
        }

        @Override
        public void cancelLongPressTimer() {
            if (DEBUG)
                Log.i(TAG, "UIHandler.cancelLongPressTimer()");
            removeMessages(MSG_LONGPRESS_KEY);
        }

        @Override
        public void cancelKeyTimers() {
            cancelKeyRepeatTimer();
            cancelLongPressTimer();
        }

        public void cancelAllMessages() {
            cancelKeyTimers();
        }
    }

    static class PointerQueue {
        private LinkedList<PointerTracker> mQueue = new LinkedList<>();

        public void add(PointerTracker tracker) {
            mQueue.add(tracker);
        }

        public int lastIndexOf(PointerTracker tracker) {
            LinkedList<PointerTracker> queue = mQueue;
            for (int index = queue.size() - 1; index >= 0; index--) {
                PointerTracker t = queue.get(index);
                if (t == tracker)
                    return index;
            }
            return -1;
        }

        public void releaseAllPointersOlderThan(PointerTracker tracker, long eventTime) {
            LinkedList<PointerTracker> queue = mQueue;
            int oldestPos = 0;
            for (PointerTracker t = queue.get(oldestPos); t != tracker; t = queue.get(oldestPos)) {
                if (t.isModifier()) {
                    oldestPos++;
                } else {
                    t.onUpEvent(t.getLastX(), t.getLastY(), eventTime);
                    t.setAlreadyProcessed();
                    queue.remove(oldestPos);
                }
            }
        }

        public void releaseAllPointersExcept(PointerTracker tracker, long eventTime) {
            for (PointerTracker t : mQueue) {
                if (t == tracker)
                    continue;
                t.onUpEvent(t.getLastX(), t.getLastY(), eventTime);
                t.setAlreadyProcessed();
            }
            mQueue.clear();
            if (tracker != null)
                mQueue.add(tracker);
        }

        public void remove(PointerTracker tracker) {
            mQueue.remove(tracker);
        }

        public boolean isInSlidingKeyInput() {
            for (final PointerTracker tracker : mQueue) {
                if (tracker.isInSlidingKeyInput())
                    return true;
            }
            return false;
        }
    }

    public LIMEKeyboardBaseView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.LIMEKeyboardBaseView);
        mContext = context;
    }

    public void setHardwareAcceleratedDrawingEnabled(final boolean enabled) {
        // All key drawing happens in software into mBuffer (see onBufferDraw);
        // onDraw only blits that bitmap, which the hardware canvas fully supports.
        // A forced LAYER_TYPE_SOFTWARE added a third buffer that was re-rasterized
        // and re-uploaded to the GPU on every key press — a major typing jank source.
        setLayerType(LAYER_TYPE_NONE, null);
        mtHardwareAcceleratedDrawingEnabled = enabled;
    }

    public LIMEKeyboardBaseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.LIMEKeyboardBaseView, defStyle, R.style.LIMEBaseKeyboard);
        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int previewLayout = 0;
        int keyTextSize = 0;

        int n = a.getIndexCount();

        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);

            // 畫鍵用的樣式（背景、字級、顏色、陰影）由 KeyPainter 解讀
            if (mPainter.applyStyleAttribute(a, attr))
                continue;
            if (attr == R.styleable.LIMEKeyboardBaseView_keyHysteresisDistance) {
                mKeyHysteresisDistance = a.getDimensionPixelOffset(attr, 0);
            } else if (attr == R.styleable.LIMEKeyboardBaseView_verticalCorrection) {
                mVerticalCorrection = a.getDimensionPixelOffset(attr, 0);
            } else if (attr == R.styleable.LIMEKeyboardBaseView_keyPreviewLayout) {
                previewLayout = a.getResourceId(attr, 0);
            } else if (attr == R.styleable.LIMEKeyboardBaseView_keyPreviewOffset) {
                mPreviewOffset = a.getDimensionPixelOffset(attr, 0);
            } else if (attr == R.styleable.LIMEKeyboardBaseView_keyPreviewHeight) {
                mPreviewHeight = a.getDimensionPixelSize(attr, 80);
            } else if (attr == R.styleable.LIMEKeyboardBaseView_popupLayout) {
                mPopupLayout = a.getResourceId(attr, 0);
            } else if (attr == R.styleable.LIMEKeyboardBaseView_spacePreviewTopPadding) { // Jeremy 15,7,13
                mSpacePreviewTopPadding = a.getDimensionPixelSize(attr, 10);
            } else if (attr == R.styleable.LIMEKeyboardBaseView_previewTopPadding) { // Jeremy 15,7,13
                mPreviewTopPadding = a.getDimensionPixelSize(attr, 0);
            } else if (attr == R.styleable.LIMEKeyboardBaseView_backgroundDimAmount) {
                mBackgroundDimAmount = a.getFloat(attr, 0.5f);
            }
        }

        final Resources res = getResources();

        mPreviewPopup = new PopupWindow(context);
        if (previewLayout != 0) {
            mKeyPreviewFadeInAnimator = AnimationUtils.loadAnimation(mContext, R.anim.key_preview_fadein);
            mKeyPreviewFadeOutAnimator = AnimationUtils.loadAnimation(mContext, R.anim.key_preview_fadeout);

            mPreviewTextSizeLarge = (int) res.getDimension(R.dimen.key_preview_text_size_large);
        } else {
            mShowPreview = false;
        }
        mPreviewPopup.setTouchable(false);
        mDelayBeforePreview = res.getInteger(R.integer.config_delay_before_preview);
        mDelayAfterPreview = res.getInteger(R.integer.config_delay_after_preview);

        mMiniKeyboardParent = this;
        mMiniPopup = new MiniKeyboardPopup(context, new MiniKeyboardPopup.Host() {
            public View view() { return LIMEKeyboardBaseView.this; }
            public LIMEBaseKeyboard getKeyboard() { return mKeyboard; }
            public OnKeyboardActionListener getOnKeyboardActionListener() { return mKeyboardActionListener; }
            public boolean isShifted() { return LIMEKeyboardBaseView.this.isShifted(); }
            public boolean isPreviewEnabled() { return mShowPreview; }
            public void invalidateAllKeys() { LIMEKeyboardBaseView.this.invalidateAllKeys(); }
        }, mPopupLayout);

        mPainter.finishStyle(keyTextSize);

        mSwipeGestures = new SwipeGestures(getContext(), new SwipeGestures.Host() {
            public View view() { return LIMEKeyboardBaseView.this; }
            public void swipeLeft() { LIMEKeyboardBaseView.this.swipeLeft(); }
            public void swipeRight() { LIMEKeyboardBaseView.this.swipeRight(); }
            public void swipeUp() { LIMEKeyboardBaseView.this.swipeUp(); }
            public void swipeDown() { LIMEKeyboardBaseView.this.swipeDown(); }
        });

        mHasDistinctMultitouch = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
        mKeyRepeatInterval = res.getInteger(R.integer.config_key_repeat_interval);
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
        mKeyboardActionListener = listener;
        for (PointerTracker tracker : mPointerTrackers) {
            tracker.setOnKeyboardActionListener(listener);
        }
    }

    /**
     * Returns the {@link OnKeyboardActionListener} object.
     *
     * @return the listener attached to this keyboard
     */
    protected OnKeyboardActionListener getOnKeyboardActionListener() {
        return mKeyboardActionListener;
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time
     * and the
     * view will re-layout itself to accommodate the keyboard.
     * //* @see Keyboard
     *
     * @param keyboard the keyboard to display in this view
     * @see #getKeyboard()
     */
    public void setKeyboard(LIMEBaseKeyboard keyboard) {
        // Remove any pending messages, except dismissing preview
        mHandler.cancelKeyTimers();
        mKeyboard = keyboard;
        // LatinImeLogger.onSetKeyboard(keyboard);
        mKeys = mKeyDetector.setKeyboard(keyboard, -getPaddingLeft(),
                -getPaddingTop() + mVerticalCorrection);
        mKeyboardVerticalGap = (int) getResources().getDimension(R.dimen.key_bottom_gap);
        for (PointerTracker tracker : mPointerTrackers) {
            tracker.setKeyboard(mKeys, mKeyHysteresisDistance);
        }
        requestLayout();
        // Hint to reallocate the buffer if the size changed
        mOffsetInWindow = null; // reset offset window. keyboard changed.
        mKeyboardChanged = true;
        invalidateAllKeys();
        computeProximityThreshold(keyboard);
        mMiniPopup.clearCache();
        mPainter.mLabelSplitCache.clear();
    }

    /**
     * Returns the current keyboard being displayed by this view.
     *
     * @return the currently attached keyboard
     *         //* @see #setKeyboard(Keyboard)
     */
    public LIMEBaseKeyboard getKeyboard() {
        return mKeyboard;
    }

    /**
     * Return whether the device has distinct multi-touch panel.
     *
     * @return true if the device has distinct multi-touch panel.
     */
    public boolean hasDistinctMultitouch() {
        return mHasDistinctMultitouch;
    }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     *
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     */
    public boolean setShifted(boolean shifted) {
        if (mKeyboard != null) {
            if (mKeyboard.setShifted(shifted)) {
                // The whole keyboard probably needs to be redrawn
                invalidateAllKeys();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     *
     * @return true if the shift is in a pressed state, false otherwise. If there is
     *         no shift key on the keyboard or there is no keyboard attached, it
     *         returns false.
     */
    public boolean isShifted() {
        return mKeyboard != null && mKeyboard.isShifted();
    }

    /**
     * Enables or disables the key feedback popup. This is a popup that shows a
     * magnified
     * version of the depressed key. By default the preview is enabled.
     *
     * @param previewEnabled whether or not to enable the key feedback popup
     * @see #isPreviewEnabled()
     */
    public void setPreviewEnabled(boolean previewEnabled) {
        mShowPreview = previewEnabled;
    }

    /**
     * Returns the enabled state of the key feedback popup.
     *
     * @return whether or not the key feedback popup is enabled
     * @see #setPreviewEnabled(boolean)
     */
    public boolean isPreviewEnabled() {
        return mShowPreview;
    }

    public void setPopupParent(View v) {
        mMiniKeyboardParent = v;
    }

    public void setPopupOffset(int x, int y) {
        mPopupPreviewOffsetX = x;
        mPopupPreviewOffsetY = y;
        if (mPreviewPopup.isShowing()) {
            mPreviewPopup.dismiss();
        }
    }

    /**
     * When enabled, calls to {@link OnKeyboardActionListener#onKey} will include
     * key
     * codes for adjacent keys. When disabled, only the primary key code will be
     * reported.
     *
     * @param enabled whether or not the proximity correction is enabled
     */
    public void setProximityCorrectionEnabled(boolean enabled) {
        mKeyDetector.setProximityCorrectionEnabled(enabled);
    }

    /**
     * Returns true if proximity correction is enabled.
     */
    public boolean isProximityCorrectionEnabled() {
        return mKeyDetector.isProximityCorrectionEnabled();
    }

    protected CharSequence adjustCase(CharSequence label) {
        if (mKeyboard.isShifted() && label != null && label.length() <= 3
                && Character.isLowerCase(label.charAt(0))) {
            label = label.toString().toUpperCase(java.util.Locale.ROOT);
        }
        return label;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Round up a little
        if (mKeyboard == null) {
            setMeasuredDimension(
                    getPaddingLeft() + getPaddingRight(), getPaddingTop() + getPaddingBottom());
        } else {
            int width = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                width = MeasureSpec.getSize(widthMeasureSpec);
            }
            setMeasuredDimension(
                    width, mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom());
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and
     * vertically)
     * and square it to get the proximity threshold. We use a square here and in
     * computing
     * the touch distance from a key's center to avoid taking a square root.
     *
     */
    private void computeProximityThreshold(LIMEBaseKeyboard keyboard) {
        if (keyboard == null)
            return;
        final Key[] keys = mKeys;
        if (keys == null)
            return;
        int length = keys.length;
        int dimensionSum = 0;
        for (Key key : keys) {
            dimensionSum += Math.min(key.width, key.height + mKeyboardVerticalGap) + key.gap;
        }
        if (dimensionSum < 0 || length == 0)
            return;
        mKeyDetector.setProximityThreshold((int) (dimensionSum * 1.4f / length));
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Release the buffer, if any and it will be reallocated on the next draw
        mBuffer = null;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw();
        }
        canvas.drawBitmap(mBuffer, 0, 0, null);
    }

    /**
     * 標籤在目前字級下的寬度超過可用寬度時，回傳等比例縮小後的字級；放得下就回傳原字級。
     */
    static int fitLabelTextSize(int labelSize, float textWidth, float availableWidth) {
        if (availableWidth > 0 && textWidth > availableWidth) {
            return (int) (labelSize * availableWidth / textWidth);
        }
        return labelSize;
    }

    private void onBufferDraw() {
        boolean bufferRebuilt = false;
        if (mBuffer == null || mKeyboardChanged) {
            if (mBuffer == null || (mBuffer.getWidth() != getWidth() || mBuffer.getHeight() != getHeight())) {
                // Make sure our bitmap is at least 1x1
                final int width = Math.max(1, getWidth());
                final int height = Math.max(1, getHeight());
                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBuffer);
            }
            invalidateAllKeys();
            mKeyboardChanged = false;
            bufferRebuilt = true;
        }
        final Canvas canvas = mCanvas;
        // Daniel: this is old usage. will cause crash
        // canvas.clipRect(mDirtyRect, Op.REPLACE);

        if (mKeyboard == null)
            return;

        final Paint paint = mPainter.mPaint;
        final int kbdPaddingLeft = getPaddingLeft();
        final int kbdPaddingTop = getPaddingTop();
        final Key[] keys = mKeys;

        // 單鍵髒矩形重繪:invalidateKey 觸發、buffer 有效且沒有整鍵盤重繪
        // 待處理時,只 clip 到該鍵區域重畫一鍵,取代原本每次按鍵 O(全部鍵) 的重繪。
        // mini keyboard 開著時有全鍵盤壓暗層,維持整張重畫。
        final Key invalidatedKey = mInvalidatedKey;
        mInvalidatedKey = null;
        final boolean drawSingleKey = invalidatedKey != null
                && !bufferRebuilt && !mDrawPending && !mMiniPopup.isMiniKeyboardOn();
        if (drawSingleKey) {
            canvas.save();
            canvas.clipRect(invalidatedKey.x + kbdPaddingLeft,
                    invalidatedKey.y + kbdPaddingTop,
                    invalidatedKey.x + invalidatedKey.width + kbdPaddingLeft,
                    invalidatedKey.y + invalidatedKey.height + kbdPaddingTop);
        }
        final Key[] keysToDraw = drawSingleKey ? new Key[]{invalidatedKey} : keys;

        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
        // final int keyCount = keys.length;
        for (final Key key : keysToDraw) {
            mPainter.drawKey(canvas, key, mKeyboard, kbdPaddingLeft, kbdPaddingTop, this::adjustCase);
        }
        if (drawSingleKey) {
            canvas.restore();
        }
        // Overlay a dark rectangle to dim the keyboard
        if (mMiniPopup.isMiniKeyboardOn()) {
            paint.setColor((int) (mBackgroundDimAmount * 0xFF) << 24);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }

        if (DEBUG) {
            if (mShowTouchPoints) {
                for (PointerTracker tracker : mPointerTrackers) {
                    int startX = tracker.getStartX();
                    int startY = tracker.getStartY();
                    int lastX = tracker.getLastX();
                    int lastY = tracker.getLastY();
                    paint.setAlpha(128);
                    paint.setColor(0xFFFF0000);
                    canvas.drawCircle(startX, startY, 3, paint);
                    canvas.drawLine(startX, startY, lastX, lastY, paint);
                    paint.setColor(0xFF0000FF);
                    canvas.drawCircle(lastX, lastY, 3, paint);
                    paint.setColor(0xFF00FF00);
                    canvas.drawCircle((startX + lastX) / 2, (startY + lastY) / 2, 2, paint);
                }
            }
        }

        mDrawPending = false;
        mDirtyRect.setEmpty();
    }

    /**
     * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not
     * sufficient
     * because the keyboard renders the keys to an off-screen buffer and an
     * invalidate() only
     * draws the cached buffer.
     *
     * @see #invalidateKey(Key)
     */
    public void invalidateAllKeys() {
        mDirtyRect.union(0, 0, getWidth(), getHeight());
        mDrawPending = true;
        invalidate();
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this
     * method if only
     * one key is changing it's content. Any changes that affect the position or
     * size of the key
     * may not be honored.
     * //* @param key key in the attached {@link //Keyboard}.
     *
     * @see #invalidateAllKeys
     */
    public void invalidateKey(Key key) {
        if (key == null)
            return;
        // 記下要重繪的鍵,onBufferDraw 只重畫這一鍵的區域,不再全鍵盤重繪
        mInvalidatedKey = key;
        mDirtyRect.union(key.x + getPaddingLeft(), key.y + getPaddingTop(),
                key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
        onBufferDraw();
        invalidate(key.x + getPaddingLeft(), key.y + getPaddingTop(),
                key.x + key.width + getPaddingLeft(), key.y + key.height + getPaddingTop());
    }

    @Override
    public void showPreview(int keyIndex, PointerTracker tracker) {

    }

    private boolean openPopupIfRequired(int keyIndex, PointerTracker tracker) {
        // Check if we have a popup layout specified first.
        if (!mMiniPopup.hasPopupLayout()) {
            return false;
        }

        Key popupKey = tracker.getKey(keyIndex);
        if (popupKey == null)
            return false;
        boolean result = onLongPress(popupKey);
        if (result) {
            mMiniPopup.setTrackerId(tracker.mPointerId);
            // Mark this tracker "already processed" and remove it from the pointer queue
            tracker.setAlreadyProcessed();
            mPointerDispatcher.queue().remove(tracker);
            // The processed tracker will never deliver this key's release event,
            // so un-press it here — otherwise the key stays stuck drawing with
            // pressed colors (near-invisible label) after the popup closes.
            popupKey.pressed = false;
            invalidateKey(popupKey);
        }
        return result;
    }

    /**
     * Called when a key is long pressed. By default this will open any popup
     * keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     *
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should
     *         call the
     *         method on the base class if the subclass doesn't wish to handle the
     *         call.
     */
    protected boolean onLongPress(Key popupKey) {
        return mMiniPopup.open(popupKey);
    }

    private PointerTracker getPointerTracker(final int id) {
        final ArrayList<PointerTracker> pointers = mPointerTrackers;
        final Key[] keys = mKeys;
        final OnKeyboardActionListener listener = mKeyboardActionListener;

        // Create pointer trackers until we can get 'id+1'-th tracker, if needed.
        for (int i = pointers.size(); i <= id; i++) {
            final Resources res = getResources();
            final PointerTracker tracker = new PointerTracker(i, mHandler, mKeyDetector, this,
                    res.getInteger(R.integer.config_delay_before_key_repeat_start),
                    res.getInteger(R.integer.config_long_press_key_timeout),
                    res.getInteger(R.integer.config_multi_tap_key_timeout));
            if (keys != null)
                tracker.setKeyboard(keys, mKeyHysteresisDistance);
            if (listener != null)
                tracker.setOnKeyboardActionListener(listener);
            pointers.add(tracker);
        }

        return pointers.get(id);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent me) {
        if (DEBUG)
            Log.i(TAG, "onTouchEvent()");
        final int action = me.getActionMasked();
        final int pointerCount = me.getPointerCount();
        final int oldPointerCount = mOldPointerCount;
        mOldPointerCount = pointerCount;

        if (DEBUG)
            Log.i(TAG, "onTouchEvent() pointerCount = " + pointerCount + ", oldPointerCount" + oldPointerCount);

        // TODO: cleanup this code into a multi-touch to single-touch event converter
        // class?
        // If the device does not have distinct multi-touch support panel, ignore all
        // multi-touch
        // events except a transition from/to single-touch.
        if (!mHasDistinctMultitouch && pointerCount > 1 && oldPointerCount > 1) {
            return true;
        }

        // Track the last few movements to look for spurious swipes.
        if (mSwipeGestures != null)
            mSwipeGestures.addMovement(me);

        // Gesture detector must be enabled only when mini-keyboard is not on the
        // screen.
        if (!mMiniPopup.isMiniKeyboardOn()
                && mSwipeGestures != null && mSwipeGestures.onTouchEvent(me)) {
            mHandler.cancelKeyTimers();
            return true;
        }

        final long eventTime = me.getEventTime();
        final int index = me.getActionIndex();
        final int id = me.getPointerId(index);
        final int x = (int) me.getX(index);
        final int y = (int) me.getY(index);

        // Needs to be called after the gesture detector gets a turn, as it may have
        // displayed the mini keyboard
        if (mMiniPopup.forwardTouchEvent(me)) {
            return true;
        }

        // 多指協調（哪個手指先送鍵、修飾鍵、單點面板轉換）見 PointerDispatcher
        final int[] pointerIds = new int[pointerCount];
        final int[] pointerXs = new int[pointerCount];
        final int[] pointerYs = new int[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            pointerIds[i] = me.getPointerId(i);
            pointerXs[i] = (int) me.getX(i);
            pointerYs[i] = (int) me.getY(i);
        }
        return mPointerDispatcher.dispatch(action, pointerCount, oldPointerCount, mHasDistinctMultitouch,
                id, x, y, eventTime, pointerIds, pointerXs, pointerYs);
    }

    protected void swipeRight() {
        mKeyboardActionListener.swipeRight();
    }

    protected void swipeLeft() {
        mKeyboardActionListener.swipeLeft();
    }

    protected void swipeUp() {
        mKeyboardActionListener.swipeUp();
    }

    protected void swipeDown() {
        mKeyboardActionListener.swipeDown();
    }

    public void closing() {
        if (DEBUG)
            Log.i(TAG, "closing()");

        mHandler.cancelAllMessages();
        mMiniPopup.dismissPopupKeyboard();
        mBuffer = null;
        mCanvas = null;
        mMiniPopup.clearCache();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        closing();
    }

    public boolean handleBack() {
        return mMiniPopup.handleBack();
    }

    @Override
    public View asView() {
        return this;
    }
}
