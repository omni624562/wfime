package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * 按鍵標籤過寬時自動縮字（LIMEKeyboardBaseView.fitLabelTextSize）。
 * 數值取自 Pixel 11 Pro（1280px、480dpi）：切換鍵 10%p＝128px，扣 3dp 間距約 119px，
 * 留邊 90% 後可用約 107px；多字元標籤字級 17dp＝51px。文字寬度為 Roboto 粗估值
 * （Robolectric 4.11.1 在 Windows 無 native graphics，無法量真實字型）。
 * 文字寬度與字級成正比，縮字後寬度＝原寬度 × 新字級 / 原字級。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LIMEKeyboardBaseViewLabelFitTest {

    private static final float AVAILABLE = 119 * 0.9f;
    private static final int LABEL_SIZE = 51;

    private static float widthAfter(float textWidth, int oldSize, int newSize) {
        return textWidth * newSize / oldSize;
    }

    @Test
    public void testChuyinShrinksToFitSwitchKey() {
        float chuyin = 155f;
        int fitted = LIMEKeyboardBaseView.fitLabelTextSize(LABEL_SIZE, chuyin, AVAILABLE);
        assertTrue("字級應縮小", fitted < LABEL_SIZE);
        assertTrue("縮字後應放得下", widthAfter(chuyin, LABEL_SIZE, fitted) <= AVAILABLE);
    }

    @Test
    public void testChineseFallbackShrinksToFitSwitchKey() {
        float chinese = 181f;
        int fitted = LIMEKeyboardBaseView.fitLabelTextSize(LABEL_SIZE, chinese, AVAILABLE);
        assertTrue(fitted < LABEL_SIZE);
        assertTrue(widthAfter(chinese, LABEL_SIZE, fitted) <= AVAILABLE);
    }

    @Test
    public void testLabelThatFitsKeepsSize() {
        // Dayi 約 99px、單一中文字「易」約 51px，都放得下，不應改變字級
        assertEquals(LABEL_SIZE, LIMEKeyboardBaseView.fitLabelTextSize(LABEL_SIZE, 99f, AVAILABLE));
        assertEquals(LABEL_SIZE, LIMEKeyboardBaseView.fitLabelTextSize(LABEL_SIZE, 51f, AVAILABLE));
        assertEquals(LABEL_SIZE, LIMEKeyboardBaseView.fitLabelTextSize(LABEL_SIZE, AVAILABLE, AVAILABLE));
    }

    @Test
    public void testLargerFontSettingStillFits() {
        // 使用者把字體調大（例如 1.2 倍）時，Dayi 也可能超出，同樣要縮到放得下
        int size = (int) (LABEL_SIZE * 1.2f);
        for (float width : new float[]{99f * 1.2f, 155f * 1.2f, 181f * 1.2f}) {
            int fitted = LIMEKeyboardBaseView.fitLabelTextSize(size, width, AVAILABLE);
            assertTrue(widthAfter(width, size, fitted) <= AVAILABLE);
        }
    }

    @Test
    public void testNoAvailableWidthKeepsSize() {
        assertEquals(LABEL_SIZE, LIMEKeyboardBaseView.fitLabelTextSize(LABEL_SIZE, 155f, 0f));
        assertEquals(LABEL_SIZE, LIMEKeyboardBaseView.fitLabelTextSize(LABEL_SIZE, 155f, -5f));
    }
}
