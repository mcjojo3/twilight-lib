# Patreon Supporters Setup Guide

This guide explains how to set up the patreon-supporters repository to enable cosmetic rewards for your supporters.

## Repository Setup

1. **Upload the supporters.json file** to your `patreon-supporters` repository:
   - Copy `supporters.json.example` from this project
   - Rename it to `supporters.json`
   - Place it in the root of the repository

2. **Make the repository public** (or keep it private if you prefer privacy)

## Adding Supporters

Edit the `supporters.json` file in the `patreon-supporters` repository:

```json
{
  "version": 1,
  "last_updated": "2025-10-20",
  "supporters": [
    {
      "uuid": "player-uuid-here",
      "name": "PlayerName",
      "tier": "gold",
      "joined": "2025-10-01",
      "cosmetics": {
        "trails": ["custom_trail"],
        "addons": ["exclusive_addon"],
        "effects": []
      }
    }
  ]
}
```

### Available Tiers

The tier system unlocks cosmetics automatically:

- **stone** - Entry tier (no cosmetics yet, reserved for future use)
- **bronze** - Basic heart trail (bronze hearts)
- **silver** - Bronze perks + silver hearts, sparkles, cherry_blossom trails, galaxy_tail addon
- **gold** - Silver perks + gold hearts, twilight, stars trails, respawn_twilight effect
- **platinum** - All benefits (future-proof tier for new cosmetics)

**Note:** Higher tiers inherit all cosmetics from lower tiers.

### Finding Player UUIDs

Use one of these methods:
1. **In-game**: Use `/twilight_lib debug` command (if implemented)
2. **Online**: Visit https://mcuuid.net/ and enter the player's username
3. **Server logs**: Check when players log in

### Available Cosmetics

**Trails** (Particle effects that follow the player):
- `hearts` - Tier-based heart particles (bronze/silver/gold based on supporter tier)
- `sparkles` - Golden sparkle trail (Silver tier+)
- `cherry_blossom` - Cherry blossom petals (Silver tier+)
- `twilight` - Purple twilight particles (Gold tier+)
- `stars` - Shooting star trail (Gold tier+)

**Addons** (Cosmetic models worn by the player):
- `galaxy_tail` - Galaxy-textured kitsune tail (Silver tier+)
- `tiara` - Crown accessory (available to all)
- Kitsune sets (ears, snout, tails) - 250+ color/variant combinations

**Effects** (Special visual effects):
- `respawn_twilight` - Special twilight particle burst on respawn (Gold tier+)

**Manual Grants** (in `cosmetics` section):
These persist even if tier changes and can be used for:
- Event rewards
- Gifts to specific players
- Grandfathered perks
- Custom exclusive cosmetics

## How It Works

1. **On Server Start**: Twilight Lib fetches the `supporters.json` file from GitHub
2. **On Player Login**: The mod checks if the player's UUID is in the supporters list
3. **If Found**: The player gets access to their cosmetics automatically
4. **Cache Duration**: The list is cached for 1 hour to reduce GitHub API calls

## Commands

### Player Commands (No permissions required)

Players can manage their **owned** cosmetics with these commands:

- `/cosmetics info` - Check supporter status and cosmetic counts
- `/cosmetics trails list` - List available trails
- `/cosmetics trails set <type>` - Set active trail
- `/cosmetics trails toggle` - Enable/disable trails
- `/cosmetics addons list` - List owned addons
- `/cosmetics addons equip <id>` - Equip an addon
- `/cosmetics addons unequip <id>` - Unequip an addon
- `/cosmetics effects list` - List owned effects

### Admin Commands (Permission level 2 required)

Admins can force-apply cosmetics to any player:

**Morphing:**
- `/tl morph <entity> [player]` - Transform into any living entity
- `/tl unmorph [player]` - Remove morph

**Trails:**
- `/tl trail set <trail> [player] [persistent]` - Set trail with optional persistent flag
- `/tl trail toggle [player]` - Toggle trail rendering
- `/tl trail list` - List all available trails

**Effects:**
- `/tl effects equip <effect> [player] [persistent]` - Grant effect
- `/tl effects unequip <effect> [player]` - Remove effect
- `/tl effects list` - List all available effects

**Addons:**
- `/tl addons equip <id> [player] [persistent]` - Force equip addon
- `/tl addons unequip <id> [player]` - Force unequip addon
- `/tl addons clear [player]` - Clear all addons
- `/tl addons list` - List all registered addons

**Persistent Flag:**
- `true` - Cosmetic persists through logout/death (for CreRaces race attributes)
- `false` (default) - Temporary preview, cleared on logout
- Self-targeting always uses non-persistent mode
- Example: `/tl addons equip kitsune_ears_white PlayerName true`

**Reload:**
- `/tl reload` - Reload supporter data and entity cache

## Important Notes

⚠️ **All cosmetics are purely visual** - No gameplay advantages whatsoever!

💜 **This is a thank-you reward** for supporting development

🔄 **Updates are automatic** - Changes to `supporters.json` take effect within 1 hour

## Example Workflow

1. Someone becomes a Patreon supporter
2. You get their Minecraft UUID (ask them or use mcuuid.net)
3. Add them to `supporters.json` in the patreon-supporters repo
4. Commit and push the changes
5. Within 1 hour, they'll have access to cosmetics when they log in

## Testing

To test the system:

1. Add your own UUID to the `supporters.json`
2. Commit and push
3. Restart your server (or wait for cache to expire)
4. Log in and use `/cosmetics info` to verify
5. Use `/cosmetics trail set hearts` to test trails

## Troubleshooting

**Cosmetics not showing up?**
- Check the server logs for "Successfully fetched X supporters"
- Verify the UUID is correct (no hyphens needed, but they're ok)
- Ensure the JSON file is valid (use https://jsonlint.com/)
- Wait for cache to expire (1 hour) or restart the server

**GitHub fetch failing?**
- Check the repository is public
- Verify the URL is correct in `SupporterService.java`
- Check your server has internet access

## Repository URL

The mod fetches from:
```
https://raw.githubusercontent.com/mcjojo3/patreon-supporters/main/supporters.json
```

Make sure this file exists and is accessible!
