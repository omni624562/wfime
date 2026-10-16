package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Compose 版鍵盤與舊鍵盤（LIMEKeyboardView）比對：樣式、尺寸、點鍵與長按送出的事件要相同。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ComposeKeyboardViewTest {

    private Context context;
    private LIMEKeyboardView oldView;
    private ComposeKeyboardView newView;

    @Before
    public void setup() {
        context = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.LIMETheme_Material3);
        oldView = (LIMEKeyboardView) LayoutInflater.from(context).inflate(R.layout.input, null);
        newView = new ComposeKeyboardView(context);
    }

    private static Object baseField(Object o, String name) throws Exception {
        Field f = LIMEKeyboardBaseView.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    /** 同一個資源載入的 Drawable 是不同物件，比對類別、內距與大小。 */
    private static Object sameDrawable(Object d) {
        if (!(d instanceof Drawable)) return d;
        Drawable dr = (Drawable) d;
        android.graphics.Rect pad = new android.graphics.Rect();
        dr.getPadding(pad);
        return dr.getClass().getName() + " " + pad + " " + dr.getIntrinsicWidth() + "x" + dr.getIntrinsicHeight();
    }

    @Test
    public void sameStyleAsOldKeyboard() throws Exception {
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
        assertNotNull(newView.painter.mActionKeyBackground);
        assertEquals((float) baseField(oldView, "mKeyHysteresisDistance"), newView.keyHysteresisDistance, 0f);
        assertEquals((float) baseField(oldView, "mVerticalCorrection"), newView.verticalCorrection, 0f);
        assertEquals(sameDrawable(oldView.getBackground()), sameDrawable(newView.getBackground()));
        assertEquals(oldView.getPaddingLeft(), newView.getPaddingLeft());
        assertEquals(oldView.getPaddingTop(), newView.getPaddingTop());
        assertEquals(oldView.getPaddingRight(), newView.getPaddingRight());
        assertEquals(oldView.getPaddingBottom(), newView.getPaddingBottom());
    }

    private LIMEKeyboard keyboard() {
        return new LIMEKeyboard(context, R.xml.lime, 0, 1f, 0, 0, 1080);
    }

    @Test
    public void sameSizeAsOldKeyboard() {
        oldView.setKeyboard(keyboard());
        newView.setKeyboard(keyboard());
        int w = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST);
        int h = View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST);
        oldView.measure(w, h);
        newView.measure(w, h);
        assertTrue(oldView.getMeasuredHeight() > 100);
        assertEquals(oldView.getMeasuredWidth(), newView.getMeasuredWidth());
        assertEquals(oldView.getMeasuredHeight(), newView.getMeasuredHeight());
        // 可用寬度比鍵盤窄時縮到可用寬度
        w = View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.AT_MOST);
        oldView.measure(w, h);
        newView.measure(w, h);
        assertEquals(900, oldView.getMeasuredWidth());
        assertEquals(oldView.getMeasuredWidth(), newView.getMeasuredWidth());
    }

    /** 記下鍵盤送出的事件。 */
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

    private static Key key(LIMEBaseKeyboard kb, int code) {
        for (Key k : kb.getKeys())
            if (k.codes[0] == code) return k;
        throw new AssertionError("no key " + code);
    }

    private static void touch(View v, int action, Key k, long down) {
        MotionEvent e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                k.x + v.getPaddingLeft() + k.width / 2f, k.y + v.getPaddingTop() + k.height / 2f, 0);
        v.onTouchEvent(e);
        e.recycle();
    }

    /** 在鍵上按下、等 holdMs、放開，回傳送出的事件。 */
    private List<String> press(ImeKeyboardView view, int code, long holdMs) {
        Recorder r = new Recorder();
        view.setOnKeyboardActionListener(r);
        view.setKeyboard(keyboard());
        Key k = key(view.getKeyboard(), code);
        long down = SystemClock.uptimeMillis();
        touch(view.asView(), MotionEvent.ACTION_DOWN, k, down);
        ShadowLooper.idleMainLooper(holdMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        touch(view.asView(), MotionEvent.ACTION_UP, k, down);
        ShadowLooper.idleMainLooper();
        return r.events;
    }

    @Test
    public void tapSendsSameEventsAsOldKeyboard() {
        List<String> old = press(oldView, 'a', 50);
        assertTrue(old.contains("key " + (int) 'a'));
        assertEquals(old, press(newView, 'a', 50));
    }

    @Test
    public void longPressSpaceSendsSameEventsAsOldKeyboard() {
        List<String> old = press(oldView, LIMEKeyboard.KEYCODE_SPACE, 1500);
        assertTrue(old.toString(), old.contains("key " + LIMEKeyboardView.KEYCODE_SPACE_LONGPRESS));
        assertEquals(old, press(newView, LIMEKeyboard.KEYCODE_SPACE, 1500));
    }

    @Test
    public void holdingDeleteRepeatsLikeOldKeyboard() {
        List<String> old = press(oldView, LIMEBaseKeyboard.KEYCODE_DELETE, 1500);
        assertTrue(old.toString(), old.stream().filter(s -> s.equals("key " + LIMEBaseKeyboard.KEYCODE_DELETE)).count() > 3);
        assertEquals(old, press(newView, LIMEBaseKeyboard.KEYCODE_DELETE, 1500));
    }
}
