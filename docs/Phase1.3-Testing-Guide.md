# Phase 1.3: Visual & Functional Testing Guide
# Phase 1.3: 視覺與功能測試指南

**Date:** 2026-01-20
**Phase:** 1.3 - Visual & Functional Testing
**Status:** 📋 READY TO BEGIN
**APK:** app-debug.apk (63MB)

---

## Overview | 概述

Phase 1.3 focuses on comprehensive testing of all Compose screens that have been integrated in Phase 1.1-1.2. This is a critical validation phase to ensure:

Phase 1.3 專注於全面測試在 Phase 1.1-1.2 中整合的所有 Compose 畫面。這是一個關鍵的驗證階段，以確保：

- ✅ All screens are accessible via navigation
- ✅ Material3 styling is consistent
- ✅ Functionality works as expected
- ✅ Database operations succeed
- ✅ User interactions are smooth

---

## Test Environment Setup | 測試環境設置

### Required Devices | 所需裝置

**Minimum Requirements:**
- 1 Android device or emulator (Android 11+)

**Recommended Setup:**
- Android 11 device (API 30) - Minimum SDK test
- Android 14 device (API 34) - Material You dynamic theming test
- Android 16 emulator (API 36) - Target SDK test

**Alternative:** Use Android Studio AVD Manager to create emulators

---

### Installation Steps | 安裝步驟

#### Method 1: ADB Install (Recommended)

**Prerequisites:**
- Android device connected via USB
- USB debugging enabled
- ADB installed on computer

**Steps:**
```bash
# Navigate to APK directory
cd C:\Users\nan\CodeSpace\nanime-main\LimeStudio\app\build\outputs\apk\debug

# Install APK
adb install app-debug.apk

# If already installed, use -r to reinstall
adb install -r app-debug.apk

# Verify installation
adb shell pm list packages | grep nan.toload.main.hd
```

**Expected Output:**
```
package:nan.toload.main.hd
```

---

#### Method 2: Manual Install

**Steps:**
1. Copy `app-debug.apk` to device
2. Open file manager on device
3. Tap on APK file
4. Allow "Install from Unknown Sources" if prompted
5. Tap "Install"
6. Open app from launcher

---

#### Method 3: Android Studio

**Steps:**
1. Open project in Android Studio
2. Select device/emulator
3. Click "Run" button (▶)
4. Wait for installation

---

## Testing Checklist | 測試檢查清單

### 🚀 Phase 1.3.1: Installation & Launch

**Time Estimate:** 15 minutes

- [ ] **1.1 APK Installation**
  - [ ] Install via ADB successful
  - [ ] No installation errors
  - [ ] App appears in launcher
  - [ ] App icon displays correctly

- [ ] **1.2 First Launch**
  - [ ] App launches without crash
  - [ ] Splash screen displays (if any)
  - [ ] Main screen loads
  - [ ] No ANR (Application Not Responding)

- [ ] **1.3 Permissions**
  - [ ] Required permissions requested
  - [ ] Permissions granted successfully
  - [ ] App functions with permissions

**Pass Criteria:** App installs and launches successfully ✅

---

### 📱 Phase 1.3.2: Navigation Testing

**Time Estimate:** 30 minutes

#### Navigation Drawer

- [ ] **2.1 Drawer Opening**
  - [ ] Hamburger icon visible in toolbar
  - [ ] Tap icon opens drawer
  - [ ] Swipe from left edge opens drawer
  - [ ] Drawer animation smooth

- [ ] **2.2 Menu Items**
  - [ ] "Setup" (設定) menu item visible
  - [ ] "Manage Related" (管理相關) menu item visible
  - [ ] "Phonetic" (注音) menu item visible
  - [ ] "Dayi" (大易) menu item visible
  - [ ] Menu items load from database
  - [ ] Material3 NavigationDrawerItem styling

- [ ] **2.3 Navigation Actions**
  - [ ] Tap "Setup" → SetupImFragment loads
  - [ ] Tap "Manage Related" → ManageRelatedFragment loads
  - [ ] Tap "Phonetic" → ManageImScreen (Compose) loads
  - [ ] Tap "Dayi" → ManageImScreen (Compose) loads
  - [ ] Drawer closes after selection
  - [ ] Screen title updates

**Pass Criteria:** All navigation works smoothly ✅

---

#### Settings Access

- [ ] **2.4 Settings Menu**
  - [ ] Toolbar overflow menu (⋮) visible
  - [ ] Tap overflow → Settings option visible
  - [ ] Tap "Settings" → SettingsScreen opens
  - [ ] Toolbar shows "Settings" title
  - [ ] Back button visible

