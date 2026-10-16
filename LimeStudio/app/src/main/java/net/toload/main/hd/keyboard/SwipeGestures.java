package net.toload.main.hd.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import net.toload.main.hd.R;

/**
 * 鍵盤上的快速滑動手勢（左滑刪字、右滑選字、下滑收鍵盤）。
 * 從 LIMEKeyboardBaseView 原樣搬出，新舊鍵盤共用（藍圖 A4-4）。
 */
final class SwipeGestures {

    /** 滑動的對象：鍵盤 View 與收到手勢時的動作。 */
    interface Host {
        View view();

        void swipeLeft();

        void swipeRight();

        void swipeUp();

        void swipeDown();
    }

    private final GestureDetector mGestureDetector;
    private final SwipeTracker mSwipeTracker = new SwipeTracker();
    private final int mSwipeThreshold;
    private final boolean mDisambiguateSwipe;

    SwipeGestures(Context context, Host host) {
        final Resources res = context.getResources();
        mSwipeThreshold = (int) (500 * res.getDisplayMetrics().density);
        // TODO: Refer frameworks/base/core/res/res/values/config.xml
        mDisambiguateSwipe = res.getBoolean(R.bool.config_swipeDisambiguation);

        GestureDetector.SimpleOnGestureListener listener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent me1, MotionEvent me2, float velocityX,
                    float velocityY) {
                final float absX = Math.abs(velocityX);
                final float absY = Math.abs(velocityY);
                float deltaX = me2.getX() - me1.getX();
                float deltaY = me2.getY() - me1.getY();
                int travelX = host.view().getWidth() / 2; // Half the keyboard width
                int travelY = host.view().getHeight() / 2; // Half the keyboard height
                mSwipeTracker.computeCurrentVelocity(1000);
                final float endingVelocityX = mSwipeTracker.getXVelocity();
                final float endingVelocityY = mSwipeTracker.getYVelocity();
                if (velocityX > mSwipeThreshold && absY < absX && deltaX > travelX) {
                    if (mDisambiguateSwipe && endingVelocityX >= velocityX / 4) {
                        host.swipeRight();
                        return true;
                    }
                } else if (velocityX < -mSwipeThreshold && absY < absX && deltaX < -travelX) {
                    if (mDisambiguateSwipe && endingVelocityX <= velocityX / 4) {
                        host.swipeLeft();
                        return true;
                    }
                } else if (velocityY < -mSwipeThreshold && absX < absY && deltaY < -travelY) {
                    if (mDisambiguateSwipe && endingVelocityY <= velocityY / 4) {
                        host.swipeUp();
                        return true;
                    }
                } else if (velocityY > mSwipeThreshold && absX < absY / 2 && deltaY > travelY) {
                    if (mDisambiguateSwipe && endingVelocityY >= velocityY / 4) {
                        host.swipeDown();
                        return true;
                    }
                }
                return false;
            }
        };

        final boolean ignoreMultitouch = true;
        mGestureDetector = new GestureDetector(context, listener, null, ignoreMultitouch);

        mGestureDetector.setIsLongpressEnabled(false);
    }

    /** 記下觸控軌跡（判斷手指離開時的速度用）。 */
    void addMovement(MotionEvent me) {
        mSwipeTracker.addMovement(me);
    }

    /** 這個觸控事件構成一次滑動手勢（已呼叫對應動作）就回傳 true。 */
    boolean onTouchEvent(MotionEvent me) {
        return mGestureDetector.onTouchEvent(me);
    }
}
