import os

files = [
    'app/src/main/res/xml/lime.xml',
    'app/src/main/res/xml/lime_number_shift.xml',
    'app/src/main/res/xml/lime_number_symbol.xml',
    'app/src/main/res/xml/lime_number_symbol_shift.xml'
]

for file in files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()

    # For lime.xml
    content = content.replace('limehd:horizontalGap="5%p" limehd:keyEdgeFlags="left"', 'limehd:horizontalGap="5%p"')
    content = content.replace('limehd:keyEdgeFlags="right" limehd:keyLabel="l"', 'limehd:keyLabel="l"')
    
    # For the multi-line ones
    content = content.replace('limehd:horizontalGap="5%p"\n            limehd:keyEdgeFlags="left"', 'limehd:horizontalGap="5%p"')
    content = content.replace('limehd:keyEdgeFlags="right"\n            limehd:keyLabel="L"', 'limehd:keyLabel="L"')
    content = content.replace('limehd:keyEdgeFlags="right"\n            limehd:keyLabel="l"', 'limehd:keyLabel="l"')
    
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

print('Done')