- [ ] **2.5 Settings Navigation**
  - [ ] Settings screen loads completely
  - [ ] Compose SettingsScreen renders
  - [ ] No fragment shown (Compose only)
  - [ ] Scroll works smoothly

**Pass Criteria:** Settings accessible and renders correctly ✅

---

### 🎨 Phase 1.3.3: Material3 Visual Testing

**Time Estimate:** 30 minutes

#### Theme & Colors

- [ ] **3.1 Material3 Theme**
  - [ ] App uses Material3 components
  - [ ] Light theme displays correctly
  - [ ] Primary color matches design
  - [ ] Surface colors appropriate
  - [ ] Text contrast readable

- [ ] **3.2 Dynamic Colors (Android 12+)**
  - [ ] Material You enabled
  - [ ] Colors adapt to wallpaper
  - [ ] Primary color updates
  - [ ] Surface colors update
  - [ ] Navigation drawer reflects theme

- [ ] **3.3 Component Styling**
  - [ ] Toolbar: MaterialToolbar style
  - [ ] Buttons: MaterialButton style
  - [ ] Cards: MaterialCardView style
  - [ ] Text fields: Material3 style
  - [ ] Switches: Material3 toggle

**Pass Criteria:** Consistent Material3 styling throughout ✅

---

#### Typography & Spacing

- [ ] **3.4 Typography**
  - [ ] Headlines readable
  - [ ] Body text appropriate size
  - [ ] Labels clear
  - [ ] Material3 type scale followed

- [ ] **3.5 Spacing**
  - [ ] Adequate padding
  - [ ] Proper margins
  - [ ] Touch targets >48dp
  - [ ] Visual hierarchy clear

**Pass Criteria:** Good readability and spacing ✅

---

### ⚙️ Phase 1.3.4: Settings Screen Testing

**Time Estimate:** 45 minutes

#### Settings Categories

- [ ] **4.1 Keyboard Category (15 preferences)**
  - [ ] Category header displays
  - [ ] Keyboard Theme (List) works
  - [ ] Enable Emoji (Switch) toggles
  - [ ] Emoji Position (List) shows dialog
  - [ ] Persistent Language Mode (Switch)
  - [ ] Number Row in English (Switch)
  - [ ] Hide Software Keyboard (Switch)
  - [ ] Show Arrow Keys (List)
  - [ ] Split Keyboard Mode (List)
  - [ ] Keyboard Size (List)
  - [ ] Font Size (List)
  - [ ] Vibrate on Keypress (Switch)
  - [ ] Vibrate Level (List)
  - [ ] Sound on Keypress (Switch)
  - [ ] Switch to English (Switch - 2 items)

- [ ] **4.2 IM Category (8 preferences)**
  - [ ] Smart Chinese Input (Switch)
  - [ ] Auto Chinese Symbol (Switch)
  - [ ] Disable Physical Selection (Switch)
  - [ ] Auto Commit (List)
  - [ ] Selection Key Option (List)
  - [ ] Phonetic Keyboard Type (List) ⚠️ CRITICAL
  - [ ] Physical Keyboard Type (List)
  - [ ] Reverse Lookup Notify (Switch)

- [ ] **4.3 Mapping Category (11 preferences)**
  - [ ] Similar Character List Size (List)
  - [ ] Enable Similar Characters (Switch)
  - [ ] Enable English Dictionary (Switch)
  - [ ] English Dictionary Physical (Switch)
  - [ ] Candidate Switch (Switch)
  - [ ] Candidate Suggestion (Switch)
  - [ ] Learn Phrases (Switch)
  - [ ] Learning Switch (Switch)
  - [ ] Physical Keyboard Sort (Switch)
  - [ ] Accept Number Index (Switch)
  - [ ] Accept Symbol Index (Switch)

**Pass Criteria:** All 34 preferences display and interact correctly ✅

---

#### Preference Interactions

- [ ] **4.4 Switch Preferences**
  - [ ] Toggle on/off works
  - [ ] Visual state updates immediately
  - [ ] Change persists after navigation
  - [ ] No lag or stutter

- [ ] **4.5 List Preferences**
  - [ ] Tap shows radio dialog
  - [ ] Options display correctly
  - [ ] Selection highlights
  - [ ] Tap option closes dialog
  - [ ] Change persists

