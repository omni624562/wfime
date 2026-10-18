package net.toload.main.hd.keyboard;

import android.content.Context;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import net.toload.main.hd.R;
import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key;

import java.util.List;
import java.util.WeakHashMap;

/**
 * 長按小鍵盤（例如「.」鍵的標點面板）：建立、定位、顯示、轉送觸控與關閉。
 * 從 LIMEKeyboardBaseView 原樣搬出（藍圖 A4-4）；小鍵盤本身是小鍵盤模式的 ComposeKeyboardView（A4-7②）。
 */
final class MiniKeyboardPopup {

    /** 開小鍵盤的鍵盤需要提供的東西。 */
    interface Host {
        /** 鍵盤本身（定位用、PopupWindow 的錨點）。 */
        View view();

        LIMEBaseKeyboard getKeyboard();

        LIMEKeyboardBaseView.OnKeyboardActionListener getOnKeyboardActionListener();

        boolean isShifted();

        boolean isPreviewEnabled();

        void invalidateAllKeys();
    }

    private final Context mContext;
    private final Host mHost;
    private final int mPopupLayout;

    private final PopupWindow mMiniKeyboardPopup;
    private ComposeKeyboardView mMiniKeyboard;
    private final WeakHashMap<Key, View> mMiniKeyboardCache = new WeakHashMap<>();
    private int mMiniKeyboardOriginX;
    private int mMiniKeyboardOriginY;
    private long mMiniKeyboardPopupTime;
    private int[] mWindowOffset;
    private final float mMiniKeyboardSlideAllowance;
    private int mMiniKeyboardTrackerId;
    private int mPopupPreviewDisplayedY;

    private final boolean isLargeScreen = true; // large || xlarge; //Force turn off fling selection now.

