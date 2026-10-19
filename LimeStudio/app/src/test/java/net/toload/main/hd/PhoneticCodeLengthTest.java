package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 注音連打選字：選中的字用掉組字裡幾碼，剩下的碼要留著繼續選。
 */
public class PhoneticCodeLengthTest {

    /** 選字後剩下的組字（同 LIMEService.commitTyped：先算用掉幾碼，再交給 ComposingRules）。 */
    private static String remainingAfterPick(String composing, String pickedCode) {
        int used = SearchServer.phoneticCodeLengthUsed(pickedCode, composing);
        ComposingRules.AfterCommit r = ComposingRules.afterCommit(composing, pickedCode, used);
        return r.notFinished ? r.remaining : "";
    }

    @Test
    public void pickingFirstToneSyllableKeepsTheRest() {
        // 打「你好」su3cl3，選單字「你」（su3）→ 剩 cl3（好）
        assertEquals(3, SearchServer.phoneticCodeLengthUsed("su3", "su3cl3"));
        assertEquals("cl3", remainingAfterPick("su3cl3", "su3"));
        // 三個音節選第一個
        assertEquals("cl3ji3", remainingAfterPick("su3cl3ji3", "su3"));
    }

    @Test
    public void toneOneSyllableEndsWithSpace() {
        // 一聲用空白：「今天」rup wu0 打成 rup wu0 時選「今」（rup）→ 剩 wu0
        assertEquals("wu0", remainingAfterPick("rup wu0", "rup"));
    }

    @Test
    public void codeWithoutToneAndExactCodeUnchanged() {
        // 打的時候沒打聲調（sucl），選了帶聲調的 su3 →用掉去掉聲調的 2 碼，剩 cl
        assertEquals(2, SearchServer.phoneticCodeLengthUsed("su3", "sucl"));
        assertEquals("cl", remainingAfterPick("sucl", "su3"));
        // 整串剛好就是選中的碼：全部用掉
        assertEquals(3, SearchServer.phoneticCodeLengthUsed("su3", "su3"));
        assertEquals("", remainingAfterPick("su3", "su3"));
        // 詞的碼不帶聲調
        assertEquals(4, SearchServer.phoneticCodeLengthUsed("sucl", "sucl"));
    }

    @Test
    public void unrelatedCodeUsesEverything() {
        assertTrue(SearchServer.phoneticCodeLengthUsed("ji3", "su3cl3") == 6);
    }
}