- [ ] **4.6 Critical: Phonetic Keyboard Type**
  - [ ] Tap "Phonetic Keyboard Type"
  - [ ] Radio dialog shows options:
    - [ ] Standard
    - [ ] ET26
    - [ ] ET
    - [ ] Hsu
    - [ ] IBM
  - [ ] Select "ET26"
  - [ ] Dialog closes
  - [ ] Database updates (verify in Manage IM)
  - [ ] Keyboard object changes

**Test Steps:**
```
1. Navigate to Settings
2. Scroll to "IM" category
3. Tap "Phonetic Keyboard Type"
4. Select different option (e.g., ET26)
5. Go back to main screen
6. Navigate to "Phonetic" in drawer
7. Verify keyboard info updated
```

**Pass Criteria:** Phonetic keyboard type change works correctly ⚠️ CRITICAL ✅

---

#### Preference Persistence

- [ ] **4.7 Data Persistence**
  - [ ] Change multiple preferences
  - [ ] Close settings
  - [ ] Reopen settings
  - [ ] All changes still present
  - [ ] No data loss

- [ ] **4.8 App Restart**
  - [ ] Change preferences
  - [ ] Close app completely
  - [ ] Reopen app
  - [ ] Open settings
  - [ ] Changes persisted

**Pass Criteria:** All preferences persist correctly ✅

---

### 📝 Phase 1.3.5: Manage IM Screen Testing

**Time Estimate:** 45 minutes

#### Screen Layout

- [ ] **5.1 Initial Load**
  - [ ] Navigate to "Phonetic" or "Dayi"
  - [ ] ManageImScreen (Compose) loads
  - [ ] Word grid displays (3 columns)
  - [ ] Search bar visible
  - [ ] Pagination controls visible
  - [ ] Loading indicator during load

- [ ] **5.2 Word Grid**
  - [ ] Words display in cards
  - [ ] 3-column grid layout
  - [ ] Card styling Material3
  - [ ] Code and word both visible
  - [ ] Score displays (if >0)

- [ ] **5.3 Search Bar**
  - [ ] Search field visible
  - [ ] Placeholder text clear
  - [ ] Can tap to focus
  - [ ] Keyboard appears

**Pass Criteria:** Screen layout renders correctly ✅

---

#### Search Functionality

- [ ] **5.4 Search Operations**
  - [ ] Type in search field
  - [ ] Grid filters in real-time
  - [ ] Only matching words shown
  - [ ] Clear search → all words return
  - [ ] Search by code works
  - [ ] Search by word works

**Test Cases:**
```
1. Search "a" → filters words with "a"
2. Search "test" → filters accordingly
3. Clear search → all words return
4. Search non-existent → empty state
```

**Pass Criteria:** Search filters correctly ✅

---

#### Pagination

- [ ] **5.5 Pagination Controls**
  - [ ] Page indicator shows "Page X of Y"
  - [ ] "Previous" button visible
  - [ ] "Next" button visible
  - [ ] First page: Previous disabled
  - [ ] Last page: Next disabled

- [ ] **5.6 Page Navigation**
  - [ ] Tap "Next" → loads next 30 words
  - [ ] Grid updates
  - [ ] Page indicator updates
  - [ ] Tap "Previous" → loads previous
  - [ ] Smooth transitions

**Pass Criteria:** Pagination works correctly ✅

---

#### Word Dialogs

- [ ] **5.7 Add Word Dialog**
  - [ ] Tap "+" or "Add" button
  - [ ] Add dialog appears
  - [ ] Title: "Add Word"
  - [ ] Code field empty
  - [ ] Word field empty
  - [ ] Score starts at 0
  - [ ] Material3 dialog styling

- [ ] **5.8 Add Word Validation**
  - [ ] Leave code empty → error
  - [ ] Leave word empty → error
  - [ ] Enter valid data → no error
  - [ ] Tap "Save" → adds to database
  - [ ] New word appears in grid
  - [ ] Dialog closes

- [ ] **5.9 Edit Word Dialog**
  - [ ] Tap existing word card
  - [ ] Edit dialog appears
  - [ ] Title: "Edit Word"
  - [ ] Code pre-filled
  - [ ] Word pre-filled
  - [ ] Score pre-filled
  - [ ] Can modify fields

- [ ] **5.10 Edit Word Actions**
  - [ ] Modify code → updates
  - [ ] Modify word → updates
  - [ ] Increment score → +1
  - [ ] Decrement score → -1
  - [ ] Tap "Save" → updates database
  - [ ] Changes reflect in grid

