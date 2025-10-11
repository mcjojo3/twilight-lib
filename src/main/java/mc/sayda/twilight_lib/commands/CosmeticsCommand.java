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
import mc.sayda.twilight_lib.supporter.SupporterRegistry;
import mc.sayda.twilight_lib.TwilightConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Set;

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

        dispatcher.register(Commands.literal("cosmetics")
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
                .then(Commands.literal("set")
                    .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(PLAYER_EFFECTS_SUGGESTIONS)
                        .executes(CosmeticsCommand::setEffect)
                    )
                )
                .then(Commands.literal("toggle")
                    .executes(CosmeticsCommand::toggleEffect)
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

    private static int setTrail(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String trailId = StringArgumentType.getString(ctx, "type");

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

            // Sync to all clients
            NetworkHandler.sendToAll(new SyncTrailsPacket(player.getUUID(), trails.serialize()));

            player.sendSystemMessage(Component.literal("✨ Trail set to: " + trailId)
                .withStyle(ChatFormatting.GREEN));
        });

        return 1;
    }

    private static int toggleTrail(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            boolean newState = !trails.isTrailEnabled();
            trails.setTrailEnabled(newState);

            // Sync to all clients
            NetworkHandler.sendToAll(new SyncTrailsPacket(player.getUUID(), trails.serialize()));

            if (newState) {
                player.sendSystemMessage(Component.literal("✨ Trail enabled!")
                    .withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.literal("Trail disabled")
                    .withStyle(ChatFormatting.GRAY));
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

            // Filter to only show trails that are supporter-exclusive
            Set<String> supporterTrailsOwned = playerTrails.stream()
                .filter(SupporterRegistry::isTrailSupporterExclusive)
                .collect(java.util.stream.Collectors.toSet());

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
                for (String trail : supporterTrailsOwned) {
                    String marker = trail.equals(trails.getActiveTrail()) ? "➤ " : "  • ";
                    player.sendSystemMessage(Component.literal(marker + trail)
                        .withStyle(trail.equals(trails.getActiveTrail()) ? ChatFormatting.GOLD : ChatFormatting.WHITE));
                }
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("Use /cosmetics trail set <type> to change trail")
                    .withStyle(ChatFormatting.GRAY));
            }

            // Show available supporter trails by tier (dynamic from SupporterRegistry)
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Trail Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

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

            // Filter to only show addons that are supporter-exclusive
            Set<String> supporterAddonsOwned = playerAddons.stream()
                .filter(SupporterRegistry::isAddonSupporterExclusive)
                .collect(java.util.stream.Collectors.toSet());

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
                    player.sendSystemMessage(Component.literal("  • " + addon)
                        .withStyle(ChatFormatting.WHITE));
                }
            }

            // Show available supporter addons by tier (dynamic from SupporterRegistry)
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Addon Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

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

            // Filter to only show effects that are supporter-exclusive
            Set<String> supporterEffectsOwned = playerEffects.stream()
                .filter(SupporterRegistry::isEffectSupporterExclusive)
                .collect(java.util.stream.Collectors.toSet());

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
                    player.sendSystemMessage(Component.literal("  • " + effect)
                        .withStyle(ChatFormatting.WHITE));
                }
            }

            // Show available supporter effects by tier (dynamic from SupporterRegistry)
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Effect Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

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

            player.sendSystemMessage(Component.literal(""));
        });

        return 1;
    }

    private static int setEffect(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String effectId = StringArgumentType.getString(ctx, "type");

        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            // Check if player has this effect
            if (!effects.hasEffect(effectId)) {
                player.sendSystemMessage(Component.literal("❌ You don't have access to that effect!")
                    .withStyle(ChatFormatting.RED));
                player.sendSystemMessage(Component.literal("Use /cosmetics effects list to see available effects")
                    .withStyle(ChatFormatting.GRAY));
                return;
            }

            player.sendSystemMessage(Component.literal("💫 Effect '" + effectId + "' is always active!")
                .withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.literal("Effects cannot be toggled on/off - they are passive abilities.")
                .withStyle(ChatFormatting.GRAY));
        });

        return 1;
    }

    private static int toggleEffect(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        player.sendSystemMessage(Component.literal("💫 Effects are always active!")
            .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Effects cannot be toggled - they work automatically.")
            .withStyle(ChatFormatting.GRAY));

        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        // Get supporter data
        String uuid = player.getStringUUID();
        java.util.Optional<mc.sayda.twilight_lib.supporter.SupporterData> supporterData =
            mc.sayda.twilight_lib.supporter.SupporterService.getSupporterData(uuid);

        player.sendSystemMessage(Component.literal("═══════════════════════════")
            .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("    💜 Twilight Lib Cosmetics")
            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("═══════════════════════════")
            .withStyle(ChatFormatting.LIGHT_PURPLE));

        if (supporterData.isPresent()) {
            mc.sayda.twilight_lib.supporter.SupporterData data = supporterData.get();

            // Show tier status
            if (data.isActiveSupporter()) {
                String tier = data.getTier();
                String tierDisplay = switch(tier.toLowerCase()) {
                    case "bronze" -> "🥉 Bronze";
                    case "silver" -> "🥈 Silver";
                    case "gold" -> "🥇 Gold";
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

            // Count cosmetics
            java.util.Set<String> allTrails = data.getAllTrails();
            java.util.Set<String> allAddons = data.getAllAddons();
            java.util.Set<String> allEffects = data.getAllEffects();

            int totalCosmetics = allTrails.size() + allAddons.size() + allEffects.size();

            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("Your Cosmetics:")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("  ✨ Trails: " + allTrails.size())
                .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("  🎨 Addons: " + allAddons.size())
                .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("  💫 Effects: " + allEffects.size())
                .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.literal("  Total: " + totalCosmetics)
                .withStyle(ChatFormatting.GREEN));

            // Show manual grants if any
            if (!data.isActiveSupporter()) {
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("Manual Grants:")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

                if (!data.getManualTrails().isEmpty()) {
                    player.sendSystemMessage(Component.literal("  Trails: " + String.join(", ", data.getManualTrails()))
                        .withStyle(ChatFormatting.GRAY));
                }
                if (!data.getManualAddons().isEmpty()) {
                    player.sendSystemMessage(Component.literal("  Addons: " + String.join(", ", data.getManualAddons()))
                        .withStyle(ChatFormatting.GRAY));
                }
                if (!data.getManualEffects().isEmpty()) {
                    player.sendSystemMessage(Component.literal("  Effects: " + String.join(", ", data.getManualEffects()))
                        .withStyle(ChatFormatting.GRAY));
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

        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            // Check if player has access to this addon
            if (!addons.hasAddon(addonId)) {
                player.sendSystemMessage(Component.literal("❌ You don't have access to that addon!")
                    .withStyle(ChatFormatting.RED));
                player.sendSystemMessage(Component.literal("Use /cosmetics addons list to see available addons")
                    .withStyle(ChatFormatting.GRAY));
                return;
            }

            // Check if already active
            if (addons.isAddonActive(addonId)) {
                player.sendSystemMessage(Component.literal("⚠️ Addon '" + addonId + "' is already active!")
                    .withStyle(ChatFormatting.YELLOW));
                return;
            }

            // Activate the addon
            addons.setActiveAddon(addonId, true);
            player.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());

            // Sync to all clients
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons()));

            player.sendSystemMessage(Component.literal("✅ Equipped addon: " + addonId)
                .withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.literal("You have " + addons.getActiveAddons().size() + " addon(s) equipped")
                .withStyle(ChatFormatting.GRAY));
        });

        return 1;
    }

    private static int unequipAddon(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        String addonId = StringArgumentType.getString(ctx, "addon");

        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
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

            player.sendSystemMessage(Component.literal("✅ Unequipped addon: " + addonId)
                .withStyle(ChatFormatting.GREEN));
            int remaining = addons.getActiveAddons().size();
            if (remaining > 0) {
                player.sendSystemMessage(Component.literal("You have " + remaining + " addon(s) still equipped")
                    .withStyle(ChatFormatting.GRAY));
            }
        });

        return 1;
    }
}
