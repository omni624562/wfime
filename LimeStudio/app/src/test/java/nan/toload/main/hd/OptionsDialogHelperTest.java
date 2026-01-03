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
    }
}
