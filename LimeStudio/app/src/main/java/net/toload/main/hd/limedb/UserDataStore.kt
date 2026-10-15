package net.toload.main.hd.limedb

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.util.Log
import java.io.File

/**
 * 使用者資料的正本 user.db（學過的字碼分數、關聯字、備忘錄），和字庫 lime.db 分開存。
 *
 * D1-1 只負責建立 user.db 並在第一次啟動時把 lime.db 裡現有的使用者資料複製過來，
 * lime.db 一筆不動。原始字庫每一列的 score 都是 0，所以 score > 0 的列就是使用者資料。
 * 先寫到 user.db.tmp，整批在交易中複製、筆數自我檢查通過才改名成 user.db；
 * 中途失敗就刪掉 tmp，下次啟動再來。
 */
class UserDataStore(context: Context) {

    companion object {
        private const val TAG = "UserDataStore"
        const val FILE_NAME = "user.db"
        const val SCHEMA_VERSION = 1

        // 有學習分數的輸入法表；*_user 是舊版重新匯入輸入法時留下的學習備份表
        private val IM_TABLES = listOf("dayi", "phonetic", "custom")

        /** 建立 user.db 的資料表（全新檔案才會呼叫）。 */
        @JvmStatic
        fun createSchema(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE learned_mapping (im TEXT NOT NULL, code TEXT NOT NULL, code3r TEXT, " +
                        "word TEXT NOT NULL, score INTEGER NOT NULL, PRIMARY KEY (im, code, word))"
            )
            db.execSQL(
                "CREATE TABLE learned_related (pword TEXT NOT NULL, cword TEXT NOT NULL, " +
                        "score INTEGER NOT NULL, PRIMARY KEY (pword, cword))"
            )
            db.execSQL(
                "CREATE TABLE memo (_id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL, " +
                        "pinned INTEGER DEFAULT 0, created_at INTEGER)"
            )
            db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)")
            db.version = SCHEMA_VERSION
        }
    }

    /** 搬遷結果的筆數（給測試與 log 用）。 */
    class Counts(
        @JvmField val mapping: Int,
        @JvmField val related: Int,
        @JvmField val memo: Int
    )

    private val target: File = context.getDatabasePath(FILE_NAME)

    /** user.db 已經存在而且搬遷完成。 */
    fun isMigrated(): Boolean {
        if (!target.exists()) return false
        return try {
            SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT value FROM meta WHERE key = 'migrated_at'", null).use { it.moveToFirst() }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 還沒搬過就從 lime.db 複製使用者資料到 user.db；已搬過、資料庫鎖定中（匯入字庫）
     * 或失敗時回傳 null。lime.db 只讀不寫。
     */
    fun migrateIfNeeded(limeDb: LimeDB): Counts? {
        if (isMigrated()) return null
        if (!limeDb.checkDBConnection()) return null
        val src = LimeDB.db ?: return null

        val tmp = File(target.path + ".tmp")
        deleteDbFiles(tmp)
        val startedAt = System.currentTimeMillis()
        var out: SQLiteDatabase? = null
        return try {
            out = SQLiteDatabase.openOrCreateDatabase(tmp, null)
            createSchema(out)
            out.beginTransaction()
            val counts: Counts
            try {
                counts = copyAll(src, out)
                writeMeta(out, "schema_version", SCHEMA_VERSION.toString())
                writeMeta(out, "source_db_version", src.version.toString())
                writeMeta(out, "migrated_at", System.currentTimeMillis().toString())
                out.setTransactionSuccessful()
            } finally {
                out.endTransaction()
            }
            out.close()
            out = null
            deleteDbFiles(target)
            if (!tmp.renameTo(target)) throw IllegalStateException("rename user.db.tmp failed")
            deleteDbFiles(tmp)
            Log.i(TAG, "migrated: mapping=${counts.mapping} related=${counts.related} memo=${counts.memo} " +
                    "in ${System.currentTimeMillis() - startedAt} ms")
            counts
        } catch (e: Exception) {
            Log.e(TAG, "migration failed, will retry next start", e)
            out?.close()
            deleteDbFiles(tmp)
            null
        }
    }

    private fun copyAll(src: SQLiteDatabase, out: SQLiteDatabase): Counts {
        val mappingKeys = HashSet<String>()
        // 預先編譯、每列重複使用，不要每一列都重新編譯 SQL
        val insertMapping = out.compileStatement(
            "INSERT OR IGNORE INTO learned_mapping (im, code, code3r, word, score) VALUES (?, ?, ?, ?, ?)"
        )
        val raiseMapping = out.compileStatement(
            "UPDATE learned_mapping SET score = MAX(score, ?) WHERE im = ? AND code = ? AND word = ?"
        )
        for (im in IM_TABLES) {
            copyMapping(src, im, im, mappingKeys, insertMapping, raiseMapping)
            copyMapping(src, im + "_user", im, mappingKeys, insertMapping, raiseMapping)
        }
        insertMapping.close()
        raiseMapping.close()
        val relatedKeys = HashSet<String>()
        val insertRelated = out.compileStatement(
            "INSERT OR IGNORE INTO learned_related (pword, cword, score) VALUES (?, ?, ?)"
        )
        val raiseRelated = out.compileStatement(
            "UPDATE learned_related SET score = MAX(score, ?) WHERE pword = ? AND cword = ?"
        )
        if (hasTable(src, "related")) {
            src.rawQuery(
                "SELECT pword, cword, score FROM related WHERE score > 0 AND pword IS NOT NULL AND cword IS NOT NULL",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    val pword = c.getString(0)
                    val cword = c.getString(1)
                    val score = c.getInt(2)
                    relatedKeys.add(pword + "\u0000" + cword)
                    insertRelated.bindString(1, pword)
                    insertRelated.bindString(2, cword)
                    insertRelated.bindLong(3, score.toLong())
                    insertRelated.executeInsert()
                    raiseRelated.bindLong(1, score.toLong())
                    raiseRelated.bindString(2, pword)
                    raiseRelated.bindString(3, cword)
                    raiseRelated.executeUpdateDelete()
                }
            }
        }
        insertRelated.close()
        raiseRelated.close()
        var memoCount = 0
        if (hasTable(src, "memo")) {
            src.rawQuery("SELECT _id, content, pinned, created_at FROM memo", null).use { c ->
                while (c.moveToNext()) {
                    val cv = ContentValues()
                    cv.put("_id", c.getLong(0))
                    cv.put("content", c.getString(1))
                    cv.put("pinned", c.getInt(2))
                    if (c.isNull(3)) cv.putNull("created_at") else cv.put("created_at", c.getLong(3))
                    out.insertOrThrow("memo", null, cv)
                    memoCount++
                }
            }
        }

        // 自我檢查：寫進去的筆數要和來源（去重後）一致
        val counts = Counts(count(out, "learned_mapping"), count(out, "learned_related"), count(out, "memo"))
        check(counts.mapping == mappingKeys.size) { "mapping ${counts.mapping} != ${mappingKeys.size}" }
        check(counts.related == relatedKeys.size) { "related ${counts.related} != ${relatedKeys.size}" }
        check(counts.memo == memoCount) { "memo ${counts.memo} != $memoCount" }
        return counts
    }

    // 同一個 (im, code, word) 在主表重複、或主表和舊備份表都有時，保留較高的分數
    private fun copyMapping(
        src: SQLiteDatabase, table: String, im: String, keys: HashSet<String>,
        insert: SQLiteStatement, raise: SQLiteStatement
    ) {
        if (!hasTable(src, table)) return
        val code3r = if (hasColumn(src, table, "code3r")) "code3r" else "NULL"
        src.rawQuery(
            "SELECT code, $code3r, word, score FROM $table " +
                    "WHERE score > 0 AND code IS NOT NULL AND word IS NOT NULL",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val code = c.getString(0)
                val word = c.getString(2)
                val score = c.getInt(3)
                keys.add(im + "\u0000" + code + "\u0000" + word)
                insert.bindString(1, im)
                insert.bindString(2, code)
                if (c.isNull(1)) insert.bindNull(3) else insert.bindString(3, c.getString(1))
                insert.bindString(4, word)
                insert.bindLong(5, score.toLong())
                insert.executeInsert()
                raise.bindLong(1, score.toLong())
                raise.bindString(2, im)
                raise.bindString(3, code)
                raise.bindString(4, word)
                raise.executeUpdateDelete()
            }
        }
    }

    private fun writeMeta(db: SQLiteDatabase, key: String, value: String) {
        val cv = ContentValues()
        cv.put("key", key)
        cv.put("value", value)
        db.insertWithOnConflict("meta", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun count(db: SQLiteDatabase, table: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    private fun hasTable(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use { it.moveToFirst() }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            while (c.moveToNext()) if (c.getString(1) == column) return@use true
            false
        }

    private fun deleteDbFiles(file: File) {
        for (suffix in listOf("", "-journal", "-wal", "-shm")) File(file.path + suffix).delete()
    }
}
