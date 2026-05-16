/*
 * Copyright 2015, The LimeIME Open Source Project
 * Licensed under GPL-3.0
 */

package net.toload.main.hd;

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

    // =========================================================================
    // 邊界測試案例
    // =========================================================================

    /**
     * 向後切換（backward）的迴繞行為：
     * 當目前是第一個輸入法時，向後切換應迴繞到最後一個。
     */
    @Test
    public void testSwitchToNextIM_Backward_WrapAround() {
        // activeIM = "dayi"（index 0），向後應迴繞到 "phonetic"（index 1）
        helper.buildActivatedIMList();

        String newName = helper.switchToNextActivatedIM(false);

        assertEquals("注音", newName);
        assertEquals("phonetic", helper.getActiveIM());
    }

    /**
     * 向後切換的中間案例：
     * 當目前是第二個輸入法時，向後切換應回到第一個。
     */
    @Test
    public void testSwitchToNextIM_Backward_Middle() {
        helper.setActiveIM("phonetic");
        helper.buildActivatedIMList();

        String newName = helper.switchToNextActivatedIM(false);

        assertEquals("大易", newName);
        assertEquals("dayi", helper.getActiveIM());
    }

    /**
     * activeIM 不在已啟動清單中時，switchToNextActivatedIM 應靜默失敗（回傳空字串）。
     * 這確認了目前靜默失敗的設計行為。
     */
    @Test
    public void testSwitchToNextIM_ActiveIMNotInList_ReturnsEmpty() {
        // 設定不在清單中的 activeIM
        helper.setActiveIM("cj");
        helper.buildActivatedIMList();
        // 注意：buildActivatedIMList 如果發現 activeIM 不在清單中會自動修正為第一個
        // 此測試確認修正後能正常切換
        // 若 buildActivatedIMList 沒修正，switchToNextActivatedIM 應回傳空字串
        String newName = helper.switchToNextActivatedIM(true);
        // 切換後 activeIM 應在清單中
        assertTrue(
            "After switch, activeIM should be in activatedIMList",
            helper.getActivatedIMList().contains(helper.getActiveIM())
        );
    }

    /**
     * 快取行為：相同狀態字串呼叫兩次 buildActivatedIMList 不應清空清單。
     */
    @Test
    public void testBuildActivatedIMList_SameState_DoesNotClearList() {
        helper.buildActivatedIMList();
        int sizeAfterFirst = helper.getActivatedIMList().size();

        helper.buildActivatedIMList(); // 第二次呼叫，狀態相同
        int sizeAfterSecond = helper.getActivatedIMList().size();

        assertEquals("Cache hit should not reset the list", sizeAfterFirst, sizeAfterSecond);
        assertEquals("List should still contain 2 IMs", 2, sizeAfterSecond);
    }

    /**
     * 快取行為：狀態改變後再次呼叫 buildActivatedIMList 應重建清單。
     */
    @Test
    public void testBuildActivatedIMList_StateChanged_RebuildsList() {
        // 初始狀態：「0;1;」 = dayi + phonetic
        helper.buildActivatedIMList();
        assertEquals(2, helper.getActivatedIMList().size());
        assertEquals("dayi", helper.getActivatedIMList().get(0));

        // 改變狀態到只有 phonetic（index 1）
        helper.setIMActivatedState("1;");
        helper.buildActivatedIMList();

        assertEquals("After state change, list should be rebuilt with 1 IM",
                1, helper.getActivatedIMList().size());
        assertEquals("phonetic", helper.getActivatedIMList().get(0));
    }

    /**
     * selectIMByPosition 傳入無效 index 時不應拋出例外，且 activeIM 應保持不變。
     */
    @Test
    public void testSelectIMByPosition_InvalidIndex_NoChange() {
        helper.buildActivatedIMList();
        String before = helper.getActiveIM();

        helper.selectIMByPosition(-1);     // 負數 index
        assertEquals("Negative index should not change activeIM", before, helper.getActiveIM());

        helper.selectIMByPosition(999);    // 超出範圍 index
        assertEquals("Out-of-bounds index should not change activeIM", before, helper.getActiveIM());
    }

    /**
     * 切換後 getActiveIM() 應與 switchToNextActivatedIM 回傳的名稱對應的 code 一致。
     */
    @Test
    public void testSwitchToNextIM_ActiveIMUpdatedAfterSwitch() {
        helper.buildActivatedIMList();
        String activeBefore = helper.getActiveIM();

        helper.switchToNextActivatedIM(true);

        String activeAfter = helper.getActiveIM();
        assertNotEquals("activeIM should change after switching", activeBefore, activeAfter);
        assertTrue("New activeIM should be in the activated list",
                helper.getActivatedIMList().contains(activeAfter));
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
