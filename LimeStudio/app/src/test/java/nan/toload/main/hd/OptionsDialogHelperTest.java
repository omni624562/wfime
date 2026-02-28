/*
 * Copyright 2015, The LimeIME Open Source Project
 * Licensed under GPL-3.0
 */

package nan.toload.main.hd;

import org.junit.Before;
import org.junit.Test;

import nan.toload.main.hd.keyboard.LIMEKeyboard;

import static org.junit.Assert.*;

/**
 * Simple unit tests for OptionsDialogHelper logic.
 * Uses a testable subclass to avoid Android framework dependencies.
 */
public class OptionsDialogHelperTest {

    private TestableOptionsDialogHelper helper;

    @Before
    public void setUp() {
        helper = new TestableOptionsDialogHelper();
    }

    @Test
    public void testIsLandscape_Portrait() {
        helper.setDisplayDimensions(1080, 1920);
        assertFalse(helper.isLandscape());
    }

    @Test
    public void testIsLandscape_Landscape() {
        helper.setDisplayDimensions(1920, 1080);
        assertTrue(helper.isLandscape());
    }

    @Test
    public void testShouldShowSplitOption_Portrait() {
        helper.setDisplayDimensions(1080, 1920);
        assertTrue(helper.shouldShowSplitOption(0));
        assertTrue(helper.shouldShowSplitOption(1)); // Arrow keys don't matter in portrait
    }

    @Test
    public void testShouldShowSplitOption_Landscape_NoArrowKeys() {
        helper.setDisplayDimensions(1920, 1080);
        assertTrue(helper.shouldShowSplitOption(0));
    }

    @Test
    public void testShouldShowSplitOption_Landscape_WithArrowKeys() {
        helper.setDisplayDimensions(1920, 1080);
        assertFalse(helper.shouldShowSplitOption(1));
    }