    MiniKeyboardPopup(Context context, Host host, int popupLayout) {
        mContext = context;
        mHost = host;
        mPopupLayout = popupLayout;
        mMiniKeyboardPopup = new PopupWindow(context);
        // Jeremy '24,1,6: Fix user issue where tapping outside symbol popup doesn't
        // close it.
        // Needs a valid background (even transparent) to intercept outside touches.
        mMiniKeyboardPopup
                .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        mMiniKeyboardPopup.setOutsideTouchable(true);
        mMiniKeyboardPopup.setFocusable(true); // FIX: Enable focusable to consume outside touch events (prevent
                                               // pass-through)

        mMiniKeyboardPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // Jeremy '24,1,6: Force state reset when popup is dismissed by outside touch.
                // Critical: Must set mMiniKeyboard to null to stop drawing the dimming overlay.
                mMiniKeyboard = null;
                mMiniKeyboardOriginX = 0;
                mMiniKeyboardOriginY = 0;
                mHost.invalidateAllKeys();
            }
        });

        mMiniKeyboardPopup.setAnimationStyle(R.style.MiniKeyboardAnimation);
        mMiniKeyboardSlideAllowance = context.getResources().getDimension(R.dimen.mini_keyboard_slide_allowance);
    }

    /** 有沒有設定小鍵盤版面（沒有就不開小鍵盤）。 */
    boolean hasPopupLayout() {
        return mPopupLayout != 0;
    }

    /** 小鍵盤開著時，鍵盤要整張重畫並蓋一層壓暗。 */
    boolean isMiniKeyboardOn() {
        return mMiniKeyboard != null;
    }

    /** 換鍵盤或收起時清掉快取的小鍵盤。 */
    void clearCache() {
        mMiniKeyboardCache.clear();
    }

    /** 長按的手指編號，之後的觸控轉送給小鍵盤。 */
    void setTrackerId(int pointerId) {
        mMiniKeyboardTrackerId = pointerId;
    }

    private View inflateMiniKeyboardContainer(Key popupKey) {
        final View host = mHost.view();
        int popupKeyboardId = popupKey.popupResId;
        // LayoutInflater inflater = (LayoutInflater)
        // getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View container = LayoutInflater.from(mContext).inflate(mPopupLayout, null);
        if (container == null)
            throw new NullPointerException();

        ComposeKeyboardView miniKeyboard = (ComposeKeyboardView) container.findViewById(R.id.LIMEPopupKeyboard);
        miniKeyboard.setOnKeyboardActionListener(new LIMEKeyboardBaseView.OnKeyboardActionListener() {
            public void onKey(int primaryCode, int[] keyCodes, int x, int y) {
                mHost.getOnKeyboardActionListener().onKey(primaryCode, keyCodes, x, y);
                dismissPopupKeyboard();
            }

            public void onText(CharSequence text) {
                mHost.getOnKeyboardActionListener().onText(text);
                dismissPopupKeyboard();
            }

            public void onCancel() {
                mHost.getOnKeyboardActionListener().onCancel();
                dismissPopupKeyboard();
            }

            public void swipeLeft() {
            }

            public void swipeRight() {
            }

            public void swipeUp() {
            }

            public void swipeDown() {
            }

            public void onPress(int primaryCode) {
                mHost.getOnKeyboardActionListener().onPress(primaryCode);
            }

            public void onRelease(int primaryCode) {
                mHost.getOnKeyboardActionListener().onRelease(primaryCode);
            }
        });
        // 換成小鍵盤的鍵偵測、關掉滑動手勢（原本直接改 mKeyDetector、把 mSwipeGestures 設 null）
        miniKeyboard.setupAsMiniKeyboard(mMiniKeyboardSlideAllowance);

        LIMEBaseKeyboard keyboard;
        if (popupKey.popupCharacters != null) {
            keyboard = new LIMEBaseKeyboard(mContext, popupKeyboardId, popupKey.popupCharacters,
                    -1, host.getPaddingLeft() + host.getPaddingRight(),
                    mHost.getKeyboard().getKeySizeScale());
        } else {
            keyboard = new LIMEBaseKeyboard(mContext, popupKeyboardId,
                    mHost.getKeyboard().getKeySizeScale(), 0, 0); // Jeremy '12,5,21 never show arrow
                                                                  // keys in popup keyboard
        }
        // mini keyboard in fling mode override with fling correction. Jeremy '12,5,27
        if (!isLargeScreen || keyboard.getKeys().size() == 1)
            miniKeyboard.verticalCorrection = host.getResources()
                    .getDimension(R.dimen.mini_keyboard_fling_vertical_correction);
        miniKeyboard.setKeyboard(keyboard);

        container.measure(View.MeasureSpec.makeMeasureSpec(host.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(host.getHeight(), View.MeasureSpec.AT_MOST));

        return container;
    }

    private static boolean isOneRowKeys(List<Key> keys) {
        if (keys.size() == 0)
            return false;
        final int edgeFlags = keys.get(0).edgeFlags;
        // HACK: The first key of mini keyboard which was inflated from xml and has
        // multiple rows,
        // does not have both top and bottom edge flags on at the same time. On the
        // other hand,
        // the first key of mini keyboard that was created with popupCharacters must
        // have both top
        // and bottom edge flags on.
        // When you want to use one row mini-keyboard from xml file, make sure that the
        // row has
        // both top and bottom edge flags set.
        return (edgeFlags & LIMEBaseKeyboard.EDGE_TOP) != 0 && (edgeFlags & LIMEBaseKeyboard.EDGE_BOTTOM) != 0;
    }

    /**
     * 長按有小鍵盤的鍵時開出小鍵盤；這個鍵沒有小鍵盤就回傳 false。
     */
    boolean open(Key popupKey) {
        // TODO if popupKey.popupCharacters has only one letter, send it as key without
        // opening
        // mini keyboard.

        if (popupKey.popupResId == 0)
            return false;

        final View host = mHost.view();
        View container = mMiniKeyboardCache.get(popupKey);
        if (container == null) {
            container = inflateMiniKeyboardContainer(popupKey);
            mMiniKeyboardCache.put(popupKey, container);
        }
        mMiniKeyboard = (ComposeKeyboardView) container.findViewById(R.id.LIMEPopupKeyboard);
        if (mWindowOffset == null) {
            mWindowOffset = new int[2];
            host.getLocationInWindow(mWindowOffset);
        }

        // Get width of a key in the mini popup keyboard = "miniKeyWidth".
        // On the other hand, "popupKey.width" is width of the pressed key on the main
        // keyboard.
        // We adjust the position of mini popup keyboard with the edge key in it:
        // a) When we have the leftmost key in popup keyboard directly above the pressed
        // key
        // Right edges of both keys should be aligned for consistent default selection
        // b) When we have the rightmost key in popup keyboard directly above the
        // pressed key
        // Left edges of both keys should be aligned for consistent default selection
        final List<Key> miniKeys = mMiniKeyboard.getKeyboard().getKeys();
        final int miniKeyWidth = miniKeys.size() > 0 ? miniKeys.get(0).width : 0;

        // HACK: Have the leftmost number in the popup characters right above the key
        boolean isNumberAtLeftmost = hasMultiplePopupChars(popupKey) && isNumberAtLeftmostPopupChar(popupKey);
        int popupX = popupKey.x + mWindowOffset[0];
        popupX += host.getPaddingLeft();
        if (isNumberAtLeftmost) {
            popupX += popupKey.width - miniKeyWidth; // adjustment for a) described above
            popupX -= container.getPaddingLeft();
        } else {
            popupX += miniKeyWidth; // adjustment for b) described above
            popupX -= container.getMeasuredWidth();
            popupX += container.getPaddingRight();
        }
        int popupY = popupKey.y + mWindowOffset[1];
        popupY += host.getPaddingTop();
        popupY -= container.getMeasuredHeight();
        popupY += container.getPaddingBottom();
        final int x = popupX;
        final int y = mHost.isPreviewEnabled() && isOneRowKeys(miniKeys) ? mPopupPreviewDisplayedY : popupY;

        int adjustedX = x;
        if (x < 0) {
            adjustedX = 0;
        } else if (x > (host.getMeasuredWidth() - container.getMeasuredWidth())) {
            adjustedX = host.getMeasuredWidth() - container.getMeasuredWidth();
        }
        mMiniKeyboardOriginX = adjustedX + container.getPaddingLeft() - mWindowOffset[0];
        mMiniKeyboardOriginY = y + container.getPaddingTop() - mWindowOffset[1];
        mMiniKeyboard.setShifted(mHost.isShifted());
        mMiniKeyboardPopup.setContentView(container);
        mMiniKeyboardPopup.setWidth(container.getMeasuredWidth());
        mMiniKeyboardPopup.setHeight(container.getMeasuredHeight());
        mMiniKeyboardPopup.showAtLocation(host, Gravity.NO_GRAVITY, x, y);

        // Inject down event on the key to mini keyboard.
        long eventTime = SystemClock.uptimeMillis();
        mMiniKeyboardPopupTime = eventTime;
        if (!isLargeScreen || miniKeys.size() == 1) { // disable fling on large screen; //Jeremy enable fling when popup
                                                      // keyboard only has 1 key '12,5,20
            MotionEvent downEvent = generateMiniKeyboardMotionEvent(MotionEvent.ACTION_DOWN, popupKey.x
                    + popupKey.width / 2, popupKey.y + popupKey.height / 2, eventTime);
            mMiniKeyboard.onTouchEvent(downEvent);
            downEvent.recycle();
        }

        mHost.invalidateAllKeys();
        return true;
    }

    private static boolean hasMultiplePopupChars(Key key) {
        return key.popupCharacters != null && key.popupCharacters.length() > 1;
    }

    private static boolean isNumberAtLeftmostPopupChar(Key key) {
        return key.popupCharacters != null && key.popupCharacters.length() > 0
                && isAsciiDigit(key.popupCharacters.charAt(0));
    }

    private static boolean isAsciiDigit(char c) {
        return (c < 0x80) && Character.isDigit(c);
    }

    private MotionEvent generateMiniKeyboardMotionEvent(int action, int x, int y, long eventTime) {
        return MotionEvent.obtain(mMiniKeyboardPopupTime, eventTime, action,
                x - mMiniKeyboardOriginX, y - mMiniKeyboardOriginY, 0);
    }

    /**
     * 只有一個鍵的小鍵盤（可滑動選取）開著時，把長按手指的觸控轉給小鍵盤並回傳 true；
     * 其他情況回傳 false，由鍵盤自己處理。
     */
    boolean forwardTouchEvent(MotionEvent me) {
        if (mMiniKeyboard != null && (!isLargeScreen || mMiniKeyboard.getKeyboard().getKeys().size() == 1)) { // Jeremy
                                                                                                              // enable
                                                                                                              // fling
                                                                                                              // when
                                                                                                              // popup
                                                                                                              // keyboard
                                                                                                              // only
                                                                                                              // has 1
                                                                                                              // key
                                                                                                              // '12,5,20
            final int action = me.getActionMasked();
            final int pointerCount = me.getPointerCount();
            final long eventTime = me.getEventTime();
            final int miniKeyboardPointerIndex = me.findPointerIndex(mMiniKeyboardTrackerId);
            if (miniKeyboardPointerIndex >= 0 && miniKeyboardPointerIndex < pointerCount) {
                final int miniKeyboardX = (int) me.getX(miniKeyboardPointerIndex);
                final int miniKeyboardY = (int) me.getY(miniKeyboardPointerIndex);
                MotionEvent translated = generateMiniKeyboardMotionEvent(action,
                        miniKeyboardX, miniKeyboardY, eventTime);
                mMiniKeyboard.onTouchEvent(translated);
                translated.recycle();
            }
            return true;
        }
        return false;
    }

    void dismissPopupKeyboard() {
        if (mMiniKeyboardPopup.isShowing()) {
            mMiniKeyboardPopup.dismiss();
            mMiniKeyboard = null;
            mMiniKeyboardOriginX = 0;
            mMiniKeyboardOriginY = 0;
            mHost.invalidateAllKeys();
        }
    }

    boolean handleBack() {
        if (mMiniKeyboardPopup.isShowing()) {
            dismissPopupKeyboard();
            return true;
        }
        return false;
    }
}
