# 麥田輸入法一站式重構、死碼大掃除與字根不上屏優化技術說明書

本文件詳細記錄了「麥田輸入法 (WFIME)」在版本 `1.2.0` 中所進行的重大技術架構重構、程式碼清理、輸入體驗（字根不上屏）優化，以及針對新設備（如 Pixel 7+）在釋放版本（Release Build）下資源收縮導致崩潰的關鍵性修復。

---

## 🎯 1. 麥田金一站式首頁重構 (Consolidated Settings Dashboard)

### 1.1 背景與設計動機
傳統輸入法引導與設定流程往往包含繁複的分步精靈（Wizard Setup）與側邊抽屜選單（Navigation Drawer），對日常打字而言是多餘的架構負擔。
我們重構後將其整併為**單一入口、直達核心**的極簡主控台：
* **全新卡片區塊**：於 Jetpack Compose 設計的 `SettingsScreen.kt` 頁面頂部，新增一站式的「下載輸入法」麥田金卡片。採用 Slate Dark 灰黑色背景搭配 HSL 調和之麥田金主題色。
* **即時反應式更新 (Reactive State Sync)**：與底層 SQLite 資料庫的狀態緊密相連，動態偵測「注音」與「大易」的載入狀態。按鈕文字即時呈現「載入注音」/「注音 (已載入)」狀態，免去手動刷新的困擾。

### 1.2 載入流程非同步化與即時重刷
為了在大規模字根資料庫寫入（包含數萬條記錄）時，主執行緒依然維持絕對的流暢，我們重新對接了非同步執行緒與事件：
1. **設定首頁回呼 (Compose Bridge Callbacks)**：
   當使用者在卡片點擊「載入」時，Compose UI 會觸發回呼函數至 `MainActivity.java`。
2. **啟動非同步對話框 (`SetupImLoadDialog`)**：
   在 `MainActivity` 拉起非同步載入對話框，透過非同步背景執行緒 `SetupImRunnable` 安全執行 SQLite 大量批次寫入，頂部顯示精緻的加載/進度條，並統一使用 `LoadingDialogHelper` 管理進度彈窗。
3. **完成載入重刷機制**：
   當背景載入完成後，透過 `SetupImHandler` 發送完成訊號（`MSG_FINISH_LOAD`）回主執行緒，主執行緒立刻調用 `SettingsViewModel` 重新偵測資料庫，驅動 Compose 設定首頁即時反應式重刷，達到極致的流暢同步。

---

## 🗑️ 2. 死碼大掃除與架構極簡化 (Legacy Code Cleanup)

為了擺脫歷史包袱，釋放記憶體並加快應用啟動速度，我們執行了全盤的 dead code 掃描與徹底清理。本項工程清除了專案中大量無效的遺留檔案：

### 2.1 刪除的 Java/Kotlin 原始碼檔案（共 28 個）
* **舊版設定模組 (`limesettings/`)**：
  `ManageImFragment.java`、`LIMEPreference.java`、`LIMEPreferenceActivity.java`、`LIMEPreferenceHC.java`、`MultiListPreference.java` 等共 9 個檔案，改由全新 Jetpack Compose 介面統一接管。
* **舊版側邊抽屜模組 (`ui/compose/`)**：
  `NavigationDrawerScreen.kt`、`NavigationViewModel.kt`、`NavigationDrawerItem.kt`、`NavigationMenu.kt`、`NavigationItem.kt` 等共 7 個檔案，徹底砍除 drawer 邏輯。
* **舊版其他 UI 與 Dialog 檔案 (`ui/` 等)**：
  `AboutFragment.java`、`MainFragment.java`、`SetupImFragment.kt`、`CINBackupRunnable.java`、`RelatedAddDialog.java`、`WordAddDialog.java`、`WordAddRunnable.java` 等共 12 個檔案。

### 2.2 刪除的 XML Layout 佈局檔案（共 18 個）
* 刪除包含 `fragment_about.xml`、`fragment_main.xml`、`fragment_setup_im.xml`、`fragment_manage_im.xml`、`fragment_manage_related.xml`、`fragment_manage_word.xml`、`related_add.xml`、`word_add.xml` 等 18 個過時的 XML 佈局檔。

### 2.3 核心組件精煉與首頁極速啟動
* **`ComposeBridge.kt`**：移除 `createNavigationDrawerView` 等所有舊有橋接，只保留核心 Emoji 與 Settings 首頁的乾淨邏輯。
* **`MainActivity.java`**：拔除 drawer 切換與側欄狀態同步，實現啟動直達設定主頁。
* **`activity_main.xml`**：將外層 root 佈局從 DrawerLayout 重構為效能更好的 `androidx.coordinatorlayout.widget.CoordinatorLayout`，完全移除抽屜視窗容器（`navigation_drawer_container` FrameLayout），使 App 啟動速度大幅縮短。
* **`Lime.java`**：常數精簡化，清空 HS 輸入法 zip 位址，僅保留大易與注音之對照表 URL。

---

## ⌨️ 3. 字根不上屏體驗優化 (Radicals Off-Screen Optimization)