- [ ] **5.11 Delete Word**
  - [ ] In edit dialog, tap "Delete"
  - [ ] Nested confirmation dialog appears
  - [ ] Confirm delete → removes from database
  - [ ] Word disappears from grid
  - [ ] Cancel delete → keeps word

**Pass Criteria:** All dialog operations work correctly ✅

---

#### Database Operations

- [ ] **5.12 Database Integration**
  - [ ] Add word → persists
  - [ ] Edit word → persists
  - [ ] Delete word → removes
  - [ ] Navigate away and back → changes persist
  - [ ] No data loss
  - [ ] No duplicate entries

**Pass Criteria:** Database operations reliable ✅

---

### 🎭 Phase 1.3.6: Navigation Drawer Screen Testing

**Time Estimate:** 15 minutes

- [ ] **6.1 Menu Item Loading**
  - [ ] Items load from database
  - [ ] "Setup" at position 0
  - [ ] "Manage Related" at position 1
  - [ ] Input methods (Phonetic, Dayi, etc.) follow
  - [ ] Correct item count

- [ ] **6.2 Selection State**
  - [ ] Current screen highlighted
  - [ ] Tap item → selection updates
  - [ ] Visual highlight clear
  - [ ] Material3 selected state

- [ ] **6.3 Interaction**
  - [ ] Smooth scrolling (if many items)
  - [ ] Tap detection accurate
  - [ ] Drawer closes on selection
  - [ ] Screen transitions smooth

**Pass Criteria:** Navigation drawer fully functional ✅

---

### 🔄 Phase 1.3.7: Back Navigation Testing

**Time Estimate:** 15 minutes

- [ ] **7.1 Back Button**
  - [ ] Hardware back button works
  - [ ] Gesture navigation works
  - [ ] OnBackPressedCallback active
  - [ ] No System.exit() call

- [ ] **7.2 Navigation Stack**
  - [ ] From ManageImScreen → returns to previous
  - [ ] From Settings → returns to main
  - [ ] From dialogs → closes dialog
  - [ ] Back to main → exits app

- [ ] **7.3 Predictive Back (Android 13+)**
  - [ ] Swipe back shows preview
  - [ ] Release completes navigation
  - [ ] Cancel returns to current

**Pass Criteria:** Back navigation works correctly ✅

---

## Test Results Template | 測試結果範本

### Summary Report

**Date:** _____________
**Tester:** _____________
**Device:** _____________
**Android Version:** _____________

---

### Results

| Phase | Test | Pass | Fail | Notes |
|-------|------|------|------|-------|
| 1.3.1 | Installation | ☐ | ☐ | |
| 1.3.2 | Navigation | ☐ | ☐ | |
| 1.3.3 | Material3 Visual | ☐ | ☐ | |
| 1.3.4 | Settings Screen | ☐ | ☐ | |
| 1.3.5 | Manage IM Screen | ☐ | ☐ | |
| 1.3.6 | Navigation Drawer | ☐ | ☐ | |
| 1.3.7 | Back Navigation | ☐ | ☐ | |

---

### Issues Found

**Critical Issues:** (Count: ___)
1. _____________________________________________
2. _____________________________________________

**Major Issues:** (Count: ___)
1. _____________________________________________
2. _____________________________________________

**Minor Issues:** (Count: ___)
1. _____________________________________________
2. _____________________________________________

---

### Screenshots

Attach screenshots for:
- ✅ Main screen (Navigation drawer open)
- ✅ Settings screen (all categories)
- ✅ Manage IM screen (word grid)
- ✅ Add word dialog
- ✅ Edit word dialog
- ✅ Material You dynamic colors (Android 12+)
- ❌ Any visual bugs found

---

### Performance Notes

**App Responsiveness:**
- Launch time: ________ seconds
- Navigation speed: ☐ Fast ☐ Normal ☐ Slow
- Database operations: ☐ Fast ☐ Normal ☐ Slow
- UI smoothness: ☐ Smooth ☐ Occasional lag ☐ Frequent lag

**Memory Usage:**
- RAM usage: ________ MB
- ANR events: ☐ None ☐ Yes (describe: _____________)
- Crashes: ☐ None ☐ Yes (describe: _____________)

---

### Recommendations

**Priority 1 (Must Fix):**
1. _____________________________________________
2. _____________________________________________

**Priority 2 (Should Fix):**
1. _____________________________________________
2. _____________________________________________

**Priority 3 (Nice to Have):**
1. _____________________________________________
2. _____________________________________________

---

## Expected Issues & Known Bugs | 預期問題與已知錯誤

### Non-Critical Warnings

These are expected and do not affect functionality:

