# Contributing to WFIME | 貢獻至 WFIME

Thank you for your interest in contributing to WFIME (Wheat Fields Input Method Editor)! We welcome contributions from the community to help improve this Android IME application.

感謝您對貢獻 WFIME（麥田輸入法編輯器）的興趣！我們歡迎社群貢獻，協助改進這個 Android 輸入法應用程式。

---

## Table of Contents | 目錄

- [Code of Conduct](#code-of-conduct--行為準則)
- [Getting Started](#getting-started--開始使用)
- [Development Workflow](#development-workflow--開發流程)
- [Coding Standards](#coding-standards--編碼標準)
- [Commit Guidelines](#commit-guidelines--提交指南)
- [Pull Request Process](#pull-request-process--拉取請求流程)
- [Areas for Contribution](#areas-for-contribution--貢獻領域)
- [Testing](#testing--測試)
- [Security](#security--安全性)
- [Questions and Support](#questions-and-support--問題與支援)

---

## Code of Conduct | 行為準則

### Our Standards | 我們的標準

We are committed to providing a welcoming and inclusive environment. All contributors are expected to:

我們致力於提供歡迎和包容的環境。所有貢獻者應：

- **Be respectful** - Treat everyone with respect and consideration | **保持尊重** - 以尊重和體貼對待每個人
- **Be collaborative** - Work together constructively | **協作合作** - 建設性地共同合作
- **Be professional** - Keep discussions focused on technical merit | **保持專業** - 討論聚焦於技術優點
- **Be open-minded** - Welcome feedback and different perspectives | **保持開放** - 歡迎回饋和不同觀點

### Unacceptable Behavior | 不可接受的行為

- Harassment, discrimination, or offensive comments | 騷擾、歧視或冒犯性言論
- Personal attacks or trolling | 人身攻擊或惡意挑釁
- Publishing private information without consent | 未經同意發布私人資訊
- Other conduct inappropriate in a professional setting | 專業環境中不當的其他行為

---

## Getting Started | 開始使用

### Prerequisites | 先決條件

Before contributing, ensure you have:

貢獻前，請確保您具備：

**Development Environment | 開發環境**
- **Android Studio** - Latest stable version (Ladybug | 2024.2.1+) | **Android Studio** - 最新穩定版本（Ladybug | 2024.2.1+）
- **JDK** - Java Development Kit 17+ | **JDK** - Java 開發工具包 17+
- **Git** - Version control system | **Git** - 版本控制系統
- **Android SDK** - API 30 (minimum) to API 35 (target) | **Android SDK** - API 30（最低）至 API 35（目標）

**Knowledge Requirements | 知識需求**
- Java and/or Kotlin programming | Java 和/或 Kotlin 程式設計
- Android development fundamentals | Android 開發基礎
- Material Design 3 principles (optional) | Material Design 3 原則（選用）
- SQLite database basics (for database work) | SQLite 資料庫基礎（資料庫工作）

### Setting Up Development Environment | 設置開發環境

**1. Fork and Clone | 複刻和克隆**
```bash
# Fork the repository on GitHub
# 在 GitHub 上複刻儲存庫

# Clone your fork
# 克隆您的複刻
git clone https://github.com/YOUR_USERNAME/nanime-main.git
cd nanime-main/LimeStudio
```

**2. Configure Git | 配置 Git**
```bash
# Set up your identity
# 設定您的身份
git config user.name "Your Name"
git config user.email "your.email@example.com"

# Add upstream remote
# 新增上游遠端
git remote add upstream https://github.com/omni624562/nanime-main.git
```

**3. Open in Android Studio | 在 Android Studio 中開啟**
- File → Open → Select `LimeStudio` directory | 檔案 → 開啟 → 選擇 `LimeStudio` 目錄
- Wait for Gradle sync to complete | 等待 Gradle 同步完成
- Ensure SDK paths are configured correctly | 確保 SDK 路徑配置正確

**4. Build and Run | 建置和執行**
```bash
# Build the project
# 建置專案
./gradlew assembleDebug

# Install on connected device/emulator
# 安裝至連接的裝置/模擬器
./gradlew installDebug
```

---

## Development Workflow | 開發流程

### Branch Strategy | 分支策略

**Main Branches | 主要分支**
- `main` - Stable production code | `main` - 穩定生產程式碼
- `feature/optimization` - Main development branch for features | `feature/optimization` - 功能的主要開發分支

**Feature Branches | 功能分支**
```bash
# Create feature branch from feature/optimization
# 從 feature/optimization 創建功能分支
git checkout feature/optimization
git pull upstream feature/optimization
git checkout -b feature/your-feature-name

# Create fix branch
# 創建修復分支
git checkout -b fix/issue-description
```

**Branch Naming Convention | 分支命名慣例**
- `feature/` - New features | 新功能
- `fix/` - Bug fixes | 錯誤修復
- `refactor/` - Code refactoring | 程式碼重構
- `docs/` - Documentation updates | 文件更新
- `test/` - Testing improvements | 測試改進

### Keeping Your Fork Updated | 保持您的複刻更新

```bash
# Fetch upstream changes
# 獲取上游變更
git fetch upstream

# Update your main branch
# 更新您的主分支
git checkout main
git merge upstream/main

# Update feature/optimization
# 更新 feature/optimization
git checkout feature/optimization
git merge upstream/feature/optimization

# Rebase your feature branch
# 重新基於您的功能分支
git checkout feature/your-feature-name
git rebase feature/optimization
```

---

## Coding Standards | 編碼標準

### Java Code Style | Java 程式碼風格

**Formatting | 格式化**
- **Indentation:** 4 spaces (no tabs) | **縮排：** 4 個空格（不使用 Tab）
- **Line length:** Maximum 120 characters | **行長度：** 最多 120 字元
- **Braces:** K&R style (opening brace on same line) | **大括號：** K&R 風格（開括號在同一行）

**Naming Conventions | 命名慣例**
```java
// Classes: PascalCase
// 類別：PascalCase
public class CandidateView { }

// Methods: camelCase
// 方法：camelCase
public void processInput() { }

// Constants: UPPER_SNAKE_CASE
// 常數：UPPER_SNAKE_CASE
public static final String DB_TABLE_CUSTOM = "custom";

// Variables: camelCase
// 變數：camelCase
private String inputText;

// Private fields: prefix with 'm' (legacy) or plain camelCase (modern)
// 私有欄位：前綴 'm'（舊版）或純 camelCase（現代）
private Context mContext;  // Legacy style (existing code)
private Context context;   // Modern style (new code)
```

**Documentation | 文件**
```java
/**
 * Validates table name to prevent SQL injection attacks.
 * 驗證表名以防止 SQL 注入攻擊。
 *
 * Security: This method provides critical SQL injection protection by validating
 * table names against a whitelist before use in SQL queries.
 * 安全性：此方法在 SQL 查詢中使用前，透過白名單驗證表名，提供關鍵的 SQL 注入防護。
 *
 * @param tableName The table name to validate | 要驗證的表名
 * @return true if the table name is valid and safe to use | 如果表名有效且可安全使用則返回 true
 * @throws IllegalArgumentException if the table name is invalid | 如果表名無效則拋出例外
 */
private static boolean isValidTableName(String tableName) {
    // Implementation
}
```

### Kotlin Code Style | Kotlin 程式碼風格

**Follow Official Kotlin Style Guide | 遵循官方 Kotlin 風格指南**
```kotlin
// Class names: PascalCase
// 類別名稱：PascalCase
class CandidateView : View() { }

// Function names: camelCase
// 函式名稱：camelCase
fun processCandidate() { }

// Properties: camelCase
// 屬性：camelCase
private var selectedIndex: Int = 0

// Constants: UPPER_SNAKE_CASE (in companion object)
// 常數：UPPER_SNAKE_CASE（在 companion object 中）
companion object {
    private const val TAG = "CandidateView"
}
```

**Compose UI Guidelines | Compose UI 指南**
```kotlin
// Composable functions: PascalCase
// Composable 函式：PascalCase
@Composable
fun CandidateList(
    candidates: List<String>,
    onCandidateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation
}
```

### Android-Specific Guidelines | Android 特定指南

**1. Security Best Practices | 安全最佳實踐**
```java
// ✅ DO: Use parameterized queries
db.execSQL("DELETE FROM im WHERE code=?", new String[]{imtype});

// ❌ DON'T: Use string concatenation
db.execSQL("DELETE FROM im WHERE code='" + imtype + "'");

// ✅ DO: Validate all input
if (!isValidTableName(tableName)) {
    throw new IllegalArgumentException("Invalid table name");
}

// ✅ DO: Use FLAG_IMMUTABLE for PendingIntent (Android 12+)
PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
```

**2. Resource Management | 資源管理**
```java
// ✅ DO: Close cursors and databases
Cursor cursor = null;
try {
    cursor = db.query(...);
    // Use cursor
} finally {
    if (cursor != null) cursor.close();
}

// ✅ DO: Use try-with-resources (Java 7+)
try (Cursor cursor = db.query(...)) {
    // Use cursor (automatically closed)
}
```

**3. UI Thread Safety | UI 執行緒安全**
```kotlin
// ✅ DO: Use coroutines for async work
viewModelScope.launch {
    val result = withContext(Dispatchers.IO) {
        // Background work
    }
    // Update UI with result
}

// ✅ DO: Use LiveData/StateFlow for UI updates
private val _candidates = MutableStateFlow<List<String>>(emptyList())
val candidates: StateFlow<List<String>> = _candidates.asStateFlow()
```

**4. Material Design 3 | Material Design 3**
```kotlin
// ✅ DO: Use Material3 components
import androidx.compose.material3.*

@Composable
fun MyDialog() {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = { TextButton(...) { } },
        text = { Text("Message") }
    )
}

// ❌ DON'T: Use deprecated ProgressDialog
// Use CircularProgressIndicator in a Dialog instead
```

---

## Commit Guidelines | 提交指南

### Commit Message Format | 提交訊息格式

We follow **Conventional Commits** specification:

我們遵循 **Conventional Commits** 規範：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types | 類型**
- `feat` - New feature | 新功能
- `fix` - Bug fix | 錯誤修復
- `refactor` - Code refactoring | 程式碼重構
- `perf` - Performance improvement | 效能改進
- `docs` - Documentation changes | 文件變更
- `style` - Code style changes (formatting, etc.) | 程式碼風格變更（格式化等）
- `test` - Adding or updating tests | 新增或更新測試
- `chore` - Build process or auxiliary tool changes | 建置流程或輔助工具變更
- `security` - Security improvements | 安全性改進

**Examples | 範例**
```bash
# Feature
feat(keyboard): add swipe gesture support for faster typing

# Bug fix
fix(candidate): resolve crash when selecting empty candidate

# Security improvement
security(database): implement table name validation to prevent SQL injection

Added whitelist-based validation for 30+ database tables with pattern
validation fallback. See SECURITY_ANALYSIS.md for details.

# Documentation
docs(readme): update installation instructions for Android 15

# Refactoring
refactor(ui): migrate ProgressDialog to Material3 CircularProgressIndicator

Replaced deprecated ProgressDialog with Material3 Dialog containing
CircularProgressIndicator across MainActivity and SetupImFragment.

Closes #2
```

**Co-Authorship | 共同作者**
```bash
# Add co-author in footer
# 在頁尾新增共同作者
git commit -m "feat: add new feature

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Pull Request Process | 拉取請求流程

### Before Submitting | 提交前

**1. Self-Review Checklist | 自我審查檢查清單**
- [ ] Code follows project coding standards | 程式碼遵循專案編碼標準
- [ ] All tests pass locally | 所有測試在本地通過
- [ ] No new compiler warnings | 無新的編譯器警告
- [ ] Documentation updated (if applicable) | 文件已更新（如適用）
- [ ] Security considerations addressed | 安全考量已處理
- [ ] CHANGELOG.md updated (for significant changes) | CHANGELOG.md 已更新（重大變更）

**2. Build and Test | 建置和測試**
```bash
# Clean build
# 清理建置
./gradlew clean

# Build debug and release
# 建置除錯和發布版本
./gradlew assembleDebug assembleRelease

# Run unit tests
# 執行單元測試
./gradlew test

# Run instrumented tests (requires device/emulator)
# 執行儀器測試（需要裝置/模擬器）
./gradlew connectedAndroidTest

# Check for lint issues
# 檢查 lint 問題
./gradlew lint
```

**3. Update Your Branch | 更新您的分支**
```bash
# Fetch latest changes
# 獲取最新變更
git fetch upstream

# Rebase on latest feature/optimization
# 重新基於最新的 feature/optimization
git rebase upstream/feature/optimization

# Resolve any conflicts
# 解決任何衝突
```

### Creating Pull Request | 創建拉取請求

**1. Push Your Changes | 推送您的變更**
```bash
git push origin feature/your-feature-name
```

**2. Open Pull Request on GitHub | 在 GitHub 上開啟拉取請求**

Use this template: | 使用此範本：

```markdown
## Summary | 摘要

[Brief description of what this PR does]
[簡短描述此 PR 的作用]

## Changes | 變更

- [List key changes made]
- [列出所做的主要變更]

## Related Issues | 相關問題

Closes #[issue-number]
Relates to #[issue-number]

## Testing | 測試

- [ ] Unit tests added/updated | 新增/更新單元測試
- [ ] Instrumented tests added/updated | 新增/更新儀器測試
- [ ] Manual testing performed | 執行手動測試

**Test Environment | 測試環境**
- Device: [e.g., Pixel 8, Android 15 emulator]
- Android Version: [e.g., Android 15 (API 35)]

## Screenshots | 螢幕截圖

[If UI changes, include before/after screenshots]
[如有 UI 變更，包含前後螢幕截圖]

## Checklist | 檢查清單

- [ ] Code follows project coding standards | 程式碼遵循專案編碼標準
- [ ] Documentation updated | 文件已更新
- [ ] CHANGELOG.md updated | CHANGELOG.md 已更新
- [ ] All tests pass | 所有測試通過
- [ ] No new compiler warnings | 無新編譯器警告
- [ ] Security considerations addressed | 安全考量已處理

## Additional Notes | 其他說明

[Any additional context or notes for reviewers]
[給審查者的任何額外背景或說明]
```

### Review Process | 審查流程

**What to Expect | 預期事項**
1. **Automated Checks** - CI/CD pipeline runs tests | **自動檢查** - CI/CD 管道執行測試
2. **Code Review** - Maintainers review code quality and security | **程式碼審查** - 維護者審查程式碼品質和安全性
3. **Feedback** - You may be asked to make changes | **回饋** - 您可能被要求進行變更
4. **Approval** - Once approved, PR will be merged | **批准** - 一旦批准，PR 將被合併

**Responding to Feedback | 回應回饋**
```bash
# Make requested changes
# 進行請求的變更

# Commit changes
# 提交變更
git add .
git commit -m "refactor: address review feedback"

# Push updates
# 推送更新
git push origin feature/your-feature-name
```

---

## Areas for Contribution | 貢獻領域

### High Priority | 高優先級

**1. Material3 ProgressDialog Replacement | Material3 ProgressDialog 替換**
- **Issue:** [#2](https://github.com/omni624562/nanime-main/issues/2)
- **Effort:** 4-6 hours | 4-6 小時
- **Complexity:** Moderate | 中等
- **Skills:** Kotlin, Jetpack Compose, Material Design 3
- **Files:** MainActivity.java, SetupImFragment.kt, ManageImFragment.java, ManageRelatedFragment.java

**2. AndroidX Preference Migration | AndroidX Preference 遷移**
- **Issue:** [#3](https://github.com/omni624562/nanime-main/issues/3)
- **Effort:** 8-12 hours | 8-12 小時
- **Complexity:** High | 高
- **Skills:** Java, Android Preferences, XML
- **Files:** MultiListPreference.java, LIMEPreferenceManager.java

### Medium Priority | 中優先級

**3. Additional Input Method Support | 其他輸入法支援**
- Cangjie (倉頡) input method | 倉頡輸入法
- Quick (簡易) input method | 簡易輸入法
- Pinyin (拼音) input method | 拼音輸入法

**4. UI/UX Improvements | UI/UX 改進**
- Enhanced emoji picker | 增強表情符號選擇器
- Customizable keyboard themes | 可自訂鍵盤主題
- Adaptive layout for foldable devices | 可摺疊裝置的自適應佈局

**5. Performance Optimization | 效能優化**
- Database query optimization | 資料庫查詢優化
- Candidate generation speed | 候選詞生成速度
- Memory usage reduction | 記憶體使用減少

### Documentation | 文件

**6. Documentation Improvements | 文件改進**
- API documentation (Javadoc/KDoc) | API 文件（Javadoc/KDoc）
- Architecture diagrams | 架構圖
- User guide and tutorials | 使用者指南和教學
- Translation (other languages) | 翻譯（其他語言）

### Testing | 測試

**7. Test Coverage | 測試覆蓋率**
- Unit tests for database layer | 資料庫層的單元測試
- UI tests for keyboard input | 鍵盤輸入的 UI 測試
- Security tests for SQL injection | SQL 注入的安全性測試
- Performance benchmarks | 效能基準測試

---

## Testing | 測試

### Unit Tests | 單元測試

**Location:** `LimeStudio/app/src/test/java/`

**Running Tests | 執行測試**
```bash
# Run all unit tests
# 執行所有單元測試
./gradlew test

# Run specific test class
# 執行特定測試類別
./gradlew test --tests "nan.toload.main.hd.limedb.LimeDBTest"

# Generate coverage report
# 生成覆蓋率報告
./gradlew jacocoTestReport
```

**Example Test | 測試範例**
```java
import org.junit.Test;
import static org.junit.Assert.*;

public class LimeDBTest {
    @Test
    public void testTableNameValidation() {
        assertTrue(LimeDB.isValidTableName("phonetic"));
        assertTrue(LimeDB.isValidTableName("custom"));
        assertFalse(LimeDB.isValidTableName("'; DROP TABLE--"));
        assertFalse(LimeDB.isValidTableName("../../../etc/passwd"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSqlInjectionPrevention() {
        String maliciousInput = "custom'; DELETE FROM im; --";
        LimeDB.validateTableName(maliciousInput);
    }
}
```

### Instrumented Tests | 儀器測試

**Location:** `LimeStudio/app/src/androidTest/java/`

**Running Tests | 執行測試**
```bash
# Connect device or start emulator first
# 先連接裝置或啟動模擬器

# Run all instrumented tests
# 執行所有儀器測試
./gradlew connectedAndroidTest

# Run specific test
# 執行特定測試
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=nan.toload.main.hd.CandidateViewTest
```

### Manual Testing Checklist | 手動測試檢查清單

**Before Submitting PR | 提交 PR 前**
- [ ] Install APK on physical device | 在實體裝置上安裝 APK
- [ ] Test on Android 11 (minimum SDK) | 在 Android 11 上測試（最低 SDK）
- [ ] Test on Android 15 (target SDK) | 在 Android 15 上測試（目標 SDK）
- [ ] Test portrait and landscape orientations | 測試直向和橫向方向
- [ ] Test on phone and tablet screen sizes | 測試手機和平板螢幕尺寸
- [ ] Verify input method switching works | 驗證輸入法切換有效
- [ ] Test candidate selection and input | 測試候選詞選擇和輸入
- [ ] Verify database import/export | 驗證資料庫匯入/匯出
- [ ] Check for memory leaks (use Android Profiler) | 檢查記憶體洩漏（使用 Android Profiler）
- [ ] Test with dynamic color themes (Android 12+) | 使用動態配色主題測試（Android 12+）

---

## Security | 安全性

### Security Considerations | 安全考量

**When Contributing Code | 貢獻程式碼時**

**1. SQL Injection Prevention | SQL 注入防護**
- Always validate table names using `isValidTableName()` | 始終使用 `isValidTableName()` 驗證表名
- Use parameterized queries for WHERE clauses | 對 WHERE 子句使用參數化查詢
- Never concatenate user input directly into SQL | 永不將使用者輸入直接串接至 SQL
- See [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) for guidelines | 請參閱 [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) 獲取指南

**2. Input Validation | 輸入驗證**
- Validate all user input | 驗證所有使用者輸入
- Sanitize file paths for imports | 清理匯入的檔案路徑
- Check file extensions and MIME types | 檢查檔案副檔名和 MIME 類型

**3. Data Privacy | 資料隱私**
- Never collect user typing data without consent | 未經同意永不蒐集使用者輸入資料
- Keep all data local (no telemetry) | 保持所有資料本地（無遙測）
- Use encryption for sensitive data | 對敏感資料使用加密

**4. Android Permissions | Android 權限**
- Request minimum necessary permissions | 請求最少必要權限
- Provide clear permission rationale | 提供清晰的權限理由
- Handle permission denials gracefully | 優雅處理權限拒絕

### Reporting Security Vulnerabilities | 報告安全漏洞

**DO NOT** open public issues for security vulnerabilities.

**請勿**為安全漏洞開設公開問題。

**Instead | 替代方式**
- Email: nanchan.tw@gmail.com
- Subject: "[SECURITY] Vulnerability in WFIME" | 主旨："[SECURITY] WFIME 中的漏洞"
- Include:
  - Description of vulnerability | 漏洞描述
  - Steps to reproduce | 重現步驟
  - Potential impact | 潛在影響
  - Suggested fix (if available) | 建議修復（如有）

---

## Questions and Support | 問題與支援

### Getting Help | 獲取協助

**GitHub Discussions | GitHub 討論**
- General questions | 一般問題
- Feature requests | 功能請求
- Development help | 開發協助

**GitHub Issues | GitHub 問題**
- Bug reports | 錯誤報告
- Specific technical problems | 特定技術問題
- Enhancement proposals | 增強提案

**Email Contact | 電子郵件聯絡**
- nanchan.tw@gmail.com
- For private matters or security issues | 私人事務或安全問題

### Resources | 資源

**Project Documentation | 專案文件**
- [README.md](README.md) - Project overview | 專案概述
- [CHANGELOG.md](CHANGELOG.md) - Version history | 版本歷史
- [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) - Security audit | 安全稽核

**Android Development | Android 開發**
- [Android Developer Guide](https://developer.android.com/guide)
- [Material Design 3](https://m3.material.io/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [AndroidX Libraries](https://developer.android.com/jetpack/androidx)

**Kotlin Resources | Kotlin 資源**
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

---

## Recognition | 致謝

### Contributors | 貢獻者

All contributors will be recognized in our README and release notes.

所有貢獻者將在我們的 README 和發布說明中獲得認可。

### How We Recognize Contributions | 我們如何認可貢獻

- **Code Contributions** - Listed in commit history and CHANGELOG | **程式碼貢獻** - 列於提交歷史和 CHANGELOG
- **Documentation** - Credited in document headers | **文件** - 在文件標頭中記載
- **Bug Reports** - Mentioned in issue/PR resolution | **錯誤報告** - 在問題/PR 解決中提及
- **Reviews** - Acknowledged in merged PRs | **審查** - 在合併的 PR 中致謝

---

## Attribution | 歸屬

This project is based on the **LIME IME open-source project**:
- Project URL: http://github.com/lime-ime/limeime/
- Website: http://android.toload.net/

本專案基於 **LIME IME 開源專案**：
- 專案網址：http://github.com/lime-ime/limeime/
- 網站：http://android.toload.net/

We are grateful to the original LIME IME team for their foundational work.

我們感謝原始 LIME IME 團隊的基礎工作。

---

## License | 授權

By contributing to WFIME, you agree that your contributions will be licensed under the **GNU General Public License v3.0**.

貢獻至 WFIME 即表示您同意您的貢獻將在 **GNU General Public License v3.0** 下授權。

See [LICENSE](LICENSE) for full license text.

請參閱 [LICENSE](LICENSE) 獲取完整授權文字。

---

<div align="center">

**Thank you for contributing to WFIME!**
**感謝您對 WFIME 的貢獻！**

Made with ❤️ by the WFIME Community

**[⬆ Back to Top](#contributing-to-wfime--貢獻至-wfime)**

</div>

---

**Document Version:** 1.0
**Last Updated:** 2026-01-09
**Maintained By:** WFIME Team
