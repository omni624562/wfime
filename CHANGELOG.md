# Changelog | 變更日誌

All notable changes to the WFIME (Wheat Fields Input Method Editor) project will be documented in this file.

本文件記錄 WFIME（麥田輸入法編輯器）專案的所有重要變更。

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

格式基於 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)，本專案遵循[語義化版本](https://semver.org/spec/v2.0.0.html)。

---

## [Unreleased] | [未發布]

### Added | 新增
- **SQL Injection Protection** - Comprehensive table name validation system to prevent SQL injection attacks | **SQL 注入防護** - 完整的表名驗證系統以防止 SQL 注入攻擊
  - Whitelist-based validation for 30+ database tables | 基於白名單的驗證，涵蓋 30+ 個資料庫表
  - Pattern validation fallback with security logging | 帶安全日誌的模式驗證備援機制
  - `isValidTableName()` and `validateTableName()` security methods | `isValidTableName()` 和 `validateTableName()` 安全方法
  - See [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) for details | 詳見 [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md)
- **Security Documentation** - Comprehensive security audit and vulnerability analysis | **安全文件** - 全面的安全稽核和漏洞分析
  - SQL injection vulnerability documentation | SQL 注入漏洞文件
  - Attack scenario examples | 攻擊場景範例
  - Mitigation recommendations | 緩解建議
  - OWASP Mobile compliance notes | OWASP Mobile 合規性說明
- **Comprehensive README** - Bilingual project documentation with professional presentation | **完整 README** - 雙語專案文件，具備專業呈現
  - English and Traditional Chinese (Taiwan) versions | 英文和繁體中文（台灣）版本
  - Feature highlights with Android 16 compliance table | 功能亮點與 Android 16 合規性表格
  - Technology stack breakdown | 技術架構分解
  - Installation and contribution guidelines | 安裝與貢獻指南

### Changed | 變更
- **Android 16 Compliance Improvements** - Modern back navigation handling | **Android 16 合規性改進** - 現代化返回導航處理
  - Migrated from deprecated `onKeyDown()` to `OnBackPressedDispatcher` | 從已棄用的 `onKeyDown()` 遷移至 `OnBackPressedDispatcher`
  - Enables predictive back gesture support (Android 13+) | 啟用預測性返回手勢支援（Android 13+）
  - Proper activity lifecycle management | 正確的活動生命週期管理

### Removed | 移除
- **System.exit() Call** - Removed harmful process termination on back press | **System.exit() 呼叫** - 移除返回鍵按下時的有害進程終止
  - Violated Android lifecycle best practices | 違反 Android 生命週期最佳實踐
  - Prevented proper activity cleanup | 阻止正確的活動清理

### Deprecated | 已棄用
- **ProgressDialog Usage** - Marked as technical debt for future replacement | **ProgressDialog 使用** - 標記為技術債務以供未來替換
  - Deprecated since Android API 26 | 自 Android API 26 起已棄用
  - Tracked in [Issue #2](https://github.com/omni624562/nanime-main/issues/2) | 追蹤於 [Issue #2](https://github.com/omni624562/nanime-main/issues/2)
  - Added `@SuppressWarnings("deprecation")` markers | 新增 `@SuppressWarnings("deprecation")` 標記
- **android.preference.* Package** - Legacy preference APIs marked for migration | **android.preference.* 套件** - 舊版偏好設定 API 標記待遷移
  - Deprecated since Android API 29 | 自 Android API 29 起已棄用
  - Tracked in [Issue #3](https://github.com/omni624562/nanime-main/issues/3) | 追蹤於 [Issue #3](https://github.com/omni624562/nanime-main/issues/3)

### Fixed | 修復
- **Back Navigation** - Fixed improper back button handling in MainActivity | **返回導航** - 修復 MainActivity 中不當的返回按鈕處理
  - Removed System.exit(0) that violated Android lifecycle | 移除違反 Android 生命週期的 System.exit(0)
  - Implemented proper OnBackPressedCallback | 實作正確的 OnBackPressedCallback
- **Security Vulnerabilities** - Addressed SQL injection risks in database layer | **安全漏洞** - 處理資料庫層中的 SQL 注入風險
  - Added table name validation to prevent injection attacks | 新增表名驗證以防止注入攻擊
  - Sanitized file paths for database imports | 清理資料庫匯入的檔案路徑

### Security | 安全性
- **SQL Injection Protection** - Implemented comprehensive input validation | **SQL 注入防護** - 實作全面的輸入驗證
  - Risk level reduced from MEDIUM to LOW | 風險等級從中等降至低
  - Whitelist validation for all table operations | 所有表操作的白名單驗證
  - Security audit documentation added | 新增安全稽核文件

---

## [1.0.0] - Previous LIME IME Fork | [1.0.0] - 先前的 LIME IME 分支

### Project Foundation | 專案基礎

**Base Version** - Fork from LIME IME open-source project | **基礎版本** - 從 LIME IME 開源專案分支

### Added | 新增
- **Android 16 (API 36) Target SDK** - Full compliance with latest Android standards | **Android 16 (API 36) 目標 SDK** - 完全符合最新 Android 標準
- **Material Design 3** - Dynamic color theming with Material You support | **Material Design 3** - 動態配色主題，支援 Material You
  - `DynamicColors.applyToActivitiesIfAvailable()` implementation | `DynamicColors.applyToActivitiesIfAvailable()` 實作
  - Theme.Material3.DynamicColors theme | Theme.Material3.DynamicColors 主題
- **Edge-to-Edge Display** - Immersive UI with proper window insets | **邊到邊顯示** - 沉浸式 UI，正確處理視窗插邊
  - `WindowCompat.setDecorFitsSystemWindows(false)` | `WindowCompat.setDecorFitsSystemWindows(false)`
  - Transparent status and navigation bars | 透明狀態列和導航列
- **Jetpack Compose UI** - Modern declarative UI components | **Jetpack Compose UI** - 現代宣告式 UI 元件
  - CandidateView.kt - Compose-based candidate selection | CandidateView.kt - 基於 Compose 的候選詞選擇
  - EmojiPicker.kt - Full Compose emoji picker | EmojiPicker.kt - 完整 Compose 表情符號選擇器
- **Chinese Input Methods** - Optimized for Taiwanese users | **中文輸入法** - 針對台灣使用者優化
  - **Phonetic (Bopomofo/注音)** - Taiwan's phonetic input system | **注音輸入法** - 台灣注音系統
  - **Dayi (大易)** - Symbol-based Chinese input | **大易輸入法** - 符號型中文輸入
  - Multiple variants and enhancements | 多種變體和增強版本
- **Database System** - SQLite-based word storage with custom wrapper | **資料庫系統** - 基於 SQLite 的詞彙儲存與自訂包裝器
  - User dictionary with frequency learning | 使用者字典與頻率學習
  - Related word suggestions | 相關詞建議
  - Import/export support (.txt, .lime, .limedb, .cin) | 匯入/匯出支援（.txt、.lime、.limedb、.cin）
- **Keyboard Features** - Advanced input capabilities | **鍵盤功能** - 進階輸入能力
  - Multi-touch and gesture support | 多點觸控和手勢支援
  - Hardware keyboard compatibility | 硬體鍵盤相容性
  - Vibration and audio feedback | 震動和音訊回饋
  - Swipe gestures | 滑動手勢
- **Foreground Service** - Android 14+ compliance | **前景服務** - Android 14+ 合規性
  - `FOREGROUND_SERVICE_SPECIAL_USE` permission | `FOREGROUND_SERVICE_SPECIAL_USE` 權限
  - `android:foregroundServiceType="specialUse"` declaration | `android:foregroundServiceType="specialUse"` 宣告
  - Required justification property | 必要的說明屬性
- **Network Security** - Hardened security configuration | **網路安全** - 強化的安全配置
  - Cleartext traffic disabled (`cleartextTrafficPermitted="false"`) | 禁用明文流量（`cleartextTrafficPermitted="false"`）
  - System certificates only | 僅系統憑證
- **ProGuard Obfuscation** - Production build security | **ProGuard 混淆** - 生產建置安全性
  - Code obfuscation for release builds | 發布建置的程式碼混淆
  - Optimized APK size | 優化的 APK 大小

### Architecture | 架構
- **Core Components** | **核心元件**
  - LIMEService.java (163KB) - Core InputMethodService | LIMEService.java（163KB）- 核心 InputMethodService
  - LimeDB.java (210KB) - Database access layer | LimeDB.java（210KB）- 資料庫存取層
  - SearchServer.java (77KB) - Search and candidate logic | SearchServer.java（77KB）- 搜尋和候選詞邏輯
  - LIMEBaseKeyboard.java (56KB) - Keyboard implementation | LIMEBaseKeyboard.java（56KB）- 鍵盤實作
  - LIMEKeyboardBaseView.java (72KB) - Custom keyboard rendering | LIMEKeyboardBaseView.java（72KB）- 自訂鍵盤渲染

### Requirements | 系統需求
- **Minimum SDK:** Android 11 (API 30) | **最低 SDK：** Android 11（API 30）
- **Target SDK:** Android 16 (API 36) | **目標 SDK：** Android 16（API 36）
- **Architecture:** ARM64, ARMv7 | **架構：** ARM64、ARMv7
- **Storage:** ~50MB | **儲存空間：** ~50MB

### Privacy & Security | 隱私與安全
- **No Data Collection** - Zero telemetry or user tracking | **無資料蒐集** - 零遙測或使用者追蹤
- **Local Database** - All data stored locally on device | **本地資料庫** - 所有資料儲存於裝置本地
- **No Internet Tracking** - Network only for optional database downloads | **無網路追蹤** - 網路僅用於選用的資料庫下載
- **Secure Data Storage** - SQLite encryption support | **安全資料儲存** - SQLite 加密支援

---

## Version History Summary | 版本歷史摘要

### [Unreleased] - 2026-01-09
**Focus:** Android 16 compliance improvements and security hardening
**重點：** Android 16 合規性改進和安全強化

**Key Changes:**
- ✅ Back navigation modernization | 返回導航現代化
- ✅ SQL injection protection | SQL 注入防護
- ✅ Comprehensive documentation | 完整文件
- ⏳ ProgressDialog replacement (tracked in Issue #2) | ProgressDialog 替換（追蹤於 Issue #2）
- ⏳ AndroidX Preference migration (tracked in Issue #3) | AndroidX Preference 遷移（追蹤於 Issue #3）

**Pull Request:** [#1 - Android 16 Compliance & Security Improvements](https://github.com/omni624562/nanime-main/pulls)

### [1.0.0] - LIME IME Fork
**Focus:** Initial fork from LIME IME with optimizations for personal use
**重點：** 從 LIME IME 初始分支，針對個人使用優化

**Key Features:**
- Modern Material Design 3 UI | 現代化 Material Design 3 UI
- Android 16 target SDK | Android 16 目標 SDK
- Phonetic and Dayi input methods | 注音和大易輸入法
- Jetpack Compose integration | Jetpack Compose 整合

---

## Attribution | 致謝

This project is based on the **LIME IME open-source project** with optimizations and adjustments for personal use habits, retaining only Dayi and Phonetic input methods.

本專案基於 **LIME IME 開源專案**，經過優化和調整以符合個人使用習慣，僅保留大易和注音輸入法。

**Original LIME IME Team:**
- Project URL: http://github.com/lime-ime/limeime/
- Website: http://android.toload.net/

**WFIME Maintainer:**
- nanchan.tw@gmail.com
- Developed with Google Antigravity collaboration

---

## Links | 連結

- [GitHub Repository](https://github.com/omni624562/nanime-main)
- [Issue Tracker](https://github.com/omni624562/nanime-main/issues)
- [Pull Requests](https://github.com/omni624562/nanime-main/pulls)
- [Security Analysis](SECURITY_ANALYSIS.md)
- [Contributing Guidelines](CONTRIBUTING.md)
- [README](README.md)

---

**Document Version:** 1.0
**Last Updated:** 2026-01-09
**Maintained By:** WFIME Team

<div align="center">

**[⬆ Back to Top](#changelog--變更日誌)**

Made with ❤️ by the WFIME Team

</div>
