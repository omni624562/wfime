# Device Testing Guide - Phase 1-3
# 裝置測試指南 - Phase 1-3

**Date:** 2026-01-20
**Test Build:** feature/material3-completion branch
**APK Location:** `LimeStudio/app/build/outputs/apk/debug/app-debug.apk`
**Material3 Compliance:** 98%

---

## Test Overview | 測試概覽

本測試指南涵蓋 Phase 1-3 的所有功能：
- ✅ Phase 1: Settings Screen Compose 整合
- ✅ Phase 2: XML 元件稽核（已確認無需變更）
- ✅ Phase 3: Material3 鍵盤顏色系統

**測試目標：**
1. 驗證 Settings Screen Compose 整合正常運作
2. 確認 Material3 鍵盤顏色在亮/暗模式下正確顯示
3. 測試 Android 12+ 的 Material You 動態主題
4. 驗證自訂主題（粉紅、科技藍等）未受影響

---

## Prerequisites | 測試前準備

### 1. APK 位置確認

**APK 檔案：**
```
C:\Users\nan\CodeSpace\nanime-main\LimeStudio\app\build\outputs\apk\debug\app-debug.apk
```

**檔案大小：** ~63 MB（預期）

---

### 2. 測試裝置需求

**最低需求：**
- **裝置 1:** Android 11 (API 30) - 測試基本功能
- **裝置 2:** Android 14 (API 34) - 測試 Material You

**推薦配置：**
| 裝置類型 | Android 版本 | 測試重點 |
|----------|--------------|----------|
| 裝置/模擬器 1 | Android 11 | 基本功能、相容性 |
| 裝置/模擬器 2 | Android 12+ | Material You 動態主題 |
| 裝置/模擬器 3 | Android 14 | 最新系統測試 |

---

### 3. APK 安裝步驟

**方法 1: ADB 安裝（推薦）**
```bash
# 確認裝置連線
adb devices

# 安裝 APK
adb install -r C:\Users\nan\CodeSpace\nanime-main\LimeStudio\app\build\outputs\apk\debug\app-debug.apk

# 確認安裝成功
adb shell pm list packages | grep nan.toload
```

**方法 2: 手動安裝**
1. 將 APK 傳輸到裝置
2. 在裝置上開啟「檔案管理器」
3. 點擊 APK 檔案安裝
4. 允許「未知來源」安裝（如需要）

---

### 4. 啟用 WFIME 輸入法

**步驟：**
1. 開啟「設定」→「系統」→「語言與輸入法」
2. 點擊「虛擬鍵盤」
3. 點擊「管理鍵盤」
4. 啟用「LIME IME HC」
5. 在任何文字輸入欄位長按，切換至 WFIME

---

## Test Section 1: Settings Screen Integration | 設定畫面整合測試

**Phase 1 功能測試**

### Test 1.1: Settings Screen 啟動測試

**步驟：**
1. 開啟 WFIME 主畫面
2. 點擊右上角「⋮」選單
3. 選擇「Settings」或「設定」

**預期結果：**
- [ ] Settings 畫面順利開啟
- [ ] 使用 Compose UI（流暢、現代化外觀）
- [ ] 無閃退或錯誤
- [ ] Toolbar 顯示「Settings」標題
- [ ] 返回按鈕正常顯示

**截圖位置：**
```
screenshots/1.1-settings-screen-launch.png
```

---

### Test 1.2: Settings 項目顯示測試

**步驟：**
1. 在 Settings 畫面中捲動
2. 檢查所有設定項目

**預期結果：**
- [ ] 所有 34 個設定項目正常顯示
- [ ] Preference 分組正確（鍵盤、外觀、進階等）
- [ ] Switch、Checkbox、List 等元件正常顯示
- [ ] Material3 樣式（圓角、顏色、字型）

**關鍵設定項目檢查：**
- [ ] 「注音鍵盤類型」(phonetic_keyboard_type)
- [ ] 「鍵盤主題」(keyboard_theme)
- [ ] 「深色模式」(dark_mode)
- [ ] 「選字鍵顯示」(show_candidate_keys)

