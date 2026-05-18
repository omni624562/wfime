"""
Remove person name entries from WFIME input method databases.
Targets: dayiunip.db, phoneticcomplete.db, phoneticcompletebig5.db
"""
import sqlite3
import zipfile
import os
import sys

PERSON_NAMES = {
    # 歷史人物
    '李廣', '李耳', '李白', '白居易', '王勃', '王維', '王安', '柳宗元',
    '司馬遷', '班固', '墨翟', '蘇軾', '范仲淹', '岳飛', '鄭成功',
    # 三國/水滸/紅樓/西遊
    '曹操', '劉備', '劉邦', '呂布', '項羽', '關羽', '武松', '張三',
    '諸葛亮', '豬八戒', '賈寶玉',
    # 近代政治人物
    '習近平', '胡錦濤', '劉少奇', '連戰',
    # 文學/藝術家
    '巴金', '冰心', '魯迅', '瓊瑤', '金庸',
    # 外國人名（音譯）
    '莫札特', '貝多芬', '達爾文', '列寧', '史達林', '邱吉爾',
    # 模糊詞（偏保守，一律移除）
    '李子', '李樹', '李家', '張家',
}

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

TARGETS = [
    ('dayiunip.db',             'dayi',     'dayiunip.zip'),
    ('phoneticcomplete.db',     'phonetic', 'phoneticcomplete.zip'),
    ('phoneticcompletebig5.db', 'phonetic', 'phoneticcompletebig5.zip'),
]


def clean_related(related_value: str, names: set) -> str | None:
    if not related_value:
        return related_value
    parts = [p for p in related_value.split('|') if p not in names]
    return '|'.join(parts) if parts else None


def process_db(db_filename: str, table: str) -> tuple[int, int]:
    db_path = os.path.join(SCRIPT_DIR, db_filename)
    conn = sqlite3.connect(db_path)
    c = conn.cursor()

    # --- DELETE word entries ---
    placeholders = ','.join('?' * len(PERSON_NAMES))
    names_list = list(PERSON_NAMES)
    c.execute(f'DELETE FROM [{table}] WHERE word IN ({placeholders})', names_list)
    deleted_words = c.rowcount

    # --- UPDATE related fields ---
    conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in PERSON_NAMES])
    c.execute(
        f'SELECT _id, related FROM [{table}] WHERE related IS NOT NULL AND ({conditions})',
        names_list,
    )
    rows = c.fetchall()

    updated_related = 0
    for row_id, related_val in rows:
        new_related = clean_related(related_val, PERSON_NAMES)
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
    placeholders = ','.join('?' * len(PERSON_NAMES))
    c.execute(f'SELECT COUNT(*) FROM [{table}] WHERE word IN ({placeholders})', list(PERSON_NAMES))
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
            print(f'  WARNING: {remaining} person name entries still remain!', file=sys.stderr)
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
