package net.toload.main.hd.global;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * 新版鍵盤預設開（A4-6）；使用者關掉的照舊用舊鍵盤。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ComposeKeyboardDefaultTest {

    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
    }

    @Test
    public void newKeyboardIsOnByDefault() {
        assertTrue(new LIMEPreferenceManager(context).getExperimentalComposeKeyboard());
    }

    @Test
    public void userWhoTurnedItOffKeepsOldKeyboard() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean("experimental_compose_keyboard", false).commit();
        assertFalse(new LIMEPreferenceManager(context).getExperimentalComposeKeyboard());
    }
}
