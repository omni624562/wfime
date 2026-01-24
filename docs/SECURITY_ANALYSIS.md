# SQL Injection Security Analysis | SQL 注入安全分析

**Analysis Date:** 2026-01-09
**Project:** WFIME (LimeIME fork)
**Severity:** MEDIUM - Potential SQL Injection Vulnerabilities
**Status:** Requires Review and Remediation

---

## Executive Summary | 執行摘要

This analysis identifies **potential SQL injection vulnerabilities** in the WFIME database layer. The codebase uses raw SQL queries with string concatenation in multiple locations, which could allow malicious input to manipulate database operations.

本分析識別出 WFIME 資料庫層中的**潛在 SQL 注入漏洞**。程式碼在多處使用字串串接的原始 SQL 查詢，可能允許惡意輸入操縱資料庫操作。

**Risk Level:** MEDIUM
**風險等級：** 中等

While this is an IME (Input Method Editor) app with primarily local database usage, SQL injection vulnerabilities should still be addressed to prevent potential data corruption or unauthorized access to user dictionaries.

雖然這是一個主要使用本地資料庫的 IME（輸入法編輯器）應用程式，但仍應處理 SQL 注入漏洞，以防止潛在的資料損壞或未經授權存取使用者字典。

---

## Vulnerability Analysis | 漏洞分析

### 1. String Concatenation in SQL Queries | SQL 查詢中的字串串接

**File:** `LimeDB.java` (210KB)
**Location:** Multiple locations throughout the file

#### High-Risk Patterns Found | 發現的高風險模式

**Pattern 1: Dynamic Table Names | 動態表名**
```java
// Line 741
cursor = db.rawQuery("SELECT * FROM " + table, null);

// Lines 2713-2730 (Database attach operations)
db.execSQL("attach database '" + sourcedbfile + "' as sourceDB");
db.execSQL("insert into sourceDB." + Lime.DB_RELATED + " select * from " + Lime.DB_RELATED);
db.execSQL("insert into sourceDB." + sourcetable + " select * from " + sourcetable);
```

**Risk:** If `table`, `sourcedbfile`, or `sourcetable` variables contain user-supplied input without validation, attackers could inject malicious SQL.

**風險：** 如果 `table`、`sourcedbfile` 或 `sourcetable` 變數包含未經驗證的使用者輸入，攻擊者可能注入惡意 SQL。

**Pattern 2: WHERE Clause Injection | WHERE 子句注入**
```java
// Line 2758
db.execSQL("delete from " + Lime.DB_IM + " where " + Lime.DB_IM_COLUMN_CODE + "='" + imtype + "'");

// Line 2727
db.execSQL("insert into sourceDB." + Lime.DB_IM + " select * from " + Lime.DB_IM + " WHERE code='" + sourcetable + "'");

// Line 2729
db.execSQL("update sourceDB." + Lime.DB_IM + " set " + Lime.DB_IM_COLUMN_CODE + "='" + sourcetable + "'");
```

**Risk:** The `imtype` and `sourcetable` parameters are directly embedded in SQL queries with single quotes, allowing SQL injection if not properly sanitized.

**風險：** `imtype` 和 `sourcetable` 參數直接嵌入帶有單引號的 SQL 查詢中，如果未正確清理，則允許 SQL 注入。

**Pattern 3: DDL Operations | DDL 操作**
```java
// Line 2805-2809
db.execSQL("drop table " + backupTableName);
db.execSQL("create table " + backupTableName + " as " + selectString);

// Line 4266
db.execSQL("ALTER TABLE " + source + " RENAME TO " + target);
```

**Risk:** DDL operations (CREATE, DROP, ALTER) with user-controlled table names can lead to data loss or privilege escalation.

**風險：** 使用使用者控制的表名進行 DDL 操作（CREATE、DROP、ALTER）可能導致資料遺失或權限提升。

---

## Attack Scenarios | 攻擊場景

### Scenario 1: Malicious Database File Import | 惡意資料庫檔案匯入

**Attack Vector:**
```java
// If sourcedbfile = "'; DROP TABLE custom; --"
db.execSQL("attach database '" + sourcedbfile + "' as sourceDB");
// Becomes: attach database ''; DROP TABLE custom; --' as sourceDB
```

**Impact:** Could drop tables, corrupt data, or execute arbitrary SQL commands.

**影響：** 可能刪除表、損壞資料或執行任意 SQL 命令。

### Scenario 2: Input Method Code Injection | 輸入法代碼注入

