package net.toload.main.hd.limedb

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.util.Log
import androidx.core.database.sqlite.transaction
import net.toload.main.hd.data.Word
import java.io.File

/**
 * 使用者資料的正本 user.db（學過的字碼分數、關聯字、備忘錄），和字庫 lime.db 分開存。
 *
 * 第一次啟動時把 lime.db 裡現有的使用者資料複製過來，lime.db 一筆不動。原始字庫每一列的
 * score 都是 0，所以 score > 0 的列就是使用者資料。先寫到 user.db.tmp，整批在交易中複製、
 * 筆數自我檢查通過才改名成 user.db；中途失敗就刪掉 tmp，下次啟動再來。
 *
 * 搬過之後，LimeDB 每次寫入學習資料都會呼叫這裡的 sync*：從 lime.db 讀出那幾筆的現況
 * 寫進 user.db（雙寫，查詢仍走 lime.db）。還沒搬過就略過，之後搬遷時會一起複製。
 * 同步失敗時標記 dirty，下次啟動從 lime.db 重新複製學習資料（備忘錄不動）。
 * 備忘錄搬過之後只存在 user.db。
 */
class UserDataStore(context: Context) {

    companion object {
        private const val TAG = "UserDataStore"
        const val FILE_NAME = "user.db"
        const val SCHEMA_VERSION = 1

        // 有學習分數的輸入法表；*_user 是舊版重新匯入輸入法時留下的學習備份表
        private val IM_TABLES = listOf("dayi", "phonetic", "custom")

        // 搬遷與所有讀寫共用同一把鎖，避免搬遷途中的寫入被漏掉
        private val LOCK = Any()
        private var shared: UserDataStore? = null

        /** 整個程序共用一個 user.db 連線。 */
        @JvmStatic
        fun get(context: Context): UserDataStore = synchronized(LOCK) {
            val path = context.getDatabasePath(FILE_NAME).path
            shared?.takeIf { it.target.path == path } ?: UserDataStore(context).also {
                shared?.close()
                shared = it
            }
        }

        /** 測試用：每個測試的資料目錄不同，關掉上一個測試留下的共用連線。 */
        @JvmStatic
        fun resetSharedForTest() = synchronized(LOCK) {
            shared?.close()
            shared = null
        }

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
    private var conn: SQLiteDatabase? = null

    /** user.db 已經存在而且搬遷完成。 */
    fun isMigrated(): Boolean = synchronized(LOCK) { openIfMigrated() != null }

    // 搬遷完成（有 migrated_at）才開；開好的連線一直沿用
    private fun openIfMigrated(): SQLiteDatabase? {
        conn?.let { if (it.isOpen) return it }
        conn = null
        if (!target.exists()) return null
        val db = try {
            SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READWRITE)
        } catch (e: Exception) {
            return null
        }
        if (!hasMeta(db, "migrated_at")) {
            db.close()
            return null
        }
        conn = db
        return db
    }

    private fun close() {
        conn?.close()
        conn = null
    }

