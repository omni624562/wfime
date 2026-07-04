package net.toload.main.hd.data;

/**
 * 大易連打切分:把連續輸入的長碼串(如 nh12vdbxe)以動態規劃切成
 * 合法大易碼序列(每字 1–4 碼),每段取字頻最高的字,還原成中文詞。
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

    /**
     * 切分碼串。偏好段數少,同段數比 score 總和。
     * 切不出完整覆蓋、長度不合或含非大易碼字元時回 null。
     * 成功時回傳 Mapping(word=串接的中文, code=原碼串, segmentedPhraseRecord)。
     */
    public static Mapping segment(String code, TopWordLookup lookup) {
        if (code == null || lookup == null) return null;
        int n = code.length();
        if (n < 2 || n > MAX_CODE_LENGTH) return null;
        if (!code.matches("[a-z0-9,./;']+")) return null;

        // dp[i]:覆蓋 code[0..i) 的最佳解
        int[] segments = new int[n + 1];
        long[] scoreSum = new long[n + 1];
        String[] words = new String[n + 1];
        words[0] = "";

        for (int i = 1; i <= n; i++) {
            words[i] = null;
            for (int len = 1; len <= Math.min(MAX_SEGMENT_LENGTH, i); len++) {
                int j = i - len;
                if (words[j] == null) continue;
                Mapping top = lookup.top(code.substring(j, i));
                if (top == null || top.getWord() == null || top.getWord().isEmpty()) continue;
                int candSegments = segments[j] + 1;
                long candScore = scoreSum[j] + top.getScore();
                if (words[i] == null
                        || candSegments < segments[i]
                        || (candSegments == segments[i] && candScore > scoreSum[i])) {
                    segments[i] = candSegments;
                    scoreSum[i] = candScore;
                    words[i] = words[j] + top.getWord();
                }
            }
        }

        if (words[n] == null) return null;

        Mapping result = new Mapping();
        result.setWord(words[n]);
        result.setCode(code);
        result.setSegmentedPhraseRecord();
        return result;
    }
}
