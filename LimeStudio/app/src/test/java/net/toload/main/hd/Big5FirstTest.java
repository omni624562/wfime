package net.toload.main.hd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import net.toload.main.hd.data.Mapping;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 注音候選：含 Big5 以外字（簡體字、罕用擴充字）的候選移到最後，其餘順序不變。
 */
public class Big5FirstTest {

    private static List<Mapping> list(String... words) {
        List<Mapping> l = new ArrayList<>();
        for (String w : words) {
            Mapping m = new Mapping();
            m.setWord(w);
            l.add(m);
        }
        return l;
    }

    private static String words(List<Mapping> l) {
        StringBuilder sb = new StringBuilder();
        for (Mapping m : l) sb.append(m.getWord()).append(' ');
        return sb.toString().trim();
    }

    @Test
    public void simplifiedAndExtensionCharsGoLast() {
        // ㄈㄟˋ：费、废 是簡體字；䒈 是擴充字
        assertEquals("費 廢 菲 肺 费 废 䒈", words(SearchServer.big5First(list("費", "费", "廢", "废", "菲", "䒈", "肺"))));
        // ㄋㄧˇ
        assertEquals("你 妳 擬 旎 拟", words(SearchServer.big5First(list("你", "妳", "擬", "拟", "旎"))));
    }

    @Test
    public void phrasesWithNonBig5CharsGoLast() {
        assertEquals("費用 免費 费用", words(SearchServer.big5First(list("费用", "費用", "免費"))));
    }

    /** 每個 {碼, 字}。 */
    private static List<Mapping> coded(String... codeWord) {
        List<Mapping> l = new ArrayList<>();
        for (int i = 0; i < codeWord.length; i += 2) {
            Mapping m = new Mapping();
            m.setCode(codeWord[i]);
            m.setWord(codeWord[i + 1]);
            l.add(m);
        }
        return l;
    }

    @Test
    public void onlyMovesWithinTheSameCode() {
        // 打 zo4：费 只排到 zo4 這段的最後，仍在碼較短的 zo（非 飛）前面；zo 段裡的 飞 也只排到 zo 段最後
        assertEquals("費 廢 费 非 飛 飞 衣",
                words(SearchServer.big5First(coded("zo4", "費", "zo4", "费", "zo4", "廢",
                        "zo", "非", "zo", "飞", "zo", "飛", "u", "衣"))));
    }

    @Test
    public void allBig5KeepsSameList() {
        List<Mapping> l = list("你", "妳", "擬");
        assertSame(l, SearchServer.big5First(l));
    }
}