### 3.1 傳統輸入法的體驗缺陷
在進行大易輸入（例如拼打「詹」，對照字根為 `魚鳥言` / 英文 `nh1`）或注音輸入時，使用者正在鍵入的字根，以前會以「Composing Text（組字中文字）」的形式直接即時填入目標應用程式（例如 Google 搜尋框、LINE 聊天輸入框）。
這會強制目標應用程式在漢字尚未拼完前，就針對字根字母（`魚鳥言`）進行高頻率的聯想與自動建議（例如被搜尋引擎誤判想搜尋「魚鳥之戀」），對用戶組字輸入的流暢感造成極大干擾。

### 3.2 智慧字根不上屏實作細節
在 [LIMEService.java](file:///c:/Storage/workspace/nanime-main/LimeStudio/app/src/main/java/net/toload/main/hd/LIMEService.java) 中，我們重構了 `updateCandidates(final boolean getAllRecords)` 方法，**將 `ic.setComposingText` 呼叫移除**，僅執行鍵盤內部的字根渲染與懸浮視窗繪製：

```java
            // getComposingDisplayString(keyString) updates the CandidateView and Floating Composing Popup.
            // We no longer set the composing text in the input connection (ic.setComposingText) to prevent
            // intermediate radicals/composing text from being injected into the host application's editor.
            getComposingDisplayString(keyString);
```

### 3.3 優化成效與運作機制
1. **輸入框極致乾淨**：組字過程中，目標 Host App 的輸入框**始終保持清空狀態**，絕不出現干擾性的字根殘留。
2. **狀態即時同步**：鍵入的字根依然即時且精準地呈現在輸入法的**組字浮動懸浮窗**與**候選字列表最前端**，打字進度一目了然。
3. **無縫漢字提交**：點選漢字（如 `詹`）或按下 Space 確認時，漢字以最乾淨的 `commitText` 正確填入游標處，體驗無比流暢。

---

## ⚙️ 4. 現代化獨立版本號管理 (Version Decoupling)

為避免使用動態時間戳記產生 `versionCode` 導致 Gradle build cache 每次編譯完全失效，我們實現了版號獨立管理解耦：
* **獨立 `version.properties` 檔案**：於 `LimeStudio/version.properties` 中集中宣告：
  ```properties
  VERSION_MAJOR=1
  VERSION_MINOR=2
  VERSION_PATCH=0
  ```
* **Gradle 動態讀取與 versionCode 自動遞增**：
  在 `LimeStudio/app/build.gradle` 中動態讀取 Properties：
  ```groovy
  def versionPropsFile = rootProject.file('version.properties')
  def versionProps = new Properties()
  if (versionPropsFile.exists()) {
      versionPropsFile.withInputStream { versionProps.load(it) }
  }
  def major = versionProps['VERSION_MAJOR'] ?: "1"
  def minor = versionProps['VERSION_MINOR'] ?: "0"
  def patch = versionProps['VERSION_PATCH'] ?: "0"
  
  // versionCode 以當天日期 yyyyMMdd 安全轉為 Integer，無 caching 失效問題
  def build = new Date().format('yyyyMMdd')
  versionCode build.toInteger()
  versionName "${major}.${minor}.${patch}"
  ```

---

## 📱 5. 新設備 Release 建置資源保留 (keep.xml Release Whitelist)

### 5.1 崩潰問題與根本原因分析
在 Pixel 7 等配備較新系統的裝置上，安裝「偵錯版本 (Debug Build)」可以完美啟動，但安裝「釋放版本 (Release Build)」時卻會發生以下嚴重崩潰：
`android.content.res.Resources$NotFoundException: Resource ID #0x0`
崩潰點位於 `LIMEBaseKeyboard.java` 內透過動態反射尋找資源 ID 處。

**根本原因**：
在 `app/build.gradle` 中，`release` 建置類型啟用了程式碼與資源收縮：
```groovy
minifyEnabled = true
shrinkResources = true
```
麥田輸入法的鍵盤佈局 XML 檔（位於 `app/src/main/res/xml/`）是由 `LIMEKeyboardSwitcher.getKeyboardXMLID()` 透過 `getIdentifier(value, "xml", ...)` 的**動態反射載入**。
在 Java 原始碼中**完全沒有靜態直接引用**（如 `R.xml.lime`），導致 Gradle 資源收縮器 (Resource Shrinker) 誤判定為「未使用的資源」，進而將其從 APK 中完全移除或取代為無效空殼，導致動態載入時拋出 `NotFoundException` 崩潰。

### 5.2 解決方案與相容性驗證
* **引入 `keep.xml` 保留配置**：
  新建 [keep.xml](file:///c:/Storage/workspace/nanime-main/LimeStudio/app/src/main/res/raw/keep.xml) 設定檔於 `app/src/main/res/raw/`：
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources xmlns:tools="http://schemas.android.com/tools"
      tools:keep="@xml/*" />
  ```
  精確指示 `tools:keep="@xml/*"` 強制保留 `xml` 目錄下的所有佈局檔，不參與收縮。
* **封裝與多機相容性驗證**：
  我們重新編譯了經過深度優化的 Release 版本，並使用 ADB 將其部署至 **Pixel 7** 與 **Pixel 5**。
  兩台實體設備均成功裝載，切換鍵盤與打字皆完全流暢、無任何閃退與資源遺失異常，相容性測試圓滿通過。
