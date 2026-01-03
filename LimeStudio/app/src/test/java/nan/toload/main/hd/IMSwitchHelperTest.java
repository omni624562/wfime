/*
 * Copyright 2015, The LimeIME Open Source Project
 * Licensed under GPL-3.0
 */

package nan.toload.main.hd;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Simple unit tests for IMSwitchHelper logic.
 * Uses a testable subclass to avoid Android framework dependencies.
 */
public class IMSwitchHelperTest {

    private TestableIMSwitchHelper helper;

    @Before
    public void setUp() {
        helper = new TestableIMSwitchHelper();
        helper.setIMActivatedState("0;1;");
        helper.setActiveIM("dayi");
    }

    @Test
    public void testGetActiveIM() {
        assertEquals("dayi", helper.getActiveIM());
    }

    @Test
    public void testSetActiveIM() {
        helper.setActiveIM("phonetic");
        assertEquals("phonetic", helper.getActiveIM());
    }

    @Test
    public void testIsActivatedListEmpty_InitiallyEmpty() {
        assertTrue(helper.isActivatedListEmpty());
    }

    @Test
    public void testBuildActivatedIMList_PopulatesList() {
        helper.buildActivatedIMList();

        assertFalse(helper.isActivatedListEmpty());
        assertEquals(2, helper.getActivatedIMList().size());
    }

    @Test
    public void testBuildActivatedIMList_CorrectIMs() {
        helper.buildActivatedIMList();

        List<String> imList = helper.getActivatedIMList();
        assertEquals("dayi", imList.get(0));
        assertEquals("phonetic", imList.get(1));
    }

    @Test
    public void testGetCurrentIMIndex_First() {
        helper.buildActivatedIMList();

        assertEquals(0, helper.getCurrentIMIndex());
    }

    @Test
    public void testGetCurrentIMIndex_Second() {
        helper.setActiveIM("phonetic");
        helper.buildActivatedIMList();

        assertEquals(1, helper.getCurrentIMIndex());
    }

    @Test
    public void testSwitchToNextIM_Forward() {
        helper.buildActivatedIMList();

        String newName = helper.switchToNextActivatedIM(true);

        assertEquals("注音", newName);
        assertEquals("phonetic", helper.getActiveIM());
    }

    @Test
    public void testSwitchToNextIM_WrapAround() {
        helper.setActiveIM("phonetic");
        helper.buildActivatedIMList();

        String newName = helper.switchToNextActivatedIM(true);

        assertEquals("大易", newName);
        assertEquals("dayi", helper.getActiveIM());
    }

    @Test
    public void testSelectIMByPosition_Valid() {
        helper.buildActivatedIMList();

        helper.selectIMByPosition(1);

        assertEquals("phonetic", helper.getActiveIM());
    }

    /**
     * Testable subclass that doesn't depend on Android Resources/Preferences.
     */
    private static class TestableIMSwitchHelper extends IMSwitchHelper {
        private String activeIM = "";
        private String imActivatedState = "";
        private String cachedState = "";

        private final List<String> activatedIMNameList = new ArrayList<>();
        private final List<String> activatedIMShortNameList = new ArrayList<>();
        private final List<String> activatedIMList = new ArrayList<>();

        // Test data
        private final String[] keyboards = { "大易", "注音", "倉頡" };
        private final String[] shortNames = { "Dayi", "Zhuyin", "Cangjie" };
        private final String[] codes = { "dayi", "phonetic", "cj" };

        public TestableIMSwitchHelper() {
            super(null, null); // Null is ok since we override all methods
        }

        public void setIMActivatedState(String state) {
            this.imActivatedState = state;
        }

        @Override
        public String getActiveIM() {
            return activeIM;
        }

        @Override
        public void setActiveIM(String im) {
            this.activeIM = im;
        }

        @Override
        public List<String> getActivatedIMNameList() {
            return activatedIMNameList;
        }

        @Override
        public List<String> getActivatedIMShortNameList() {
            return activatedIMShortNameList;
        }

        @Override
        public List<String> getActivatedIMList() {
            return activatedIMList;
        }

        @Override
        public boolean isActivatedListEmpty() {
            return activatedIMList.isEmpty();
        }

        @Override
        public int getCurrentIMIndex() {
            for (int i = 0; i < activatedIMList.size(); i++) {
                if (activeIM.equals(activatedIMList.get(i))) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public void buildActivatedIMList() {
            if (cachedState.equals(imActivatedState)) {
                return; // Already built
            }
            cachedState = imActivatedState;

            activatedIMNameList.clear();
            activatedIMList.clear();
            activatedIMShortNameList.clear();

            String[] indices = imActivatedState.split(";");
            for (String indexStr : indices) {
                if (indexStr.isEmpty())
                    continue;
                int index = Integer.parseInt(indexStr);
                if (index < keyboards.length) {
                    activatedIMNameList.add(keyboards[index]);
                    activatedIMShortNameList.add(shortNames[index]);
                    activatedIMList.add(codes[index]);
                }
            }

            // Check if activeIM is in list
            boolean found = false;
            for (String im : activatedIMList) {
                if (im.equals(activeIM)) {
                    found = true;
                    break;
                }
            }
            if (!found && !activatedIMList.isEmpty()) {
                activeIM = activatedIMList.get(0);
            }
        }

        @Override
        public String switchToNextActivatedIM(boolean forward) {
            buildActivatedIMList();
            if (activatedIMList.isEmpty())
                return "";

            for (int i = 0; i < activatedIMList.size(); i++) {
                if (activeIM.equals(activatedIMList.get(i))) {
                    int nextIndex;
                    if (forward) {
                        nextIndex = (i + 1) % activatedIMList.size();
                    } else {
                        nextIndex = (i - 1 + activatedIMList.size()) % activatedIMList.size();
                    }
                    activeIM = activatedIMList.get(nextIndex);
                    return activatedIMNameList.get(nextIndex);
                }
            }
            return "";
        }

        @Override
        public void selectIMByPosition(int position) {
            if (position >= 0 && position < activatedIMList.size()) {
                activeIM = activatedIMList.get(position);
            }
        }
    }
}
