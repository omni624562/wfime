# WFIME (Wheat Fields Input Method Editor) Project Analysis

## Overview
**WFIME** is an Android Input Method Editor (IME) based on the open-source LIME IME project. It is optimized for specific user habits, specifically retaining **Dayi (大易)** and **Phonetic (注音)** input methods.

## Project Structure
The project follows a standard Android project structure:

- **Root Directory**:
  - `LimeStudio/`: Main Android project directory.
  - `Database/`: Contains SQLite database files (`.db`) and archives (`.zip`) for input methods (Dayi, Phonetic).
  - `README.md`: Project documentation.
  - `app-release.apk`: Pre-built release APK.

- **Android Module (`LimeStudio/app`)**:
  - `src/main/java/nan/toload/main/hd/`: Source code package.
  - `src/main/res/`: Reources (layouts, drawables, values).
  - `src/main/AndroidManifest.xml`: App manifest defining components.

## Technical Stack & Configuration

### Build System (Gradle)
- **Agp Version**: 8.13.2
- **Kotlin Version**: 1.9.22
- **Compile SDK**: 36
- **Target SDK**: 36
- **Min SDK**: 30 (Android 11)

### Key Dependencies
- **Language**: Kotlin & Java
- **UI Frameworks**:
  - **Jetpack Compose**: Used for modern UI components (Material 3).
  - **XML Layouts**: Likely used for legacy parts or preference screens (implied by `LIMEPreferenceHC`).
- **AndroidX Libraries**: Core, AppCompat, Activity, ConstraintLayout, CoordinatorLayout, DrawerLayout, Preference.
- **Testing**: JUnit 4, Mockito, Robolectric, Espresso.

## Core Components
Based on `AndroidManifest.xml`:

1.  **LIMEService** (`.LIMEService`):
    - **Type**: `InputMethodService`
    - **Function**: The core engine of the keyboard. It handles key events, candidate selection, and input logic.
    - **Permissions**: `BIND_INPUT_METHOD` (Required for IMEs), `FOREGROUND_SERVICE_SPECIAL_USE`.

2.  **MainActivity** (`nan.toload.main.hd.MainActivity`):
    - **Type**: `Activity` (Launcher)
    - **Function**: Likely the entry point for the user to see app info or initial setup instructions.

3.  **LIMEPreferenceHC** (`.limesettings.LIMEPreferenceHC`):
    - **Type**: `Activity`
    - **Function**: Settings screen for configuring the keyboard (Preferences).

## Data Management
The `Database/` directory suggests the app uses pre-populated SQLite databases for character mappings:
- `dayi.db` / `dayiuni.db`: Dayi input method data.
- `phonetic.db` / `phoneticcomplete.db`: Phonetic input method data.
Permissions `READ_USER_DICTIONARY` and `WRITE_USER_DICTIONARY` indicate it also interacts with the system user dictionary.

## Summary
This is a mature Android project transitioning to modern Android development practices (adopting Kotlin and Jetpack Compose) while maintaining core IME functionality. It targets recent Android versions (SDK 36) and focuses on providing a clean, specialized input experience for Dayi and Phonetic users.
