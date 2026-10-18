package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import net.toload.main.hd.R;
import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 長按小鍵盤（「.」鍵的標點面板）與滑動手勢（左滑刪字、下滑收鍵盤）。
 * 舊鍵盤（LIMEKeyboardView）在 A4-7 移除前，同樣的測試新舊鍵盤都跑過。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class KeyboardPopupSwipeTest {

    private Context context;
    private Activity activity;

    @Before
    public void setup() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        context = new ContextThemeWrapper(activity, R.style.LIMETheme_Material3);
    }

    /** 記下鍵盤送出的事件。 */
    static class Recorder implements LIMEKeyboardBaseView.OnKeyboardActionListener {
        final List<String> events = new ArrayList<>();
        public void onPress(int c) { events.add("press " + c); }
        public void onRelease(int c) { events.add("release " + c); }
        public void onKey(int c, int[] codes, int x, int y) { events.add("key " + c); }
        public void onText(CharSequence t) { events.add("text " + t); }
        public void onCancel() { events.add("cancel"); }
        public void swipeLeft() { events.add("swipeLeft"); }
        public void swipeRight() { events.add("swipeRight"); }
        public void swipeDown() { events.add("swipeDown"); }
        public void swipeUp() { events.add("swipeUp"); }
    }

    /** 放進 Activity 畫面並量好尺寸（小鍵盤要有視窗才能顯示）。 */
    private Recorder attach(ImeKeyboardView view) {
        Recorder r = new Recorder();
        view.setOnKeyboardActionListener(r);
        view.setKeyboard(new LIMEKeyboard(context, R.xml.lime, 0, 1f, 0, 0, 1080));
        if (view.asView().getParent() != null)
            ((ViewGroup) view.asView().getParent()).removeView(view.asView());
        FrameLayout root = new FrameLayout(activity);
        root.addView(view.asView(), new FrameLayout.LayoutParams(1080, ViewGroup.LayoutParams.WRAP_CONTENT));
        activity.setContentView(root, new ViewGroup.LayoutParams(1080, 2000));
        root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1080, 2000);
        ShadowLooper.idleMainLooper();
        return r;
    }

    private static Key key(ImeKeyboardView view, int code) {
        for (Key k : view.getKeyboard().getKeys())
            if (k.codes[0] == code) return k;
        throw new AssertionError("no key " + code);
    }

    private static void touch(View v, int action, float x, float y, long down, long at) {
        MotionEvent e = MotionEvent.obtain(down, at, action, x, y, 0);
        v.onTouchEvent(e);
        e.recycle();
    }

    private static float cx(View v, Key k) { return k.x + v.getPaddingLeft() + k.width / 2f; }

    private static float cy(View v, Key k) { return k.y + v.getPaddingTop() + k.height / 2f; }

    private void longPressDotOpensPopupAndPopupKeySends(ImeKeyboardView view) {
        Recorder r = attach(view);
        View v = view.asView();
        Key dot = key(view, '.');
        assertTrue(dot.popupResId != 0);
        long down = SystemClock.uptimeMillis();
        touch(v, MotionEvent.ACTION_DOWN, cx(v, dot), cy(v, dot), down, down);
        ShadowLooper.idleMainLooper(600, TimeUnit.MILLISECONDS);
        touch(v, MotionEvent.ACTION_UP, cx(v, dot), cy(v, dot), down, SystemClock.uptimeMillis());
        ShadowLooper.idleMainLooper();
        // 長按開出小鍵盤，「.」本身不送出
        assertFalse(r.events.toString(), r.events.contains("key 46"));
        PopupWindow popup = ShadowApplication.getInstance().getLatestPopupWindow();
        assertNotNull(popup);
        assertTrue(popup.isShowing());

        // 點小鍵盤的第一個鍵：送出該鍵並關閉小鍵盤
        ComposeKeyboardView mini = popup.getContentView().findViewById(R.id.LIMEPopupKeyboard);
        Key first = mini.getKeyboard().getKeys().get(0);
        r.events.clear();
        long t = SystemClock.uptimeMillis();
        touch(mini, MotionEvent.ACTION_DOWN, cx(mini, first), cy(mini, first), t, t);
        touch(mini, MotionEvent.ACTION_UP, cx(mini, first), cy(mini, first), t, t + 50);
        ShadowLooper.idleMainLooper();
        assertTrue(r.events.toString(), r.events.contains("key " + first.codes[0]));
        assertFalse(popup.isShowing());
        assertFalse(view.handleBack());
    }

    private void backClosesPopup(ImeKeyboardView view) {
        attach(view);
        View v = view.asView();
        Key dot = key(view, '.');
        long down = SystemClock.uptimeMillis();
        touch(v, MotionEvent.ACTION_DOWN, cx(v, dot), cy(v, dot), down, down);
        ShadowLooper.idleMainLooper(600, TimeUnit.MILLISECONDS);
        PopupWindow popup = ShadowApplication.getInstance().getLatestPopupWindow();
        assertTrue(popup.isShowing());
        assertTrue(view.handleBack());
        assertFalse(popup.isShowing());
        assertFalse(view.handleBack());
    }

    /** 從 (x0,y0) 快速滑到 (x1,y1)。 */
    private List<String> fling(ImeKeyboardView view, float x0, float y0, float x1, float y1) {
        Recorder r = attach(view);
        View v = view.asView();
        long t = SystemClock.uptimeMillis();
        touch(v, MotionEvent.ACTION_DOWN, x0, y0, t, t);
        for (int i = 1; i <= 4; i++)
            touch(v, MotionEvent.ACTION_MOVE, x0 + (x1 - x0) * i / 4, y0 + (y1 - y0) * i / 4, t, t + 10 * i);
        touch(v, MotionEvent.ACTION_UP, x1, y1, t, t + 40);
        ShadowLooper.idleMainLooper();
        return r.events;
    }

    private void swipes(ImeKeyboardView view) {
        View v = view.asView();
        List<String> left = fling(view, 1000, 200, 50, 210);
        assertTrue(left.toString(), left.contains("swipeLeft"));
        assertFalse(left.toString(), left.stream().anyMatch(s -> s.startsWith("key ")));

        List<String> down = fling(view, 500, 20, 510, v.getHeight() - 5);
        assertTrue(down.toString(), down.contains("swipeDown"));

        List<String> right = fling(view, 50, 200, 1000, 210);
        assertTrue(right.toString(), right.contains("swipeRight"));
    }

    ImeKeyboardView composeKeyboard() {
        return new ComposeKeyboardView(context);
    }

    @Test
    public void composeKeyboardLongPressPopup() {
        longPressDotOpensPopupAndPopupKeySends(composeKeyboard());
    }

    @Test
    public void composeKeyboardBackClosesPopup() {
        backClosesPopup(composeKeyboard());
    }

    @Test
    public void composeKeyboardSwipes() {
        swipes(composeKeyboard());
    }
}
