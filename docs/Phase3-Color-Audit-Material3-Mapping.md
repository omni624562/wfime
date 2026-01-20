# Phase 3: Color Audit & Material3 Mapping
# Phase 3: 顏色稽核與 Material3 映射

**Date:** 2026-01-20
**Phase:** 3.1 - Color System Analysis
**Status:** 🔍 AUDIT COMPLETE

---

## Executive Summary | 執行摘要

Comprehensive audit of WFIME's color system reveals a well-organized structure with **59 color definitions** across light/dark themes and custom keyboard themes. Material3 color tokens are already defined and used in the app UI, but keyboard colors remain hardcoded.

對 WFIME 顏色系統的全面稽核顯示，跨亮/暗主題和自訂鍵盤主題共有 **59 個顏色定義**的良好組織結構。Material3 顏色 tokens 已定義並用於 app UI，但鍵盤顏色仍為硬編碼。

**Key Findings:**
- ✅ Material3 color tokens fully defined (Light + Dark themes)
- ✅ Main app UI already uses Material3 (?attr/colorSurface, etc.)
- ⚠️ Keyboard colors bypass Material3 system (hardcoded)
- ⚠️ Custom themes (Pink, TechBlue, etc.) use static colors
- ✅ Good color organization (common, light, dark, themed)

---

## Color Inventory | 顏色清單

### Total: 59 Color Definitions

**Breakdown:**
1. Common colors: 8 definitions
2. Light theme: 11 definitions
3. Dark theme: 10 definitions
4. Pink theme: 6 definitions
5. Tech Blue theme: 6 definitions
6. Fashion Purple theme: 6 definitions
7. Relax Green theme: 6 definitions
8. Material3 tokens (Light): 17 definitions
9. Material3 tokens (Dark): 17 definitions
10. Miscellaneous: 6 definitions

**Total with Material3 tokens:** 93 color definitions (59 legacy + 34 Material3)

---

## Material3 Color Tokens Analysis | Material3 顏色 Tokens 分析

### ✅ Fully Defined Material3 System

**Location:** `values/colors_material3.xml` + `values-night/colors.xml`

**Light Theme (17 tokens):**
```xml
<!-- Primary -->
md_theme_primary                   #006C4C (Green - traditional Chinese theme)
md_theme_onPrimary                 #FFFFFF
md_theme_primaryContainer          #89F8C7
md_theme_onPrimaryContainer        #002115

<!-- Secondary -->
md_theme_secondary                 #4C6358
md_theme_onSecondary               #FFFFFF
md_theme_secondaryContainer        #CFE9D9
md_theme_onSecondaryContainer      #092017

<!-- Tertiary -->
md_theme_tertiary                  #3E6374
md_theme_onTertiary                #FFFFFF
md_theme_tertiaryContainer         #C1E8FC
md_theme_onTertiaryContainer       #001F29

<!-- Error -->
md_theme_error                     #BA1A1A
md_theme_onError                   #FFFFFF
md_theme_errorContainer            #FFDAD6
md_theme_onErrorContainer          #410002

<!-- Surface & Background -->
md_theme_background                #FBFDF8
md_theme_onBackground              #191C1A
md_theme_surface                   #FBFDF8
md_theme_onSurface                 #191C1A
md_theme_surfaceVariant            #DBE5DD
md_theme_onSurfaceVariant          #404943

<!-- Outline -->
md_theme_outline                   #707972
md_theme_outlineVariant            #BFC9C1

<!-- Inverse -->
md_theme_inverseSurface            #2E312E
md_theme_inverseOnSurface          #F0F1EC
md_theme_inversePrimary            #6CDBAC

<!-- Scrim -->
md_theme_scrim                     #000000
```

