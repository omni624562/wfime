package net.toload.main.hd.keyboard;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import net.toload.main.hd.keyboard.LIMEBaseKeyboard.Key;

import net.toload.main.hd.R;

import java.util.HashMap;

/**
 * 畫一個鍵：背景、主標籤、字根副標籤、圖示，以及這些需要的樣式（從鍵盤主題讀入）。
 * 從 LIMEKeyboardBaseView.onBufferDraw 原樣搬出，舊鍵盤與 Compose 鍵盤共用，外觀因此一模一樣（藍圖 A4-3）。
 */
final class KeyPainter {
    private static final int NUMBER_HINT_VERTICAL_ADJUSTMENT_PIXEL = -1;

    /** Shift 按著時把標籤轉大寫（由鍵盤 View 提供）。 */
    interface CaseAdjuster {
        CharSequence adjustCase(CharSequence label);
    }

    Drawable mKeyBackground;
    Drawable mActionKeyBackground;
    Drawable mFunctionalKeyBackground;
    int mKeyTextSize;
    int mLabelTextSize;
    int mSmallLabelTextSize;
    int mSubLabelTextSize;
    Typeface mKeyTextStyle = Typeface.DEFAULT;
    int mShadowColor;
    float mShadowRadius;
    Drawable mPopupHint;// Jeremy /11,8,11
    int mKeyTextColorNormal;
    int mKeyTextColorPressed; // Jeremy '15,5,13
    int mFunctionKeyTextColorNormal;
    int mFunctionKeyTextColorPressed; // Jeremy '15,5,13
    int mKeySubLabelTextColorNormal; // Jeremy '12,4,29
    int mKeySubLabelTextColorPressed; // Jeremy '15,5,13
    Paint mPaint;
    Rect mPadding;
    /**
     * 鍵面 label 拆解快取(主字/子標籤/第二子標籤),避免每次重繪 split 配置
     */
    final java.util.HashMap<String, String[]> mLabelSplitCache = new java.util.HashMap<>();
    final Rect mTextBounds = new Rect();
    // This map caches key label text height in pixel as value and key label text
    // size as map key.
    final HashMap<Integer, Integer> mTextHeightCache = new HashMap<>();
    final HashMap<Integer, Integer> mTextWidthCache = new HashMap<>();

    /**
     * 解讀鍵盤主題裡畫鍵用的一個樣式屬性（背景、字級、顏色、陰影、圖示）；
     * 是畫鍵用的屬性就回傳 true。從 LIMEKeyboardBaseView 建構子原樣搬出，新舊鍵盤共用。
     */
    boolean applyStyleAttribute(TypedArray a, int attr) {
        if (attr == R.styleable.LIMEKeyboardBaseView_keyBackground) {
            mKeyBackground = a.getDrawable(attr);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_actionKeyBackground) {
            mActionKeyBackground = a.getDrawable(attr);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_functionalKeyBackground) {
            mFunctionalKeyBackground = a.getDrawable(attr);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_keyTextSize) {
            mKeyTextSize = a.getDimensionPixelSize(attr, 18);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_functionKeyTextColorNormal) {
            mFunctionKeyTextColorNormal = a.getColor(attr, 0xFF000000);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_functionKeyTextColorPressed) {
            mFunctionKeyTextColorPressed = a.getColor(attr, 0xFF000000);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_keyTextColorNormal) {
            mKeyTextColorNormal = a.getColor(attr, 0xFF000000);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_keyTextColorPressed) {
            mKeyTextColorPressed = a.getColor(attr, 0xFF000000);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_keySubLabelTextColorNormal) {
            mKeySubLabelTextColorNormal = a.getColor(attr, 0xFF000000);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_keySubLabelTextColorPressed) {
            mKeySubLabelTextColorPressed = a.getColor(attr, 0xFF000000);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_labelTextSize) {
            mLabelTextSize = a.getDimensionPixelSize(attr, 14);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_smallLabelTextSize) { // Jeremy '11,8,11
            mSmallLabelTextSize = a.getDimensionPixelSize(attr, 14);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_subLabelTextSize) { // Jeremy '11,8,11
            mSubLabelTextSize = a.getDimensionPixelSize(attr, 14);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_popupHint) {
            mPopupHint = a.getDrawable(attr);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_shadowColor) {
            mShadowColor = a.getColor(attr, 0);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_shadowRadius) {
            mShadowRadius = a.getFloat(attr, 0f);
        } else if (attr == R.styleable.LIMEKeyboardBaseView_keyTextStyle) {
            int textStyle = a.getInt(attr, 0);
            switch (textStyle) {
                case 0:
                    mKeyTextStyle = Typeface.DEFAULT;
                    break;
                case 1:
                    mKeyTextStyle = Typeface.DEFAULT_BOLD;
                    break;
                default:
                    mKeyTextStyle = Typeface.defaultFromStyle(textStyle);
                    break;
            }
        } else {
            return false;
        }
        return true;
    }

