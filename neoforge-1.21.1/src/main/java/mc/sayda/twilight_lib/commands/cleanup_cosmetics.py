import re

# Read the file
with open('CosmeticsCommand.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Fix duplicate import and remove extra lines
fixed_lines = []
seen_mod_attachments = False
for line in lines:
    # Remove duplicate ModAttachments import
    if 'import mc.sayda.twilight_lib.capabilities.ModAttachments;' in line:
        if not seen_mod_attachments:
            fixed_lines.append(line)
            seen_mod_attachments = True
    # Remove standalone }); lines that are leftovers
    elif line.strip() == '});':
        continue
    # Fix indentation for getData() calls that are at wrong level
    elif line.startswith('var trails = player.getData(ModAttachments.TRAILS);') or \
         line.startswith('var addons = player.getData(ModAttachments.ADDONS);') or \
         line.startswith('var effects = player.getData(ModAttachments.EFFECTS);'):
        fixed_lines.append('        ' + line.lstrip())
    else:
        fixed_lines.append(line)

# Write back
with open('CosmeticsCommand.java', 'w', encoding='utf-8') as f:
    f.writelines(fixed_lines)

print("Cleanup complete")