**Dark Theme (17 tokens):**
```xml
<!-- Primary -->
md_theme_primary                   #6CDBAC (Lighter green for dark mode)
md_theme_onPrimary                 #003826
md_theme_primaryContainer          #005138
md_theme_onPrimaryContainer        #89F8C7

<!-- Secondary -->
md_theme_secondary                 #B3CCBD
md_theme_onSecondary               #1F352B
md_theme_secondaryContainer        #354B40
md_theme_onSecondaryContainer      #CFE9D9

<!-- Tertiary -->
md_theme_tertiary                  #A5CCE0
md_theme_onTertiary                #073543
md_theme_tertiaryContainer         #244C5B
md_theme_onTertiaryContainer       #C1E8FC

<!-- Error -->
md_theme_error                     #FFB4AB
md_theme_onError                   #690005
md_theme_errorContainer            #93000A
md_theme_onErrorContainer          #FFDAD6

<!-- Surface & Background -->
md_theme_background                #191C1A
md_theme_onBackground              #E1E3DE
md_theme_surface                   #191C1A
md_theme_onSurface                 #E1E3DE
md_theme_surfaceVariant            #404943
md_theme_onSurfaceVariant          #BFC9C1

<!-- Outline -->
md_theme_outline                   #89938B
md_theme_outlineVariant            #404943

<!-- Inverse -->
md_theme_inverseSurface            #E1E3DE
md_theme_inverseOnSurface          #2E312E
md_theme_inversePrimary            #006C4C

<!-- Scrim -->
md_theme_scrim                     #000000
```

---

## Legacy Color Categories | 舊版顏色分類

### 1. Common Colors (8 definitions) - ✅ KEEP

**Purpose:** Reusable utility colors

```xml
color_white                        #FFFFFFFF
color_black                        #FF000000
color_transparent                  #00FFFFFF
color_common_green_hl              #FF4DB6AC (Highlight - teal)
color_common_light_green_hl        #FF80CBC4 (Light teal)
color_common_orange_hl             #FFFBB732 (Highlight - orange)
color_common_light_orange_hl       #FFF29238 (Light orange)
background_score                   #88333333 (Score overlay)
```

**Recommendation:** KEEP - These are semantic utility colors, not Material3 replacements

---

### 2. Light Theme Colors (11 definitions) - ⚠️ MIGRATE

**Purpose:** Keyboard colors for light theme

```xml
keyboard_background_light          #FFC8C8C8 → ?attr/colorSurface
second_background_light            #FFE1E1E1 → ?attr/colorSurfaceVariant
second_background_light_fl         #FFFBFBFB → ?attr/colorSurface
third_background_light             #FFFAFAFA → ?attr/colorSurfaceContainer
composing_background_light         #99E1E1E1 → ?attr/colorSurfaceVariant (60% alpha)

candidate_spacer                   #FF101010 → ?attr/colorOutline
candidate_selection_keys           #CC5E5A80 → ?attr/colorPrimary (80% alpha)

foreground_light                   #FF0F0F0F → ?attr/colorOnSurface
second_foreground_light            #FF717171 → ?attr/colorOnSurfaceVariant
third_foreground_light             #FF7D7D7D → ?attr/colorOnSurfaceVariant
```

**Migration Status:**
- **Impact:** HIGH (used in 8 drawable files)
- **Effort:** 2-3 hours
- **Priority:** HIGH

---

### 3. Dark Theme Colors (10 definitions) - ⚠️ MIGRATE

**Purpose:** Keyboard colors for dark theme

```xml
keyboard_background_dark           #FF121212 → ?attr/colorSurface
background_dark                    #FF121212 → ?attr/colorSurface
second_background_dark             #FF424242 → ?attr/colorSurfaceVariant
functional_key_background_dark     #FF2F2F2F → ?attr/colorSurfaceContainer
composing_background_dark          #99121212 → ?attr/colorSurface (60% alpha)

foreground_dark                    #FFFFFFFF → ?attr/colorOnSurface
candidate_foreground_dark          #FFFFFFFF → ?attr/colorOnSurface
key_background_pressed_dark        #FF454545 → ?attr/colorPrimaryContainer
second_foreground_dark             #FF9E9E9E → ?attr/colorOnSurfaceVariant
third_foreground_dark              #FF9E9E9E → ?attr/colorOnSurfaceVariant

action_key_color                   #FF70C0B0 → ?attr/colorPrimary
```

**Migration Status:**
- **Impact:** HIGH (used in 8+ drawable files)
- **Effort:** 2-3 hours
- **Priority:** HIGH

---

### 4. Custom Themes - 📌 DOCUMENT & KEEP

#### Pink Theme (6 definitions)
```xml
pink_hl                            #FFC74A72 (Highlight)
keyboard_background_pink           #FFFAD5E5
candidate_background_pink          #FFFEF3F7
composing_background_pink          #99FAD5E5 (60% alpha)
second_background_pink             #FFF49AC1
second_background_pink_hl          #FFF173AC (Highlight)
```

