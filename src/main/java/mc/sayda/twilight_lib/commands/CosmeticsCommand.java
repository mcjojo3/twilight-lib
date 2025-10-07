package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.cosmetics.TrailType;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncTrailsPacket;
import mc.sayda.twilight_lib.supporter.SupporterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("cosmetics")
            .then(Commands.literal("trail")
                .then(Commands.literal("set")
                    .then(Commands.argument("type", StringArgumentType.word())
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
                .then(Commands.literal("list")
                    .executes(CosmeticsCommand::listAddons)
                )
            )
            .then(Commands.literal("effects")
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
            player.sendSystemMessage(Component.literal("💜 Cosmetic Only - No Gameplay Advantage")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
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

            player.sendSystemMessage(Component.literal("✨ Your Supporter Trails:")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

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

            // Show available supporter trails by tier
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Trail Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("🥉 Bronze: hearts")
                .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("🥈 Silver: +sparkles, cherry_blossom")
                .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("🥇 Gold: +twilight, stars")
                .withStyle(ChatFormatting.GRAY));
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

            player.sendSystemMessage(Component.literal("🎨 Your Supporter Addons:")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

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

            // Show available supporter addons by tier
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Addon Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("🥈 Silver: galaxy_tail")
                .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("🥇 Gold: +starlight_ears, halo, twilight_wings")
                .withStyle(ChatFormatting.GRAY));
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

            player.sendSystemMessage(Component.literal("💫 Your Supporter Effects:")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

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

            // Show available supporter effects by tier
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("💜 Supporter Effect Tiers:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("🥇 Gold: respawn_twilight")
                .withStyle(ChatFormatting.GRAY));
        });

        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        // Check if player has any supporter items
        final boolean[] hasAnySupporter = {false};

        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            long supporterTrailCount = trails.getTrails().stream()
                .filter(SupporterRegistry::isTrailSupporterExclusive)
                .count();
            if (supporterTrailCount > 0) hasAnySupporter[0] = true;
        });

        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            long supporterAddonCount = addons.getAddons().stream()
                .filter(SupporterRegistry::isAddonSupporterExclusive)
                .count();
            if (supporterAddonCount > 0) hasAnySupporter[0] = true;
        });

        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            long supporterEffectCount = effects.getEffects().stream()
                .filter(SupporterRegistry::isEffectSupporterExclusive)
                .count();
            if (supporterEffectCount > 0) hasAnySupporter[0] = true;
        });

        player.sendSystemMessage(Component.literal("═══════════════════════════")
            .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("    💜 Twilight Lib Cosmetics")
            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("═══════════════════════════")
            .withStyle(ChatFormatting.LIGHT_PURPLE));

        if (hasAnySupporter[0]) {
            player.sendSystemMessage(Component.literal("✨ Supporter Status: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("Active").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("Thank you for supporting development! 💜")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
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
        player.sendSystemMessage(Component.literal("💜 All cosmetics are purely visual,")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("  enjoy your cosmetics!")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("═══════════════════════════")
            .withStyle(ChatFormatting.LIGHT_PURPLE));

        return 1;
    }
}
