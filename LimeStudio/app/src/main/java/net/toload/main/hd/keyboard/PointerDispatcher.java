package net.toload.main.hd.keyboard;

import android.util.Log;
import android.view.MotionEvent;

/**
 * 多指觸控的協調：快打時前一個鍵還沒放開就按下一個鍵，要照順序送出；按下或放開修飾鍵（Shift）時
 * 先放開其他手指；不支援獨立多點觸控的面板轉成單點。從 LIMEKeyboardBaseView.onTouchEvent 原樣搬出，
 * 不依賴 View，只吃座標與時間，可以直接寫單元測試（藍圖 A4-2）。
 */
final class PointerDispatcher {
    private static final String TAG = "LIMEKeyboardBaseView";
    private static final boolean DEBUG = false;

    /** 依手指編號取得（或建立）對應的 PointerTracker。 */
    interface TrackerProvider {
        PointerTracker getPointerTracker(int id);
    }

    private final LIMEKeyboardBaseView.PointerQueue mPointerQueue = new LIMEKeyboardBaseView.PointerQueue();
    private final KeyTimers mHandler;
    private final TrackerProvider mTrackers;

    PointerDispatcher(KeyTimers timers, TrackerProvider trackers) {
        mHandler = timers;
        mTrackers = trackers;
    }

    LIMEKeyboardBaseView.PointerQueue queue() {
        return mPointerQueue;
    }

    /**
     * 處理一個觸控事件（已排除手勢、長按小鍵盤等 View 自己處理的情況）。
     * pointerIds／pointerXs／pointerYs 是事件裡每個手指的編號與座標（ACTION_MOVE 用）。
     */
    boolean dispatch(int action, int pointerCount, int oldPointerCount, boolean hasDistinctMultitouch,
            int id, int x, int y, long eventTime, int[] pointerIds, int[] pointerXs, int[] pointerYs) {
        if (mHandler.isInKeyRepeat()) {
            // It will keep being in the key repeating mode while the key is being pressed.
            if (action == MotionEvent.ACTION_MOVE) {
                return true;
            }
            final PointerTracker tracker = mTrackers.getPointerTracker(id);
            // Key repeating timer will be canceled if 2 or more keys are in action, and
            // current
            // event (UP or DOWN) is non-modifier key.
            if (pointerCount > 1 && !tracker.isModifier()) {
                mHandler.cancelKeyRepeatTimer();
            }
            // Up event will pass through.
        }

        // TODO: cleanup this code into a multi-touch to single-touch event converter
        // class?
        // Translate mutli-touch event to single-touch events on the device that has no
        // distinct
        // multi-touch panel.
        if (!hasDistinctMultitouch) {
            // Use only main (id=0) pointer tracker.
            PointerTracker tracker = mTrackers.getPointerTracker(0);
            if (pointerCount == 1 && oldPointerCount == 2) {
                // Multi-touch to single touch transition.
                // Send a down event for the latest pointer.
                tracker.onDownEvent(x, y, eventTime);
            } else if (pointerCount == 2 && oldPointerCount == 1) {
                // Single-touch to multi-touch transition.
                // Send an up event for the last pointer.
                tracker.onUpEvent(tracker.getLastX(), tracker.getLastY(), eventTime);
            } else if (pointerCount == 1 && oldPointerCount == 1) {
                tracker.onTouchEvent(action, x, y, eventTime);
            } else {
                Log.w(TAG, "Unknown touch panel behavior: pointer count is " + pointerCount
                        + " (old " + oldPointerCount + ")");
            }
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < pointerCount; i++) {
                PointerTracker tracker = mTrackers.getPointerTracker(pointerIds[i]);
                tracker.onMoveEvent(pointerXs[i], pointerYs[i], eventTime);
            }
        } else {
            PointerTracker tracker = mTrackers.getPointerTracker(id);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    onDownEvent(tracker, x, y, eventTime);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    onUpEvent(tracker, x, y, eventTime);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    onCancelEvent(tracker, x, y, eventTime);
                    break;
            }
        }

        return true;
    }

    private void onDownEvent(PointerTracker tracker, int x, int y, long eventTime) {
        if (DEBUG)
            Log.i(TAG, "onDownEvent() eventTime = " + eventTime);
        if (tracker.isOnModifierKey(x, y)) {
            // Before processing a down event of modifier key, all pointers already being
            // tracked
            // should be released.
            mPointerQueue.releaseAllPointersExcept(null, eventTime);
        }
        tracker.onDownEvent(x, y, eventTime);
        mPointerQueue.add(tracker);
    }

    private void onUpEvent(PointerTracker tracker, int x, int y, long eventTime) {
        if (DEBUG)
            Log.i(TAG, "onUpEvent() eventTime = " + eventTime);

        if (tracker.isModifier()) {
            // Before processing an up event of modifier key, all pointers already being
            // tracked
            // should be released.
            mPointerQueue.releaseAllPointersExcept(tracker, eventTime);
        } else {
            int index = mPointerQueue.lastIndexOf(tracker);
            if (index >= 0) {
                mPointerQueue.releaseAllPointersOlderThan(tracker, eventTime);
            } else {
                Log.w(TAG, "onUpEvent: corresponding down event not found for pointer "
                        + tracker.mPointerId);
            }
        }
        tracker.onUpEvent(x, y, eventTime);
        mPointerQueue.remove(tracker);
    }

    private void onCancelEvent(PointerTracker tracker, int x, int y, long eventTime) {
        if (DEBUG)
            Log.i(TAG, "onCancelEvent() eventTime = " + eventTime);

        tracker.onCancelEvent(x, y, eventTime);
        mPointerQueue.remove(tracker);
    }
}