**Attack Vector:**
```java
// If imtype = "custom'; DELETE FROM im WHERE '1'='1"
db.execSQL("delete from " + Lime.DB_IM + " where " + Lime.DB_IM_COLUMN_CODE + "='" + imtype + "'");
// Becomes: delete from im where code='custom'; DELETE FROM im WHERE '1'='1'
```

**Impact:** Mass deletion of input method configurations.

**影響：** 大量刪除輸入法配置。

### Scenario 3: Table Name Manipulation | 表名操縱

**Attack Vector:**
```java
// If table = "phonetic UNION SELECT * FROM sensitive_table--"
cursor = db.rawQuery("SELECT * FROM " + table, null);
// Becomes: SELECT * FROM phonetic UNION SELECT * FROM sensitive_table--
```

**Impact:** Unauthorized data access from other tables.

**影響：** 未經授權存取其他表的資料。

---

## Mitigation Status | 緩解狀態

### Current Protection Mechanisms | 當前保護機制

✅ **Local Database Only:** The app uses a local SQLite database, reducing remote attack surface.

✅ **僅本地資料庫：** 應用程式使用本地 SQLite 資料庫，減少遠程攻擊面。

⚠️ **Limited Input Validation:** Some table names are validated against predefined constants (`Lime.DB_TABLE_*`), but not consistently.

⚠️ **有限的輸入驗證：** 某些表名根據預定義常數（`Lime.DB_TABLE_*`）進行驗證，但不一致。

❌ **No Parameterized Queries:** Raw SQL with string concatenation is used throughout.

❌ **無參數化查詢：** 整個程式碼中使用字串串接的原始 SQL。

---

## Recommendations | 建議

### Immediate Actions (High Priority) | 立即行動（高優先級）

**1. Input Validation for Table Names | 表名輸入驗證**

Create a whitelist of valid table names and validate all table parameters:

建立有效表名的白名單並驗證所有表參數：

```java
private static final Set<String> VALID_TABLES = new HashSet<>(Arrays.asList(
    Lime.DB_TABLE_CUSTOM, "phonetic", "dayi", "array", "cj",
    Lime.DB_IM, Lime.DB_RELATED, Lime.DB_KEYBOARD
));

private boolean isValidTableName(String tableName) {
    return VALID_TABLES.contains(tableName) ||
           tableName.matches("^[a-zA-Z][a-zA-Z0-9_]{0,63}$");
}

// Use before any SQL operation
if (!isValidTableName(table)) {
    throw new IllegalArgumentException("Invalid table name: " + table);
}
```

**2. Sanitize File Paths | 清理檔案路徑**

Validate database file paths to prevent injection through file names:

驗證資料庫檔案路徑以防止通過檔案名注入：

```java
private String sanitizeDbPath(String path) {
    File file = new File(path);
    try {
        String canonical = file.getCanonicalPath();
        if (!canonical.startsWith(getExpectedDbDirectory())) {
            throw new SecurityException("Database path outside allowed directory");
        }
        return canonical;
    } catch (IOException e) {
        throw new IllegalArgumentException("Invalid database path", e);
    }
}
```

**3. Use Parameterized Queries Where Possible | 盡可能使用參數化查詢**

Replace string concatenation with parameterized queries for WHERE clauses:

將 WHERE 子句的字串串接替換為參數化查詢：

```java
// Before (Vulnerable)
db.execSQL("delete from " + Lime.DB_IM + " where code='" + imtype + "'");

// After (Safe)
db.execSQL("delete from " + Lime.DB_IM + " where code=?", new String[]{imtype});
```

**Note:** Table names cannot be parameterized in SQL, so validation is mandatory.

**注意：** 表名無法在 SQL 中參數化，因此驗證是強制性的。

### Medium-Term Improvements | 中期改進

**4. Migrate to Room Database | 遷移到 Room 資料庫**

Consider migrating from raw SQLite to Room for type-safe, compile-time verified queries:

考慮從原始 SQLite 遷移到 Room，以獲得類型安全、編譯時驗證的查詢：

```kotlin
@Dao
interface WordDao {
    @Query("SELECT * FROM :tableName")
    suspend fun getAllWords(tableName: String): List<Word>

    @Insert
    suspend fun insert(word: Word)

    @Delete
    suspend fun delete(word: Word)
}
```

**Benefits:**
- Compile-time SQL validation
- Type-safe queries
- Automatic parameterization
- Better testing support

**優勢：**
- 編譯時 SQL 驗證
- 類型安全查詢
- 自動參數化
- 更好的測試支援

### Long-Term Enhancements | 長期增強

**5. Security Audit and Penetration Testing | 安全稽核和滲透測試**

Conduct a comprehensive security audit focusing on:
- All database operations
- File import/export functions
- User dictionary management
- Related word associations

