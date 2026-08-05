package net.toload.main.hd;

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ApplicationProvider;
import net.toload.main.hd.data.Mapping;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CandidateSelectionTest {

    @Test
    public void testGetRealCodeLengthWithNullCode() {
        SearchServer searchServer = new SearchServer(ApplicationProvider.getApplicationContext());
        Mapping mapping = new Mapping();
        mapping.setWord("測試");
        mapping.setCode(null); // Related phrase / Emoji scenario

        int len = searchServer.getRealCodeLength(mapping, "test");
        assertEquals(0, len);
    }

    @Test
    public void testGetRealCodeLengthWithValidCode() {
        SearchServer searchServer = new SearchServer(ApplicationProvider.getApplicationContext());
        Mapping mapping = new Mapping();
        mapping.setWord("牛");
        mapping.setCode("nh");

        int len = searchServer.getRealCodeLength(mapping, "nh");
        assertEquals(2, len);
    }
}
