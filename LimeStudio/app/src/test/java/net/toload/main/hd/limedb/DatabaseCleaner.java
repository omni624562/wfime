package net.toload.main.hd.limedb;

import org.junit.Test;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * 詞庫清理工具 - 用於移除資料庫中的政治人物姓名或特定敏感詞彙。
 */
public class DatabaseCleaner {

    // 1. 全球重要政治人物黑名單 (包含台、中、美、日、國際等)
    private static final List<String> BLACKLIST = Arrays.asList(
        // 台灣相關
        "蔡英文", "馬英九", "賴清德", "韓國瑜", "柯文哲", "朱立倫", "侯友宜", "陳水扁", "李登輝", "蔣中正", "蔣經國",
        "蘇貞昌", "游錫堃", "陳建仁", "蕭美琴", "吳敦義", "連戰", "宋楚瑜", "林佳龍", "鄭文燦", "郭台銘", "謝長廷",
        // 中國相關
        "習近平", "習大大", "李強", "王滬寧", "蔡奇", "丁薛祥", "李希", "韓正", "胡錦濤", "江澤民", "溫家寶", "朱鎔基", "鄧小平", "毛澤東", "王毅", "秦剛",
        // 美國相關
        "拜登", "拜登", "川普", "川普", "歐巴馬", "歐巴馬", "柯林頓", "柯林頓", "布希", "布希", "裴洛西", "賀錦麗", "希拉蕊", "賀錦麗",
        // 日本、韓國、歐美、其他
        "安倍晉三", "岸田文雄", "石破茂", "菅義偉", "普丁", "普京", "澤倫斯基", "馬克宏", "蕭茲", "莫迪", "金正恩", "文在寅", "尹錫悅", "李顯龍", "黃循財"
    );

    // 2. 政治職稱與敏感前字 - 用於清理聯想詞庫 (例如：刪除包含 "總統" 的短語，或特定的姓氏關聯)
    private static final List<String> TITLES = Arrays.asList(
        "總統", "總理", "主席", "首相", "部長", "委員", "參議員", "眾議員", "州長", "秘書長", "執委會", "政治局", "常委",
        "司長", "發言人", "辦公室"
    );

    // 3. 敏感姓氏/前字 - 用於暴力清理以其開頭的特定長度聯想
    private static final List<String> SENSITIVE_SURNAMES = Arrays.asList(
        "詹", "蔡", "馬", "習", "賴", "韓", "柯", "朱", "侯", "陳", "李", "蔣", "蘇"
    );

    // 3. 指定資料庫目錄
    private static final String DB_DIR = "C:/Storage/workspace/nanime-main/Database/";

    @Test
    public void cleanGlobalPoliticalFigures() {
        File dir = new File(DB_DIR);
        File[] dbFiles = dir.listFiles((d, name) -> name.endsWith(".db"));

        if (dbFiles == null || dbFiles.length == 0) {
            System.out.println("找不到任何 .db 檔案，請檢查路徑：" + DB_DIR);
            return;
        }

        for (File dbFile : dbFiles) {
            processDatabase(dbFile);
        }
    }

    private void processDatabase(File dbFile) {
        String dbName = dbFile.getName().replace(".db", "");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        System.out.println("正在處理資料庫: " + dbFile.getName() + "...");

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);

            // A. 處理精準姓名刪除 (主表與聯想表)
            for (String name : BLACKLIST) {
                try {
                    stmt.execute("DELETE FROM " + dbName + " WHERE word = '" + name + "'");
                    stmt.execute("DELETE FROM related WHERE pword = '" + name + "' OR cword = '" + name + "'");
                    stmt.execute("DELETE FROM related WHERE (pword || cword) = '" + name + "'");
                } catch (Exception ignored) {}
            }

            // B. 處理職稱聯想刪除 (刪除 cword 包含職稱的聯想，例如 "我 總統" -> 刪除)
            for (String title : TITLES) {
                try {
                    stmt.execute("DELETE FROM related WHERE cword LIKE '%" + title + "%'");
                } catch (Exception ignored) {}
            }

            // C. 處理特定姓氏引發的名字聯想 (例如：打 "詹" 出現 "宏志")
            for (String surname : SENSITIVE_SURNAMES) {
                try {
                    // 刪除 pword 為該姓氏，且 cword 長度為 2 (通常是名字) 的情況
                    // 在 SQLite 中，length() 計算字元數
                    stmt.execute("DELETE FROM related WHERE pword = '" + surname + "' AND length(cword) >= 2");
                } catch (Exception ignored) {}
            }

            // D. 針對性清理特定案例 (如您提到的 姆諅布瑞吉 等長名字)
            List<String> specificRelated = Arrays.asList("宏志", "姆諅布瑞吉", "姆士", "近平", "大大");
            for (String part : specificRelated) {
                try {
                    stmt.execute("DELETE FROM related WHERE cword = '" + part + "'");
                } catch (Exception ignored) {}
            }

            conn.commit();
            conn.setAutoCommit(true); // VACUUM must be outside a transaction
            stmt.execute("VACUUM");
            System.out.println("成功清理 " + dbFile.getName());

        } catch (Exception e) {
            System.err.println("處理 " + dbFile.getName() + " 時發生錯誤: " + e.getMessage());
        }
    }
}
