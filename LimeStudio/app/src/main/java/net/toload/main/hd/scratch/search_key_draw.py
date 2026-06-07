with open('app/src/main/java/net/toload/main/hd/keyboard/LIMEKeyboardBaseView.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

print("SEARCHING KEY DRAWING LOGIC:")
for i, line in enumerate(lines):
    if 'draw' in line.lower() or 'label' in line.lower() or 'key' in line.lower():
        if 'paint' in line or 'canvas' in line or 'text' in line.lower():
            print(f"Line {i+1}: {line.strip()}")
