package net.toload.main.hd.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 大易連打切分的純邏輯測試(lookup 用 HashMap stub,不需 Robolectric)。
 */
public class DayiCodeSegmenterTest {

    private static class StubLookup implements DayiCodeSegmenter.TopWordLookup {
        private final Map<String, Mapping> table = new HashMap<>();

        StubLookup put(String code, String word, int score) {
            Mapping m = new Mapping();
            m.setCode(code);
            m.setWord(word);
            m.setScore(score);
            table.put(code, m);
            return this;
        }

        @Override
        public Mapping top(String exactCode) {
            return table.get(exactCode);
        }
    }

    @Test
    public void testFullCoverage_ThreeSegments() {
        StubLookup lookup = new StubLookup()
                .put("nh1", "明", 10)
                .put("2vd", "天", 8)
                .put("bxe", "好", 6);
        Mapping result = DayiCodeSegmenter.segment("nh12vdbxe", lookup);
        assertNotNull(result);
        assertEquals("明天好", result.getWord());
        assertEquals("nh12vdbxe", result.getCode());
        assertTrue(result.isSegmentedPhraseRecord());
    }

    @Test
    public void testNoFullCoverage_ReturnsNull() {
        StubLookup lookup = new StubLookup().put("nh1", "明", 10); // 後段切不出
        assertNull(DayiCodeSegmenter.segment("nh12vdbxe", lookup));
    }

    @Test
    public void testPrefersFewerSegments_EvenIfScoreLower() {
        // 2 段解(ab|cd 總分 2)應勝 3 段解(a|b|cd 或 ab|c|d 總分 30)
        StubLookup lookup = new StubLookup()
                .put("a", "甲", 10).put("b", "乙", 10)
                .put("c", "丙", 10).put("d", "丁", 10)
                .put("ab", "戊", 1).put("cd", "己", 1);
        Mapping result = DayiCodeSegmenter.segment("abcd", lookup);
        assertNotNull(result);
        assertEquals("戊己", result.getWord());
    }

    @Test
    public void testSameSegmentCount_PrefersHigherScore() {
        // ab|cd(總分 20)勝 a|bcd?不同段數;改成兩種 2 段解:ab|cd vs abc|d
        StubLookup lookup = new StubLookup()
                .put("ab", "戊", 5).put("cd", "己", 5)   // 總分 10
                .put("abc", "庚", 100).put("d", "丁", 100); // 總分 200
        Mapping result = DayiCodeSegmenter.segment("abcd", lookup);
        assertNotNull(result);
        assertEquals("庚丁", result.getWord());
    }

    @Test
    public void testDayiSymbolCodes_Segmentable() {
        StubLookup lookup = new StubLookup()
                .put(",.", "壬", 3)
                .put("/1", "癸", 3);
        Mapping result = DayiCodeSegmenter.segment(",./1", lookup);
        assertNotNull(result);
        assertEquals("壬癸", result.getWord());
    }

    @Test
    public void testInvalidCharacter_ReturnsNull() {
        StubLookup lookup = new StubLookup().put("ab", "戊", 1);
        assertNull(DayiCodeSegmenter.segment("a!b", lookup));
    }

    @Test
    public void testLengthBounds() {
        StubLookup lookup = new StubLookup().put("a", "甲", 1);
        assertNull("單碼不切分(一般查詢已涵蓋)", DayiCodeSegmenter.segment("a", lookup));
        assertNull("空字串", DayiCodeSegmenter.segment("", lookup));
        assertNull("null", DayiCodeSegmenter.segment(null, lookup));
        // 21 碼超過上限
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 21; i++) sb.append('a');
        assertNull(DayiCodeSegmenter.segment(sb.toString(), lookup));
        // 20 碼可切
        sb.setLength(20);
        assertNotNull(DayiCodeSegmenter.segment(sb.toString(), lookup));
    }

    @Test
    public void testSegmentCodes_KBest_DistinctAndOrdered() {
        StubLookup lookup = new StubLookup()
                .put("ab", "戊", 9).put("cd", "己", 9)   // 2 段,總分 18(最佳)
                .put("abc", "庚", 5).put("d", "丁", 5)   // 2 段,總分 10
                .put("a", "甲", 9).put("bcd", "辛", 9);  // 2 段,總分 18(與最佳同分)
        java.util.List<java.util.List<String>> segs =
                DayiCodeSegmenter.segmentCodes("abcd", lookup, 3);
        assertEquals(3, segs.size());
        // 全部都是 2 段解
        for (java.util.List<String> s : segs) assertEquals(2, s.size());
        // 前兩名總分 18,第三名總分 10
        java.util.List<String> third = segs.get(2);
        assertEquals("abc", third.get(0));
        assertEquals("d", third.get(1));
    }

    @Test
    public void testAbbreviate() {
        assertEquals("", DayiCodeSegmenter.abbreviate(""));
        assertEquals("a", DayiCodeSegmenter.abbreviate("a"));
        assertEquals("dj", DayiCodeSegmenter.abbreviate("dj"));
        assertEquals(",5", DayiCodeSegmenter.abbreviate(",l5"));
        assertEquals(",;", DayiCodeSegmenter.abbreviate(",4b;"));
    }

    @Test
    public void testSegmentMaxFourCodes() {
        // 5 碼單字不存在:必須至少切 2 段
        StubLookup lookup = new StubLookup()
                .put("abcde", "不可能", 99) // 超過 4 碼,DP 不會嘗試這段
                .put("abcd", "庚", 1)
                .put("e", "辛", 1);
        Mapping result = DayiCodeSegmenter.segment("abcde", lookup);
        assertNotNull(result);
        assertEquals("庚辛", result.getWord());
    }
}
