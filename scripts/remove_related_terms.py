"""
Remove person names and sensitive terms from the 'related' column of WFIME databases.
These terms appear as pipe-separated candidate suggestions; we strip them from the lists.
"""
import sqlite3
import zipfile
import os
import sys

REMOVE_FROM_RELATED = {
    # === 人名 ===
    # 中國歷史人物
    '劉徹', '劉秀', '秦始皇', '秦檜', '秦穆公', '趙匡胤', '郭子儀', '魏徵',
    '夏禹', '夏桀', '孟嘗君', '孟姜女', '阮籍', '吳起', '吳鳳', '梁山伯',
    '鍾馗', '馮婦', '虞姬', '陳勝', '田單', '張先', '徐福', '白樸', '柳永',
    '溫庭筠', '屈原', '胡適之', '鄭和', '鄭紀倫',
    # 清朝皇帝
    '乾隆', '康熙', '雍正', '武則天',
    # 近代台灣/中國政治人物
    '魏京生', '錢其琛', '黃昆輝', '趙少康', '趙建銘', '陳履安', '陳誠',
    '錢復', '李遠哲',
    # 藝人
    '鄧麗君', '周慧敏',
    # 外國歷史/文化人物
    '盧梭', '孟德斯鳩', '但丁', '佛洛伊德', '托爾斯泰', '雨果', '戈巴契夫',
    '胡佛', '柯林頓', '史瓦辛格', '戴安娜', '馬勒',
    # 宗教/神話人物
    '如來', '觀音', '夏娃',
    # === 敏感詞 ===
    # 政治
    '台獨', '共產黨', '國民黨', '民進黨', '達賴', '馬列主義',
    # WWII意識形態
    '納粹', '法西斯',
    # 歷史政治事件/現象
    '白色恐怖', '黃禍',
    # 不雅/色情
    '黃色小說', '黃色故事', '黃色文學', '王八羔子',
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


def process_db(db_filename: str, table: str) -> int:
    db_path = os.path.join(SCRIPT_DIR, db_filename)
    conn = sqlite3.connect(db_path)
    c = conn.cursor()

    # Build LIKE conditions for initial filtering (performance)
    like_conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in REMOVE_FROM_RELATED])
    terms_list = list(REMOVE_FROM_RELATED)

    c.execute(
        f'SELECT _id, related FROM [{table}] WHERE related IS NOT NULL AND ({like_conditions})',
        terms_list,
    )
    rows = c.fetchall()

    updated = 0
    for row_id, related_val in rows:
        # Verify actual token match (not just substring)
        parts = set(related_val.split('|'))
        if parts & REMOVE_FROM_RELATED:
            new_related = clean_related(related_val, REMOVE_FROM_RELATED)
            if new_related != related_val:
                c.execute(f'UPDATE [{table}] SET related = ? WHERE _id = ?', (new_related, row_id))
                updated += 1

    conn.commit()
    conn.execute('VACUUM')
    conn.close()
    return updated


def repack_zip(db_filename: str, zip_filename: str) -> None:
    db_path  = os.path.join(SCRIPT_DIR, db_filename)
    zip_path = os.path.join(SCRIPT_DIR, zip_filename)
    with zipfile.ZipFile(zip_path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        zf.write(db_path, arcname=db_filename)


def verify(db_filename: str, table: str) -> int:
    db_path = os.path.join(SCRIPT_DIR, db_filename)
    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    terms_list = list(REMOVE_FROM_RELATED)
    like_conditions = ' OR '.join([f"related LIKE '%' || ? || '%'" for _ in terms_list])
    c.execute(
        f'SELECT _id, related FROM [{table}] WHERE related IS NOT NULL AND ({like_conditions})',
        terms_list,
    )
    rows = c.fetchall()
    remaining = 0
    for _, related_val in rows:
        if set(related_val.split('|')) & REMOVE_FROM_RELATED:
            remaining += 1
    conn.close()
    return remaining


def main() -> None:
    for db_filename, table, zip_filename in TARGETS:
        print(f'\n--- {db_filename} ---')
        updated = process_db(db_filename, table)
        print(f'  Updated related rows : {updated}')

        remaining = verify(db_filename, table)
        if remaining > 0:
            print(f'  WARNING: {remaining} rows still contain remove terms!', file=sys.stderr)
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
