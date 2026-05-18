"""
Scan WFIME databases for all PRC-related vocabulary.
Output organized by category to a UTF-8 file for review.
"""
import sqlite3
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_FILE = os.path.join(SCRIPT_DIR, 'prc_scan.txt')

TARGETS = [
    ('dayiunip.db',             'dayi'),
    ('phoneticcomplete.db',     'phonetic'),
    ('phoneticcompletebig5.db', 'phonetic'),
]

# Organized by category for user review
CATEGORIES = {
    '政府部會': [
        '國務院', '全國人大', '全國政協', '中華人民共和國',
        '國防部', '外交部', '公安部', '國家安全部', '財政部',
        '教育部', '科技部', '農業部', '水利部', '交通部',
        '衛生部', '衛生廳', '民政部', '司法部', '文化部',
        '體育總局', '國土資源部', '環境保護部', '建設部',
        '勞動部', '人事部', '發展改革委', '國家計委', '國家計劃委員會',
        '最高人民法院', '最高人民檢察院', '人民檢察院', '人民法院',
        '全國人民代表大會', '中國人民政治協商會議',
        '中央軍事委員會', '中央軍委',
    ],
    '黨政機構': [
        '中國共產黨', '共產黨員', '黨員', '入黨',
        '中央政治局', '中央委員會', '中央書記處',
        '中央紀律檢查委員會', '中紀委', '紀委',
        '政治部', '組織部', '宣傳部', '統戰部',
        '全國代表大會', '黨代會', '黨代表',
        '地委', '旗委',
    ],
    '軍事': [
        '中國人民解放軍', '人民海軍', '人民空軍', '武裝警察', '武警',
        '人民戰爭', '中央警衛局', '解放區',
    ],
    '特有機構/組織': [
        '中國科學院', '中科院', '中國社會科學院', '社科院',
        '全國婦女聯合會', '婦聯', '全國總工會', '工商聯',
        '中國共青團', '共青團', '少先隊',
        '中國人民銀行', '人民銀行',
        '新華書店',
    ],
    '政治運動/事件': [
        '四人幫', '批林批孔', '反右運動', '肅反運動',
        '三反五反', '土地改革', '土改', '公私合營',
        '合作化運動', '反革命', '走資本主義道路當權派',
        '走資派', '革命委員會', '工農兵', '上山下鄉',
        '知識青年', '知青', '下放',
    ],
    '政治術語': [
        '中共中央', '黨中央', '無產階級', '資產階級',
        '小資產階級', '帝國主義', '反動派', '反動',
        '資本主義', '社會主義', '共產主義', '集體主義',
        '群眾路線', '民主集中制', '統一戰線', '人民民主',
        '人民民主專政', '人民民主獨裁',
        '四個堅持', '三個代表', '科學發展觀', '中國夢',
        '四個現代化', '改革開放', '社會主義初級階段',
        '有中國特色的社會主義', '社會主義建設',
    ],
    '經濟/行政術語': [
        '國有企業', '國企', '央企', '鄉鎮企業', '集體企業',
        '合作社', '生產隊', '大隊', '公社', '社員',
        '下崗', '下崗工人', '國營', '計劃經濟',
        '經濟特區', '深圳特區',
        '人民公社社員',
        '戶籍', '戶口', '暫住證', '單位',
    ],
    '國家象徵/稱謂': [
        '五星紅旗', '人民英雄紀念碑', '天安門城樓',
        '開國典禮', '國慶節', '建黨節', '建軍節',
        '毛澤東思想', '鄧小平理論',
    ],
    '媒體/出版': [
        '中央電視台', '央視', '中央人民廣播電台', '中國國際廣播電台',
        '中國青年報', '解放日報', '光明日報', '參考消息',
        '求是', '紅旗', '人民文學',
    ],
    '教育/稱謂': [
        '小學生手冊', '政治課', '思想品德',
        '革命英雄', '勞動模範', '勞模', '英雄模範',
        '先進工作者', '優秀黨員', '入團',
    ],
}

ALL_TERMS = set()
for terms in CATEGORIES.values():
    ALL_TERMS.update(terms)


def scan_db(db_path: str, table: str):
    conn = sqlite3.connect(db_path)
    c = conn.cursor()

    found_word = {}   # term -> [row_id, ...]
    found_related = {}  # term -> count

    placeholders = ','.join(['?' for _ in ALL_TERMS])
    c.execute(
        f'SELECT _id, word FROM [{table}] WHERE word IN ({placeholders})',
        list(ALL_TERMS),
    )
    for row_id, word in c.fetchall():
        found_word.setdefault(word, []).append(row_id)

    terms_list = list(ALL_TERMS)
    like_conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in terms_list])
    c.execute(
        f'SELECT related FROM [{table}] WHERE related IS NOT NULL AND ({like_conditions})',
        terms_list,
    )
    for (rv,) in c.fetchall():
        for t in rv.split('|'):
            if t in ALL_TERMS:
                found_related[t] = found_related.get(t, 0) + 1

    conn.close()
    return found_word, found_related


def main():
    with open(OUT_FILE, 'w', encoding='utf-8') as out:
        for db_filename, table in TARGETS:
            db_path = os.path.join(SCRIPT_DIR, db_filename)
            out.write(f'\n====== {db_filename} ======\n')
            found_word, found_related = scan_db(db_path, table)

            if not found_word and not found_related:
                out.write('  (無符合詞條)\n')
                continue

            out.write(f'\n  [word 欄找到] {sum(len(v) for v in found_word.values())} 條:\n')
            for cat, terms in CATEGORIES.items():
                cat_hits = {t: found_word[t] for t in terms if t in found_word}
                if cat_hits:
                    out.write(f'    [{cat}]\n')
                    for t, ids in sorted(cat_hits.items()):
                        out.write(f'      {t}  (×{len(ids)})\n')

            if found_related:
                out.write(f'\n  [related 欄找到] {sum(found_related.values())} 處:\n')
                for t, cnt in sorted(found_related.items()):
                    out.write(f'    {t}  (×{cnt})\n')

    print('Scan complete. Results in:', OUT_FILE)


if __name__ == '__main__':
    main()
