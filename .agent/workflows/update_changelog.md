---
description: Update Help Dialog Release Notes
---

# Update Changelog in Help Dialog

Whenever feature changes, UI updates, or bug fixes are implemented, you MUST update the in-app changelog to keep the user informed.

1.  **Edit File**: `app/src/main/res/values/strings.xml`
2.  **Target String**: `<string name="help_dialog_detail">`
3.  **Format**:
    ```xml
    \n版本更新資訊 (YYYY.MM.DD)\n
    1. [Feature]: Description...\n
    2. [Fix]: Description...\n\n
    ```
4.  **Language**: Traditional Chinese (繁體中文).
5.  **Placement**: Add the new version info at the top of the changelog section (below the intro warning).
