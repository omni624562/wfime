# -*- coding: utf-8 -*-
"""WFIME emoji 資料產生器

從 Unicode emoji-test.txt 與 CLDR annotations 重建:
  - LimeStudio/app/src/main/assets/emojis.json  (emoji 選擇器資料)
  - LimeStudio/app/src/main/res/raw/emoji.db    (打字聯想 tag 資料庫,en/tw/cn 三表)

用法:
  python gen_emoji.py            # 使用 cache/ 內的來源檔
  python gen_emoji.py --download # 先下載最新來源再產生

未來 Unicode/CLDR 改版時:更新 EMOJI_TEST_URL 與 CLDR_RELEASE,
執行 --download,並把 LimeDB.java 的 EMOJI_DB_VERSION 加一,
裝置端就會在 App 更新後自動重新複製新資料庫。
"""
import json, os, re, sqlite3, sys, urllib.request
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, "cache")
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT_JSON = os.path.join(REPO, "LimeStudio", "app", "src", "main", "assets", "emojis.json")
OUT_DB = os.path.join(REPO, "LimeStudio", "app", "src", "main", "res", "raw", "emoji.db")

EMOJI_TEST_URL = "https://www.unicode.org/Public/17.0.0/emoji/emoji-test.txt"
CLDR_RELEASE = "release-48"
CLDR_BASE = f"https://raw.githubusercontent.com/unicode-org/cldr/{CLDR_RELEASE}/common"
SOURCES = {
    "emoji-test.txt": EMOJI_TEST_URL,
    "ann_en.xml": f"{CLDR_BASE}/annotations/en.xml",
    "ann_zhHant.xml": f"{CLDR_BASE}/annotations/zh_Hant.xml",
    "ann_zh.xml": f"{CLDR_BASE}/annotations/zh.xml",
    "der_en.xml": f"{CLDR_BASE}/annotationsDerived/en.xml",
    "der_zhHant.xml": f"{CLDR_BASE}/annotationsDerived/zh_Hant.xml",
    "der_zh.xml": f"{CLDR_BASE}/annotationsDerived/zh.xml",
}

GROUP_MAP = {
    "Smileys & Emotion": "SMILEYS",
    "People & Body": "PEOPLE",
    "Animals & Nature": "ANIMALS_NATURE",
    "Food & Drink": "FOOD_DRINK",
    "Travel & Places": "TRAVEL_PLACES",
    "Activities": "ACTIVITIES",
    "Objects": "OBJECTS",
    "Symbols": "SYMBOLS",
    "Flags": "FLAGS",
}
SKIN_RANGE = set(range(0x1F3FB, 0x1F400))
MAX_EMOJI_PER_TAG = 8   # 每個 tag 最多對應的 emoji 數,避免常見 tag 灌爆候選列
MAX_KEYWORDS = 6        # 選擇器每個 emoji 保留的關鍵字數
VS16 = "️"


def download():
    for name, url in SOURCES.items():
        path = os.path.join(CACHE, name)
        print(f"downloading {url}")
        urllib.request.urlretrieve(url, path)


def load_annotations(paths):
    """回傳 {emoji(去 VS16): (tts 名稱, [關鍵字])}"""
    result = {}
    for path in paths:
        root = ET.parse(path).getroot()
        for node in root.iter("annotation"):
            cp = node.get("cp")
            if not cp or not node.text:
                continue
            key = cp.replace(VS16, "")
            tts, kws = result.get(key, (None, []))
            if node.get("type") == "tts":
                tts = node.text.strip()
            else:
                kws = [k.strip() for k in node.text.split("|") if k.strip()]
            result[key] = (tts, kws)
    return result


