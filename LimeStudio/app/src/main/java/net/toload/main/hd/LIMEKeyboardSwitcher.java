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

package net.toload.main.hd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.toload.main.hd.data.ImObj;
import net.toload.main.hd.data.KeyboardObj;
import net.toload.main.hd.global.LIMEPreferenceManager;
import net.toload.main.hd.keyboard.LIMEKeyboard;
import net.toload.main.hd.keyboard.LIMEKeyboardView;

public class LIMEKeyboardSwitcher {

    public static final int MODE_TEXT = 1;
    public static final int MODE_SYMBOLS = 2;
    public static final int MODE_PHONE = 3;
    public static final int MODE_URL = 4;
    public static final int MODE_EMAIL = 5;
    public static final int MODE_IM = 6;
    public static final int MODE_NUMBER = 7;
    public static final int MODE_TEXT_QWERTY = 0;
    public static final int MODE_TEXT_ALPHA = 1;
    public static final int MODE_TEXT_COUNT = 2;
    public static final int KEYBOARDMODE_NORMAL = R.id.mode_normal;
    public static final int KEYBOARDMODE_URL = R.id.mode_url;
    public static final int KEYBOARDMODE_EMAIL = R.id.mode_email;
    public static final int KEYBOARDMODE_IM = R.id.mode_im;
    public static final int IM_KEYBOARD = 0;
    static final boolean DEBUG = false;
    static final String TAG = "LIMEKeyboardSwitcher";
    private static final int SYMBOLS_KEYBOARD_1 = 1;
    private static final int SYMBOLS_KEYBOARD_2 = 2;
    private static final int SYMBOLS_KEYBOARD_3 = 3;
    private static List<String> mActivatedIMList;
    private static List<String> mActivatedIMShortnameList;

    private static Map<String, Integer> imMap = new HashMap<>();
    private final Map<KeyboardId, LIMEKeyboard> mKeyboards;
    private final int mTextMode = MODE_TEXT_QWERTY;
    private final LIMEPreferenceManager mLIMEPref;
    private final boolean mIsAlphabet = false;
    LIMEKeyboardView mInputView;
    LIMEService mService;
    Context mThemedContext;
    private int mMode = KEYBOARDMODE_NORMAL;
    private int mImeOptions;
    private boolean mIsShifted;
    private boolean mIsSymbols;
    private boolean mIsChinese = true;
    private boolean mPreferSymbols;
    private int mCurrentSymbolsKeyboard = SYMBOLS_KEYBOARD_1;
    private int mLastDisplayWidth;
    private String imtype = null;
    private HashMap<String, KeyboardObj> kbHm;
    private HashMap<String, String> imHm; // 暫存已下載字根的輸入法
    private float mKeySizeScale = 1;

    private int actionIM = -1;

    public LIMEKeyboardSwitcher(LIMEService service, Context themedContext) {
        mService = service;
        mThemedContext = themedContext;

        mLIMEPref = new LIMEPreferenceManager(service);
        mKeyboards = new HashMap<>();

        mKeySizeScale = mLIMEPref.getFontSize();
    }

    public static LIMEKeyboardSwitcher getInstance() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setThemedContext(Context context) {
        mThemedContext = context;
    }

    public int getKeyboardSize() {
        if (kbHm != null) {
            return kbHm.size();
        }
        return 0;
    }

    public void setKeyboardList(List<KeyboardObj> list) {
        if (list == null || (list.size() == 0))
            return; // Jeremy '12,4,10 avoid fc when database is locked.
        kbHm = new HashMap<>();
        for (KeyboardObj o : list) {
            kbHm.put(o.getCode(), o);
        }
    }

    public String getImKeyboard(String code) {
        if (imHm != null && imHm.get(code) != null) {
            return imHm.get(code);
        }
        return "";
    }

    //
    public void setImList(List<ImObj> list) {
        if (list == null || list.size() == 0)
            return; // Jeremy '12,4,10 avoid fc when database is locked.
        imHm = new HashMap<>();
        for (ImObj o : list) {
            imHm.put(o.getCode(), o.getKeyboard());
        }
    }

