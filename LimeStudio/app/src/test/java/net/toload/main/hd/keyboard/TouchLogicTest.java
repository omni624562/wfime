package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.MotionEvent;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.R;
import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 觸控判斷（PointerTracker、PointerDispatcher）不經過鍵盤 View，直接用真的英文鍵盤版面（res/xml/lime.xml）、
 * 假計時器與事件紀錄器驗證：點擊、鄰近鍵、滑動換鍵、小幅晃動、長按、連續刪除、快打多指、Shift、單點面板。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TouchLogicTest {

    private static final int REPEAT_DELAY = 400;
    private static final int LONG_PRESS = 400;
    private static final int MULTI_TAP = 800;
    private static final int HYSTERESIS = 20;

    private final List<String> events = new ArrayList<>();
    private final List<String> timerCalls = new ArrayList<>();
    private Key[] keys;
    private ProximityKeyDetector detector;
    private boolean inKeyRepeat;

    private final KeyTimers timers = new KeyTimers() {
        @Override
        public void startKeyRepeatTimer(long delay, int keyIndex, PointerTracker tracker) {
            inKeyRepeat = true;
            timerCalls.add("repeat " + delay + " " + label(keyIndex));
        }

        @Override
        public void cancelKeyRepeatTimer() {
            inKeyRepeat = false;
            timerCalls.add("cancelRepeat");
        }

        @Override
        public boolean isInKeyRepeat() {
            return inKeyRepeat;
        }

        @Override
        public void startLongPressTimer(long delay, int keyIndex, PointerTracker tracker) {
            timerCalls.add("long " + delay + " " + label(keyIndex));
        }

        @Override
        public void cancelLongPressTimer() {
            timerCalls.add("cancelLong");
        }

        @Override
        public void cancelKeyTimers() {
            inKeyRepeat = false;
            timerCalls.add("cancelAll");
        }
    };

    private final PointerTracker.UIProxy proxy = new PointerTracker.UIProxy() {
        @Override
        public void invalidateKey(Key key) {
        }

        @Override
        public void showPreview(int keyIndex, PointerTracker tracker) {
        }

        @Override
        public boolean hasDistinctMultitouch() {
            return true;
        }
    };

    private final OnKeyboardActionListener listener = new OnKeyboardActionListener() {
        @Override
        public void onPress(int primaryCode) {
            events.add("press " + name(primaryCode));
        }

        @Override
        public void onRelease(int primaryCode) {
            events.add("release " + name(primaryCode));
        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes, int x, int y) {
            events.add("key " + name(primaryCode));
        }

        @Override
        public void onText(CharSequence text) {
            events.add("text " + text);
        }

        @Override
        public void onCancel() {
            events.add("cancel");
        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void swipeUp() {
        }
    };

    private static String name(int code) {
        if (code == LIMEBaseKeyboard.KEYCODE_SHIFT) return "SHIFT";
        if (code == LIMEBaseKeyboard.KEYCODE_DELETE) return "DEL";
        if (code == ' ') return "SPACE";
        return code > 0 ? String.valueOf((char) code) : String.valueOf(code);
    }

    private String label(int keyIndex) {
        return keyIndex >= 0 && keyIndex < keys.length ? name(keys[keyIndex].codes[0]) : "none";
    }

    private Key key(int code) {
        for (Key k : keys) if (k.codes[0] == code) return k;
        throw new AssertionError("no key " + code);
    }

    private int index(int code) {
        return Arrays.asList(keys).indexOf(key(code));
    }

    private static int cx(Key k) {
        return k.x + k.width / 2;
    }

    private static int cy(Key k) {
        return k.y + k.height / 2;
    }

    private PointerTracker tracker(int id) {
        PointerTracker t = new PointerTracker(id, timers, detector, proxy, REPEAT_DELAY, LONG_PRESS, MULTI_TAP);
        t.setKeyboard(keys, HYSTERESIS);
        t.setOnKeyboardActionListener(listener);
        return t;
    }

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        LIMEKeyboard keyboard = new LIMEKeyboard(context, R.xml.lime, 0, 1f, 0, 0, 1080);
        detector = new ProximityKeyDetector();
        keys = detector.setKeyboard(keyboard, 0, 0);
        detector.setProximityCorrectionEnabled(true);
        // 和 LIMEKeyboardBaseView.computeProximityThreshold 同樣算法
        int sum = 0;
        int verticalGap = (int) context.getResources().getDimension(R.dimen.key_bottom_gap);
        for (Key k : keys) sum += Math.min(k.width, k.height + verticalGap) + k.gap;
        detector.setProximityThreshold((int) (sum * 1.4f / keys.length));
        assertTrue(keys.length > 20);
    }

    // ---------- 單指 ----------

    @Test
    public void tapSendsPressKeyRelease() {
        PointerTracker t = tracker(0);
        Key a = key('a');
        t.onDownEvent(cx(a), cy(a), 100);
        t.onUpEvent(cx(a), cy(a), 150);
        assertEquals(Arrays.asList("press a", "key a", "release a"), events);
        assertEquals(Arrays.asList("long " + LONG_PRESS + " a", "cancelAll"), timerCalls);
    }

    @Test
    public void touchInRowMarginBeforeFirstKeyHitsEdgeKey() {
        PointerTracker t = tracker(0);
        Key a = key('a');
        // 第二列左邊有留白（horizontalGap 5%p），按在留白上仍算最左邊的 a 鍵
        assertTrue(a.x > 10);
        t.onDownEvent(2, cy(a), 100);
        t.onUpEvent(2, cy(a), 150);
        assertEquals(Arrays.asList("press a", "key a", "release a"), events);
    }

    @Test
    public void slidingToAnotherKeySendsTheLastKey() {
        PointerTracker t = tracker(0);
        Key a = key('a'), d = key('d');
        t.onDownEvent(cx(a), cy(a), 100);
        t.onMoveEvent(cx(d), cy(d), 130);
        t.onUpEvent(cx(d), cy(d), 160);
        assertEquals(Arrays.asList("press a", "release a", "press d", "key d", "release d"), events);
        assertTrue(t.isInSlidingKeyInput() == false);
    }

    @Test
    public void smallWobbleAcrossEdgeStaysOnOriginalKey() {
        PointerTracker t = tracker(0);
        Key a = key('a'), s = key('s');
        t.onDownEvent(cx(a), cy(a), 100);
        // 滑進隔壁 s 鍵 5 像素：離 a 鍵邊緣小於遲滯距離，仍算 a
        int x = s.x + 5;
        t.onMoveEvent(x, cy(a), 120);
        t.onUpEvent(x, cy(a), 140);
        assertEquals(Arrays.asList("press a", "key a", "release a"), events);
    }

    @Test
    public void longPressTimerIsArmedOnDownAndCancelledOnUp() {
        PointerTracker t = tracker(0);
        Key a = key('a'), d = key('d');
        t.onDownEvent(cx(a), cy(a), 100);
        assertEquals("long " + LONG_PRESS + " a", timerCalls.get(0));
        // 滑到別的鍵重新計時
        t.onMoveEvent(cx(d), cy(d), 200);
        assertEquals("long " + LONG_PRESS + " d", timerCalls.get(timerCalls.size() - 1));
        t.onUpEvent(cx(d), cy(d), 300);
        assertEquals("cancelAll", timerCalls.get(timerCalls.size() - 1));
    }

    @Test
    public void deleteRepeatsWhileHeldAndSendsNothingOnUp() {
        PointerTracker t = tracker(0);
        Key del = key(LIMEBaseKeyboard.KEYCODE_DELETE);
        t.onDownEvent(cx(del), cy(del), 100);
        assertEquals(Arrays.asList("press DEL", "key DEL", "release DEL"), events);
        assertTrue(timerCalls.contains("repeat " + REPEAT_DELAY + " DEL"));
        t.repeatKey(index(LIMEBaseKeyboard.KEYCODE_DELETE));   // 計時器觸發
        t.onUpEvent(cx(del), cy(del), 900);
        assertEquals(Arrays.asList("press DEL", "key DEL", "release DEL", "key DEL", "release DEL"), events);
    }

    @Test
    public void touchOutsideAnyKeyCancels() {
        PointerTracker t = tracker(0);
        t.onDownEvent(-500, -500, 100);
        t.onUpEvent(-500, -500, 150);
        assertEquals(Arrays.asList("cancel"), events);
    }

    // ---------- 多指 ----------

    private final List<PointerTracker> trackers = new ArrayList<>();

    private PointerDispatcher dispatcher() {
        return new PointerDispatcher(timers, id -> {
            while (trackers.size() <= id) trackers.add(tracker(trackers.size()));
            return trackers.get(id);
        });
    }

    private static int[] ints(int... v) {
        return v;
    }

    @Test
    public void fastTypingSecondFingerUpSendsFirstKeyFirst() {
        PointerDispatcher d = dispatcher();
        Key a = key('a'), s = key('s');
        d.dispatch(MotionEvent.ACTION_DOWN, 1, 1, true, 0, cx(a), cy(a), 100, ints(0), ints(cx(a)), ints(cy(a)));
        d.dispatch(MotionEvent.ACTION_POINTER_DOWN, 2, 1, true, 1, cx(s), cy(s), 120,
                ints(0, 1), ints(cx(a), cx(s)), ints(cy(a), cy(s)));
        d.dispatch(MotionEvent.ACTION_POINTER_UP, 2, 2, true, 1, cx(s), cy(s), 140,
                ints(0, 1), ints(cx(a), cx(s)), ints(cy(a), cy(s)));
        d.dispatch(MotionEvent.ACTION_UP, 1, 2, true, 0, cx(a), cy(a), 160, ints(0), ints(cx(a)), ints(cy(a)));
        // a 先按，所以 s 放開時先把 a 送出；a 之後放開不再重複送
        assertEquals(Arrays.asList("press a", "press s", "key a", "release a", "key s", "release s"), events);
    }

    @Test
    public void holdingShiftWhileTypingKeepsShiftUntilItIsReleased() {
        PointerDispatcher d = dispatcher();
        Key shift = key(LIMEBaseKeyboard.KEYCODE_SHIFT), a = key('a');
        d.dispatch(MotionEvent.ACTION_DOWN, 1, 1, true, 0, cx(shift), cy(shift), 100,
                ints(0), ints(cx(shift)), ints(cy(shift)));
        d.dispatch(MotionEvent.ACTION_POINTER_DOWN, 2, 1, true, 1, cx(a), cy(a), 120,
                ints(0, 1), ints(cx(shift), cx(a)), ints(cy(shift), cy(a)));
        d.dispatch(MotionEvent.ACTION_POINTER_UP, 2, 2, true, 1, cx(a), cy(a), 140,
                ints(0, 1), ints(cx(shift), cx(a)), ints(cy(shift), cy(a)));
        d.dispatch(MotionEvent.ACTION_UP, 1, 2, true, 0, cx(shift), cy(shift), 160,
                ints(0), ints(cx(shift)), ints(cy(shift)));
        assertEquals(Arrays.asList("press SHIFT", "press a", "key a", "release a", "key SHIFT", "release SHIFT"),
                events);
    }

    @Test
    public void pressingShiftReleasesKeysAlreadyHeld() {
        PointerDispatcher d = dispatcher();
        Key shift = key(LIMEBaseKeyboard.KEYCODE_SHIFT), a = key('a');
        d.dispatch(MotionEvent.ACTION_DOWN, 1, 1, true, 0, cx(a), cy(a), 100, ints(0), ints(cx(a)), ints(cy(a)));
        d.dispatch(MotionEvent.ACTION_POINTER_DOWN, 2, 1, true, 1, cx(shift), cy(shift), 120,
                ints(0, 1), ints(cx(a), cx(shift)), ints(cy(a), cy(shift)));
        assertEquals(Arrays.asList("press a", "key a", "release a", "press SHIFT"), events);
    }

    @Test
    public void panelWithoutDistinctMultitouchTreatsSecondFingerAsRelease() {
        PointerDispatcher d = dispatcher();
        Key a = key('a'), s = key('s');
        d.dispatch(MotionEvent.ACTION_DOWN, 1, 1, false, 0, cx(a), cy(a), 100, ints(0), ints(cx(a)), ints(cy(a)));
        // 第二指按下：只支援單點的面板把它當成第一指放開
        d.dispatch(MotionEvent.ACTION_POINTER_DOWN, 2, 1, false, 1, cx(s), cy(s), 120,
                ints(0, 1), ints(cx(a), cx(s)), ints(cy(a), cy(s)));
        assertEquals(Arrays.asList("press a", "key a", "release a"), events);
        assertEquals(1, trackers.size());
    }

    @Test
    public void secondKeyStopsDeleteRepeat() {
        PointerDispatcher d = dispatcher();
        Key del = key(LIMEBaseKeyboard.KEYCODE_DELETE), a = key('a');
        d.dispatch(MotionEvent.ACTION_DOWN, 1, 1, true, 0, cx(del), cy(del), 100,
                ints(0), ints(cx(del)), ints(cy(del)));
        assertTrue(inKeyRepeat);
        // 連續刪除中，移動事件不處理
        d.dispatch(MotionEvent.ACTION_MOVE, 1, 1, true, 0, cx(del) + 3, cy(del), 110,
                ints(0), ints(cx(del) + 3), ints(cy(del)));
        d.dispatch(MotionEvent.ACTION_POINTER_DOWN, 2, 1, true, 1, cx(a), cy(a), 120,
                ints(0, 1), ints(cx(del), cx(a)), ints(cy(del), cy(a)));
        assertFalse(inKeyRepeat);
        assertTrue(timerCalls.contains("cancelRepeat"));
        assertNotNull(d.queue());
    }
}
