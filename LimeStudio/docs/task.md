# WFIME 備忘錄（Memo）功能與極簡工具列實作排程

- `[x]` 1. 資料庫持久層實作 (SQLite / LimeDB)
  - `[x]` 於 `LimeDB.java` 定義 `memo` 資料表與開機/升級 SQL 宣告
  - `[x]` 實作備忘錄 Java 資料模型 `MemoObj.java`
  - `[x]` 實作資料庫增刪查改（CRUD）與置頂排序輔助方法
- `[x]` 2. Compose 視圖橋接層 (ComposeBridge.kt)
  - `[x]` 於 `ComposeBridge.kt` 建立生命週期與 Insets 封裝的 `createMemoPanelView` 橋接介面
  - `[x]` 建立 Compose 常用備忘錄卡片面板 `MemoPanel.kt`
- `[x]` 3. 輸入法引擎核心狀態管理 (LIMEService.java)
  - `[x]` 宣告 `mMemoKeyboardView` 與內置狀態切換
  - `[x]` 實作 `toggleMemoVisibility()` 處理備忘錄面板與鍵盤/表情面板的互斥切換
- `[x]` 4. 工具列極簡化排版與事件串接 (CandidateView.kt)
  - `[x]` 修改 `ToolbarRow()` 以僅保留「備忘錄」、「表情符號」、「設定」三大旗艦按鈕
  - `[x]` 綁定點選事件，對接 `toggleMemoVisibility()` 等服務方法
- `[x]` 5. 編譯、實體部署與功能驗證
  - `[x]` 執行 `./gradlew assembleDebug` 進行雙端靜態編譯
  - `[x]` 部署至 Pixel 5/Pixel 7 手機與 SM-X730 平板並完成手動測試驗證
