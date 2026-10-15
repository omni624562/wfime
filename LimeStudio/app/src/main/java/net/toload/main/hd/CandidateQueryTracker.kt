package net.toload.main.hd

import net.toload.main.hd.data.Mapping

/**
 * 候選查詢的先後順序與套用規則（從 LIMEService 抽出，純邏輯）。
 *
 * 每次組字改變就送出一次查詢，查詢在背景執行、結果排到主執行緒才套用，
 * 所以結果到達的順序和送出的順序不一定相同，按鍵也可能比結果先到。這裡負責：
 * - 只套用最新一次查詢、而且組字沒變的結果（舊結果一律丟掉）
 * - 記住查詢執行緒已算好、還在排隊的最新結果，選字鍵搶先時可以直接套用
 * - 電話鍵盤自動上字：這一碼的結果還沒到時，記下來等結果到了再上字
 *
 * 序號只在主執行緒讀寫；排隊中的結果由查詢執行緒寫入。
 */
class CandidateQueryTracker {

    /** 查詢執行緒算好、還在主執行緒排隊等著套用的候選結果。 */
    class Pending(
        @JvmField val seq: Int,
        @JvmField val code: String,
        @JvmField val list: List<Mapping>,
        @JvmField val physical: Boolean,
        @JvmField val selkey: String?
    )

    /** 最新送出的查詢序號。 */
    var latestSeq = 0
        private set

    /** 結果已套用到候選列的查詢序號。 */
    var appliedSeq = 0
        private set

    // 電話鍵盤自動上字：等這個序號的結果到了再上字（-1 表示沒有等待中的上字）
    private var autoCommitSeq = -1

    @Volatile
    private var pending: Pending? = null

    /** 送出新查詢，回傳這次查詢的序號。 */
    fun startQuery(): Int = ++latestSeq

    /** 最新一次查詢的結果已經套用（候選清單就是目前組字的結果）。 */
    fun isLatestApplied(): Boolean = appliedSeq == latestSeq

    /** 查詢執行緒：記下算好的結果（可能比較舊，取用時再比對序號）。 */
    fun publish(result: Pending) {
        pending = result
    }

    /** 排隊中的結果正好是最新一次查詢的，才回傳；否則 null。 */
    fun latestPending(): Pending? = pending?.takeIf { it.seq == latestSeq }

    /**
     * 主執行緒收到查詢結果時呼叫：是最新一次查詢、組字也沒變才套用，
     * 並記為已套用。回傳 false 時呼叫端應該丟掉這個結果。
     */
    fun acceptResult(seq: Int, code: String, composing: CharSequence): Boolean {
        if (seq != latestSeq || !code.contentEquals(composing)) return false
        appliedSeq = seq
        return true
    }

    /** 直接套用排隊中的結果（選字鍵搶在結果之前）時，記為已套用。 */
    fun markApplied(seq: Int) {
        appliedSeq = seq
    }

    /** 電話鍵盤自動上字：最新一碼的結果還沒到，等結果到了再上字。 */
    fun requestAutoCommitOnArrival() {
        autoCommitSeq = latestSeq
    }

    /**
     * 結果套用時呼叫：如果這個序號有等待中的自動上字就清掉並回傳 true。
     * 查無結果時也要呼叫（清掉等待，但不上字）。
     */
    fun takeAutoCommit(seq: Int): Boolean {
        if (autoCommitSeq != seq) return false
        autoCommitSeq = -1
        return true
    }
}
