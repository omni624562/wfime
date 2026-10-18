package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.R;
import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 長按小鍵盤改用 ComposeKeyboardView（A4-7②）：與原本的 LIMEKeyboardBaseView 小鍵盤比對樣式、尺寸、點鍵事件。
 * 舊小鍵盤照原本 keyboard_popup.xml 的屬性與 MiniKeyboardPopup 的設定建立。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MiniKeyboardViewTest {

    private Context context;
    private LIMEKeyboardBaseView oldView;
    private ComposeKeyboardView newView;

    @Before
    public void setup() {
        context = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.LIMETheme_Material3);
        float allowance = context.getResources().getDimension(R.dimen.mini_keyboard_slide_allowance);
        oldView = new LIMEKeyboardBaseView(context, Robolectric.buildAttributeSet()
                .setStyleAttribute("?attr/LIMEPopupKeyboardLayout")
                .addAttribute(R.attr.keyHysteresisDistance, "0dip")
                .addAttribute(R.attr.verticalCorrection, "@dimen/mini_keyboard_vertical_correction")
                .build());
        oldView.mKeyDetector = new MiniKeyboardKeyDetector(allowance);
        oldView.mSwipeGestures = null;
        newView = LayoutInflater.from(context).inflate(R.layout.keyboard_popup, null).findViewById(R.id.LIMEPopupKeyboard);
        newView.setupAsMiniKeyboard(allowance);
    }

    private static Object baseField(Object o, String name) throws Exception {
        Field f = LIMEKeyboardBaseView.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    private static Object sameDrawable(Object d) {
        if (!(d instanceof Drawable)) return d;
        Drawable dr = (Drawable) d;
        android.graphics.Rect pad = new android.graphics.Rect();
        dr.getPadding(pad);
        return dr.getClass().getName() + " " + pad + " " + dr.getIntrinsicWidth() + "x" + dr.getIntrinsicHeight();
    }

    @Test
    public void sameStyleAsOldMiniKeyboard() throws Exception {
        int compared = 0;
        for (Field f : KeyPainter.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers()))
                continue;
            f.setAccessible(true);
            Object o = f.get(oldView.mPainter), n = f.get(newView.painter);
            if (f.getType() == android.graphics.Paint.class) {
                assertEquals(((android.graphics.Paint) o).getTextSize(), ((android.graphics.Paint) n).getTextSize(), 0f);
            } else {
                assertEquals(f.getName(), sameDrawable(o), sameDrawable(n));
            }
            compared++;
        }
        assertTrue(compared >= 19);
        assertEquals((float) baseField(oldView, "mKeyHysteresisDistance"), newView.keyHysteresisDistance, 0f);
        assertEquals((float) baseField(oldView, "mVerticalCorrection"), newView.verticalCorrection, 0f);
        assertEquals(0f, newView.keyHysteresisDistance, 0f);
        assertEquals(sameDrawable(oldView.getBackground()), sameDrawable(newView.getBackground()));
        assertEquals(oldView.getPaddingLeft(), newView.getPaddingLeft());
        assertEquals(oldView.getPaddingTop(), newView.getPaddingTop());
        assertEquals(oldView.getPaddingRight(), newView.getPaddingRight());
        assertEquals(oldView.getPaddingBottom(), newView.getPaddingBottom());
    }

    /** 「.」鍵的標點小鍵盤（同 MiniKeyboardPopup 建法）。 */
    private LIMEBaseKeyboard popupKeyboard() {
        return new LIMEBaseKeyboard(context, R.xml.popup_c_punctuation, 1f, 0, 0);
    }

    @Test
    public void sameSizeAsOldMiniKeyboard() {
        oldView.setKeyboard(popupKeyboard());
        newView.setKeyboard(popupKeyboard());
        int w = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST);
        int h = View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST);
        oldView.measure(w, h);
        newView.measure(w, h);
        assertTrue(oldView.getMeasuredHeight() > 50);
        assertEquals(oldView.getMeasuredWidth(), newView.getMeasuredWidth());
        assertEquals(oldView.getMeasuredHeight(), newView.getMeasuredHeight());
    }

    private static class Recorder implements LIMEKeyboardBaseView.OnKeyboardActionListener {
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

    /** 點小鍵盤上每一個鍵，回傳送出的事件。 */
    private List<String> tapAll(ImeKeyboardView view) {
        Recorder r = new Recorder();
        view.setOnKeyboardActionListener(r);
        view.setKeyboard(popupKeyboard());
        View v = view.asView();
        for (Key k : view.getKeyboard().getKeys()) {
            float x = k.x + v.getPaddingLeft() + k.width / 2f, y = k.y + v.getPaddingTop() + k.height / 2f;
            long t = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0);
            v.onTouchEvent(down);
            down.recycle();
            MotionEvent up = MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, x, y, 0);
            v.onTouchEvent(up);
            up.recycle();
            ShadowLooper.idleMainLooper();
        }
        return r.events;
    }

    @Test
    public void tappingEveryKeySendsSameEventsAsOldMiniKeyboard() {
        List<String> old = tapAll(oldView);
        assertTrue(old.toString(), old.size() >= 3 * 18);
        assertEquals(old, tapAll(newView));
    }

    @Test
    public void miniKeyboardHasNoSwipe() {
        Recorder r = new Recorder();
        newView.setOnKeyboardActionListener(r);
        newView.setKeyboard(popupKeyboard());
        long t = SystemClock.uptimeMillis();
        newView.onTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 900, 40, 0));
        for (int i = 1; i <= 4; i++)
            newView.onTouchEvent(MotionEvent.obtain(t, t + 10 * i, MotionEvent.ACTION_MOVE, 900 - 200 * i, 42, 0));
        newView.onTouchEvent(MotionEvent.obtain(t, t + 40, MotionEvent.ACTION_UP, 100, 42, 0));
        ShadowLooper.idleMainLooper();
        assertTrue(r.events.toString(), r.events.stream().noneMatch(s -> s.startsWith("swipe")));
    }
}
