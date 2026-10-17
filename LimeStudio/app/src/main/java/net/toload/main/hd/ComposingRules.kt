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

    /**
     * 大易連打模式：新鍵接不上目前的碼時原本會先自動上字。已打的全是英文字母、有 3 碼以上，
     * 新鍵也是字母時不自動上字，讓英文單字（例如 book）繼續組成，可以點選或按空白送出。
     */
    @JvmStatic
    fun keepComposingEnglishWord(composing: CharSequence, nextKey: Int): Boolean =
        composing.length >= 3 && composing.all { isAsciiLetter(it.code) } && isAsciiLetter(nextKey)

    /**
     * 大易連打模式：已打的全是數字、有 3 碼以上，新鍵也是數字時不自動上字，
     * 讓數字串（例如 123456789）繼續組成。
     */
    @JvmStatic
    fun keepComposingNumber(composing: CharSequence, nextKey: Int): Boolean =
        composing.length >= 3 && composing.all { isAsciiDigit(it.code) } && isAsciiDigit(nextKey)

    /**
     * 大易長碼串（沒有對應的字、改由連打切分出中文）時，原始碼要不要排在中文前面：
     * 純英文字母（可能是英文單字，如 sorry）或純數字（如 123456789）排最前，空白鍵送出原始碼；
     * 字母、數字、符號混在一起的字根碼（如 nh1dfu）中文排最前。
     */
    @JvmStatic
    fun rawCodeFirst(code: String): Boolean =
        code.isNotEmpty() && (code.all { isAsciiLetter(it.code) } || code.all { isAsciiDigit(it.code) })

    private fun isAsciiLetter(c: Int): Boolean = c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code

    private fun isAsciiDigit(c: Int): Boolean = c in '0'.code..'9'.code
}