---

### Test 1.3: Phonetic Keyboard Type 切換測試

**步驟：**
1. 在 Settings 中找到「注音鍵盤類型」
2. 點擊開啟選擇對話框
3. 選擇不同的鍵盤類型：
   - Standard（標準）
   - Eten（倚天）
   - Eten26（倚天26鍵）
   - HSU（許氏）
4. 確認選擇後返回

**預期結果：**
- [ ] 對話框正常開啟（Material3 樣式）
- [ ] 所有選項正常顯示
- [ ] 選擇後對話框關閉
- [ ] 設定值正確儲存
- [ ] **重要：** `setupPreferenceChangeListener()` 正確觸發
- [ ] 資料庫更新成功（`DBSrv.setIMKeyboard()` 被呼叫）

**驗證方法：**
```bash
# 查看 Logcat 確認無錯誤
adb logcat | grep -i "LIMEPreference"
```

---

### Test 1.4: Settings 儲存與恢復測試

**步驟：**
1. 在 Settings 中修改多個設定項目
2. 返回主畫面
3. 重新進入 Settings

**預期結果：**
- [ ] 所有變更都已儲存
- [ ] 重新開啟 Settings 時，設定值正確顯示
- [ ] SharedPreferences 正常運作
- [ ] BackupManager 觸發（`dataChanged()` 被呼叫）

---

### Test 1.5: Settings 畫面旋轉測試

**步驟：**
1. 開啟 Settings 畫面
2. 旋轉裝置（直向 ↔ 橫向）
3. 觀察畫面重新繪製

**預期結果：**
- [ ] 畫面順利旋轉
- [ ] 設定項目狀態保持（不會重置）
- [ ] 無閃退或重新載入問題
- [ ] Compose State 正確保存

---

## Test Section 2: Material3 Keyboard Colors | Material3 鍵盤顏色測試

**Phase 3 功能測試**

### Test 2.1: Light Theme 鍵盤顏色測試

**前置條件：** 裝置設定為亮色模式

**步驟：**
1. 確認系統為亮色模式（Settings → Display → Light theme）
2. 在 WFIME Settings 中選擇「Light」鍵盤主題
3. 開啟任何文字輸入欄位（如 Google Keep、訊息等）
4. 啟動 WFIME 鍵盤

**預期結果：**
- [ ] 鍵盤背景使用 Material3 surface 顏色（淺灰色 #FBFDF8）
- [ ] 按鍵背景使用 surfaceVariant（淺灰色 #DBE5DD）
- [ ] 文字顏色使用 onSurface（深灰色 #191C1A）
- [ ] 功能鍵（Shift、Del 等）有明確視覺區隔
- [ ] 按下按鍵時，顏色變化為 primaryContainer（淺綠色 #89F8C7）

**截圖位置：**
```
screenshots/2.1-light-theme-keyboard.png
screenshots/2.1-light-theme-key-pressed.png
```

**顏色驗證：**
| 元素 | 預期顏色 | 實際顏色 | ✓/✗ |
|------|----------|----------|-----|
| 鍵盤背景 | #FBFDF8 (surface) | | |
| 一般按鍵 | #DBE5DD (surfaceVariant) | | |
| 按鍵文字 | #191C1A (onSurface) | | |
| 按下狀態 | #89F8C7 (primaryContainer) | | |

---

### Test 2.2: Dark Theme 鍵盤顏色測試

**前置條件：** 裝置設定為暗色模式

**步驟：**
1. 確認系統為暗色模式（Settings → Display → Dark theme）
2. 在 WFIME Settings 中選擇「Dark」鍵盤主題
3. 開啟任何文字輸入欄位
4. 啟動 WFIME 鍵盤

**預期結果：**
- [ ] 鍵盤背景使用 Material3 dark surface（深灰色 #191C1A）
- [ ] 按鍵背景使用 dark surfaceVariant（中灰色 #404943）
- [ ] 文字顏色使用 dark onSurface（淺灰色 #E1E3DE）
- [ ] 功能鍵有適當對比度
- [ ] 按下按鍵時，顏色變化為 dark primaryContainer（深綠色 #005138）

