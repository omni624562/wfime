# 麥田輸入法 (LIME HD)

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/omni624562/wfime)
[![Platform](https://img.shields.io/badge/platform-Android-blue)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/minSdk-26-orange)](https://developer.android.com/about/versions/oreo)

> 支援大易、注音等多種輸入法的 Android 中文輸入法，針對手機與平板（含實體鍵盤）最佳化。

---

## 功能特色

- 🈶 **多種輸入法**：大易、注音（可同時啟用並快速循環切換）
- ⌨️ **實體鍵盤完整支援**：快捷鍵、候選字選取、輸入法切換
- 📱 **手機 / 平板雙版本**：平板版候選字列固定於螢幕頂部，相容 Samsung DeX 模式
- 🔤 **邊框無縫整合**：候選字區自動迴避系統導覽列（支援手勢導覽 & 按鍵導覽）

---

## 實體鍵盤快捷鍵

| 快捷鍵 | 功能 |
|--------|------|
| `Ctrl + Space` | 切換中文 / 英文模式 |
| `Ctrl + Shift` | 循環切換已啟用的內部輸入法（大易 → 注音 → …） |
| `Menu + Shift` | 開啟系統 IME 選擇器（切換到其他鍵盤 app） |
| `Shift`（單按） | 切換中文 / 英文（需開啟設定「快速切換輸入模式2」） |
| `Shift + Space` | 切換中文 / 英文（需開啟設定「快速切換輸入模式1」） |
| `Ctrl + 1`～`0` | 快速選取第 1～10 個候選字 |
| `Ctrl + /` | 全形符號候選字 |
| `Ctrl + \`` | 循環切換已啟用的內部輸入法（備用） |

> **大易 / 注音互切**：請在麥田設定「輸入法」頁面同時啟用兩種輸入法，再使用 `Ctrl + Shift` 循環切換。

---

## 建置說明

### 需求
- Android Studio Hedgehog 以上
- JDK 17+
- Android SDK 35（compileSdk）
- 實體裝置或模擬器（minSdk 26）

### Build Variants

| Flavor | Application ID | 說明 |
|--------|---------------|------|
| `phone` | `net.toload.main.hd.phone` | 手機版（含平板使用） |
| `tablet` | `net.toload.main.hd.tablet` | 平板專用版 |

### 常用指令

```bash
# 部署手機版（Debug）到已連接裝置
.\gradlew installPhoneDebug

# 部署平板版（Debug）到已連接裝置
.\gradlew installTabletDebug

# 執行單元測試
.\gradlew test

# 執行平板版儀器化整合測試
.\gradlew connectedTabletDebugAndroidTest
```

---

## 分支結構

| 分支 | 說明 |
|------|------|
| `main` | 主線，穩定版本 |
| `feature/tablet-support` | 平板功能開發分支 |
| `release/phone-v1.2` | 手機版 v1.2 發布分支 |

---

## 主要模組說明

| 檔案 | 說明 |
|------|------|
| [`LIMEService.java`](app/src/main/java/net/toload/main/hd/LIMEService.java) | InputMethodService 主體，處理所有輸入事件 |
| [`PhysicalKeyHandler.java`](app/src/main/java/net/toload/main/hd/PhysicalKeyHandler.java) | 實體鍵盤按鍵事件處理（`onKeyDown` / `onKeyUp`） |
| [`IMSwitchHelper.java`](app/src/main/java/net/toload/main/hd/IMSwitchHelper.java) | 輸入法循環切換邏輯（大易↔注音） |
| [`LIMEKeyboardSwitcher.java`](app/src/main/java/net/toload/main/hd/LIMEKeyboardSwitcher.java) | 鍵盤 View 切換管理 |
| [`LIMEPreferenceManager.java`](app/src/main/java/net/toload/main/hd/LIMEPreferenceManager.java) | 設定項目讀寫封裝 |

---

## 版本紀錄

### v1.4（效能與穩定度最佳化）
- **輸入法核心瘦身**：徹底移除其他 5 種無用輸入法（行列、倉頡、輕鬆、五筆、拼音等）的邏輯與常數分支，縮短資料庫查字判斷路徑，大幅優化鍵盤啟動與打字流暢度。
- **大易輕量級智慧選字（主動式防漏追蹤）**：引進 Bigram 關聯詞庫提權排序。特別針對手機版非同步環境，開發了「主動式字元攔截」機制，在文字送出的瞬間（`commitTyped`）立即更新上下文，確保智慧選字在所有手機/平板型號、任何 App 的文字框上都百分之百穩定生效。
- **修復 Shift 鍵卡頓與誤判**：解決軟鍵盤 Shift 在 `onPress` 與 `onKey` 雙重事件中重複觸發 `handleShift()` 的長年 Bug，消除 Shift 切換時的頓挫感，且單擊不再誤判為大寫鎖定（Caps Lock）。
- **停用長按空白鍵選單**：應使用者回饋，停用長按空白鍵彈出 LIME-HD 控制選單的行為，以防日常打字中頻繁誤觸。
- **適配與排版修復**：修正鍵盤 `horizontalGap` 解析造成的按鍵 `a` 寬度腰斬 Bug，重新配置寬度與邊界 Flags，保證按鍵滿血飽滿、無觸控死角。

### v1.3（平板版）
- 新增實體鍵盤 `Ctrl + Shift` 循環切換大易/注音
- 修復候選字列與 Samsung 導覽列鍵盤圖示重疊問題（`WindowInsets` bottom padding）
- 修復 `showIMPicker()` 在 IME Service context 中因 AppCompat 主題缺失導致的 crash
- 平板版候選字列固定於頂部（`TYPE_APPLICATION_OVERLAY`）

### v1.2（手機版）
- 移除 `!mEnglishOnly` 對實體鍵盤快捷鍵的限制，確保英文模式下仍可切換輸入法
- 整合測試（Instrumented Tests）覆蓋實體鍵盤核心快捷鍵行為

---

## 授權

本專案為私有專案，未經授權不得散佈。