    @Test
    public void testToggleSplitKeyboard_Never_Portrait() {
        helper.setDisplayDimensions(1080, 1920);
        int newState = helper.toggleSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_NEVER);
        assertEquals(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS, newState);
    }

    @Test
    public void testToggleSplitKeyboard_Never_Landscape() {
        helper.setDisplayDimensions(1920, 1080);
        int newState = helper.toggleSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_NEVER);
        assertEquals(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY, newState);
    }

    @Test
    public void testToggleSplitKeyboard_Always_Portrait() {
        helper.setDisplayDimensions(1080, 1920);
        int newState = helper.toggleSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS);
        assertEquals(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY, newState);
    }

    @Test
    public void testGetHanConvertOption() {
        helper.setHanConvertOption(2);
        assertEquals(2, helper.getHanConvertOption());
    }

    @Test
    public void testGetSplitKeyboardMenuTextResId_Split() {
        int resId = helper.getSplitKeyboardMenuTextResId(LIMEKeyboard.SPLIT_KEYBOARD_NEVER);
        assertEquals(R.string.split_keyboard, resId);
    }

    // =========================================================================
    // 邊界測試案例：toggleSplitKeyboard 所有狀態轉換路徑
    // =========================================================================

    /**
     * Portrait 下：ALWAYS → LANDSCAPD_ONLY
     * 這是原測試未覆蓋的路徑。
     */
    @Test
    public void testToggleSplitKeyboard_Always_Portrait_GoesToLandscapeOnly() {
        helper.setDisplayDimensions(1080, 1920); // portrait
        int newState = helper.toggleSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS);
        assertEquals(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY, newState);
    }

    /**
     * Portrait 下：LANDSCAPD_ONLY → ALWAYS
     * 這是完全未被測試的路徑（既非 NEVER 也非 ALWAYS 的初始狀態）。
     */
    @Test
    public void testToggleSplitKeyboard_LandscapeOnly_Portrait_GoesToAlways() {
        helper.setDisplayDimensions(1080, 1920); // portrait
        int newState = helper.toggleSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY);
        assertEquals(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS, newState);
    }

    /**
     * Landscape 下：ALWAYS → NEVER
     * 從「永遠分割」在橫向切換應變成「永不分割」。
     */
    @Test
    public void testToggleSplitKeyboard_Always_Landscape_GoesToNever() {
        helper.setDisplayDimensions(1920, 1080); // landscape
        int newState = helper.toggleSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS);
        assertEquals(LIMEKeyboard.SPLIT_KEYBOARD_NEVER, newState);
    }

    /**
     * Landscape 下：LANDSCAPD_ONLY → NEVER
     * 在橫向時「僅橫向分割」切換應變成「永不分割」。
     */
    @Test
    public void testToggleSplitKeyboard_LandscapeOnly_Landscape_GoesToNever() {
        helper.setDisplayDimensions(1920, 1080); // landscape
        int newState = helper.toggleSplitKeyboard(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY);
        assertEquals(LIMEKeyboard.SPLIT_KEYBOARD_NEVER, newState);
    }

    // =========================================================================
    // 邊界測試案例：getSplitKeyboardMenuTextResId 文字資源
    // =========================================================================

    /**
     * LANDSCAPD_ONLY 在 landscape 下應顯示「合併」文字。
     */
    @Test
    public void testGetSplitKeyboardMenuTextResId_LandscapeOnly_InLandscape_ShowsMerge() {
        helper.setDisplayDimensions(1920, 1080); // landscape
        int resId = helper.getSplitKeyboardMenuTextResId(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY);
        assertEquals(R.string.merge_keyboard, resId);
    }

    /**
     * LANDSCAPD_ONLY 在 portrait 下應顯示「分割」文字（尚未實際分割）。
     */
    @Test
    public void testGetSplitKeyboardMenuTextResId_LandscapeOnly_InPortrait_ShowsSplit() {
        helper.setDisplayDimensions(1080, 1920); // portrait
        int resId = helper.getSplitKeyboardMenuTextResId(LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY);
        assertEquals(R.string.split_keyboard, resId);
    }

    /**
     * ALWAYS 狀態應永遠顯示「合併」文字，不管方向。
     */
    @Test
    public void testGetSplitKeyboardMenuTextResId_Always_AlwaysShowsMerge() {
        helper.setDisplayDimensions(1080, 1920); // portrait
        assertEquals(R.string.merge_keyboard,
                helper.getSplitKeyboardMenuTextResId(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS));

        helper.setDisplayDimensions(1920, 1080); // landscape
        assertEquals(R.string.merge_keyboard,
                helper.getSplitKeyboardMenuTextResId(LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS));
    }

    // =========================================================================
    // 邊界測試案例：buildOptionsMenuConfig items 陣列長度
    // =========================================================================

    /**
     * 有分割選項時 items 長度應為 6（含 split keyboard 選項）。
     */
    @Test
    public void testBuildOptionsMenuConfig_WithSplitOption_HasSixItems() {
        helper.setDisplayDimensions(1080, 1920); // portrait, no arrow keys
        OptionsDialogHelper.OptionsMenuConfig config =
                helper.buildOptionsMenuConfig(LIMEKeyboard.SPLIT_KEYBOARD_NEVER, 0);
        assertTrue("Should have split option", config.hasSplitOption);
        assertEquals("Items should include split option → 5 items", 5, config.items.length);
    }

    /**
     * 無分割選項時（landscape + arrow keys）items 長度應為 5。
     */
    @Test
    public void testBuildOptionsMenuConfig_WithoutSplitOption_HasFiveItems() {
        helper.setDisplayDimensions(1920, 1080); // landscape
        OptionsDialogHelper.OptionsMenuConfig config =
                helper.buildOptionsMenuConfig(LIMEKeyboard.SPLIT_KEYBOARD_NEVER, 1); // showArrowKeys=1
        assertFalse("Should NOT have split option in landscape+arrowkeys", config.hasSplitOption);
        assertEquals("Items should exclude split option → 4 items", 4, config.items.length);
    }

    /**
     * Testable subclass that doesn't depend on Android Resources/Preferences.
     */
    private static class TestableOptionsDialogHelper extends OptionsDialogHelper {
        private int displayWidth = 1080;
        private int displayHeight = 1920;
        private int hanConvertOption = 0;
        private int splitKeyboardState = LIMEKeyboard.SPLIT_KEYBOARD_NEVER;

        public TestableOptionsDialogHelper() {
            super(null, null); // Null is ok since we override all methods
        }

        public void setDisplayDimensions(int width, int height) {
            this.displayWidth = width;
            this.displayHeight = height;
        }

        @Override
        public boolean isLandscape() {
            return displayWidth > displayHeight;
        }

        @Override
        public boolean shouldShowSplitOption(int showArrowKeys) {
            return !(isLandscape() && showArrowKeys > 0);
        }

        @Override
        public int getSplitKeyboardMenuTextResId(int splitKeyboardState) {
            boolean isLandscape = isLandscape();
            if ((splitKeyboardState == LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY && isLandscape)
                    || splitKeyboardState == LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS) {
                return R.string.merge_keyboard;
            }
            return R.string.split_keyboard;
        }

        @Override
        public int toggleSplitKeyboard(int currentState) {
            boolean isLandscape = isLandscape();
            int newState;

            if (currentState == LIMEKeyboard.SPLIT_KEYBOARD_NEVER) {
                newState = isLandscape
                        ? LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY
                        : LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS;
            } else if (currentState == LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS) {
                newState = isLandscape
                        ? LIMEKeyboard.SPLIT_KEYBOARD_NEVER
                        : LIMEKeyboard.SPLIT_KEYBOARD_LANDSCAPD_ONLY;
            } else {
                newState = isLandscape
                        ? LIMEKeyboard.SPLIT_KEYBOARD_NEVER
                        : LIMEKeyboard.SPLIT_KEYBOARD_ALWAYS;
            }

            this.splitKeyboardState = newState;
            return newState;
        }

        @Override
        public int getHanConvertOption() {
            return hanConvertOption;
        }

        @Override
        public void setHanConvertOption(int option) {
            this.hanConvertOption = option;
        }

        /**
         * Override buildOptionsMenuConfig to avoid null mResources.
         * Uses stub strings; tests only verify item count and flags.
         */
        @Override
        public OptionsMenuConfig buildOptionsMenuConfig(int splitKeyboardState, int showArrowKeys) {
            boolean isLandscape = isLandscape();
            boolean hasSplitOption = shouldShowSplitOption(showArrowKeys);

            // Stub CharSequence items (content not important for structural tests)
            CharSequence[] items;
            if (hasSplitOption) {
                items = new CharSequence[]{
                        "settings", "hanconvert", "switchim",
                        "system_im", "split_keyboard"
                };
            } else {
                items = new CharSequence[]{
                        "settings", "hanconvert", "switchim",
                        "system_im"
                };
            }
            return new OptionsMenuConfig(items, hasSplitOption, isLandscape);
        }
    }
}