進行全面的安全稽核，重點關注：
- 所有資料庫操作
- 檔案匯入/匯出功能
- 使用者字典管理
- 相關詞關聯

**6. Add Security Documentation | 新增安全文件**

Document secure coding practices for future contributors:
- SQL injection prevention guidelines
- Input validation requirements
- Database operation best practices

為未來貢獻者記錄安全編碼實踐：
- SQL 注入預防指南
- 輸入驗證要求
- 資料庫操作最佳實踐

---

## Impact Assessment | 影響評估

### Exploitability | 可利用性

**Difficulty Level:** MEDIUM
**難度級別：** 中等

- Requires malicious file import or crafted input method configurations
- Attacker needs local device access or social engineering
- No direct network attack vector

- 需要惡意檔案匯入或精心製作的輸入法配置
- 攻擊者需要本地裝置存取或社交工程
- 無直接網路攻擊向量

### Potential Damage | 潛在損害

**Severity:** MEDIUM to HIGH
**嚴重性：** 中等到高

- **Data Corruption:** Deletion or modification of user dictionaries (HIGH)
- **Privacy Breach:** Unauthorized access to user typing history (MEDIUM)
- **App Malfunction:** Corrupted database causing crashes (MEDIUM)
- **System Access:** Limited to app sandbox (LOW)

- **資料損壞：** 刪除或修改使用者字典（高）
- **隱私洩露：** 未經授權存取使用者輸入歷史（中）
- **應用程式故障：** 損壞的資料庫導致崩潰（中）
- **系統存取：** 限於應用程式沙盒（低）

---

## Testing Recommendations | 測試建議

### Unit Tests | 單元測試

Create unit tests for input validation:

為輸入驗證創建單元測試：

```java
@Test
public void testTableNameValidation() {
    assertTrue(isValidTableName("phonetic"));
    assertTrue(isValidTableName("custom"));
    assertFalse(isValidTableName("'; DROP TABLE--"));
    assertFalse(isValidTableName("../../../etc/passwd"));
}

@Test
public void testSqlInjectionPrevention() {
    String maliciousInput = "custom'; DELETE FROM im; --";
    assertThrows(IllegalArgumentException.class, () -> {
        deleteTable(maliciousInput);
    });
}
```

### Integration Tests | 整合測試

Test file import with malicious filenames and content.

使用惡意檔案名和內容測試檔案匯入。

---

## Compliance Notes | 合規性說明

### Android Security Best Practices | Android 安全最佳實踐

✅ **App Sandbox:** Android's app sandbox limits damage scope
✅ **應用程式沙盒：** Android 的應用程式沙盒限制損害範圍

⚠️ **Input Validation:** Needs improvement for file imports
⚠️ **輸入驗證：** 檔案匯入需要改進

❌ **Parameterized Queries:** Not consistently used
❌ **參數化查詢：** 未一致使用

### OWASP Mobile Top 10 | OWASP 行動應用程式十大風險

**M4: Insecure Data Storage** - Partially applicable (user dictionaries)
**M4：不安全的資料儲存** - 部分適用（使用者字典）

**M7: Client Code Quality** - SQL injection vulnerabilities
**M7：客戶端程式碼品質** - SQL 注入漏洞

---

## Conclusion | 結論

The WFIME project contains **multiple SQL injection vulnerabilities** primarily in the `LimeDB.java` database layer. While the local-only nature of the database reduces remote attack risk, these vulnerabilities should be addressed to prevent:

WFIME 專案在 `LimeDB.java` 資料庫層中包含**多個 SQL 注入漏洞**。雖然資料庫的僅本地性質降低了遠程攻擊風險，但應處理這些漏洞以防止：

1. **Data corruption through malicious file imports** | 通過惡意檔案匯入導致的資料損壞
2. **Unauthorized access to user dictionaries** | 未經授權存取使用者字典
3. **App crashes from database corruption** | 資料庫損壞導致的應用程式崩潰

**Recommended Priority:** Address table name validation and file path sanitization before next production release.

**建議優先級：** 在下一個生產版本發布前處理表名驗證和檔案路徑清理。

**Next Steps:**
1. Implement table name whitelist validation
2. Add file path sanitization for imports
3. Replace vulnerable string concatenation with parameterized queries where possible
4. Consider Room database migration for long-term maintainability

**後續步驟：**
1. 實作表名白名單驗證
2. 為匯入新增檔案路徑清理
3. 盡可能用參數化查詢替換易受攻擊的字串串接
4. 考慮 Room 資料庫遷移以實現長期可維護性

---

**Document Version:** 1.0
**Last Updated:** 2026-01-09
**Reviewed By:** Claude Code Analysis Agent
