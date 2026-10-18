package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.R;
import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

/**
 * 長按小鍵盤（keyboard_popup.xml 的 ComposeKeyboardView，A4-7②）：樣式、尺寸、點鍵事件。
 * 預期值是原本 LIMEKeyboardBaseView 小鍵盤的結果（A4-7②a 與舊小鍵盤逐項比對過，②b 刪除舊類別前印出）。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MiniKeyboardViewTest {

    private Context context;
    private ComposeKeyboardView newView;

    @Before
    public void setup() {
        context = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.LIMETheme_Material3);
        newView = LayoutInflater.from(context).inflate(R.layout.keyboard_popup, null).findViewById(R.id.LIMEPopupKeyboard);
        newView.setupAsMiniKeyboard(context.getResources().getDimension(R.dimen.mini_keyboard_slide_allowance));
    }

    private static Object sameDrawable(Object d) {
        if (!(d instanceof Drawable)) return d;
        Drawable dr = (Drawable) d;
        android.graphics.Rect pad = new android.graphics.Rect();
        dr.getPadding(pad);
        return dr.getClass().getName() + " " + pad + " " + dr.getIntrinsicWidth() + "x" + dr.getIntrinsicHeight();
    }

    @Test
    public void styleFromPopupLayout() {
        Resources res = context.getResources();
        KeyPainter p = newView.painter;
        assertNotNull(p.mKeyBackground);
        // 小鍵盤樣式沒有動作鍵／功能鍵底（同原本小鍵盤）
        assertNull(p.mActionKeyBackground);
        assertNull(p.mFunctionalKeyBackground);
        assertEquals(res.getDimensionPixelSize(R.dimen.key_text_size_material3), p.mKeyTextSize);
        assertEquals(0f, newView.keyHysteresisDistance, 0f);
        assertEquals(res.getDimensionPixelOffset(R.dimen.mini_keyboard_vertical_correction), newView.verticalCorrection, 0f);
        assertEquals(sameDrawable(ContextCompat.getDrawable(context, R.drawable.keyboard_popup_panel_background_material3)),
                sameDrawable(newView.getBackground()));
        // 內距來自小鍵盤背景（GradientDrawable 的 4,4,4,4）
        assertEquals(4, newView.getPaddingLeft());
        assertEquals(4, newView.getPaddingTop());
        assertEquals(4, newView.getPaddingRight());
        assertEquals(4, newView.getPaddingBottom());
    }

    /** 「.」鍵的標點小鍵盤（同 MiniKeyboardPopup 建法）。 */
    private LIMEBaseKeyboard popupKeyboard() {
        return new LIMEBaseKeyboard(context, R.xml.popup_c_punctuation, 1f, 0, 0);
    }

    @Test
    public void sizeIsKeyboardPlusPadding() {
        newView.setKeyboard(popupKeyboard());
        newView.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST));
        LIMEBaseKeyboard kb = newView.getKeyboard();
        assertEquals(kb.getMinWidth() + 8, newView.getMeasuredWidth());
        assertEquals(kb.getHeight() + 8, newView.getMeasuredHeight());
        assertTrue(newView.getMeasuredHeight() > 50);
    }

    private static class Recorder implements OnKeyboardActionListener {
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

    @Test
    public void tappingEveryKeySendsPressKeyRelease() {
        Recorder r = new Recorder();
        newView.setOnKeyboardActionListener(r);
        newView.setKeyboard(popupKeyboard());
        List<String> expected = new ArrayList<>();
        for (Key k : newView.getKeyboard().getKeys()) {
            float x = k.x + newView.getPaddingLeft() + k.width / 2f, y = k.y + newView.getPaddingTop() + k.height / 2f;
            long t = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0);
            newView.onTouchEvent(down);
            down.recycle();
            MotionEvent up = MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, x, y, 0);
            newView.onTouchEvent(up);
            up.recycle();
            ShadowLooper.idleMainLooper();
            int c = k.codes[0];
            expected.add("press " + c);
            expected.add("key " + c);
            expected.add("release " + c);
        }
        assertEquals(18 * 3, expected.size());
        assertEquals(expected, r.events);
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
