"""
Remove PRC-specific vocabulary from WFIME databases.
Keeps terms also used in Taiwan ROC context.
"""
import sqlite3
import zipfile
import os
import sys

REMOVE_PRC = {
    # 政府部會（PRC 專屬，ROC 無同名）
    '國務院', '公安部', '國土資源部', '國家計委', '中央軍委',
    '人事部', '民政部', '水利部', '衛生部', '司法部',
    '人民法院',
    # 黨政機構
    '中國共產黨', '中共中央', '共產黨員', '黨員', '入黨',
    '中央政治局', '中央委員會', '中央書記處',
    '中紀委', '紀委', '宣傳部', '組織部', '政治部',
    # 特有機構/組織
    '中國科學院', '中科院', '人民銀行', '共青團', '少先隊', '婦聯', '新華書店',
    # 政治運動/事件
    '上山下鄉', '下放', '反革命', '土地改革', '土改', '工農兵',
    '知識青年', '知青', '走資派',
    # 政治術語
    '共產主義', '反動', '反動派', '四個現代化', '帝國主義', '改革開放',
    '民主集中制', '無產階級', '群眾路線', '集體主義', '毛澤東思想', '資產階級',
    # 媒體/出版
    '中央電視台', '央視', '光明日報', '參考消息', '紅旗',
    # 其他
    '下崗', '公社', '國有企業', '國營', '計劃經濟', '經濟特區', '勞動模範',
}

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

TARGETS = [
    ('dayiunip.db',             'dayi',     'dayiunip.zip'),
    ('phoneticcomplete.db',     'phonetic', 'phoneticcomplete.zip'),
    ('phoneticcompletebig5.db', 'phonetic', 'phoneticcompletebig5.zip'),
]

LOG_FILE = os.path.join(SCRIPT_DIR, 'prc_removal_results.txt')


def clean_related(related_value: str, terms: set) -> str | None:
    if not related_value:
        return related_value
    parts = [p for p in related_value.split('|') if p not in terms]
    return '|'.join(parts) if parts else None


def process_db(db_filename: str, table: str, log) -> tuple[int, int]:
    db_path = os.path.join(SCRIPT_DIR, db_filename)
    conn = sqlite3.connect(db_path)
    c = conn.cursor()

    placeholders = ','.join(['?' for _ in REMOVE_PRC])
    c.execute(f'DELETE FROM [{table}] WHERE word IN ({placeholders})', list(REMOVE_PRC))
    deleted = c.rowcount
    log.write(f'  Deleted word entries : {deleted}\n')

    terms_list = list(REMOVE_PRC)
    like_conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in terms_list])
    c.execute(
        f'SELECT _id, related FROM [{table}] WHERE related IS NOT NULL AND ({like_conditions})',
        terms_list,
    )
    rows = c.fetchall()

    updated = 0
    for row_id, related_val in rows:
        parts = set(related_val.split('|'))
        if parts & REMOVE_PRC:
            new_related = clean_related(related_val, REMOVE_PRC)
            if new_related != related_val:
                c.execute(f'UPDATE [{table}] SET related = ? WHERE _id = ?', (new_related, row_id))
                updated += 1

    log.write(f'  Updated related rows : {updated}\n')

    conn.commit()
    conn.execute('VACUUM')
    conn.close()
    return deleted, updated


def verify(db_filename: str, table: str, log) -> int:
    db_path = os.path.join(SCRIPT_DIR, db_filename)
    conn = sqlite3.connect(db_path)
    c = conn.cursor()

    placeholders = ','.join(['?' for _ in REMOVE_PRC])
    c.execute(f'SELECT COUNT(*) FROM [{table}] WHERE word IN ({placeholders})', list(REMOVE_PRC))
    word_remaining = c.fetchone()[0]

    terms_list = list(REMOVE_PRC)
    like_conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in terms_list])
    c.execute(
        f'SELECT _id, related FROM [{table}] WHERE related IS NOT NULL AND ({like_conditions})',
        terms_list,
    )
    rows = c.fetchall()
    related_remaining = sum(1 for _, rv in rows if set(rv.split('|')) & REMOVE_PRC)

    conn.close()
    return word_remaining + related_remaining


def repack_zip(db_filename: str, zip_filename: str, log) -> None:
    db_path  = os.path.join(SCRIPT_DIR, db_filename)
    zip_path = os.path.join(SCRIPT_DIR, zip_filename)
    with zipfile.ZipFile(zip_path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(db_path, arcname=db_filename)
    zip_size = os.path.getsize(zip_path)
    log.write(f'  ZIP size             : {zip_size:,} bytes\n')


def main() -> None:
    with open(LOG_FILE, 'w', encoding='utf-8') as log:
        log.write('=== remove_prc_terms.py results ===\n\n')

        for db_filename, table, zip_filename in TARGETS:
            log.write(f'--- {db_filename} ---\n')
            process_db(db_filename, table, log)

            remaining = verify(db_filename, table, log)
            if remaining > 0:
                log.write(f'  WARNING: {remaining} entries still remain!\n')
            else:
                log.write(f'  Verification         : PASS (0 remaining)\n')

            log.write(f'  Repacking {zip_filename} ...\n')
            repack_zip(db_filename, zip_filename, log)
            log.write('\n')

        log.write('Done.\n')

    print('Results written to:', LOG_FILE)


if __name__ == '__main__':
    main()
