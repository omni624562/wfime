import json

try:
    with open("LimeStudio/app/src/main/assets/emojis.json", "r", encoding="utf-8") as f:
        data = json.load(f)
    
    for cat in data["categories"]:
        if cat["name"] == "ANIMALS_NATURE":
            print(f"Category: {cat['name']} has {len(cat['emojis'])} emojis")
            # Print last 10
            print("Last 10 items:")
            for e in cat["emojis"][-10:]:
                print(f"Char: {repr(e['char'])}, Keywords: {e['keywords']}")
except Exception as e:
    print(e)
