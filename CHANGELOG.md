# Changelog | 變更日誌

WFIME (Wheat Fields Input Method Editor) 所有重要版本變更記錄於此。

格式遵循 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)，版本號遵循[語義化版本](https://semver.org/spec/v2.0.0.html)。

---

## [Unreleased]

### Fixed | 修復
- **數字鍵盤模式辨識** — 新增 `MODE_NUMBER = 7` 常數，取代不穩定的 `isNumeric` 旗標判斷，修正英文模式下切換符號鍵盤時被誤判為數字鍵盤的問題
- **符號鍵盤誤觸發** — 修正 `toggleSymbols()` 在 `MODE_TEXT` 下錯誤顯示 `numeric.xml` 而非 `lime_number_symbol.xml` 的行為
- **英文預測干擾** — 在符號模式與電話模式下停用英文預測，避免產生「Pacific」等無關建議詞
- **大易輸入原始碼顯示** — 修正 Dayi 輸入時非字母數字組合（如 `./`）不必要地出現於候選列的問題

### Security | 安全性
- **移除 `requestLegacyExternalStorage`** — 此屬性在 Android 11（API 30）後已無效，從 `AndroidManifest.xml` 中刪除
- **停用 `allowBackup`** — 改為 `false`，防止用戶輸入資料（字典、設定）透過 `adb backup` 被外部存取
- **簽署憑證外部化** — `build.gradle` 的 release signing 密碼改從 `keystore.properties` 讀取（已加入 `.gitignore`），支援 CI 環境變數（`KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`）作為回退，不再硬編碼於版本控制

---

## [1.2.0] - 2026-05-21

重大程式碼清理與重構，建立穩定可發布基線。

### Added | 新增
- **數字鍵盤佈局** (`numeric.xml`) — 支援 `TYPE_CLASS_NUMBER` 輸入欄位，提供獨立的 3×4 純數字鍵盤
- **鍵盤導航圖示** — 新增方向鍵等 drawable 資源
- **發布簽署設定** — build.gradle 加入 release signing configuration

### Changed | 變更
- **Java 21 相容性** — 將 `sourceCompatibility` / `targetCompatibility` 升級至 Java 21
- **鍵盤識別碼重構** — 統一鍵盤名稱對應，更新導航選單刷新邏輯
- **EmojiPicker 佈局優化** — 改善 Emoji 選擇器的顯示與初始化流程

### Removed | 移除
- **廢棄版面配置** — 移除 `fragment_dialog_related_*.xml`、`kbsetting.xml`、`kbsetting2.xml`、`related.xml`、`word.xml` 等不再使用的 XML
- **舊符號鍵盤檔** — 刪除 `symbols1.xml`、`symbols2.xml`、`symbols3.xml`、`phone_simple.xml`、`popup_domains.xml`、`templime.xml`，改用統一的 `lime_number_symbol.xml`
- **SetupImRestoreRunnable** — 移除已廢棄的輸入法還原執行緒

### Fixed | 修復
- **輸入法處理更新** — 修復多項輸入模式切換相關問題
- **Emoji 資料清理** — 修正初始化流程並清理舊 Emoji 資料

---

## [1.1.0] - 2026-04-22

建置系統現代化、效能優化與 Android 16 鍵盤穩定性修復。

### Fixed | 修復
- **Gradle 9.x 相容性** — 修正 Groovy DSL 屬性賦值語法
- **AGP 9.0 相容性** — 修正 Android Gradle Plugin 9.0 Breaking Changes
- **Android 16 鍵盤卡頓** — 修復 onStartInput 錯誤父方法呼叫及 Handler Looper 宣告問題
- **CandidateView 黑屏** — 限制 CandidateView 高度並重構 IME 佈局邏輯
- **Material3 鍵盤** — 修復 ResourcesNotFoundException、更新箭頭圖示為現代向量、優化按鍵間距與圓角

### Performance | 效能
- **資料庫架構優化** — 啟用 WAL 模式、修復記憶體洩漏、設定 cache 上限
- **N+1 查詢消除** — 新增 `code` 欄位索引，修正 volatile 競態條件
- **LRU 快取** — 實作 LRU 快取淘汰策略，快取主執行緒 Handler，移除重複清除邏輯

### Changed | 變更
- **Product Flavors** — 正式區分 `phone` / `tablet` 兩個建置變體，支援動態資料庫路徑
- **大易三碼邏輯** — 精煉 Dayi 3-code 候選詞邏輯，已在手機實機驗證
- **CI/CD** — 新增 GitHub Actions APK 自動建置 workflow

---

## [1.0.0] - 2026-01-09

安全強化與 Android 16 現代化基礎建設。

### Added | 新增
- **SQL 注入防護** — 白名單式表名驗證（30+ 資料庫表），加入 `isValidTableName()` / `validateTableName()` 安全方法
- **安全文件** — 新增 `docs/SECURITY_ANALYSIS.md`，完整記錄漏洞分析與緩解策略
- **雙語 README** — 英文與繁體中文（台灣）完整專案說明文件

### Fixed | 修復
- **返回導航** — 移除 `System.exit(0)`，改用 `OnBackPressedDispatcher` 符合 Android 生命週期規範
- **AndroidX Preference 遷移** — 完整從 `android.preference.*` 遷移至 AndroidX Preference

### Deprecated | 標記技術債
- `ProgressDialog` — 自 API 26 起已棄用，標記待替換為 Material3 `CircularProgressIndicator`

---

## 基礎版本 — LIME IME Fork

從 [LIME IME](http://github.com/lime-ime/limeime/) 開源專案分支，針對個人使用習慣優化：

- 保留大易（Dayi）與注音（Phonetic）輸入法
- 升級 Target SDK 至 36（Android 16）
- 導入 Material Design 3 動態配色（Material You）
- 整合 Jetpack Compose（設定頁、Emoji 選擇器）
- 邊到邊顯示（Edge-to-Edge）支援
- SQLite 本地詞庫，支援 `.lime` / `.limedb` / `.cin` 格式匯入匯出

---

## 版本管理說明

| 欄位 | 說明 |
|------|------|
| `versionName` | `VERSION_MAJOR.VERSION_MINOR.VERSION_PATCH`（定義於 `LimeStudio/version.properties`）|
| `versionCode` | Gradle 建置時自動產生，格式為 `yyyyMMdd`（每日自動遞增，無需手動維護）|

發版流程：編輯 `LimeStudio/version.properties` 中的版本號 → commit → build。

---

**維護者：** WFIME Team / nanchan.tw@gmail.com  
**原始專案：** [LIME IME](http://github.com/lime-ime/limeime/)