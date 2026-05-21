# WFIME - Wheat Fields Input Method Editor | 麥田輸入法編輯器

<div align="center">

**A Modern Android IME Application | 現代化 Android 輸入法應用程式**

[![Android](https://img.shields.io/badge/Android-11+-green.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-blue.svg)](https://developer.android.com/about/versions/16)
[![License](https://img.shields.io/badge/License-GPL%20v3-orange.svg)](LICENSE)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-purple.svg)](https://m3.material.io/)

[English](#english) | [繁體中文](#繁體中文)

</div>

---

## English

### Overview

WFIME (Wheat Fields Input Method Editor) is a sophisticated Android IME application derived from the LIME IME open-source project. Optimized for personal use with focus on **Dayi (大易)** and **Phonetic (注音)** input methods, featuring modern Material Design 3 UI and Android 16 compliance.

### ✨ Key Features

- **🎨 Material Design 3** - Dynamic color theming with Material You support
- **📱 Android 16 Ready** - Full compliance with latest Android standards (Target SDK 36)
- **🛠️ Modern Architecture**
  - **AndroidX Preferences** - Fully migrated from legacy APIs
  - **RecyclerView** - Optimized list performance
  - **ConstraintLayout** - Flattened, efficient UI hierarchy
- **⌨️ Multiple Input Methods**
  - Phonetic (Bopomofo/注音) - Taiwan's phonetic system
  - Dayi (大易) - Symbol-based Chinese input
  - Additional variants and enhancements
- **🔒 Security Hardened** - SQL injection protection and secure coding practices
- **🎯 Predictive Back Gesture** - Modern Android 13+ back navigation support
- **🌓 Edge-to-Edge Display** - Full immersive experience with proper insets handling
- **📊 Smart Candidate System** - Intelligent word suggestions with user scoring
- **😀 Enhanced Emoji Picker** - Unicode 15.0 support, infinite scroll, and customizable scaling
- **🎭 Jetpack Compose UI** - Consolidated "one-stop" settings dashboard and emoji picker for elegant, drawer-free control
- **⚡ Typing Experience Optimization** - Intelligent radical-filtering prevents intermediate radicals (e.g. "魚鳥言") from polluting host application editors, 100% avoiding search/suggestion interference
- **🔧 Highly Customizable** - Extensive settings and keyboard layouts

### 📋 Requirements

- **Minimum SDK:** Android 11 (API 30)
- **Target SDK:** Android 16 (API 36)
- **Architecture:** All (pure Java/Kotlin)
- **Storage:** ~6 MB

### 🚀 Installation

#### From APK
1. Download the latest `app-release.apk`
2. Enable "Install from Unknown Sources" in Settings
3. Install the APK
4. Enable WFIME in Settings → System → Languages & Input → On-screen keyboard

#### From Source
```bash
git clone https://github.com/omni624562/nanime-main.git
cd nanime-main/LimeStudio
./gradlew assembleRelease
```

### 📁 Project Structure

This repository is organized as a **monorepo** containing the Android project and related resources:

```
nanime-main/                    # Git repository root
├── LimeStudio/                 # ⭐ Android project (open this in Android Studio)
│   ├── app/                    # Application source code
│   │   └── src/main/java/      # Java/Kotlin source files
│   ├── build.gradle            # Project build configuration
│   ├── settings.gradle         # Gradle settings
│   └── gradlew                 # Gradle wrapper
│
├── Database/                   # Dictionary database files
├── Resources/                  # Static resources
│
├── README.md                   # Project documentation
├── CHANGELOG.md                # Version history
├── CONTRIBUTING.md             # Contribution guidelines
└── SECURITY_ANALYSIS.md        # Security audit
```

**📌 Important for Developers:**
- **For Android development:** Open `LimeStudio/` directory in Android Studio
- **For database/resource management:** Work in the root `nanime-main/` directory
- **For Git operations:** Execute commands from root `nanime-main/` directory

### 🛠️ Technology Stack

**Languages & Frameworks:**
- Java (Primary) - Core IME logic
- Kotlin - Modern UI components
- Jetpack Compose - Declarative UI
- Material Design 3 - UI/UX framework

**Architecture & Libraries:**
- SQLite - Local database
- AndroidX - Modern Android components
- Gradle 9.4.1 - Build system
- Android Gradle Plugin 9.2.1 - Build tool

**Security:**
- ProGuard - Code obfuscation
- Network security configuration
- SQL injection protection
- Secure data handling

### 📊 Android 16 Compliance

**Compliance Score:** 100% ✅

| Category | Status | Score |
|----------|--------|-------|
| Target SDK 36 | ✅ | 100% |
| Material Design 3 | ✅ | 100% |
| Foreground Services | ✅ | 100% |
| Edge-to-Edge UI | ✅ | 100% |
| Predictive Back | ✅ | 100% |
| Security | ✅ | 100% |
| Deprecated APIs | ✅ | 100% |

See [docs/SECURITY_ANALYSIS.md](docs/SECURITY_ANALYSIS.md) for detailed security audit.

### 📚 Documentation

- **[CHANGELOG.md](CHANGELOG.md)** - Version history and changes
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines
- **[docs/SECURITY_ANALYSIS.md](docs/SECURITY_ANALYSIS.md)** - Security audit report
- **[docs/README.md](docs/README.md)** - **Documentation Index** (Technical guides & reports)

### 🤝 Contributing

We welcome contributions! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting PRs.

**Areas for Contribution:**
- Material3 ProgressDialog replacement
- AndroidX Preference migration
- Additional input method support
- UI/UX improvements
- Documentation translations

See [open issues](https://github.com/omni624562/nanime-main/issues) for current needs.

### 📄 License

WFIME is licensed under **GNU General Public License v3.0**.

This project is based on the LIME IME project, with optimizations and adjustments for personal use habits, retaining only Dayi and Phonetic input methods.

### 🔒 Privacy & Security

**Data Collection:** This application does NOT collect or share user personal information.

**Security Features:**
- ✅ SQL injection protection
- ✅ Network security hardening
- ✅ Secure data storage
- ✅ No internet tracking

See [docs/SECURITY_ANALYSIS.md](docs/SECURITY_ANALYSIS.md) for comprehensive security documentation.

### 👥 Core Development Team

**Original LIME IME Team**
- Project URL: http://github.com/lime-ime/limeime/
- Website: http://android.toload.net/

**WFIME Maintainer**
- nanchan.tw@gmail.com
- Developed with Google Antigravity collaboration

### 🙏 Acknowledgments

- LIME IME Open Source Project Team
- Android Open Source Project
- Material Design Team
- All contributors

### 📞 Support

- **Issues:** [GitHub Issues](https://github.com/omni624562/nanime-main/issues)
- **Email:** nanchan.tw@gmail.com

---

## 繁體中文

### 概述

WFIME（麥田輸入法編輯器）是一個基於 LIME IME 開源專案衍生的精緻 Android 輸入法應用程式。專為個人使用最佳化，專注於**大易輸入法**和**注音輸入法**，具備現代化 Material Design 3 介面和 Android 16 合規性。

### ✨ 主要特色

- **🎨 Material Design 3** - 動態配色主題，支援 Material You
- **📱 Android 16 就緒** - 完全符合最新 Android 標準 (Target SDK 36)
- **🛠️ 現代化架構**
  - **AndroidX Preferences** - 完整遷移舊版 API
  - **RecyclerView** - 最佳化的列表效能
  - **ConstraintLayout** - 扁平且高效的 UI 階層
- **⌨️ 多種輸入法**
  - 注音輸入法 - 台灣注音系統
  - 大易輸入法 - 符號型中文輸入
  - 其他變體和增強版本
- **🔒 安全強化** - SQL 注入防護和安全編碼實踐
- **🎯 預測性返回手勢** - 支援現代 Android 13+ 返回導航
- **🌓 邊到邊顯示** - 完整沉浸式體驗，正確處理插邊
- **📊 智慧候選系統** - 智慧詞彙建議，使用者評分
- **😀 強大表情符號選單** - 支援 Unicode 15.0、無限捲動與自訂縮放
- **🎭 Jetpack Compose UI** - 整合「一站式」設定首頁卡片與表情符號選單，極致流暢且無抽屜選單負擔
- **⚡ 組字體驗最佳化** - 智慧字根不上字過濾（魚鳥言不上字），在拼打完成前不干擾 Host 應用程式的搜尋與自動建議，點選確認才送出漢字
- **🔧 高度客製化** - 豐富的設定和鍵盤佈局

### 📋 系統需求

- **最低 SDK：** Android 11 (API 30)
- **目標 SDK：** Android 16 (API 36)
- **架構：** 所有架構（純 Java/Kotlin）
- **儲存空間：** ~6 MB

### 🚀 安裝方式

#### 從 APK 安裝
1. 下載最新的 `app-release.apk`
2. 在設定中啟用「允許安裝未知來源的應用程式」
3. 安裝 APK
4. 在設定 → 系統 → 語言與輸入法 → 螢幕鍵盤中啟用 WFIME

#### 從原始碼建置
```bash
git clone https://github.com/omni624562/nanime-main.git
cd nanime-main/LimeStudio
./gradlew assembleRelease
```

### 📁 專案結構

本儲存庫採用 **monorepo** 組織結構，包含 Android 專案和相關資源：

```
nanime-main/                    # Git 儲存庫根目錄
├── LimeStudio/                 # ⭐ Android 專案（在 Android Studio 中開啟此目錄）
│   ├── app/                    # 應用程式原始碼
│   │   └── src/main/java/      # Java/Kotlin 原始檔案
│   ├── build.gradle            # 專案建置配置
│   ├── settings.gradle         # Gradle 設定
│   └── gradlew                 # Gradle wrapper
│
├── Database/                   # 詞庫資料庫檔案
├── Resources/                  # 靜態資源
│
├── README.md                   # 專案文件
├── CHANGELOG.md                # 版本歷史
├── CONTRIBUTING.md             # 貢獻指南
└── SECURITY_ANALYSIS.md        # 安全稽核
```

**📌 開發者注意事項：**
- **Android 開發：** 在 Android Studio 中開啟 `LimeStudio/` 目錄
- **資料庫/資源管理：** 在根目錄 `nanime-main/` 中作業
- **Git 操作：** 從根目錄 `nanime-main/` 執行指令

### 🛠️ 技術架構

**語言與框架：**
- Java（主要）- 核心輸入法邏輯
- Kotlin - 現代 UI 元件
- Jetpack Compose - 宣告式 UI
- Material Design 3 - UI/UX 框架

**架構與函式庫：**
- SQLite - 本地資料庫
- AndroidX - 現代 Android 元件
- Gradle 9.4.1 - 建置系統
- Android Gradle Plugin 9.2.1 - 建置工具

**安全性：**
- ProGuard - 程式碼混淆
- 網路安全配置
- SQL 注入防護
- 安全資料處理

### 📊 Android 16 合規性

**合規性分數：** 100% ✅

| 類別                | 狀態  | 分數   |
| ----------------- | --- | ---- |
| 目標 SDK 36         | ✅   | 100% |
| Material Design 3 | ✅   | 100% |
| 前景服務              | ✅   | 100% |
| 邊到邊 UI            | ✅   | 100% |
| 預測性返回             | ✅   | 100% |
| 安全性               | ✅   | 100% |
| 已棄用 API           | ✅   | 100% |

詳細的安全稽核請參閱 [docs/SECURITY_ANALYSIS.md](docs/SECURITY_ANALYSIS.md)。

### 📚 文件

- **[CHANGELOG.md](CHANGELOG.md)** - 版本歷史與變更
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - 貢獻指南
- **[docs/SECURITY_ANALYSIS.md](docs/SECURITY_ANALYSIS.md)** - 安全稽核報告
- **[docs/README.md](docs/README.md)** - **文件目錄索引** (技術指南與報告)

### 🤝 貢獻

我們歡迎貢獻！請在提交 PR 前閱讀我們的[貢獻指南](CONTRIBUTING.md)。

**貢獻領域：**
- Material3 ProgressDialog 替換
- AndroidX Preference 遷移
- 其他輸入法支援
- UI/UX 改進
- 文件翻譯

請查看[開放問題](https://github.com/omni624562/nanime-main/issues)了解當前需求。

### 📄 授權

WFIME 採用 **GNU General Public License v3.0** 授權。

本專案基於 LIME IME 專案，經過最佳化與調整以符合個人使用習慣，僅保留大易和注音輸入法。

### 🔒 隱私與安全

**資料蒐集：** 本應用程式**不會**蒐集或分享使用者的個人資訊。

**安全功能：**
- ✅ SQL 注入防護
- ✅ 網路安全強化
- ✅ 安全資料儲存
- ✅ 無網路追蹤

完整的安全文件請參閱 [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md)。

### 👥 核心開發團隊

**原始 LIME IME 團隊**
- 專案網址：http://github.com/lime-ime/limeime/
- 網站：http://android.toload.net/

**WFIME 維護者**
- nanchan.tw@gmail.com
- 透過 Google Antigravity 進行協同開發

### 🙏 致謝

- LIME IME 開源專案團隊
- Android 開源專案
- Material Design 團隊
- 所有貢獻者

### 📞 支援

- **問題回報：** [GitHub Issues](https://github.com/omni624562/nanime-main/issues)
- **電子郵件：** nanchan.tw@gmail.com

---

<div align="center">

Made with ❤️ by the WFIME Team

**[⬆ Back to Top](#wfime---wheat-fields-input-method-editor--麥田輸入法編輯器)**

</div>
