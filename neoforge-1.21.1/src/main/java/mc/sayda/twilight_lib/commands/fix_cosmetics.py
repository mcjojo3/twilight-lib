import re

# Read the file
with open('CosmeticsCommand.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Fix imports
content = content.replace('import mc.sayda.twilight_lib.capabilities.TrailsProvider;', '')
content = content.replace('import mc.sayda.twilight_lib.capabilities.AddonsProvider;', '')
content = content.replace('import mc.sayda.twilight_lib.capabilities.EffectsProvider;', '')
content = content.replace('import mc.sayda.twilight_lib.cosmetics.TrailType;', 'import mc.sayda.twilight_lib.capabilities.ModAttachments;\nimport mc.sayda.twilight_lib.cosmetics.TrailType;')

# 2. Fix suggestion providers - trails
content = re.sub(
    r'player\.getCapability\(TrailsProvider\.TRAILS_CAP\)\.ifPresent\(trails -> \{\s+Set<String> playerTrails = trails\.getTrails\(\);\s+SharedSuggestionProvider\.suggest\(playerTrails\.stream\(\), builder\);\s+\}\);',
    'var trails = player.getData(ModAttachments.TRAILS);\n            Set<String> playerTrails = trails.getTrails();\n            SharedSuggestionProvider.suggest(playerTrails.stream(), builder);',
    content,
    flags=re.DOTALL
)

# 3. Fix suggestion providers - addons
content = re.sub(
    r'player\.getCapability\(AddonsProvider\.ADDONS_CAP\)\.ifPresent\(addons -> \{\s+Set<String> playerAddons = addons\.getAddons\(\);\s+SharedSuggestionProvider\.suggest\(playerAddons\.stream\(\), builder\);\s+\}\);',
    'var addons = player.getData(ModAttachments.ADDONS);\n            Set<String> playerAddons = addons.getAddons();\n            SharedSuggestionProvider.suggest(playerAddons.stream(), builder);',
    content,
    flags=re.DOTALL
)

# 4. Fix suggestion providers - effects
content = re.sub(
    r'player\.getCapability\(EffectsProvider\.EFFECTS_CAP\)\.ifPresent\(effects -> \{\s+Set<String> playerEffects = effects\.getEffects\(\);\s+SharedSuggestionProvider\.suggest\(playerEffects\.stream\(\), builder\);\s+\}\);',
    'var effects = player.getData(ModAttachments.EFFECTS);\n            Set<String> playerEffects = effects.getEffects();\n            SharedSuggestionProvider.suggest(playerEffects.stream(), builder);',
    content,
    flags=re.DOTALL
)

# 5. Remove all isPresent checks for capabilities
content = re.sub(
    r'\s+// Check if capability exists\s+if \(!player\.getCapability\([^)]+\)\.isPresent\(\)\) \{\s+player\.sendSystemMessage\([^}]+\}\s+',
    '\n',
    content,
    flags=re.DOTALL
)

# 6. Replace getCapability().ifPresent() with getData() - this needs careful handling
# We'll do this for each type separately

# Trails
lines = content.split('\n')
fixed_lines = []
in_trails_block = False
skip_next_brace = False

for i, line in enumerate(lines):
    if 'player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {' in line:
        fixed_lines.append(line.replace('player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {', 'var trails = player.getData(ModAttachments.TRAILS);'))
        in_trails_block = True
        skip_next_brace = True
    elif in_trails_block and '});' in line and skip_next_brace:
        # Skip this closing brace
        in_trails_block = False
        skip_next_brace = False
    else:
        fixed_lines.append(line)

content = '\n'.join(fixed_lines)

# Addons
lines = content.split('\n')
fixed_lines = []
in_addons_block = False
skip_next_brace = False

for i, line in enumerate(lines):
    if 'player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {' in line:
        fixed_lines.append(line.replace('player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {', 'var addons = player.getData(ModAttachments.ADDONS);'))
        in_addons_block = True
        skip_next_brace = True
    elif in_addons_block and '});' in line and skip_next_brace:
        # Skip this closing brace
        in_addons_block = False
        skip_next_brace = False
    else:
        fixed_lines.append(line)

content = '\n'.join(fixed_lines)

# Effects
lines = content.split('\n')
fixed_lines = []
in_effects_block = False
skip_next_brace = False

for i, line in enumerate(lines):
    if 'player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {' in line:
        fixed_lines.append(line.replace('player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {', 'var effects = player.getData(ModAttachments.EFFECTS);'))
        in_effects_block = True
        skip_next_brace = True
    elif in_effects_block and '});' in line and skip_next_brace:
        # Skip this closing brace
        in_effects_block = False
        skip_next_brace = False
    else:
        fixed_lines.append(line)

content = '\n'.join(fixed_lines)

# Write back
with open('CosmeticsCommand.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("File fixed successfully")
