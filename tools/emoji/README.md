# Emoji 資料產生器 | Emoji Data Generator

從 Unicode 官方 `emoji-test.txt` 與 CLDR annotations 重建 WFIME 的兩個 emoji 資料檔：

| 輸出 | 用途 | 來源 |
|---|---|---|
| `LimeStudio/app/src/main/assets/emojis.json` | Emoji 選擇器 | emoji-test.txt（分類/順序/膚色）＋ CLDR en（關鍵字） |
| `LimeStudio/app/src/main/res/raw/emoji.db` | 打字時的 emoji 聯想（tag → emoji） | CLDR en / zh-Hant / zh annotations |

## 使用方式 | Usage

```bash
# 用 cache/ 內既有來源重新產生
python gen_emoji.py

# 先下載最新來源再產生
python gen_emoji.py --download
```

## Emoji 版本更新流程 | Upgrading to a new Emoji version

1. 修改 `gen_emoji.py` 開頭的 `EMOJI_TEST_URL`（指向新的 Unicode 版本目錄）與 `CLDR_RELEASE`（對應的 CLDR release）
2. 執行 `python gen_emoji.py --download`
3. 把 `LimeDB.java` 的 `EMOJI_DB_VERSION` 加一 —— 裝置端會在 App 更新後自動重新複製新的 emoji.db
4. 重新建置並測試

## 設計要點 | Design notes

- 每個 tag 最多對應 8 個 emoji（`MAX_EMOJI_PER_TAG`），避免「臉」這類常見 tag 灌爆候選列
- 排除膚色變體，選擇器以 `hasSkinTone` 標記＋長按選擇呈現
- 顯示端由 `PaintCompat.hasGlyph` ＋ `androidx.emoji2`（GMS 可下載字型）過濾，資料檔不需要跟裝置字型能力對齊
- 目前版本：Emoji 17.0（Unicode 17.0）＋ CLDR 48
