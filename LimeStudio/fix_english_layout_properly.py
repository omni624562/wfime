import os
import re

files_to_fix = [
    'app/src/main/res/xml/lime.xml',
    'app/src/main/res/xml/lime_number_shift.xml',
    'app/src/main/res/xml/lime_number_symbol.xml',
    'app/src/main/res/xml/lime_number_symbol_shift.xml'
]

for file in files_to_fix:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # lime.xml is single-line format
    if 'lime.xml' in file:
        content = re.sub(
            r'<Key limehd:codes="97"(.*?)limehd:keyLabel="a" />',
            r'<Key limehd:codes="97" limehd:keyWidth="15%p" limehd:horizontalGap="5%p" limehd:keyEdgeFlags="left" limehd:keyLabel="a" />',
            content
        )
        content = re.sub(
            r'<Key limehd:codes="108" limehd:keyLabel="l" />',
            r'<Key limehd:codes="108" limehd:keyEdgeFlags="right" limehd:keyLabel="l" />',
            content
        )
    else:
        # Multi-line formatting
        # For 'a' (97) and 'A' (65)
        # Note: They might have horizontalGap already or not, or keyEdgeFlags or not.
        # We will just replace the whole block from <Key to limehd:keyLabel="a" />
        content = re.sub(
            r'<Key\s+limehd:codes="97"[^>]+?limehd:keyLabel="a" />',
            '<Key\n            limehd:codes="97"\n            limehd:keyWidth="15%p"\n            limehd:horizontalGap="5%p"\n            limehd:keyEdgeFlags="left"\n            limehd:keyLabel="a" />',
            content
        )
        content = re.sub(
            r'<Key\s+limehd:codes="65"[^>]+?limehd:keyLabel="A" />',
            '<Key\n            limehd:codes="65"\n            limehd:keyWidth="15%p"\n            limehd:horizontalGap="5%p"\n            limehd:keyEdgeFlags="left"\n            limehd:keyLabel="A" />',
            content
        )
        
        # For 'l' (108) and 'L' (76)
        content = re.sub(
            r'<Key\s+limehd:codes="108"[^>]+?limehd:keyLabel="l" />',
            '<Key\n            limehd:codes="108"\n            limehd:keyEdgeFlags="right"\n            limehd:keyLabel="l" />',
            content
        )
        content = re.sub(
            r'<Key\s+limehd:codes="76"[^>]+?limehd:keyLabel="L" />',
            '<Key\n            limehd:codes="76"\n            limehd:keyEdgeFlags="right"\n            limehd:keyLabel="L" />',
            content
        )

    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

print("Done")
