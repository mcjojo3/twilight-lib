package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.capabilities.ITrails;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.cosmetics.EffectType;
import mc.sayda.twilight_lib.cosmetics.ModRequirement;
import mc.sayda.twilight_lib.cosmetics.TrailType;
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
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.SyncEffectsPacket;
import mc.sayda.twilight_lib.network.SyncTrailsPacket;
import mc.sayda.twilight_lib.network.SyncMorphPacket;

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

        private static final SuggestionProvider<CommandSourceStack> PLAYER_TRAILS_SUGGESTIONS = (context, builder) -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        ITrails trails = DataUtils.getTrailsData(player);
                        if (trails != null) {
                                Set<String> playerTrails = trails.getTrails();
                                SharedSuggestionProvider.suggest(playerTrails.stream(), builder);
                        }
                }
                return builder.buildFuture();
        };

        // Suggestion provider that shows only addons the player has access to
        private static final SuggestionProvider<CommandSourceStack> PLAYER_ADDONS_SUGGESTIONS = (context, builder) -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        IAddons addons = DataUtils.getAddonsData(player);
                        if (addons != null) {
                                Set<String> playerAddons = addons.getAddons();
                                SharedSuggestionProvider.suggest(playerAddons.stream(), builder);
                        }
                }
                return builder.buildFuture();
        };

        // Suggestion provider that shows only effects the player has access to
        private static final SuggestionProvider<CommandSourceStack> PLAYER_EFFECTS_SUGGESTIONS = (context, builder) -> {
                if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        IEffects effects = DataUtils.getEffectsData(player);
                        if (effects != null) {
                                Set<String> playerEffects = effects.getEffects();
                                SharedSuggestionProvider.suggest(playerEffects.stream().map(String::toString), builder);
                        }
                }
                return builder.buildFuture();
        };

        public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

                // Register all command aliases
                for (String alias : new String[] { "cosmetics", "tlc", "tlcosmetics", "twilightlibcosmetics" }) {
                        dispatcher.register(Commands.literal(alias)
                                        .then(Commands.literal("trails")
                                                        .then(Commands.literal("equip")
                                                                        .then(Commands.argument("type",
                                                                                        StringArgumentType.string())
                                                                                        .suggests(PLAYER_TRAILS_SUGGESTIONS)
                                                                                        .executes(CosmeticsCommand::equipTrail)))
                                                        .then(Commands.literal("unequip")
                                                                        .then(Commands.argument("type",
                                                                                        StringArgumentType.string())
                                                                                        .suggests(PLAYER_TRAILS_SUGGESTIONS)
                                                                                        .executes(CosmeticsCommand::unequipTrail)))
                                                        .then(Commands.literal("list")
                                                                        .executes(CosmeticsCommand::listTrails)))
                                        .then(Commands.literal("addons")
                                                        .then(Commands.literal("equip")
                                                                        .then(Commands.argument("addon",
                                                                                        StringArgumentType.string())
                                                                                        .suggests(PLAYER_ADDONS_SUGGESTIONS)
                                                                                        .executes(CosmeticsCommand::equipAddon)))
                                                        .then(Commands.literal("unequip")
                                                                        .then(Commands.argument("addon",
                                                                                        StringArgumentType.string())
                                                                                        .suggests(PLAYER_ADDONS_SUGGESTIONS)
                                                                                        .executes(CosmeticsCommand::unequipAddon)))
                                                        .then(Commands.literal("list")
                                                                        .executes(CosmeticsCommand::listAddons)))
                                        .then(Commands.literal("effects")
                                                        .then(Commands.literal("equip")
                                                                        .then(Commands.argument("type",
                                                                                        StringArgumentType.string())
                                                                                        .suggests(PLAYER_EFFECTS_SUGGESTIONS)
                                                                                        .executes(CosmeticsCommand::equipEffect)))
                                                        .then(Commands.literal("unequip")
                                                                        .then(Commands.argument("type",
                                                                                        StringArgumentType.string())
                                                                                        .suggests(PLAYER_EFFECTS_SUGGESTIONS)
                                                                                        .executes(CosmeticsCommand::unequipEffect)))
                                                        .then(Commands.literal("list")
                                                                        .executes(CosmeticsCommand::listEffects)))
                                        .then(Commands.literal("info")
                                                        .executes(CosmeticsCommand::showInfo)));
                }
        }

        private static int equipTrail(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                String trailId = StringArgumentType.getString(ctx, "type");
                ITrails trails = DataUtils.getTrailsData(player);
                if (trails == null) {
                        player.sendSystemMessage(Component.literal("❁ETrail data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check ownership - players can only interact with trails they own
                if (!trails.hasTrail(trailId)) {
                        player.sendSystemMessage(Component.literal("❁EYou don't have access to that trail!")
                                        .withStyle(ChatFormatting.RED));
                        player.sendSystemMessage(Component.literal("Use /cosmetics trails list to see available trails")
                                        .withStyle(ChatFormatting.GRAY));
                        return 0;
                }

                // Check if already active
                if (trails.getActiveTrails().contains(trailId)) {
                        player.sendSystemMessage(Component.literal("⚠ Trail '" + trailId + "' is already equipped!")
                                        .withStyle(ChatFormatting.YELLOW));
                        return 0;
                }

                // Activate the trail (player action = non-persistent, cleared if trail removed
                // from account)
                trails.setActiveTrail(trailId, true);

                // Save to persistent NBT
                DataUtils.getPersistentData(player).put(TwilightConstants.NBT_TRAILS, trails.serialize());

                // Sync to all clients
                NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.getActiveTrails()));

                player.sendSystemMessage(Component.literal("Trail '" + trailId + "' equipped")
                                .withStyle(ChatFormatting.GREEN));

                return 1;
        }

        private static int unequipTrail(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                String trailId = StringArgumentType.getString(ctx, "type");
                ITrails trails = DataUtils.getTrailsData(player);
                if (trails == null) {
                        player.sendSystemMessage(Component.literal("❁ETrail data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check ownership - players can only interact with trails they own
                if (!trails.hasTrail(trailId)) {
                        player.sendSystemMessage(Component.literal("❁EYou don't have access to that trail!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check if trail is active
                if (!trails.getActiveTrails().contains(trailId)) {
                        player.sendSystemMessage(Component.literal("❁ETrail '" + trailId + "' is not equipped!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Deactivate the trail
                trails.setActiveTrail(trailId, false);

                // Save to persistent NBT
                DataUtils.getPersistentData(player).put(TwilightConstants.NBT_TRAILS, trails.serialize());

                // Sync to all clients
                NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.getActiveTrails()));

                player.sendSystemMessage(Component.literal("Trail '" + trailId + "' unequipped")
                                .withStyle(ChatFormatting.GREEN));

                return 1;
        }

        private static int listTrails(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                var trails = DataUtils.getTrailsData(player);
                if (trails == null) {
                        player.sendSystemMessage(Component.literal("❁ETrail data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                Set<String> playerTrails = trails.getTrails();
                Set<String> allSupporterTrails = SupporterRegistry.getAllSupporterTrails();

                // Show ALL owned trails (including manual grants not in registry)
                Set<String> supporterTrailsOwned = playerTrails;

                player.sendSystemMessage(Component.literal("══════════════════════════╁")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                player.sendSystemMessage(Component.literal("    ✨ Your Supporter Trails")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("══════════════════════════╁")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));

                if (supporterTrailsOwned.isEmpty()) {
                        player.sendSystemMessage(Component.literal("  No supporter trails available yet!")
                                        .withStyle(ChatFormatting.GRAY));
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("💜 Support to unlock exclusive trails!")
                                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                        Set<String> activeTrails = trails.getActiveTrails();

                        for (String trailId : supporterTrailsOwned) {
                                TrailType type = TrailType.fromId(trailId);
                                if (type != null && !type.isAvailable())
                                        continue;

                                boolean isActive = activeTrails.contains(trailId);
                                String marker = isActive ? "✁" : "  • ";
                                ChatFormatting color = isActive ? ChatFormatting.GOLD : ChatFormatting.WHITE;
                                player.sendSystemMessage(Component.literal(marker + trailId)
                                                .withStyle(color));
                        }
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(
                                        Component.literal("Use /cosmetics trails equip <type> to equip a trail")
                                                        .withStyle(ChatFormatting.GRAY));
                        player.sendSystemMessage(
                                        Component.literal("Use /cosmetics trails unequip <type> to unequip a trail")
                                                        .withStyle(ChatFormatting.GRAY));
                }

                // Show available supporter trails by tier (dynamic from SupporterRegistry)
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("💜 Supporter Trail Tiers:")
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

                // For future expansion?
                /*
                 * Set<String> stoneTrails = SupporterRegistry.getTrailsForTier("stone");
                 * if (!stoneTrails.isEmpty()) {
                 * player.sendSystemMessage(Component.literal("🏅 Stone: " + String.join(", ",
                 * stoneTrails))
                 * .withStyle(ChatFormatting.GRAY));
                 * }
                 */

                Set<String> bronzeTrails = SupporterRegistry.getTrailsForTier("bronze");
                if (!bronzeTrails.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁EBronze: " + String.join(", ", bronzeTrails))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> silverTrails = SupporterRegistry.getTrailsForTier("silver");
                if (!silverTrails.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁ESilver: " + String.join(", ", silverTrails))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> goldTrails = SupporterRegistry.getTrailsForTier("gold");
                if (!goldTrails.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁 Gold: " + String.join(", ", goldTrails))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> platinumTrails = SupporterRegistry.getTrailsForTier("platinum");
                if (!platinumTrails.isEmpty()) {
                        player.sendSystemMessage(Component.literal("💎 Platinum: " + String.join(", ", platinumTrails))
                                        .withStyle(ChatFormatting.GRAY));
                }

                player.sendSystemMessage(Component.literal(""));

                return 1;
        }

        private static int listAddons(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                IAddons addons = DataUtils.getAddonsData(player);
                if (addons == null) {
                        player.sendSystemMessage(Component.literal("❁EAddon data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                Set<String> playerAddons = addons.getAddons();
                Set<String> activeAddons = addons.getActiveAddons();

                // Show ALL owned addons (including manual grants not in registry)
                Set<String> supporterAddonsOwned = playerAddons;

                player.sendSystemMessage(Component.literal("══════════════════════════╁")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                player.sendSystemMessage(Component.literal("    🎨 Your Supporter Addons")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("══════════════════════════╁")
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

                                // Only show addons that are available based on mod requirements and config
                                boolean isAvailable = AddonRegistry.getAddon(addon)
                                                .map(info -> ModRequirement.shouldLoad(info.modTags()))
                                                .orElse(true); // Default to true if not in registry (manual grant)

                                if (!isAvailable)
                                        continue;

                                String marker = isEquipped ? "✁" : "  • ";
                                ChatFormatting color = isEquipped ? ChatFormatting.GOLD : ChatFormatting.WHITE;
                                player.sendSystemMessage(Component.literal(marker + addon)
                                                .withStyle(color));
                        }
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("Use /cosmetics addons equip <type> to add an addon")
                                        .withStyle(ChatFormatting.GRAY));
                        player.sendSystemMessage(
                                        Component.literal("Use /cosmetics addons unequip <type> to remove an addon")
                                                        .withStyle(ChatFormatting.GRAY));
                }

                // Show available supporter addons by tier (dynamic from SupporterRegistry)
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("💜 Supporter Addon Tiers:")
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

                // For future expansion?
                /*
                 * Set<String> stoneAddons = SupporterRegistry.getAddonsForTier("stone");
                 * if (!stoneAddons.isEmpty()) {
                 * player.sendSystemMessage(Component.literal("🏅 Stone: " + String.join(", ",
                 * stoneAddons))
                 * .withStyle(ChatFormatting.GRAY));
                 * }
                 */

                Set<String> bronzeAddons = SupporterRegistry.getAddonsForTier("bronze");
                if (!bronzeAddons.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁EBronze: " + String.join(", ", bronzeAddons))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> silverAddons = SupporterRegistry.getAddonsForTier("silver");
                if (!silverAddons.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁ESilver: " + String.join(", ", silverAddons))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> goldAddons = SupporterRegistry.getAddonsForTier("gold");
                if (!goldAddons.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁EGold: " + String.join(", ", goldAddons))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> platinumAddons = SupporterRegistry.getAddonsForTier("platinum");
                if (!platinumAddons.isEmpty()) {
                        player.sendSystemMessage(Component.literal("💎 Platinum: " + String.join(", ", platinumAddons))
                                        .withStyle(ChatFormatting.GRAY));
                }

                player.sendSystemMessage(Component.literal(""));

                return 1;
        }

        private static int listEffects(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                IEffects effects = DataUtils.getEffectsData(player);
                if (effects == null) {
                        player.sendSystemMessage(Component.literal("❁EEffect data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                Set<String> playerEffects = effects.getEffects();
                Set<String> activeEffects = effects.getActiveEffects();

                // Show ALL owned effects (including manual grants not in registry)
                Set<String> supporterEffectsOwned = playerEffects;

                player.sendSystemMessage(Component.literal("══════════════════════════╁")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                player.sendSystemMessage(Component.literal("    💫 Your Supporter Effects")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("══════════════════════════╁")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));

                if (supporterEffectsOwned.isEmpty()) {
                        player.sendSystemMessage(Component.literal("  No supporter effects available yet!")
                                        .withStyle(ChatFormatting.GRAY));
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("💜 Support to unlock exclusive effects!")
                                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                } else {
                        for (String effectId : supporterEffectsOwned) {
                                EffectType type = EffectType.fromId(effectId);
                                if (type != null && !type.isAvailable())
                                        continue;

                                boolean isEquipped = activeEffects.contains(effectId);
                                String marker = isEquipped ? "✁" : "  • ";
                                ChatFormatting color = isEquipped ? ChatFormatting.GOLD : ChatFormatting.WHITE;
                                player.sendSystemMessage(Component.literal(marker + effectId)
                                                .withStyle(color));
                        }
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(
                                        Component.literal("Use /cosmetics effects equip <type> to equip an effect")
                                                        .withStyle(ChatFormatting.GRAY));
                        player.sendSystemMessage(
                                        Component.literal("Use /cosmetics effects unequip <type> to unequip an effect")
                                                        .withStyle(ChatFormatting.GRAY));
                }

                // Show available supporter effects by tier (dynamic from SupporterRegistry)
                player.sendSystemMessage(Component.literal(""));
                player.sendSystemMessage(Component.literal("💜 Supporter Effect Tiers:")
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

                // For future expansion?
                /*
                 * Set<String> stoneEffects = SupporterRegistry.getEffectsForTier("stone");
                 * if (!stoneEffects.isEmpty()) {
                 * player.sendSystemMessage(Component.literal("🏅 Stone: " + String.join(", ",
                 * stoneEffects))
                 * .withStyle(ChatFormatting.GRAY));
                 * }
                 */

                Set<String> bronzeEffects = SupporterRegistry.getEffectsForTier("bronze");
                if (!bronzeEffects.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁EBronze: " + String.join(", ", bronzeEffects))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> silverEffects = SupporterRegistry.getEffectsForTier("silver");
                if (!silverEffects.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁ESilver: " + String.join(", ", silverEffects))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> goldEffects = SupporterRegistry.getEffectsForTier("gold");
                if (!goldEffects.isEmpty()) {
                        player.sendSystemMessage(Component.literal("🥁EGold: " + String.join(", ", goldEffects))
                                        .withStyle(ChatFormatting.GRAY));
                }

                Set<String> platinumEffects = SupporterRegistry.getEffectsForTier("platinum");
                if (!platinumEffects.isEmpty()) {
                        player.sendSystemMessage(Component.literal("💎 Platinum: " + String.join(", ", platinumEffects))
                                        .withStyle(ChatFormatting.GRAY));
                }

                player.sendSystemMessage(Component.literal(""));

                return 1;
        }

        private static int equipEffect(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                String effectId = StringArgumentType.getString(ctx, "type");
                IEffects effects = DataUtils.getEffectsData(player);
                if (effects == null) {
                        player.sendSystemMessage(Component.literal("❁EEffect data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check ownership - players can only interact with effects they own
                if (!effects.hasEffect(effectId)) {
                        player.sendSystemMessage(Component.literal("❁EYou don't own '" + effectId + "'!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check if already active
                if (effects.isEffectActive(effectId)) {
                        player.sendSystemMessage(Component.literal("⚠ Effect '" + effectId + "' is already equipped!")
                                        .withStyle(ChatFormatting.YELLOW));
                        return 0;
                }

                // Activate the effect
                effects.setActiveEffect(effectId, true);
                DataUtils.getPersistentData(player).put(TwilightConstants.NBT_EFFECTS, effects.serialize());

                // Sync to all clients
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects()));

                player.sendSystemMessage(Component.literal("Effect '" + effectId + "' equipped")
                                .withStyle(ChatFormatting.GREEN));

                return 1;
        }

        private static int unequipEffect(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                String effectId = StringArgumentType.getString(ctx, "type");
                IEffects effects = DataUtils.getEffectsData(player);
                if (effects == null) {
                        player.sendSystemMessage(Component.literal("❁EEffect data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check ownership - players can only interact with effects they own
                if (!effects.hasEffect(effectId)) {
                        player.sendSystemMessage(Component.literal("❁EYou don't own '" + effectId + "'!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check if effect is active
                if (!effects.isEffectActive(effectId)) {
                        player.sendSystemMessage(Component.literal("❁EEffect '" + effectId + "' is not equipped!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Deactivate the effect
                effects.setActiveEffect(effectId, false);
                DataUtils.getPersistentData(player).put(TwilightConstants.NBT_EFFECTS, effects.serialize());

                // Sync to all clients
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects()));

                player.sendSystemMessage(Component.literal("Effect '" + effectId + "' unequipped")
                                .withStyle(ChatFormatting.GREEN));

                return 1;
        }

        private static int showInfo(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                // Get supporter data
                String uuid = player.getStringUUID();
                Optional<SupporterData> supporterData = SupporterService.getSupporterData(uuid);

                player.sendSystemMessage(Component.literal("══════════════════════════╁")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                player.sendSystemMessage(Component.literal("    💜 Twilight Lib Cosmetics")
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("══════════════════════════╁")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));

                if (supporterData.isPresent()) {
                        SupporterData data = supporterData.get();

                        // Show tier status
                        if (data.isActiveSupporter()) {
                                String tier = data.getTier();
                                String tierDisplay = switch (tier.toLowerCase()) {
                                        case "stone" -> "🏅 Stone";
                                        case "bronze" -> "🥉 Bronze";
                                        case "silver" -> "🥈 Silver";
                                        case "gold" -> "🥇 Gold";
                                        case "platinum" -> "💎 Platinum";
                                        default -> tier;
                                };

                                player.sendSystemMessage(
                                                Component.literal("✨ Supporter Tier: ").withStyle(ChatFormatting.WHITE)
                                                                .append(Component.literal(tierDisplay).withStyle(
                                                                                ChatFormatting.GOLD,
                                                                                ChatFormatting.BOLD)));
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
                        final int[] trailCount = { 0 };
                        final int[] trailEquippedCount = { 0 };
                        final int[] addonCount = { 0 };
                        final int[] addonEquippedCount = { 0 };
                        final int[] effectCount = { 0 };
                        final int[] effectEquippedCount = { 0 };

                        var trails = DataUtils.getTrailsData(player);
                        if (trails != null) {
                                // Count ALL owned/equipped trails (including manual grants not in registry)
                                trailCount[0] = trails.getTrails().size();
                                trailEquippedCount[0] = trails.getActiveTrails().size();
                        }

                        var addons = DataUtils.getAddonsData(player);
                        if (addons != null) {
                                // Count ALL owned/equipped addons (including manual grants not in registry)
                                addonCount[0] = addons.getAddons().size();
                                addonEquippedCount[0] = addons.getActiveAddons().size();
                        }

                        var effects = DataUtils.getEffectsData(player);
                        if (effects != null) {
                                // Count ALL owned/equipped effects (including manual grants not in registry)
                                effectCount[0] = effects.getEffects().size();
                                effectEquippedCount[0] = effects.getActiveEffects().size();
                        }

                        int totalCosmetics = trailCount[0] + addonCount[0] + effectCount[0];

                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("Your Cosmetics:")
                                        .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
                        player.sendSystemMessage(Component
                                        .literal("  ✨ Trails: " + trailEquippedCount[0] + " equipped / " + trailCount[0]
                                                        + " owned")
                                        .withStyle(ChatFormatting.AQUA));
                        player.sendSystemMessage(Component
                                        .literal("  🎨 Addons: " + addonEquippedCount[0] + " equipped / "
                                                        + addonCount[0] + " owned")
                                        .withStyle(ChatFormatting.AQUA));
                        player.sendSystemMessage(Component
                                        .literal("  💫 Effects: " + effectEquippedCount[0] + " equipped / "
                                                        + effectCount[0] + " owned")
                                        .withStyle(ChatFormatting.AQUA));
                        player.sendSystemMessage(Component.literal("  Total: " + totalCosmetics)
                                        .withStyle(ChatFormatting.GREEN));

                        // Show manual grants if any (reading from capabilities)
                        if (!data.isActiveSupporter()) {
                                final Set<String>[] manualTrails = new Set[] { Collections.emptySet() };
                                final Set<String>[] manualAddons = new Set[] { Collections.emptySet() };
                                final Set<String>[] manualEffects = new Set[] { Collections.emptySet() };

                                // Reuse variables declared earlier in the method
                                if (trails != null) {
                                        Set<String> trailSet = trails.getTrails();
                                        if (trailSet != null) {
                                                manualTrails[0] = trailSet.stream()
                                                                .filter(SupporterRegistry::isTrailSupporterExclusive)
                                                                .collect(Collectors.toSet());
                                        }
                                }

                                if (addons != null) {
                                        Set<String> addonSet = addons.getAddons();
                                        if (addonSet != null) {
                                                manualAddons[0] = addonSet.stream()
                                                                .filter(SupporterRegistry::isAddonSupporterExclusive)
                                                                .collect(Collectors.toSet());
                                        }
                                }

                                if (effects != null) {
                                        Set<String> effectSet = effects.getEffects();
                                        if (effectSet != null) {
                                                manualEffects[0] = effectSet.stream()
                                                                .filter(SupporterRegistry::isEffectSupporterExclusive)
                                                                .collect(Collectors.toSet());
                                        }
                                }

                                if (!manualTrails[0].isEmpty() || !manualAddons[0].isEmpty()
                                                || !manualEffects[0].isEmpty()) {
                                        player.sendSystemMessage(Component.literal(""));
                                        player.sendSystemMessage(Component.literal("Manual Grants:")
                                                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

                                        if (!manualTrails[0].isEmpty()) {
                                                player.sendSystemMessage(Component
                                                                .literal("  Trails: "
                                                                                + String.join(", ", manualTrails[0]))
                                                                .withStyle(ChatFormatting.GRAY));
                                        }
                                        if (!manualAddons[0].isEmpty()) {
                                                player.sendSystemMessage(Component
                                                                .literal("  Addons: "
                                                                                + String.join(", ", manualAddons[0]))
                                                                .withStyle(ChatFormatting.GRAY));
                                        }
                                        if (!manualEffects[0].isEmpty()) {
                                                player.sendSystemMessage(Component
                                                                .literal("  Effects: "
                                                                                + String.join(", ", manualEffects[0]))
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
                player.sendSystemMessage(Component.literal("  /cosmetics trails list - View your trails")
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
                IAddons addons = DataUtils.getAddonsData(player);
                if (addons == null) {
                        player.sendSystemMessage(Component.literal("❁EAddon data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check ownership - players can only interact with addons they own
                if (!addons.hasAddon(addonId)) {
                        player.sendSystemMessage(Component.literal("❁EYou don't own '" + addonId + "'!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Verify addon is available based on mod requirements
                boolean isAvailable = AddonRegistry.getAddon(addonId)
                                .map(info -> ModRequirement.shouldLoad(info.modTags()))
                                .orElse(true);
                if (!isAvailable) {
                        player.sendSystemMessage(Component.literal("❁EThat addon requires a mod that isn't loaded on this server!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check if already active
                if (addons.isAddonActive(addonId)) {
                        player.sendSystemMessage(Component.literal("⚠ Addon '" + addonId + "' is already equipped!")
                                        .withStyle(ChatFormatting.YELLOW));
                        return 0;
                }

                // Activate the addon
                addons.setActiveAddon(addonId, true);
                DataUtils.getPersistentData(player).put(TwilightConstants.NBT_ADDONS, addons.serialize());

                // Sync to all clients
                NetworkHandler.sendAddonsToAll(
                                new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons(),
                                                addons.getExternalGrants(), addons.getAllAddonTints()));

                player.sendSystemMessage(Component.literal("Addon '" + addonId + "' equipped")
                                .withStyle(ChatFormatting.GREEN));

                return 1;
        }

        private static int unequipAddon(CommandContext<CommandSourceStack> ctx) {
                if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                        return 0;
                }

                String addonId = StringArgumentType.getString(ctx, "addon");
                IAddons addons = DataUtils.getAddonsData(player);
                if (addons == null) {
                        player.sendSystemMessage(Component.literal("❁EAddon data not initialized!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check ownership - players can only interact with addons they own
                if (!addons.hasAddon(addonId)) {
                        player.sendSystemMessage(Component.literal("❁EYou don't own '" + addonId + "'!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Check if addon is active
                if (!addons.isAddonActive(addonId)) {
                        player.sendSystemMessage(Component.literal("❁EAddon '" + addonId + "' is not equipped!")
                                        .withStyle(ChatFormatting.RED));
                        return 0;
                }

                // Deactivate the addon
                addons.setActiveAddon(addonId, false);
                DataUtils.getPersistentData(player).put(TwilightConstants.NBT_ADDONS, addons.serialize());

                // Sync to all clients
                NetworkHandler.sendAddonsToAll(
                                new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons(),
                                                addons.getExternalGrants(), addons.getAllAddonTints()));

                player.sendSystemMessage(Component.literal("Addon '" + addonId + "' unequipped")
                                .withStyle(ChatFormatting.GREEN));

                return 1;
        }
}
