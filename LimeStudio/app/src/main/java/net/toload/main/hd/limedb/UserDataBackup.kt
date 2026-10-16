package net.toload.main.hd.limedb

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import net.toload.main.hd.DBServer
import net.toload.main.hd.global.SmartSelectionManager
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 「我的資料」單一檔案匯出／匯入（zip）：user.db（學過的字碼、關聯字、備忘錄）＋智慧選字紀錄。
 * 匯入是合併：兩邊都保留，同一組取較高分，重複匯入同一個檔不會重複。
 */
object UserDataBackup {
    private const val TAG = "UserDataBackup"
    private const val ENTRY_DB = "user.db"
    private const val ENTRY_SMART = "smart_selection.json"
    private const val ENTRY_MANIFEST = "manifest.json"
    private const val FORMAT = 1
    private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024

    /** 匯出或匯入的筆數；smart 是智慧選字紀錄筆數。 */
    class Result(
        @JvmField val mapping: Int,
        @JvmField val related: Int,
        @JvmField val memo: Int,
        @JvmField val smart: Int
    )

    /** 寫成 zip 到 out；失敗回傳 null。 */
    @JvmStatic
    fun exportData(context: Context, out: OutputStream): Result? {
        val tmp = File(context.cacheDir, "userdata_export.db")
        return try {
            val limeDb = LimeDB(context)
            if (!UserDataStore.get(context).exportTo(limeDb, tmp)) return null
            val smartJson = SmartSelectionManager.getInstance(context).exportJson()
            val result = SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                Result(count(db, "learned_mapping"), count(db, "learned_related"), count(db, "memo"),
                    JSONObject(smartJson).let { root -> root.keys().asSequence().sumOf { root.getJSONObject(it).length() } })
            }
            ZipOutputStream(out).use { zip ->
                val manifest = JSONObject()
                    .put("format", FORMAT)
                    .put("schema_version", UserDataStore.SCHEMA_VERSION)
                    .put("exported_at", System.currentTimeMillis())
                zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(ENTRY_DB))
                tmp.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(ENTRY_SMART))
                zip.write(smartJson.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "export failed", e)
            null
        } finally {
            tmp.delete()
        }
    }

    /** 從 zip 合併進來並套回 lime.db；不是本程式的資料檔或失敗回傳 null。回傳的是新增的筆數。 */
    @JvmStatic
    fun importData(context: Context, input: InputStream): Result? {
        val dir = File(context.cacheDir, "userdata_import")
        dir.deleteRecursively()
        dir.mkdirs()
        return try {
            // 只收認得的檔名、寫到固定路徑（不照 zip 裡的路徑寫檔）
            var dbFile: File? = null
            var smartJson: String? = null
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        ENTRY_DB -> dbFile = File(dir, ENTRY_DB).also { copyLimited(zip, it) }
                        ENTRY_SMART -> smartJson = File(dir, ENTRY_SMART).also { copyLimited(zip, it) }
                            .readText(Charsets.UTF_8)
                    }
                }
            }
            val db = dbFile ?: return null
            val limeDb = LimeDB(context)
            val counts = UserDataStore.get(context).mergeFrom(limeDb, db) ?: return null
            limeDb.restoreAllFromUserData()
            val smart = smartJson?.let { SmartSelectionManager.getInstance(context).mergeJson(it) } ?: 0
            DBServer.resetCache()
            Result(counts.mapping, counts.related, counts.memo, smart)
        } catch (e: Exception) {
            Log.e(TAG, "import failed", e)
            null
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun copyLimited(input: InputStream, dest: File) {
        dest.outputStream().use { out ->
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                check(total <= MAX_ENTRY_BYTES) { "entry too large" }
                out.write(buf, 0, n)
            }
        }
    }

    private fun count(db: SQLiteDatabase, table: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
}
