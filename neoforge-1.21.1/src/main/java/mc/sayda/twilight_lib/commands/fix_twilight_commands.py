import re

# Read the file  
with open('TwilightLibCommands.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Fix imports
content = content.replace('import mc.sayda.twilight_lib.capabilities.MorphProvider;', '')
content = content.replace('import mc.sayda.twilight_lib.capabilities.TrailsProvider;', '')
content = content.replace('import mc.sayda.twilight_lib.capabilities.EffectsProvider;', '')
content = content.replace('import mc.sayda.twilight_lib.capabilities.AddonsProvider;', '')
content = content.replace('import net.neoforged.common.util.LazyOptional;', '')
content = content.replace('import mc.sayda.twilight_lib.capabilities.IMorph;', 'import mc.sayda.twilight_lib.capabilities.IMorph;\nimport mc.sayda.twilight_lib.capabilities.ModAttachments;')

# 2. Replace LazyOptional<IMorph> cap = ... with direct getData
content = re.sub(
    r'LazyOptional<IMorph> cap = target\.getCapability\(MorphProvider\.MORPH_CAP\);',
    'IMorph morph = target.getData(ModAttachments.MORPH);',
    content
)

# 3. Replace cap.ifPresent(m -> { ... }); with direct calls  
content = re.sub(
    r'cap\.ifPresent\(m -> \{',
    '// morph already accessed',
    content
)

# 4. Replace uses of 'm' variable with 'morph'
content = content.replace('m.setEntityType', 'morph.setEntityType')
content = content.replace('m.serialize', 'morph.serialize')

# 5. Replace getCapability for trails, addons, effects
content = re.sub(
    r'target\.getCapability\(TrailsProvider\.TRAILS_CAP\)\.ifPresent\(trails -> \{',
    'var trails = target.getData(ModAttachments.TRAILS);',
    content
)

content = re.sub(
    r'target\.getCapability\(AddonsProvider\.ADDONS_CAP\)\.ifPresent\(addons -> \{',
    'var addons = target.getData(ModAttachments.ADDONS);',
    content
)

content = re.sub(
    r'target\.getCapability\(EffectsProvider\.EFFECTS_CAP\)\.ifPresent\(effects -> \{',
    'var effects = target.getData(ModAttachments.EFFECTS);',
    content
)

content = re.sub(
    r'player\.getCapability\(TrailsProvider\.TRAILS_CAP\)\.ifPresent\(trails -> \{',
    'var trails = player.getData(ModAttachments.TRAILS);',
    content
)

content = re.sub(
    r'player\.getCapability\(AddonsProvider\.ADDONS_CAP\)\.ifPresent\(addons -> \{',
    'var addons = player.getData(ModAttachments.ADDONS);',
    content
)

content = re.sub(
    r'player\.getCapability\(EffectsProvider\.EFFECTS_CAP\)\.ifPresent\(effects -> \{',
    'var effects = player.getData(ModAttachments.EFFECTS);',
    content
)

# 6. Remove });  closures at the end of ifPresent blocks
lines = content.split('\n')
fixed_lines = []
for i, line in enumerate(lines):
    if line.strip() == '});' and i > 0:
        # Check if this is closing an ifPresent block
        # Look back for context
        prev_lines = '\n'.join(lines[max(0, i-20):i])
        if 'getData(ModAttachments.' in prev_lines:
            continue  # Skip this line
    fixed_lines.append(line)

content = '\n'.join(fixed_lines)

# Write back
with open('TwilightLibCommands.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("TwilightLibCommands.java fixed")
