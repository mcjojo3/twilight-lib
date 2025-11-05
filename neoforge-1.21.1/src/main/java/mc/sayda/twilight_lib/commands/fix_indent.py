# Read the file
with open('CosmeticsCommand.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Fix indentation - code after getData() should not be extra-indented
fixed_lines = []
prev_was_getdata = False

for i, line in enumerate(lines):
    # Check if previous line was a getData() call
    if prev_was_getdata and line.strip() and not line.strip().startswith('//'):
        # Remove 4 extra spaces of indentation
        if line.startswith('            '):
            fixed_lines.append(line[4:])
        else:
            fixed_lines.append(line)
        prev_was_getdata = False
    else:
        fixed_lines.append(line)
        # Check if this line is a getData() call
        if 'var trails = player.getData(ModAttachments.TRAILS);' in line or \
           'var addons = player.getData(ModAttachments.ADDONS);' in line or \
           'var effects = player.getData(ModAttachments.EFFECTS);' in line:
            prev_was_getdata = True
        else:
            prev_was_getdata = False

# Write back
with open('CosmeticsCommand.java', 'w', encoding='utf-8') as f:
    f.writelines(fixed_lines)

print("Indentation fixed")
