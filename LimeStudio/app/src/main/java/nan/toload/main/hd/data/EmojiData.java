package nan.toload.main.hd.data;

public class EmojiData {
    public static final String[] RECENT = {
            "😂", "❤️", "👍", "😭", "🙏", "😘", "🥰", "😍", "😊", "🎉"
    };

    public static final String[] SMILEYS = {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "☺️", "😊",
            "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙",
            "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎",
            "🤩", "🥳", "😏", "😒", "😞", "😔", "worried", "😕", "🙁", "☹️",
            "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡",
            "🤬", "🤯", "😳", "🥵", "🥶", "😱", "hk", "😨", "😰", "😥",
            "😓", "🤗", "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬",
            "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪",
            "😵", "🤐", "🥴", "🤢", "🤮", "sneezing", "😷", "🤒", "🤕", "🤑"
    };

    public static final String[] ANIMALS = {
            "🐶", "🐱", "🐭", "🐹", "🐰", "fox", "🐻", "panda", "koala", "🐯",
            "🦁", "cow", "pig", "frog", "monkey", "chicken", "penguin", "bird", "chick", "duck",
            "eagle", "owl", "bat", "wolf", "boar", "horse", "unicorn", "bee", "bug", "butterfly",
            "snail", "beetle", "ant", "mosquito", "spider", "web", "turtle", "snake", "lizard", "scorpion"
    };

    public static final String[] FOOD = {
            "🍏", "🍎", "🍐", "🍊", "🍋", "banana", "watermelon", "grape", "strawberry", "melon",
            "cherry", "peach", "mango", "pineapple", "coconut", "kiwi", "tomato", "avocado", "broccoli", "cucumber",
            "corn", "carrot", "potato", "bread", "croissant", "baguette", "pretzel", "cheese", "egg", "bacon",
            "steak", "burger", "fries", "pizza", "hotdog", "sandwich", "taco", "burrito", "soup", "salad"
    };

    public static final String[] OBJECTS = {
            "⌚", "📱", "💻", "⌨️", "🖥️", "printer", "mouse", "trackball", "joystick", "clamp",
            "💽", "💾", "💿", "📀", "📼", "📷", "📸", "📹", "🎥", "📽️",
            "🎞️", "📞", "☎️", "pager", "fax", "📺", "radio", "🎙️", "🎚️", "🎛️",
            "🧭", "⏱️", "⏲️", "⏰", "🕰️", "⌛", "⏳", "satellite", "battery", "plug"
    };

    public static String[] getListByCategory(int categoryIndex) {
        switch (categoryIndex) {
            case 0:
                return RECENT;
            case 1:
                return SMILEYS;
            case 2:
                return ANIMALS;
            case 3:
                return FOOD;
            case 4:
                return OBJECTS;
            default:
                return SMILEYS;
        }
    }
}
