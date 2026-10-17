package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 選字後組字的處理:還沒打完時切掉已上字的碼,剩下的碼再查一次候選。純邏輯,不需要 Android。
 */
public class ComposingRulesTest {

    @Test
    public void finishedWhenComposingNotLongerThanCode() {
        ComposingRules.AfterCommit r = ComposingRules.afterCommit("d", "d", 1);
        assertFalse(r.notFinished);
        assertFalse(r.shouldUpdateCandidates);
    }

    @Test
    public void finishedWhenCodeIsNull() {
        ComposingRules.AfterCommit r = ComposingRules.afterCommit("nh1", null, 0);
        assertFalse(r.notFinished);
        assertFalse(r.shouldUpdateCandidates);
    }

    @Test
    public void dayiContinuousTyping_KeepsRemainingCode() {
        // 大易連打 nh1dfu:選了 詹(nh1)之後,剩下 dfu 再查一次
        ComposingRules.AfterCommit r = ComposingRules.afterCommit("nh1dfu", "nh1", 3);
        assertTrue(r.notFinished);
        assertEquals("dfu", r.remaining);
        assertTrue(r.shouldUpdateCandidates);
    }

    @Test
    public void chuyinRemainingStartingWithSpace_DropsOneSpace() {
        // 注音連打 su3 u4:選了 你(su3)之後剩「 u4」,去掉開頭的空白
        ComposingRules.AfterCommit r = ComposingRules.afterCommit("su3 u4", "su3", 3);
        assertTrue(r.notFinished);
        assertEquals("u4", r.remaining);
        assertTrue(r.shouldUpdateCandidates);
    }

    @Test
    public void remainingIsExactlyOneSpace_KeptAndNoQuery() {
        ComposingRules.AfterCommit r = ComposingRules.afterCommit("su3 ", "su3", 3);
        assertTrue(r.notFinished);
        assertEquals(" ", r.remaining);
        assertFalse(r.shouldUpdateCandidates);
    }

    @Test
    public void onlyOneLeadingSpaceIsDropped() {
        ComposingRules.AfterCommit r = ComposingRules.afterCommit("a  b", "a", 1);
        assertEquals(" b", r.remaining);
        assertTrue(r.shouldUpdateCandidates);
    }

    @Test
    public void committedLengthBeyondComposing_RemainingEmpty() {
        // 實際上字用掉的碼比組字還長:切到空,不再查詢
        ComposingRules.AfterCommit r = ComposingRules.afterCommit("ab", "a", 5);
        assertTrue(r.notFinished);
        assertEquals("", r.remaining);
        assertFalse(r.shouldUpdateCandidates);
    }

    @Test
    public void realCodeLengthDiffersFromCandidateCode() {
        // 注音的碼帶聲調、實際上字用掉的碼長可能和候選的碼長不同:切的長度以實際碼長為準
        ComposingRules.AfterCommit r = ComposingRules.afterCommit("su3u4", "su3", 2);
        assertTrue(r.notFinished);
        assertEquals("3u4", r.remaining);
    }

    // ---- 大易連打模式：英文單字不自動上字 ----

    @Test
    public void englishWordKeepsComposingFromFourthLetter() {
        // book：boo 之後打 k 接不上大易碼，仍繼續組字
        assertTrue(ComposingRules.keepComposingEnglishWord("boo", 'k'));
        assertTrue(ComposingRules.keepComposingEnglishWord("book", 's'));
        assertTrue(ComposingRules.keepComposingEnglishWord("Boo", 'K'));
    }

    @Test
    public void shortCodesStillAutoCommit() {
        // 不到 3 碼照舊自動上字
        assertFalse(ComposingRules.keepComposingEnglishWord("bo", 'k'));
        assertFalse(ComposingRules.keepComposingEnglishWord("", 'k'));
    }

    @Test
    public void codesWithDigitsOrSymbolsStillAutoCommit() {
        // 含數字或符號的字根（例如 nh1、i2.）照舊自動上字；新鍵不是字母也照舊
        assertFalse(ComposingRules.keepComposingEnglishWord("nh1", 'k'));
        assertFalse(ComposingRules.keepComposingEnglishWord("i2.", 'k'));
        assertFalse(ComposingRules.keepComposingEnglishWord("boo", '1'));
        assertFalse(ComposingRules.keepComposingEnglishWord("boo", ','));
    }

    // ---- 大易連打模式：數字串不自動上字 ----

    @Test
    public void numberKeepsComposingFromFourthDigit() {
        // 123456789：123 之後再打數字仍繼續組字
        assertTrue(ComposingRules.keepComposingNumber("123", '4'));
        assertTrue(ComposingRules.keepComposingNumber("12345678", '9'));
    }

    @Test
    public void shortOrMixedNumbersStillAutoCommit() {
        assertFalse(ComposingRules.keepComposingNumber("12", '3'));
        assertFalse(ComposingRules.keepComposingNumber("nh1", '2'));
        assertFalse(ComposingRules.keepComposingNumber("123", 'a'));
        assertFalse(ComposingRules.keepComposingNumber("abc", '1'));
    }

    @Test
    public void rawCodeFirstOnlyForPureLettersOrPureDigits() {
        assertTrue(ComposingRules.rawCodeFirst("sorry"));
        assertTrue(ComposingRules.rawCodeFirst("123456789"));
        assertFalse(ComposingRules.rawCodeFirst("nh1dfu"));
        assertFalse(ComposingRules.rawCodeFirst("i2.ab"));
        assertFalse(ComposingRules.rawCodeFirst(""));
    }
}
