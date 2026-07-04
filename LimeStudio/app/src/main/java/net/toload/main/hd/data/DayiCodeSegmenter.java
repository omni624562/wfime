package net.toload.main.hd.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 大易連打切分:把連續輸入的長碼串(如 nh12vdbxe)以動態規劃切成
 * 合法大易碼序列(每字 1–4 碼)。偏好段數少,同段數比 score 總和。
 * 純邏輯、查詢以介面注入,便於單元測試;實際查詢由 SearchServer 提供(含快取)。
 */
public final class DayiCodeSegmenter {

    /** 查 exact code 分數最高的字;無此碼回 null */
    public interface TopWordLookup {
        Mapping top(String exactCode);
    }

    public static final int MAX_CODE_LENGTH = 20;
    private static final int MAX_SEGMENT_LENGTH = 4; // 大易每字最多 4 碼

    private DayiCodeSegmenter() {
    }

    // K-best DP 的一個解:到某位置為止的段數/總分,與回溯資訊
    private static final class Solution {
        final int segments;
        final long scoreSum;
        final int prevPos;       // 前一段結束位置
        final int prevRank;      // 在 dp[prevPos] 中的名次
        final String lastCode;   // 最後一段的碼

        Solution(int segments, long scoreSum, int prevPos, int prevRank, String lastCode) {
            this.segments = segments;
            this.scoreSum = scoreSum;
            this.prevPos = prevPos;
            this.prevRank = prevRank;
            this.lastCode = lastCode;
        }

        boolean betterThan(Solution other) {
            if (segments != other.segments) return segments < other.segments;
            return scoreSum > other.scoreSum;
        }
    }

    /**
     * K-best 切分:回傳最多 maxResults 種切法(每種是一串段落碼),最佳在前。
     * 切不出完整覆蓋、長度不合或含非大易碼字元時回空清單。
     */
    public static List<List<String>> segmentCodes(String code, TopWordLookup lookup, int maxResults) {
        List<List<String>> results = new LinkedList<>();
        if (code == null || lookup == null || maxResults <= 0) return results;
        int n = code.length();
        if (n < 2 || n > MAX_CODE_LENGTH) return results;
        if (!code.matches("[a-z0-9,./;']+")) return results;

        // dp[i]:覆蓋 code[0..i) 的前 K 個解,最佳在前
        List<List<Solution>> dp = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) dp.add(new ArrayList<>());
        dp.get(0).add(new Solution(0, 0, -1, -1, null));

        for (int i = 1; i <= n; i++) {
            List<Solution> here = dp.get(i);
            for (int len = 1; len <= Math.min(MAX_SEGMENT_LENGTH, i); len++) {
                int j = i - len;
                if (dp.get(j).isEmpty()) continue;
                String segCode = code.substring(j, i);
                Mapping top = lookup.top(segCode);
                if (top == null || top.getWord() == null || top.getWord().isEmpty()) continue;
                List<Solution> prevList = dp.get(j);
                for (int r = 0; r < prevList.size(); r++) {
                    Solution prev = prevList.get(r);
                    here.add(new Solution(prev.segments + 1,
                            prev.scoreSum + top.getScore(), j, r, segCode));
                }
            }
            here.sort((a, b) -> a.betterThan(b) ? -1 : (b.betterThan(a) ? 1 : 0));
            while (here.size() > maxResults) here.remove(here.size() - 1);
        }

        for (Solution sol : dp.get(n)) {
            LinkedList<String> segs = new LinkedList<>();
            Solution cur = sol;
            int pos = n;
            while (cur != null && cur.prevPos >= 0) {
                segs.addFirst(cur.lastCode);
                pos = cur.prevPos;
                cur = dp.get(pos).get(cur.prevRank);
            }
            results.add(segs);
        }
        return results;
    }

    /**
     * 最佳切分的便利版本:每段取字頻最高的字組成中文詞。
     * 切不出回 null;成功回 Mapping(word=串接結果, code=原碼串, segmentedPhraseRecord)。
     */
    public static Mapping segment(String code, TopWordLookup lookup) {
        List<List<String>> segs = segmentCodes(code, lookup, 1);
        if (segs.isEmpty()) return null;

        StringBuilder words = new StringBuilder();
        for (String segCode : segs.get(0)) {
            Mapping top = lookup.top(segCode);
            if (top == null || top.getWord() == null) return null;
            words.append(top.getWord());
        }

        Mapping result = new Mapping();
        result.setWord(words.toString());
        result.setCode(code);
        result.setSegmentedPhraseRecord();
        return result;
    }

    /** 大易詞庫縮碼:每字取全碼的首碼＋末碼(1 碼字取 1 碼) */
    public static String abbreviate(String fullCode) {
        if (fullCode == null || fullCode.isEmpty()) return "";
        if (fullCode.length() == 1) return fullCode;
        return "" + fullCode.charAt(0) + fullCode.charAt(fullCode.length() - 1);
    }
}
