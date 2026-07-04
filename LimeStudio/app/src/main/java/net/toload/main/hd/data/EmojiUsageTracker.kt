package net.toload.main.hd.data

import android.content.Context

/**
 * emoji 使用頻率追蹤:記錄使用者實際選過哪些 emoji,
 * 讓打字聯想的 emoji 候選依常用程度排序(常用在前)。
 * 資料存於 emoji_prefs SharedPreferences,上限 200 筆(淘汰次數最低者)。
 */
object EmojiUsageTracker {

    private const val PREF_NAME = "emoji_prefs"
    private const val KEY_USAGE = "emoji_usage"
    private const val MAX_ENTRIES = 200
    private const val ENTRY_SEP = ""
    private const val FIELD_SEP = ""

    @Volatile
    private var counts: MutableMap<String, Int>? = null

    @Synchronized
    private fun load(context: Context): MutableMap<String, Int> {
        counts?.let { return it }
        val map = LinkedHashMap<String, Int>()
        val saved = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USAGE, "") ?: ""
        if (saved.isNotEmpty()) {
            for (entry in saved.split(ENTRY_SEP)) {
                val parts = entry.split(FIELD_SEP)
                if (parts.size == 2)
                    parts[1].toIntOrNull()?.let { map[parts[0]] = it }
            }
        }
        counts = map
        return map
    }

    private fun save(context: Context, map: Map<String, Int>) {
        val serialized = map.entries.joinToString(ENTRY_SEP) { "${it.key}$FIELD_SEP${it.value}" }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USAGE, serialized).apply()
    }

    /** 使用者選了一個 emoji(候選列或選擇器) */
    @JvmStatic
    @Synchronized
    fun record(context: Context, emoji: String) {
        if (emoji.isEmpty()) return
        val map = load(context)
        map[emoji] = (map[emoji] ?: 0) + 1
        if (map.size > MAX_ENTRIES) {
            // 淘汰次數最低者(平手時淘汰較舊的)
            val evict = map.entries.minByOrNull { it.value }?.key
            if (evict != null) map.remove(evict)
        }
        save(context, map)
    }

    /** 該 emoji 的累計使用次數(未用過為 0) */
    @JvmStatic
    fun countOf(context: Context, emoji: String?): Int {
        if (emoji.isNullOrEmpty()) return 0
        return load(context)[emoji] ?: 0
    }
}
