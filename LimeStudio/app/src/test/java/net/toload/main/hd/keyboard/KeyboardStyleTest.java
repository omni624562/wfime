package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.ContextThemeWrapper;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;

import net.toload.main.hd.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

/**
 * 鍵盤主題樣式的解讀：畫鍵用的屬性進 KeyPainter，其他屬性（遲滯距離、長按小鍵盤版面）仍由鍵盤 View 讀到。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class KeyboardStyleTest {

    private static Object field(Object o, String name) throws Exception {
        Field f = LIMEKeyboardBaseView.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    // 舊鍵盤 LIMEKeyboardView 與 input.xml 在 A4-7 移除；改直接建 LIMEKeyboardBaseView（長按小鍵盤仍用它），
    // 套用 input.xml 原本的 style="?attr/LIMEKeyboardLayout"
    private LIMEKeyboardBaseView inflateOld() {
        Context context = new ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.LIMETheme_Material3);
        return new LIMEKeyboardBaseView(context,
                Robolectric.buildAttributeSet().setStyleAttribute("?attr/LIMEKeyboardLayout").build());
    }

    @Test
    public void paintingStyleGoesToKeyPainter() {
        LIMEKeyboardBaseView view = inflateOld();
        Context context = view.getContext();
        KeyPainter p = view.mPainter;
        assertNotNull(p.mKeyBackground);
        assertNotNull(p.mActionKeyBackground);
        assertNotNull(p.mFunctionalKeyBackground);
        assertNotNull(p.mPopupHint);
        assertEquals(context.getResources().getDimensionPixelSize(R.dimen.key_text_size_material3), p.mKeyTextSize);
        assertNotEquals(0, p.mKeyTextColorNormal);
        assertNotNull(p.mPaint);
        assertNotNull(p.mPadding);
    }

    @Test
    public void otherAttributesStillReachTheView() throws Exception {
        LIMEKeyboardBaseView view = inflateOld();
        float hysteresis = (float) field(view, "mKeyHysteresisDistance");
        assertEquals(view.getContext().getResources().getDimensionPixelOffset(R.dimen.key_hysteresis_distance),
                hysteresis, 0f);
        assertEquals(R.layout.keyboard_popup, (int) field(view, "mPopupLayout"));
    }
}
