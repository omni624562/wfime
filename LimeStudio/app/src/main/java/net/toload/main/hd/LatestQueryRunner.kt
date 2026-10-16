package net.toload.main.hd

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * 候選、關聯字、英文預測的查詢都交給這裡：新查詢自動取消上一個，只有最新一次查詢的結果
 * 會在主執行緒套用（被取代的查詢就算已經算完，結果也不會套用）。
 *
 * 查詢本身在單一背景執行緒依序執行（資料庫存取不並行）；被取消時中斷該執行緒，
 * 查詢內原本用 Thread.interrupted 判斷「已有更新的查詢」的地方照樣有效。
 */
class LatestQueryRunner(
    private val workDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)

    @Volatile
    private var job: Job? = null

    @Volatile
    private var workDone: CompletableFuture<Unit>? = null

    /**
     * 在主執行緒呼叫。work 在背景執行緒執行；完成時若這次查詢還是最新的，
     * apply 在主執行緒收到結果。
     */
    fun <T> submit(work: Callable<T>, apply: Consumer<T>) {
        job?.cancel()
        val done = CompletableFuture<Unit>()
        workDone = done
        // 「做完」要在背景執行緒標記：主執行緒在 awaitWork 等的時候，協程回不到主執行緒
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = runInterruptible(workDispatcher) {
                try {
                    work.call()
                } finally {
                    done.complete(Unit)
                }
            }
            apply.accept(result)
        }
    }

    /** 取消目前的查詢（結果不會套用）。 */
    fun cancel() {
        job?.cancel()
        // 排隊中的查詢要等背景執行緒空出來才會真正結束；取消了就不用再等
        workDone?.complete(Unit)
    }

    /**
     * 等最新一次查詢的背景工作做完，最多 timeoutMs 毫秒（不等主執行緒套用）。
     * 沒有查詢或已做完回傳 true，逾時回傳 false。
     */
    fun awaitWork(timeoutMs: Long): Boolean {
        val done = workDone ?: return true
        return try {
            done.get(timeoutMs, TimeUnit.MILLISECONDS)
            true
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        } catch (e: Exception) {
            false
        }
    }

    /** 服務結束時呼叫：取消所有查詢。 */
    fun shutdown() {
        scope.cancel()
    }
}
