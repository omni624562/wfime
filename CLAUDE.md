# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview | 專案概述

WFIME (Wheat Fields Input Method Editor) is a fork of LimeIME — an Android IME (Input Method Editor) supporting multiple Chinese input methods. Licensed under GPLv3.

- **Package:** `net.toload.main.hd`
- **Min SDK:** 30 / **Target SDK:** 36 / **Compile SDK:** 36
- **Build flavors:** `phone` / `tablet` (differ by `IS_TABLET` flag and `applicationId` suffix)
- **Languages:** Java (primary) + Kotlin (newer features, Compose)
- **UI:** Mixed XML layouts + Jetpack Compose (migration in progress)
- **Theming:** Material Design 3 with dynamic colors
- **Bilingual:** English + Traditional Chinese (zh-TW)
- **Version:** Managed in `LimeStudio/version.properties` (major.minor.patch); versionCode auto-generated as `yyyyMMdd`

## Build & Test | 建置與測試

All Gradle commands run from `LimeStudio/`:

```bash
# Build debug APK (phone flavor)
./gradlew assemblePhoneDebug

# Install to connected device
./gradlew installPhoneDebug

# Run all unit tests
./gradlew testPhoneDebugUnitTest

# Run a single test class
./gradlew testPhoneDebugUnitTest --tests "net.toload.main.hd.limedb.LimeDBValidationTest"

# Run lint
./gradlew lintPhoneDebug
```

Signing config is loaded from `keystore.properties` (excluded from git). If missing, falls back to the Android debug keystore.

## Architecture | 架構

### IME Service Layer

`LIMEService.java` is the central `InputMethodService`. It owns the view hierarchy and delegates to:

| Class | Role |
|---|---|
| `LIMEKeyboardSwitcher` | Tracks current keyboard mode (text/symbol/phone/number/IM) and inflates the right XML keyboard |
| `CandidateController` | Manages candidate view display and selection logic (extracted from `LIMEService`) |
| `SearchServer` | Looks up candidates from the DB; caches up to 512 entries; runs queries off the main thread |
| `DBServer` | Handles DB import/export, zip packing, and IM loading; triggers `LIMEProgressListener` callbacks |
| `InputModeHelper` (Kotlin) | Pure-function; determines keyboard mode from `EditorInfo` flags at input focus time |
| `TextCompositionManager` | Manages composing text state sent to the active `InputConnection` |
| `PhysicalKeyHandler` | Handles hardware keyboard key events |
| `HardKeyHelper` | Maps hardware key codes to IME actions |

### Database Layer

`LimeDB.java` (~210 KB) extends `LimeSQLiteOpenHelper` and contains all raw SQL. It is the **only** class that touches SQLite directly. Known SQL injection risks from legacy code — see `docs/SECURITY_ANALYSIS.md`. Do not add new raw string concatenation queries; use parameterized statements.

`EmojiConverter.java` and `LimeHanConverter.java` are smaller DB helpers for emoji lookup and Han character conversion respectively.

Database files live in `../Database/` relative to the repo root (outside `LimeStudio/`).

### Keyboard Views (XML-based)

Keyboard layout is defined in `res/xml/` (e.g., `lime_dayi.xml`, `lime_phonetic.xml`, `symbols1.xml`). The view hierarchy:

```
LIMEKeyboardBaseView  ←  base drawing + touch dispatch
  └── LIMEKeyboardView  ← IME-specific codes (KEYCODE_OPTIONS, KEYCODE_NEXT_IM, etc.)
        └── PointerTracker / KeyDetector  ← multi-touch & proximity detection
```

### Candidate Views

Three candidate view implementations coexist:
- `CandidateViewContainer` — full standalone bar (shown above keyboard)
- `CandidateInInputViewContainer` — embedded inside the input view
- `CandidateView.kt` (Compose) — Compose reimplementation of the candidate row

### Compose Integration

`ComposeBridge.kt` is a singleton that creates Compose views from Java code. It wires up a `Recomposer`, lifecycle owner, and `SavedStateRegistry` manually because `LIMEService` is not a `ComponentActivity`. Use this pattern for any new Compose views embedded in the IME.

Compose is used for:
- **Settings screen** — `SettingsScreen.kt` / `SettingsViewModel.kt` / `PreferenceComponents.kt`
- **Emoji picker** — `EmojiPicker.kt`
- **Candidate view** — `CandidateView.kt`
- **Composing text popup** — `ComposingTextPopup.kt`

### Global Utilities

| Class | Role |
|---|---|
| `LIME.java` | Global string constants (DB names, preference keys, IM status keys) |
| `LIMEPreferenceManager.java` | Typed accessors for all `SharedPreferences` keys; cached `SharedPreferences` instance |
| `LIMEUtilities.java` | Misc static helpers |
| `RootMapper.java` | Maps root characters for phonetic input |

## Commit Convention | 提交慣例

```
<type>(<scope>): <description>
```

Types: `feat`, `fix`, `refactor`, `style`, `chore`, `docs`, `test`  
Scopes: `keyboard`, `candidate`, `phase1`–`phase4`, `compose`, `db`, etc.  
Commit messages may be in English or Chinese.

## Key Technical Notes | 技術要點

- **LimeDB raw SQL** — known injection risks; parameterize new queries; see `docs/SECURITY_ANALYSIS.md`
- **Compose in IME** — no `ComponentActivity`; use the `ComposeBridge` / manual `Recomposer` pattern
- **Material 3 color migration** — in progress; see `docs/Phase3-Color-Audit-Material3-Mapping.md`
- **Version bump** — edit `VERSION_MAJOR`/`VERSION_MINOR`/`VERSION_PATCH` in `LimeStudio/version.properties`

## Development Guidelines | 開發規範

- Always provide both English and Traditional Chinese strings for user-facing text
- Prefer Kotlin for new code; use Java only when modifying existing Java files
- New UI should use Jetpack Compose; avoid creating new XML layouts
- Follow Material Design 3 guidelines for all UI components