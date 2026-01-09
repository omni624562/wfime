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
- **📱 Android 16 Ready** - Full compliance with latest Android standards
- **⌨️ Multiple Input Methods**
  - Phonetic (Bopomofo/注音) - Taiwan's phonetic system
  - Dayi (大易) - Symbol-based Chinese input
  - Additional variants and enhancements
- **🔒 Security Hardened** - SQL injection protection and secure coding practices
- **🎯 Predictive Back Gesture** - Modern Android 13+ back navigation support
- **🌓 Edge-to-Edge Display** - Full immersive experience with proper insets handling
- **📊 Smart Candidate System** - Intelligent word suggestions with user scoring
- **🎭 Jetpack Compose UI** - Modern declarative UI for better performance
- **🔧 Highly Customizable** - Extensive settings and keyboard layouts

### 📋 Requirements

- **Minimum SDK:** Android 11 (API 30)
- **Target SDK:** Android 16 (API 36)
- **Architecture:** ARM64, ARMv7
- **Storage:** ~50MB

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

### 🛠️ Technology Stack

**Languages & Frameworks:**
- Java (Primary) - Core IME logic
- Kotlin - Modern UI components
- Jetpack Compose - Declarative UI
- Material Design 3 - UI/UX framework

**Architecture & Libraries:**
- SQLite - Local database
- AndroidX - Modern Android components
- Gradle 8.13.2 - Build system

**Security:**
- ProGuard - Code obfuscation
- Network security configuration
- SQL injection protection
- Secure data handling

### 📊 Android 16 Compliance

**Compliance Score:** 95% ✅

| Category | Status | Score |
|----------|--------|-------|
| Target SDK 36 | ✅ | 100% |
| Material Design 3 | ✅ | 95% |
| Foreground Services | ✅ | 100% |
| Edge-to-Edge UI | ✅ | 100% |
| Predictive Back | ✅ | 100% |
| Security | ✅ | 95% |

See [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) for detailed security audit.

### 📚 Documentation

- **[CHANGELOG.md](CHANGELOG.md)** - Version history and changes
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines
- **[SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md)** - Security audit report
- **[docs/](docs/)** - Additional documentation

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

See [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) for comprehensive security documentation.

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

WFIME（麥田輸入法編輯器）是一個基於 LIME IME 開源專案衍生的精緻 Android 輸入法應用程式。專為個人使用優化，專注於**大易輸入法**和**注音輸入法**，具備現代化 Material Design 3 介面和 Android 16 合規性。

### ✨ 主要特色

- **🎨 Material Design 3** - 動態配色主題，支援 Material You
- **📱 Android 16 就緒** - 完全符合最新 Android 標準
- **⌨️ 多種輸入法**
  - 注音輸入法 - 台灣注音系統
  - 大易輸入法 - 符號型中文輸入
  - 其他變體和增強版本
- **🔒 安全強化** - SQL 注入防護和安全編碼實踐
- **🎯 預測性返回手勢** - 支援現代 Android 13+ 返回導航
- **🌓 邊到邊顯示** - 完整沉浸式體驗，正確處理插邊
- **📊 智慧候選系統** - 智慧詞彙建議，使用者評分
- **🎭 Jetpack Compose UI** - 現代宣告式 UI，更佳效能
- **🔧 高度客製化** - 豐富的設定和鍵盤佈局

### 📋 系統需求

- **最低 SDK：** Android 11 (API 30)
- **目標 SDK：** Android 16 (API 36)
- **架構：** ARM64, ARMv7
- **儲存空間：** ~50MB

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

### 🛠️ 技術架構

**語言與框架：**
- Java（主要）- 核心輸入法邏輯
- Kotlin - 現代 UI 元件
- Jetpack Compose - 宣告式 UI
- Material Design 3 - UI/UX 框架

**架構與函式庫：**
- SQLite - 本地資料庫
- AndroidX - 現代 Android 元件
- Gradle 8.13.2 - 建置系統

**安全性：**
- ProGuard - 程式碼混淆
- 網路安全配置
- SQL 注入防護
- 安全資料處理

### 📊 Android 16 合規性

**合規性分數：** 95% ✅

| 類別 | 狀態 | 分數 |
|------|------|------|
| 目標 SDK 36 | ✅ | 100% |
| Material Design 3 | ✅ | 95% |
| 前景服務 | ✅ | 100% |
| 邊到邊 UI | ✅ | 100% |
| 預測性返回 | ✅ | 100% |
| 安全性 | ✅ | 95% |

詳細的安全稽核請參閱 [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md)。

### 📚 文件

- **[CHANGELOG.md](CHANGELOG.md)** - 版本歷史與變更
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - 貢獻指南
- **[SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md)** - 安全稽核報告
- **[docs/](docs/)** - 其他文件

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

本專案基於 LIME IME 專案，經過優化和調整以符合個人使用習慣，僅保留大易和注音輸入法。

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
