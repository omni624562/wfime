import os

path = 'app/src/main/res/values/strings_settings.xml'
if os.path.exists(path):
    with open(path, 'r', encoding='utf-8') as f:
        print("STRINGS SETTINGS:")
        for line in f:
            if 'label' in line.lower() or 'key' in line.lower() or 'root' in line.lower() or '字根' in line:
                print(line.strip())

path_pref = 'app/src/main/java/net/toload/main/hd/global/LIMEPreferenceManager.java'
if os.path.exists(path_pref):
    with open(path_pref, 'r', encoding='utf-8') as f:
        print("\nPREFERENCE MANAGER:")
        for line in f:
            if 'label' in line.lower() or 'key' in line.lower() or 'root' in line.lower():
                print(line.strip())