1. **Kotlin Deprecated Icons**
   - Warning about `KeyboardArrowLeft/Right`
   - Impact: None (visual only)
   - Fix: Scheduled for Phase 2

2. **Java Source/Target 8**
   - Java compiler deprecation warning
   - Impact: None (still supported)
   - Fix: Future Gradle update

3. **Gradle Deprecations**
   - Various Gradle plugin warnings
   - Impact: None (future versions)
   - Fix: Monitor for Gradle 10

---

### Areas Requiring Special Attention

**Critical Tests:**

1. **Phonetic Keyboard Type Change**
   - **Why:** Complex database logic
   - **Test carefully:** All 5 options
   - **Verify:** Database updates correctly

2. **Preference Persistence**
   - **Why:** New Compose implementation
   - **Test carefully:** App restart
   - **Verify:** No data loss

3. **Word CRUD Operations**
   - **Why:** Core functionality
   - **Test carefully:** Add, edit, delete
   - **Verify:** Database integrity

---

## Troubleshooting | 故障排除

### Common Issues

**Issue: APK Won't Install**
- Solution: Uninstall old version first
- Command: `adb uninstall nan.toload.main.hd`

**Issue: App Crashes on Launch**
- Check: Logcat for error messages
- Command: `adb logcat | grep -i error`

**Issue: Settings Don't Persist**
- Check: SharedPreferences directory
- Verify: Preference change listener registered

**Issue: Compose Screen Not Showing**
- Check: ComposeBridge integration
- Verify: FrameLayout container exists
- Check: Logcat for Compose errors

**Issue: Database Operations Fail**
- Check: Database file exists
- Verify: Permissions granted
- Check: SQL error messages in Logcat

---

## Success Criteria | 成功標準

### Phase 1.3 Complete When:

**Functionality:**
- [ ] All screens accessible
- [ ] All navigation works
- [ ] All preferences functional
- [ ] Database operations succeed
- [ ] No critical bugs

**Visual:**
- [ ] Material3 styling consistent
- [ ] Smooth animations
- [ ] Proper spacing
- [ ] Readable text

**Performance:**
- [ ] <2 second launch time
- [ ] No ANR
- [ ] No crashes
- [ ] Smooth scrolling

**Data Integrity:**
- [ ] Preferences persist
- [ ] Database updates correctly
- [ ] No data loss

---

## Next Steps | 下一步

### After Phase 1.3:

**If Tests Pass:**
→ Proceed to Phase 1.4: Edge Case Testing

**If Tests Fail:**
→ Document bugs
→ Fix critical issues
→ Re-test
→ Proceed when stable

---

## Documentation | 文件記錄

### Required Deliverables

1. **Test Results Form** (filled out)
2. **Screenshots** (6+ images)
3. **Issue Report** (if bugs found)
4. **Video Recording** (optional, recommended)

### Save To:
```
docs/testing/
  ├── phase1.3-test-results.md
  ├── screenshots/
  │   ├── main-screen.png
  │   ├── settings-screen.png
  │   ├── manage-im-screen.png
  │   ├── add-dialog.png
  │   ├── edit-dialog.png
  │   └── material-you.png
  └── screen-recording.mp4 (optional)
```

---

## Timeline | 時間表

**Total Estimated Time:** 3-4 hours

| Task | Time | Priority |
|------|------|----------|
| Installation & Launch | 15 min | Critical |
| Navigation Testing | 30 min | Critical |
| Material3 Visual | 30 min | High |
| Settings Screen | 45 min | Critical |
| Manage IM Screen | 45 min | Critical |
| Navigation Drawer | 15 min | High |
| Back Navigation | 15 min | High |
| Documentation | 30 min | Medium |

**Recommended Schedule:**
- Day 1: Sections 1.3.1-1.3.4 (2 hours)
- Day 2: Sections 1.3.5-1.3.7 + Documentation (2 hours)

---

## Conclusion | 結論

Phase 1.3 is a comprehensive functional and visual validation of the Compose integration work completed in Phases 1.1-1.2. Successful completion of this phase confirms:

Phase 1.3 是對 Phase 1.1-1.2 中完成的 Compose 整合工作的全面功能和視覺驗證。成功完成此階段確認：

- ✅ 100% Compose integration is stable
- ✅ Material3 implementation is correct
- ✅ Database operations work reliably
- ✅ User experience is smooth

**Ready to begin testing!** 📱✨

---

**Document Version:** 1.0
**Created:** 2026-01-20
**Phase:** 1.3 Testing Guide
**Status:** Ready for Execution