    /** 樣式屬性讀完後建立畫筆與鍵背景內距。 */
    void finishStyle(int keyTextSize) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(keyTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAlpha(255);

        mPadding = new Rect(0, 0, 0, 0);
        mKeyBackground.getPadding(mPadding);
    }

    /** 在 canvas 上畫一個鍵；(kbdPaddingLeft, kbdPaddingTop) 是鍵盤內距。 */
    void drawKey(Canvas canvas, Key key, LIMEBaseKeyboard keyboard, int kbdPaddingLeft, int kbdPaddingTop,
            CaseAdjuster caseAdjuster) {
        final Paint paint = mPaint;
        final Drawable keyBackground = mKeyBackground;
        final Rect padding = mPadding;
        int[] drawableState = key.getCurrentDrawableState();

        // Use action key background for Enter key, More Symbols (hamburger) key, and [?123] mode change key
        boolean isActionKey = key.codes != null && key.codes.length > 0
                && (key.codes[0] == LIMEBaseKeyboard.KEYCODE_ENTER
                    || key.codes[0] == LIMEBaseKeyboard.KEYCODE_MODE_CHANGE
                    || key.codes[0] == -15);

        // Identify functional keys (Delete, Shift, Symbol, Done) - Space (32) removed to match Gboard style
        // Also include keys marked as modifiers in XML for consistency
        boolean isFunctionalKey = key.isFunctionalKey() || (key.codes != null && key.codes.length > 0 && (
                key.codes[0] == LIMEBaseKeyboard.KEYCODE_DELETE ||
                key.codes[0] == LIMEBaseKeyboard.KEYCODE_SHIFT ||
                key.codes[0] == LIMEBaseKeyboard.KEYCODE_MODE_CHANGE ||
                key.codes[0] == LIMEBaseKeyboard.KEYCODE_DONE ||
                key.codes[0] == -15)); // KEYCODE_SWITCH_SYMBOL_KEYBOARD (more pages ≡)

        Drawable currentBackground = keyBackground;
        if (isActionKey && mActionKeyBackground != null) {
            currentBackground = mActionKeyBackground;
        } else if (isFunctionalKey && mFunctionalKeyBackground != null) {
            currentBackground = mFunctionalKeyBackground;
        }

        // Drawable currentBackground = (isActionKey && mActionKeyBackground != null) ?
        // mActionKeyBackground
        // : keyBackground;

        currentBackground.setState(drawableState);

        // Switch the character to uppercase if shift is pressed
        String label = key.label == null ? null : caseAdjuster.adjustCase(key.label).toString();

        // Drawing dimensions now trust the pre-calculated key width/height (which have gaps subtracted)
        int drawWidth = key.width;
        int drawHeight = key.height;

        final Rect bounds = currentBackground.getBounds();
        if (drawWidth != bounds.right || drawHeight != bounds.bottom) {
            currentBackground.setBounds(0, 0, drawWidth, drawHeight);
        }
        // Offset drawing by key position (which already includes horizontal gap)
        canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
        currentBackground.draw(canvas);

        boolean shouldDrawIcon = true;
        if (label != null) {
            // For characters, use large font. For labels like "Done", use small font.
            int labelSize;

            float keySizeScale = keyboard.getKeySizeScale();
            float labelSizeScale = key.getLabelSizeScale();

            boolean hasSubLabel = label.contains("\n");
            boolean hasSecondSubLabel = false;
            String subLabel = "", secondSubLabel = "";
            if (hasSubLabel) {
                // 拆解結果快取:同一 label 不在每次重繪重複 split 配置
                String[] parts = mLabelSplitCache.get(label);
                if (parts == null) {
                    String mainPart = label, subPart, secondPart = "";
                    String labelA[] = label.split("\n");
                    if (labelA.length > 0)
                        mainPart = labelA[1];
                    subPart = labelA[0];
                    if (subPart.contains("\t")) {
                        String subLabelA[] = subPart.split("\t");
                        if (subLabelA.length > 0)
                            subPart = subLabelA[0];
                        secondPart = subLabelA[1];
                    }
                    parts = new String[]{mainPart, subPart, secondPart};
                    mLabelSplitCache.put(label, parts);
                }
                label = parts[0];
                subLabel = parts[1];
                secondSubLabel = parts[2];
                hasSecondSubLabel = !secondSubLabel.isEmpty();
            }
            if (hasSubLabel) {
                if (label.length() > 1) { // Jeremy '12,6,6 shrink the font size for more characters on label
                    labelSize = (int) (mSmallLabelTextSize * keySizeScale * labelSizeScale * 0.8f);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    labelSize = (int) (mSmallLabelTextSize * keySizeScale * labelSizeScale);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                }
            } else if (label.length() > 1 && key.codes.length < 2) {
                labelSize = (int) (mLabelTextSize * keySizeScale * labelSizeScale);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                labelSize = (int) (mKeyTextSize * keySizeScale * labelSizeScale);
                // Material3: Use Medium weight for main keys to match Gboard style
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            }
            paint.setTextSize(labelSize);
            // 沒有副標籤的標籤比按鍵寬時（如英文介面切換鍵的 "Chuyin"）等比例縮小字級，左右各留 5%
            if (!hasSubLabel) {
                float availableWidth = (drawWidth - padding.left - padding.right) * 0.9f;
                int fittedSize = fitLabelTextSize(labelSize, paint.measureText(label), availableWidth);
                if (fittedSize != labelSize) {
                    labelSize = fittedSize;
                    paint.setTextSize(labelSize);
                }
            }

            final int labelHeight;
            final int labelWidth;
            String KEY_LABEL_HEIGHT_REFERENCE_CHAR = "W";
            if (mTextHeightCache.get(labelSize) != null) {
                labelHeight = mTextHeightCache.get(labelSize);
                labelWidth = mTextWidthCache.get(labelSize);
            } else {
                paint.getTextBounds(KEY_LABEL_HEIGHT_REFERENCE_CHAR, 0, 1, mTextBounds);
                labelHeight = mTextBounds.height();
                labelWidth = mTextBounds.width();
                mTextHeightCache.put(labelSize, labelHeight);
                mTextWidthCache.put(labelSize, labelWidth);
            }

            // Draw a drop shadow for the text
            if (mShadowRadius > 0)
                paint.setShadowLayer(mShadowRadius, 0, 0, mShadowColor);
            final int centerX = (drawWidth + padding.left - padding.right) / 2;
            final int centerY = (drawHeight + padding.top - padding.bottom) / 2;
            final int keyColor = isActionKey
                    ? mFunctionKeyTextColorPressed  // action key always contrasts against primary background
                    : key.isFunctionalKey()
                            ? (key.pressed ? mFunctionKeyTextColorPressed : mFunctionKeyTextColorNormal)
                            : (key.pressed ? mKeyTextColorPressed : mKeyTextColorNormal);
            final int subKeyColor = isActionKey
                    ? mFunctionKeyTextColorPressed
                    : key.isFunctionalKey()
                            ? (key.pressed ? mFunctionKeyTextColorPressed : mFunctionKeyTextColorNormal)
                            : (key.pressed ? mKeySubLabelTextColorPressed : mKeySubLabelTextColorNormal);

            float KEY_LABEL_VERTICAL_ADJUSTMENT_FACTOR = 0.55f;
            float baseline = centerY
                    + labelHeight * KEY_LABEL_VERTICAL_ADJUSTMENT_FACTOR;
            if (hasSubLabel) {
                final int subLabelSize = (int) (mSubLabelTextSize * keySizeScale * labelSizeScale);
                final int subLabelHeight;
                final int subLabelWidth;
                paint.setTypeface(Typeface.DEFAULT_BOLD);

                paint.setTextSize(subLabelSize);
                if (mTextHeightCache.get(subLabelSize) != null) {
                    subLabelHeight = mTextHeightCache.get(subLabelSize);
                    subLabelWidth = mTextWidthCache.get(subLabelSize);
                } else {

                    paint.getTextBounds(KEY_LABEL_HEIGHT_REFERENCE_CHAR, 0, 1, mTextBounds);
                    subLabelHeight = mTextBounds.height();
                    subLabelWidth = mTextBounds.width();
                    mTextHeightCache.put(subLabelSize, subLabelHeight);
                    mTextWidthCache.put(subLabelSize, subLabelWidth);
                }

                // portrait keyboard
                if (drawHeight > drawWidth || subLabel.length() > 2 || hasSecondSubLabel) {
                    baseline = (drawHeight + padding.top - padding.bottom) * 2 / 3
                            + labelHeight * KEY_LABEL_VERTICAL_ADJUSTMENT_FACTOR;
                    float subBaseline = (drawHeight + padding.top - padding.bottom) / 4
                            + subLabelHeight * KEY_LABEL_VERTICAL_ADJUSTMENT_FACTOR;
                    paint.setColor(subKeyColor);

                    if (hasSecondSubLabel) {
                        canvas.drawText(subLabel, centerX / 2, subBaseline, paint);

                        paint.setColor(keyColor);
                        canvas.drawText(secondSubLabel, centerX / 2 * 3, subBaseline, paint);
                    } else
                        canvas.drawText(subLabel, centerX, subBaseline, paint);

                    paint.setTextSize(labelSize);
                    paint.setTypeface(mKeyTextStyle);
                    paint.setColor(keyColor);
                    canvas.drawText(label, centerX, baseline, paint);

                } else { // landscape keyboard
                    paint.setColor(subKeyColor);
                    // if (subLabel.length() > 2) // draw sub keys as portrait keys in two rows.
                    // paint.setTextSize(subLabelSize * 2 / 3); //123 EN in landscape is usually to
                    // wide.
                    canvas.drawText(subLabel, centerX - subLabelWidth, baseline, paint);

                    paint.setTextSize(labelSize);
                    paint.setTypeface(mKeyTextStyle);
                    paint.setColor(keyColor);
                    canvas.drawText(label, centerX + labelWidth / 2, baseline, paint);

                }

            } else {
                paint.setColor(keyColor);
                canvas.drawText(label, centerX, baseline, paint);
            }
            // Turn off drop shadow
            if (mShadowRadius > 0)
                paint.setShadowLayer(0, 0, 0, 0);

            // Usually don't draw icon if label is not null, but we draw icon for the number
            // hint and popup hint.
            shouldDrawIcon = shouldDrawLabelAndIcon(key);
        }
        if (shouldDrawIcon) {
            Drawable icon = key.icon;
            if (icon == null)
                icon = mPopupHint;
            else {
                icon.setState(drawableState);
            }

            // Apply tint to ALL icons based on key state and type
            int iconTintColor = isActionKey
                    ? mFunctionKeyTextColorPressed  // action key always contrasts against primary background
                    : key.isFunctionalKey()
                            ? (key.pressed ? mFunctionKeyTextColorPressed : mFunctionKeyTextColorNormal)
                            : (key.pressed ? mKeyTextColorPressed : mKeyTextColorNormal);
            
            icon.mutate();
            icon.setColorFilter(new PorterDuffColorFilter(iconTintColor, PorterDuff.Mode.SRC_IN));
            icon.setAlpha(255);

            // Special handing for the upper-right number hint icons
            final int drawableWidth;
            final int drawableHeight;
            final int drawableX;
            final int drawableY;
            if (shouldDrawIconFully(key)) {
                drawableWidth = drawWidth;
                drawableHeight = drawHeight;
                drawableX = 0;
                drawableY = NUMBER_HINT_VERTICAL_ADJUSTMENT_PIXEL;
            } else if (key.codes != null && key.codes.length > 0 && key.codes[0] == LIMEBaseKeyboard.KEYCODE_SHIFT) {
                // Shift Key: Scale to 65% of key height for better visibility
                float scaleFactor = 0.65f;
                drawableHeight = (int) (drawHeight * scaleFactor);

                // Maintain aspect ratio if intrinsic dimensions are available
                if (icon.getIntrinsicWidth() > 0 && icon.getIntrinsicHeight() > 0) {
                    drawableWidth = icon.getIntrinsicWidth() * drawableHeight / icon.getIntrinsicHeight();
                } else {
                    drawableWidth = drawableHeight;
                }
                drawableX = (drawWidth + padding.left - padding.right - drawableWidth) / 2;
                drawableY = (drawHeight + padding.top - padding.bottom - drawableHeight) / 2;
            } else {
                // Default icon scaling (Emoji, Translate, Delete, Return, etc.)
                // Use 55% of key height to match Emoji and Translate icons
                float scaleFactor = 0.55f;
                drawableHeight = (int) (drawHeight * scaleFactor);

                // Maintain aspect ratio if intrinsic dimensions are available
                if (icon.getIntrinsicWidth() > 0 && icon.getIntrinsicHeight() > 0) {
                    drawableWidth = icon.getIntrinsicWidth() * drawableHeight / icon.getIntrinsicHeight();
                } else {
                    drawableWidth = drawableHeight;
                }
                drawableX = (drawWidth + padding.left - padding.right - drawableWidth) / 2;
                drawableY = (drawHeight + padding.top - padding.bottom - drawableHeight) / 2;
            }
            canvas.translate(drawableX, drawableY);
            icon.setBounds(0, 0, drawableWidth, drawableHeight);
            icon.draw(canvas);
            canvas.translate(-drawableX, -drawableY);
        }
        canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
    }