    public void setActivatedIMList(List<String> codes, List<String> names, List<String> shortnames) {
        if (DEBUG)
            Log.d(TAG, "setActiveKeyboardList()");

        if (codes.equals(mActivatedIMList) && shortnames.equals(mActivatedIMShortnameList))
            return;

        mActivatedIMList = codes;
        mActivatedIMShortnameList = shortnames;
    }

    public List<String> getActivatedIMShortnameList() {
        return mActivatedIMShortnameList;
    }

    @SuppressLint("SuspiciousIndentation")
    public String getActiveIMShortname() {
        if (DEBUG)
            Log.i(TAG, "getCurrentActiveKeyboardShortName() current IM:" + imtype);
        for (int i = 0; i < mActivatedIMList.size(); i++) {
            if (imtype.equals(mActivatedIMList.get(i))) {
                if (DEBUG)
                    Log.i(TAG, "getCurrentActiveKeyboardShortName()=" + mActivatedIMShortnameList.get(i));
                actionIM = i;
                return mActivatedIMShortnameList.get(i);
            }
        }
        return "";
    }

    /**
     * Returns a single-character abbreviation for the current active input method.
     * Used for the IM switch key label.
     * 
     * @return Single character like "易" for dayi, "注" for phonetic
     */
    public String getActiveIMSingleChar() {
        if (imtype == null)
            return "中";
        switch (imtype) {
            case "dayi":
                return "易";
            case "phonetic":
                return "注";
            default:
                return "中";
        }
    }

    public String getNextActivatedIMShortname() {
        for (int i = 0; i < mActivatedIMList.size(); i++) {
            if (imtype.equals(mActivatedIMList.get(i))) {
                if (i == mActivatedIMList.size() - 1)
                    return mActivatedIMShortnameList.get(0);
                else
                    return mActivatedIMShortnameList.get(i + 1);
            }
        }
        return "";
    }

    public String getPrevActivatedIMShortname() {

        if (mActivatedIMList.size() == 2) {
            for (int i = 0; i < mActivatedIMList.size(); i++) {
                if (imtype.equals(mActivatedIMList.get(i))) {
                    if (DEBUG)
                        Log.i(TAG, "getCurrentActiveKeyboardShortName()=" + mActivatedIMShortnameList.get(i));
                    actionIM = i;
                    return mActivatedIMShortnameList.get(i);
                }
            }
        } else {
            for (int i = 0; i < mActivatedIMList.size(); i++) {
                if (imtype.equals(mActivatedIMList.get(i))) {
                    if (i == 0)
                        return mActivatedIMShortnameList.get(mActivatedIMList.size() - 1);
                    else
                        return mActivatedIMShortnameList.get(i - 1);
                }
            }
        }
        return "";
    }

    public void setInputView(LIMEKeyboardView inputView) {
        mInputView = inputView;
    }

    public void clearKeyboards() {
        if (DEBUG)
            Log.i(TAG, "clearkeyboards()");
        if (mKeyboards != null) {
            mKeyboards.clear();
        }
    }

    public void resetKeyboards(boolean forceCreate) {
        if (DEBUG)
            Log.i(TAG, "resetKeyboards(): forceCreate:" + forceCreate);
        if (forceCreate)
            clearKeyboards();
        // Configuration change is coming after the keyboard gets recreated. So don't
        // rely on that.
        // If keyboards have already been made, check if we have a screen width change
        // and
        // create the keyboard layouts again at the correct orientation
        int displayWidth = mService.getMaxWidth();
        if (displayWidth != mLastDisplayWidth) {
            mLastDisplayWidth = displayWidth;
            clearKeyboards();
        }
    }

    private LIMEKeyboard getKeyboard(KeyboardId id) {
        if (DEBUG)
            Log.i(TAG, "getKeyboard()");
        // Jeremy '11,9,3
        if (mLIMEPref.getKeyboardSize() != mKeySizeScale) {
            clearKeyboards();
            mKeySizeScale = mLIMEPref.getKeyboardSize();
        }
        if (id != null) {
            if (!mKeyboards.containsKey(id)) {
                if (DEBUG)
                    Log.i(TAG, "getKeyboard() keyboard for id, " + id + ", is not exist. create one now.");
                LIMEKeyboard keyboard = new LIMEKeyboard(
                        mThemedContext, id.mXml, id.mMode, mKeySizeScale,
                        mLIMEPref.getShowArrowKeys(), // Jeremy '12,5,21 add the show arrow keys option
                        mLIMEPref.getSplitKeyboard(),
                        mService.getMaxWidth() // Jeremy '26,1,7 Pass max width to prevent overflow
                ); // Jeremy '12,5,27 add the split keyboard option
                keyboard.setKeyboardSwitcher(this);
                if (id.mEnableShiftLock) {
                    keyboard.enableShiftLock();
                }
                mKeyboards.put(id, keyboard);
            }
            return mKeyboards.get(id);
        }
        return null;
    }

