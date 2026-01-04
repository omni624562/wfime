package nan.toload.main.hd.global;

import java.util.HashMap;
import java.util.Map;

public class RootMapper {

    private static final Map<Character, Character> phoneticMap = new HashMap<>();
    private static final Map<Character, Character> dayiMap = new HashMap<>();

    static {
        // Standard Phonetic (Zhuyin) Mapping (Daqian)
        phoneticMap.put('1', 'ㄅ');
        phoneticMap.put('q', 'ㄆ');
        phoneticMap.put('a', 'ㄇ');
        phoneticMap.put('z', 'ㄈ');
        phoneticMap.put('2', 'ㄉ');
        phoneticMap.put('w', 'ㄊ');
        phoneticMap.put('s', 'ㄋ');
        phoneticMap.put('x', 'ㄌ');
        phoneticMap.put('3', 'ˇ');
        phoneticMap.put('e', 'ㄍ');
        phoneticMap.put('d', 'ㄎ');
        phoneticMap.put('c', 'ㄏ');
        phoneticMap.put('4', 'ˋ');
        phoneticMap.put('r', 'ㄐ');
        phoneticMap.put('f', 'ㄑ');
        phoneticMap.put('v', 'ㄒ');
        phoneticMap.put('5', 'ㄓ');
        phoneticMap.put('t', 'ㄔ');
        phoneticMap.put('g', 'ㄕ');
        phoneticMap.put('b', 'ㄖ');
        phoneticMap.put('6', 'ˊ');
        phoneticMap.put('y', 'ㄗ');
        phoneticMap.put('h', 'ㄘ');
        phoneticMap.put('n', 'ㄙ');
        phoneticMap.put('7', '˙');
        phoneticMap.put('u', 'ㄧ');
        phoneticMap.put('j', 'ㄨ');
        phoneticMap.put('m', 'ㄩ');
        phoneticMap.put('8', 'ㄚ');
        phoneticMap.put('i', 'ㄛ');
        phoneticMap.put('k', 'ㄜ');
        phoneticMap.put(',', 'ㄝ');
        phoneticMap.put('9', 'ㄞ');
        phoneticMap.put('o', 'ㄟ');
        phoneticMap.put('l', 'ㄠ');
        phoneticMap.put('.', 'ㄡ');
        phoneticMap.put('0', 'ㄢ');
        phoneticMap.put('p', 'ㄣ');
        phoneticMap.put(';', 'ㄤ');
        phoneticMap.put('/', 'ㄥ');
        phoneticMap.put('-', 'ㄦ');

        // Standard Dayi Mapping
        // Based on standard layouts (some variations exist, using common one)
        dayiMap.put('1', '言');
        dayiMap.put('2', '牛');
        dayiMap.put('3', '目');
        dayiMap.put('4', '四');
        dayiMap.put('5', '王');
        dayiMap.put('6', '車');
        dayiMap.put('7', '田');
        dayiMap.put('8', '八');
        dayiMap.put('9', '足');
        dayiMap.put('0', '金');

        dayiMap.put('q', '言');
        dayiMap.put('w', '山');
        dayiMap.put('e', '工');
        dayiMap.put('r', '人');
        dayiMap.put('t', '非');
        dayiMap.put('y', '老');
        dayiMap.put('u', '月');
        dayiMap.put('i', '金');
        dayiMap.put('o', '口');
        dayiMap.put('p', '意');

        dayiMap.put('a', '止');
        dayiMap.put('s', '日');
        dayiMap.put('d', '石');
        dayiMap.put('f', '一');
        dayiMap.put('g', '土');
        dayiMap.put('h', '鳥');
        dayiMap.put('j', '二');
        dayiMap.put('k', '大');
        dayiMap.put('l', '木');

        dayiMap.put('z', '心');
        dayiMap.put('x', '水');
        dayiMap.put('c', '鹿');
        dayiMap.put('v', '禾');
        dayiMap.put('b', '馬');
        dayiMap.put('n', '魚');
        dayiMap.put('m', '雨'); // Note: 'n' varies (sometimes 虫), 'm' varies

        dayiMap.put(',', '力');
        dayiMap.put('.', '舟');
        dayiMap.put('/', '竹');
    }

    public static Character getRoot(String imType, char key) {
        if (imType == null)
            return key;

        char lowerKey = Character.toLowerCase(key);
        Character root = null;

        if (imType.contains("phonetic")) { // Matches "phonetic", "phonetic_hs", etc. assumes standard
            root = phoneticMap.get(lowerKey);
        } else if (imType.contains("dayi")) {
            root = dayiMap.get(lowerKey);
        }

        return root != null ? root : key;
    }
}
