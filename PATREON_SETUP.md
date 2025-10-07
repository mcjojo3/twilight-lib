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
  "last_updated": "2025-10-07",
  "supporters": [
    {
      "uuid": "player-uuid-here",
      "name": "PlayerName",
      "tier": "patreon",
      "joined": "2025-10-01",
      "cosmetics": {
        "trails": ["hearts", "sparkles", "cherry_blossom", "twilight", "stars"],
        "addons": ["galaxy_tail", "starlight_ears"],
        "effects": ["respawn_twilight"]
      }
    }
  ]
}
```

### Finding Player UUIDs

Use one of these methods:
1. **In-game**: Use `/twilight_lib debug` command (if implemented)
2. **Online**: Visit https://mcuuid.net/ and enter the player's username
3. **Server logs**: Check when players log in

### Available Cosmetics

**Trails** (Particle effects that follow the player):
- `hearts` - Pink heart particles
- `sparkles` - Golden sparkle trail
- `cherry_blossom` - Cherry blossom petals
- `twilight` - Purple twilight particles
- `stars` - Shooting star trail

**Addons** (Cosmetic models - to be implemented):
- `galaxy_tail` - Galaxy-textured tail variant
- `starlight_ears` - Glowing starlight ears
- `halo` - Floating halo accessory
- `twilight_wings` - Ethereal twilight wings

**Effects** (Special visual effects):
- `respawn_twilight` - Special twilight respawn effect

## How It Works

1. **On Server Start**: Twilight Lib fetches the `supporters.json` file from GitHub
2. **On Player Login**: The mod checks if the player's UUID is in the supporters list
3. **If Found**: The player gets access to their cosmetics automatically
4. **Cache Duration**: The list is cached for 1 hour to reduce GitHub API calls

## Commands

Players can manage their cosmetics with these commands:

- `/cosmetics info` - Check supporter status
- `/cosmetics trail list` - List available trails
- `/cosmetics trail set <type>` - Set active trail
- `/cosmetics trail toggle` - Enable/disable trails

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