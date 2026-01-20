# Material Design 3 - 5% 完成計劃
# Material Design 3 - 5% Completion Plan

**Project:** WFIME (Wheat Fields Input Method Editor)
**Current Progress:** 95% Material Design 3 Compliance
**Target:** 100% Material Design 3 Compliance
**Estimated Effort:** 40-60 hours (1-2 sprints)
**Created:** 2026-01-20

---

## Executive Summary | 執行摘要

This document outlines a comprehensive work plan to complete the remaining 5% of Material Design 3 migration for WFIME. The plan is divided into 4 phases with clear milestones, prioritization, and resource allocation.

本文檔概述了完成 WFIME 剩餘 5% Material Design 3 遷移的全面工作計劃。該計劃分為 4 個階段，具有明確的里程碑、優先級和資源分配。

**Completion Timeline:** 2-3 weeks
**Risk Level:** Low
**Business Impact:** Medium (UX improvement)

---

## Table of Contents | 目錄

1. [Phase 1: Integration & Testing (Critical)](#phase-1-integration--testing-critical)
2. [Phase 2: XML Component Migration (High Priority)](#phase-2-xml-component-migration-high-priority)
3. [Phase 3: Color System Refactoring (Medium Priority)](#phase-3-color-system-refactoring-medium-priority)
4. [Phase 4: Advanced Components (Low Priority)](#phase-4-advanced-components-low-priority)
5. [Resource Allocation](#resource-allocation)
6. [Risk Assessment](#risk-assessment)
7. [Success Metrics](#success-metrics)
8. [Acceptance Criteria](#acceptance-criteria)

---

## Phase 1: Integration & Testing (Critical)

**Priority:** 🔴 CRITICAL
**Estimated Effort:** 12-16 hours
**Dependencies:** None
**Target Completion:** Week 1

### Objective | 目標

Complete the integration of Phase 4 Compose migration and ensure all migrated screens work correctly in production.

完成 Phase 4 Compose 遷移的整合，確保所有遷移的畫面在生產環境中正常運作。

### Tasks | 任務

#### Task 1.1: MainActivity Integration (4-6 hours)

**Description:** Wire up Compose screens in MainActivity navigation system.

**Subtasks:**
- [ ] Integrate NavigationDrawerScreen.kt with MainActivity drawer
- [ ] Connect ManageImScreen.kt to navigation menu
- [ ] Link SettingsScreen.kt to settings menu item
- [ ] Update fragment transactions to use ComposeView
- [ ] Test navigation flow between all screens

**Files to Modify:**
- `MainActivity.java`
- `ComposeBridge.kt` (if needed)

**Acceptance Criteria:**
- All Compose screens accessible from main navigation
- No crashes during navigation
- Proper back stack handling

---

#### Task 1.2: Build & Device Testing (3-4 hours)

**Description:** Build APK and test on physical devices/emulators.

**Subtasks:**
- [ ] Build debug APK
- [ ] Install on Android 11 device (minimum SDK test)
- [ ] Install on Android 14+ device (Material You test)
- [ ] Test on tablet (landscape mode)
- [ ] Verify ProGuard obfuscation doesn't break Compose

**Test Devices:**
- Android 11 (API 30) - Minimum SDK verification
- Android 14 (API 34) - Dynamic theming test
- Android 16 (API 36) - Target SDK test (if available)

**Acceptance Criteria:**
- APK installs successfully on all test devices
- No runtime crashes
- Material You theming works on Android 12+

---

#### Task 1.3: Visual & Functional Testing (3-4 hours)

**Description:** Comprehensive UI/UX testing of migrated components.

**Test Areas:**

**Navigation Drawer:**
- [ ] Drawer opens/closes smoothly
- [ ] Menu items load from database correctly
- [ ] Selection state updates properly
- [ ] Material3 styling is correct

**Manage IM Screen:**
- [ ] Word grid displays correctly (3 columns)
- [ ] Search functionality works
- [ ] Pagination controls function properly
- [ ] Add/Edit/Delete dialogs work
- [ ] Database operations complete successfully

**Word Dialog:**
- [ ] Add mode: Creates new words
- [ ] Edit mode: Updates existing words
- [ ] Delete confirmation works
- [ ] Form validation displays errors
- [ ] Score increment/decrement works

**Settings Screen:**
- [ ] All 34 preferences display correctly
- [ ] Switch preferences toggle properly
- [ ] List preferences show radio dialogs
- [ ] Settings persist to SharedPreferences
- [ ] Changes apply immediately

**Acceptance Criteria:**
- All features work as expected
- No visual glitches or layout issues
- User interactions feel smooth

---

#### Task 1.4: Edge Case Testing (2 hours)

**Description:** Test error handling and edge cases.

**Test Scenarios:**
- [ ] Empty database (no words)
- [ ] Search with no results
- [ ] Add duplicate words
- [ ] Invalid input in dialogs
- [ ] Configuration changes (rotation)
- [ ] Low memory conditions
- [ ] Dark theme compatibility

**Acceptance Criteria:**
- App handles errors gracefully
- No crashes in edge cases
- Proper error messages displayed

---

### Deliverables | 交付物

- ✅ Fully integrated Compose screens in MainActivity
- ✅ Tested APK on multiple Android versions
- ✅ Test report documenting all scenarios
- ✅ Bug fixes for any issues found

---

## Phase 2: XML Component Migration (High Priority)

**Priority:** 🟡 HIGH
**Estimated Effort:** 16-20 hours
**Dependencies:** Phase 1 complete
**Target Completion:** Week 2

### Objective | 目標

Migrate legacy XML UI components to Material3 equivalents for consistency and improved UX.

將舊版 XML UI 元件遷移至 Material3 對應元件，以實現一致性和改進的使用者體驗。

### Task 2.1: Component Inventory & Prioritization (2 hours)

**Description:** Create detailed inventory of all legacy components and prioritize migration.

**Actions:**
- [ ] Scan all 42 XML layout files
- [ ] Document each legacy component usage
- [ ] Categorize by migration priority (user-facing vs internal)
- [ ] Estimate effort for each component type

**Deliverable:** Component Migration Matrix

| Component | Count | Priority | Effort | User Impact |
|-----------|-------|----------|--------|-------------|
| Button → MaterialButton | 15+ | High | 3h | High |
| EditText → TextInputLayout | 10+ | High | 4h | High |
| ListView → RecyclerView | 5+ | Medium | 6h | Medium |
| GridView → RecyclerView | 2+ | Medium | 3h | Medium |
| ToggleButton → SwitchMaterial | 1+ | Low | 1h | Low |

---

### Task 2.2: MaterialButton Migration (3-4 hours)

**Description:** Replace all Button instances with MaterialButton.

**Strategy:**
```xml
<!-- Before -->
<Button
    android:id="@+id/button_action"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/action" />

<!-- After -->
<com.google.android.material.button.MaterialButton
    android:id="@+id/button_action"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/action"
    app:cornerRadius="12dp"
    app:elevation="2dp" />
```

**Files to Update:**
- Scan with: `grep -r "<Button" LimeStudio/app/src/main/res/layout/`
- Update each occurrence
- Test button interactions

**Variants:**
- Primary buttons: `style="@style/Widget.Material3.Button"`
- Secondary: `style="@style/Widget.Material3.Button.TonalButton"`
- Text buttons: `style="@style/Widget.Material3.Button.TextButton"`

**Acceptance Criteria:**
- All buttons use MaterialButton
- Consistent styling across app
- No functionality broken

---

### Task 2.3: TextInputLayout Migration (4-5 hours)

**Description:** Wrap all EditText instances with TextInputLayout for Material3 compliance.

**Strategy:**
```xml
<!-- Before -->
<EditText
    android:id="@+id/edit_text"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/hint" />

<!-- After -->
<com.google.android.material.textfield.TextInputLayout
    android:id="@+id/text_input_layout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="@string/hint"
    app:endIconMode="clear_text">

    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/edit_text"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</com.google.android.material.textfield.TextInputLayout>
```

**Features to Implement:**
- Error message support: `app:errorEnabled="true"`
- Helper text: `app:helperText="@string/helper"`
- Character counter: `app:counterEnabled="true"`
- Clear button: `app:endIconMode="clear_text"`

**Files Priority:**
- Dialog layouts (highest impact)
- Form layouts
- Search layouts

**Acceptance Criteria:**
- All EditText wrapped in TextInputLayout
- Error handling improved
- Consistent Material3 styling

---

### Task 2.4: RecyclerView Migration (6-8 hours)

**Description:** Replace ListView and GridView with RecyclerView for better performance.

**ListView Migration (5+ instances):**

**Steps:**
1. Create RecyclerView adapter
2. Create ViewHolder class
3. Update layout XML
4. Update Java/Kotlin code
5. Test scrolling performance

**GridView Migration (2+ instances):**

**Steps:**
1. Create RecyclerView with GridLayoutManager
2. Set span count (currently 3 for word grid)
3. Migrate adapter logic
4. Update item layout for grid
5. Test grid layout on different screen sizes

**Files to Update:**
- `layout/fragment_*.xml` files containing ListView/GridView
- Corresponding Fragment Java files
- Create new Adapter classes if needed

**Benefits:**
- Better scrolling performance
- ViewHolder pattern reduces memory
- More flexible layouts
- Better animation support

**Acceptance Criteria:**
- All ListView/GridView replaced
- No performance regression
- Smooth scrolling on all devices

---

### Task 2.5: Layout Optimization (3-4 hours)

**Description:** Convert RelativeLayout and unnecessary LinearLayout to ConstraintLayout.

**Priority:**
- Focus on complex layouts (5+ children)
- Skip simple LinearLayout (1-3 children)

**Strategy:**
1. Use Android Studio's "Convert to ConstraintLayout" tool
2. Manually optimize constraints
3. Test layout on different screen sizes
4. Verify no layout issues

**Benefits:**
- Flatter view hierarchy
- Better performance
- More flexible responsive design

**Target:** Convert 8+ RelativeLayout instances

**Acceptance Criteria:**
- Complex layouts use ConstraintLayout
- No visual changes for users
- Improved layout performance

---

### Deliverables | 交付物

- ✅ All legacy components migrated to Material3
- ✅ Component migration report
- ✅ Updated layouts tested on multiple devices
- ✅ Performance benchmarks (before/after)

---

## Phase 3: Color System Refactoring (Medium Priority)

**Priority:** 🟢 MEDIUM
**Estimated Effort:** 8-12 hours
**Dependencies:** Phase 2 complete
**Target Completion:** Week 2-3

### Objective | 目標

Refactor hardcoded color values to use Material3 color tokens for consistent dynamic theming.

重構硬編碼的顏色值以使用 Material3 色彩 tokens，實現一致的動態主題。

### Task 3.1: Color Audit (2 hours)

**Description:** Analyze current color usage and create migration map.

**Actions:**
- [ ] List all colors in `colors.xml` (100+ colors)
- [ ] Categorize by usage (keyboard, UI, themes)
- [ ] Map to Material3 semantic tokens
- [ ] Identify custom colors to preserve

**Color Categories:**

**Keyboard Colors (High Priority):**
```xml
<!-- Current -->
<color name="keyboard_background_light">#FFC8C8C8</color>
<color name="second_background_light">#FFE1E1E1</color>

<!-- Should Map To -->
@color/md_theme_surface
@color/md_theme_surfaceVariant
```

**UI Colors (Medium Priority):**
```xml
<!-- Current -->
<color name="button_background">#FF6200EE</color>
<color name="text_primary">#FF000000</color>

<!-- Should Map To -->
@color/md_theme_primary
@color/md_theme_onSurface
```

**Theme-Specific Colors (Low Priority):**
- Pink, TechBlue, FashionPurple, RelaxGreen
- Consider keeping as custom theme overlays

**Deliverable:** Color Migration Map (spreadsheet)

---

### Task 3.2: Material3 Token Mapping (3-4 hours)

**Description:** Create systematic mapping of colors to Material3 tokens.

**Material3 Color System:**

**Surface Colors:**
- `md_theme_surface` - Main background
- `md_theme_surfaceVariant` - Secondary background
- `md_theme_surfaceContainerHighest` - Elevated surfaces

**Primary Colors:**
- `md_theme_primary` - Primary brand color
- `md_theme_onPrimary` - Text/icons on primary
- `md_theme_primaryContainer` - Subdued primary

**Secondary/Tertiary:**
- `md_theme_secondary`
- `md_theme_tertiary`

**Utility Colors:**
- `md_theme_error`
- `md_theme_outline`
- `md_theme_shadow`

**Strategy:**
1. Map each keyboard color to semantic token
2. Update drawable resources
3. Update theme attributes
4. Test with dynamic colors (Android 12+)

**Acceptance Criteria:**
- Keyboard uses Material3 tokens
- Dynamic theming works correctly
- Custom themes still functional

---

### Task 3.3: Keyboard Color Refactoring (3-4 hours)

**Description:** Apply Material3 colors to keyboard drawables and styles.

**Files to Update:**

**Drawables (5 files from Phase 4):**
- `btn_flat_keyboard_normal_key_normal_light.xml`
- `btn_flat_keyboard_function_key_normal_light.xml`
- `btn_flat_keyboard_function_key_pressed_light.xml`
- `btn_flat_keyboard_key_preview_light.xml`
- `btn_flat_keyboard_normal_key_pressed_light.xml`

**Dark Theme Drawables:**
- Corresponding dark theme versions
- Ensure contrast ratios meet WCAG standards

**Example Refactoring:**
```xml
<!-- Before -->
<solid android:color="#FFC8C8C8" />

<!-- After -->
<solid android:color="?attr/colorSurface" />
```

**Testing:**
- Light theme on Android 11
- Dark theme on Android 11
- Material You on Android 12+
- Custom themes (Pink, Blue, etc.)

**Acceptance Criteria:**
- Keyboard respects dynamic colors
- Contrast ratios maintained
- All themes work correctly

---

### Task 3.4: Theme System Cleanup (2-3 hours)

**Description:** Consolidate theme definitions and remove duplicate colors.

**Actions:**
- [ ] Review all theme definitions in `themes.xml`
- [ ] Identify duplicate color definitions
- [ ] Merge similar colors
- [ ] Document custom theme system

**Custom Themes Strategy:**

**Option A: Keep as overlays**
```xml
<style name="AppTheme.Pink" parent="AppTheme">
    <item name="colorPrimary">#FFE91E63</item>
    <!-- Override only necessary colors -->
</style>
```

**Option B: Migrate to Material3 dynamic colors**
- Let users choose system Material You colors
- Remove custom themes

**Recommendation:** Option A (preserve user choice)

**Deliverable:** Cleaned `themes.xml` and `colors.xml`

---

### Deliverables | 交付物

- ✅ Color Migration Map
- ✅ Updated keyboard drawables using Material3 tokens
- ✅ Cleaned color definitions
- ✅ Dynamic theming verified on Android 12+

---

## Phase 4: Advanced Components (Low Priority)

**Priority:** 🔵 LOW
**Estimated Effort:** 8-12 hours
**Dependencies:** Phases 1-3 complete
**Target Completion:** Week 3

### Objective | 目標

Implement advanced Material3 components to enhance UX and follow best practices.

實作進階 Material3 元件以增強使用者體驗並遵循最佳實踐。

### Task 4.1: Snackbar Implementation (3-4 hours)

**Description:** Replace Toast messages with Material3 Snackbar for better UX.

**Benefits:**
- Actionable messages (e.g., "Undo delete")
- Material3 styling
- Better visibility and timing
- Consistent with Material Design guidelines

**Implementation Strategy:**

**Step 1: Identify Toast usage**
```bash
grep -r "Toast.makeText" LimeStudio/app/src/main/java/
```

**Step 2: Replace with Snackbar**
```java
// Before
Toast.makeText(context, "Word deleted", Toast.LENGTH_SHORT).show();

// After
Snackbar.make(view, "Word deleted", Snackbar.LENGTH_SHORT)
    .setAction("UNDO", v -> restoreWord())
    .setAnchorView(findViewById(R.id.bottom_bar))
    .show();
```

**Use Cases:**
- Word added/deleted confirmations
- Settings saved notifications
- Error messages
- Network status updates

**Compose Integration:**
```kotlin
// For Compose screens, use SnackbarHost
val snackbarHostState = remember { SnackbarHostState() }

Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
) {
    // Content
}

// Show snackbar
scope.launch {
    snackbarHostState.showSnackbar("Word deleted")
}
```

**Acceptance Criteria:**
- All critical Toast messages replaced
- Snackbar positioned correctly
- Action buttons work (where applicable)
- Consistent styling across app

---

### Task 4.2: Chip Implementation (2-3 hours)

**Description:** Use Material3 Chip for tags, filters, and selections.

**Use Cases:**

**1. Input Method Selection:**
```kotlin
// Replace ToggleButton with Chip
ChipGroup {
    Chip(
        selected = selectedIm == "phonetic",
        onClick = { selectIm("phonetic") },
        label = { Text("注音") }
    )
    Chip(
        selected = selectedIm == "dayi",
        onClick = { selectIm("dayi") },
        label = { Text("大易") }
    )
}
```

**2. Search Filters:**
```kotlin
FilterChip(
    selected = showCustomOnly,
    onClick = { showCustomOnly = !showCustomOnly },
    label = { Text("Custom words only") },
    leadingIcon = {
        Icon(Icons.Default.FilterList, null)
    }
)
```

**3. Keyboard Theme Selection:**
- Replace radio buttons with Chip selection
- Visual, clickable chips for theme preview

**Benefits:**
- Better visual hierarchy
- More interactive
- Space-efficient
- Modern Material3 appearance

**Acceptance Criteria:**
- Chips used for appropriate selections
- Proper selection state
- Accessible (TalkBack compatible)

---

### Task 4.3: MaterialCardView Enhancement (2-3 hours)

**Description:** Upgrade existing CardView to MaterialCardView with proper elevation.

**Current Usage:**
- Word items in grid
- Candidate suggestions
- Settings categories (if applicable)

**Enhancement:**
```xml
<!-- Before -->
<androidx.cardview.widget.CardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="8dp">
    <!-- Content -->
</androidx.cardview.widget.CardView>

<!-- After -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardElevation="2dp"
    app:cardCornerRadius="12dp"
    app:strokeColor="?attr/colorOutline"
    app:strokeWidth="1dp"
    app:rippleColor="?attr/colorPrimaryContainer">
    <!-- Content -->
</com.google.android.material.card.MaterialCardView>
```

**Features:**
- Stroke borders (Material3 style)
- Proper elevation levels
- Ripple effects on click
- State layers for interactions

**Acceptance Criteria:**
- All CardView replaced with MaterialCardView
- Consistent elevation levels
- Proper ripple animations

---

### Task 4.4: Extended FAB & Bottom App Bar (2-3 hours)

**Description:** Consider adding ExtendedFloatingActionButton for primary actions.

**Potential Use Cases:**

**1. Manage IM Screen:**
```kotlin
ExtendedFloatingActionButton(
    onClick = { showAddDialog() },
    icon = { Icon(Icons.Default.Add, null) },
    text = { Text("Add Word") }
)
```

**2. Settings Screen:**
- "Reset to Defaults" action
- "Export Settings" action

**Implementation Considerations:**
- Position: Bottom-right corner
- Collapse on scroll (for space)
- Accessibility: Clear labels

**Alternative:** Bottom App Bar
- For actions like "Export", "Import", "Reset"
- Material3 styled with proper elevation

**Decision:** Evaluate during implementation based on UX

**Acceptance Criteria:**
- FAB/Bottom Bar enhances UX (not clutters)
- Proper positioning and behavior
- Accessible and discoverable

---

### Deliverables | 交付物

- ✅ Snackbar replacing Toast messages
- ✅ Chip components for selections/filters
- ✅ MaterialCardView throughout app
- ✅ FAB/Bottom Bar evaluation report

---

## Resource Allocation | 資源分配

### Time Breakdown | 時間分解

| Phase | Priority | Effort (hours) | Week |
|-------|----------|----------------|------|
| Phase 1: Integration & Testing | Critical | 12-16 | Week 1 |
| Phase 2: XML Component Migration | High | 16-20 | Week 2 |
| Phase 3: Color System Refactoring | Medium | 8-12 | Week 2-3 |
| Phase 4: Advanced Components | Low | 8-12 | Week 3 |
| **Total** | | **44-60** | **2-3 weeks** |

### Team Requirements | 團隊需求

**Developer 1 (Lead):**
- Phase 1: Integration & Testing (full-time)
- Phase 2: Component Migration (lead)
- Code review for all phases

**Developer 2 (if available):**
- Phase 3: Color System Refactoring (parallel with Phase 2)
- Phase 4: Advanced Components

**QA/Tester:**
- Phase 1: Comprehensive testing
- Each phase: Regression testing
- Final: Full acceptance testing

### Milestones | 里程碑

**Week 1 End:**
- ✅ Phase 1 complete
- ✅ All Compose screens integrated and tested
- ✅ APK tested on multiple devices

**Week 2 End:**
- ✅ Phase 2 complete (or 80%+)
- ✅ Phase 3 started (or complete)
- ✅ XML components mostly migrated

**Week 3 End:**
- ✅ All phases complete
- ✅ 100% Material Design 3 compliance
- ✅ Final acceptance testing passed
- ✅ Production-ready APK

---

## Risk Assessment | 風險評估

### High Risk | 高風險

**Risk 1: Integration Issues with Compose Screens**

**Impact:** 🔴 High
**Probability:** 🟡 Medium
**Mitigation:**
- Thorough testing in Phase 1
- Fallback to XML versions if needed
- Incremental integration (one screen at a time)

**Risk 2: Breaking Changes in RecyclerView Migration**

**Impact:** 🟡 Medium
**Probability:** 🟡 Medium
**Mitigation:**
- Keep old code commented until testing complete
- Create feature flag for gradual rollout
- Comprehensive functional testing

---

### Medium Risk | 中風險

**Risk 3: Color System Refactoring Breaks Custom Themes**

**Impact:** 🟡 Medium
**Probability:** 🟢 Low
**Mitigation:**
- Test each theme individually
- Maintain backward compatibility
- User option to revert if needed

**Risk 4: Performance Regression**

**Impact:** 🟡 Medium
**Probability:** 🟢 Low
**Mitigation:**
- Benchmark before/after
- Profile with Android Profiler
- Optimize if needed

---

### Low Risk | 低風險

**Risk 5: APK Size Increase**

**Impact:** 🟢 Low
**Probability:** 🟡 Medium
**Mitigation:**
- Monitor APK size during development
- Use R8/ProGuard optimization
- Target <5% increase (acceptable)

---

## Success Metrics | 成功指標

### Quantitative Metrics | 定量指標

**Material Design 3 Compliance:**
- **Current:** 95%
- **Target:** 100%
- **Measurement:** Component audit checklist

**Legacy Component Count:**
- **Current:** 60+ legacy components
- **Target:** 0 critical legacy components
- **Measurement:** XML/code scan

**Dynamic Theming Coverage:**
- **Current:** 75% (UI only, keyboard uses static colors)
- **Target:** 95%+ (include keyboard)
- **Measurement:** Color token usage analysis

**APK Size:**
- **Current:** ~50MB
- **Target:** <52.5MB (<5% increase)
- **Measurement:** APK analyzer

**Test Coverage:**
- **Current:** Unknown
- **Target:** 80%+ for migrated components
- **Measurement:** JaCoCo coverage report

---

### Qualitative Metrics | 定性指標

**User Experience:**
- Consistent Material3 design throughout
- Smooth animations and transitions
- Proper touch target sizes
- Accessible (TalkBack compatible)

**Code Quality:**
- No deprecated API warnings
- Clean architecture (MVVM where applicable)
- Proper error handling
- Documented code

**Maintainability:**
- Reduced code duplication
- Clear component hierarchy
- Easy to update in future

---

## Acceptance Criteria | 驗收標準

### Phase 1: Integration & Testing

- [ ] All Compose screens accessible from MainActivity
- [ ] APK installs on Android 11, 14, 16
- [ ] No crashes during navigation
- [ ] All features functional
- [ ] Dark theme works correctly
- [ ] Configuration changes handled properly
- [ ] Test report completed

---

### Phase 2: XML Component Migration

- [ ] All Button → MaterialButton
- [ ] All EditText → TextInputLayout + TextInputEditText
- [ ] All ListView → RecyclerView
- [ ] All GridView → RecyclerView with GridLayoutManager
- [ ] Complex layouts use ConstraintLayout
- [ ] No visual regressions
- [ ] Performance maintained or improved

---

### Phase 3: Color System Refactoring

- [ ] Color migration map completed
- [ ] Keyboard uses Material3 color tokens
- [ ] Dynamic theming works on Android 12+
- [ ] Custom themes still functional
- [ ] colors.xml cleaned (duplicates removed)
- [ ] Proper contrast ratios maintained
- [ ] WCAG accessibility standards met

---

### Phase 4: Advanced Components

- [ ] Critical Toast → Snackbar migrations complete
- [ ] Chip components implemented where appropriate
- [ ] All CardView → MaterialCardView
- [ ] FAB/Bottom Bar evaluated (implemented if beneficial)
- [ ] Consistent Material3 styling
- [ ] No UX degradation

---

### Final Acceptance

- [ ] **100% Material Design 3 compliance**
- [ ] All phases complete
- [ ] All tests passing
- [ ] APK size within target (<5% increase)
- [ ] Performance benchmarks acceptable
- [ ] Code review approved
- [ ] Documentation updated
- [ ] User acceptance testing passed
- [ ] Production deployment approved

---

## Implementation Order | 實作順序

### Week 1: Critical Path

**Day 1-2: Phase 1.1-1.2**
- MainActivity integration
- Build and install testing

**Day 3-4: Phase 1.3-1.4**
- Visual and functional testing
- Edge case testing
- Bug fixes

**Day 5: Buffer**
- Address any critical issues
- Begin Phase 2 planning

---

### Week 2: High Priority Work

**Day 1-2: Phase 2.1-2.3**
- Component inventory
- MaterialButton migration
- TextInputLayout migration

**Day 3-4: Phase 2.4-2.5**
- RecyclerView migration
- Layout optimization

**Day 5: Phase 3.1-3.2**
- Color audit
- Material3 token mapping

---

### Week 3: Medium/Low Priority

**Day 1-2: Phase 3.3-3.4**
- Keyboard color refactoring
- Theme system cleanup

**Day 3-4: Phase 4.1-4.2**
- Snackbar implementation
- Chip implementation

**Day 5: Phase 4.3-4.4 + Final Testing**
- MaterialCardView enhancement
- FAB evaluation
- Final acceptance testing

---

## Dependencies & Prerequisites | 依賴項與前置條件

### Prerequisites | 前置條件

**Environment:**
- [ ] Android Studio Ladybug 2024.2.1+
- [ ] Android SDK 30-36 installed
- [ ] Gradle 8.13.2+
- [ ] Git repository up to date

**Knowledge:**
- [ ] Material Design 3 guidelines
- [ ] Jetpack Compose basics
- [ ] AndroidX components
- [ ] WFIME architecture understanding

**Documentation:**
- [ ] PHASE4_PROGRESS.md reviewed
- [ ] UX-UI-Design-Analysis.md reviewed
- [ ] AndroidX-Preference-Migration.md reviewed

---

### External Dependencies | 外部依賴

**Libraries (already in project):**
- ✅ Material Design 3: 1.12.0
- ✅ Jetpack Compose BOM: 2024.02.00
- ✅ AndroidX Core: 1.15.0

**No new dependencies required** - all needed libraries already included.

---

## Testing Strategy | 測試策略

### Unit Testing | 單元測試

**Focus Areas:**
- ViewModel state management
- Color token resolution
- Component behavior

**Tools:**
- JUnit 4.13.2
- Mockito 5.11.0
- Robolectric 4.11.1

**Target Coverage:** 80%+

---

### Integration Testing | 整合測試

**Focus Areas:**
- Compose-Java interop
- Navigation flows
- Database operations
- SharedPreferences persistence

**Tools:**
- Espresso 3.5.1
- Compose UI testing

**Target Coverage:** Key user flows

---

### UI Testing | UI 測試

**Manual Testing:**
- Visual appearance
- Interaction feedback
- Animation smoothness
- Accessibility (TalkBack)

**Devices:**
- Android 11 (minimum SDK)
- Android 14 (dynamic colors)
- Tablet (layout adaptation)

**Scenarios:**
- Light/dark theme
- Portrait/landscape
- Different screen sizes
- Low memory conditions

---

### Acceptance Testing | 驗收測試

**Criteria:**
- All features work as before
- Material3 styling consistent
- No performance regression
- User satisfaction (if possible to gather feedback)

---

## Rollback Plan | 回滾計劃

### If Issues Found | 如果發現問題

**Phase 1 Issues:**
- Revert MainActivity changes
- Use XML versions temporarily
- Fix and re-test

**Phase 2 Issues:**
- Keep feature flag for new components
- Gradual rollout (A/B testing if possible)
- Revert specific components if needed

**Phase 3 Issues:**
- Revert color changes
- Keep old color definitions temporarily
- Fix theme issues one by one

**Phase 4 Issues:**
- Low risk (additive changes)
- Simply disable new components
- No impact on core functionality

---

## Post-Completion Tasks | 完成後任務

### Documentation Updates | 文件更新

- [ ] Update README.md (Material3 compliance: 100%)
- [ ] Update CHANGELOG.md with all changes
- [ ] Create Material3 component guide for future developers
- [ ] Update CONTRIBUTING.md with new standards

### Code Cleanup | 程式碼清理

- [ ] Remove commented-out legacy code
- [ ] Delete unused resources
- [ ] Optimize imports
- [ ] Run code formatter

### Performance Optimization | 效能優化

- [ ] Profile with Android Profiler
- [ ] Optimize layouts (reduce overdraw)
- [ ] Optimize images (if any added)
- [ ] Run R8/ProGuard and verify

### Release Preparation | 發布準備

- [ ] Create release notes
- [ ] Update version code/name
- [ ] Build release APK
- [ ] Test release APK on devices
- [ ] Create GitHub release
- [ ] Update app store listing (if applicable)

---

## Monitoring & Metrics | 監控與指標

### During Development | 開發期間

**Daily:**
- Build success rate
- Test pass rate
- Code coverage

**Weekly:**
- APK size tracking
- Performance benchmarks
- Bug count

---

### Post-Release | 發布後

**Metrics to Monitor:**
- Crash rate (should remain stable)
- User ratings (should improve)
- Performance metrics (should improve)
- User feedback

**Target:**
- Crash-free rate: >99.5%
- App rating: Maintain or improve
- User complaints: No increase in UI-related issues

---

## Conclusion | 結論

This comprehensive plan provides a structured approach to achieving 100% Material Design 3 compliance for WFIME. By following the phased approach with clear priorities, the team can deliver consistent improvements while minimizing risk.

此全面計劃提供了一個結構化的方法，以實現 WFIME 100% Material Design 3 合規性。通過遵循具有明確優先級的階段性方法，團隊可以在最小化風險的同時提供一致的改進。

**Key Success Factors:**
- Prioritize critical integration first
- Test thoroughly at each phase
- Maintain backward compatibility
- Focus on user experience improvements

**Expected Outcome:**
- Modern, consistent Material3 UI
- Improved user experience
- Better maintainability
- Future-proof architecture

---

**Document Version:** 1.0
**Created:** 2026-01-20
**Author:** Claude Sonnet 4.5
**Project:** WFIME - Wheat Fields Input Method Editor
**Status:** Ready for Implementation

---

## Quick Reference Checklist | 快速參考檢查清單

### Phase 1: Integration & Testing ⏱️ 12-16h
- [ ] 1.1 MainActivity Integration (4-6h)
- [ ] 1.2 Build & Device Testing (3-4h)
- [ ] 1.3 Visual & Functional Testing (3-4h)
- [ ] 1.4 Edge Case Testing (2h)

### Phase 2: XML Component Migration ⏱️ 16-20h
- [ ] 2.1 Component Inventory (2h)
- [ ] 2.2 MaterialButton Migration (3-4h)
- [ ] 2.3 TextInputLayout Migration (4-5h)
- [ ] 2.4 RecyclerView Migration (6-8h)
- [ ] 2.5 Layout Optimization (3-4h)

### Phase 3: Color System Refactoring ⏱️ 8-12h
- [ ] 3.1 Color Audit (2h)
- [ ] 3.2 Material3 Token Mapping (3-4h)
- [ ] 3.3 Keyboard Color Refactoring (3-4h)
- [ ] 3.4 Theme System Cleanup (2-3h)

### Phase 4: Advanced Components ⏱️ 8-12h
- [ ] 4.1 Snackbar Implementation (3-4h)
- [ ] 4.2 Chip Implementation (2-3h)
- [ ] 4.3 MaterialCardView Enhancement (2-3h)
- [ ] 4.4 Extended FAB & Bottom App Bar (2-3h)

### Final Checklist ✅
- [ ] All phases complete
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Production APK ready
- [ ] 100% Material Design 3 compliance achieved

---

**Total Estimated Effort:** 44-60 hours (2-3 weeks)
**Priority Order:** Phase 1 → Phase 2 → Phase 3 → Phase 4
**Target Completion:** End of Week 3
