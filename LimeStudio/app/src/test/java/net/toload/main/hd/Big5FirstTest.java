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

    @Test
    public void allBig5KeepsSameList() {
        List<Mapping> l = list("你", "妳", "擬");
        assertSame(l, SearchServer.big5First(l));
    }
}
