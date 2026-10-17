package net.toload.main.hd.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.compose.ui.geometry.Rect;
import androidx.compose.ui.node.RootForTest;
import androidx.compose.ui.semantics.AccessibilityAction;
import androidx.compose.ui.semantics.SemanticsActions;
import androidx.compose.ui.semantics.SemanticsConfiguration;
import androidx.compose.ui.semantics.SemanticsNode;
import androidx.compose.ui.semantics.SemanticsProperties;

import net.toload.main.hd.R;
import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.jvm.functions.Function0;

/**
 * 新版鍵盤的 TalkBack：每個鍵在畫面上的位置有一個「按鈕」語意節點，念出鍵名，
 * 點兩下送出與手指點鍵相同的事件。（Compose 把語意樹轉成 Android 無障礙節點的部分，
 * 在 Robolectric 的舊版圖形模擬下無法運作，另在模擬器上用 TalkBack 驗證。）
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class KeyboardAccessibilityTest {

    private Context context;
    private ComposeKeyboardView view;
    private KeyboardPopupSwipeTest.Recorder recorder;

    @Before
    public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        context = new ContextThemeWrapper(activity, R.style.LIMETheme_Material3);
        view = new ComposeKeyboardView(context);
        recorder = new KeyboardPopupSwipeTest.Recorder();
        view.setOnKeyboardActionListener(recorder);
        view.setKeyboard(new LIMEKeyboard(context, R.xml.lime, 0, 1f, 0, 0, 1080));
        FrameLayout root = new FrameLayout(activity);
        root.addView(view, new FrameLayout.LayoutParams(1080, ViewGroup.LayoutParams.WRAP_CONTENT));
        activity.setContentView(root, new ViewGroup.LayoutParams(1080, 2000));
        root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1080, 2000);
        ShadowLooper.idleMainLooper();
    }

    /** ComposeView 裡的 AndroidComposeView（持有語意樹）。 */
    private RootForTest composeRoot() {
        View child = view.getChildAt(0);
        while (!(child instanceof RootForTest))
            child = ((ViewGroup) child).getChildAt(0);
        return (RootForTest) child;
    }

    /** 有說明文字的語意節點：說明 → 節點。 */
    private Map<String, SemanticsNode> nodes() {
        Map<String, SemanticsNode> found = new HashMap<>();
        List<SemanticsNode> stack = new ArrayList<>();
        stack.add(composeRoot().getSemanticsOwner().getUnmergedRootSemanticsNode());
        while (!stack.isEmpty()) {
            SemanticsNode node = stack.remove(stack.size() - 1);
            stack.addAll(node.getChildren());
            SemanticsConfiguration config = node.getConfig();
            if (config.contains(SemanticsProperties.INSTANCE.getContentDescription())) {
                List<String> desc = config.get(SemanticsProperties.INSTANCE.getContentDescription());
                found.put(String.join(" ", desc), node);
            }
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private static boolean click(SemanticsNode node) {
        AccessibilityAction<Function0<Boolean>> action =
                (AccessibilityAction<Function0<Boolean>>) node.getConfig().get(SemanticsActions.INSTANCE.getOnClick());
        return action.getAction().invoke();
    }

    @Test
    public void everyKeyIsAButtonAtItsPosition() {
        Map<String, SemanticsNode> nodes = nodes();
        int withName = 0;
        for (Key k : view.getKeyboard().getKeys()) {
            String name = view.keyDescription(k);
            if (name.isEmpty()) continue;
            withName++;
            SemanticsNode node = nodes.get(name);
            assertNotNull("no node for " + name, node);
            SemanticsConfiguration config = node.getConfig();
            assertEquals(name, "Button", config.get(SemanticsProperties.INSTANCE.getRole()).toString());
            assertTrue(name, config.contains(SemanticsActions.INSTANCE.getOnClick()));
            Rect r = node.getBoundsInRoot();
            assertEquals(name, k.x + view.getPaddingLeft(), r.getLeft(), 0.5f);
            assertEquals(name, k.y + view.getPaddingTop(), r.getTop(), 0.5f);
            assertEquals(name, k.width, r.getWidth(), 0.5f);
            assertEquals(name, k.height, r.getHeight(), 0.5f);
        }
        assertTrue(withName > 25);
        assertEquals(withName, nodes.size());
    }

    @Test
    public void functionKeysHaveSpokenNames() {
        Map<String, SemanticsNode> nodes = nodes();
        for (int res : new int[]{R.string.key_a11y_space, R.string.key_a11y_delete, R.string.key_a11y_shift,
                R.string.key_a11y_enter, R.string.key_a11y_symbols})
            assertTrue(context.getString(res), nodes.containsKey(context.getString(res)));
        assertTrue(nodes.containsKey("a"));
    }

    @Test
    public void doubleTapSendsSameEventsAsATap() {
        assertTrue(click(nodes().get("a")));
        ShadowLooper.idleMainLooper();
        assertEquals(Arrays.asList("press 97", "key 97", "release 97"), recorder.events);

        recorder.events.clear();
        assertTrue(click(nodes().get(context.getString(R.string.key_a11y_delete))));
        int del = LIMEBaseKeyboard.KEYCODE_DELETE;
        assertEquals(Arrays.asList("press " + del, "key " + del, "release " + del), recorder.events);
    }

    @Test
    public void shiftStateIsSpoken() {
        String off = context.getString(R.string.key_a11y_shift);
        String on = context.getString(R.string.key_a11y_shift_on);
        assertTrue(nodes().containsKey(off));
        view.setShifted(true);
        ShadowLooper.idleMainLooper();
        Map<String, SemanticsNode> nodes = nodes();
        assertTrue(nodes.containsKey(on));
        assertFalse(nodes.containsKey(off));
    }
}
