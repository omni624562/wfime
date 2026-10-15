package net.toload.main.hd

/**
 * 組字的規則（從 LIMEService 抽出，純邏輯）。
 */
object ComposingRules {

    /** 選字後組字的結果。 */
    class AfterCommit(
        /** 組字比選中字的碼還長：還沒打完（連打），要留下剩下的碼。 */
        @JvmField val notFinished: Boolean,
        /** 切掉已上字的碼之後剩下的組字（還沒打完時才有意義）。 */
        @JvmField val remaining: String,
        /** 剩下的碼要再查一次候選。 */
        @JvmField val shouldUpdateCandidates: Boolean
    )

    /**
     * 選字上字之後，組字要怎麼處理。
     * - 組字比選中字的碼長，才算還沒打完；
     * - 還沒打完時，從前面切掉實際上字用掉的碼長（超過組字長度就切到空）；
     * - 剩下的碼剛好是一個空白時原樣保留、不再查詢；否則去掉開頭一個空白，
     *   還有剩才再查一次候選。
     */
    @JvmStatic
    fun afterCommit(composing: CharSequence, committedCode: String?, committedCodeLength: Int): AfterCommit {
        val notFinished = committedCode != null && composing.length > committedCode.length
        if (!notFinished) return AfterCommit(false, composing.toString(), false)

        var remaining = composing.toString().substring(committedCodeLength.coerceIn(0, composing.length))
        if (remaining == " ") return AfterCommit(true, remaining, false)
        if (remaining.startsWith(" ")) remaining = remaining.substring(1)
        return AfterCommit(true, remaining, remaining.isNotEmpty())
    }
}