    /**
     * 還沒搬過就從 lime.db 複製使用者資料到 user.db；已搬過、資料庫鎖定中（匯入字庫）
     * 或失敗時回傳 null。已搬過但上次同步失敗（dirty）時，從 lime.db 重新複製學習資料。
     * lime.db 只讀不寫。
     */
    fun migrateIfNeeded(limeDb: LimeDB): Counts? {
        return synchronized(LOCK) {
            val existing = openIfMigrated()
            if (existing != null) {
                if (!hasMeta(existing, "dual_write") && limeDb.checkDBConnection()) {
                    LimeDB.db?.let { upgradeFromSnapshot(it, existing) }
                } else if (hasMeta(existing, "dirty") && limeDb.checkDBConnection()) {
                    LimeDB.db?.let { resyncLearned(it, existing) }
                }
                return null
            }
            if (!limeDb.checkDBConnection()) return null
            val src = LimeDB.db ?: return null
            close()

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
                    writeMeta(out, "dual_write", "1")
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
    }

    private fun copyAll(src: SQLiteDatabase, out: SQLiteDatabase): Counts {
        val mapping = copyAllMapping(src, out)
        val related = copyAllRelated(src, out)
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
        check(count(out, "memo") == memoCount) { "memo ${count(out, "memo")} != $memoCount" }
        return Counts(mapping, related, memoCount)
    }

    // 學過的字碼複製到空的 learned_mapping；回傳筆數（已自我檢查）
    private fun copyAllMapping(src: SQLiteDatabase, out: SQLiteDatabase): Int {
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
        // 自我檢查：寫進去的筆數要和來源（去重後）一致
        val n = count(out, "learned_mapping")
        check(n == mappingKeys.size) { "mapping $n != ${mappingKeys.size}" }
        return n
    }

    // 學過的關聯字複製到空的 learned_related；回傳筆數（已自我檢查）
    private fun copyAllRelated(src: SQLiteDatabase, out: SQLiteDatabase): Int {
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
        val n = count(out, "learned_related")
        check(n == relatedKeys.size) { "related $n != ${relatedKeys.size}" }
        return n
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

    // 上次同步失敗：學習資料整批以 lime.db 為準重抄，備忘錄不動
    private fun resyncLearned(src: SQLiteDatabase, out: SQLiteDatabase) {
        try {
            out.transaction {
                delete("learned_mapping", null, null)
                delete("learned_related", null, null)
                val mapping = copyAllMapping(src, this)
                val related = copyAllRelated(src, this)
                delete("meta", "key = 'dirty'", null)
                Log.i(TAG, "resynced after dirty: mapping=$mapping related=$related")
            }
        } catch (e: Exception) {
            Log.e(TAG, "resync failed, will retry next start", e)
        }
    }

    // D1-1 版只在搬遷當下複製一次，之後的學習、刪除、備忘錄都只寫 lime.db：
    // 學習資料整批以 lime.db 為準重抄；備忘錄只補上 user.db 沒有的（不刪，避免弄丟）。之後標記為雙寫版
    private fun upgradeFromSnapshot(src: SQLiteDatabase, out: SQLiteDatabase) {
        try {
            out.transaction {
                delete("learned_mapping", null, null)
                delete("learned_related", null, null)
                val mapping = copyAllMapping(src, this)
                val related = copyAllRelated(src, this)
                var memo = 0
                if (hasTable(src, "memo")) {
                    src.rawQuery("SELECT content, pinned, created_at FROM memo", null).use { c ->
                        while (c.moveToNext()) {
                            val createdAt: Long? = if (c.isNull(2)) null else c.getLong(2)
                            val exists = rawQuery(
                                "SELECT 1 FROM memo WHERE content = ? AND created_at IS ?",
                                arrayOf(c.getString(0), createdAt?.toString())
                            ).use { it.moveToFirst() }
                            if (!exists) {
                                val cv = ContentValues()
                                cv.put("content", c.getString(0))
                                cv.put("pinned", c.getInt(1))
                                if (createdAt == null) cv.putNull("created_at") else cv.put("created_at", createdAt)
                                insertOrThrow("memo", null, cv)
                                memo++
                            }
                        }
                    }
                }
                delete("meta", "key = 'dirty'", null)
                writeMeta(this, "dual_write", "1")
                Log.i(TAG, "upgraded snapshot: mapping=$mapping related=$related memo+=$memo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "upgrade failed, will retry next start", e)
        }
    }

    // ---- 雙寫：LimeDB 寫完 lime.db 後呼叫，把那幾筆的現況抄進 user.db ----

    // 還沒搬過就略過（之後搬遷會從 lime.db 一起複製）；失敗就標記 dirty
    private fun write(what: String, block: (src: SQLiteDatabase, out: SQLiteDatabase) -> Unit) {
        synchronized(LOCK) {
            val src = LimeDB.db ?: return
            val out = openIfMigrated() ?: return
            try {
                block(src, out)
            } catch (e: Exception) {
                Log.e(TAG, "$what failed, mark dirty", e)
                try {
                    writeMeta(out, "dirty", "1")
                } catch (e2: Exception) {
                    Log.e(TAG, "mark dirty failed", e2)
                }
            }
        }
    }

    /** 選字加分：這個字在該輸入法所有字碼的分數（LimeDB.addScore 依字更新）。 */
    fun syncMappingWord(im: String?, word: String?) {
        if (im == null || im !in IM_TABLES || word.isNullOrEmpty()) return
        write("syncMappingWord") { src, out -> upsertMapping(src, out, im, "word = ?", arrayOf(word)) }
    }

    /** 新增或更新單一字碼（LimeDB.addOrUpdateMappingRecord）。 */
    fun syncMapping(im: String?, code: String?, word: String?) {
        if (im == null || im !in IM_TABLES || code.isNullOrEmpty() || word.isNullOrEmpty()) return
        write("syncMapping") { src, out -> upsertMapping(src, out, im, "code = ? AND word = ?", arrayOf(code, word)) }
    }

    // 抄 lime.db 的現況（選字加分可能用到快取裡較舊的分數而變低，所以不能只取較高分）；
    // 和搬遷一樣，主表和舊備份表 *_user 都有時取較高分，兩邊都是 0 就從 user.db 刪掉
    private fun upsertMapping(src: SQLiteDatabase, out: SQLiteDatabase, im: String, where: String, args: Array<String>) {
        if (!hasTable(src, im)) return
        val code3r = if (hasColumn(src, im, "code3r")) "code3r" else "NULL"
        val backup = im + "_user"
        val hasBackup = hasTable(src, backup)
        val rows = ArrayList<Array<String?>>()
        src.rawQuery(
            "SELECT code, $code3r, word, MAX(score) FROM $im WHERE $where AND code IS NOT NULL AND word IS NOT NULL " +
                    "GROUP BY code, word",
            args
        ).use { c ->
            while (c.moveToNext()) rows.add(arrayOf(c.getString(0), c.getString(1), c.getString(2), c.getString(3)))
        }
        for (r in rows) {
            val code = r[0]!!
            val word = r[2]!!
            var score = r[3]?.toInt() ?: 0
            var code3rValue = r[1]
            if (hasBackup) {
                val backupCode3r = if (hasColumn(src, backup, "code3r")) "code3r" else "NULL"
                src.rawQuery(
                    "SELECT MAX(score), $backupCode3r FROM $backup WHERE code = ? AND word = ? AND score > 0",
                    arrayOf(code, word)
                ).use { c ->
                    if (c.moveToFirst() && !c.isNull(0) && c.getInt(0) > score) {
                        if (score <= 0) code3rValue = c.getString(1)
                        score = c.getInt(0)
                    }
                }
            }
            if (score > 0) {
                val cv = ContentValues()
                cv.put("im", im)
                cv.put("code", code)
                if (code3rValue == null) cv.putNull("code3r") else cv.put("code3r", code3rValue)
                cv.put("word", word)
                cv.put("score", score)
                out.insertWithOnConflict("learned_mapping", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                out.execSQL(
                    "UPDATE learned_mapping SET score = ? WHERE im = ? AND code = ? AND word = ?",
                    arrayOf<Any>(score, im, code, word)
                )
            } else {
                out.delete("learned_mapping", "im = ? AND code = ? AND word = ?", arrayOf(im, code, word))
            }
        }
    }

    /** 關聯字的 pword/cword（刪除前先查，刪完才能同步）。 */
    fun relatedPair(id: Long): Array<String>? {
        return synchronized(LOCK) {
            val src = LimeDB.db ?: return null
            try {
                src.rawQuery("SELECT pword, cword FROM related WHERE _id = ?", arrayOf(id.toString())).use { c ->
                    if (c.moveToFirst() && !c.isNull(0) && !c.isNull(1)) arrayOf(c.getString(0), c.getString(1)) else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /** 關聯字加分（LimeDB.addScore 依 id 更新）。 */
    fun syncRelatedById(id: Long) {
        val pair = relatedPair(id) ?: return
        syncRelated(pair[0], pair[1])
    }

    /** 這組關聯字在 lime.db 的現況：使用次數 > 0 就寫入，否則從 user.db 刪掉。 */
    fun syncRelated(pword: String?, cword: String?) {
        if (pword.isNullOrEmpty() || cword.isNullOrEmpty()) return
        write("syncRelated") { src, out ->
            val score = src.rawQuery(
                "SELECT MAX(score) FROM related WHERE pword = ? AND cword = ? AND score > 0",
                arrayOf(pword, cword)
            ).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getInt(0) else 0 }
            if (score > 0) {
                val cv = ContentValues()
                cv.put("pword", pword)
                cv.put("cword", cword)
                cv.put("score", score)
                out.insertWithOnConflict("learned_related", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            } else {
                out.delete("learned_related", "pword = ? AND cword = ?", arrayOf(pword, cword))
            }
        }
    }

    /** 關聯字整批變動（清除自建關聯字、匯入關聯字庫）：整張以 lime.db 為準重抄。 */
    fun resyncAllRelated() {
        write("resyncAllRelated") { src, out ->
            out.beginTransaction()
            try {
                out.delete("learned_related", null, null)
                copyAllRelated(src, out)
                out.setTransactionSuccessful()
            } finally {
                out.endTransaction()
            }
        }
    }

    /** 匯入字庫後：匯入檔自帶分數（score > 0）的列收進 user.db，同一組取較高分。 */
    fun absorbIm(im: String?) {
        if (im == null || im !in IM_TABLES) return
        write("absorbIm") { src, out ->
            out.transaction {
                val insert = compileStatement(
                    "INSERT OR IGNORE INTO learned_mapping (im, code, code3r, word, score) VALUES (?, ?, ?, ?, ?)"
                )
                val raise = compileStatement(
                    "UPDATE learned_mapping SET score = MAX(score, ?) WHERE im = ? AND code = ? AND word = ?"
                )
                copyMapping(src, im, im, HashSet(), insert, raise)
                insert.close()
                raise.close()
            }
        }
    }

    /** 這個輸入法學過的字碼（匯入字庫後套回 lime.db 用）；還沒搬過回傳 null。 */
    fun learnedMapping(im: String?): List<Word>? {
        return synchronized(LOCK) {
            val out = openIfMigrated() ?: return null
            val list = ArrayList<Word>()
            if (im == null || im !in IM_TABLES) return list
            out.rawQuery("SELECT code, code3r, word, score FROM learned_mapping WHERE im = ?", arrayOf(im)).use { c ->
                while (c.moveToNext()) {
                    val w = Word()
                    w.code = c.getString(0)
                    w.setCode3r(c.getString(1))
                    w.word = c.getString(2)
                    w.score = c.getInt(3)
                    list.add(w)
                }
            }
            list
        }
    }

    /** 學過的關聯字（匯入我的資料後套回 lime.db 用）；還沒搬過回傳 null。 */
    fun learnedRelated(): List<Array<String>>? {
        return synchronized(LOCK) {
            val out = openIfMigrated() ?: return null
            val list = ArrayList<Array<String>>()
            out.rawQuery("SELECT pword, cword, score FROM learned_related", null).use { c ->
                while (c.moveToNext()) list.add(arrayOf(c.getString(0), c.getString(1), c.getInt(2).toString()))
            }
            list
        }
    }

    /** 匯出：把 user.db 的一致快照寫到 dest（不能已存在）。還沒搬過會先搬；失敗回傳 false。 */
    fun exportTo(limeDb: LimeDB, dest: File): Boolean {
        return synchronized(LOCK) {
            memoDb(limeDb) ?: return false
            // 所有寫入都在這把鎖裡、各自已提交，鎖住時整個檔案就是一致的快照
            target.copyTo(dest, overwrite = true)
            true
        }
    }

    /**
     * 匯入：把另一份 user.db 合併進來。同一組字碼、關聯字取較高分，沒有的補上；
     * 備忘錄內容與建立時間都相同的視為同一則，不重複加入。檔案不對或版本較新回傳 null。
     */
    fun mergeFrom(limeDb: LimeDB, src: File): Counts? {
        return synchronized(LOCK) {
            if (!isValidUserDb(src)) return null
            val out = memoDb(limeDb) ?: return null
            val before = Counts(count(out, "learned_mapping"), count(out, "learned_related"), count(out, "memo"))
            out.execSQL("ATTACH DATABASE ? AS src", arrayOf<Any>(src.path))
            try {
                out.transaction {
                    execSQL(
                        "INSERT OR IGNORE INTO learned_mapping (im, code, code3r, word, score) " +
                                "SELECT im, code, code3r, word, score FROM src.learned_mapping WHERE score > 0"
                    )
                    execSQL(
                        "UPDATE learned_mapping SET score = MAX(score, (SELECT s.score FROM src.learned_mapping s " +
                                "WHERE s.im = learned_mapping.im AND s.code = learned_mapping.code " +
                                "AND s.word = learned_mapping.word)) WHERE EXISTS (SELECT 1 FROM src.learned_mapping s " +
                                "WHERE s.im = learned_mapping.im AND s.code = learned_mapping.code AND s.word = learned_mapping.word)"
                    )
                    execSQL(
                        "INSERT OR IGNORE INTO learned_related (pword, cword, score) " +
                                "SELECT pword, cword, score FROM src.learned_related WHERE score > 0"
                    )
                    execSQL(
                        "UPDATE learned_related SET score = MAX(score, (SELECT s.score FROM src.learned_related s " +
                                "WHERE s.pword = learned_related.pword AND s.cword = learned_related.cword)) " +
                                "WHERE EXISTS (SELECT 1 FROM src.learned_related s " +
                                "WHERE s.pword = learned_related.pword AND s.cword = learned_related.cword)"
                    )
                    execSQL(
                        "INSERT INTO memo (content, pinned, created_at) SELECT content, pinned, created_at FROM src.memo s " +
                                "WHERE NOT EXISTS (SELECT 1 FROM memo m WHERE m.content = s.content " +
                                "AND m.created_at IS s.created_at)"
                    )
                }
            } finally {
                out.execSQL("DETACH DATABASE src")
            }
            Counts(
                count(out, "learned_mapping") - before.mapping,
                count(out, "learned_related") - before.related,
                count(out, "memo") - before.memo
            )
        }
    }

    // 是搬遷完成、版本不比這一版新的 user.db
    private fun isValidUserDb(file: File): Boolean = try {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            hasMeta(db, "migrated_at") && db.version in 1..SCHEMA_VERSION &&
                    hasTable(db, "learned_mapping") && hasTable(db, "learned_related") && hasTable(db, "memo")
        }
    } catch (e: Exception) {
        false
    }

    /** 重設輸入法且不保留學習紀錄：清掉該輸入法學過的字碼。 */
    fun clearIm(im: String?) {
        if (im == null || im !in IM_TABLES) return
        write("clearIm") { _, out -> out.delete("learned_mapping", "im = ?", arrayOf(im)) }
    }

    // ---- 備忘錄：搬過之後只存在 user.db；還沒搬過（或搬不動）回傳 null，由 LimeDB 照舊寫 lime.db ----

    private fun memoDb(limeDb: LimeDB): SQLiteDatabase? {
        openIfMigrated()?.let { return it }
        migrateIfNeeded(limeDb)
        return openIfMigrated()
    }

    fun insertMemo(limeDb: LimeDB, content: String, pinned: Int): Long? {
        return synchronized(LOCK) {
            val db = memoDb(limeDb) ?: return null
            val cv = ContentValues()
            cv.put("content", content)
            cv.put("pinned", pinned)
            cv.put("created_at", System.currentTimeMillis())
            db.insert("memo", null, cv)
        }
    }

    fun getMemos(limeDb: LimeDB): List<MemoObj>? {
        return synchronized(LOCK) {
            val db = memoDb(limeDb) ?: return null
            val list = ArrayList<MemoObj>()
            try {
                db.query(
                    "memo", arrayOf("_id", "content", "pinned", "created_at"), null, null, null, null,
                    "pinned DESC, created_at DESC"
                ).use { c ->
                    while (c.moveToNext()) list.add(MemoObj(c.getInt(0), c.getString(1), c.getInt(2), c.getLong(3)))
                }
            } catch (e: Exception) {
                Log.e(TAG, "getMemos error", e)
            }
            list
        }
    }

    fun deleteMemo(limeDb: LimeDB, id: Int): Int? {
        return synchronized(LOCK) {
            val db = memoDb(limeDb) ?: return null
            db.delete("memo", "_id = ?", arrayOf(id.toString()))
        }
    }

    fun updateMemoPin(limeDb: LimeDB, id: Int, pinned: Int): Int? {
        return synchronized(LOCK) {
            val db = memoDb(limeDb) ?: return null
            val cv = ContentValues()
            cv.put("pinned", pinned)
            db.update("memo", cv, "_id = ?", arrayOf(id.toString()))
        }
    }

    fun updateMemoContent(limeDb: LimeDB, id: Int, content: String): Int? {
        return synchronized(LOCK) {
            val db = memoDb(limeDb) ?: return null
            val cv = ContentValues()
            cv.put("content", content)
            db.update("memo", cv, "_id = ?", arrayOf(id.toString()))
        }
    }

    private fun writeMeta(db: SQLiteDatabase, key: String, value: String) {
        val cv = ContentValues()
        cv.put("key", key)
        cv.put("value", value)
        db.insertWithOnConflict("meta", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun hasMeta(db: SQLiteDatabase, key: String): Boolean = try {
        db.rawQuery("SELECT 1 FROM meta WHERE key = ?", arrayOf(key)).use { it.moveToFirst() }
    } catch (e: Exception) {
        false
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