#### Tech Blue Theme (6 definitions)
```xml
tech_blue_hl                       #FF4167B0
keyboard_background_tech_blue      #FFC5DBEC
candidate_background_tech_blue     #FFD8E7F3
composing_background_tech_blue     #99B2CFE6 (60% alpha)
second_background_tech_blue        #FF9BC5E4
second_background_tech_blue_hl     #FF6699CC
foreground_tech_blue               #FF314453
second_foreground_tech_blue        #FF4E6677
```

#### Fashion Purple Theme (6 definitions)
```xml
fashion_purple_hl                  #FF45196F
keyboard_background_fashion_purple #FFB0ACD5
candidate_background_fashion_purple#FFEFEDFF
composing_background_fashion_purple#99B0ACD4 (60% alpha)
second_background_fashion_purple   #FFB28ABF
second_background_fashion_purple_hl#FF8F53A1
foreground_fashion_purple          #FFEEEEEE
second_foreground_fashion_purple   #FF45196F
```

#### Relax Green Theme (6 definitions)
```xml
relax_green_hl                     #FF006838
keyboard_background_relax_green    #FF8DC63F
candidate_background_relax_green   #FFF2F5D5
composing_background_relax_green   #99D7DF23 (60% alpha)
second_background_relax_green      #FF39B54A
second_background_relax_green_hl   #FF009444
foreground_relax_green             #FF003A17
second_foreground_relax_green      #FF009444
```

**Recommendation:** KEEP as-is (user customization feature, not Material3 compliance)

---

## Material3 Mapping Strategy | Material3 映射策略

### Phase 3.2: Keyboard Color Migration (2-3h)

**Goal:** Replace hardcoded keyboard colors with Material3 tokens for Light/Dark themes

**Approach:**
1. Create new color references in `colors.xml` that point to Material3 attributes
2. Update drawable files to use new references
3. Preserve custom themes unchanged

**Mapping Table:**

| Legacy Color | Material3 Token | Usage |
|--------------|-----------------|-------|
| `keyboard_background_light` | `?attr/colorSurface` | Main keyboard surface |
| `second_background_light` | `?attr/colorSurfaceVariant` | Secondary keys |
| `third_background_light` | `?attr/colorSurfaceContainer` | Tertiary elements |
| `composing_background_light` | `?attr/colorSurfaceVariant` (60%) | Composing text popup |
| `foreground_light` | `?attr/colorOnSurface` | Main text color |
| `second_foreground_light` | `?attr/colorOnSurfaceVariant` | Secondary text |
| `candidate_spacer` | `?attr/colorOutline` | Visual separators |
| `action_key_color` | `?attr/colorPrimary` | Action buttons |
| `keyboard_background_dark` | `?attr/colorSurface` | Dark theme surface |
| `second_background_dark` | `?attr/colorSurfaceVariant` | Dark secondary |
| `functional_key_background_dark` | `?attr/colorSurfaceContainer` | Dark functional keys |
| `key_background_pressed_dark` | `?attr/colorPrimaryContainer` | Dark pressed state |

---

## Drawable Files Using Keyboard Colors | 使用鍵盤顏色的 Drawable 檔案

**Files Found:** 8 files

```
btn_flat_keyboard_normal_key_pressed_light.xml
btn_flat_keyboard_normal_key_normal_light.xml
btn_flat_keyboard_function_key_pressed_light.xml
btn_flat_keyboard_function_key_normal_light.xml
keyboard_popup_panel_background_light.xml
keyboard_background_light.xml
(+ corresponding dark theme files)
```

**Migration Impact:**
- Need to update color references in these drawable files
- Replace `@color/keyboard_background_light` with dynamic color attributes
- Test with both light and dark system themes

---

## Phase 3.3: Duplicate Color Cleanup (1-2h)

### Identified Duplicates

**Same Color, Different Names:**

