package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

public class AddonCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Suggestion provider for all registered addons
    private static final SuggestionProvider<CommandSourceStack> ADDON_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(AddonRegistry.getAllAddonIds(), builder);

    public static void registerCommands(RegisterCommandsEvent evt) {
        // Register addon commands with both aliases
        for (String alias : new String[]{"twilightlib", "tl"}) {
            evt.getDispatcher().register(
                Commands.literal(alias)
                    .requires(src -> src.hasPermission(2))
                    .then(Commands.literal("addon")
                        // addon list - list all available addons
                        .then(Commands.literal("list")
                            .executes(ctx -> {
                                var addons = AddonRegistry.getAllAddonIds();
                                if (addons.isEmpty()) {
                                    ctx.getSource().sendSuccess(() -> Component.literal("No addons registered"), false);
                                } else {
                                    ctx.getSource().sendSuccess(() -> Component.literal("Available addons: " + String.join(", ", addons)), false);
                                }
                                return 1;
                            }))
                        // addon equip <addonId> [target] - equip addon
                        .then(Commands.literal("equip")
                            .then(Commands.argument("addonId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests(ADDON_SUGGESTIONS)
                                .executes(ctx -> {
                                    String addonId = ctx.getArgument("addonId", String.class);
                                    ServerPlayer target = getTargetPlayer(ctx.getSource());
                                    if (target == null) {
                                        ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                        return 0;
                                    }
                                    return executeEquipAddon(ctx.getSource(), target, addonId);
                                })
                                .then(Commands.argument("target", EntityArgument.player())
                                    .executes(ctx -> {
                                        String addonId = ctx.getArgument("addonId", String.class);
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                        return executeEquipAddon(ctx.getSource(), target, addonId);
                                    }))))
                        // addon unequip <addonId> [target] - unequip addon
                        .then(Commands.literal("unequip")
                            .then(Commands.argument("addonId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests(ADDON_SUGGESTIONS)
                                .executes(ctx -> {
                                    String addonId = ctx.getArgument("addonId", String.class);
                                    ServerPlayer target = getTargetPlayer(ctx.getSource());
                                    if (target == null) {
                                        ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                        return 0;
                                    }
                                    return executeUnequipAddon(ctx.getSource(), target, addonId);
                                })
                                .then(Commands.argument("target", EntityArgument.player())
                                    .executes(ctx -> {
                                        String addonId = ctx.getArgument("addonId", String.class);
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                        return executeUnequipAddon(ctx.getSource(), target, addonId);
                                    }))))
                        // addon clear [target] - clear all addons
                        .then(Commands.literal("clear")
                            .executes(ctx -> {
                                ServerPlayer target = getTargetPlayer(ctx.getSource());
                                if (target == null) {
                                    ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                    return 0;
                                }
                                return executeClearAddons(ctx.getSource(), target);
                            })
                            .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    return executeClearAddons(ctx.getSource(), target);
                                }))))
            );
        }
    }

    private static ServerPlayer getTargetPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception e) {
            return null;
        }
    }

    private static int executeEquipAddon(CommandSourceStack source, ServerPlayer target, String addonId) {
        if (!AddonRegistry.hasAddon(addonId)) {
            source.sendFailure(Component.literal("Unknown addon: " + addonId));
            LOGGER.warn("There is another reality inside all of us. Maybe not this addon though: {}", addonId);
            return 0;
        }

        target.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            addons.addAddon(addonId);
            target.getPersistentData().put("TwilightLibAddons", addons.serialize());
            // Sync to all clients
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(target.getUUID(), addons.getAddons()));
            LOGGER.debug("Changed your mind about me yet? {} equipped addon: {}", target.getGameProfile().getName(), addonId);
        });

        // Only send feedback if source is NOT the target player (admin, command block, console)
        boolean shouldSendFeedback = false;
        try {
            ServerPlayer sourcePlayer = source.getPlayerOrException();
            shouldSendFeedback = !sourcePlayer.getUUID().equals(target.getUUID());
        } catch (Exception e) {
            // Source is not a player (command block, console, etc.)
            shouldSendFeedback = true;
        }

        if (shouldSendFeedback) {
            source.sendSuccess(() -> Component.literal("Equipped addon '" + addonId + "' on " + target.getGameProfile().getName()), true);
        }
        return 1;
    }

    private static int executeUnequipAddon(CommandSourceStack source, ServerPlayer target, String addonId) {
        target.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (addons.hasAddon(addonId)) {
                addons.removeAddon(addonId);
                target.getPersistentData().put("TwilightLibAddons", addons.serialize());
                // Sync to all clients
                NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(target.getUUID(), addons.getAddons()));
                LOGGER.debug("It's so random! {} unequipped addon: {}", target.getGameProfile().getName(), addonId);
            }
        });

        // Only send feedback if source is NOT the target player (admin, command block, console)
        boolean shouldSendFeedback = false;
        try {
            ServerPlayer sourcePlayer = source.getPlayerOrException();
            shouldSendFeedback = !sourcePlayer.getUUID().equals(target.getUUID());
        } catch (Exception e) {
            // Source is not a player (command block, console, etc.)
            shouldSendFeedback = true;
        }

        if (shouldSendFeedback) {
            source.sendSuccess(() -> Component.literal("Unequipped addon '" + addonId + "' from " + target.getGameProfile().getName()), true);
        }
        return 1;
    }

    private static int executeClearAddons(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            addons.clearAddons();
            target.getPersistentData().remove("TwilightLibAddons");
            // Sync to all clients
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(target.getUUID(), addons.getAddons()));
            LOGGER.debug("Dusk and dawn are the same. Cleared all addons for {}", target.getGameProfile().getName());
        });

        // Only send feedback if source is NOT the target player (admin, command block, console)
        boolean shouldSendFeedback = false;
        try {
            ServerPlayer sourcePlayer = source.getPlayerOrException();
            shouldSendFeedback = !sourcePlayer.getUUID().equals(target.getUUID());
        } catch (Exception e) {
            // Source is not a player (command block, console, etc.)
            shouldSendFeedback = true;
        }

        if (shouldSendFeedback) {
            source.sendSuccess(() -> Component.literal("Cleared all addons from " + target.getGameProfile().getName()), true);
        }
        return 1;
    }
}