    private boolean shouldDrawIconFully(Key key) {
        // Exclude keys with labels (like the period key) from drawing icon fully,
        // even if they have a popup keyboard. This prevents the popup hint (black box)
        // from covering the text label.
        if (key.label != null) {
            return false;
        }

        // Exclude emoji key (KEYCODE_OPTIONS = -100) from drawing icon fully,
        // even if it has a popup keyboard. This ensures consistent 55% scaling.
        if (key.codes != null && key.codes.length > 0 && key.codes[0] == -100) {
            return false;
        }
        return (hasPopupKeyboard(key));
    }

    private boolean shouldDrawLabelAndIcon(Key key) {
        // Only draw the icon (or fallback popup hint) if there's no label,
        // or if it's an actual icon provided by the key.
        // This prevents the fallback popup hint (mPopupHint) from covering the text label.
        return (hasPopupKeyboard(key) && key.label == null) || key.icon != null;
    }

    private boolean hasPopupKeyboard(Key key) {
        return key.popupResId != 0;
    }

    /**
     * （原本在 LIMEKeyboardBaseView，A4-7② 搬來。）標籤在目前字級下的寬度超過可用寬度時，回傳等比例縮小後的字級；放得下就回傳原字級。
     */
    static int fitLabelTextSize(int labelSize, float textWidth, float availableWidth) {
        if (availableWidth > 0 && textWidth > availableWidth) {
            return (int) (labelSize * availableWidth / textWidth);
        }
        return labelSize;
    }
}
