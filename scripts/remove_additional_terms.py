"""
Remove politically sensitive, outdated, and offensive terms from WFIME databases.
Targets: dayiunip.db, phoneticcomplete.db, phoneticcompletebig5.db
"""
import sqlite3
import zipfile
import os
import sys

REMOVE_TERMS = {
    # 中共政治術語
    '革命', '共產', '毛主席', '解放軍', '中共', '共黨', '黨中央', '天安門',
    '天安門廣場', '文化大革命', '大躍進', '人民公社', '政治局', '紅衛兵',
    '紅軍', '八路軍', '列寧主義', '為人民服務', '人民大會堂',
    # 蘇聯術語
    '加盟共和國', '蘇維埃', '第三國際',
    # PRC 已改制部會
    '冶金工業部', '航天工業部', '航空工業部', '輕工業部', '紡織工業部', '林業部', '商業部',
    # 台灣已廢止機構
    '台灣省政府', '台灣省議會', '台灣省主席',
    '台北縣政府', '台中縣政府', '台南縣政府', '台東縣政府', '高雄縣政府',
    # 不雅詞彙
    '王八蛋', '混蛋', '賤人', '狗屁', '他媽的', '滾開', '滾蛋', '兔崽子', '窩囊廢', '廢物',
    # 外國政治人物
    '墨索里尼',
}

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

TARGETS = [
    ('dayiunip.db',             'dayi',     'dayiunip.zip'),
    ('phoneticcomplete.db',     'phonetic', 'phoneticcomplete.zip'),
    ('phoneticcompletebig5.db', 'phonetic', 'phoneticcompletebig5.zip'),
]


def clean_related(related_value: str, terms: set) -> str | None:
    if not related_value:
        return related_value
    parts = [p for p in related_value.split('|') if p not in terms]
    return '|'.join(parts) if parts else None


def process_db(db_filename: str, table: str) -> tuple[int, int]:
    db_path = os.path.join(SCRIPT_DIR, db_filename)
    conn = sqlite3.connect(db_path)
    c = conn.cursor()

    # --- DELETE word entries ---
    placeholders = ','.join('?' * len(REMOVE_TERMS))
    terms_list = list(REMOVE_TERMS)
    c.execute(f'DELETE FROM [{table}] WHERE word IN ({placeholders})', terms_list)
    deleted_words = c.rowcount

    # --- UPDATE related fields ---
    conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in REMOVE_TERMS])
    c.execute(
        f'SELECT _id, related FROM [{table}] WHERE related IS NOT NULL AND ({conditions})',
        terms_list,
    )
    rows = c.fetchall()

    updated_related = 0
    for row_id, related_val in rows:
        new_related = clean_related(related_val, REMOVE_TERMS)
        if new_related != related_val:
            c.execute(f'UPDATE [{table}] SET related = ? WHERE _id = ?', (new_related, row_id))
            updated_related += 1

    conn.commit()
    conn.execute('VACUUM')
    conn.close()
    return deleted_words, updated_related


def repack_zip(db_filename: str, zip_filename: str) -> None:
    db_path  = os.path.join(SCRIPT_DIR, db_filename)
    zip_path = os.path.join(SCRIPT_DIR, zip_filename)
    with zipfile.ZipFile(zip_path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(db_path, arcname=db_filename)


def verify(db_filename: str, table: str) -> int:
    db_path = os.path.join(SCRIPT_DIR, db_filename)
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    placeholders = ','.join('?' * len(REMOVE_TERMS))
    c.execute(f'SELECT COUNT(*) FROM [{table}] WHERE word IN ({placeholders})', list(REMOVE_TERMS))
    remaining = c.fetchone()[0]
    conn.close()
    return remaining


def main() -> None:
    for db_filename, table, zip_filename in TARGETS:
        print(f'\n--- {db_filename} ---')
        deleted, updated = process_db(db_filename, table)
        print(f'  Deleted word entries : {deleted}')
        print(f'  Updated related rows : {updated}')

        remaining = verify(db_filename, table)
        if remaining > 0:
            print(f'  WARNING: {remaining} entries still remain!', file=sys.stderr)
            sys.exit(1)
        else:
            print(f'  Verification        : PASS (0 remaining)')

        print(f'  Repacking {zip_filename} ...')
        repack_zip(db_filename, zip_filename)
        zip_size = os.path.getsize(os.path.join(SCRIPT_DIR, zip_filename))
        print(f'  ZIP size            : {zip_size:,} bytes')

    print('\nDone.')


if __name__ == '__main__':
    main()
