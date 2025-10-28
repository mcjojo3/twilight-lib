package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.cosmetics.TrailType;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncTrailsPacket;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.SyncEffectsPacket;
import mc.sayda.twilight_lib.supporter.SupporterData;
import mc.sayda.twilight_lib.supporter.SupporterRegistry;
import mc.sayda.twilight_lib.supporter.SupporterService;
import mc.sayda.twilight_lib.TwilightConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Commands for managing cosmetic features
 * All cosmetics are supporter-exclusive and purely visual!
 * This command is Level 1 (accessible to all players)
 */
public class CosmeticsCommand {

    // Suggestion provider that shows only trails the player has access to
    private static final SuggestionProvider<CommandSourceStack> PLAYER_TRAILS_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                Set<String> playerTrails = trails.getTrails();
                SharedSuggestionProvider.suggest(playerTrails.stream(), builder);
            });
        }
        return builder.buildFuture();
    };

    // Suggestion provider that shows only addons the player has access to
    private static final SuggestionProvider<CommandSourceStack> PLAYER_ADDONS_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                Set<String> playerAddons = addons.getAddons();
                SharedSuggestionProvider.suggest(playerAddons.stream(), builder);
            });
        }
        return builder.buildFuture();
    };

    // Suggestion provider that shows only effects the player has access to
    private static final SuggestionProvider<CommandSourceStack> PLAYER_EFFECTS_SUGGESTIONS = (context, builder) -> {
        if (context.getSource().getEntity() instanceof ServerPlayer player) {
            player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                Set<String> playerEffects = effects.getEffects();
                SharedSuggestionProvider.suggest(playerEffects.stream(), builder);
            });
        }
        return builder.buildFuture();
    };

    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // Register all command aliases
        for (String alias : new String[]{"cosmetics", "tlc", "tlcosmetics", "twilightlibcosmetics"}) {
            dispatcher.register(Commands.literal(alias)
                .then(Commands.literal("trail")
                .then(Commands.literal("set")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(PLAYER_TRAILS_SUGGESTIONS)
                        .executes(CosmeticsCommand::setTrail)
                    )
                )
                .then(Commands.literal("toggle")
                    .executes(CosmeticsCommand::toggleTrail)
                )
                .then(Commands.literal("list")
                    .executes(CosmeticsCommand::listTrails)
                )
            )
            .then(Commands.literal("addons")
                .then(Commands.literal("equip")
                    .then(Commands.argument("addon", StringArgumentType.word())
                        .suggests(PLAYER_ADDONS_SUGGESTIONS)
                        .executes(CosmeticsCommand::equipAddon)
                    )
                )
                .then(Commands.literal("unequip")
                    .then(Commands.argument("addon", StringArgumentType.word())
                        .suggests(PLAYER_ADDONS_SUGGESTIONS)
                        .executes(CosmeticsCommand::unequipAddon)
                    )
                )
                .then(Commands.literal("list")
                    .executes(CosmeticsCommand::listAddons)
                )
            )
            .then(Commands.literal("effects")
                .then(Commands.literal("equip")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(PLAYER_EFFECTS_SUGGESTIONS)
                        .executes(CosmeticsCommand::equipEffect)
                    )
                )
                .then(Commands.literal("unequip")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(PLAYER_EFFECTS_SUGGESTIONS)
                        .executes(CosmeticsCommand::unequipEffect)
                    )
                )
                .then(Commands.literal("list")
                    .executes(CosmeticsCommand::listEffects)
                )
            )
                .then(Commands.literal("info")
                    .executes(CosmeticsCommand::showInfo)
                )
            );
        }
    }

    private static int setTrail(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String trailId = StringArgumentType.getString(ctx, "type");

        // Check if capability exists
        if (!player.getCapability(TrailsProvider.TRAILS_CAP).isPresent()) {
            player.sendSystemMessage(Component.literal("❌ Error: Trail capability not initialized")
                .withStyle(ChatFormatting.RED));
            return 1;
        }

        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            // Check if player has this trail
            if (!trails.hasTrail(trailId)) {
                player.sendSystemMessage(Component.literal("❌ You don't have access to that trail!")
                    .withStyle(ChatFormatting.RED));
                player.sendSystemMessage(Component.literal("Use /cosmetics trail list to see available trails")
                    .withStyle(ChatFormatting.GRAY));
                return;
            }

            trails.setActiveTrail(trailId);
            trails.setTrailEnabled(true);

            // Save to persistent NBT
            player.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());

            // Sync to all clients
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.serialize()));

            player.sendSystemMessage(Component.literal("Trail set to '" + trailId + "'")
                .withStyle(ChatFormatting.GREEN));
        });

        return 1;
    }

    private static int toggleTrail(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        // Check if capability exists
        if (!player.getCapability(TrailsProvider.TRAILS_CAP).isPresent()) {
            player.sendSystemMessage(Component.literal("❌ Error: Trail capability not initialized")
                .withStyle(ChatFormatting.RED));
            return 1;
        }

        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            boolean newState = !trails.isTrailEnabled();
            trails.setTrailEnabled(newState);

            // Save to persistent NBT
            player.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());

            // Sync to all clients
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.serialize()));

            if (newState) {
                player.sendSystemMessage(Component.literal("Trail enabled")
                    .withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.literal("Trail disabled")
                    .withStyle(ChatFormatting.GREEN));
            }
        });

        return 1;
    }

    private static int listTrails(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            Set<String> playerTrails = trails.getTrails();
            Set<String> allSupporterTrails = SupporterRegistry.getAllSupporterTrails();

            // Show ALL owned trails (including manual grants not in registry)
            Set<String> supporterTrailsOwned = playerTrails;

            player.sendSystemMessage(Component.literal("═══════════════════════════")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.literal("    ✨ Your Supporter Trails")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("═══════════════════════════")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            if (supporterTrailsOwned.isEmpty()) {
                player.sendSystemMessage(Component.literal("  No supporter trails available yet!")
                    .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("💜 Support to unlock exclusive trails!")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                String activeTrail = trails.getActiveTrail();
                boolean isEnabled = trails.isTrailEnabled();

                // Show enabled/disabled status
                String statusText = isEnabled ? "Enabled" : "Disabled";
                ChatFormatting statusColor = isEnabled ? ChatFormatting.GREEN : ChatFormatting.RED;
                player.sendSystemMessage(Component.literal("  Status: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(statusText).withStyle(statusColor)));
                player.sendSystemMessage(Component.literal(""));

                for (String trail : supporterTrailsOwned) {
                    boolean isActive = trail.equals(activeTrail);
                    String marker = isActive ? "➤ " : "  • ";
                    player.sendSystemMessage(Component.literal(marker + trail)
                        .withStyle(isActive ? ChatFormatting.GOLD : ChatFormatting.WHITE));
                }
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("Use /cosmetics trail set <type> to change trail")
                    .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal("Use /cosmetics trail toggle to toggle the trail")
                        .withStyle(ChatFormatting.GRAY));
            }

            // Show available supporter trails by tier (dynamic from SupporterRegistry)
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Trail Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            //For future expansion?
            /*Set<String> stoneTrails = SupporterRegistry.getTrailsForTier("stone");
            if (!stoneTrails.isEmpty()) {
                player.sendSystemMessage(Component.literal("🏅 Stone: " + String.join(", ", stoneTrails))
                        .withStyle(ChatFormatting.GRAY));
            }*/

            Set<String> bronzeTrails = SupporterRegistry.getTrailsForTier("bronze");
            if (!bronzeTrails.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥉 Bronze: " + String.join(", ", bronzeTrails))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> silverTrails = SupporterRegistry.getTrailsForTier("silver");
            if (!silverTrails.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥈 Silver: " + String.join(", ", silverTrails))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> goldTrails = SupporterRegistry.getTrailsForTier("gold");
            if (!goldTrails.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥇 Gold: " + String.join(", ", goldTrails))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> platinumTrails = SupporterRegistry.getTrailsForTier("platinum");
            if (!platinumTrails.isEmpty()) {
                player.sendSystemMessage(Component.literal("💎 Platinum: " + String.join(", ", platinumTrails))
                        .withStyle(ChatFormatting.GRAY));
            }

            player.sendSystemMessage(Component.literal(""));
        });

        return 1;
    }

    private static int listAddons(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            Set<String> playerAddons = addons.getAddons();
            Set<String> activeAddons = addons.getActiveAddons();

            // Show ALL owned addons (including manual grants not in registry)
            Set<String> supporterAddonsOwned = playerAddons;

            player.sendSystemMessage(Component.literal("═══════════════════════════")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.literal("    🎨 Your Supporter Addons")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("═══════════════════════════")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            if (supporterAddonsOwned.isEmpty()) {
                player.sendSystemMessage(Component.literal("  No supporter addons available yet!")
                    .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("💜 Support to unlock exclusive addons!")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                for (String addon : supporterAddonsOwned) {
                    boolean isEquipped = activeAddons.contains(addon);
                    String marker = isEquipped ? "✓ " : "  • ";
                    ChatFormatting color = isEquipped ? ChatFormatting.GOLD : ChatFormatting.WHITE;
                    player.sendSystemMessage(Component.literal(marker + addon)
                        .withStyle(color));
                }
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("Use /cosmetics addons equip <type> to add an addon")
                        .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal("Use /cosmetics addons unequip <type> to remove an addon")
                        .withStyle(ChatFormatting.GRAY));
            }

            // Show available supporter addons by tier (dynamic from SupporterRegistry)
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Addon Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            //For future expansion?
            /*Set<String> stoneAddons = SupporterRegistry.getAddonsForTier("stone");
            if (!stoneAddons.isEmpty()) {
                player.sendSystemMessage(Component.literal("🏅 Stone: " + String.join(", ", stoneAddons))
                        .withStyle(ChatFormatting.GRAY));
            }*/

            Set<String> bronzeAddons = SupporterRegistry.getAddonsForTier("bronze");
            if (!bronzeAddons.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥉 Bronze: " + String.join(", ", bronzeAddons))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> silverAddons = SupporterRegistry.getAddonsForTier("silver");
            if (!silverAddons.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥈 Silver: " + String.join(", ", silverAddons))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> goldAddons = SupporterRegistry.getAddonsForTier("gold");
            if (!goldAddons.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥇 Gold: " + String.join(", ", goldAddons))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> platinumAddons = SupporterRegistry.getAddonsForTier("platinum");
            if (!platinumAddons.isEmpty()) {
                player.sendSystemMessage(Component.literal("💎 Platinum: " + String.join(", ", platinumAddons))
                        .withStyle(ChatFormatting.GRAY));
            }

            player.sendSystemMessage(Component.literal(""));
        });

        return 1;
    }

    private static int listEffects(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            Set<String> playerEffects = effects.getEffects();
            Set<String> activeEffects = effects.getActiveEffects();

            // Show ALL owned effects (including manual grants not in registry)
            Set<String> supporterEffectsOwned = playerEffects;

            player.sendSystemMessage(Component.literal("═══════════════════════════")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.literal("    💫 Your Supporter Effects")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("═══════════════════════════")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            if (supporterEffectsOwned.isEmpty()) {
                player.sendSystemMessage(Component.literal("  No supporter effects available yet!")
                    .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("💜 Support to unlock exclusive effects!")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                for (String effect : supporterEffectsOwned) {
                    boolean isEquipped = activeEffects.contains(effect);
                    String marker = isEquipped ? "✓ " : "  • ";
                    ChatFormatting color = isEquipped ? ChatFormatting.GOLD : ChatFormatting.WHITE;
                    player.sendSystemMessage(Component.literal(marker + effect)
                        .withStyle(color));
                }
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("Use /cosmetics effects equip <type> to equip an effect")
                    .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal("Use /cosmetics effects unequip <type> to unequip an effect")
                    .withStyle(ChatFormatting.GRAY));
            }

            // Show available supporter effects by tier (dynamic from SupporterRegistry)
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Effect Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            //For future expansion?
            /*Set<String> stoneEffects = SupporterRegistry.getEffectsForTier("stone");
            if (!stoneEffects.isEmpty()) {
                player.sendSystemMessage(Component.literal("🏅 Stone: " + String.join(", ", stoneEffects))
                        .withStyle(ChatFormatting.GRAY));
            }*/

            Set<String> bronzeEffects = SupporterRegistry.getEffectsForTier("bronze");
            if (!bronzeEffects.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥉 Bronze: " + String.join(", ", bronzeEffects))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> silverEffects = SupporterRegistry.getEffectsForTier("silver");
            if (!silverEffects.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥈 Silver: " + String.join(", ", silverEffects))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> goldEffects = SupporterRegistry.getEffectsForTier("gold");
            if (!goldEffects.isEmpty()) {
                player.sendSystemMessage(Component.literal("🥇 Gold: " + String.join(", ", goldEffects))
                    .withStyle(ChatFormatting.GRAY));
            }

            Set<String> platinumEffects = SupporterRegistry.getEffectsForTier("platinum");
            if (!platinumEffects.isEmpty()) {
                player.sendSystemMessage(Component.literal("💎 Platinum: " + String.join(", ", platinumEffects))
                        .withStyle(ChatFormatting.GRAY));
            }

            player.sendSystemMessage(Component.literal(""));
        });

        return 1;
    }

    private static int equipEffect(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String effectId = StringArgumentType.getString(ctx, "type");

        // Check if capability exists
        if (!player.getCapability(EffectsProvider.EFFECTS_CAP).isPresent()) {
            player.sendSystemMessage(Component.literal("❌ Error: Effects capability not initialized")
                .withStyle(ChatFormatting.RED));
            return 1;
        }

        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            // Check ownership - players can only interact with effects they own
            if (!effects.hasEffect(effectId)) {
                player.sendSystemMessage(Component.literal("❌ You don't own '" + effectId + "'!")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Check if already active
            if (effects.isEffectActive(effectId)) {
                player.sendSystemMessage(Component.literal("⚠ Effect '" + effectId + "' is already equipped!")
                    .withStyle(ChatFormatting.YELLOW));
                return;
            }

            // Activate the effect
            effects.setActiveEffect(effectId, true);
            player.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());

            // Sync to all clients
            NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects()));

            player.sendSystemMessage(Component.literal("Effect '" + effectId + "' equipped")
                .withStyle(ChatFormatting.GREEN));
        });

        return 1;
    }

    private static int unequipEffect(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String effectId = StringArgumentType.getString(ctx, "type");

        // Check if capability exists
        if (!player.getCapability(EffectsProvider.EFFECTS_CAP).isPresent()) {
            player.sendSystemMessage(Component.literal("❌ Error: Effects capability not initialized")
                .withStyle(ChatFormatting.RED));
            return 1;
        }

        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            // Check ownership - players can only interact with effects they own
            if (!effects.hasEffect(effectId)) {
                player.sendSystemMessage(Component.literal("❌ You don't own '" + effectId + "'!")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Check if effect is active
            if (!effects.isEffectActive(effectId)) {
                player.sendSystemMessage(Component.literal("❌ Effect '" + effectId + "' is not equipped!")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Deactivate the effect
            effects.setActiveEffect(effectId, false);
            player.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());

            // Sync to all clients
            NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects()));

            player.sendSystemMessage(Component.literal("Effect '" + effectId + "' unequipped")
                .withStyle(ChatFormatting.GREEN));
        });

        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        // Get supporter data
        String uuid = player.getStringUUID();
        Optional<SupporterData> supporterData =
            SupporterService.getSupporterData(uuid);

        player.sendSystemMessage(Component.literal("═══════════════════════════")
            .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("    💜 Twilight Lib Cosmetics")
            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("═══════════════════════════")
            .withStyle(ChatFormatting.LIGHT_PURPLE));

        if (supporterData.isPresent()) {
            SupporterData data = supporterData.get();

            // Show tier status
            if (data.isActiveSupporter()) {
                String tier = data.getTier();
                String tierDisplay = switch(tier.toLowerCase()) {
                    case "stone" -> "🏅 Stone";
                    case "bronze" -> "🥉 Bronze";
                    case "silver" -> "🥈 Silver";
                    case "gold" -> "🥇 Gold";
                    case "platinum" -> "💎 Platinum";
                    default -> tier;
                };

                player.sendSystemMessage(Component.literal("✨ Supporter Tier: ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(tierDisplay).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("Thank you for supporting development! 💜")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            } else {
                player.sendSystemMessage(Component.literal("Supporter Tier: Expired/Gift")
                    .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("You have manual cosmetic grants!")
                    .withStyle(ChatFormatting.YELLOW));
            }

            // Count cosmetics from capabilities (actual active cosmetics)
            final String[] activeTrail = {"none"};
            final int[] trailCount = {0};
            final int[] addonCount = {0};
            final int[] addonEquippedCount = {0};
            final int[] effectCount = {0};
            final int[] effectEquippedCount = {0};

            player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                // Count all trails (including manual overrides not in registry)
                trailCount[0] = trails.getTrails().size();

                // Get active trail if enabled
                if (trails.isTrailEnabled() && trails.getActiveTrail() != null) {
                    activeTrail[0] = trails.getActiveTrail();
                }
            });

            player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                // Count ALL owned/equipped addons (including manual grants not in registry)
                addonCount[0] = addons.getAddons().size();
                addonEquippedCount[0] = addons.getActiveAddons().size();
            });

            player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                // Count ALL owned/equipped effects (including manual grants not in registry)
                effectCount[0] = effects.getEffects().size();
                effectEquippedCount[0] = effects.getActiveEffects().size();
            });

            int totalCosmetics = trailCount[0] + addonCount[0] + effectCount[0];

            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("Your Cosmetics:")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("  ✨ Trails: " + activeTrail[0] + " / " + trailCount[0] + " owned")
                .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("  🎨 Addons: " + addonEquippedCount[0] + " equipped / " + addonCount[0] + " owned")
                .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("  💫 Effects: " + effectEquippedCount[0] + " equipped / " + effectCount[0] + " owned")
                .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("  Total: " + totalCosmetics)
                .withStyle(ChatFormatting.GREEN));

            // Show manual grants if any (reading from capabilities)
            if (!data.isActiveSupporter()) {
                final Set<String>[] manualTrails = new Set[]{Collections.emptySet()};
                final Set<String>[] manualAddons = new Set[]{Collections.emptySet()};
                final Set<String>[] manualEffects = new Set[]{Collections.emptySet()};

                player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                    manualTrails[0] = trails.getTrails().stream()
                        .filter(SupporterRegistry::isTrailSupporterExclusive)
                        .collect(Collectors.toSet());
                });

                player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                    manualAddons[0] = addons.getAddons().stream()
                        .filter(SupporterRegistry::isAddonSupporterExclusive)
                        .collect(Collectors.toSet());
                });

                player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                    manualEffects[0] = effects.getEffects().stream()
                        .filter(SupporterRegistry::isEffectSupporterExclusive)
                        .collect(Collectors.toSet());
                });

                if (!manualTrails[0].isEmpty() || !manualAddons[0].isEmpty() || !manualEffects[0].isEmpty()) {
                    player.sendSystemMessage(Component.literal(""));
                    player.sendSystemMessage(Component.literal("Manual Grants:")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

                    if (!manualTrails[0].isEmpty()) {
                        player.sendSystemMessage(Component.literal("  Trails: " + String.join(", ", manualTrails[0]))
                            .withStyle(ChatFormatting.GRAY));
                    }
                    if (!manualAddons[0].isEmpty()) {
                        player.sendSystemMessage(Component.literal("  Addons: " + String.join(", ", manualAddons[0]))
                            .withStyle(ChatFormatting.GRAY));
                    }
                    if (!manualEffects[0].isEmpty()) {
                        player.sendSystemMessage(Component.literal("  Effects: " + String.join(", ", manualEffects[0]))
                            .withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        } else {
            player.sendSystemMessage(Component.literal("Supporter Status: Not Active")
                .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("Support development to unlock:")
                .withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(Component.literal("  ✨ Particle trails")
                .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("  🎨 Exclusive addon variants")
                .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("  💫 Special effects")
                .withStyle(ChatFormatting.GRAY));
        }

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("Commands:")
            .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("  /cosmetics trail list - View your trails")
            .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("  /cosmetics addons list - View your addons")
            .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("  /cosmetics effects list - View your effects")
            .withStyle(ChatFormatting.GRAY));

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("💜 All cosmetics are purely visual!")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal(""));

        return 1;
    }

    private static int equipAddon(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String addonId = StringArgumentType.getString(ctx, "addon");

        // Check if capability exists
        if (!player.getCapability(AddonsProvider.ADDONS_CAP).isPresent()) {
            player.sendSystemMessage(Component.literal("❌ Error: Addons capability not initialized")
                .withStyle(ChatFormatting.RED));
            return 1;
        }

        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            // Check ownership - players can only interact with addons they own
            if (!addons.hasAddon(addonId)) {
                player.sendSystemMessage(Component.literal("❌ You don't own '" + addonId + "'!")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Check if already active
            if (addons.isAddonActive(addonId)) {
                player.sendSystemMessage(Component.literal("⚠ Addon '" + addonId + "' is already equipped!")
                    .withStyle(ChatFormatting.YELLOW));
                return;
            }

            // Activate the addon
            addons.setActiveAddon(addonId, true);
            player.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());

            // Sync to all clients
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons()));

            player.sendSystemMessage(Component.literal("Addon '" + addonId + "' equipped")
                .withStyle(ChatFormatting.GREEN));
        });

        return 1;
    }

    private static int unequipAddon(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String addonId = StringArgumentType.getString(ctx, "addon");

        // Check if capability exists
        if (!player.getCapability(AddonsProvider.ADDONS_CAP).isPresent()) {
            player.sendSystemMessage(Component.literal("❌ Error: Addons capability not initialized")
                .withStyle(ChatFormatting.RED));
            return 1;
        }

        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            // Check ownership - players can only interact with addons they own
            if (!addons.hasAddon(addonId)) {
                player.sendSystemMessage(Component.literal("❌ You don't own '" + addonId + "'!")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Check if addon is active
            if (!addons.isAddonActive(addonId)) {
                player.sendSystemMessage(Component.literal("❌ Addon '" + addonId + "' is not equipped!")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Deactivate the addon
            addons.setActiveAddon(addonId, false);
            player.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());

            // Sync to all clients
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons()));

            player.sendSystemMessage(Component.literal("Addon '" + addonId + "' unequipped")
                .withStyle(ChatFormatting.GREEN));
        });

        return 1;
    }
}
