/*
 *
 *  *
 *  **    Copyright 2015, The LimeIME Open Source Project
 *  **
 *  **    Project Url: http://github.com/lime-ime/limeime/
 *  **                 http://android.toload.net/
 *  **
 *  **    This program is free software: you can redistribute it and/or modify
 *  **    it under the terms of the GNU General Public License as published by
 *  **    the Free Software Foundation, either version 3 of the License, or
 *  **    (at your option) any later version.
 *  *
 *  **    This program is distributed in the hope that it will be useful,
 *  **    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  **    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  **    GNU General Public License for more details.
 *  *
 *  **    You should have received a copy of the GNU General Public License
 *  **    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *
 */

package net.toload.main.hd.limedb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Looper;

import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import net.toload.main.hd.Lime;
import net.toload.main.hd.R;
import net.toload.main.hd.data.ChineseSymbol;
import net.toload.main.hd.data.Im;
import net.toload.main.hd.data.ImObj;
import net.toload.main.hd.data.Keyboard;
import net.toload.main.hd.data.KeyboardObj;
import net.toload.main.hd.data.Mapping;
import net.toload.main.hd.data.Related;
import net.toload.main.hd.data.Word;
import net.toload.main.hd.global.LIME;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.global.LIMEProgressListener;
import net.toload.main.hd.global.LIMEUtilities;

public class LimeDB extends LimeSQLiteOpenHelper {

    public final static String FIELD_ID = "_id";
    public final static String FIELD_CODE = "code";
    public final static String FIELD_WORD = "word";
    public final static String FIELD_RELATED = Lime.DB_RELATED;

    // Jeremy '15, 6, 1 between search clause without using related column for
    // better sorting order.
    public final static String FIELD_SCORE = "score";
    public final static String FIELD_BASESCORE = "basescore"; // jeremy '11,9,8 base frequency got from han converter
                                                              // when table loading.
    public final static String FIELD_NO_TONE_CODE = "code3r";
    public final static String FIELD_DIC_id = "_id";
    public final static String FIELD_DIC_pcode = "pcode";
    public final static String FIELD_DIC_pword = "pword";
    public final static String FIELD_DIC_ccode = "ccode";
    public final static String FIELD_DIC_cword = "cword";
    public final static String FIELD_DIC_score = "score";
    public final static String FIELD_DIC_is = "isDictionary";
    public static final String DB_MEMO = "memo";
    public static final String DB_MEMO_COLUMN_ID = "_id";
    public static final String DB_MEMO_COLUMN_CONTENT = "content";
    public static final String DB_MEMO_COLUMN_PINNED = "pinned";
    public static final String DB_MEMO_COLUMN_CREATED_AT = "created_at";
    static final boolean DEBUG = false;
    static final String TAG = "LIMEDB";
    private final static int DATABASE_VERSION = 103;
    // Jeremy '11,8,5
    // TODO: should set INITIAL_RESULT_LIMIT according to screen size.
    final static String INITIAL_RESULT_LIMIT = "15";
    final static String FINAL_RESULT_LIMIT = "210";
    private final static int INITIAL_RELATED_LIMIT = 5;
    final static int COMPOSING_CODE_LENGTH_LIMIT = 16; // Jeremy '12,5,30 changed from 12 to 16 because of
                                                               // improved performance using binary tree.
    final static int DUALCODE_COMPOSING_LIMIT = 16; // Jeremy '12,5,30 changed from 7 to 16 because of improved
                                                            // performance using binary tree.
    final static int DUALCODE_NO_CHECK_LIMIT = 2; // Jeremy '12,5,30 changed from 5 to 3 for phonetic correct
                                                          // valid code display.
    private final static int BETWEEN_SEARCH_WAY_BACK_LEVELS = 5; // Jeremy '15,6,30
    // for keyToChar
    final static String DAYI_KEY = "1234567890qwertyuiopasdfghjkl;zxcvbnm,./";
    final static String DAYI_CHAR = "言|牛|目|四|王|門|田|米|足|金|石|山|一|工|糸|火|艸|木|口|耳|人|革|日|土|手|鳥|月|立|女|虫|心|水|鹿|禾|馬|魚|雨|力|舟|竹";
    final static String BPMF_KEY = "1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik,9ol.0p;/-";
    final static String BPMF_CHAR = "ㄅ|ㄆ|ㄇ|ㄈ|ㄉ|ㄊ|ㄋ|ㄌ|ˇ|ㄍ|ㄎ|ㄏ|ˋ|ㄐ|ㄑ|ㄒ|ㄓ|ㄔ|ㄕ|ㄖ|ˊ|ㄗ|ㄘ|ㄙ|˙|ㄧ|ㄨ|ㄩ|ㄚ|ㄛ|ㄜ|ㄝ|ㄞ|ㄟ|ㄠ|ㄡ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ";
    final static String SHIFTED_NUMBERIC_KEY = "!@#$%^&*()";
    final static String SHIFTED_NUMBERIC_KEY_REMAP = "1234567890";
    final static String SHIFTED_SYMBOL_KEY = "<>?_:+\"";
    final static String SHIFTED_SYMBOL_KEY_REMAP = ",./-;='";
    final static String ETEN_KEY = "abcdefghijklmnopqrstuvwxyz12347890-=;',./!@#$&*()<>?_+:\"";
    final static String ETEN_KEY_REMAP = "81v2uzrc9bdxasiqoknwme,j.l7634f0p;/-yh5tg7634f0p;5tg/yh-";
    final static String ETEN_CHAR = "ㄚ|ㄅ|ㄒ|ㄉ|ㄧ|ㄈ|ㄐ|ㄏ|ㄞ|ㄖ|ㄎ|ㄌ|ㄇ|ㄋ|ㄛ|ㄆ|ㄟ|ㄜ|ㄙ|ㄊ|ㄩ|ㄍ|ㄝ|ㄨ|ㄡ|ㄠ" +
            "|˙|ˊ|ˇ|ˋ|ㄑ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ|ㄗ|ㄘ|ㄓ|ㄔ|ㄕ|˙|ˊ|ˇ|ˋ|ㄑ|ㄢ|ㄣ|ㄤ|ㄓ|ㄔ|ㄕ|ㄥ|ㄦ|ㄗ|ㄘ";
    final static String ETEN26_KEY = "qazwsxedcrfvtgbyhnujmikolp,.";
    final static String ETEN26_KEY_REMAP_INITIAL = "y8lhnju2vkzewr1tcsmba9dixq<>";
    final static String ETEN26_KEY_REMAP_FINAL = "y8lhnju7vk6ewr1tcsm3a94ixq<>";
    final static String ETEN26_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String ETEN26_DUALKEY = "yhvewrscpaxqs3467";
    final static String ETEN26_CHAR_INITIAL = "(ㄗ/ㄟ)|ㄚ|ㄠ|(ㄘ/ㄝ)|ㄙ|ㄨ|ㄧ|ㄉ|(ㄕ/ㄒ)|ㄜ|ㄈ|(ㄍ/ㄑ)|(ㄊ/ㄤ)|(ㄐ/ㄓ)|ㄅ|ㄔ|(ㄏ/ㄦ)|(ㄋ/ㄣ)|ㄩ|ㄖ|(ㄇ/ㄢ)|ㄞ|ㄎ|ㄛ|(ㄌ/ㄥ)|(ㄆ/ㄡ)|，|。";
    final static String ETEN26_CHAR_FINAL = "(ㄗ/ㄟ)|ㄚ|ㄠ|(ㄘ/ㄝ)|ㄙ|ㄨ|ㄧ|˙|(ㄕ/ㄒ)|ㄜ|ˊ|(ㄍ/ㄑ)|(ㄊ/ㄤ)|(ㄐ/ㄓ)|ㄅ|ㄔ|(ㄏ/ㄦ)|(ㄋ/ㄣ)|ㄩ|ˇ|(ㄇ/ㄢ)|ㄞ|ˋ|ㄛ|(ㄌ/ㄥ)|(ㄆ/ㄡ)|，|。";
    final static String MILESTONE3_DAYI_CHAR = "言|石|人|心|牛|山|革|水|目|一|日|鹿|四|工|土|禾|王|糸|手|馬|門|火|鳥|魚|田|" +
            "艸|月|雨|米|木|立|(力/虫)|足|口|女|舟|金|耳|竹";
    final static String MILESTONE3_BPMF_CHAR = "ㄅ|ㄆ|ㄇ|ㄈ|ㄉ|ㄊ|ㄋ|ㄌ|ˇ|ㄍ|ㄎ|ㄏ|ˋ|ㄐ|ㄑ|ㄒ|ㄓ|ㄔ|ㄕ|ㄖ|ˊ|ㄗ|ㄘ|ㄙ|˙|" +
            "ㄧ|ㄨ|ㄩ|ㄚ|ㄛ|ㄜ|ㄝ|ㄞ|ㄟ|(ㄠ/ㄤ)|(ㄡ/ㄥ)|ㄢ|ㄣ|ㄥ";

    private final static String CJ_KEY = "abcdefghijklmnopqrstuvwxyz";
    private final static String CJ_CHAR = "日|月|金|木|水|火|土|竹|戈|十|大|中|一|弓|人|心|手|口|尸|廿|山|女|田|難|卜";
    final static String MILESTONE_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm,./";
    final static String MILESTONE_BPMF_CHAR = "ㄅ|ㄉ|ˇ|ˋ|ㄓ|ˊ|˙|ㄚ|ㄞ|ㄢ|ㄦ|ㄆ|ㄊ|ㄍ|ㄐ|ㄔ|ㄗ|ㄧ|ㄛ|ㄟ|ㄣ|ㄇ|ㄋ|ㄎ|ㄑ|ㄕ|ㄘ|ㄨ|ㄜ|ㄠ|ㄤ|ㄈ|ㄌ|ㄏ|ㄒ|ㄖ|ㄙ|ㄩ|ㄝ|ㄡ|ㄥ";
    final static String MILESTONE_DAYI_CHAR = "言|牛|目|四|王|車|田|八|足|金|一|工|糸|火|舟|竹|戈|十|大|中|水|手|鳥|月|立|女|虫|心|鹿|禾|馬|魚|雨|力|口|日|石|人|革";
    final static String MILESTONE_ETEN_CHAR = "ㄚ|ㄅ|ㄒ|ㄉ|ㄧ|ㄈ|ㄐ|ㄏ|ㄞ|ㄖ|ㄎ|ㄌ|ㄇ|ㄋ|ㄛ|ㄆ|ㄟ|ㄜ|ㄙ|ㄊ|ㄩ|ㄍ|ㄝ|ㄨ|ㄡ|ㄠ|˙|ˊ|ˇ|ˋ|ㄑ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ|ㄗ|ㄘ|ㄓ|ㄔ|ㄕ";
    final static String MILESTONE_DUALKEY = "yhvewrscpaxq3467";
    final static String MILESTONE_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz";
    final static String MILESTONE_ETEN_DUALKEY = "yhvewrscpaxqs3467";
    final static String MILESTONE_ETEN_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String MILESTONE2_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm,./";
    final static String MILESTONE2_BPMF_CHAR = "ㄅ|ㄉ|ˇ|ˋ|ㄓ|ˊ|˙|ㄚ|ㄞ|ㄢ|ㄦ|ㄆ|ㄊ|ㄍ|ㄐ|ㄔ|ㄗ|ㄧ|ㄛ|ㄟ|ㄣ|ㄇ|ㄋ|ㄎ|ㄑ|ㄕ|ㄘ|ㄨ|ㄜ|ㄠ|ㄤ|ㄈ|ㄌ|ㄏ|ㄒ|ㄖ|ㄙ|ㄩ|ㄝ|ㄡ|ㄥ";
    final static String MILESTONE2_ETEN_CHAR = "ㄚ|ㄅ|ㄒ|ㄉ|ㄧ|ㄈ|ㄐ|ㄏ|ㄞ|ㄖ|ㄎ|ㄌ|ㄇ|ㄋ|ㄛ|ㄆ|ㄟ|ㄜ|ㄙ|ㄊ|ㄩ|ㄍ|ㄝ|ㄨ|ㄡ|ㄠ|˙|ˊ|ˇ|ˋ|ㄑ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ|ㄗ|ㄘ|ㄓ|ㄔ|ㄕ";
    final static String MILESTONE2_DUALKEY = "yhvewrscpaxq3467";
    final static String MILESTONE2_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz";
    final static String MILESTONE2_ETEN_DUALKEY = "yhvewrscpaxqs3467";
    final static String MILESTONE2_ETEN_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String MILESTONE3_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm,./";
    final static String MILESTONE3_ETEN_CHAR = "ㄚ|ㄅ|ㄒ|ㄉ|ㄧ|ㄈ|ㄐ|ㄏ|ㄞ|ㄖ|ㄎ|ㄌ|ㄇ|ㄋ|ㄛ|ㄆ|ㄟ|ㄜ|ㄙ|ㄊ|ㄩ|ㄍ|ㄝ|ㄨ|ㄡ|ㄠ|˙|ˊ|ˇ|ˋ|ㄑ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ|ㄗ|ㄘ|ㄓ|ㄔ|ㄕ";
    final static String MILESTONE3_DUALKEY = "yhvewrscpaxq3467";
    final static String MILESTONE3_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz";
    final static String MILESTONE3_ETEN_DUALKEY = "yhvewrscpaxqs3467";
    final static String MILESTONE3_ETEN_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String MILESTONE3_BPMF_DUALKEY = "yhvewrscpaxqs3467";
    final static String MILESTONE3_BPMF_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String DESIREZ_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm,.";
    final static String DESIREZ_BPMF_CHAR = "ㄅ|ㄉ|ˇ|ˋ|ㄓ|ˊ|˙|ㄚ|ㄞ|ㄢ|ㄆ|ㄊ|ㄍ|ㄐ|ㄔ|ㄗ|ㄧ|ㄛ|ㄟ|ㄣ|ㄇ|ㄋ|ㄎ|ㄑ|ㄕ|ㄘ|ㄨ|ㄜ|ㄠ|ㄤ|ㄈ|ㄌ|ㄏ|ㄒ|ㄖ|ㄙ|ㄩ|ㄝ|ㄡ|ㄥ";
    final static String DESIREZ_DAYI_CHAR = "言|牛|目|四|王|車|田|八|足|金|一|工|糸|火|舟|竹|戈|十|大|中|水|手|鳥|月|立|女|虫|心|鹿|禾|馬|魚|雨|力|口|日|石|人|革";
    final static String DESIREZ_ETEN_CHAR = "ㄚ|ㄅ|ㄒ|ㄉ|ㄧ|ㄈ|ㄐ|ㄏ|ㄞ|ㄖ|ㄎ|ㄌ|ㄇ|ㄋ|ㄛ|ㄆ|ㄟ|ㄜ|ㄙ|ㄊ|ㄩ|ㄍ|ㄝ|ㄨ|ㄡ|ㄠ|˙|ˊ|ˇ|ˋ|ㄑ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ|ㄗ|ㄘ|ㄓ|ㄔ|ㄕ";
    final static String DESIREZ_BPMF_KEY_REMAP = "1234567890qwertyuiopasdfghjklzxcvbnm,.";
    final static String DESIREZ_DUALKEY = "yhvewrscpaxq3467";
    final static String DESIREZ_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz";
    final static String DESIREZ_ETEN_DUALKEY = "yhvewrscpaxqs3467";
    final static String DESIREZ_ETEN_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String DESIREZ_BPMF_DUALKEY = "yhvewrscpaxqs3467";
    final static String DESIREZ_BPMF_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String CHACHA_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm,.";
    final static String CHACHA_BPMF_CHAR = "ㄅ|ㄉ|ˇ|ˋ|ㄓ|ˊ|˙|ㄚ|ㄞ|ㄢ|ㄆ|ㄊ|ㄍ|ㄐ|ㄔ|ㄗ|ㄧ|ㄛ|ㄟ|ㄣ|ㄇ|ㄋ|ㄎ|ㄑ|ㄕ|ㄘ|ㄨ|ㄜ|ㄠ|ㄤ|ㄈ|ㄌ|ㄏ|ㄒ|ㄖ|ㄙ|ㄩ|ㄝ|ㄡ|ㄥ";
    final static String CHACHA_BPMF_KEY_REMAP = "1234567890qwertyuiopasdfghjklzxcvbnm,.";
    final static String CHACHA_DUALKEY = "yhvewrscpaxq3467";
    final static String CHACHA_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz";
    final static String CHACHA_ETEN_DUALKEY = "yhvewrscpaxqs3467";
    final static String CHACHA_ETEN_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String CHACHA_BPMF_DUALKEY = "yhvewrscpaxqs3467";
    final static String CHACHA_BPMF_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String XPERIAPRO_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm,.";
    final static String XPERIAPRO_BPMF_KEY_REMAP = "1234567890qwertyuiopasdfghjklzxcvbnm,.";
    final static String XPERIAPRO_DUALKEY = "yhvewrscpaxq3467";
    final static String XPERIAPRO_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz";
    final static String XPERIAPRO_ETEN_DUALKEY = "yhvewrscpaxqs3467";
    final static String XPERIAPRO_ETEN_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    final static String HSU_KEY = "1qaz2wsx3edc4rfv5tgb6yhn7ujm8ik,9ol.0p;/-";
    final static String HSU_CHAR_INITIAL = "ㄅ|ㄆ|ㄇ|ㄈ|ㄉ|ㄊ|ㄋ|ㄌ|ˇ|ㄍ|ㄎ|ㄏ|ˋ|ㄐ|ㄑ|ㄒ|ㄓ|ㄔ|ㄕ|ㄖ|ˊ|ㄗ|ㄘ|ㄙ|˙|ㄧ|ㄨ|ㄩ|ㄚ|ㄛ|ㄜ|ㄝ|ㄞ|ㄟ|ㄠ|ㄡ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ";
    final static String HSU_CHAR_FINAL = "ㄅ|ㄆ|ㄇ|ㄈ|ㄉ|ㄊ|ㄋ|ㄌ|ˇ|ㄍ|ㄎ|ㄏ|ˋ|ㄐ|ㄑ|ㄒ|ㄓ|ㄔ|ㄕ|ㄖ|ˊ|ㄗ|ㄘ|ㄙ|˙|ㄧ|ㄨ|ㄩ|ㄚ|ㄛ|ㄜ|ㄝ|ㄞ|ㄟ|ㄠ|ㄡ|ㄢ|ㄣ|ㄤ|ㄥ|ㄦ";
    final static String HSU_KEY_REMAP_INITIAL = "y8lhnju2vkzewr1tcsmba9dixq<>";
    final static String HSU_KEY_REMAP_FINAL = "y8lhnju7vk6ewr1tcsm3a94ixq<>";
    final static String HSU_DUALKEY = "yhvewrscpaxqs3467";
    final static String HSU_DUALKEY_REMAP = "o,gf;5p-s0/.pbdz2";
    private final static String ARRAY_KEY = "1234567890qwertyuiopasdfghjkl;zxcvbnm,./";
    private final static String ARRAY_CHAR = "1-|2-|3-|4-|5-|6-|7-|8-|9-|0-|1⇡|2⇡|3⇡|4⇡|5⇡|6⇡|7⇡|8⇡|9⇡|0⇡|1⇣|2⇣|3⇣|4⇣|5⇣|6⇣|7⇣|8⇣|9⇣|0⇣|？|＊|．|，|。";

