package nan.toload.main.hd.data

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class Emoji(
    val char: String,
    val keywords: List<String>,
    val hasSkinTone: Boolean = false
)

object EmojiData {
    // Skin tone modifiers
    val SKIN_TONES = listOf(
        "🏻", "🏼", "🏽", "🏾", "🏿"
    )

    // Categories - Initially empty, populated from JSON
    var SMILEYS: List<Emoji> = emptyList()
    var PEOPLE: List<Emoji> = emptyList()
    var ANIMALS_NATURE: List<Emoji> = emptyList()
    var FOOD_DRINK: List<Emoji> = emptyList()
    var TRAVEL_PLACES: List<Emoji> = emptyList()
    var ACTIVITIES: List<Emoji> = emptyList()
    var OBJECTS: List<Emoji> = emptyList()
    var SYMBOLS: List<Emoji> = emptyList()
    var FLAGS: List<Emoji> = emptyList()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val jsonString = context.assets.open("emojis.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val categoriesArray = jsonObject.getJSONArray("categories")

            for (i in 0 until categoriesArray.length()) {
                val categoryObj = categoriesArray.getJSONObject(i)
                val categoryName = categoryObj.getString("name")
                val emojisArray = categoryObj.getJSONArray("emojis")
                
                val emojiList = ArrayList<Emoji>()
                for (j in 0 until emojisArray.length()) {
                    val emojiObj = emojisArray.getJSONObject(j)
                    val char = emojiObj.getString("char")
                    val hasSkinTone = emojiObj.optBoolean("hasSkinTone", false)
                    
                    val keywordsArray = emojiObj.getJSONArray("keywords")
                    val keywords = ArrayList<String>()
                    for (k in 0 until keywordsArray.length()) {
                        keywords.add(keywordsArray.getString(k))
                    }
                    
                    emojiList.add(Emoji(char, keywords, hasSkinTone))
                }

                when (categoryName) {
                    "SMILEYS" -> SMILEYS = emojiList
                    "PEOPLE" -> PEOPLE = emojiList
                    "ANIMALS_NATURE" -> ANIMALS_NATURE = emojiList
                    "FOOD_DRINK" -> FOOD_DRINK = emojiList
                    "TRAVEL_PLACES" -> TRAVEL_PLACES = emojiList
                    "ACTIVITIES" -> ACTIVITIES = emojiList
                    "OBJECTS" -> OBJECTS = emojiList
                    "SYMBOLS" -> SYMBOLS = emojiList
                    "FLAGS" -> FLAGS = emojiList
                }
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to empty or crash? 
            // For now, print stack trace. If file is missing, lists remain empty.
        }
    }

    fun getListByCategory(categoryIndex: Int): List<Emoji> {
        return when (categoryIndex) {
            1 -> SMILEYS
            2 -> PEOPLE
            3 -> ANIMALS_NATURE
            4 -> FOOD_DRINK
            5 -> TRAVEL_PLACES
            6 -> ACTIVITIES
            7 -> OBJECTS
            8 -> SYMBOLS
            9 -> FLAGS
            else -> SMILEYS
        }
    }
}
