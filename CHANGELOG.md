# Changelog | 變更日誌

WFIME (Wheat Fields Input Method Editor) 所有重要版本變更記錄於此。

格式遵循 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)，版本號遵循[語義化版本](https://semver.org/spec/v2.0.0.html)。

---

## [Unreleased]

## [1.5.0] - 2026-10-19

大易候選改為與電腦版一致（完全相符＋三碼）、新版螢幕鍵盤、使用者資料獨立保存與備份、英文介面、淺色模式，以及大量穩定性與資料安全修正。

### Added | 新增
- **大易三碼** — 4 碼字可用第 1、2 碼加最後一碼找到（只列 Big5 字），連打切分與連打模式也支援三碼
- **英文介面** — 依系統或 app 語言自動切換；注音英文名稱為 Chuyin
- **淺色模式** — 候選列與面板的淺色配色
- **導入關聯字庫** — 匯入時保留已學習的關聯字；字根檔改用系統檔案選擇器、背景匯入
- **新版螢幕鍵盤** — 改用 Jetpack Compose 繪製：白色字母鍵、淡綠功能鍵與圓角，支援長按小鍵盤、滑動手勢與 TalkBack 逐鍵朗讀
- **我的資料** — 學習資料（字碼分數、關聯字、備忘錄）另存 `user.db`，匯入字庫後自動套回；可匯出／匯入（單一 zip，匯入時合併），並可自動備份到 Google 帳號
- **大易同碼字參考前後字** — 智慧選字沒有記錄時，依關聯字表中前一個字常接的字排第一（內建字典不受「啟用關聯字典」顯示開關影響）
- **語音輸入按鈕** — 候選列工具列
- **字根反查** — 改為設定開關，結果顯示在候選列左側
- **Emoji 18.0** — 表情符號與關鍵字更新（關鍵字取自 CLDR 49 beta3）

### Changed | 變更
- **大易候選只列碼完全相符的字** — 不再夾帶碼比打的還短的字或延伸碼；沒有命中時改由連打切分處理
- **非大易輸入法組字中按空白** — 上第一個候選（原本送出空白而組字留著）
- **學字與匯入的 SQL 全面參數化** — 含引號的詞可正常學習與匯入，並刪除可傳任意 SQL 的方法
- **大易連打** — 連續 3 碼以上的純字母或純數字不自動上字（可直接打英文單字與長數字，數字排第一）；刪光組字時中斷連打，不再學到接錯的詞
- **注音候選** — 同音字多的碼列出全部候選；Big5 以外的字（含簡體字）排到同一個碼的最後
- 表情符號與備忘錄面板配色跟上新鍵盤；「快捷鍵速查」補齊實體鍵盤標點
- 輸入法清單標題改為「切換輸入法」、通知標題改為「麥田輸入法」，不再用舊名 LIME
- 移除滑動選字與未使用的佈景主題、刪除多處死碼
- 移除舊鍵盤、舊機型實體鍵盤設定、注音鍵盤類型、無作用的「重設」選項、簡繁轉換與從不顯示的獨立候選列

### Fixed | 修復
- 快速刪除或快打時候選列殘留舊字、空白鍵與選字鍵上到前一碼的字
- 智慧選字把其他碼的字排到前面
- 英文預測點到過期候選 crash、空白後沒有預測、選字後游標位置、游標移動後未重設
- 分享文字開啟、長按刪除關聯字、新增備忘錄時的 crash；建立 LimeDB 會關掉共用連線導致查詢 crash
- 字根顯示偶爾拼到別次查詢的結果；背景預取改到實體鍵盤狀態；資料庫鎖定時快取「查無此碼」
- 電話鍵盤自動上字在查詢較慢時被略過
- 注音連打選第一個音節的單字時，後面的音節被吃掉
- 按過實體鍵盤後換欄位，螢幕鍵盤收起並重建；實體鍵盤使用中鍵盤收起時切換深淺色，再點欄位會當機
- 出現英文預測時 Delete 要先收起清單才刪字
- 還原學習資料沒有記錄時處理中視窗不關；輸入法下載按鈕的筆數；設定畫面表情符號、打字震動的預設值與實際不一致
- 英文介面的表情搜尋提示被切掉
- 隱藏的獨立候選列每按一鍵都被量測（打字 CPU 用量約少 15～20%）
- **數字鍵盤模式辨識** — 新增 `MODE_NUMBER = 7` 常數，取代不穩定的 `isNumeric` 旗標判斷，修正英文模式下切換符號鍵盤時被誤判為數字鍵盤的問題
- **符號鍵盤誤觸發** — 修正 `toggleSymbols()` 在 `MODE_TEXT` 下錯誤顯示 `numeric.xml` 而非 `lime_number_symbol.xml` 的行為
- **英文預測干擾** — 在符號模式與電話模式下停用英文預測，避免產生「Pacific」等無關建議詞
- **大易輸入原始碼顯示** — 修正 Dayi 輸入時非字母數字組合（如 `./`）不必要地出現於候選列的問題

### Security | 安全性
- **移除 `requestLegacyExternalStorage`** — 此屬性在 Android 11（API 30）後已無效，從 `AndroidManifest.xml` 中刪除
- **自動備份只含使用者資料** — `allowBackup` 搭配 `backup_rules`／`data_extraction_rules`，只備份 `user.db` 與智慧選字紀錄（字庫與設定不備份）；雲端備份只在能端對端加密時進行，設定頁可關閉
- **LimeDB 查詢全面綁定參數** — 主查詢與其餘串進 SQL 的值改用綁定參數，表名一律經 `validateTableName`
- **移除用不到的權限與 FileProvider**
- **簽署憑證外部化** — `build.gradle` 的 release signing 密碼改從 `keystore.properties` 讀取（已加入 `.gitignore`），支援 CI 環境變數（`KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`）作為回退，不再硬編碼於版本控制

---

## [1.4.0] - 2026-05-24

- 新增大易／注音輸入法啟用開關

---

## [1.3.0] - 2026-05-23

- 平板版支援

---

## [1.2.0] - 2026-05-21

重大程式碼清理、首頁重構、輸入體驗最佳化與新機相容性提升，建立穩定可發布基線。

### Added | 新增
- **一站式麥田金首頁** — 於 `SettingsScreen.kt` 建立「下載輸入法」麥田金卡片，整合注音與大易對照表下載，並與 `SetupImLoadDialog` 橋接，支援非同步安全載入與狀態反應式即時重刷，取代舊有側邊抽屜選單與多餘齒輪設定。
- **組字字根不上字最佳化** — 重構 `LIMEService.java`，在拼打漢字時不將中間字根（如大易的 `魚鳥言` 或注音拼音）作為 Composing Text 送至 Host App 編輯框，徹底杜絕目標應用程式的多餘搜尋聯想與干擾，並保留浮動組字懸浮視窗與 CandidateView 字根提示。
- **新設備資源保留配置** (`keep.xml`) — 新增 `app/src/main/res/raw/keep.xml`，強制保留所有 XML 佈局檔（`tools:keep="@xml/*"`），防止 ProGuard/Gradle 資源收縮器 (`shrinkResources`) 將動態反射解析的鍵盤佈局 XML 誤剪，完美解決在 Pixel 7 等新設備上安裝 Release 版本時崩潰的 Bug。
- **數字鍵盤佈局** (`numeric.xml`) — 支援 `TYPE_CLASS_NUMBER` 輸入欄位，提供獨立的 3×4 純數字鍵盤。
- **鍵盤導航圖示** — 新增方向鍵等 drawable 資源。
- **發布簽署設定** — `build.gradle` 加入 release signing configuration。
- **獨立版本號管理** (`version.properties`) — 於 `LimeStudio/version.properties` 解耦版號變數，由 Gradle 動態解析以避免動態時間導致編譯快取失效。

### Changed | 變更
- **Java 21 相容性** — 將 `sourceCompatibility` / `targetCompatibility` 升級至 Java 21。
- **鍵盤識別碼與防錯 Fallback** — 統一鍵盤名稱對應，並於 `LIMEKeyboardSwitcher.java` 引入雙重安全 Fallback 容錯切換，即使遇到資料庫加載異常，也能自動安全回退至注音或大易默認佈局，保障鍵盤切換的 100% 穩定度。
- **EmojiPicker 佈局最佳化** — 改善 Emoji 選擇器的顯示與初始化流程。
- **主畫面 CoordinatorLayout 化** — 將 `activity_main.xml` 的 root 佈局重構為 CoordinatorLayout，完全移除 drawer 容器，實現 0 負擔極速啟動。

### Removed | 移除
- **死碼與廢棄檔案大掃除** — 徹底掃描並刪除 **28 個 Java/Kotlin 原始碼檔案**（包含 `ManageImFragment.java`、`AboutFragment.java`、側邊抽屜模組 `NavigationDrawerScreen.kt` 等相關 UI、Dialog 與非同步備份任務檔案）與 **18 個 XML layout 佈局檔案**，精煉專案架構。
- **廢棄版面配置** — 移除 `fragment_dialog_related_*.xml`、`kbsetting.xml`、`kbsetting2.xml`、`related.xml`、`word.xml` 等不再使用的 XML。
- **舊符號鍵盤檔** — 刪除 `symbols1.xml`、`symbols2.xml` 、`symbols3.xml`、`phone_simple.xml`、`popup_domains.xml`、`templime.xml`，改用統一的 `lime_number_symbol.xml`。
- **SetupImRestoreRunnable** — 移除已廢棄的輸入法還原執行緒。

### Fixed | 修復
- **輸入法處理更新** — 修復多項輸入模式切換與動態 package flavor name 資源解析問題。
- **Emoji 資料清理** — 修正初始化流程並清理舊 Emoji 資料。

---

## [1.1.0] - 2026-04-22

建置系統現代化、效能最佳化與 Android 16 鍵盤穩定性修復。

### Fixed | 修復
- **Gradle 9.x 相容性** — 修正 Groovy DSL 屬性賦值語法
- **AGP 9.0 相容性** — 修正 Android Gradle Plugin 9.0 Breaking Changes
- **Android 16 鍵盤卡頓** — 修復 onStartInput 錯誤父方法呼叫及 Handler Looper 宣告問題
- **CandidateView 黑屏** — 限制 CandidateView 高度並重構 IME 佈局邏輯
- **Material3 鍵盤** — 修復 ResourcesNotFoundException、更新箭頭圖示為現代向量、最佳化按鍵間距與圓角

### Performance | 效能
- **資料庫架構最佳化** — 啟用 WAL 模式、修復記憶體洩漏、設定 cache 上限
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

從 [LIME IME](http://github.com/lime-ime/limeime/) 開源專案分支，針對個人使用習慣最佳化：

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