"""
Remove simplified Chinese characters and mainland-specific vocabulary
from WFIME databases. Targets both the word column and related column.
"""
import sqlite3
import zipfile
import os
import sys

SIMP_CHARS = {
    '们', '从', '众', '亿', '华', '马', '广', '东', '属', '万',
    '层', '爱', '实', '写', '乱', '种', '义', '难', '汉', '乐',
    '针', '证', '设', '话', '转', '将', '软', '数', '总',
}

MAINLAND_TERMS = {
    # 政治機構
    '人大', '兩會', '省委', '市委', '縣委', '書記', '政協',
    # 媒體
    '人民日報', '新華社', '電臺',
    # 大陸用語
    '信息', '軟件', '硬件', '計算機中心', '初中', '高考', '普通話',
    '公共汽車', '摩托車', '出租車',
}

ALL_REMOVE = SIMP_CHARS | MAINLAND_TERMS

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

TARGETS = [
    ('dayiunip.db',             'dayi',     'dayiunip.zip'),
    ('phoneticcomplete.db',     'phonetic', 'phoneticcomplete.zip'),
    ('phoneticcompletebig5.db', 'phonetic', 'phoneticcompletebig5.zip'),
]

LOG_FILE = os.path.join(SCRIPT_DIR, 'simp_mainland_results.txt')


def clean_related(related_value: str, terms: set) -> str | None:
    if not related_value:
        return related_value
    parts = [p for p in related_value.split('|') if p not in terms]
    return '|'.join(parts) if parts else None


def process_db(db_filename: str, table: str, log) -> tuple[int, int]:
    db_path = os.path.join(SCRIPT_DIR, db_filename)
    conn = sqlite3.connect(db_path)
    c = conn.cursor()

    # Delete word entries
    placeholders = ','.join(['?' for _ in ALL_REMOVE])
    c.execute(f'DELETE FROM [{table}] WHERE word IN ({placeholders})', list(ALL_REMOVE))
    deleted = c.rowcount
    log.write(f'  Deleted word entries : {deleted}\n')

    # Update related column
    terms_list = list(ALL_REMOVE)
    like_conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in terms_list])
    c.execute(
        f'SELECT _id, related FROM [{table}] WHERE related IS NOT NULL AND ({like_conditions})',
        terms_list,
    )
    rows = c.fetchall()

    updated = 0
    for row_id, related_val in rows:
        parts = set(related_val.split('|'))
        if parts & ALL_REMOVE:
            new_related = clean_related(related_val, ALL_REMOVE)
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

    placeholders = ','.join(['?' for _ in ALL_REMOVE])
    c.execute(f'SELECT COUNT(*) FROM [{table}] WHERE word IN ({placeholders})', list(ALL_REMOVE))
    word_remaining = c.fetchone()[0]

    terms_list = list(ALL_REMOVE)
    like_conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in terms_list])
    c.execute(
        f'SELECT _id, related FROM [{table}] WHERE related IS NOT NULL AND ({like_conditions})',
        terms_list,
    )
    rows = c.fetchall()
    related_remaining = sum(
        1 for _, rv in rows if set(rv.split('|')) & ALL_REMOVE
    )

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
        log.write('=== remove_simp_mainland.py results ===\n\n')

        for db_filename, table, zip_filename in TARGETS:
            log.write(f'--- {db_filename} ---\n')
            deleted, updated = process_db(db_filename, table, log)

            remaining = verify(db_filename, table, log)
            if remaining > 0:
                log.write(f'  WARNING: {remaining} entries still remain!\n')
            else:
                log.write(f'  Verification         : PASS (0 remaining)\n')

            log.write(f'  Repacking {zip_filename} ...\n')
            repack_zip(db_filename, zip_filename, log)
            log.write('\n')

        log.write('Done.\n')

    # Print summary to stdout (ASCII only to avoid encoding issues)
    print('Results written to:', LOG_FILE)
    print('Check simp_mainland_results.txt for details.')


if __name__ == '__main__':
    main()
