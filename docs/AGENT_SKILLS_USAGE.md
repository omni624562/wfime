# Agent Skills Usage Guide | Agent Skill 使用指南

This document explains the specialized capabilities (Skills) available to the AI Agent in this workspace and how to use them.
本文件說明 AI Agent 在此工作區中具備的專業能力 (Skill) 及其使用方式。

## Installed Skills | 已安裝的技能

The following skills are installed in `.agent/skills/`:
以下技能已安裝於 `.agent/skills/`：

1.  **Gradle Commander** (Gradle 指揮官)
2.  **ADB & Logcat Expert** (ADB 與 Logcat 專家)
3.  **UI Screenshot** (UI 截圖工具)
4.  **Material Design 3 Specialist** (Material Design 3 專家)
5.  **Android Manifest Guardian** (Android Manifest 守門員)
6.  **Localization & String Manager** (多語系與字串管理員)
7.  **Code Style & Lint Enforcer** (程式碼風格與 Lint 執行者)
8.  **Git Version Control** (Git 版本控制)
9.  **Project Documentation Architect** (專案文件架構師)
10. **Codebase Navigator & Analyst** (程式碼庫導航與分析師)

---

## Skill Details & Triggers | 技能詳情與觸發指令

### 1. Gradle Commander
**Purpose**: Manages build tasks, testing, and dependency analysis.
**用途**：管理建置任務、測試與相依性分析。

**How to use (如何使用)**:
- "Build the debug APK" (建立 debug APK)
- "Clean the project" (清理專案)
- "Run unit tests" (執行單元測試)
- "Check app dependencies" (檢查 App 相依性)

### 2. ADB & Logcat Expert
**Purpose**: Handles device interaction and crash log analysis.
**用途**：處理裝置互動與當機日誌分析。

**How to use (如何使用)**:
- "Install the app to my device" (安裝 App 到我的裝置)
- "Why did the app crash? Check logs" (為什麼 App 閃退？檢查 log)
- "Filter logs for tag 'LIME'" (過濾標籤為 'LIME' 的 log)

### 3. UI Screenshot
**Purpose**: Captures and retrieves device screenshots for verification.
**用途**：擷取並取回裝置截圖以進行驗證。

**How to use (如何使用)**:
- "Take a screenshot" (幫我截圖)
- "Capture the current screen and save it as home_screen.png" (擷取目前螢幕並存為 home_screen.png)

### 4. Material Design 3 Specialist
**Purpose**: Guides implementation of modern UI components and theming.
**用途**：引導實作現代化 UI 元件與主題。

**How to use (如何使用)**:
- "Update this button to Material 3 style" (將此按鈕更新為 Material 3 風格)
- "Apply dynamic colors to the layout" (將動態色彩應用到此佈局)
- "How do I implement a filled card in M3?" (我該如何在 M3 實作填滿卡片？)

### 5. Android Manifest Guardian
**Purpose**: Ensures safe edits to `AndroidManifest.xml` (permissions, services).
**用途**：確保安全編輯 `AndroidManifest.xml`（權限、服務）。

**How to use (如何使用)**:
- "Add the VIBRATE permission" (新增震動權限)
- "Register the new InputMethodService" (註冊新的輸入法服務)
- "Check if my manifest permissions are correct" (檢查我的 Manifest 權限是否正確)

### 6. Localization & String Manager
**Purpose**: Manages bilingual strings (En/Zh-TW) and prevents hardcoding.
**用途**：管理雙語字串（英/繁中）並防止寫死文字。

**How to use (如何使用)**:
- "Extract this string to resources" (將此字串提取到資源檔)
- "Add a new error message for 'Network Error'" (新增一個「網路錯誤」的錯誤訊息)
- "Ensure all strings have Chinese translations" (確保所有字串都有中文翻譯)

### 7. Code Style & Lint Enforcer
**Purpose**: Maintains code quality via Android Lint.
**用途**：透過 Android Lint 維持程式碼品質。

**How to use (如何使用)**:
- "Run lint and fix issues" (執行 lint 並修復問題)
- "Check for unused resources" (檢查未使用的資源)
- "Are there any deprecated API usages?" (是否有使用到已棄用的 API？)

### 8. Git Version Control
**Purpose**: Manages source code versioning and collaboration.
**用途**：管理原始碼版本控制與協作。

**How to use (如何使用)**:
- "Commit these changes with message 'fix: typo'" (提交這些變更，訊息為 'fix: typo')
- "Create a new branch called feature/dark-mode" (建立一個名為 feature/dark-mode 的新分支)
- "Check git status" (檢查 git 狀態)

### 9. Project Documentation Architect
**Purpose**: Maintains README and project documentation quality.
**用途**：維護 README 與專案文件品質。

**How to use (如何使用)**:
- "Update the README with the new features" (將新功能更新到 README)
- "Create a bilingual structure for the documentation" (為文件建立雙語架構)
- "Add build instructions to the README" (將建置說明加到 README)

### 10. Codebase Navigator & Analyst
**Purpose**: Analyzes project health, stats, and structure.
**用途**：分析專案體質、統計數據與結構。

**How to use (如何使用)**:
- "Analyze the dependencies of the app" (分析 App 的相依性)
- "Count the lines of code in the project" (計算專案的程式碼行數)
- "What architecture pattern is this project using?" (此專案使用哪種架構模式？)