**截圖位置：**
```
screenshots/2.2-dark-theme-keyboard.png
screenshots/2.2-dark-theme-key-pressed.png
```

**顏色驗證：**
| 元素 | 預期顏色 | 實際顏色 | ✓/✗ |
|------|----------|----------|-----|
| 鍵盤背景 | #191C1A (dark surface) | | |
| 一般按鍵 | #404943 (dark surfaceVariant) | | |
| 按鍵文字 | #E1E3DE (dark onSurface) | | |
| 按下狀態 | #005138 (dark primaryContainer) | | |

---

### Test 2.3: 亮/暗模式自動切換測試

**步驟：**
1. 確保 WFIME 主題設定為「跟隨系統」（如有此選項）
2. 開啟鍵盤（亮色模式）
3. 切換系統主題至暗色模式（下拉通知欄 → Dark theme）
4. 觀察鍵盤顏色變化

**預期結果：**
- [ ] 鍵盤顏色立即或短暫延遲後切換至暗色
- [ ] 所有 Material3 color tokens 自動對應至 dark 版本
- [ ] 無需重啟鍵盤或 app
- [ ] 顏色過渡流暢

**注意：** 若需重啟鍵盤才能看到變化，這是正常的（部分 IME 實作限制）

---

### Test 2.4: Material You 動態主題測試 (Android 12+)

**前置條件：** Android 12 或更新版本

**步驟：**
1. 在系統設定中更換桌布（Settings → Wallpaper & style）
2. 選擇一個顏色鮮明的桌布（例如：紅色、藍色、紫色）
3. 確認系統主題顏色已改變（通知欄、設定等會跟著變色）
4. 開啟 WFIME 鍵盤

**預期結果：**
- [ ] **鍵盤顏色跟隨系統動態主題**
- [ ] Primary color 反映桌布主色調
- [ ] Surface 和 background 顏色適當調整
- [ ] 整體視覺與系統 UI 和諧一致

**Material You 測試案例：**

