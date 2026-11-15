package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * 分割鍵盤：第一排 10 個鍵左右各 5 個，有鍵距的版面（注音、英文、符號）也一樣。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SplitKeyboardTest {

    private static void assertFiveFive(int layout) {
        Context c = ApplicationProvider.getApplicationContext();
        LIMEBaseKeyboard kb = new LIMEBaseKeyboard(c, layout, 0, 1f, 0, LIMEBaseKeyboard.SPLIT_KEYBOARD_ALWAYS, 2560);
        int firstRowY = kb.getKeys().get(0).y, left = 0, right = 0;
        for (LIMEBaseKeyboard.Key k : kb.getKeys()) {
            if (k.y != firstRowY) continue;
            if (k.x + k.width <= 1280) left++;
            else if (k.x >= 1280) right++;
        }
        assertEquals("left", 5, left);
        assertEquals("right", 5, right);
    }

    @Test
    public void dayi() { assertFiveFive(R.xml.lime_dayi); }

    @Test
    public void phonetic() { assertFiveFive(R.xml.lime_phonetic); }

    @Test
    public void english() { assertFiveFive(R.xml.lime); }

    @Test
    public void symbols() { assertFiveFive(R.xml.symbols1); }
}
