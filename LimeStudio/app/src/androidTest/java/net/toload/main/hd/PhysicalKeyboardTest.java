/*
 * Copyright 2026 The LimeIME Open Source Project
 */

package net.toload.main.hd;

import android.content.Context;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.toload.main.hd.candidate.CandidateView;
import net.toload.main.hd.LIMEKeyboardSwitcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 平板版本實體鍵盤操作 Instrumented 整合測試。
 * 模擬外接實體鍵盤（Physical Keyboard）按鍵的輸入、選字、退格刪除等核心交互邏輯，
 * 確保在平板的大螢幕環境下實體鍵盤的功能完全正常運作。
 */
@RunWith(AndroidJUnit4.class)
public class PhysicalKeyboardTest {

    private TestableLIMEService service;
    private InputConnection mockInputConnection;

    @Before
    public void setUp() {
        // 使用 runOnMainSync 確保 Service.onCreate() 可以在具備 Looper 的 UI 主執行緒中安全執行
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
                mockInputConnection = mock(InputConnection.class);
                
                // 實例化注入 Context 與 Mock InputConnection
                service = new TestableLIMEService(context, mockInputConnection);
                
                // 執行服務的初始化，載入 preferences 等
                service.onCreate();
                
                // 設為中文模式與大易輸入法，用以驗證字根轉譯功能
                service.mEnglishOnly = false;
                service.activeIM = "dayi";

                // Mock 鍵盤切換器與候選字列以避免 null pointer
                service.mKeyboardSwitcher = mock(LIMEKeyboardSwitcher.class);
                when(service.mKeyboardSwitcher.isSymbols()).thenReturn(false);
                when(service.mKeyboardSwitcher.isAlphabetMode()).thenReturn(false);

                service.mCandidateView = mock(CandidateView.class);
                when(service.mCandidateView.takeSelectedSuggestion()).thenReturn(true);
            }
        });
    }

    @Test
    public void testPhysicalKeyboardKeyPressDetection() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // 模擬實體按鍵按下事件
                KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A);
                service.onKeyDown(KeyEvent.KEYCODE_A, event);
                
                // 驗證輸入法成功辨識為實體鍵盤按鍵按下
                assertTrue("hasPhysicalKeyPressed 必須被識別並設為 true", service.hasPhysicalKeyPressed);
            }
        });
    }

    @Test
    public void testPhysicalKeyboardAlphabetInputAndComposing() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // 模擬大易輸入法實體鍵盤輸入 'X' (大易字根為 'x')
                // 對於大易輸入法，'x' 對應的實體按鍵是 KEYCODE_X (Unicode 字元 120)
                KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X, 0, 0);
                
                boolean handled = service.onKeyDown(KeyEvent.KEYCODE_X, event);
                
                // 驗證按鍵被輸入法攔截處理
                assertTrue("字母按鍵應該被 translateKeyDown 處理", handled);
                
                // 驗證 Composing Buffer 被正確裝填為 'x'
                assertTrue("mComposing 長度應該大於 0", service.mComposing.length() > 0);
                assertEquals("字根 'x' 應該被寫入 composing", "x", service.mComposing.toString());
            }
        });
    }

    @Test
    public void testPhysicalKeyboardBackspaceBehavior() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // 手動寫入 composing 測試字根
                service.mComposing.append("abc");
                
                // 模擬實體鍵盤按下 Backspace 鍵
                KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);
                service.onKeyDown(KeyEvent.KEYCODE_DEL, event);
                
                // 驗證 Composing Buffer 是否退格並縮減為 "ab"
                assertEquals("Backspace 應該成功退格一個字根", "ab", service.mComposing.toString());
            }
        });
    }

    @Test
    public void testPhysicalKeyboardEnterBehavior() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // 手動寫入 composing 字根並模擬候選字列已彈出
                service.mComposing.append("x");
                service.hasCandidatesShown = true;
                
                // 模擬實體鍵盤按下 Enter 鍵確認高亮選字
                KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER);
                service.onKeyDown(KeyEvent.KEYCODE_ENTER, event);
                
                // 驗證 Enter 被成功處理（composing 被清除或 hasEnterProcessed 為 true）
                assertTrue("有 composing 且顯示候選字時，按下 Enter 應該被輸入法處理", 
                           service.hasEnterProcessed || service.mComposing.length() == 0);
            }
        });
    }

    @Test
    public void testCtrlSpaceShortcutSwitch() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                service.resetFlags();
                // 模擬 Ctrl 鍵按下
                KeyEvent ctrlEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT);
                service.onKeyDown(KeyEvent.KEYCODE_CTRL_LEFT, ctrlEvent);

                // 模擬 Space 鍵按下 (Ctrl 鍵仍在按下狀態，所以 metaState 有 META_CTRL_ON，isCtrlPressed() 為 true)
                KeyEvent spaceEvent = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE, 0, KeyEvent.META_CTRL_ON);
                service.onKeyDown(KeyEvent.KEYCODE_SPACE, spaceEvent);

                assertTrue("Ctrl+Space 必須觸發 switchChiEng()", service.switchChiEngCalled);
            }
        });
    }

    @Test
    public void testCtrlShiftShortcutShowPicker() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                service.resetFlags();
                service.mEnglishOnly = false; // 確保非英文模式

                // 1. 按下 Ctrl 鍵
                KeyEvent ctrlDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT);
                service.onKeyDown(KeyEvent.KEYCODE_CTRL_LEFT, ctrlDown);

                // 2. 按下 Shift 鍵
                KeyEvent shiftDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT);
                service.onKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, shiftDown);

                // 3. 放開 Shift 鍵
                KeyEvent shiftUp = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT);
                service.onKeyUp(KeyEvent.KEYCODE_SHIFT_LEFT, shiftUp);

                assertTrue("Ctrl+Shift 必須觸發 showIMPicker()", service.showIMPickerCalled);
            }
        });
    }

    @Test
    public void testShiftAtShortcutShowPicker() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                service.resetFlags();
                service.mEnglishOnly = false; // 確保非英文模式

                // 1. 按下 Shift 鍵
                KeyEvent shiftDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT);
                service.onKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT, shiftDown);

                // 2. 按下 @ 鍵
                KeyEvent atDown = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_AT, 0, KeyEvent.META_SHIFT_ON);
                service.onKeyDown(KeyEvent.KEYCODE_AT, atDown);

                // 3. 放開 @ 鍵 (放開時 shift 仍然為 true，故 metaState 具有 META_SHIFT_ON)
                KeyEvent atUp = new KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(),
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_AT, 0, KeyEvent.META_SHIFT_ON);
                service.onKeyUp(KeyEvent.KEYCODE_AT, atUp);

                assertTrue("Shift+@ 必須觸發 showIMPicker()", service.showIMPickerCalled);
            }
        });
    }

    /**
     * 測試用的 Testable LIMEService 子類別，覆寫部分系統 UI 依賴以確保測試穩定執行。
     */
    private static class TestableLIMEService extends LIMEService {
        private final InputConnection inputConnection;

        public TestableLIMEService(Context context, InputConnection ic) {
            this.inputConnection = ic;
            // 關鍵！使用 attachBaseContext 將真實 Context 注入，繞過 InputMethodService 啟動框架限制
            this.attachBaseContext(context);
        }

        @Override
        public InputConnection getCurrentInputConnection() {
            return inputConnection;
        }

        @Override
        public EditorInfo getCurrentInputEditorInfo() {
            EditorInfo info = new EditorInfo();
            info.packageName = getPackageName();
            return info;
        }

        @Override
        public void showWindow(boolean showInput) {
            // 空實作：避免儀器測試環境下彈出 Window 導致 BadTokenException 崩潰
        }

        @Override
        public void requestHideSelf(int flags) {
            // 空實作
        }

        public boolean showIMPickerCalled = false;
        public boolean switchChiEngCalled = false;

        @Override
        public void showIMPicker() {
            showIMPickerCalled = true;
        }

        @Override
        public void switchChiEng() {
            switchChiEngCalled = true;
            mEnglishOnly = !mEnglishOnly;
        }

        public void resetFlags() {
            showIMPickerCalled = false;
            switchChiEngCalled = false;
        }
    }
}