| 桌布主色 | 預期鍵盤 Primary 色調 | 實際顯示 | ✓/✗ |
|----------|----------------------|----------|-----|
| 紅色桌布 | 紅色系 primary | | |
| 藍色桌布 | 藍色系 primary | | |
| 綠色桌布 | 綠色系 primary | | |
| 預設桌布 | WFIME 預設綠色 (#006C4C) | | |

**截圖位置：**
```
screenshots/2.4-material-you-red.png
screenshots/2.4-material-you-blue.png
screenshots/2.4-material-you-green.png
```

**注意：** Material You 需要 Android 12+ 且支援動態顏色的裝置

---

### Test 2.5: Composing Text Popup 測試

**步驟：**
1. 開啟鍵盤
2. 輸入注音符號（例如：ㄋㄧˇ ㄏㄠˇ）
3. 觀察 composing text popup（輸入預覽彈出框）

**預期結果：**
- [ ] Composing popup 正常顯示
- [ ] 背景顏色為半透明 Material3 色（60% alpha）
- [ ] **Light theme:** 背景 #99DBE5DD（surfaceVariant @ 60%）
- [ ] **Dark theme:** 背景 #99191C1A（surface @ 60%）
- [ ] 文字清晰可讀
- [ ] 位置正確（在輸入鍵上方）

**截圖位置：**
```
screenshots/2.5-composing-popup-light.png
screenshots/2.5-composing-popup-dark.png
```

---

### Test 2.6: Candidate View 測試

**步驟：**
1. 輸入完整注音（如：ㄋㄧˇ ㄏㄠˇ）
2. 觀察候選字視窗

**預期結果：**
- [ ] 候選字正常顯示
- [ ] 分隔線使用 Material3 outline 顏色
- [ ] 選字鍵使用 primary 顏色（80% alpha）
- [ ] **Light theme:** 選字鍵 #CC006C4C
- [ ] **Dark theme:** 選字鍵 #CC6CDBAC
- [ ] 文字對比度良好

---

## Test Section 3: Custom Themes | 自訂主題測試

**驗證 Phase 3 未影響自訂主題**

### Test 3.1: Pink Theme 測試

**步驟：**
1. 在 Settings 中選擇「Pink」鍵盤主題
2. 開啟鍵盤

**預期結果：**
- [ ] 鍵盤顯示粉紅色主題
- [ ] 顏色與 Phase 3 前一致（未被 Material3 影響）
- [ ] 鍵盤背景：#FFFAD5E5
- [ ] 候選字背景：#FFFEF3F7
- [ ] 功能鍵：#FFF49AC1

**截圖位置：**
```
screenshots/3.1-pink-theme.png
```

---

### Test 3.2: Tech Blue Theme 測試

**步驟：**
1. 在 Settings 中選擇「Tech Blue」鍵盤主題
2. 開啟鍵盤

**預期結果：**
- [ ] 鍵盤顯示科技藍色主題
- [ ] 鍵盤背景：#FFC5DBEC
- [ ] 候選字背景：#FFD8E7F3
- [ ] 文字顏色：#FF314453

**截圖位置：**
```
screenshots/3.2-tech-blue-theme.png
```

---

### Test 3.3: Fashion Purple Theme 測試

**步驟：**
1. 在 Settings 中選擇「Fashion Purple」鍵盤主題
2. 開啟鍵盤

**預期結果：**
- [ ] 鍵盤顯示時尚紫色主題
- [ ] 鍵盤背景：#FFB0ACD5
- [ ] 候選字背景：#FFEFEDFF

**截圖位置：**
```
screenshots/3.3-fashion-purple-theme.png
```

---

### Test 3.4: Relax Green Theme 測試

**步驟：**
1. 在 Settings 中選擇「Relax Green」鍵盤主題
2. 開啟鍵盤

**預期結果：**
- [ ] 鍵盤顯示放鬆綠色主題
- [ ] 鍵盤背景：#FF8DC63F
- [ ] 候選字背景：#FFF2F5D5

**截圖位置：**
```
screenshots/3.4-relax-green-theme.png
```

---

### Test 3.5: 主題切換流暢度測試

**步驟：**
1. 在不同主題間快速切換（Light → Dark → Pink → Tech Blue）
2. 每次切換後開啟鍵盤

**預期結果：**
- [ ] 主題切換順暢
- [ ] 無閃退或錯誤
- [ ] 顏色正確套用
- [ ] 無視覺故障或顏色混淆

---

## Test Section 4: Navigation & Integration | 導航與整合測試

**驗證所有 Compose 畫面整合**

### Test 4.1: Navigation Drawer 測試

**步驟：**
1. 開啟 WFIME 主畫面
2. 點擊左上角「☰」或從左側滑動
3. 觀察 Navigation Drawer

**預期結果：**
- [ ] Navigation Drawer 順利開啟
- [ ] 使用 Compose UI
- [ ] Material3 樣式（圓角、陰影、動畫）
- [ ] 所有選單項目正常顯示
- [ ] 點擊項目可正常導航

---

### Test 4.2: Manage IM Screen 測試

**步驟：**
1. 從 Navigation Drawer 選擇「Manage Input Methods」
2. 觀察 Manage IM 畫面

**預期結果：**
- [ ] 畫面正常開啟
- [ ] 使用 Compose UI
- [ ] 輸入法列表正常顯示
- [ ] 可以新增/刪除輸入法

---

### Test 4.3: Word Dialog 測試

**步驟：**
1. 在 Manage IM Screen 中選擇一個輸入法
2. 嘗試新增自訂詞彙

**預期結果：**
- [ ] Word Dialog 正常開啟
- [ ] 使用 Compose UI
- [ ] Material3 對話框樣式
- [ ] 輸入欄位正常運作
- [ ] 儲存/取消按鈕正常

---

### Test 4.4: Emoji Picker 測試

**步驟：**
1. 開啟鍵盤
2. 切換至 Emoji 模式（如有）
3. 觀察 Emoji Picker

**預期結果：**
- [ ] Emoji Picker 正常顯示
- [ ] 使用 Compose UI
- [ ] 分類標籤正常
- [ ] 可以選擇並輸入 emoji

---

## Test Section 5: Edge Cases | 邊緣案例測試

### Test 5.1: 低記憶體情境測試

**步驟：**
1. 開啟多個大型 app（Chrome、YouTube、相機等）
2. 開啟 WFIME 鍵盤
3. 在 app 間快速切換

**預期結果：**
- [ ] 鍵盤不會閃退
- [ ] Settings 畫面不會重置
- [ ] Compose State 正確保存
- [ ] 無記憶體洩漏

---

### Test 5.2: 快速操作測試

**步驟：**
1. 快速開啟/關閉 Settings 畫面（10 次）
2. 快速切換鍵盤主題（10 次）
3. 快速切換 Light/Dark 模式（10 次）

**預期結果：**
- [ ] 無閃退
- [ ] 無 ANR（Application Not Responding）
- [ ] UI 反應靈敏
- [ ] 無視覺故障

---

### Test 5.3: 空資料庫測試

**步驟：**
1. 清除 WFIME app 資料（Settings → Apps → WFIME → Clear data）
2. 重新開啟 app
3. 測試 Settings 和鍵盤

**預期結果：**
- [ ] App 正常啟動（不閃退）
- [ ] Settings 顯示預設值
- [ ] 鍵盤使用預設主題
- [ ] 資料庫初始化成功

---

### Test 5.4: 無效輸入測試

**步驟：**
1. 在 Settings 中嘗試輸入無效值（如有輸入欄位）
2. 測試邊界值（極長文字、特殊字元等）

**預期結果：**
- [ ] App 正確處理無效輸入
- [ ] 顯示適當錯誤訊息
- [ ] 不會閃退
- [ ] 資料驗證正常運作

---

## Test Section 6: Performance | 效能測試

### Test 6.1: Settings 載入時間測試

**步驟：**
1. 使用碼錶或計時器
2. 點擊「Settings」
3. 測量到畫面完全顯示的時間

**預期結果：**
- [ ] 載入時間 < 500ms（良好）
- [ ] 載入時間 < 1000ms（可接受）
- [ ] 無明顯延遲或卡頓

**實際測量：** _______ ms

---

### Test 6.2: 鍵盤啟動時間測試

**步驟：**
1. 點擊文字輸入欄位
2. 測量鍵盤完全顯示的時間

**預期結果：**
- [ ] 啟動時間 < 300ms（良好）
- [ ] 啟動時間 < 500ms（可接受）
- [ ] 第二次啟動更快（有快取）

**實際測量：** _______ ms

---

### Test 6.3: 主題切換時間測試

**步驟：**
1. 在 Settings 中切換主題
2. 測量到鍵盤顏色更新的時間

**預期結果：**
- [ ] 切換時間 < 200ms（立即）
- [ ] 無明顯延遲
- [ ] 動畫流暢（60fps）

---

### Test 6.4: 記憶體使用測試

**步驟：**
1. 開啟 Android Studio Profiler 或使用 `adb shell dumpsys meminfo`
2. 測量 WFIME 記憶體使用

**預期結果：**
- [ ] 記憶體使用合理（< 100MB idle）
- [ ] 無明顯記憶體洩漏
- [ ] 長時間使用記憶體穩定

**實際測量：**
```bash
adb shell dumpsys meminfo nan.toload.main.hd
```

---

## Test Section 7: Compatibility | 相容性測試

### Test 7.1: Android 11 相容性

**裝置：** Android 11 (API 30)

**測試項目：**
- [ ] App 正常安裝
- [ ] Settings 正常運作
- [ ] 鍵盤正常顯示
- [ ] Light/Dark 主題正常
- [ ] 所有 Compose 畫面正常

**已知限制：**
- Material You 動態主題不支援（預期，Android 12+ 限定）

---

### Test 7.2: Android 12 相容性

**裝置：** Android 12 (API 31)

**測試項目：**
- [ ] App 正常安裝
- [ ] Material You 動態主題運作
- [ ] 鍵盤顏色跟隨系統主題
- [ ] 所有功能正常

---

### Test 7.3: Android 13 相容性

**裝置：** Android 13 (API 33)

**測試項目：**
- [ ] App 正常安裝
- [ ] 所有功能正常
- [ ] Material You 動態主題運作
- [ ] 無 Android 13 特定問題

---

### Test 7.4: Android 14 相容性

**裝置：** Android 14 (API 34)

**測試項目：**
- [ ] App 正常安裝
- [ ] 所有功能正常
- [ ] Material You 動態主題運作
- [ ] Edge-to-edge 顯示正確
- [ ] Predictive back gesture 支援

---

## Test Results Summary | 測試結果摘要

### Overall Test Status | 整體測試狀態

**測試日期：** __________________

**測試人員：** __________________

**APK 版本：** feature/material3-completion (commit: 3f27219)

**測試裝置：**

| 裝置編號 | 型號 | Android 版本 | 測試狀態 |
|----------|------|--------------|----------|
| 1 | | Android __ | ⬜ Pass / ⬜ Fail |
| 2 | | Android __ | ⬜ Pass / ⬜ Fail |
| 3 | | Android __ | ⬜ Pass / ⬜ Fail |

---

### Test Section Results | 測試章節結果

| 章節 | 測試項目 | 通過 | 失敗 | 狀態 |
|------|----------|------|------|------|
| 1. Settings Integration | 5 | | | ⬜ Pass / ⬜ Fail |
| 2. Material3 Keyboard Colors | 6 | | | ⬜ Pass / ⬜ Fail |
| 3. Custom Themes | 5 | | | ⬜ Pass / ⬜ Fail |
| 4. Navigation & Integration | 4 | | | ⬜ Pass / ⬜ Fail |
| 5. Edge Cases | 4 | | | ⬜ Pass / ⬜ Fail |
| 6. Performance | 4 | | | ⬜ Pass / ⬜ Fail |
| 7. Compatibility | 4 | | | ⬜ Pass / ⬜ Fail |
| **總計** | **32** | | | |

---

### Critical Issues Found | 發現的關鍵問題

**Issue 1:**
- **Severity:** ⬜ Critical / ⬜ High / ⬜ Medium / ⬜ Low
- **Description:**
- **Steps to Reproduce:**
- **Expected:**
- **Actual:**
- **Screenshot:**

**Issue 2:**
- **Severity:** ⬜ Critical / ⬜ High / ⬜ Medium / ⬜ Low
- **Description:**
- **Steps to Reproduce:**
- **Expected:**
- **Actual:**
- **Screenshot:**

*(Add more as needed)*

---

### Non-Critical Issues | 非關鍵問題

**Issue 1:**
- **Type:** ⬜ UI/UX / ⬜ Performance / ⬜ Documentation
- **Description:**
- **Recommendation:**

*(Add more as needed)*

---

### Performance Metrics | 效能指標

**Settings 載入時間：** _______ ms (目標: < 500ms)

**鍵盤啟動時間：** _______ ms (目標: < 300ms)

**主題切換時間：** _______ ms (目標: < 200ms)

**記憶體使用（idle）：** _______ MB (目標: < 100MB)

**評分：** ⬜ Excellent / ⬜ Good / ⬜ Acceptable / ⬜ Needs Improvement

---

### Material You Dynamic Theming | Material You 動態主題

**測試裝置：** Android 12+ only

**桌布主色測試：**

| 桌布顏色 | 鍵盤 Primary 顏色 | 符合預期 |
|----------|------------------|----------|
| 紅色 | | ⬜ Yes / ⬜ No |
| 藍色 | | ⬜ Yes / ⬜ No |
| 綠色 | | ⬜ Yes / ⬜ No |
| 預設 | #006C4C (WFIME green) | ⬜ Yes / ⬜ No |

**Material You 評分：** ⬜ Excellent / ⬜ Good / ⬜ Limited / ⬜ Not Working

---

### Regression Testing | 回歸測試

**Phase 1-3 變更未影響既有功能：**

- [ ] 輸入法切換正常
- [ ] 詞庫功能正常
- [ ] 符號輸入正常
- [ ] 候選字選擇正常
- [ ] 所有鍵盤佈局正常（注音、大易等）
- [ ] 設定儲存正常
- [ ] 資料庫讀寫正常

**評分：** ⬜ No Regression / ⬜ Minor Issues / ⬜ Major Regression

---

## Recommendations | 建議

### For Production Release | 正式發布建議

**如果所有測試通過：**
- [ ] ✅ 可以進行 Phase 4（Snackbar 實作）
- [ ] ✅ 可以建立 Pull Request
- [ ] ✅ 可以合併至 main branch

**如果發現問題：**
- [ ] 記錄所有 critical 和 high severity issues
- [ ] 修復後重新測試
- [ ] 確認修復未引入新問題

---

### Next Steps After Testing | 測試後下一步

**選項 A: 測試全部通過**
→ 繼續 Phase 4.1（Snackbar 實作）
→ 完成後達成 100% Material3 合規性

**選項 B: 發現輕微問題**
→ 記錄問題，繼續 Phase 4
→ 在 Phase 4 中一併修復

**選項 C: 發現嚴重問題**
→ 暫停 Phase 4
→ 優先修復 critical issues
→ 重新測試後再繼續

---

## Appendix A: Screenshot Checklist | 附錄 A：截圖檢查清單

**必需截圖（最少）：**

1. ✅ Settings Screen (light mode)
2. ✅ Settings Screen (dark mode)
3. ✅ Light theme keyboard
4. ✅ Dark theme keyboard
5. ✅ Material You keyboard (Android 12+)
6. ✅ Pink theme keyboard
7. ✅ Tech Blue theme keyboard
8. ✅ Composing text popup
9. ✅ Candidate view

**建議截圖位置：**
```
C:\Users\nan\CodeSpace\nanime-main\test-screenshots\
├── phase1-settings\
│   ├── 1.1-settings-launch.png
│   ├── 1.2-settings-items.png
│   └── ...
├── phase3-keyboard-colors\
│   ├── 2.1-light-keyboard.png
│   ├── 2.2-dark-keyboard.png
│   ├── 2.4-material-you-*.png
│   └── ...
└── custom-themes\
    ├── 3.1-pink.png
    ├── 3.2-tech-blue.png
    └── ...
```

---

## Appendix B: Logcat Commands | 附錄 B：Logcat 指令

**查看 WFIME 相關 log：**
```bash
# 所有 WFIME log
adb logcat | grep -i "LIME"

# Settings 相關
adb logcat | grep -i "LIMEPreference"

# 資料庫相關
adb logcat | grep -i "DBServer"

# Compose 相關
adb logcat | grep -i "Compose"

# 錯誤和警告
adb logcat *:E *:W | grep -i "nan.toload"
```

**清除 logcat：**
```bash
adb logcat -c
```

**儲存 logcat 到檔案：**
```bash
adb logcat > test-logcat.txt
```

---

## Appendix C: Test Data | 附錄 C：測試資料

**測試用輸入：**

**注音測試：**
- 你好（ㄋㄧˇ ㄏㄠˇ）
- 測試（ㄘㄜˋ ㄕˋ）
- 鍵盤（ㄐㄧㄢˋ ㄆㄢˊ）

**特殊符號測試：**
- @#$%^&*()
- 中文標點：，。！？；：

**長文字測試：**
- 重複輸入 50+ 字，測試效能

---

## Conclusion | 結論

本測試指南涵蓋 Phase 1-3 的所有關鍵功能。完成測試後：

1. **填寫測試結果摘要**
2. **記錄所有發現的問題**
3. **上傳截圖到指定位置**
4. **回報測試結果**

**預期測試時間：** 1-2 小時（單一裝置）

**若有問題，請記錄：**
- 詳細重現步驟
- 預期 vs 實際結果
- 截圖或錄影
- Logcat 輸出（如相關）

測試完成後，我們將繼續 Phase 4（Snackbar 實作）！

---

**Document Version:** 1.0
**Created:** 2026-01-20
**For Build:** feature/material3-completion (3f27219)
**Status:** Ready for Device Testing