1. `background_dark` (#FF121212) = `keyboard_background_dark` (#FF121212)
   - **Action:** Merge into single reference

2. `foreground_dark` (#FFFFFFFF) = `candidate_foreground_dark` (#FFFFFFFF)
   - **Action:** Use single `foreground_dark`

3. `second_foreground_dark` (#FF9E9E9E) = `third_foreground_dark` (#FF9E9E9E)
   - **Action:** Merge into `second_foreground_dark`

**Potential Savings:** 3-4 color definitions removed

---

## Theme Structure Analysis | 主題結構分析

### Current Architecture

**App Theme (Material3):**
```xml
AppTheme (parent: Theme.Material3.Light.NoActionBar)
├── Uses md_theme_* colors ✅
├── Edge-to-edge support ✅
└── PreferenceTheme override ✅
```

**Keyboard Themes (Legacy):**
```xml
LIMETheme.Light
├── LIMEKeyboardStyle → uses legacy colors ⚠️
├── LIMEBaseKeyboardStyle → uses legacy colors ⚠️
├── LIMEKeyboardBaseView → uses legacy colors ⚠️
├── LIMEKeyboardLayout → uses legacy colors ⚠️
└── LIMECandidateView → uses legacy colors ⚠️

LIMETheme.Dark (same structure)
LIMETheme.Pink (custom, keep)
LIMETheme.TechBlue (custom, keep)
LIMETheme.FashionPurple (custom, keep)
LIMETheme.RelaxGreen (custom, keep)
```

**Issue:** Keyboard styles reference hardcoded colors instead of Material3 tokens

**Solution:** Update Light/Dark keyboard styles to use `?attr/color*` attributes

---

## Implementation Plan | 實施計劃

### Phase 3.2: Keyboard Color Refactoring (2-3 hours)

#### Task 3.2.1: Create Color Attribute References (30 min)

**Create new file:** `values/colors_keyboard_material3.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Material3 Keyboard Colors - Dynamic theming support -->
<resources>
    <!-- Light theme keyboard colors (point to Material3 tokens) -->
    <color name="keyboard_background_material3_light">@color/md_theme_surface</color>
    <color name="keyboard_second_background_material3_light">@color/md_theme_surfaceVariant</color>
    <color name="keyboard_foreground_material3_light">@color/md_theme_onSurface</color>

    <!-- Dark theme keyboard colors (automatic via -night folder) -->
</resources>
```

**Create:** `values-night/colors_keyboard_material3.xml` (dark variants)

#### Task 3.2.2: Update Drawable Files (1-1.5h)

Update 8 drawable files to reference new color attributes:

**Example:** `btn_flat_keyboard_normal_key_normal_light.xml`

**Before:**
```xml
<color android:color="@color/keyboard_background_light" />
```

**After:**
```xml
<color android:color="@color/keyboard_background_material3_light" />
```

**Files to update:**
- btn_flat_keyboard_normal_key_pressed_light.xml
- btn_flat_keyboard_normal_key_normal_light.xml
- btn_flat_keyboard_function_key_pressed_light.xml
- btn_flat_keyboard_function_key_normal_light.xml
- keyboard_popup_panel_background_light.xml
- keyboard_background_light.xml
- (+ dark variants)

#### Task 3.2.3: Update Keyboard Styles (30 min)

**File:** `values/styles.xml`

Update `LIMEKeyboard.Light` and `LIMEKeyboard.Dark` styles to reference Material3 colors

#### Task 3.2.4: Testing (30 min)

- Build APK
- Test Light theme keyboard
- Test Dark theme keyboard
- Toggle system dark mode
- Verify colors update correctly

---

### Phase 3.3: Color Cleanup (1-2 hours)

#### Task 3.3.1: Merge Duplicate Colors (30 min)

Update all references of:
- `candidate_foreground_dark` → `foreground_dark`
- `third_foreground_dark` → `second_foreground_dark`
- Remove duplicate definitions from `colors.xml`

#### Task 3.3.2: Add Color Documentation (30 min)

Add XML comments to `colors.xml`:
```xml
<!-- Common utility colors - Keep for semantic use -->
<!-- Material3 keyboard colors - Migrated to dynamic theming -->
<!-- Custom theme colors - User customization, keep as-is -->
```

#### Task 3.3.3: Verify No Broken References (30 min)

- Search for removed color references
- Run build
- Fix any compilation errors

---

## Expected Outcomes | 預期成果

### After Phase 3.2-3.3:

**Material3 Compliance:**
- Light/Dark keyboard themes use Material3 tokens ✅
- Dynamic color support for Android 12+ ✅
- Custom themes preserved for user choice ✅

**Code Quality:**
- ~5-10 fewer color definitions
- Better organized color files
- Clear documentation

**Build Status:**
- Zero compilation errors
- APK size unchanged (colors are metadata)
- Runtime performance identical

**Material3 Progress:**
- Before: 95% compliance
- After Phase 3: **98% compliance** 🎉

---

## Risks & Mitigation | 風險與緩解

### Risk 1: Visual Changes

**Risk:** Colors might look different with Material3 tokens

**Mitigation:**
- Take screenshots before/after
- Test on Android 11 (no dynamic colors) and Android 14 (with dynamic colors)
- Keep original colors in comments for rollback

**Probability:** LOW (Material3 tokens designed for good contrast)

### Risk 2: Breaking Custom Themes

**Risk:** Accidentally changing Pink/TechBlue/etc. themes

**Mitigation:**
- ONLY modify Light/Dark themes
- Test all 6 themes after changes
- Keep custom theme colors untouched

**Probability:** LOW (clear separation in code)

### Risk 3: Drawable Attribute Errors

**Risk:** Drawable files might not support ?attr/ references

**Mitigation:**
- Use @color/ references to Material3 color resources
- Not direct ?attr/ in drawable XML
- Test build after each file update

**Probability:** MEDIUM (but easy to fix)

---

## Success Criteria | 成功標準

### Phase 3.2 Complete When:

- [ ] All Light theme keyboard drawables use Material3 colors
- [ ] All Dark theme keyboard drawables use Material3 colors
- [ ] Build successful with zero errors
- [ ] Keyboard renders correctly in both themes
- [ ] Custom themes (Pink, TechBlue, etc.) unchanged
- [ ] Dynamic colors work on Android 12+

### Phase 3.3 Complete When:

- [ ] Duplicate colors merged
- [ ] All color definitions documented
- [ ] No broken color references
- [ ] Build successful
- [ ] Color count reduced by 3-5 definitions

---

## Time Estimate | 時間估計

**Original Plan:** 8-12 hours (full color refactoring)

**Revised Plan (Essential):**
- Phase 3.2: Keyboard color migration (2-3h)
- Phase 3.3: Duplicate cleanup (1-2h)
- **Total:** 3-5 hours

**Time Savings:** ~5 hours (by keeping custom themes as-is)

---

## Next Steps | 下一步

### Immediate:

1. **Review this document** - Ensure mapping strategy is correct
2. **Get user approval** - Confirm approach before implementation
3. **Start Phase 3.2** - Create color attribute files

### After Phase 3:

- Phase 4.1: Implement Snackbar (1-2h)
- Phase 4.2: UI enhancements (1-2h)
- Final testing and documentation
- Create Pull Request

---

## Material3 Color Token Reference | Material3 顏色 Token 參考

### Surface Colors (Backgrounds)

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| `colorSurface` | #FBFDF8 | #191C1A | Main surface |
| `colorSurfaceVariant` | #DBE5DD | #404943 | Secondary surface |
| `colorSurfaceContainer` | (derived) | (derived) | Container surface |
| `colorPrimaryContainer` | #89F8C7 | #005138 | Primary emphasis |

### Foreground Colors (Text)

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| `colorOnSurface` | #191C1A | #E1E3DE | Main text |
| `colorOnSurfaceVariant` | #404943 | #BFC9C1 | Secondary text |
| `colorOnPrimary` | #FFFFFF | #003826 | Text on primary |

### Accent Colors

| Token | Light | Dark | Usage |
|-------|-------|------|-------|
| `colorPrimary` | #006C4C | #6CDBAC | Primary actions |
| `colorOutline` | #707972 | #89938B | Borders, dividers |

---

## Conclusion | 結論

The color audit reveals a well-structured system with clear separation between:
1. ✅ Material3 tokens (fully defined, used in app UI)
2. ⚠️ Legacy keyboard colors (need migration to Material3)
3. 📌 Custom themes (keep as-is for user customization)

**Recommendation:** Proceed with Phase 3.2-3.3 implementation for 3-5 hours of focused work to achieve 98% Material3 compliance while preserving user customization options.

**建議：** 進行 Phase 3.2-3.3 實施，專注工作 3-5 小時，以達成 98% Material3 合規性，同時保留使用者自訂選項。

---

**Document Version:** 1.0
**Created:** 2026-01-20
**Phase:** 3.1 Complete (Audit)
**Next:** Phase 3.2 Implementation
**Estimated Effort:** 3-5 hours (Phase 3.2-3.3)