    private int getKeyboardXMLID(String value) {
        if (value == null || value.isEmpty()) {
            Log.w(TAG, "getKeyboardXMLID: value is null or empty, falling back to 'lime'");
            return mThemedContext.getResources().getIdentifier("lime", "xml", mService.getPackageName());
        }
        int id = mThemedContext.getResources().getIdentifier(value, "xml", mService.getPackageName());
        if (id == 0) {
            Log.w(TAG, "getKeyboardXMLID: resource '" + value + "' not found, falling back to 'lime'");
            id = mThemedContext.getResources().getIdentifier("lime", "xml", mService.getPackageName());
        }
        return id;
    }

    public void setKeyboardMode(String code, int mode, int imeOptions, boolean isIm, boolean isSymbol,
            boolean isShift) {
        if (DEBUG) {
            Log.d(TAG, "setKeyboardMode () code:" + code + ", mode:" + mode + ", imOptions:" + imeOptions + "" +
                    ", isIM:" + isIm + ", isSymbol:" + isSymbol + ", isShift:" + isShift);
        }
        
        imtype = code;

        // Jeremy '11,6,2. Has to preserve these options for toggle keyboard controls.
        mImeOptions = imeOptions;
        if (isSymbol && !mIsSymbols)
            mCurrentSymbolsKeyboard = SYMBOLS_KEYBOARD_1; // reset the symbol keyboard to first one if it's switching
                                                          // from non-symbol keyboards
        mIsSymbols = isSymbol;
        mIsShifted = isShift;
        if (mode != 0)
            mMode = mode;

        String imcode = "";
        if (imHm != null) {
            imcode = imHm.get(code);
            if (imcode != null) {
                if (imcode.contains("dayi")) {
                    imcode = "dayi";
                } else if (imcode.contains("phonetic") || imcode.contains("hsu") || imcode.contains("et26") || imcode.contains("et41")) {
                    imcode = "phonetic";
                }
            }
        }

        KeyboardObj kobj = null;

        if (imcode == null || imcode.equals("") || imcode.equals("custom")) {
            // Try to use the code directly if it exists in keyboards map, 
            // otherwise fallback to default 'lime'
            if (kbHm != null && kbHm.containsKey(code)) {
                imcode = code;
                kobj = kbHm.get(imcode);
            } else {
                imcode = "lime";
                if (kbHm != null)
                    kobj = kbHm.get(imcode);
            }
        } else {
            if (kbHm != null) {
                kobj = kbHm.get(imcode);
                if (kobj == null) {
                    if (imcode.contains("dayi")) {
                        kobj = kbHm.get("dayi");
                    } else if (imcode.contains("phonetic")) {
                        kobj = kbHm.get("phonetic");
                    }
                }
            }
        }

        android.util.Log.d("LIME_KBD", "setKeyboardMode: code=" + code
                + " imcode=" + imcode
                + " kbHm=" + (kbHm == null ? "null" : "size=" + kbHm.size() + " keys=" + kbHm.keySet())
                + " imHm=" + (imHm == null ? "null" : "size=" + imHm.size() + " val=" + (imHm.get(code)))
                + " kobj=" + (kobj == null ? "null" : kobj.getCode()));

        KeyboardId kid = null;

        if (kobj == null) {
            // Fallback: If no keyboard object found, try to use default "lime" keyboard
            // from database.
            if (kbHm != null && kbHm.containsKey("lime")) {
                kobj = kbHm.get("lime");
            } else {
                Log.e(TAG, "setKeyboardMode: Critical failure - kobj is null and 'lime' keyboard missing!");
                // Emergency fallback kid if we have NOTHING
                kid = new KeyboardId(getKeyboardXMLID("lime"), KEYBOARDMODE_NORMAL, true);
            }
        }

        // Only run main logic if kid hasn't been set by emergency fallback yet
        boolean isTablet = mThemedContext.getResources().getBoolean(R.bool.is_tablet);
        android.util.Log.d("LIME_KBD", "setKeyboardMode: isTablet=" + isTablet);

        if (kid == null && kobj != null) {

            mIsChinese = false;
            if (mode == MODE_NUMBER) {
                kid = new KeyboardId(getKeyboardXMLID("numeric"));
            } else if (isSymbol) {
                switch (mCurrentSymbolsKeyboard) {
                    case SYMBOLS_KEYBOARD_1:
                    default:
                        kid = new KeyboardId(getKeyboardXMLID("symbols1"));
                        break;
                    case SYMBOLS_KEYBOARD_2:
                        kid = new KeyboardId(getKeyboardXMLID("symbols2"));
                        break;
                    case SYMBOLS_KEYBOARD_3:
                        kid = new KeyboardId(getKeyboardXMLID("symbols3"));
                        break;
                }
            } else {
                switch (mode) {
                    case MODE_PHONE:
                        // Log.i("ART","KBMODE ->: phone");
                        kid = new KeyboardId(getKeyboardXMLID("phone"));
                        break;
                    case MODE_URL:
                        // Log.i("ART","KBMODE ->: url");
                        if (!imcode.equals("wb")) {
                            if (isTablet || mLIMEPref.getShowNumberRowInEnglish()) {
                                kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_number_shift" : "lime_number"),
                                        KEYBOARDMODE_URL, true);
                            } else {
                                kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_shift" : "lime"),
                                        KEYBOARDMODE_URL, true);
                            }
                        } else {
                            kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_shift" : "lime"),
                                    KEYBOARDMODE_URL, true);
                        }
                        break;
                    case MODE_EMAIL:
                        // Log.i("ART","KBMODE ->: email");
                        if (!imcode.equals("wb")) {
                            if (isTablet || mLIMEPref.getShowNumberRowInEnglish()) {
                                kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_number_shift" : "lime_number"),
                                        KEYBOARDMODE_EMAIL, true);
                            } else {
                                kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_shift" : "lime"),
                                        KEYBOARDMODE_EMAIL, true);
                            }
                        } else {
                            kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_shift" : "lime"),
                                    KEYBOARDMODE_URL, true);
                        }
                        break;
                    default:
                        if (isIm) { // Chinese IM keyboards
                            if (isShift) {
                                kid = new KeyboardId(getKeyboardXMLID(kobj.getImshiftkb()), KEYBOARDMODE_NORMAL, true);
                            } else {
                                kid = new KeyboardId(getKeyboardXMLID(kobj.getImkb()), KEYBOARDMODE_NORMAL, true);
                            }
                            mIsChinese = true;
                        } else {// if(!isIm){ //English normal keyboard

                            if (!imcode.equals("wb")) {
                                if (isTablet || mLIMEPref.getShowNumberRowInEnglish()) {
                                    kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_number_shift" : "lime_number"),
                                            KEYBOARDMODE_NORMAL, true);
                                } else {
                                    kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_shift" : "lime"),
                                            KEYBOARDMODE_NORMAL, true);
                                }
                            } else {
                                kid = new KeyboardId(getKeyboardXMLID(isShift ? "lime_shift" : "lime"),
                                        KEYBOARDMODE_NORMAL, true);
                            }
                        }

                }
            }

            if (mInputView == null)
                return;

            LIMEKeyboard keyboard = getKeyboard(kid);

            // Jeremy '24,1,7: Dynamic Space Bar Label & Dayi Hints
            List<net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key> keys = keyboard.getKeys();
            for (net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key key : keys) {
                // 1. Space Bar Label
                if (key.codes[0] == net.toload.main.hd.keyboard.LIMEBaseKeyboard.KEYCODE_SPACE) {
                    if (mode == MODE_NUMBER) {
                        key.label = null;
                        key.icon = mThemedContext.getResources().getDrawable(R.drawable.sym_keyboard_spacebar_numeric_material3);
                    } else {
                        key.icon = null;
                        if (isIm) {
                            key.label = getActiveIMShortname();
                        } else {
                            key.label = "English";
                        }
                    }
                }
                // 2. IM Switch Key Label (keycode -10)
                if (key.codes[0] == -10) {
                    if (isIm && !isSymbol) {
                        key.label = getActiveIMSingleChar();
                        key.icon = null;
                    } else {
                        // Use Translation Icon for Symbol/English mode
                        key.label = null;
                        key.icon = mThemedContext.getResources().getDrawable(R.drawable.ic_translate);
                    }
                }

            }

            // mCurrentId = kid;
            mInputView.setKeyboard(keyboard);

            assert keyboard != null;
            keyboard.setShiftLocked(keyboard.isShiftLocked());
            keyboard.setShifted(mIsShifted);
            mInputView.setKeyboard(keyboard); // instead of invalidateAllKeys();

            keyboard.setImeOptions(mThemedContext.getResources(), mMode, imeOptions);
        }
    }

    public int getKeyboardMode() {
        return mMode;
    }

    public boolean isTextMode() {
        return mMode == MODE_TEXT;
    }

    public int getTextMode() {
        return mTextMode;
    }

    public int getTextModeCount() {
        return MODE_TEXT_COUNT;
    }

    public boolean isAlphabetMode() {
        return mIsAlphabet;
    }

    public void toggleShift() {
        if (DEBUG)
            Log.i(TAG, "toggleShift() KBMODE mode:" + mMode);
        mIsShifted = !mIsShifted;
        if (mIsChinese)
            this.setKeyboardMode(imtype, 0, mImeOptions, true, mIsSymbols, mIsShifted);
        else {
            this.setKeyboardMode(imtype, mMode, mImeOptions, false, mIsSymbols, mIsShifted);
        }

    }

    public void setIsChinese(boolean value) {
        mIsChinese = value;
    }

    public void setIsSymbols(boolean value) {
        mIsSymbols = value;
    }

    public void toggleChinese() {
        mIsChinese = !mIsChinese;

        if (mIsChinese) {

            this.setKeyboardMode(imtype, 0, mImeOptions, true, mIsSymbols, mIsShifted);

        } else {

            this.setKeyboardMode(imtype, mMode, mImeOptions, false, mIsSymbols, mIsShifted);

        }
    }

    public void toggleSymbols() {

        if (mIsChinese)
            this.setKeyboardMode(imtype, 0, mImeOptions, true, !mIsSymbols, false);
        else
            this.setKeyboardMode(imtype, mMode, mImeOptions, false, !mIsSymbols, false);

    }

    public void switchSymbols() {
        switch (mCurrentSymbolsKeyboard) {
            case SYMBOLS_KEYBOARD_1:
            default:
                mCurrentSymbolsKeyboard = SYMBOLS_KEYBOARD_2;
                break;
            case SYMBOLS_KEYBOARD_2:
                mCurrentSymbolsKeyboard = SYMBOLS_KEYBOARD_3;
                break;
            case SYMBOLS_KEYBOARD_3:
                mCurrentSymbolsKeyboard = SYMBOLS_KEYBOARD_1;
                break;

        }
        if (mIsChinese)
            this.setKeyboardMode(imtype, 0, mImeOptions, true, mIsSymbols, false);
        else
            this.setKeyboardMode(imtype, mMode, mImeOptions, false, mIsSymbols, false);

    }

    public boolean isChinese() {
        return mIsChinese;
    }

    public boolean isSymbols() {
        return mIsSymbols;
    }

    public boolean isShifted() {
        return mIsShifted;
    }

    /**
     * Represents the parameters necessary to construct a new LatinKeyboard,
     * which also serve as a unique identifier for each keyboard type.
     */
    private static class KeyboardId {
        public int mXml;
        public int mMode;
        public boolean mEnableShiftLock;

        public KeyboardId(int xml, int mode, boolean enableShiftLock) {
            this.mXml = xml;
            this.mMode = mode;
            this.mEnableShiftLock = enableShiftLock;
        }

        public KeyboardId(int xml) {
            this(xml, 0, false);
        }

        public boolean equals(Object other) {
            return other instanceof KeyboardId && equals((KeyboardId) other);
        }

        public boolean equals(KeyboardId other) {
            return other.mXml == this.mXml && other.mMode == this.mMode;
        }

        public int hashCode() {
            return (mXml + 1) * (mMode + 1) * (mEnableShiftLock ? 2 : 1);
        }
    }
}
