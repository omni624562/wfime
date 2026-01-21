# WFIME Project Analysis & Roadmap | 專案分析與路線圖

**Analysis Date:** 2026-01-21
**Branch:** `main`
**Version:** `2026.01.20`

---

## 1. Current Branch Status | 目前分支狀態

The `main` branch is stable and includes recent modernizations:
- **Material 3 UI**: Full integration for keyboard styling (light/dark themes).
- **Android 16 Compliance**: Target SDK 36, `OnBackPressedDispatcher` migration complete.
- **Documentation**: Comprehensive documentation infrastructure in `docs/`.

### Active Feature Branches (Unmerged) | 活躍功能分支（未合併）
- `feature/ui-adjustments`: Contains critical fixes for **Keyboard Auto-Close/Flicker** and **Compose CandidateView** improvements.
- `feature/optimization`: Largely redundant (merged preference migration), likely safe to delete after verification.

---

## 2. Technical Debt & Modernization Analysis | 技術債與現代化分析

### Critical (Immediate Action) | 關鍵（立即行動）
1.  **Legacy List Components**:
    - **Issue**: Extensive use of `ListView` (NavigationDrawer, settings) and `GridView` (Management screens).
    - **Impact**: Performance degradation and lack of modern scrolling features.
    - **Plan**: Migrate to `RecyclerView`.

2.  **UI Components outdated**:
    - **Issue**: Standard `Button` and `EditText` usage throughout fragments.
    - **Modernization**: Replace with `MaterialButton` and `TextInputLayout` / `TextInputEditText`.

3.  **Layout Architecture**:
    - **Issue**: Deeply nested `LinearLayout` hierarchies and overuse of `RelativeLayout`.
    - **Impact**: Rendering performance overhead.
    - **Plan**: Flatten UI using `ConstraintLayout`.

4.  **Hardware Keyboard Compatibility**:
    - **Issue**: `HardKeyHelper.java` and `LIMEKeyboardSwitcher` rely on older event handling.
    - **Plan**: Review against Android 13+ hardware keyboard support APIs.

### Cleanup Candidates | 清理候選
- **ProgressDialog**: Mostly replaced by `LoadingDialog` (Compose), but code comments indicate some wrapper methods remain for API compatibility. These should be deprecated/removed.
- **System.exit()**: Removed from main flow, but verify no debug/testing remnants exist.
- **Lint Suppressions**: 13 locations with `@Suppress` / `@SuppressLint` (e.g., `ForegroundServiceType`, `InflateParams`) need review to see if underlying issues can be fixed.

---

## 3. Architecture Optimization Opportunities | 架構優化機會

### Jetpack Compose Migration
- **Current State**: ~15% adoption (CandidateView, EmojiPicker, LoadingDialog).
- **Target**:
    - **Phase 1**: Convert Setup/Settings screens to Compose.
    - **Phase 2**: Convert Main Activity UI to Compose (scaffold).
    - **Phase 3**: Custom Keyboard View (Long-term goal, possibly keep legacy View for performance if needed).

### Input Method Service
- **Refactoring**: `LIMEService.java` is large (170KB+).
- **Optimization**: Extract logic into distinct managers (ServiceLifecycleManager, InputConnectionManager) to improve testability.

---

## 4. Recommended Next Steps | 建議後續步驟

1.  **Merge `feature/ui-adjustments`**: Bring in the keyboard flicker fixes.
2.  **UI Cleanup Sprint**:
    - Replace all `Button` -> `MaterialButton`.
    - Replace `ListView` -> `RecyclerView` in Navigation Drawer.
3.  **ConstraintLayout Migration**: Tackle `fragment_manage_im.xml` first (highest complexity).