    // ==================== Security: Table Name Validation ====================
    // SQL Injection Prevention: Whitelist of valid table names
    // See SECURITY_ANALYSIS.md for detailed security analysis
    private static final java.util.Set<String> VALID_TABLE_NAMES = new java.util.HashSet<>(java.util.Arrays.asList(
        // Main IM tables
        Lime.DB_TABLE_CUSTOM,
        Lime.DB_TABLE_DAYI,
        Lime.DB_TABLE_PHONETIC,
        Lime.IM_PHONETIC_BIG5,
        Lime.IM_PHONETIC_ADV,
        Lime.IM_PHONETIC_ADV_BIG5,
        // System tables
        Lime.DB_IM,
        Lime.DB_RELATED,
        Lime.DB_KEYBOARD,
        "memo",
        // Backup tables (with _user suffix)
        "custom_user", "dayi_user", "phonetic_user"
    ));

    /**
     * Validates table name to prevent SQL injection attacks.
     *
     * Security: This method provides critical SQL injection protection by validating
     * table names against a whitelist before use in SQL queries.
     *
     * @param tableName The table name to validate
     * @return true if the table name is valid and safe to use
     * @throws IllegalArgumentException if the table name is invalid
     */
    private static boolean isValidTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }

        String cleanedName = tableName.trim().toLowerCase(Locale.ROOT);

        // First check whitelist
        if (VALID_TABLE_NAMES.contains(cleanedName)) {
            return true;
        }

        // Allow pattern: valid_name_user (user backup tables)
        if (cleanedName.endsWith("_user")) {
            String baseName = cleanedName.substring(0, cleanedName.length() - 5);
            if (VALID_TABLE_NAMES.contains(baseName)) {
                return true;
            }
        }

        // Additional pattern validation for safety
        // Allow only alphanumeric and underscores, max 64 chars
        if (!cleanedName.matches("^[a-z][a-z0-9_]{0,63}$")) {
            throw new IllegalArgumentException(
                "Invalid table name format: " + tableName +
                " (must be alphanumeric with underscores, max 64 chars)"
            );
        }

        // If pattern is valid but not in whitelist, log warning but allow
        // This allows for future table additions while maintaining security
        try {
            Log.w(TAG, "Table name not in whitelist but matches pattern: " + tableName);
        } catch (RuntimeException ignored) {
            // Some JVM unit test environments do not mock android.util.Log.
        }
        return true;
    }

    /**
     * Validates and sanitizes table name for use in SQL queries.
     *
     * @param tableName The table name to validate
     * @return The validated table name (trimmed and lowercased)
     * @throws IllegalArgumentException if the table name is invalid
     */
    private static String validateTableName(String tableName) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        return tableName.trim().toLowerCase(Locale.ROOT);
    }

    static final boolean probePerformance = false;
    static SQLiteDatabase db = null; // Jeremy '12,5,1 add static modifier. Shared db instance for dbserver and
                                             // searchserver
    // private final static Boolean fuzzySearch = false;
    // hold database connection when database is in maintainable. Jeremy '15,5,23
    private static boolean databaseOnHold = false;
    static boolean codeDualMapped = false;
    /**
     * Black list cache stored code without valid return. Jeremy '12,6,3
     */
    static java.util.Map<String, Boolean> blackListCache = null;
    final LIMEPreferenceManager mLIMEPref;
    private final Context mContext;
    // Cache for Related Score
    private final HashMap<String, Integer> relatedscore = new HashMap<>();
    private File filename = null;
    String tablename = "custom";
    private int count = 0;
    // Jeremy '15,5,23 for new progress listener progress status update
    private int progressPercentageDone = 0;
    private String progressStatus;
    // private Map<String, String> codeDualMap = new HashMap<String, String>();
    // private int ncount = 0;
    private boolean finish = false;
    // private boolean relatedfinish = false;
    // Db loading loadingMappingThread.
    private Thread loadingMappingThread = null;
    private boolean threadAborted = false;
    // Han and Emoji Databases
    private LimeHanConverter hanConverter;
    private EmojiConverter emojiConverter;
    // 重構二期:打字熱路徑查詢引擎,見 LimeQueryEngine
    private final LimeQueryEngine queryEngine = new LimeQueryEngine(this);

    /*
     * Initialize LIME database, Context and LIMEPreferenceManager
     */
    public LimeDB(Context context) {

        super(context.getApplicationContext(), LIME.DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context.getApplicationContext();

        mLIMEPref = new LIMEPreferenceManager(mContext);

        // Bounded LRU — invalid codes accumulate forever on an unbounded map
        blackListCache = java.util.Collections.synchronizedMap(
                new java.util.LinkedHashMap<String, Boolean>(LIME.LIMEDB_CACHE_SIZE, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(java.util.Map.Entry<String, Boolean> eldest) {
                        return size() > LIME.LIMEDB_CACHE_SIZE;
                    }
                });

        // Jeremy '12,4,7 open DB connection in constructor
        openDBConnection(true);

    }

    public static boolean isCodeDualMapped() {
        return codeDualMapped;
    }

    public void setFinish(boolean value) {
        this.finish = value;
    }

    /*
     * For DBService to set the filename to be load to database
     */
    public void setFilename(File filename) {
        this.filename = filename;
    }

    /*
     * 
     * private void checkLengthColumn(String table){
     * if (!getImInfo(table, "lengthcolumns").equals("present")) {
     * if (!checkDBConnection()) {
     * checkCodeColumnPending = true;
     * return;
     * }
     * Toast.makeText(mContext, mContext.getText(R.string.l3_database_upgrade),
     * Toast.LENGTH_SHORT).show();
     * 
     * 
     * Log.i(TAG,
     * "checkLengthColumn(); create code length columns and index on table:" +
     * table);
     * long startTime = System.currentTimeMillis();
     * holdDBConnection();
     * db.execSQL("alter table " + table + " add 'codelen'");
     * db.execSQL("alter table " + table + " add 'wordlen'");
     * db.execSQL("create index " + table + "_idx_code_len on " + table +
     * " (codelen)");
     * db.execSQL("create index " + table + "_idx_word_len on " + table +
     * " (wordlen)");
     * if (table.equals("phonetic")) {
     * db.execSQL("alter table " + table + " add 'code3rlen'");
     * db.execSQL("create index " + table + "_idx_code3r_len on " + table +
     * " (code3rlen)");
     * db.execSQL("update " + table +
     * " set codelen=length(code), code3rlen=length(code3r), wordlen=length(word)");
     * } else {
     * db.execSQL("update " + table +
     * " set codelen=length(code), wordlen=length(word)");
     * }
     * Log.i(TAG,
     * "checkLengthColumn() create code length columns and index on table:" + table
     * + ". Elapsed time = " + (System.currentTimeMillis() - startTime));
     * unHoldDBConnection();
     * setImInfo(table, "lengthcolumns", "present");
     * }
     * 
     * }
     */
    public String getTablename() {
        return this.tablename;
    }

    /**
     * Create SQLite Database and create related tables
     */
    // Jeremy'12,4,7 on OnCreate now. db is always preloaded.
    // @Override
    // public void onCreate(SQLiteDatabase dbin) {
    // Start from 3.0v no need to create internal database
    // }

    /*
     * Update Database Schema
     *
     * @see
     * android.database.sqlite.SQLit eOpenHelper#onUpgrade(android.database.sqlite
     * .SQLiteDatabase, int, int)
     */

    /*
     * For LIMEService to setup tablename for further word mapping query
     */
    public void setTablename(String tablename) {
        this.tablename = tablename;
        // checkLengthColumn(tablename);
        if (DEBUG) {
            Log.i(TAG, "settTableName(), tablename:" + tablename + " this.tablename:"
                    + this.tablename);
        }
    }

    /**
     * Jeremy '15,6,6 left only oldVersion <80
     * Do upgrade here if db version is not up to date.
     */
    @Override
    public void onUpgrade(SQLiteDatabase dbin, int oldVersion, int newVersion) {

        Log.i(TAG, "OnUpgrade() db old version = " + oldVersion + ", new version = " + newVersion);

        if (oldVersion < 102) {
            // Add code column index on main IM tables for faster prefix range queries
            String[] imTables = {"phonetic", "custom", "dayi"};
            for (String tbl : imTables) {
                try {
                    execSQL(dbin, "CREATE INDEX IF NOT EXISTS idx_" + tbl + "_code ON " + tbl + " (code)");
                } catch (Exception ignored) {
                }
            }
        }

        if (oldVersion < 103) {
            // Add word column index so addScore() UPDATE ... WHERE word = ? doesn't
            // full-scan the IM table while holding the LimeDB monitor
            String[] imTables = {"phonetic", "custom", "dayi"};
            for (String tbl : imTables) {
                try {
                    execSQL(dbin, "CREATE INDEX IF NOT EXISTS idx_" + tbl + "_word ON " + tbl + " (word)");
                } catch (Exception ignored) {
                }
            }
        }

        if (oldVersion < 101) {
            long startTime = System.currentTimeMillis();
            // create index on related (cword) for better perfomance when making run-time
            // suggestion checking related phrases. Jeremy '15,7,17

            try {
                String CREATE_INDEX = "CREATE INDEX related_idx_cword "
                        + "on " + Lime.DB_RELATED + " (" + Lime.DB_RELATED_COLUMN_CWORD + "); ";

                execSQL(dbin, CREATE_INDEX);
            } catch (Exception ignored) {
            }

            if (oldVersion < 100) {

                Cursor cursor = dbin.query("sqlite_master", null, "type='index' and name = 'phonetic_idx_code3r'",
                        null, null, null, null);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        Log.i(TAG, "OnUpgrade(), NoToneCodeI index is exist!!");
                    } else {
                        Log.i(TAG, "OnUpgrade()  creating phonetic code3r column and index.");
                        execSQL(dbin, "alter table phonetic add column 'code3r'");
                        execSQL(dbin, "create index 'phonetic_idx_code3r' on phonetic (code3r)");
                    }
                    cursor.close();
                }
                cursor = dbin.query("phonetic", null, "code3r='ru'", null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        Log.i(TAG, "OnUpgrade(), NoToneCode column has valid data!!");
                    } else {
                        Log.i(TAG, "OnUpgrade()  update phonetic code3r data from trimmed code.");
                        execSQL(dbin, "update phonetic set code3r=trim(code,'3467')");
                    }
                    cursor.close();
                }
                long endTime = System.currentTimeMillis();
                Log.i(TAG,
                        "OnUpgrade() build phonetic code3r finished.  Elapsed time = " + (endTime - startTime) + "ms.");

                // Update Related table
                if (oldVersion > 78) {
                    try {

                        String BACKUP_OLD_RELATED = "ALTER TABLE " + Lime.DB_RELATED + " RENAME TO " + Lime.DB_RELATED
                                + "_old";
                        execSQL(dbin, BACKUP_OLD_RELATED);

                        String CREATE_NEW_TABLE = "";

                        CREATE_NEW_TABLE += "CREATE TABLE " + Lime.DB_RELATED + " ("
                                + Lime.DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + Lime.DB_RELATED_COLUMN_PWORD + " text, "
                                + Lime.DB_RELATED_COLUMN_CWORD + " text, "
                                + Lime.DB_RELATED_COLUMN_BASESCORE + " INTEGER, "
                                + Lime.DB_RELATED_COLUMN_USERSCORE + " INTEGER DEFAULT 0  NOT NULL)";

                        execSQL(dbin, CREATE_NEW_TABLE);

                        try {
                            String CREATE_INDEX = "";
                            CREATE_INDEX += "CREATE INDEX related_idx_pword "
                                    + "ON " + Lime.DB_RELATED + "(" + Lime.DB_RELATED_COLUMN_PWORD + "); ";

                            execSQL(dbin, CREATE_INDEX);
                        } catch (Exception e) {
                            // ignore index creation error
                        }

                        String MIGRATE_DATA = "";
                        MIGRATE_DATA += "INSERT INTO " + Lime.DB_RELATED + "("
                                + Lime.DB_RELATED_COLUMN_PWORD + ", "
                                + Lime.DB_RELATED_COLUMN_CWORD + ", "
                                + Lime.DB_RELATED_COLUMN_USERSCORE + ","
                                + Lime.DB_RELATED_COLUMN_BASESCORE + ")";
                        MIGRATE_DATA += "SELECT " + Lime.DB_RELATED_COLUMN_PWORD + ", "
                                + Lime.DB_RELATED_COLUMN_CWORD + ", user_score, score  FROM " + Lime.DB_RELATED
                                + "_old";

                        execSQL(dbin, MIGRATE_DATA);

                        String DROP_OLD_TABLE = "DROP TABLE " + Lime.DB_RELATED + "_old";
                        execSQL(dbin, DROP_OLD_TABLE);

                        // Download and restore related DB

                    } catch (SQLiteException e) {
                        e.printStackTrace();
                    }
                } else {
                    String add_column = "ALTER TABLE " + Lime.DB_RELATED + " ADD ";
                    add_column += Lime.DB_RELATED_COLUMN_BASESCORE + " INTEGER";
                    execSQL(dbin, add_column);
                }

            }
        }
    }

    public void checkAndUpdateRelatedTable() {
        // Check related table structure
        String CHECK_RELATED = "SELECT basescore FROM " + Lime.DB_RELATED;

        // If system can find the score field which is mean the table still use old
        // schema
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(CHECK_RELATED, null);
            if (cursor == null || !cursor.moveToFirst()) {
                try {
                    String add_column = "ALTER TABLE " + Lime.DB_RELATED + " ADD ";
                    add_column += Lime.DB_RELATED_COLUMN_BASESCORE + " INTEGER";
                    db.execSQL(add_column);
                } catch (SQLiteException e) {
                    Log.e("LimeDB", "Error adding basescore column: " + e.getMessage());
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        // Check and create index for pword
        cursor = null;
        try {
            cursor = db.query("sqlite_master", null, "type='index' and name = 'related_idx_pword'", null, null, null,
                    null);
            if (cursor == null || !cursor.moveToFirst()) {
                try {
                    db.execSQL("create index 'related_idx_pword' on related (pword)");
                } catch (SQLiteException e) {
                    Log.e("LimeDB", "Error creating pword index: " + e.getMessage());
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        // Check and create index for cword
        cursor = null;
        try {
            cursor = db.query("sqlite_master", null, "type='index' and name = 'related_idx_cword'", null, null, null,
                    null);
            if (cursor == null || !cursor.moveToFirst()) {
                try {
                    db.execSQL("create index 'related_idx_cword' on related (cword)");
                } catch (SQLiteException e) {
                    Log.e("LimeDB", "Error creating cword index: " + e.getMessage());
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    @Deprecated
    public void upgradeRelatedTable(SQLiteDatabase dbin) {
        try {

            String BACKUP_OLD_RELATED = "ALTER " + Lime.DB_RELATED + " RENAME TO " + Lime.DB_RELATED + "_old";
            execSQL(dbin, BACKUP_OLD_RELATED);

            String CREATE_NEW_TABLE = "";

            CREATE_NEW_TABLE += "CREATE TABLE \"" + Lime.DB_RELATED + "\" ( ";
            CREATE_NEW_TABLE += "        \"" + Lime.DB_COLUMN_ID + "\"  INTEGER PRIMARY KEY AUTOINCREMENT,";
            CREATE_NEW_TABLE += "       \"" + Lime.DB_RELATED_COLUMN_PWORD + "\"  text,";
            CREATE_NEW_TABLE += "        \"" + Lime.DB_RELATED_COLUMN_CWORD + "\"  text,";
            CREATE_NEW_TABLE += "        \"" + Lime.DB_RELATED_COLUMN_BASESCORE + "\"  integer,";
            CREATE_NEW_TABLE += "        \"" + Lime.DB_RELATED_COLUMN_USERSCORE + "\"  INTEGER DEFAULT 0";
            CREATE_NEW_TABLE += ");";

            execSQL(dbin, CREATE_NEW_TABLE);

            String CREATE_INDEX = "";
            CREATE_INDEX += "CREATE INDEX \"" + Lime.DB_RELATED + "\".\"related_idx_pword\" ";
            CREATE_INDEX += "ON \"" + Lime.DB_RELATED + "\" (\"" + Lime.DB_RELATED_COLUMN_PWORD + "\" ASC); ";

            execSQL(dbin, CREATE_INDEX);

            String MIGRATE_DATA = "";
            MIGRATE_DATA += "INSERT INTO " + Lime.DB_RELATED + "(" + Lime.DB_RELATED_COLUMN_PWORD + ", "
                    + Lime.DB_RELATED_COLUMN_CWORD + ", " + Lime.DB_RELATED_COLUMN_BASESCORE + ")";
            MIGRATE_DATA += "SELECT " + Lime.DB_RELATED_COLUMN_PWORD + ", " + Lime.DB_RELATED_COLUMN_CWORD
                    + ", score FROM " + Lime.DB_RELATED + "_old";

            execSQL(dbin, MIGRATE_DATA);

            String DROP_OLD_TABLE = "DROP TABLE " + Lime.DB_RELATED + "_old";
            execSQL(dbin, DROP_OLD_TABLE);

            // Download and restore related DB

        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check the consistency of phonetic keyboard setting in preference and db.
     * Jeremy '12,6,8
     */
    public void checkPhoneticKeyboardSetting() {
        if (!checkDBConnection())
            return;
        try {
            checkPhoneticKeyboardSettingOnDB(db);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param dbin sqlite database object
     */
    private void checkPhoneticKeyboardSettingOnDB(SQLiteDatabase dbin) {
        String selectedPhoneticKeyboardType = mLIMEPref.getPhoneticKeyboardType();
        if (DEBUG)
            Log.i("OnUpgrade()", "phonetickeyboardtype:" + selectedPhoneticKeyboardType);
        switch (selectedPhoneticKeyboardType) {
            case "hsu":
                setIMKeyboardOnDB(dbin, "phonetic",
                        getKeyboardInfoOnDB(dbin, "hsu", "desc"), "hsu");// jeremy '12,6,6 new hsu and et26 keybaord

                break;
            case "eten26":
                setIMKeyboardOnDB(dbin, "phonetic",
                        getKeyboardInfoOnDB(dbin, "et26", "desc"), "et26");
                break;
            case "eten":
                setIMKeyboardOnDB(dbin, "phonetic",
                        getKeyboardInfoOnDB(dbin, "phoneticet41", "desc"), "phoneticet41");
                break;
            default:
                setIMKeyboardOnDB(dbin, "phonetic",
                        getKeyboardInfoOnDB(dbin, "phonetic", "desc"), "phonetic");
                break;
        }
    }

    /*
     * Calling from onUpgrade with SquliteDataabase object to upgrade.
     */
    private void execSQL(SQLiteDatabase dbin, String command) {

        try {
            dbin.execSQL(command);

        } catch (Exception e) {
            Log.w(TAG, "Ignore all possible exceptions~");
        }
    }

    // Jeremy '12,4,7
    public boolean openDBConnection(boolean force_reload) {
        if (DEBUG) {
            Log.i(TAG, "openDBConnection(), force_reload = " + force_reload);
            if (db != null)
                Log.i(TAG, "db.isOpen()" + db.isOpen());
        }

        if (!force_reload && db != null && db.isOpen()) {
            return true;
        } else {

            // Reset related phrsae score cache
            if (relatedscore != null)
                relatedscore.clear();

            if (force_reload) {
                try {
                    if (db != null && db.isOpen()) {
                        db.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            db = this.getWritableDatabase();
            databaseOnHold = false;
            return db != null && db.isOpen();
        }
    }

    /**
     * Jeremy '12,5,1 checkDBconnection try to openDBconection if db is not open.
     * Return true if the db connection is valid, return false if dbconnection is
     * not valid
     *
     * @return return true if db connection is ready.
     */
    synchronized boolean checkDBConnection() {
        // Jeremy '12,5,1 mapping loading. db is locked
        if (DEBUG)
            Log.i(TAG, "checkDBConnection()");

        if (databaseOnHold) { // mapping loading in progress, database is not available for query
            if (DEBUG)
                Log.i(TAG, "checkDBConnection() : mapping loading ");
            if (Looper.myLooper() == null)
                Looper.prepare();
            Toast.makeText(mContext, mContext.getText(R.string.l3_database_loading), Toast.LENGTH_SHORT).show();
            Looper.loop();
            return false;
        } else
            return openDBConnection(false);

    }

    /**
     * Base on given table name to remove records
     */
    public void deleteAll(String table) {

        if (DEBUG)
            Log.i(TAG, "deleteAll()");
        if (loadingMappingThread != null) {
            threadAborted = true;
            while (loadingMappingThread.isAlive()) {
                Log.d(TAG, "deleteAll():waiting for loadingMappingThread stopped...");
                SystemClock.sleep(1000);
            }
        }

        if (countMapping(table) > 0)
            db.delete(table, null, null);

        finish = false;
        resetImInfo(table);
        // mLIMEPref.setParameter("im_loading", false);
        // mLIMEPref.setParameter("im_loading_table", "");

        if (blackListCache != null)
            blackListCache.clear();// Jeremy '12, 6,3 clear black list cache after mapping file updated
    }

    /**
     * Empty Related table records
     */
    public synchronized void deleteUserDictAll() {
        if (!checkDBConnection())
            return;
        mLIMEPref.setTotalUserdictRecords("0");
        // -------------------------------------------------------------------------
        // SQLiteDatabase db = this.getSqliteDb(false);
        db.delete(Lime.DB_RELATED, FIELD_DIC_score + " > 0", null);

    }

    /**
     * Count total amount of specific table
     *
     * @return return 0 if db is not ready, the table is not available or with 0
     *         mapping records.
     */
    public int countMapping(String table) {
        if (DEBUG)
            Log.i(TAG, "countMapping() on table:" + table);

        if (!checkDBConnection())
            return 0;
            
        // Check if table exists first to avoid "no such table" log errors
        if (!hasTable(table)) {
            return 0;
        }

        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + table, null);
            if (cursor == null)
                return 0;
            int total = cursor.getCount();
            if (DEBUG)
                Log.i(TAG, "countMapping" + "Table," + table + ": " + total);
            return total;
        } catch (Exception e) {
            Log.e(TAG, "Error counting mapping: " + e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return 0;
    }

    public int getCount() {
        return count;
    }

    public int getProgressPercentageDone() {
        return progressPercentageDone;
    }

    /**
     * Count total amount loaded records amount
     *
     * @return 0 if db is not ready, table is not available or 0 userdic records
     */
    public int countUserdic() {

        if (!checkDBConnection())
            return 0;
        int total = 0;
        Cursor cursor = null;
        try {

            cursor = db.rawQuery(
                    "SELECT * FROM related where " + FIELD_DIC_score + " > 0",
                    null);
            total += cursor.getCount();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return total;
    }

    /**
     * Return the score after add or updated. Jeremy '12,6,7
     */

    public synchronized int addOrUpdateRelatedPhraseRecord(String pword, String cword) {

        // Jeremy '12,4,17 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return -1;

        // Jeremy '11,6,12
        // Return if not learing related words and cword is not null (recording word
        // frequency in IM relatedlist field)
        if (!mLIMEPref.getLearnRelatedWord() && cword != null)
            return -1;

        // Remove all the chinese symbols from the related words
        if (mLIMEPref.getLearnRelatedWord()) {
            try {
                // Remove Punctutation
                String[] chinesesymbols = ChineseSymbol.chineseSymbols.split("|");
                for (String s : chinesesymbols) {
                    cword = cword.replaceAll(s, "");
                    if (cword == null || cword.isEmpty()) {
                        return -1;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /*
         * if (!mLIMEPref.getCandidateSuggestionPunctutation()){
         * 
         * // Remove Punctutation
         * String chinesesymbols[] = ChineseSymbol.chineseSymbols.split("|");
         * for(String s: chinesesymbols){
         * cword = cword.replaceAll(s, "");
         * if(cword == null || cword.isEmpty()){
         * return -1;
         * }
         * }
         * 
         * String englishsymbols[] =
         * {"!","@","#","$","%","^","&","*","(",")","{","}","[","]","\\","/","?",".",",
         * ","<",">",";",":","'"};
         * for(String s: englishsymbols){
         * cword = cword.replace(s, "");
         * cword = cword.replace(s, "");
         * cword = cword.replace(s, "");
         * if(cword == null || cword.isEmpty()){
         * return -1;
         * }
         * }
         * 
         * }
         */

        int dictotal = Integer.parseInt(mLIMEPref.getTotalUserdictRecords());

        if (DEBUG)
            Log.i(TAG,
                    "addOrUpdateRelatedPhraseRecord(): pword:" + pword + " cword:" + cword + "dictotoal:" + dictotal);

        int score = 1;

        ContentValues cv = new ContentValues();
        try {
            Mapping munit = this.isRelatedPhraseExistOnDB(db, pword, cword);

            if (munit == null) {
                cv.put(Lime.DB_RELATED_COLUMN_PWORD, pword);
                cv.put(Lime.DB_RELATED_COLUMN_CWORD, cword);
                // cv.put(Lime.DB_RELATED_COLUMN_SCORE, score); leave this field null so as we
                // can distinguish records learned from user. Jeremy '15,6,3
                cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, score);
                db.insert(Lime.DB_RELATED, null, cv);
                dictotal++;
                mLIMEPref.setTotalUserdictRecords(String.valueOf(dictotal));
                if (DEBUG)
                    Log.i(TAG, "addOrUpdateRelatedPhraseRecord(): new record, dictotal:" + dictotal);
            } else {// the item exist in preload related database.
                if (relatedscore.get(munit.getId()) == null) {
                    score = munit.getScore() + 1;
                    relatedscore.put(munit.getId(), score);
                } else {
                    score = relatedscore.get(munit.getId()) + 1;
                    relatedscore.put(munit.getId(), score);
                }
                cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, score);
                db.update(Lime.DB_RELATED, cv, FIELD_ID + " = " + munit.getId(), null);

                // Log.i("TAG RELATED A", munit.getId() + " : Related ADD Score :" + score);

                if (DEBUG)
                    Log.i(TAG, "addOrUpdateRelatedPhraseRecord():update score on existing record; score:" + score);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return score;

    }

    public synchronized void addOrUpdateMappingRecord(String code, String word) {
        addOrUpdateMappingRecord(this.tablename, code, word, -1);
    }

    /**
     * Add new mapping into current table
     */
    // Jeremy '11, 7, 31 add new phrase mapping into current table (for LD phrase
    // learning).
    public synchronized void addOrUpdateMappingRecord(String table, String code, String word, int score) {
        // String code = preProcessingRemappingCode(raw_code); //Jeremy '12,6,4 the code
        // is build from mapping.getcode() should not do remap again.
        if (DEBUG)
            Log.i(TAG, "addOrUpdateMappingRecord(), code = '" + code + "'. word=" + word + ", score =" + score);
        // Jeremy '12,4,17 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return;

        try {
            Mapping munit = isMappingExistOnDB(db, table, code, word);
            ContentValues cv = new ContentValues();

            if (munit == null) {
                if (code.length() > 0 && word.length() > 0) {

                    cv.put(FIELD_CODE, code);
                    removeFromBlackList(code); // remove from black list if it listed. Jeremy 12,6, 4
                    if (table.equals("phonetic")) {
                        String noToneCode = code.replaceAll("[ 3467]", "");
                        cv.put(FIELD_NO_TONE_CODE, noToneCode);// Jeremy '12,6,1, add missing space
                        removeFromBlackList(noToneCode); // remove from black list if it listed. Jeremy 12,6, 4
                    }
                    cv.put(FIELD_WORD, word);
                    cv.put(FIELD_SCORE, (score == -1) ? 1 : score);
                    db.insert(table, null, cv);

                    if (DEBUG)
                        Log.i(TAG, "addOrUpdateMappingRecord(): mapping does not exist, new record inserted");
                }

            } else {// the item exist in preload related database.

                int newScore = (score == -1) ? munit.getScore() + 1 : score;
                cv.put(FIELD_SCORE, newScore);
                db.update(table, cv, FIELD_ID + " = " + munit.getId(), null);
                if (DEBUG)
                    Log.i(TAG, "addOrUpdateMappingRecord(): mapping exist, update score on existing record; score:"
                            + score);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Add score to the mapping item
     */
    public synchronized void addScore(Mapping srcunit) {

        // Jeremy '12,4,17 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return;

        // Jeremy '11,7,31 even selected from realted list, udpate the corresponding
        // score in im table.
        // Jeremy '11,6,12 Id=null denotes selection from related list in im table
        // Jeremy '11,9,8 query highest score first. Erase relatedlist if new score is
        // not highest.
        try {

            if (srcunit != null && srcunit.getWord() != null &&
                    !srcunit.getWord().trim().equals("")) {

                if (DEBUG)
                    Log.i(TAG, "addScore(): addScore on word:" + srcunit.getWord());

                if (srcunit.isRelatedPhraseRecord()) {

                    int score;
                    if (relatedscore.get(srcunit.getId()) == null) {
                        score = srcunit.getScore() + 1;
                        relatedscore.put(srcunit.getId(), score);
                    } else {
                        score = relatedscore.get(srcunit.getId()) + 1;
                        relatedscore.put(srcunit.getId(), score);
                    }

                    ContentValues cv = new ContentValues();
                    cv.put(Lime.DB_RELATED_COLUMN_USERSCORE, score);
                    db.update(Lime.DB_RELATED, cv, FIELD_ID + " = " + srcunit.getId(), null);

                    // Log.i("TAG RELATED B", srcunit.getId() + " : Related ADD Score :" + score);

                } else {
                    int newScore = srcunit.getScore() + 1;
                    if (srcunit.getCode() != null && !srcunit.getCode().trim().equals("")) {
                        try {
                            Cursor maxCursor = db.rawQuery("SELECT MAX(" + FIELD_SCORE + ") FROM " + tablename 
                                    + " WHERE " + FIELD_CODE + " = ?", new String[] { srcunit.getCode() });
                            if (maxCursor != null) {
                                if (maxCursor.moveToFirst()) {
                                    int maxScore = maxCursor.getInt(0);
                                    if (maxScore >= newScore) {
                                        newScore = maxScore + 1;
                                    }
                                }
                                maxCursor.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    ContentValues cv = new ContentValues();
                    cv.put(FIELD_SCORE, newScore);
                    // Jeremy 11',7,29 update according to word instead of ID, may have multiple
                    // records mathing word but with diff code/id
                    db.update(tablename, cv, FIELD_WORD + " = ?", new String[] { srcunit.getWord() });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Add by jeremy '10, 4, 1. For reverse lookup

    /**
     * Jeremy '12,6,7 for phrase learning to get code from word
     */
    /*
     * public List<Mapping> getMappingFromWord(Mapping mapping, String table) {
     * String keyword = mapping.getWord();
     * return getMappingFromWord(keyword, table);
     * }
     */
    public List<Mapping> getMappingByWord(String keyword, String table) {

        if (DEBUG)
            Log.i(TAG, "getMappingByWord():tablename:" + table + "  keyworad:" + keyword);

        if (!checkDBConnection())
            return null;

        List<Mapping> result = new LinkedList<>();

        try {

            if (keyword != null && !keyword.trim().equals("")) {
                Cursor cursor;
                cursor = db.query(table, null, FIELD_WORD + " = ?", new String[] { keyword },
                        null, null, FIELD_SCORE + " DESC", null);
                if (DEBUG)
                    Log.i(TAG, "getMappingByWord():tablename:" + table + "  keyworad:"
                            + keyword + "  cursor.getCount:"
                            + cursor.getCount());

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            int idColumn = cursor.getColumnIndex(FIELD_ID);
                            int codeColumn = cursor.getColumnIndex(FIELD_CODE);
                            int wordColumn = cursor.getColumnIndex(FIELD_WORD);
                            int scoreColumn = cursor.getColumnIndex(FIELD_SCORE);
                            Mapping munit = new Mapping();
                            munit.setId(cursor.getString(idColumn));
                            munit.setCode(cursor.getString(codeColumn));
                            munit.setWord(cursor.getString(wordColumn));
                            munit.setExactMatchToWordRecord();
                            munit.setScore(cursor.getInt(scoreColumn));
                            result.add(munit);

                        } while (cursor.moveToNext());

                    }
                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }

        if (DEBUG)
            Log.i(TAG, "getMappingByWord() Result.size() = " + result.size());

        return result;
    }

    /**
     * Reverse lookup on keyword.
     */
    public String getCodeListStringByWord(String keyword) {

        if (!checkDBConnection())
            return null;

        String table = mLIMEPref.getRerverseLookupTable(tablename);

        if (table.equals("none")) {
            return null;
        }

        String result = "";
        try {

            if (keyword != null && !keyword.trim().equals("")) {
                Cursor cursor;
                cursor = db.query(table, null, FIELD_WORD + " = ?", new String[] { keyword },
                        null, null, null, null);
                if (DEBUG)
                    Log.i(TAG, "getRmapping():tablename:" + table + "  keyworad:"
                            + keyword + "  cursor.getCount:"
                            + cursor.getCount());

                if (cursor != null) {

                    if (cursor.moveToFirst()) {
                        int codeColumn = cursor.getColumnIndex(FIELD_CODE);
                        int wordColumn = cursor.getColumnIndex(FIELD_WORD);
                        result = cursor.getString(wordColumn) + "="
                                + keyToKeyname(cursor.getString(codeColumn), table, false);
                        if (DEBUG)
                            Log.i(TAG, "getRmapping():Code:"
                                    + cursor.getString(codeColumn));

                        while (cursor.moveToNext()) {
                            result = result
                                    + "; "
                                    + keyToKeyname(cursor.getString(codeColumn),
                                            table, false);
                            if (DEBUG)
                                Log.i(TAG, "getRmapping():Code:"
                                        + cursor.getString(codeColumn));

                        }
                    }

                    cursor.close();
                }
            }
        } catch (Exception ignored) {
        }

        if (DEBUG)
            Log.i(TAG, "getRmapping() Result:" + result);

        return result;
    }

    private LinkedList<Mapping> updateSimilarCodeListInRelatedColumnOnDB(SQLiteDatabase db, String table, String code) {

        String escapedCode = code.replace("'", "''"); // Jeremy '11,9,10 escape '
        if (DEBUG)
            Log.i(TAG, "updateSimilarCodeListInRelatedColumnOnDB(): escapedCodes: " + escapedCode);

        char[] charray = escapedCode.toCharArray();
        charray[escapedCode.length() - 1]++;
        String nextcode = new String(charray);

        // Jeremy '11,9,8 sorting with score + basescore
        String selectString = "SELECT * FROM '" + table + "" +
                "' WHERE " + FIELD_CODE + " > '" + escapedCode + "' AND " + FIELD_CODE + " < '" + nextcode + "'" +
                " ORDER BY " + FIELD_SCORE + " DESC, " + FIELD_BASESCORE + " DESC LIMIT 50";
        Cursor cursor = db.rawQuery(selectString, null);

        if (DEBUG)
            Log.i(TAG, "updateSimilarCodeListInRelatedColumnOnDB(): raw query string: " + selectString);

        LinkedList<Mapping> newMappingList = new LinkedList<>();

        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    HashSet<String> duplicateCheck = new HashSet<>();

                    // int idColumn = cursor.getColumnIndex(FIELD_ID);
                    // int codeColumn = cursor.getColumnIndex(FIELD_CODE);
                    int wordColumn = cursor.getColumnIndex(FIELD_WORD);
                    // int scoreColumn = cursor.getColumnIndex(FIELD_SCORE);
                    do {

                        Mapping munit = new Mapping();
                        munit.setCode(code);
                        munit.setPartialMatchToCodeRecord();
                        munit.setWord(cursor.getString(wordColumn));
                        munit.setId(null);
                        munit.setScore(0);

                        if (munit.getWord() == null || munit.getWord().trim().equals(""))
                            continue;

                        if (duplicateCheck.add(munit.getWord())) {
                            newMappingList.add(munit);
                        }
                    } while (cursor.moveToNext());

                    // Rebuild the related list string and update the record.
                    String newRelatedlist;

                    newRelatedlist = "";
                    for (Mapping munit : newMappingList) {
                        if (newRelatedlist.equals(""))
                            newRelatedlist = munit.getWord();
                        else
                            newRelatedlist = newRelatedlist + "|" + munit.getWord();

                    }
                    ContentValues cv = new ContentValues();
                    cv.put(FIELD_RELATED, newRelatedlist);
                    int highestScoreID = getHighestScoreIDOnDB(db, table, code);
                    if (highestScoreID > 0) {
                        db.update(table, cv, FIELD_ID + " = " + highestScoreID, null);
                        if (DEBUG)
                            Log.i(TAG, "updateSimilarCodeListInRelatedColumnOnDB(): updating code =" + code
                                    + ", the new relatedlist:" + newRelatedlist);
                    } else {
                        cv.put(FIELD_CODE, code);
                        cv.put(FIELD_SCORE, 0);
                        cv.put(FIELD_BASESCORE, 0);
                        if (table.equals("phonetic"))
                            cv.put(FIELD_NO_TONE_CODE, code.replaceAll("[3467 ]", "'")); // Jeremy '12,6,6 should build
                                                                                         // noToneCode for phonetic
                        db.insert(table, null, cv);
                        if (DEBUG)
                            Log.i(TAG, "updateSimilarCodeListInRelatedColumnOnDB(): insert new code =" + code
                                    + ", the new relatedlist:" + newRelatedlist);
                    }

                }
                if (DEBUG)
                    Log.i(TAG, "updateSimilarCodeListInRelatedColumnOnDB(): scorelist.size() =  "
                            + newMappingList.size());

            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return newMappingList;
    }

    /*
     * Rewrite by Jeremy 11,6,4. Supporting array and dayi now.
     * Covert composing codes into composing text (reading string).
     *
     */
    public String keyToKeyname(String code, String table, Boolean composingText) {
        // 重構二期:本體移至 LimeQueryEngine
        return queryEngine.keyToKeyname(code, table, composingText);
    }

    /**
     * Retrieve matched records
     */
    public List<Mapping> getMappingByCode(String code, boolean softKeyboard, boolean getAllRecords) {
        // 重構二期:本體移至 LimeQueryEngine
        return queryEngine.getMappingByCode(code, softKeyboard, getAllRecords);
    }

    public String preProcessingRemappingCode(String code) {
        // 重構二期:本體移至 LimeQueryEngine
        return queryEngine.preProcessingRemappingCode(code);
    }


    /**
     * Jeremy '12,6,4 check black list on code , code + wildcard and reduced code
     * with wildcard
     */
    private void removeFromBlackList(String code) {
        if (blackListCache.get(cacheKey(code)) != null)
            blackListCache.remove(cacheKey(code));

        for (int i = DUALCODE_NO_CHECK_LIMIT - 1; i <= code.length(); i++) {
            String codeToCheck = code.substring(0, i) + "%";
            if (blackListCache.get(cacheKey(codeToCheck)) != null)
                blackListCache.remove(cacheKey(codeToCheck));

        }

    }


    /**
     * Jeremy '12,6,3 Build unique cache key for black list cache.
     */

    String cacheKey(String code) {

        return tablename + "_" + code;
    }


    /**
     * @return Cursor for
     *
     *         public Cursor getDictionaryAll() {
     *         //Jeremy '12,5,1 !checkDBConnection() when db is restoring or
     *         replaced.
     *         if (!checkDBConnection()) return null;
     * 
     *         Cursor cursor;
     *         cursor = db.query("dictionary", null, null, null, null, null, null,
     *         null);
     *         return cursor;
     *         }
     */

    /**
     * Get dictionary database contents
     */
    public List<Mapping> getRelatedPhrase(String pword, boolean getAllRecords) {
        if (DEBUG)
            Log.i(TAG, "getRelatedPhrase(), " + getAllRecords);

        List<Mapping> result = new LinkedList<>();

        if (mLIMEPref.getSimiliarEnable()) {

            if (pword != null && !pword.trim().equals("")) {

                Cursor cursor;

                // Jeremy '11,8.23 remove group by condition to avoid sorting ordr
                // Jeremy '11,8,1 add group by cword to remove duplicate items.
                // Jeremy '11,6,12, Add constraint on cword is not null (cword =null is for
                // recoding im related list selected count).
                // Jeremy '12,12,21 Add limitClause to limit candidates in only 1 page first.
                // to do 2 stage query.
                // Jeremy '14,12,38 Add query on word length > 1 to include last character into
                // query
                String limitClause;

                limitClause = (getAllRecords) ? FINAL_RESULT_LIMIT : INITIAL_RESULT_LIMIT;

                if (pword.length() > 1) {

                    String last = pword.substring(pword.length() - 1);

                    String selectString = "SELECT " + FIELD_ID + ", " + FIELD_DIC_pword + ", " + FIELD_DIC_cword + ", "
                            + Lime.DB_RELATED_COLUMN_BASESCORE + ", " + Lime.DB_RELATED_COLUMN_USERSCORE
                            + ", length(" + FIELD_DIC_pword + ") as len FROM " + Lime.DB_RELATED + " where "
                            + FIELD_DIC_pword + " = '" + pword
                            + "' or " + FIELD_DIC_pword + " = '" + last
                            + "' and " + FIELD_DIC_cword + " is not null"
                            + " order by len desc, " + Lime.DB_RELATED_COLUMN_USERSCORE + " desc, "
                            + Lime.DB_RELATED_COLUMN_BASESCORE + " desc ";

                    selectString += " limit " + limitClause;

                    if (DEBUG)
                        Log.i(TAG, "getRelatedPhrase() selectString = " + selectString);

                    try {
                        cursor = db.rawQuery(selectString, null);
                    } catch (SQLiteException sqe) {
                        if (DEBUG)
                            sqe.getStackTrace();

                        cursor = null;
                    }

                } else {
                    cursor = db.query(Lime.DB_RELATED, null, FIELD_DIC_pword + " = '" + pword
                            + "' and " + FIELD_DIC_cword + " is not null ", null, null, null,
                            Lime.DB_RELATED_COLUMN_USERSCORE + " DESC, "
                                    + Lime.DB_RELATED_COLUMN_BASESCORE + " DESC",
                            limitClause);
                }
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {

                            int rsize = 0;
                            do {
                                Mapping munit = new Mapping();
                                munit.setId(cursor.getString(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_ID)));
                                munit.setPword(cursor.getString(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_PWORD)));
                                munit.setCode("");
                                munit.setWord(cursor.getString(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_CWORD)));
                                munit.setScore(cursor.getInt(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_USERSCORE)));
                                munit.setBasescore(
                                        cursor.getInt(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_BASESCORE)));
                                munit.setRelatedPhraseRecord();
                                result.add(munit);
                                rsize++;
                            } while (cursor.moveToNext());
                            // Removed "..." indicator - now using horizontal scroll in CandidateView
                            // Mapping temp = new Mapping();
                            // temp.setCode("has_more_records");
                            // temp.setWord("...");
                            // temp.setHasMoreRecordsMarkRecord();
                            // if ((!getAllRecords && rsize == Integer.parseInt(INITIAL_RESULT_LIMIT)))
                            // result.add(temp);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
        return result;
    }

    public boolean prepareBackupRelatedDb(String sourcedbfile) {
        if (!checkDBConnection())
            return false;

        holdDBConnection();
        db.execSQL("attach database ? as sourceDB", new Object[] { sourcedbfile });
        db.execSQL("insert into sourceDB." + Lime.DB_RELATED + " select * from " + Lime.DB_RELATED);
        db.execSQL("detach database sourceDB");
        unHoldDBConnection();
        return true;
    }

    public boolean prepareBackupDb(String sourcedbfile, String sourcetable) {
        if (!checkDBConnection())
            return false;

        String validatedTable = validateTableName(sourcetable);
        holdDBConnection();
        db.execSQL("attach database ? as sourceDB", new Object[] { sourcedbfile });
        db.execSQL("insert into sourceDB." + Lime.DB_TABLE_CUSTOM + " select * from " + validatedTable);
        db.execSQL("insert into sourceDB." + Lime.DB_IM + " select * from " + Lime.DB_IM + " WHERE code=?",
                new Object[] { validatedTable });
        db.execSQL("update sourceDB." + Lime.DB_IM + " set " + Lime.DB_IM_COLUMN_CODE + "=?",
                new Object[] { validatedTable });
        db.execSQL("detach database sourceDB");
        unHoldDBConnection();
        return true;
    }

    public boolean importBackupRelatedDb(File sourcedbfile) {
        if (!checkDBConnection())
            return false;

        // Reset IM Info
        deleteAll(Lime.DB_RELATED);

        holdDBConnection();

        // Load data from DB File
        db.execSQL("attach database ? as sourceDB", new Object[] { sourcedbfile.getAbsolutePath() });
        db.execSQL("insert into " + Lime.DB_RELATED + " select * from sourceDB." + Lime.DB_RELATED);
        db.execSQL("detach database sourceDB");
        unHoldDBConnection();
        return true;
    }

    public boolean importBackupDb(File sourcedbfile, String imtype) {
        if (!checkDBConnection())
            return false;

        String validatedImType = validateTableName(imtype);
        // Reset IM Info
        deleteAll(validatedImType);
        db.execSQL("delete from " + Lime.DB_IM + " where " + Lime.DB_IM_COLUMN_CODE + "=?",
                new Object[] { validatedImType });

        holdDBConnection();

        // Load data from DB File
        db.execSQL("attach database ? as sourceDB", new Object[] { sourcedbfile.getAbsolutePath() });
        db.execSQL("insert into " + validatedImType + " select * from sourceDB." + Lime.DB_TABLE_CUSTOM);
        db.execSQL("update sourceDB." + Lime.DB_IM + " set " + Lime.DB_IM_COLUMN_CODE + "=?",
                new Object[] { validatedImType });
        db.execSQL("insert into " + Lime.DB_IM + " select * from sourceDB." + Lime.DB_IM);
        db.execSQL("detach database sourceDB");
        unHoldDBConnection();
        return true;
    }

    public int importDb(String sourcedbfile, String imtype) {
        if (!checkDBConnection())
            return -1;

        String validatedImType = validateTableName(imtype);
        deleteAll(validatedImType);
        holdDBConnection();
        try {
            db.execSQL("attach database ? as sourceDB", new Object[] { sourcedbfile });

            // Identify source table name - it might match imtype or be a generic 'phonetic' or 'dayi'
            String sourceTable = null;
            String[] possibleSourceTables = {validatedImType, "phonetic", "dayi", Lime.DB_TABLE_CUSTOM};

            for (String table : possibleSourceTables) {
                Cursor cursor = db.rawQuery("SELECT name FROM sourceDB.sqlite_master WHERE type='table' AND name=?",
                        new String[] { table });
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        sourceTable = cursor.getString(0);
                    }
                    cursor.close();
                }
                if (sourceTable != null) break;
            }

            if (sourceTable == null) {
                Log.e(TAG, "importDb: No valid source table found in " + sourcedbfile);
                db.execSQL("detach database sourceDB");
                return -1;
            }

            Log.d(TAG, "importDb: Importing from source table '" + sourceTable + "' to '" + validatedImType + "'");
            db.execSQL("insert into " + validatedImType + " select * from sourceDB." + sourceTable);

            // Update IM info if present in source
            Cursor imCursor = db.rawQuery("SELECT name FROM sourceDB.sqlite_master WHERE type='table' AND name=?",
                    new String[] { Lime.DB_IM });
            if (imCursor != null) {
                if (imCursor.moveToFirst()) {
                    db.execSQL("delete from " + Lime.DB_IM + " where code = ?", new Object[] { validatedImType });
                    // We need to ensure we only insert the relevant IM info if there are multiple
                    db.execSQL("insert into " + Lime.DB_IM + " select * from sourceDB." + Lime.DB_IM + " where code = ?",
                            new Object[] { sourceTable });
                    // Update the code in our DB to match the requested imtype if it was different in source
                    db.execSQL("update " + Lime.DB_IM + " set code = ? where code = ?",
                            new Object[] { validatedImType, sourceTable });
                }
                imCursor.close();
            }

            db.execSQL("detach database sourceDB");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                db.execSQL("detach database sourceDB");
            } catch (Exception ex) {
                // ignore
            }
            return -1;
        } finally {
            unHoldDBConnection();
        }

        return countMapping(validatedImType);
    }

    /*
     * Backup learned user scores and phrases from the specified table to the backup
     * table.
     * Jeremy '15,5,21
     */
    public void deleteRelatedPhrase(String pword, String cword) {
        if (!checkDBConnection())
            return;
        db.delete(Lime.DB_RELATED, FIELD_DIC_pword + " = ? AND " + FIELD_DIC_cword + " = ?", new String[] { pword, cword });
    }

    public int backupUserRecords(final String table) {
        if (!checkDBConnection())
            return -1;
        String backupTableName = table + "_user";

        String selectString = "select * from " + table +
                " where " + FIELD_WORD + " is not null and " +
                FIELD_SCORE + " >0 order by " + FIELD_SCORE + " desc";
        Cursor cursor = db.rawQuery(selectString, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.close();
            try {
                db.execSQL("drop table " + backupTableName);
            } catch (Exception e) {
                Log.i(TAG, "Remove the table " + backupTableName);
            }
            db.execSQL("create table " + backupTableName + " as " + selectString);
        }

        return countMapping(backupTableName);
    }

    /*
     * Restore learned user scores and phrases from the backup table to the
     * specified table.
     * Jeremy '15,5,21
     */
    /*
     * public void restoreUserRecordsStep1(final String table) {
     * 
     * if (!checkDBConnection()) return;
     * 
     * String backupTableName = table + "_user";
     * 
     * // check if user data backup table is present and have valid records
     * int userRecordsCount = countMapping(backupTableName);
     * if (userRecordsCount == 0) return;
     * 
     * try {
     * // Load backuptable records
     * Cursor cursorsource = db.rawQuery("select * from " + table, null);
     * List<Word> clist = Word.getList(cursorsource);
     * cursorsource.close();
     * 
     * HashMap<String, Word> check = new HashMap<String, Word>();
     * for(Word w : clist){
     * String key = w.getCode() + w.getWord();
     * check.put(key, w);
     * }
     * 
     * Cursor cursorbackup = db.rawQuery("select * from " + backupTableName, null);
     * List<Word> backuplist = Word.getList(cursorbackup);
     * cursorbackup.close();
     * 
     * int count = 0;
     * int total = backuplist.size();
     * 
     * for(Word w: backuplist){
     * 
     * count++;
     * 
     * // update record
     * String key = w.getCode() + w.getWord();
     * 
     * if(check.containsKey(key)){
     * try{
     * db.execSQL("update " + table + " set " + Lime.DB_COLUMN_SCORE + " = " +
     * w.getScore()
     * + " WHERE " + Lime.DB_COLUMN_CODE + " = '" + w.getCode() + "'"
     * + " AND " + Lime.DB_COLUMN_WORD + " = '" + w.getWord() + "'"
     * );
     * }catch(Exception e){
     * e.printStackTrace();
     * }
     * }else{
     * try{
     * Word temp = check.get(key);
     * String insertsql = Word.getInsertQuery(table, temp);
     * db.execSQL(insertsql);
     * }catch(Exception e){
     * e.printStackTrace();
     * }
     * }
     * 
     * // Update Progress
     * double progress = (((count / total) * 0.8) * 100) + 10;
     * 
     * 
     * }
     * 
     * check.clear();
     * 
     * }catch(Exception e){
     * e.printStackTrace();
     * }
     * }
     */

    @Deprecated
    public void restoreUserRecordsStep2(final String table) {

        if (!checkDBConnection())
            return;

        String backupTableName = table + "_user";

        // check if user data backup table is present and have valid records
        int userRecordsCount = countMapping(backupTableName);
        if (userRecordsCount == 0)
            return;

        try {
            // TODO: put this into working loadingMappingThread?
            Cursor cursor = db.rawQuery("select " + FIELD_CODE + " from " + backupTableName, null);

            if (cursor != null) {

                if (cursor.moveToFirst()) {

                    int codeColumn = cursor.getColumnIndex(FIELD_CODE);
                    HashSet<String> codeList = new HashSet<>();
                    do {
                        String code = cursor.getString(codeColumn);
                        codeList.add(code);
                        if (code.length() > 1) {
                            int len = code.length();
                            if (len > 5)
                                len = 5; // Jeremy '12,6,12 track code bakcward for 5 levels.
                            for (int k = 1; k < len; k++) {
                                String subCode = code.substring(0, code.length() - k);
                                codeList.add(subCode);
                            }
                        }
                    } while (cursor.moveToNext());

                    db.beginTransaction();
                    try {
                        for (String entry : codeList) {
                            // if(threadAborted) break;
                            // progressPercentageDone = (int) ((float)(i++)/(float)entrySize *50 +50);
                            // f(progressPercentageDone>99) progressPercentageDone = 99;
                            // if(DEBUG)
                            // Log.i(TAG, "loadFileV2():building related list:" + i +"/" + entrySize);
                            try {
                                updateSimilarCodeListInRelatedColumnOnDB(db, table, entry);

                            } catch (Exception e2) {
                                Log.i(TAG, "restoreUserData():create related field error on code =" + entry);
                            }

                        }
                        codeList.clear();
                        db.setTransactionSuccessful();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (DEBUG)
                            Log.i(TAG, "restoreUserData():  related list buiding loop final section");
                        db.endTransaction();

                    }

                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        public boolean checkBackuptable(String table) {
        String backupTableName = table + "_user";
        
        if (!checkDBConnection() || !hasTable(backupTableName)) {
            return false;
        }

        try {
            Cursor cursor = db.rawQuery("select COUNT(*) as total from " + backupTableName, null);

            cursor.moveToFirst();

            int total = cursor.getInt(cursor.getColumnIndex("total"));
            cursor.close();
            
            if (total > 0) {
                Log.i("LIME", "Total size :" + total);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a table exists in the database.
     *
     * @param tableName Name of the table to check
     * @return true if the table exists
     */
    public boolean hasTable(String tableName) {
        if (!checkDBConnection()) return false;
        
        Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            new String[]{tableName}
        );
        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return exists;
    }

    /**
     * Jeremy '11,9,8 loadFile() with basescore got from hanconverter
     */
    public synchronized void loadFileV2(final String table, final LIMEProgressListener progressListener) {

        if (DEBUG)
            Log.i(TAG, "loadFileV2()");
        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection()) {
            progressListener.onError(-1, "Database is not avaiable. Please try to do it later");
            return;
        }

        finish = false;
        progressPercentageDone = 0;
        count = 0;
        if (loadingMappingThread != null) {
            // threadAborted = true;
            while (loadingMappingThread.isAlive()) {
                Log.d(TAG, "loadFile():waiting for last loading loadingMappingThread stopped...");
                SystemClock.sleep(1000);
            }
            loadingMappingThread = null;
        }

        loadingMappingThread = new Thread() {

            public void run() {

                String delimiter_symbol = "";

                // Reset Database Table
                // SQLiteDatabase db = getSqliteDb(false);
                if (DEBUG)
                    Log.i(TAG, "loadFileV2 loadingMappingThread starting...");

                try {
                    if (countMapping(table) > 0)
                        db.delete(table, null, null);

                    if (table.equals("phonetic")) {
                        if (DEBUG)
                            Log.i(TAG, "loadfile(), build code3r index.");
                        mLIMEPref.setParameter("checkLDPhonetic", "doneV2");
                        db.execSQL("CREATE INDEX phonetic_idx_code3r ON phonetic(code3r)");

                    }
                } catch (Exception e1) {
                    e1.printStackTrace();

                }

                resetImInfo(table);
                boolean isCinFormat = false;

                String imname = "";
                String line;
                String endkey = "";
                String selkey = "";
                String spacestyle = "";
                String imkeys = "";
                String imkeynames = "";

                // Check if source file is .cin format
                if (filename.getName().toLowerCase(Locale.US).endsWith(".cin")) {
                    isCinFormat = true;
                }

                // Base on first 100 line to identify the Delimiter
                try {
                    // Prepare Source File
                    FileReader fr = new FileReader(filename);
                    BufferedReader buf = new BufferedReader(fr);
                    int i = 0;
                    List<String> templist = new ArrayList<>();
                    while ((line = buf.readLine()) != null
                            && !isCinFormat) {
                        templist.add(line);
                        if (i >= 100) {
                            break;
                        } else {
                            i++;
                        }
                    }
                    delimiter_symbol = identifyDelimiter(templist);
                    templist.clear();
                    buf.close();
                    fr.close();
                } catch (Exception ignored) {
                    progressListener.onError(-1, "Source file reading error.");
                }

                // HashSet<String> codeList = new HashSet<>();

                // db = getSqliteDb(false);

                // Jeremy '12,4,10 db will locked after beginTrasaction();
                // mLIMEPref.holdDatabaseCoonection(true);
                // Jeremy '15,5,23 new database on hold mechanism.
                holdDBConnection();
                db.beginTransaction();

                try {
                    // Prepare Source File
                    progressStatus = mContext.getResources().getText(R.string.setup_load_migrate_import).toString();
                    long fileLength = filename.length();
                    long processedLength = 0;
                    FileReader fr = new FileReader(filename);
                    BufferedReader buf = new BufferedReader(fr);
                    boolean firstline = true;
                    boolean inChardefBlock = false;
                    boolean inKeynameBlock = false;
                    // String precode = "";

                    while ((line = buf.readLine()) != null && !threadAborted) {
                        processedLength += line.getBytes().length + 2; // +2 for the eol mark.
                        progressPercentageDone = (int) ((float) processedLength / (float) fileLength * 100);

                        // Log.i(TAG, line + " / " + delimiter_symbol.equals(" ") + " / " +
                        // line.indexOf(delimiter_symbol));
                        // if(DEBUG)
                        // Log.i(TAG, "loadFile():loadFile()"+ progressPercentageDone +"% processed"
                        // + ". processedLength:" + processedLength + ". fileLength:" + fileLength + ",
                        // threadAborted=" + threadAborted);
                        if (progressPercentageDone > 99)
                            progressPercentageDone = 99;

                        if (delimiter_symbol.equals(" ") && line.indexOf(delimiter_symbol) == -1) {
                            continue;
                        }

                        if (delimiter_symbol.equals(" ")) {
                            line = line.replaceAll("     ", " ");
                            line = line.replaceAll("    ", " ");
                            line = line.replaceAll("   ", " ");
                            line = line.replaceAll("  ", " ");
                        }

                        if (line.length() < 3) {
                            continue;
                        }

                        /*
                         * If source is cin format start from the tag %chardef
                         * begin until %chardef end
                         */
                        if (isCinFormat) {
                            if (!(inChardefBlock || inKeynameBlock)) {
                                // Modified by Jeremy '10, 3, 28. Some .cin have
                                // double space between $chardef and begin or
                                // end
                                if (line != null
                                        && line.trim().toLowerCase(Locale.US).startsWith("%chardef")
                                        && line.trim().toLowerCase(Locale.US).endsWith("begin")) {
                                    inChardefBlock = true;
                                }
                                if (line != null
                                        && line.trim().toLowerCase(Locale.US).startsWith("%keyname")
                                        && line.trim().toLowerCase(Locale.US).endsWith("begin")) {
                                    inKeynameBlock = true;
                                }
                                // Add by Jeremy '10, 3 , 27
                                // use %cname as mapping_version of .cin
                                // Jeremy '11,6,5 add selkey, endkey and spacestyle support
                                if (!(line.trim().toLowerCase(Locale.US).startsWith("%cname")
                                        || line.trim().toLowerCase(Locale.US).startsWith("%selkey")
                                        || line.trim().toLowerCase(Locale.US).startsWith("%endkey")
                                        || line.trim().toLowerCase(Locale.US).startsWith("%spacestyle"))) {
                                    continue;
                                }
                            }
                            if (line != null
                                    && line.trim().toLowerCase(Locale.US).startsWith("%keyname")
                                    && line.trim().toLowerCase(Locale.US).endsWith("end")) {
                                inKeynameBlock = false;
                                continue;
                            }
                            if (line != null
                                    && line.trim().toLowerCase(Locale.US).startsWith("%chardef")
                                    && line.trim().toLowerCase(Locale.US).endsWith("end")) {
                                break;
                            }
                        }

                        // Check if file contain BOM MARK at file header
                        if (firstline) {
                            byte[] srcstring = line.getBytes();
                            if (srcstring.length > 3) {
                                if (srcstring[0] == -17 && srcstring[1] == -69
                                        && srcstring[2] == -65) {
                                    byte[] tempstring = new byte[srcstring.length - 3];
                                    // int a = 0;
                                    System.arraycopy(srcstring, 3, tempstring, 0, srcstring.length - 3);
                                    line = new String(tempstring);
                                }
                            }
                            firstline = false;
                        } else if (line == null || line.trim().equals("") || line.length() < 3) {
                            continue;
                        }

                        try {

                            int source_score = 0, source_basescore = 0;
                            String code = null, word = null;
                            if (isCinFormat) {
                                if (line.contains("\t")) {
                                    try {
                                        code = line.split("\t")[0];
                                        word = line.split("\t")[1];
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    try {
                                        // Simply ignore error and try to load score and basescore values
                                        source_score = Integer.parseInt(line.split("\t")[2]);
                                        source_basescore = Integer.parseInt(line.split("\t")[3]);
                                    } catch (Exception ignored) {
                                    }
                                } else if (line.contains(" ")) {
                                    try {
                                        code = line.split(" ")[0];
                                        word = line.split(" ")[1];
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    try {
                                        // Simply ignore error and try to load score and basescore values
                                        source_score = Integer.parseInt(line.split(" ")[2]);
                                        source_basescore = Integer.parseInt(line.split(" ")[3]);
                                    } catch (Exception ignored) {
                                    }
                                }
                            } else {
                                if (delimiter_symbol.equals("|")) {
                                    try {
                                        code = line.split("\\|")[0];
                                        word = line.split("\\|")[1];
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    try {
                                        // Simply ignore error and try to load score and basescore values
                                        source_score = Integer.parseInt(line.split("\\|")[2]);
                                        source_basescore = Integer.parseInt(line.split("\\|")[3]);
                                    } catch (Exception ignored) {
                                    }
                                } else {
                                    try {
                                        code = line.split(delimiter_symbol)[0];
                                        word = line.split(delimiter_symbol)[1];
                                    } catch (Exception e) {
                                        continue;
                                    }
                                    try {
                                        // Simply ignore error and try to load score and basescore values
                                        source_score = Integer.parseInt(line.split(delimiter_symbol)[2]);
                                        source_basescore = Integer.parseInt(line.split(delimiter_symbol)[3]);
                                    } catch (Exception ignored) {
                                    }
                                }

                            }
                            if (code == null || code.trim().equals("")) {
                                continue;
                            } else {
                                code = code.trim();
                            }
                            if (word == null || word.trim().equals("")) {
                                continue;
                            } else {
                                word = word.trim();
                            }
                            if (code.toLowerCase(Locale.US).contains("@version@")) {
                                imname = word.trim();
                                continue;
                            } else if (code.toLowerCase(Locale.US).contains("%cname")) {
                                imname = word.trim();
                                continue;
                            } else if (code.toLowerCase(Locale.US).contains("%selkey")) {
                                selkey = word.trim();
                                if (DEBUG)
                                    Log.i(TAG, "loadfile(): selkey:" + selkey);
                                continue;
                            } else if (code.toLowerCase(Locale.US).contains("%endkey")) {
                                endkey = word.trim();
                                if (DEBUG)
                                    Log.i(TAG, "loadfile(): endkey:" + endkey);
                                continue;
                            } else if (code.toLowerCase(Locale.US).contains("%spacestyle")) {
                                spacestyle = word.trim();
                                continue;
                            } else {
                                code = code.toLowerCase(Locale.US);
                            }

                            if (inKeynameBlock) { // Jeremy '11,6,5 preserve keyname blocks here.
                                imkeys = imkeys + code.toLowerCase(Locale.US).trim();
                                String c = word.trim();
                                if (!c.equals("")) {
                                    if (imkeynames.equals(""))
                                        imkeynames = c;
                                    else
                                        imkeynames = imkeynames + "|" + c;
                                }

                            } else {
                                /*
                                 * if (code.length() > 1) {
                                 * int len = code.length();
                                 * if (len > 5)
                                 * len = 5; //Jeremy '12,6,12 track code bakcward for 5 levels.
                                 * for (int k = 1; k < len; k++) {
                                 * String subCode = code.substring(0, code.length() - k);
                                 * codeList.add(subCode);
                                 * }
                                 * }
                                 */
                                count++;
                                ContentValues cv = new ContentValues();
                                cv.put(FIELD_CODE, code);

                                if (table.equals("phonetic")) {
                                    cv.put(FIELD_NO_TONE_CODE, code.replaceAll("[3467 ]", ""));
                                }
                                cv.put(FIELD_WORD, word);
                                cv.put(FIELD_SCORE, source_score);
                                if (source_basescore == 0) {
                                    source_basescore = getBaseScore(word);
                                }
                                cv.put(FIELD_BASESCORE, source_basescore);
                                // if(DEBUG) Log.i(TAG, "loadfilev2():code="+code+", word="+word+",
                                // basescore="+basescore);
                                db.insert(table, null, cv);
                            }

                        } catch (StringIndexOutOfBoundsException ignored) {
                        }
                    }

                    buf.close();
                    fr.close();

                    db.setTransactionSuccessful();
                } catch (Exception e) {

                    Log.i(TAG, "Error : " + e);
                    setImInfo(table, "amount", "0");
                    setImInfo(table, "source", "Failed!!!");
                    e.printStackTrace();
                    progressListener.onError(-1, "Table file import failed!");
                } finally {
                    if (DEBUG)
                        Log.i(TAG, "loadfile(): main import loop final section");
                    db.endTransaction();
                    // mLIMEPref.holdDatabaseCoonection(false); // Jeremy '12,4,10 reset
                    // mapping_loading status
                    unHoldDBConnection();

                }

                // TODO: do phrase table learning here.
                // Create related field
                /*
                 * if (!threadAborted) {
                 * //db = getSqliteDb(false);
                 * progressStatus =
                 * mContext.getResources().getText(R.string.setup_load_migrate_rebuild_related).
                 * toString();
                 * //mLIMEPref.holdDatabaseCoonection(true); // Jeremy '12,4,10 reset
                 * mapping_loading status
                 * holdDBConnection(); //Jeremy '12,5,23
                 * db.beginTransaction();
                 * try {
                 * long entrySize = codeList.size();
                 * long i = 0;
                 * 
                 * 
                 * for (String entry : codeList) {
                 * if (threadAborted) break;
                 * progressPercentageDone = (int) ((float) (i++) / (float) entrySize * 50 + 50);
                 * if (progressPercentageDone > 99) progressPercentageDone = 99;
                 * 
                 * try {
                 * updateSimilarCodeListInRelatedColumnOnDB(db, table, entry);
                 * 
                 * } catch (Exception e2) {
                 * 
                 * Log.i(TAG, "loadfile():create related field error on code =" + entry);
                 * }
                 * 
                 * }
                 * codeList.clear();
                 * db.setTransactionSuccessful();
                 * } catch (Exception e) {
                 * setImInfo(table, "amount", "0");
                 * setImInfo(table, "source", "Failed!!!");
                 * progressListener.onError(-1, "Create related field error");
                 * e.printStackTrace();
                 * } finally {
                 * if (DEBUG)
                 * Log.i(TAG, "loadfile(): related list buiding loop final section");
                 * db.endTransaction();
                 * progressListener.onStatusUpdate(mContext.getResources().getText(R.string.
                 * setup_load_import_finish).toString());
                 * 
                 * }
                 * unHoldDBConnection(); //Jeremy '15,6,3. need to un-hold DB connection either
                 * loading is successfully or not.
                 * 
                 * }
                 */

                // Fill IM information into the IM Table
                if (!threadAborted) {
                    if (!threadAborted)
                        progressPercentageDone = 100;
                    finish = true;

                    mLIMEPref.setParameter("_table", "");

                    setImInfo(table, "source", filename.getName());
                    if (imname == null || imname.isEmpty()) {
                        setImInfo(table, "name", filename.getName());
                    } else {
                        setImInfo(table, "name", imname);
                    }
                    setImInfo(table, "amount", String.valueOf(count));
                    setImInfo(table, "import", new Date().toString()); // Jeremy '12,4,21 toLocaleString() is deprecated

                    if (DEBUG)
                        Log.i("limedb:loadfile()", "Fianlly section: source:"
                                + getImInfo(table, "source") + " amount:" + getImInfo(table, "amount"));

                    // If user download from LIME Default IM SET then fill in related information
                    if (filename.getName().equals("phonetic.lime") || filename.getName().equals("phonetic_adv.lime")) {
                        setImInfo("phonetic", "selkey", "123456789");
                        setImInfo("phonetic", "endkey", "3467'[]\\=<>?:\"{}|~!@#$%^&*()_+");
                        setImInfo("phonetic", "imkeys",
                                ",-./0123456789;abcdefghijklmnopqrstuvwxyz'[]\\=<>?:\"{}|~!@#$%^&*()_+");
                        setImInfo("phonetic", "imkeynames",
                                "ㄝ|ㄦ|ㄡ|ㄥ|ㄢ|ㄅ|ㄉ|ˇ|ˋ|ㄓ|ˊ|˙|ㄚ|ㄞ|ㄤ|ㄇ|ㄖ|ㄏ|ㄎ|ㄍ|ㄑ|ㄕ|ㄘ|ㄛ|ㄨ|ㄜ|ㄠ|ㄩ|ㄙ|ㄟ|ㄣ|ㄆ|ㄐ|ㄋ|ㄔ|ㄧ|ㄒ|ㄊ|ㄌ|ㄗ|ㄈ|、|「|」|＼|＝|，|。|？|：|；|『|』|│|～|！|＠|＃|＄|％|︿|＆|＊|（|）|－|＋");
                    }
                    if (true) {
                        if (!selkey.equals(""))
                            setImInfo(table, "selkey", selkey);
                        if (!endkey.equals(""))
                            setImInfo(table, "endkey", endkey);
                        if (!spacestyle.equals(""))
                            setImInfo(table, "spacestyle", spacestyle);
                        if (!imkeys.equals(""))
                            setImInfo(table, "imkeys", imkeys);
                        if (!imkeynames.equals(""))
                            setImInfo(table, "imkeynames", imkeynames);
                    }
                    if (DEBUG)
                        Log.i(TAG, "loadfilev2():update IM info: imkeys:" + imkeys + " imkeynames:" + imkeynames);

                    // Prepare and Setup the Keyboard of the IM

                    // If there is no keyboard assigned for current input method then use default
                    // keyboard layout
                    // String keyboard = getImInfo(table, "keyboard");
                    // if(keyboard == null || keyboard.equals("")){
                    // setImInfo(table, "keyboard", "lime");
                    // '11,5,23 by Jeremy: Preset keyboard info. by tablename
                    KeyboardObj kobj = getKeyboardObj(table);
                    if (table.equals("phonetic")) {
                        String selectedPhoneticKeyboardType = mLIMEPref.getParameterString("phonetic_keyboard_type",
                                "standard");
                        switch (selectedPhoneticKeyboardType) {
                            case "standard":
                                kobj = getKeyboardObj("phonetic");
                                break;
                            case "eten":
                                kobj = getKeyboardObj("phoneticet41");
                                break;
                            case "eten26":
                                if (mLIMEPref.getParameterBoolean("number_row_in_english", false)) {
                                    kobj = getKeyboardObj("limenum");
                                } else {
                                    kobj = getKeyboardObj("lime");
                                }
                                break;
                            case "eten26_symbol":
                                kobj = getKeyboardObj("et26");
                                break;
                            case "hsu": // Jeremy '12,7,6 Add HSU english keyboard support
                                if (mLIMEPref.getParameterBoolean("number_row_in_english", false)) {
                                    kobj = getKeyboardObj("limenum");
                                } else {
                                    kobj = getKeyboardObj("lime");
                                }
                                break;
                            case "hsu_symbol":
                                kobj = getKeyboardObj("hsu");
                                break;
                        }
                    } else if (table.startsWith("dayi")) {
                        kobj = getKeyboardObj("dayi");
                    } else if (kobj == null) { // Jeremy '12,5,21 chose english with number keyboard if the optione is
                                               // on for default keyboard.
                        if (mLIMEPref.getParameterBoolean("number_row_in_english", true)) {
                            kobj = getKeyboardObj("limenum");
                        } else {
                            kobj = getKeyboardObj("lime");
                        }
                    }
                    setIMKeyboard(table, kobj.getDescription(), kobj.getCode());
                }

                // finishing

            }
        };

        Thread reportProgressThread = new Thread() {
            public void run() {

                long interval = progressListener.progressInterval();
                while (loadingMappingThread.isAlive()) {
                    SystemClock.sleep(interval);
                    progressListener.onProgress(progressPercentageDone, 0, progressStatus);
                }
                progressPercentageDone = 100;
                progressListener.onPostExecute(true, null, 0);

            }

        };

        threadAborted = false;
        loadingMappingThread.start();
        reportProgressThread.start();
    }

    /*
     * public ContentValues getInsertItem(String code, String word) {
     * try {
     * ContentValues cv = new ContentValues();
     * cv.put(FIELD_CODE, code);
     * cv.put(FIELD_WORD, word);
     * cv.put(FIELD_SCORE, 0);
     * return cv;
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * return null;
     * }
     */

    /**
     * Identify the delimiter of the source file
     *
     * @param src text format table string
     */
    public String identifyDelimiter(List<String> src) {

        int commaCount = 0;
        int tabCount = 0;
        int pipeCount = 0;
        int spaceCount = 0;

        for (String line : src) {
            if (line.contains("\t")) {
                tabCount++;
            }
            if (line.contains(",")) {
                commaCount++;
            }
            if (line.contains("|")) {
                pipeCount++;
            }
            if (line.contains(" ")) {
                spaceCount++;
            }
        }
        if (commaCount >= tabCount && commaCount >= pipeCount && commaCount >= spaceCount) {
            return ",";
        } else if (tabCount >= commaCount && tabCount >= pipeCount && tabCount >= spaceCount) {
            return "\t";
        } else if (pipeCount >= tabCount && pipeCount >= commaCount && pipeCount >= spaceCount) {
            return "|";
        } else if (spaceCount >= tabCount && spaceCount >= commaCount && spaceCount >= pipeCount) {
            return " ";
        }

        return " ";
    }

    /* */

    /**
     * Check if the specific mapping exists in current table
     *//*
        * public Mapping isMappingExist(String code, String word) {
        * if (!checkDBConnection()) return null;
        * Mapping munit = null;
        * try {
        * munit = isMappingExistOnDB(db, code, word);
        * } catch (Exception e) {
        * e.printStackTrace();
        * 
        * }
        * 
        * 
        * return munit;
        * 
        * }
        */
    private Mapping isMappingExistOnDB(SQLiteDatabase db, String table, String code, String word)
            {
        if (DEBUG)
            Log.i(TAG, "isMappingExistOnDB(), code = '" + code + "'");
        Mapping munit = null;
        if (code != null && code.trim().length() > 0) {

            Cursor cursor;
            // Process the escape characters of query
            code = code.replace("'", "''");
            if (word == null || word.trim().length() == 0) {
                cursor = db.query(table, null, FIELD_CODE + " = '"
                        + code + "'", null, null, null, null, null);
            } else {
                cursor = db.query(table, null, FIELD_CODE + " = '"
                        + code + "'" + " AND " + FIELD_WORD + " = '"
                        + word + "'", null, null, null, null, null);
            }
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    munit = new Mapping();
                    int idColumn = cursor.getColumnIndex(FIELD_ID);
                    int codeColumn = cursor.getColumnIndex(FIELD_CODE);
                    int wordColumn = cursor.getColumnIndex(FIELD_WORD);
                    int scoreColumn = cursor.getColumnIndex(FIELD_SCORE);
                    // int relatedColumn = cursor.getColumnIndex(FIELD_RELATED);

                    munit.setId(cursor.getString(idColumn));
                    munit.setCode(cursor.getString(codeColumn));
                    munit.setWord(cursor.getString(wordColumn));
                    munit.setScore(cursor.getInt(scoreColumn));
                    // munit.setHighLighted(cursor.getString(relatedColumn));
                    // munit.setHighLighted(false);
                    munit.setExactMatchToCodeRecord();
                    if (DEBUG)
                        Log.i(TAG, "isMappingExistOnDB(), mapping is exist");
                } else if (DEBUG)
                    Log.i(TAG, "isMappingExistOnDB(), mapping is not exist");

                cursor.close();
            }

        }
        return munit;
    }

    /**
     * Jeremy '11,9,8 get Highest socre for 'code'. relatedList will be stored on
     * highest score record after 3.6.
     */
    public int getHighestScore(String word) {

        if (!checkDBConnection())
            return 0;

        int highestScore = 0;
        if (word != null && word.trim().length() > 0) {

            try {
                highestScore = getHighestScoreOnDB(db, word);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return highestScore;

    }

    /**
     * f
     * Jeremy '12,4,6 core of getHightestScore()
     */
    private int getHighestScoreOnDB(SQLiteDatabase db, String word) {
        // '14,12,28 use word instead of code when evaluating scores

        int highestScore = 0;
        if (word != null && word.trim().length() > 0) {

            // Process the escape characters of query
            word = word.replace("'", "''");
            Cursor cursor = db.query(tablename, null, FIELD_WORD + " = '"
                    + word + "'", null, null, null, FIELD_SCORE + " DESC", null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int scoreColumn = cursor.getColumnIndex(FIELD_SCORE);
                    highestScore = cursor.getInt(scoreColumn);
                }
                cursor.close();
            }

        }
        return highestScore;
    }

    /**
     * Jeremy '11,9,8 get Highest socre for 'code'. relatedList will be stored on
     * highest score record after 3.6.
     */
    public int getHighestScoreIDOnDB(SQLiteDatabase db, String table, String code) {
        int ID = -1;
        if (code != null && code.trim().length() > 0) {
            // Process the escape characters of query
            code = code.replace("'", "''");
            Cursor cursor = db.query(table, null, FIELD_CODE + " = '"
                    + code + "'", null, null, null,
                    FIELD_SCORE + " DESC, " + FIELD_BASESCORE + " DESC", null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int idColumn = cursor.getColumnIndex(FIELD_ID);
                    ID = cursor.getInt(idColumn);
                }

                // cursor.deactivate();
                cursor.close();
            }

        }
        return ID;

    }

    /**
     * Check if usesr dictionary record exists
     */
    public Mapping isRelatedPhraseExist(String pword, String cword) {

        long startTime = 0;
        if (DEBUG || probePerformance) {
            startTime = System.currentTimeMillis();
            Log.i(TAG, "isRelatedPhraseExist(): pword='" + pword + ", cword=" + cword);
        }
        if (!checkDBConnection())
            return null;
        Mapping munit = null;

        // SQLiteDatabase db = this.getSqliteDb(true);
        try {
            munit = isRelatedPhraseExistOnDB(db, pword, cword);

        } catch (Exception e) {

            e.printStackTrace();
        }

        if (DEBUG || probePerformance) {

            Log.i(TAG, "isRelatedPhraseExist(): time elapsed = " + (System.currentTimeMillis() - startTime));
        }

        return munit;
    }

    /**
     * Jeremy '12/4/16 core of isUserDictExist()
     */
    private Mapping isRelatedPhraseExistOnDB(SQLiteDatabase db, String pword, String cword) {

        Mapping munit = null;
        if (pword != null && !pword.trim().equals("")) {
            Cursor cursor;

            if (cword == null || cword.trim().equals("")) {
                cursor = db.query(Lime.DB_RELATED, null, FIELD_DIC_pword + " = '"
                        + pword + "'" + " AND " + FIELD_DIC_cword + " IS NULL", null, null, null, null, null);
            } else {
                cursor = db.query(Lime.DB_RELATED, null, FIELD_DIC_pword + " = '"
                        + pword + "'" + " AND " + FIELD_DIC_cword + " = '"
                        + cword + "'", null, null, null, null, null);
            }

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        munit = new Mapping();
                        munit.setId(cursor.getString(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_ID)));
                        munit.setPword(cursor.getString(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_PWORD)));
                        munit.setWord(cursor.getString(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_CWORD)));
                        munit.setBasescore(cursor.getInt(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_BASESCORE)));
                        munit.setScore(cursor.getInt(cursor.getColumnIndex(Lime.DB_RELATED_COLUMN_USERSCORE)));
                        munit.setRelatedPhraseRecord();

                    }
                } finally {
                    cursor.close();
                }
            }

        }
        return munit;
    }

    /**
     *
     */
    public synchronized void resetImInfo(String im) {
        // Jeremy '12,5,1
        if (!checkDBConnection())
            return;
        db.execSQL("DELETE FROM im WHERE code=?", new Object[] { im });

    }

    /**
     *
     */
    public String getImInfo(String im, String field) {
        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return "";

        String iminfo = "";
        try {
            // String value = "";
            String selectString = "SELECT * FROM im WHERE code=? AND title=?";

            Cursor cursor = db.rawQuery(selectString, new String[] { im, field });

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int descCol = cursor.getColumnIndex("desc");
                    iminfo = cursor.getString(descCol);
                }
                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return iminfo;
    }

    /**
     *
     */
    public synchronized void removeImInfo(String im, String field) {
        if (DEBUG)
            Log.i(TAG, "removeImInfo()");
        if (!checkDBConnection())
            return;
        try {
            removeImInfoOnDB(db, im, field);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Jeremy '12,6,7 for working with OnUpgrade() before db is created
     */
    private void removeImInfoOnDB(SQLiteDatabase dbin, String im, String field) {
        if (DEBUG)
            Log.i(TAG, "removeImInfoOnDB()");
        dbin.execSQL("DELETE FROM im WHERE code=? AND title=?", new Object[] { im, field });

    }

    /**
     *
     */
    public synchronized void setImInfo(String im, String field, String value) {
        // Jeremy '12,4,17 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return;

        ContentValues cv = new ContentValues();
        cv.put("code", im);
        cv.put("title", field);
        cv.put("desc", value);

        removeImInfo(im, field);

        db.insert("im", null, cv);

    }

    public List<Im> getImList(String code) {

        if (!checkDBConnection())
            return null;

        List<Im> result = null;
        try {
            // SQLiteDatabase db = this.getSqliteDb(true);
            Cursor cursor = db.query("im", null, Lime.DB_IM_COLUMN_CODE + " = ?", new String[] { code }, null, null,
                    "code ASC", null);
            result = Im.getList(cursor);
            cursor.close();
        } catch (Exception e) {
            Log.i(TAG, "getIm(): Cannot get IM : " + e);
        }
        return result;
    }

    public List<ImObj> getImList() {
        if (DEBUG)
            Log.i(TAG, "getIMList()");
        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return null;

        List<ImObj> result = new LinkedList<>();
        try {
            // SQLiteDatabase db = this.getSqliteDb(true);
            Cursor cursor = db.query("im", null, null, null, null, null, "code ASC", null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String title = cursor.getString(cursor.getColumnIndex("title"));
                        if (title.equals("keyboard")) {
                            ImObj kobj = new ImObj();
                            kobj.setCode(cursor.getString(cursor.getColumnIndex("code")));
                            String kb = cursor.getString(cursor.getColumnIndex("keyboard"));
                            if (kb != null) {
                                if (kb.contains("dayi")) {
                                    kb = "dayi";
                                } else if (kb.contains("phonetic") || kb.contains("hsu") || kb.contains("et26") || kb.contains("et41")) {
                                    kb = "phonetic";
                                }
                            }
                            kobj.setKeyboard(kb);
                            result.add(kobj);
                        }
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }

        } catch (Exception e) {
            Log.i(TAG, "getImList(): Cannot get IM List : " + e);
        }
        return result;
    }

    public KeyboardObj getKeyboardObj(String keyboard) {

        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return null;

        if (keyboard == null || keyboard.equals(""))
            return null;
        KeyboardObj kobj = null;

        try {
            Cursor cursor = db.query("keyboard", null, FIELD_CODE + " = ?", new String[]{keyboard}, null, null,
                    null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    kobj = new KeyboardObj();
                    kobj.setCode(cursor.getString(cursor.getColumnIndex("code")));
                    kobj.setName(cursor.getString(cursor.getColumnIndex("name")));
                    kobj.setDescription(cursor.getString(cursor.getColumnIndex("desc")));
                    kobj.setType(cursor.getString(cursor.getColumnIndex("type")));
                    kobj.setImage(cursor.getString(cursor.getColumnIndex("image")));
                    kobj.setImkb(cursor.getString(cursor.getColumnIndex("imkb")));
                    kobj.setImshiftkb(cursor.getString(cursor.getColumnIndex("imshiftkb")));
                    kobj.setEngkb(cursor.getString(cursor.getColumnIndex("engkb")));
                    kobj.setEngshiftkb(cursor.getString(cursor.getColumnIndex("engshiftkb")));
                    kobj.setSymbolkb(cursor.getString(cursor.getColumnIndex("symbolkb")));
                    kobj.setSymbolshiftkb(cursor.getString(cursor.getColumnIndex("symbolshiftkb")));
                    kobj.setDefaultkb(cursor.getString(cursor.getColumnIndex("defaultkb")));
                    kobj.setDefaultshiftkb(cursor.getString(cursor.getColumnIndex("defaultshiftkb")));
                    kobj.setExtendedkb(cursor.getString(cursor.getColumnIndex("extendedkb")));
                    kobj.setExtendedshiftkb(cursor.getString(cursor.getColumnIndex("extendedshiftkb")));
                }

                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return kobj;
    }

    public String getKeyboardInfo(String keyboardCode, String field) {
        if (DEBUG)
            Log.i(TAG, "getKeyboardInfo()");
        if (!checkDBConnection())
            return null;
        String info = null;
        try {
            info = getKeyboardInfoOnDB(db, keyboardCode, field);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;

    }

    /**
     * Jeremy '12,6,7 for working with OnUpgrade() before db is created
     */
    private String getKeyboardInfoOnDB(SQLiteDatabase dbin, String keyboardCode, String field) {
        if (DEBUG)
            Log.i(TAG, "getKeyboardInfoOnDB()");

        String info = null;

        Cursor cursor = dbin.query("keyboard", null, FIELD_CODE + " = '" + keyboardCode + "'", null, null, null, null,
                null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                info = cursor.getString(cursor.getColumnIndex(field));
            }
            cursor.close();
        }
        if (DEBUG)
            Log.i(TAG, "getKeyboardInfoOnDB() info = " + info);

        return info;
    }

    public List<KeyboardObj> getKeyboardList() {

        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return null;

        List<KeyboardObj> result = new LinkedList<>();
        try {
            // SQLiteDatabase db = this.getSqliteDb(true);
            Cursor cursor = db.query("keyboard", null, null, null, null, null, "name ASC", null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        KeyboardObj kobj = new KeyboardObj();
                        kobj.setCode(cursor.getString(cursor.getColumnIndex("code")));
                        kobj.setName(cursor.getString(cursor.getColumnIndex("name")));
                        kobj.setDescription(cursor.getString(cursor.getColumnIndex("desc")));
                        kobj.setType(cursor.getString(cursor.getColumnIndex("type")));
                        kobj.setImage(cursor.getString(cursor.getColumnIndex("image")));
                        kobj.setImkb(cursor.getString(cursor.getColumnIndex("imkb")));
                        kobj.setImshiftkb(cursor.getString(cursor.getColumnIndex("imshiftkb")));
                        kobj.setEngkb(cursor.getString(cursor.getColumnIndex("engkb")));
                        kobj.setEngshiftkb(cursor.getString(cursor.getColumnIndex("engshiftkb")));
                        kobj.setSymbolkb(cursor.getString(cursor.getColumnIndex("symbolkb")));
                        kobj.setSymbolshiftkb(cursor.getString(cursor.getColumnIndex("symbolshiftkb")));
                        kobj.setDefaultkb(cursor.getString(cursor.getColumnIndex("defaultkb")));
                        kobj.setDefaultshiftkb(cursor.getString(cursor.getColumnIndex("defaultshiftkb")));
                        kobj.setExtendedkb(cursor.getString(cursor.getColumnIndex("extendedkb")));
                        kobj.setExtendedshiftkb(cursor.getString(cursor.getColumnIndex("extendedshiftkb")));
                        result.add(kobj);
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public synchronized void setIMKeyboard(String im, String value,
            String keyboard) {
        if (DEBUG)
            Log.i(TAG, "setIMKeyboard() im=" + im + " value= " + value + " keyboard= " + keyboard);
        if (!checkDBConnection())
            return;
        try {
            setIMKeyboardOnDB(db, im, value, keyboard);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Jeremy '12,6,7 for working with OnUpgrade() before db is created
     */
    private void setIMKeyboardOnDB(SQLiteDatabase dbin, String im, String value, String keyboard) {
        if (DEBUG)
            Log.i(TAG, "setIMKeyboardOnDB()");
        ContentValues cv = new ContentValues();
        cv.put("code", im);
        cv.put("title", "keyboard");
        cv.put("desc", value);
        cv.put("keyboard", keyboard);

        removeImInfoOnDB(dbin, im, "keyboard");

        dbin.insert("im", null, cv);
    }

    public String getKeyboardCode(String im) {
        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return "";

        Cursor cursor = null;
        try {
            String selectString = "SELECT * FROM im WHERE code=? AND title='keyboard'";
            // SQLiteDatabase db = this.getSqliteDb(true);

            cursor = db.rawQuery(selectString, new String[] { im });

            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                int descCol = cursor.getColumnIndex("keyboard");

                return cursor.getString(descCol);
            }

        } catch (Exception ignored) {
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return "";
    }

    public List<String> getEnglishSuggestions(String word) {

        // Jeremy '12,5,1 !checkDBConnection() when db is restoring or replaced.
        if (!checkDBConnection())
            return null;

        List<String> result = new ArrayList<>();
        try {
            // String value = "";
            int ssize = mLIMEPref.getSimilarCodeCandidates();
            char[] sourcechars = word.toCharArray();
            /*
             * stemmer = new Stemmer();
             * for(char c: sourcechars){
             * stemmer.add(c);
             * }
             * stemmer.stem();
             */
            String selectString = "SELECT word FROM dictionary WHERE word MATCH '" + word + "*' AND word <> '" + word
                    + "'ORDER BY word ASC LIMIT " + ssize + ";";
            // SQLiteDatabase db = this.getSqliteDb(true);

            Cursor cursor = db.rawQuery(selectString, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        String w = cursor.getString(cursor.getColumnIndex("word"));
                        if (w != null && !w.equals("")) {
                            result.add(w);
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    public List<Mapping> emojiConvert(String source, int emoji) {
        checkEmojiDB();
        return emojiConverter.convert(source, emoji);
    }

    /**
     * Jeremy '11,9,8 moved from searchService
     */
    public String hanConvert(String input, int hanOption) {
        checkHanDB();
        return hanConverter.convert(input, hanOption);
    }

    /**
     * Jeremy '11,9,8 get basescore of word store in hanconverter
     */
    public int getBaseScore(String input) {
        checkHanDB();
        return hanConverter.getBaseScore(input);

    }

    private void checkEmojiDB() {
        if (emojiConverter == null) {

            File emojiDBFile = mContext.getDatabasePath("emoji.db");
            if (!emojiDBFile.getParentFile().exists()) {
                emojiDBFile.getParentFile().mkdirs();
            }

            if (!emojiDBFile.exists())
                LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.emoji), emojiDBFile);

            emojiConverter = new EmojiConverter(mContext);
        }
    }

    private void checkHanDB() {
        if (hanConverter == null) {

            // Jeremy '11,9,8 update handconverdb to v2 with base score in TCSC table
            File hanDBFile = mContext.getDatabasePath("hanconvert.db");
            if (hanDBFile.exists())
                hanDBFile.delete();
            
            File hanDBV2File = mContext.getDatabasePath("hanconvertv2.db");
            if (!hanDBV2File.getParentFile().exists()) {
                hanDBV2File.getParentFile().mkdirs();
            }

            if (DEBUG)
                Log.i(TAG, "LimeDB: checkHanDB(): hanDBV2Filepaht:" + hanDBV2File.getAbsolutePath());

            if (!hanDBV2File.exists())
                LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.hanconvertv2), hanDBV2File);
            else { // Jeremy '11,9,14 copy the db file if it's newer.
                if (mLIMEPref.getParameterLong("hanDBDate") != hanDBV2File.lastModified())
                    LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.hanconvertv2), hanDBV2File);
            }

            hanConverter = new LimeHanConverter(mContext);
        }
    }

    /**
     * This is the method to rename the table name in database
     */
    public boolean renameTableName(String source, String target) {
        if (!checkDBConnection())
            return false;

        try {
            // ALTER TABLE foo RENAME TO bar
            db.execSQL("ALTER TABLE " + source + " RENAME TO " + target);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // Method from DataSource

    public void beginTransaction() {
        if (db != null && db.isOpen()) {
            db.beginTransaction();
        }
    }

    public void endTransaction() {
        if (db != null && db.isOpen()) {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    /**
     * 取得表格內的所有記錄
     */
    public Cursor list(String table) {
        Cursor cursor = null;
        if (db != null && db.isOpen()) {
            cursor = db.query(table, null, null, null, null, null, null);
        }
        return cursor;
    }

    /**
     * 依 SQL 指令進行資料新增
     */
    public void insert(String insertsql) {
        if (db != null && db.isOpen() &&
                insertsql != null && insertsql.toLowerCase().trim().startsWith("insert")) {
            db.execSQL(insertsql);
        }
    }

    public void add(String addsql) {
        if (db != null && db.isOpen()) {
            if (addsql.toLowerCase().startsWith("insert")) {
                db.execSQL(addsql);
            }
        }
    }

    /**
     * 移除 SQL 指令的操作
     */
    public void remove(String removesql) {
        if (!checkDBConnection())
            return;

        if (removesql.toLowerCase().startsWith("delete")) {
            db.execSQL(removesql);
        }

    }

    /**
     * Safe removal by ID using parameterized query to prevent SQL injection
     * 
     * @param table Table name to delete from
     * @param id    The ID of the record to delete
     * @return Number of rows affected
     */
    public int removeById(String table, String id) {
        if (!checkDBConnection())
            return 0;

        return db.delete(table, Lime.DB_COLUMN_ID + " = ?", new String[] { id });
    }

    public void update(String updatesql) {
        if (!checkDBConnection())
            return;

        if (updatesql.toLowerCase().startsWith("update")) {
            db.execSQL(updatesql);
        }

    }

    public List<Keyboard> getKeyboard() {
        List<Keyboard> result = new ArrayList<>();
        if (!checkDBConnection())
            return result;

        // Filter to only show Dayi English and Phonetic keyboards
        String selection = Lime.DB_KEYBOARD_COLUMN_CODE + " = ? OR " + Lime.DB_KEYBOARD_COLUMN_CODE + " = ?";
        String[] selectionArgs = { "dayi", "phonetic" };
        Cursor cursor = db.query(Lime.DB_KEYBOARD, null, selection, selectionArgs,
                null, null, Lime.DB_KEYBOARD_COLUMN_NAME + " ASC");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Keyboard r = Keyboard.get(cursor);
            result.add(r);
            cursor.moveToNext();
        }
        cursor.close();

        return result;
    }

    public List<Im> getIm(String code, String type) {

        List<Im> result = new ArrayList<>();
        if (!checkDBConnection())
            return result;

        Cursor cursor;
        String query = null;
        if (code != null && code.length() > 1) {
            query = Lime.DB_IM_COLUMN_CODE + "='" + code + "'";
        }
        if (type != null && type.length() > 1) {
            if (query != null) {
                query += " AND ";
            } else {
                query = "";
            }

            query += " " + Lime.DB_IM_COLUMN_TITLE + "='" + type + "'";
        }

        cursor = db.query(Lime.DB_IM,
                null, query,
                null, null, null, Lime.DB_IM_COLUMN_DESC + " ASC");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Im r = Im.get(cursor);
            result.add(r);
            cursor.moveToNext();
        }
        cursor.close();

        return result;
    }

    public List<Word> loadWord(String code, String query, boolean searchroot, int maximum, int offset) {
        List<Word> result = new ArrayList<>();
        if (!checkDBConnection())
            return result;

        Cursor cursor;
        if (query != null && query.length() >= 1) {
            if (searchroot) {
                query = Lime.DB_COLUMN_CODE + " LIKE '" + query + "%' AND ifnull(" + Lime.DB_COLUMN_WORD
                        + ", '') <> ''";
            } else {
                query = Lime.DB_COLUMN_WORD + " LIKE '%" + query + "%' AND ifnull(" + Lime.DB_COLUMN_WORD
                        + ", '') <> ''";
            }
        } else {
            query = "ifnull(" + Lime.DB_COLUMN_WORD + ", '') <> ''";
        }

        String order;

        if (searchroot) {
            order = Lime.DB_COLUMN_CODE + " ASC";
        } else {
            order = Lime.DB_COLUMN_WORD + " ASC";
        }

        if (maximum > 0) {
            order += " LIMIT " + maximum + " OFFSET " + offset;
        }

        cursor = db.query(code,
                null, query,
                null, null, null, order);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Word r = Word.get(cursor);
            result.add(r);
            cursor.moveToNext();
        }
        cursor.close();

        return result;
    }

    public Word getWord(String code, long id) {
        if (!checkDBConnection())
            return null;
        Word w;
        Cursor cursor;

        String query = Lime.DB_COLUMN_ID + " = '" + id + "' ";

        cursor = db.query(code,
                null, query,
                null, null, null, null);

        cursor.moveToFirst();
        w = Word.get(cursor);
        cursor.close();
        return w;
    }

    public void setImKeyboard(String code, Keyboard keyboard) {
        if (!checkDBConnection())
            return;

        String removesql = "DELETE FROM " + Lime.DB_IM + " WHERE " + Lime.DB_IM_COLUMN_CODE + " = '" + code + "'";
        removesql += " AND " + Lime.DB_IM_COLUMN_TITLE + " = '" + Lime.IM_TYPE_KEYBOARD + "'";
        db.execSQL(removesql);

        Im im = new Im();
        im.setCode(code);
        im.setKeyboard(keyboard.getCode());
        im.setTitle(Lime.IM_TYPE_KEYBOARD);
        im.setDesc(keyboard.getDesc());

        String addsql = Im.getInsertQuery(im);
        db.execSQL(addsql);

    }

    public Keyboard getImKeyboard(String code) {
        if (!checkDBConnection())
            return null;

        String query = Lime.DB_IM_COLUMN_CODE + " = '" + code + "' AND " +
                Lime.DB_IM_COLUMN_TITLE + " = '" + Lime.IM_TYPE_KEYBOARD + "'";
        Cursor cursor = db.query(Lime.DB_IM, null, query, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String keyboardCode = cursor.getString(cursor.getColumnIndex(Lime.DB_IM_COLUMN_KEYBOARD));
            cursor.close();

            // Get the full keyboard object
            List<Keyboard> keyboards = getKeyboard();
            for (Keyboard kb : keyboards) {
                if (kb.getCode().equals(keyboardCode)) {
                    return kb;
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    public int hasRelated(String pword, String cword) {

        try {
            Cursor cursor;

            String query = "";
            if (pword != null && !pword.isEmpty() && cword != null && !cword.isEmpty()) {
                query = Lime.DB_RELATED_COLUMN_PWORD + " = '" + pword + "' AND ";
                query += Lime.DB_RELATED_COLUMN_CWORD + " = '" + cword + "'";
            }

            cursor = db.query(Lime.DB_RELATED,
                    null, query,
                    null, null, null, null);

            int id = 0;
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Related r = Related.get(cursor);
                id = r.getId();
                cursor.moveToNext();
            }
            cursor.close();

            return id;
        } catch (SQLiteException sqe) {
            return 9999999;
        }
    }

    public List<Related> loadRelated(String pword, int maximum, int offset) {

        List<Related> result = new ArrayList<>();
        if (!checkDBConnection())
            return result;

        Cursor cursor;

        String query = "";
        String cword = "";

        if (pword != null && pword.length() > 1) {
            cword = pword.substring(1);
            pword = pword.substring(0, 1);
        }
        if (pword != null && !pword.isEmpty()) {
            query = Lime.DB_RELATED_COLUMN_PWORD + " = '" + pword +
                    "' AND ";
        }
        if (cword != null && !cword.isEmpty()) {
            query += Lime.DB_RELATED_COLUMN_CWORD + " LIKE '" + cword +
                    "%' AND ";
        }

        query += "ifnull(" + Lime.DB_RELATED_COLUMN_CWORD + ", '') <> ''";

        String order = Lime.DB_RELATED_COLUMN_USERSCORE + " desc," + Lime.DB_RELATED_COLUMN_BASESCORE + " desc";

        if (maximum > 0) {
            order += " LIMIT " + maximum + " OFFSET " + offset;
        }

        cursor = db.query(Lime.DB_RELATED,
                null, query,
                null, null, null, order);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Related r = Related.get(cursor);
            result.add(r);
            cursor.moveToNext();
        }
        cursor.close();

        return result;
    }

    public Related getRelated(long id) {
        if (!checkDBConnection())
            return null;
        Related w;
        Cursor cursor;

        String query = Lime.DB_RELATED_COLUMN_ID + " = '" + id + "' ";

        cursor = db.query(Lime.DB_RELATED,
                null, query,
                null, null, null, null);

        cursor.moveToFirst();
        w = Related.get(cursor);
        cursor.close();

        return w;
    }

    public int count(String table) {

        if (!checkDBConnection())
            return 0;
        int total;

        Cursor cursor;
        String query = "SELECT COUNT(*) as count FROM " + table;
        cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        total = cursor.getInt(cursor.getColumnIndex(Lime.DB_TOTAL_COUNT));
        cursor.close();

        return total;

    }

    public int getWordSize(String table, String curquery, boolean searchroot) {

        if (!checkDBConnection())
            return 0;

        int total;

        Cursor cursor;

        String query = "SELECT COUNT(*) as count FROM " + table + " WHERE ";

        if (curquery != null && curquery.length() >= 1) {
            if (searchroot) {
                query += Lime.DB_COLUMN_CODE + " LIKE '" + curquery + "%' AND ifnull(" + Lime.DB_COLUMN_WORD
                        + ", '') <> ''";
            } else {
                query += Lime.DB_COLUMN_WORD + " LIKE '%" + curquery + "%' AND ifnull(" + Lime.DB_COLUMN_WORD
                        + ", '') <> ''";
            }
        } else {
            query += " ifnull(" + Lime.DB_COLUMN_WORD + ", '') <> ''";
        }

        cursor = db.rawQuery(query, null);

        cursor.moveToFirst();
        total = cursor.getInt(cursor.getColumnIndex(Lime.DB_TOTAL_COUNT));
        cursor.close();
        return total;

    }

    public int getRelatedSize(String pword) {

        if (!checkDBConnection())
            return -1;
        int total;

        Cursor cursor;

        String query = "SELECT COUNT(*) as count FROM " + Lime.DB_RELATED + " WHERE ";

        String cword = "";
        if (pword != null && !pword.isEmpty()) {
            cword = pword.substring(1);
            pword = pword.substring(0, 1);
        }

        if (pword != null && !pword.isEmpty()) {
            query += Lime.DB_RELATED_COLUMN_PWORD + " = '" + pword +
                    "' AND ";
        }
        if (cword != null && !cword.isEmpty()) {
            query += Lime.DB_RELATED_COLUMN_CWORD + " LIKE '" + cword + "%' AND ";
        }

        query += "ifnull(" + Lime.DB_RELATED_COLUMN_CWORD + ", '') <> ''";

        cursor = db.rawQuery(query, null);

        cursor.moveToFirst();
        total = cursor.getInt(cursor.getColumnIndex(Lime.DB_TOTAL_COUNT));
        cursor.close();

        return total;
    }

    public void insert(String table, ContentValues cv) {
        if (!checkDBConnection())
            return;
        db.insert(table, null, cv);

    }

    public Cursor query(String table, String where) {
        if (!checkDBConnection())
            return null;

        return db.query(table, null, where, null, null, null, null, null);

    }

    // Hold database connection to prevent further transactions when database is in
    // maintenance. Jeremy '15,5,23
    public void holdDBConnection() {
        databaseOnHold = true;
    }

    public void unHoldDBConnection() {
        databaseOnHold = false;
    }

    public boolean isDatabseOnHold() {
        return databaseOnHold;
    }

    public void updateBackupScore(String imtype, List<Word> scorelist) {
        if (!checkDBConnection())
            return;
        db.beginTransaction();
        try {
            for (Word w : scorelist) {
                String updatesql = Word.getUpdateScoreQuery(imtype, w);
                db.execSQL(updatesql);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Cursor rawQuery(String query) {
        if (!checkDBConnection())
            return null;
        try {
            return db.rawQuery(query, null);
        } catch (Exception e) {
            Log.w(TAG, "Ignore all possible exceptions~");
        }
        return null;
    }

    public void execSQL(String insertsql) {
        if (!checkDBConnection())
            return;
        try {
            db.execSQL(insertsql);
        } catch (Exception e) {
            Log.w(TAG, "Ignore all possible exceptions~");
        }
    }

    public void resetLimeSetting() {

        if (db != null)
            db.close();

        File dbFile = new File(Lime.getDatabaseDeviceFolder(mContext) + File.separator + Lime.DATABASE_NAME);
        dbFile.deleteOnExit();
        LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.lime), dbFile);
        openDBConnection(true);

        if (emojiConverter != null)
            emojiConverter.close();

        emojiConverter = null;
        File emojiDbFile = new File(mContext.getFilesDir().getParentFile().getPath() + "/databases/emoji.db");
        emojiDbFile.deleteOnExit();
        LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.emoji), emojiDbFile);
        emojiConverter = new EmojiConverter(mContext);

        if (hanConverter != null)
            hanConverter.close();

        hanConverter = null;
        File hanDBFile = new File(mContext.getFilesDir().getParentFile().getPath() + "/databases/hanconvert.db");
        hanDBFile.deleteOnExit();
        File hanDB2File = new File(mContext.getFilesDir().getParentFile().getPath() + "/databases/hanconvertv2.db");
        hanDB2File.deleteOnExit();

        LIMEUtilities.copyRAWFile(mContext.getResources().openRawResource(R.raw.hanconvertv2), hanDB2File);
        hanConverter = new LimeHanConverter(mContext);

    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS memo (" +
                       "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                       "content TEXT NOT NULL, " +
                       "pinned INTEGER DEFAULT 0, " +
                       "created_at INTEGER);");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create/ensure memo table", e);
        }
    }

    public long insertMemo(String content, int pinned) {
        if (!checkDBConnection())
            return -1;
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        cv.put("pinned", pinned);
        cv.put("created_at", System.currentTimeMillis());
        return db.insert("memo", null, cv);
    }

    public List<MemoObj> getMemos() {
        List<MemoObj> list = new ArrayList<>();
        if (!checkDBConnection())
            return list;
        Cursor cursor = null;
        try {
            cursor = db.query("memo", null, null, null, null, null, "pinned DESC, created_at DESC");
            if (cursor != null && cursor.moveToFirst()) {
                int idIdx = cursor.getColumnIndex("_id");
                int contentIdx = cursor.getColumnIndex("content");
                int pinnedIdx = cursor.getColumnIndex("pinned");
                int createdIdx = cursor.getColumnIndex("created_at");
                do {
                    MemoObj obj = new MemoObj();
                    obj.setId(cursor.getInt(idIdx));
                    obj.setContent(cursor.getString(contentIdx));
                    obj.setPinned(cursor.getInt(pinnedIdx));
                    obj.setCreatedAt(cursor.getLong(createdIdx));
                    list.add(obj);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "getMemos error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public int deleteMemo(int id) {
        if (!checkDBConnection())
            return -1;
        return db.delete("memo", "_id = ?", new String[]{String.valueOf(id)});
    }

    public int updateMemoPin(int id, int pinned) {
        if (!checkDBConnection())
            return -1;
        ContentValues cv = new ContentValues();
        cv.put("pinned", pinned);
        return db.update("memo", cv, "_id = ?", new String[]{String.valueOf(id)});
    }

    public int updateMemoContent(int id, String content) {
        if (!checkDBConnection())
            return -1;
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        return db.update("memo", cv, "_id = ?", new String[]{String.valueOf(id)});
    }
}
