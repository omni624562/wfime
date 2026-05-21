# CLAUDE.md — WFIME (麥田輸入法)

## Project Overview | 專案概述

WFIME (Wheat Fields Input Method Editor) is a fork of LimeIME — an Android IME (Input Method Editor) supporting multiple Chinese input methods. The app is licensed under GPLv3.

- **Package:** `net.toload.main.hd`
- **Min SDK:** 30 / **Target SDK:** 36 / **Compile SDK:** 36
- **Languages:** Java (primary) + Kotlin (newer features, Compose)
- **UI:** Mixed XML layouts + Jetpack Compose (migration in progress)
- **Theming:** Material Design 3 with dynamic colors
- **Bilingual:** English + Traditional Chinese (zh-TW)
- **Version:** Managed in `LimeStudio/version.properties` (major.minor.patch + auto-date versionCode)

## Project Structure | 專案結構

```
LimeStudio/                  # Android project root (open this in Android Studio)
├── app/
│   ├── src/main/java/net/toload/main/hd/   # Main source code
│   ├── src/main/res/                        # Resources (layouts, strings, drawables)
│   └── src/test/                            # Unit tests (JUnit + Robolectric)
├── version.properties                       # Version management (major/minor/patch)
├── build.gradle                             # Root build script
└── gradle/                                  # Gradle wrapper
docs/                        # Project documentation
```

## Build & Test | 建置與測試

```bash
# Build debug APK
cd LimeStudio && ./gradlew assembleDebug

# Run unit tests
cd LimeStudio && ./gradlew testDebugUnitTest

# Run lint
cd LimeStudio && ./gradlew lintDebug

# Install to connected device
cd LimeStudio && ./gradlew installDebug
```

## Commit Convention | 提交慣例

Follow Conventional Commits format with scope when applicable:

```
<type>(<scope>): <description>
```

- Types: `feat`, `fix`, `refactor`, `style`, `chore`, `docs`, `test`
- Scopes: `keyboard`, `candidate`, `phase1`–`phase4`, `compose`, etc.
- Commit messages may be in English or Chinese

## Key Technical Notes | 技術要點

- The main database layer (`LimeDB.java`, ~210KB) uses raw SQL — known SQL injection risks (see `docs/SECURITY_ANALYSIS.md`)
- AndroidX Preference migration completed (see `docs/AndroidX-Preference-Migration.md`)
- Material 3 color migration is in progress (see `docs/Phase3-Color-Audit-Material3-Mapping.md`)
- Compose adoption is partial — Settings screen is fully Compose; keyboard/candidate views remain XML
- Version name (`major.minor.patch`) is defined in `LimeStudio/version.properties`; versionCode is auto-generated as `yyyyMMdd` on each build
- To bump version: edit `VERSION_MAJOR`/`VERSION_MINOR`/`VERSION_PATCH` in `LimeStudio/version.properties`

## Development Guidelines | 開發規範

- Always provide both English and Traditional Chinese translations for user-facing strings
- Prefer Kotlin for new code; use Java only when modifying existing Java files
- New UI should use Jetpack Compose; avoid creating new XML layouts
- Follow Material Design 3 guidelines for all UI components
- Run `./gradlew lintDebug` before committing to catch common issues
