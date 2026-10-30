package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compose 版鍵盤：樣式、尺寸、點鍵與長按送出的事件。
 * 預期值是舊鍵盤（LIMEKeyboardView，A4-7 移除）的結果，移除前與新舊比對一起跑過、確認相同。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ComposeKeyboardViewTest {

    private Context context;
    private ComposeKeyboardView newView;

    @Before
    public void setup() {
        context = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.LIMETheme_Material3);
        newView = new ComposeKeyboardView(context);
    }

    @Test
    public void styleFromTheme() {
        Resources res = context.getResources();
        KeyPainter p = newView.painter;
        assertNotNull(p.mKeyBackground);
        assertNotNull(p.mActionKeyBackground);
        assertNotNull(p.mFunctionalKeyBackground);
        assertNotNull(p.mPopupHint);
        assertNotNull(p.mPaint);
        assertNotNull(p.mPadding);
        assertEquals(res.getDimensionPixelSize(R.dimen.key_text_size_material3), p.mKeyTextSize);
        assertEquals(res.getDimensionPixelOffset(R.dimen.key_hysteresis_distance), newView.keyHysteresisDistance, 0f);
        assertEquals(res.getDimensionPixelOffset(R.dimen.keyboard_vertical_correction), newView.verticalCorrection, 0f);
        assertNotNull(newView.getBackground());
        assertEquals(0, newView.getPaddingLeft());
        assertEquals(0, newView.getPaddingTop());
        assertEquals(0, newView.getPaddingRight());
        assertEquals(0, newView.getPaddingBottom());
    }

    private LIMEKeyboard keyboard() {
        return new LIMEKeyboard(context, R.xml.lime, 0, 1f, 0, 0, 1080);
    }

    @Test
    public void sizeFollowsKeyboard() {
        newView.setKeyboard(keyboard());
        int h = View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST);
        newView.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.AT_MOST), h);
        assertEquals(1080, newView.getMeasuredWidth());
        assertEquals(newView.getKeyboard().getHeight(), newView.getMeasuredHeight());
        assertTrue(newView.getMeasuredHeight() > 100);
        // 可用寬度比鍵盤窄時縮到可用寬度
        newView.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.AT_MOST), h);
        assertEquals(900, newView.getMeasuredWidth());
    }

    @Test
    public void narrowKeyboardIsCentered() {
        // 平板橫放：鍵盤用較窄的寬度排版，整排寬度不變、左右補內距置中，點右邊的鍵送出那個鍵
        Recorder r = new Recorder();
        newView.setOnKeyboardActionListener(r);
        newView.setKeyboard(new LIMEKeyboard(context, R.xml.lime, 0, 1f, 0, 0, 600));
        int h = View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST);
        newView.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), h);
        int side = (1080 - newView.getKeyboard().getMinWidth()) / 2;
        assertTrue(side > 200);
        assertEquals(side, newView.getPaddingLeft());
        assertEquals(side, newView.getPaddingRight());
        assertEquals(1080, newView.getMeasuredWidth());
        Key p = key(newView.getKeyboard(), 'p');
        long down = SystemClock.uptimeMillis();
        touch(newView, MotionEvent.ACTION_DOWN, p, down);
        touch(newView, MotionEvent.ACTION_UP, p, down);
        ShadowLooper.idleMainLooper();
        assertEquals(Arrays.asList("press 112", "key 112", "release 112"), r.events);
        // 換回滿版鍵盤時內距回到 0
        newView.setKeyboard(keyboard());
        newView.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY), h);
        assertEquals(0, newView.getPaddingLeft());
        assertEquals(0, newView.getPaddingRight());
    }

    /** 記下鍵盤送出的事件。 */
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
    public void tapSendsPressKeyRelease() {
        assertEquals(Arrays.asList("press 97", "key 97", "release 97"), press(newView, 'a', 50));
    }

    @Test
    public void longPressSpaceSendsSpaceLongPress() {
        List<String> e = press(newView, LIMEKeyboard.KEYCODE_SPACE, 1500);
        assertTrue(e.toString(), e.contains("key " + ComposeKeyboardView.KEYCODE_SPACE_LONGPRESS));
    }

    @Test
    public void holdingDeleteRepeats() {
        List<String> e = press(newView, LIMEBaseKeyboard.KEYCODE_DELETE, 1500);
        assertTrue(e.toString(), e.stream().filter(s -> s.equals("key " + LIMEBaseKeyboard.KEYCODE_DELETE)).count() > 3);
    }
}