def parse_emoji_test():
    """回傳 (entries, skin_capable): fully-qualified、排除膚色變體、依檔案順序"""
    entries = []
    skin_capable = set()
    group = None
    line_re = re.compile(
        r"^([0-9A-F ]+?)\s*;\s*fully-qualified\s*#\s*(\S+)\s+E\d+\.\d+\s+(.*)$")
    with open(os.path.join(CACHE, "emoji-test.txt"), encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if line.startswith("# group:"):
                group = line.split(":", 1)[1].strip()
                continue
            m = line_re.match(line)
            if not m or group not in GROUP_MAP:
                continue
            cps = [int(c, 16) for c in m.group(1).split()]
            name = m.group(3).strip()
            if any(c in SKIN_RANGE for c in cps):
                skin_capable.add(name.split(":")[0].strip())
                continue
            entries.append((GROUP_MAP[group], "".join(chr(c) for c in cps), name))
    return entries, skin_capable


def build_json(entries, skin_capable, en_ann):
    categories = {v: [] for v in GROUP_MAP.values()}
    for cat, char, name in entries:
        tts, kws = en_ann.get(char.replace(VS16, ""), (None, []))
        keywords = []
        for k in ([tts] if tts else []) + kws:
            k = k.lower()
            if k and k not in keywords:
                keywords.append(k)
        if not keywords:
            keywords = [name.lower()]
        obj = {"char": char, "keywords": keywords[:MAX_KEYWORDS]}
        if name in skin_capable:
            obj["hasSkinTone"] = True
        categories[cat].append(obj)
    with open(OUT_JSON, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"categories": [{"name": k, "emojis": v} for k, v in categories.items()]},
                  f, ensure_ascii=False, indent=1)
    total = sum(len(v) for v in categories.values())
    print("emojis.json:", {k: len(v) for k, v in categories.items()}, "TOTAL:", total)


def build_rows(entries, annmap, lower):
    """tag -> emoji 列;依 emoji-test 順序,每 tag 最多 MAX_EMOJI_PER_TAG 個"""
    tagmap = {}
    for cat, char, name in entries:
        tts, kws = annmap.get(char.replace(VS16, ""), (None, []))
        for k in ([tts] if tts else []) + kws:
            k = k.strip()
            if lower:
                k = k.lower()
            if not k:
                continue
            lst = tagmap.setdefault(k, [])
            if char not in lst and len(lst) < MAX_EMOJI_PER_TAG:
                lst.append(char)
    return [(tag, ch) for tag, lst in tagmap.items() for ch in lst]


def build_db(entries, en_ann, tw_ann, cn_ann):
    if os.path.exists(OUT_DB):
        os.remove(OUT_DB)
    con = sqlite3.connect(OUT_DB)
    cur = con.cursor()
    for t in ("en", "tw", "cn"):
        ctype = "CHAR" if t == "en" else "TEXT"
        cur.execute(f"CREATE TABLE {t} (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    f"tag TEXT NOT NULL, value {ctype} NOT NULL)")
        cur.execute(f"CREATE INDEX {t}_tag ON {t} (tag)")
    rows = {
        "en": build_rows(entries, en_ann, lower=True),
        "tw": build_rows(entries, tw_ann, lower=False),
        "cn": build_rows(entries, cn_ann, lower=False),
    }
    for t, r in rows.items():
        cur.executemany(f"INSERT INTO {t} (tag, value) VALUES (?, ?)", r)
    con.commit()
    cur.execute("VACUUM")
    con.close()
    print("emoji.db:", {t: len(r) for t, r in rows.items()})


def main():
    if "--download" in sys.argv:
        os.makedirs(CACHE, exist_ok=True)
        download()
    entries, skin_capable = parse_emoji_test()
    en_ann = load_annotations([os.path.join(CACHE, "ann_en.xml"), os.path.join(CACHE, "der_en.xml")])
    tw_ann = load_annotations([os.path.join(CACHE, "ann_zhHant.xml"), os.path.join(CACHE, "der_zhHant.xml")])
    cn_ann = load_annotations([os.path.join(CACHE, "ann_zh.xml"), os.path.join(CACHE, "der_zh.xml")])
    build_json(entries, skin_capable, en_ann)
    build_db(entries, en_ann, tw_ann, cn_ann)

    # 抽查
    con = sqlite3.connect(f"file:{OUT_DB}?mode=ro", uri=True)
    for table, tag in [("tw", "貓"), ("tw", "鳳凰"), ("en", "phoenix"), ("cn", "凤凰")]:
        vals = [r[0] for r in con.execute(f"SELECT value FROM {table} WHERE tag=?", (tag,))]
        print(f"  抽查 {table}:{tag} -> {vals}")
    con.close()


if __name__ == "__main__":
    main()
