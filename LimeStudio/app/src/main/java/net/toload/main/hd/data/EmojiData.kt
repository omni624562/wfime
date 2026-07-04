package net.toload.main.hd.data

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

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

    // Categories - Using mutableStateOf so Compose UI recomposes when data is loaded
    var SMILEYS by mutableStateOf<List<Emoji>>(emptyList())
    var PEOPLE by mutableStateOf<List<Emoji>>(emptyList())
    var ANIMALS_NATURE by mutableStateOf<List<Emoji>>(emptyList())
    var FOOD_DRINK by mutableStateOf<List<Emoji>>(emptyList())
    var TRAVEL_PLACES by mutableStateOf<List<Emoji>>(emptyList())
    var ACTIVITIES by mutableStateOf<List<Emoji>>(emptyList())
    var OBJECTS by mutableStateOf<List<Emoji>>(emptyList())
    var SYMBOLS by mutableStateOf<List<Emoji>>(emptyList())
    var FLAGS by mutableStateOf<List<Emoji>>(emptyList())

    private var isInitialized = false

    // base emoji -> 完整資訊(keywords/hasSkinTone),供最近使用等以字元回查
    @Volatile
    private var index: Map<String, Emoji> = emptyMap()

    /** 去除膚色修飾字元,取得基底 emoji */
    fun baseOf(char: String): String {
        var s = char
        SKIN_TONES.forEach { tone -> s = s.replace(tone, "") }
        return s
    }

    /** 以基底字元回查完整 Emoji 資訊(未載入或查無時回 null) */
    fun lookup(baseChar: String): Emoji? = index[baseChar]

    suspend fun initialize(context: Context) {
        if (isInitialized) return

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // Re-check initialized inside the IO context
            if (isInitialized) return@withContext

            try {
                android.util.Log.d("EMOJI_DEBUG", "Reading emojis.json from assets...")
                val jsonString = context.assets.open("emojis.json").bufferedReader().use { it.readText() }
                
                val jsonObject = JSONObject(jsonString)
                val categoriesArray = jsonObject.getJSONArray("categories")

                val tempCategories = mutableMapOf<String, List<Emoji>>()

                // 過濾顯示不了的新版 emoji,避免豆腐格。
                // 判斷順序:系統字型 → EmojiCompat(Gboard 式 GMS 可下載字型 fallback)
                val glyphPaint = android.graphics.Paint()
                val emojiCompat = try {
                    androidx.emoji2.text.EmojiCompat.get()
                } catch (_: IllegalStateException) {
                    null // 未初始化(無 GMS 等)
                }
                val emojiCompatReady =
                    emojiCompat?.loadState == androidx.emoji2.text.EmojiCompat.LOAD_STATE_SUCCEEDED

                fun canRender(char: String): Boolean {
                    if (androidx.core.graphics.PaintCompat.hasGlyph(glyphPaint, char)) return true
                    return emojiCompatReady && emojiCompat!!.getEmojiMatch(char, Int.MAX_VALUE) ==
                            androidx.emoji2.text.EmojiCompat.EMOJI_SUPPORTED
                }

                // EmojiCompat 字型還在下載:載入完成後重新過濾一次,把新 emoji 補進清單
                if (emojiCompat != null && !emojiCompatReady) {
                    val appContext = context.applicationContext
                    emojiCompat.registerInitCallback(object :
                        androidx.emoji2.text.EmojiCompat.InitCallback() {
                        override fun onInitialized() {
                            isInitialized = false
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                                .launch { initialize(appContext) }
                        }
                    })
                }

                for (i in 0 until categoriesArray.length()) {
                    val categoryObj = categoriesArray.getJSONObject(i)
                    val categoryName = categoryObj.getString("name")
                    val emojisArray = categoryObj.getJSONArray("emojis")

                    val emojiList = ArrayList<Emoji>(emojisArray.length())
                    for (j in 0 until emojisArray.length()) {
                        val emojiObj = emojisArray.getJSONObject(j)
                        val char = emojiObj.getString("char")
                        val hasSkinTone = emojiObj.optBoolean("hasSkinTone", false)

                        if (!canRender(char)) continue

                        val keywordsArray = emojiObj.getJSONArray("keywords")
                        val keywords = ArrayList<String>(keywordsArray.length())
                        for (k in 0 until keywordsArray.length()) {
                            keywords.add(keywordsArray.getString(k))
                        }

                        emojiList.add(Emoji(char, keywords, hasSkinTone))
                    }
                    tempCategories[categoryName] = emojiList
                }

                index = tempCategories.values.flatten().associateBy { it.char }

                // Batch update state on Main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    tempCategories["SMILEYS"]?.let { SMILEYS = it }
                    tempCategories["PEOPLE"]?.let { PEOPLE = it }
                    tempCategories["ANIMALS_NATURE"]?.let { ANIMALS_NATURE = it }
                    tempCategories["FOOD_DRINK"]?.let { FOOD_DRINK = it }
                    tempCategories["TRAVEL_PLACES"]?.let { TRAVEL_PLACES = it }
                    tempCategories["ACTIVITIES"]?.let { ACTIVITIES = it }
                    tempCategories["OBJECTS"]?.let { OBJECTS = it }
                    tempCategories["SYMBOLS"]?.let { SYMBOLS = it }
                    tempCategories["FLAGS"]?.let { FLAGS = it }
                    isInitialized = true
                    android.util.Log.d("EMOJI_DEBUG", "EmojiData initialization COMPLETE")
                }
            } catch (e: Exception) {
                android.util.Log.e("EMOJI_DEBUG", "EmojiData initialization FAILED: ${e.message}", e)
            }
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

    /**
     * Applies a skin tone modifier to a base emoji string.
     * Handles complex ZWJ sequences by inserting the tone before the first ZWJ set.
     */
    fun applySkinTone(baseEmoji: String, tone: String): String {
        // Check for ZWJ (Zero Width Joiner - \u200D)
        val zwjIndex = baseEmoji.indexOf("\u200D")
        
        return if (zwjIndex != -1) {
            // If ZWJ exists, insert tone before it.
            // Example: Woman (1F469) + ZWJ (200D) + Medical (2695)
            // Result: Woman (1F469) + Tone + ZWJ (200D) + Medical (2695)
            baseEmoji.substring(0, zwjIndex) + tone + baseEmoji.substring(zwjIndex)
        } else {
            // Simple emoji (e.g. Thumbs Up), just append
            baseEmoji + tone
        }
    }
}
