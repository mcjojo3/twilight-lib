package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;

import mc.sayda.twilight_lib.capabilities.TrailsData;

import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.addon.AddonRegistry;

import mc.sayda.twilight_lib.capabilities.AddonsData;
import mc.sayda.twilight_lib.capabilities.IModelVariant;

import mc.sayda.twilight_lib.cosmetics.TrailType;
import mc.sayda.twilight_lib.cosmetics.EffectType;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncMorphPacket;
import mc.sayda.twilight_lib.network.SyncTrailsPacket;
import mc.sayda.twilight_lib.network.SyncEffectsPacket;
import mc.sayda.twilight_lib.network.SyncModelVariantPacket;
import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.config.TwilightConfig;
import mc.sayda.twilight_lib.supporter.SupporterService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import org.slf4j.Logger;
import com.mojang.brigadier.CommandDispatcher;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TwilightLibCommands {
        private static final Logger LOGGER = LogUtils.getLogger();

        // Cache of valid living entity types to avoid creating test entities repeatedly
        // Note: Cache is invalidated on world unload to detect dynamically registered
        // entities
        // Thread-safe: Synchronized set for concurrent command access
        private static final Set<ResourceLocation> VALID_LIVING_ENTITIES = Collections.synchronizedSet(new HashSet<>());
        private static volatile boolean cacheInitialized = false;
        private static volatile int cachedRegistrySize = 0;

        // Suggestion provider for all entity types
        private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTIONS = (context, builder) -> {
                // Initialize or refresh cache if registry changed (synchronized to prevent race
                // condition)
                var level = context.getSource().getLevel();
                // Null check: console/command block commands have no level context
                if (level == null) {
                        LOGGER.debug("Or, what. Cannot provide entity suggestions - no world context (console/command block)");
                        return builder.buildFuture(); // Return empty suggestions
                }
                synchronized (TwilightLibCommands.class) {
                        if (!cacheInitialized || shouldRefreshCache(level)) {
                                initializeEntityCache(level);
                        }
                }
                // Synchronize on the set itself for iteration (per
                // Collections.synchronizedSet() contract)
                synchronized (VALID_LIVING_ENTITIES) {
                        return SharedSuggestionProvider.suggestResource(
                                        (java.util.stream.Stream<ResourceLocation>) VALID_LIVING_ENTITIES.stream(),
                                        builder);
                }
        };

        // Suggestion provider for trail types
        private static final SuggestionProvider<CommandSourceStack> TRAIL_SUGGESTIONS = (context, builder) -> {
                for (TrailType type : TrailType.values()) {
                        builder.suggest(type.getId());
                }
                return builder.buildFuture();
        };

        // Suggestion provider for effect types
        private static final SuggestionProvider<CommandSourceStack> EFFECT_SUGGESTIONS = (context, builder) -> {
                for (EffectType type : EffectType.values()) {
                        builder.suggest(type.getId());
                }
                return builder.buildFuture();
        };

        // Suggestion provider for addon types
        private static final SuggestionProvider<CommandSourceStack> ADDON_SUGGESTIONS = (context,
                        builder) -> SharedSuggestionProvider.suggest((Iterable<String>) AddonRegistry.getAllAddonIds(),
                                        builder);

        // Suggestion provider for model variants (steve/alex)
        private static final SuggestionProvider<CommandSourceStack> MODEL_VARIANT_SUGGESTIONS = (context,
                        builder) -> SharedSuggestionProvider
                                        .suggest((Iterable<String>) java.util.Arrays.asList("steve", "alex"), builder);

        /**
         * Check if entity registry has changed (new mods loaded entities).
         * 
         * @return true if cache should be refreshed
         */
        private static boolean shouldRefreshCache(net.minecraft.world.level.Level level) {
                int currentSize = BuiltInRegistries.ENTITY_TYPE.size();
                if (currentSize != cachedRegistrySize) {
                        LOGGER.debug(
                                        "There is another reality inside all of us. Maybe not you though. Entity registry changed: {} -> {}",
                                        cachedRegistrySize, currentSize);
                        return true;
                }
                return false;
        }

        /**
         * Initialize cache of valid living entity types.
         * Called once on first command suggestion to avoid creating test entities on
         * every keystroke.
         * Automatically invalidates and refreshes if registry size changes.
         * Thread-safe with double-check locking pattern.
         */
        private static synchronized void initializeEntityCache(net.minecraft.world.level.Level level) {
                // Double-check pattern to avoid repeated initialization
                if (cacheInitialized && cachedRegistrySize == BuiltInRegistries.ENTITY_TYPE.size()) {
                        return;
                }

                // Clear cache if refreshing (no nested synchronization needed - method is
                // already synchronized)
                if (cacheInitialized) {
                        VALID_LIVING_ENTITIES.clear();
                }

                LOGGER.debug("Hi! My name is Zoe. Initializing entity type cache...");
                for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                        // Safety check: prevent unbounded cache growth
                        if (VALID_LIVING_ENTITIES.size() >= TwilightConfig.MAX_ENTITY_CACHE_SIZE.get()) {
                                LOGGER.warn(
                                                "Really?! Entity cache size limit reached ({}). Stopping cache initialization to prevent memory issues.",
                                                TwilightConfig.MAX_ENTITY_CACHE_SIZE.get());
                                break;
                        }

                        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
                        if (type == null || type == EntityType.PLAYER)
                                continue;

                        // Optimization: Most living entities belong to specific categories.
                        // Items, Projectiles, Boats, etc. are under MISC.
                        net.minecraft.world.entity.MobCategory category = type.getCategory();
                        if (category != net.minecraft.world.entity.MobCategory.MISC) {
                                VALID_LIVING_ENTITIES.add(rl);
                                continue;
                        }

                        // For MISC category, we still need to check if it's a LivingEntity (e.g.
                        // ArmorStand)
                        // but we do it more carefully.
                        net.minecraft.world.entity.Entity testEntity = null;
                        try {
                                testEntity = type.create(level);
                                if (testEntity instanceof LivingEntity) {
                                        VALID_LIVING_ENTITIES.add(rl);
                                }
                        } catch (Exception e) {
                                // Ignore entities that fail to create
                        } finally {
                                if (testEntity != null) {
                                        try {
                                                testEntity.discard();
                                        } catch (Exception e) {
                                        }
                                }
                        }
                }
                cachedRegistrySize = BuiltInRegistries.ENTITY_TYPE.size();
                cacheInitialized = true;
                LOGGER.info("What's your name? Entity type cache initialized with {} living entities",
                                VALID_LIVING_ENTITIES.size());
        }

        public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
                for (String alias : new String[] { "twilightlib", "tl" }) {
                        dispatcher.register(
                                        Commands.literal(alias)
                                                        .requires(src -> src.hasPermission(2))
                                                        .then(Commands.literal("morph")
                                                                        .then(Commands.argument("entity",
                                                                                        ResourceLocationArgument.id())
                                                                                        .suggests(ENTITY_SUGGESTIONS)
                                                                                        .executes(ctx -> executeMorph(
                                                                                                        ctx.getSource(),
                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                        ctx.getSource()),
                                                                                                        ResourceLocationArgument
                                                                                                                        .getId(ctx, "entity"),
                                                                                                        false))
                                                                                        .then(Commands.argument(
                                                                                                        "hidenametag",
                                                                                                        BoolArgumentType.bool())
                                                                                                        .executes(ctx -> executeMorph(
                                                                                                                        ctx.getSource(),
                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                        ctx.getSource()),
                                                                                                                        ResourceLocationArgument
                                                                                                                                        .getId(ctx, "entity"),
                                                                                                                        BoolArgumentType.getBool(
                                                                                                                                        ctx,
                                                                                                                                        "hidenametag"))))
                                                                                        .then(Commands.argument(
                                                                                                        "target",
                                                                                                        EntityArgument.player())
                                                                                                        .executes(ctx -> executeMorph(
                                                                                                                        ctx.getSource(),
                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                        ctx,
                                                                                                                                        "target"),
                                                                                                                        ResourceLocationArgument
                                                                                                                                        .getId(ctx, "entity"),
                                                                                                                        false))
                                                                                                        .then(Commands.argument(
                                                                                                                        "hidenametag",
                                                                                                                        BoolArgumentType.bool())
                                                                                                                        .executes(ctx -> executeMorph(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                        ctx,
                                                                                                                                                        "target"),
                                                                                                                                        ResourceLocationArgument
                                                                                                                                                        .getId(ctx, "entity"),
                                                                                                                                        BoolArgumentType.getBool(
                                                                                                                                                        ctx,
                                                                                                                                                        "hidenametag")))))))
                                                        .then(Commands.literal("unmorph")
                                                                        .executes(ctx -> {
                                                                                setMorph(ctx.getSource(), CommandUtils
                                                                                                .getTargetPlayer(ctx
                                                                                                                .getSource()),
                                                                                                Optional.empty(),
                                                                                                false);
                                                                                return 1;
                                                                        })
                                                                        .then(Commands.argument("target",
                                                                                        EntityArgument.player())
                                                                                        .executes(ctx -> {
                                                                                                setMorph(ctx.getSource(),
                                                                                                                EntityArgument.getPlayer(
                                                                                                                                ctx,
                                                                                                                                "target"),
                                                                                                                Optional.empty(),
                                                                                                                false);
                                                                                                return 1;
                                                                                        })))
                                                        .then(Commands.literal("trails")
                                                                        .then(Commands.literal("equip")
                                                                                        .then(Commands.argument("trail",
                                                                                                        StringArgumentType
                                                                                                                        .string())
                                                                                                        .suggests(TRAIL_SUGGESTIONS)
                                                                                                        .executes(ctx -> executeEquipTrail(
                                                                                                                        ctx.getSource(),
                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                        ctx.getSource()),
                                                                                                                        StringArgumentType
                                                                                                                                        .getString(ctx, "trail"),
                                                                                                                        false))
                                                                                                        .then(Commands.argument(
                                                                                                                        "target",
                                                                                                                        EntityArgument.player())
                                                                                                                        .executes(ctx -> executeEquipTrail(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                        ctx,
                                                                                                                                                        "target"),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "trail"),
                                                                                                                                        false))
                                                                                                                        .then(Commands.argument(
                                                                                                                                        "persistent",
                                                                                                                                        BoolArgumentType.bool())
                                                                                                                                        .executes(ctx -> executeEquipTrail(
                                                                                                                                                        ctx.getSource(),
                                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                                        ctx,
                                                                                                                                                                        "target"),
                                                                                                                                                        StringArgumentType
                                                                                                                                                                        .getString(ctx, "trail"),
                                                                                                                                                        BoolArgumentType.getBool(
                                                                                                                                                                        ctx,
                                                                                                                                                                        "persistent")))))))
                                                                        .then(Commands.literal("unequip")
                                                                                        .then(Commands.argument("trail",
                                                                                                        StringArgumentType
                                                                                                                        .string())
                                                                                                        .suggests(TRAIL_SUGGESTIONS)
                                                                                                        .executes(ctx -> executeUnequipTrail(
                                                                                                                        ctx.getSource(),
                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                        ctx.getSource()),
                                                                                                                        StringArgumentType
                                                                                                                                        .getString(ctx, "trail")))
                                                                                                        .then(Commands.argument(
                                                                                                                        "target",
                                                                                                                        EntityArgument.player())
                                                                                                                        .executes(ctx -> executeUnequipTrail(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                        ctx,
                                                                                                                                                        "target"),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "trail"))))))
                                                                        .then(Commands.literal("list").executes(
                                                                                        ctx -> executeListTrails(ctx
                                                                                                        .getSource()))))
                                                        .then(Commands.literal("effects")
                                                                        .then(Commands.literal("equip")
                                                                                        .then(Commands.argument(
                                                                                                        "effect",
                                                                                                        StringArgumentType
                                                                                                                        .string())
                                                                                                        .suggests(EFFECT_SUGGESTIONS)
                                                                                                        .executes(ctx -> executeEquipEffect(
                                                                                                                        ctx.getSource(),
                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                        ctx.getSource()),
                                                                                                                        StringArgumentType
                                                                                                                                        .getString(ctx, "effect"),
                                                                                                                        false))
                                                                                                        .then(Commands.argument(
                                                                                                                        "target",
                                                                                                                        EntityArgument.player())
                                                                                                                        .executes(ctx -> executeEquipEffect(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                        ctx,
                                                                                                                                                        "target"),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "effect"),
                                                                                                                                        false))
                                                                                                                        .then(Commands.argument(
                                                                                                                                        "persistent",
                                                                                                                                        BoolArgumentType.bool())
                                                                                                                                        .executes(ctx -> executeEquipEffect(
                                                                                                                                                        ctx.getSource(),
                                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                                        ctx,
                                                                                                                                                                        "target"),
                                                                                                                                                        StringArgumentType
                                                                                                                                                                        .getString(ctx, "effect"),
                                                                                                                                                        BoolArgumentType.getBool(
                                                                                                                                                                        ctx,
                                                                                                                                                                        "persistent")))))))
                                                                        .then(Commands.literal("unequip")
                                                                                        .then(Commands.argument(
                                                                                                        "effect",
                                                                                                        StringArgumentType
                                                                                                                        .string())
                                                                                                        .suggests(EFFECT_SUGGESTIONS)
                                                                                                        .executes(ctx -> executeUnequipEffect(
                                                                                                                        ctx.getSource(),
                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                        ctx.getSource()),
                                                                                                                        StringArgumentType
                                                                                                                                        .getString(ctx, "effect")))
                                                                                                        .then(Commands.argument(
                                                                                                                        "target",
                                                                                                                        EntityArgument.player())
                                                                                                                        .executes(ctx -> executeUnequipEffect(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                        ctx,
                                                                                                                                                        "target"),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "effect"))))))
                                                                        .then(Commands.literal("list").executes(
                                                                                        ctx -> executeListEffects(ctx
                                                                                                        .getSource()))))
                                                        .then(Commands.literal("addons")
                                                                        .then(Commands.literal("list").executes(ctx -> {
                                                                                var addons = AddonRegistry
                                                                                                .getAllAddonIds();
                                                                                if (addons.isEmpty()) {
                                                                                        ctx.getSource().sendSuccess(
                                                                                                        () -> Component.literal(
                                                                                                                        "No addons registered"),
                                                                                                        false);
                                                                                } else {
                                                                                        ctx.getSource().sendSuccess(
                                                                                                        () -> Component.literal(
                                                                                                                        "Available addons: "
                                                                                                                                        + String.join(", ",
                                                                                                                                                        addons)),
                                                                                                        false);
                                                                                }
                                                                                return 1;
                                                                        }))
                                                                        .then(Commands.literal("equip")
                                                                                        .then(Commands.argument("addon",
                                                                                                        StringArgumentType
                                                                                                                        .string())
                                                                                                        .suggests(ADDON_SUGGESTIONS)
                                                                                                        .executes(ctx -> executeEquipAddon(
                                                                                                                        ctx.getSource(),
                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                        ctx.getSource()),
                                                                                                                        StringArgumentType
                                                                                                                                        .getString(ctx, "addon"),
                                                                                                                        false))
                                                                                                        .then(Commands.argument(
                                                                                                                        "target",
                                                                                                                        EntityArgument.player())
                                                                                                                        .executes(ctx -> executeEquipAddon(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                        ctx,
                                                                                                                                                        "target"),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "addon"),
                                                                                                                                        false))
                                                                                                                        .then(Commands.argument(
                                                                                                                                        "persistent",
                                                                                                                                        BoolArgumentType.bool())
                                                                                                                                        .executes(ctx -> executeEquipAddon(
                                                                                                                                                        ctx.getSource(),
                                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                                        ctx,
                                                                                                                                                                        "target"),
                                                                                                                                                        StringArgumentType
                                                                                                                                                                        .getString(ctx, "addon"),
                                                                                                                                                        BoolArgumentType.getBool(
                                                                                                                                                                        ctx,
                                                                                                                                                                        "persistent")))))))
                                                                        .then(Commands.literal("unequip")
                                                                                        .then(Commands.argument("addon",
                                                                                                        StringArgumentType
                                                                                                                        .string())
                                                                                                        .suggests(ADDON_SUGGESTIONS)
                                                                                                        .executes(ctx -> executeUnequipAddon(
                                                                                                                        ctx.getSource(),
                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                        ctx.getSource()),
                                                                                                                        StringArgumentType
                                                                                                                                        .getString(ctx, "addon")))
                                                                                                        .then(Commands.argument(
                                                                                                                        "target",
                                                                                                                        EntityArgument.player())
                                                                                                                        .executes(ctx -> executeUnequipAddon(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                        ctx,
                                                                                                                                                        "target"),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "addon"))))))
                                                                        .then(Commands.literal("clear")
                                                                                        .executes(ctx -> executeClearAddons(
                                                                                                        ctx.getSource(),
                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                        ctx.getSource())))
                                                                                        .then(Commands.argument(
                                                                                                        "target",
                                                                                                        EntityArgument.player())
                                                                                                        .executes(ctx -> executeClearAddons(
                                                                                                                        ctx.getSource(),
                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                        ctx,
                                                                                                                                        "target")))))
                                                                        .then(Commands.literal("tint")
                                                                                        .then(Commands.argument("addon",
                                                                                                        StringArgumentType
                                                                                                                        .string())
                                                                                                        .suggests(ADDON_SUGGESTIONS)
                                                                                                        .then(Commands.argument(
                                                                                                                        "color",
                                                                                                                        StringArgumentType
                                                                                                                                        .string())
                                                                                                                        .executes(ctx -> executeSetAddonTint(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                                        ctx.getSource()),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "addon"),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "color")))
                                                                                                                        .then(Commands.argument(
                                                                                                                                        "target",
                                                                                                                                        EntityArgument.player())
                                                                                                                                        .executes(ctx -> executeSetAddonTint(
                                                                                                                                                        ctx.getSource(),
                                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                                        ctx,
                                                                                                                                                                        "target"),
                                                                                                                                                        StringArgumentType
                                                                                                                                                                        .getString(ctx, "addon"),
                                                                                                                                                        StringArgumentType
                                                                                                                                                                        .getString(ctx, "color"))))))))
                                                        .then(Commands.literal("model")
                                                                        .then(Commands.literal("set")
                                                                                        .then(Commands.argument(
                                                                                                        "variant",
                                                                                                        StringArgumentType
                                                                                                                        .string())
                                                                                                        .suggests(MODEL_VARIANT_SUGGESTIONS)
                                                                                                        .executes(ctx -> executeSetModelVariant(
                                                                                                                        ctx.getSource(),
                                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                                        ctx.getSource()),
                                                                                                                        StringArgumentType
                                                                                                                                        .getString(ctx, "variant")))
                                                                                                        .then(Commands.argument(
                                                                                                                        "target",
                                                                                                                        EntityArgument.player())
                                                                                                                        .executes(ctx -> executeSetModelVariant(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                                        ctx,
                                                                                                                                                        "target"),
                                                                                                                                        StringArgumentType
                                                                                                                                                        .getString(ctx, "variant"))))))
                                                                        .then(Commands.literal("clear")
                                                                                        .executes(ctx -> executeClearModelVariant(
                                                                                                        ctx.getSource(),
                                                                                                        CommandUtils.getTargetPlayer(
                                                                                                                        ctx.getSource())))
                                                                                        .then(Commands.argument(
                                                                                                        "target",
                                                                                                        EntityArgument.player())
                                                                                                        .executes(ctx -> executeClearModelVariant(
                                                                                                                        ctx.getSource(),
                                                                                                                        EntityArgument.getPlayer(
                                                                                                                                        ctx,
                                                                                                                                        "target"))))))
                                                        .then(Commands.literal("reload")
                                                                        .executes(ctx -> executeReload(
                                                                                        ctx.getSource()))));
                }
        }

        private static int executeMorph(CommandSourceStack source, ServerPlayer target, ResourceLocation rl,
                        boolean hideNametag) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);

                if (type == null || type == EntityType.PLAYER) {
                        source.sendFailure(Component.literal("Invalid entity type: " + rl));
                        LOGGER.warn("Or, what. {}", rl);
                        return 0;
                }

                // Validate by creating a test entity
                var testEntity = type.create(target.serverLevel());
                if (testEntity == null) {
                        source.sendFailure(Component.literal("Failed to create entity: " + rl));
                        LOGGER.error("How did I?! Uuuughh! Critical failure creating test entity for type: {}", rl);
                        return 0;
                }

                try {
                        boolean isLiving = testEntity instanceof LivingEntity;

                        if (!isLiving) {
                                source.sendFailure(Component.literal("Entity must be a LivingEntity."));
                                LOGGER.warn("Is this the best physical representation you can manifest? {}", rl);
                                return 0;
                        }
                } finally {
                        // Always discard test entity to prevent leaks
                        try {
                                testEntity.discard();
                        } catch (Exception e) {
                                LOGGER.error("How did I?! Uuuughh! Failed to discard test entity for {}: {}", rl,
                                                e.getMessage());
                        }
                }

                setMorph(source, target, Optional.of(rl), hideNametag);
                LOGGER.debug("Here you go! {} morphed into {} (nametag hidden: {})",
                                target.getGameProfile().getName(), rl, hideNametag);
                return 1;
        }

        private static void setMorph(CommandSourceStack source, ServerPlayer target,
                        Optional<ResourceLocation> morphType,
                        boolean hideNametag) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return;
                }
                IMorph morph = DataUtils.getMorphData(target);
                if (morph == null) {
                        source.sendFailure(
                                        Component.literal("Or, what. Player " + target.getName().getString()
                                                        + " has no morph data!"));
                        LOGGER.error("Or, what. Cannot set morph for {} - morph data not present",
                                        target.getName().getString());
                        return;
                }

                // Optimization: Skip if already morphed to this entity
                if (morph.getEntityType().equals(morphType)) {
                        LOGGER.debug("Yeah? Well... {} is already morphed as {}, skipping unnecessary update",
                                        target.getGameProfile().getName(),
                                        morphType.map(ResourceLocation::toString).orElse("none"));
                        return;
                }

                morph.setEntityType(morphType);
                morph.setNametagHidden(hideNametag); // Set nametag visibility

                // Save to persistent NBT for death persistence
                if (morphType.isPresent()) {
                        DataUtils.getPersistentData(target).put(TwilightConstants.NBT_MORPH, morph.serialize());
                } else {
                        DataUtils.getPersistentData(target).remove(TwilightConstants.NBT_MORPH);
                        LOGGER.debug("Time to change! {} has been unmorphed", target.getGameProfile().getName());
                }

                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(target.getUUID(), morphType, hideNametag));
                target.refreshDimensions();

                // Send feedback only to admin/console (not when player targets self)
                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        if (morphType.isPresent()) {
                                String nametagStatus = hideNametag ? " (nametag hidden)" : " (nametag visible)";
                                source.sendSuccess(() -> Component.literal(
                                                "Morph '" + morphType.get() + "' set for "
                                                                + target.getGameProfile().getName() + nametagStatus),
                                                true);
                        } else {
                                source.sendSuccess(
                                                () -> Component.literal("Morph removed for "
                                                                + target.getGameProfile().getName()),
                                                true);
                        }
                }
        }

        private static int executeEquipTrail(CommandSourceStack source, ServerPlayer target, String trailId,
                        boolean persistent) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                // Validate trail ID length (DoS protection)
                if (trailId == null || trailId.length() > TwilightConfig.MAX_COSMETIC_ID_LENGTH.get()) {
                        source.sendFailure(Component.literal("Or, what. Trail ID too long (max "
                                        + TwilightConfig.MAX_COSMETIC_ID_LENGTH.get() + " characters)"));
                        LOGGER.warn("Or, what. Rejected oversized trail ID (length: {}, max: {})",
                                        trailId == null ? 0 : trailId.length(),
                                        TwilightConfig.MAX_COSMETIC_ID_LENGTH.get());
                        return 0;
                }

                // Validate trail type
                TrailType trailType = TrailType.fromId(trailId);
                if (trailType == null) {
                        source.sendFailure(Component.literal("Invalid trail type: " + trailId));
                        return 0;
                }

                var trails = DataUtils.getTrailsData(target);
                if (trails == null) {
                        source.sendFailure(
                                        Component.literal("Or, what. Player " + target.getName().getString()
                                                        + " has no trails data!"));
                        LOGGER.error("Or, what. Cannot equip trail for {} - trails data not present",
                                        target.getName().getString());
                        return 0;
                }
                // Optimization: Skip if trail is already active
                if (trails.isTrailActive(trailId)) {
                        LOGGER.debug("Yeah? Well... {} already has trail '{}' active, skipping unnecessary update",
                                        target.getGameProfile().getName(), trailId);
                        return 1;
                }

                // Activate trail with persistence control
                // persistent=true: persists through logout/death (for race mods and admin
                // grants)
                // persistent=false: Temporary admin preview (cleared on logout)
                ((TrailsData) trails).setActiveTrail(trailId, true, persistent);

                // Save to persistent NBT
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_TRAILS, trails.serialize());

                // Sync to all clients (we only sync active trails now)
                NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(target.getUUID(), trails.getActiveTrails()));

                // Send feedback only to admin/console (not when player targets self)
                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        String persistMode = persistent ? " (persistent)" : " (temporary)";
                        source.sendSuccess(
                                        () -> Component.literal(
                                                        "Trail '" + trailId + "' equipped for "
                                                                        + target.getGameProfile().getName()
                                                                        + persistMode),
                                        true);
                }

                return 1;
        }

        private static int executeListTrails(CommandSourceStack source) {
                // Admin command - list ALL available trail types
                String[] trailIds = new String[TrailType.values().length];
                int i = 0;
                for (TrailType type : TrailType.values()) {
                        trailIds[i++] = type.getId();
                }

                if (trailIds.length == 0) {
                        source.sendSuccess(() -> Component.literal("No trails registered"), false);
                } else {
                        source.sendSuccess(() -> Component.literal("Available trails: " + String.join(", ", trailIds)),
                                        false);
                }

                return 1;
        }

        private static int executeUnequipTrail(CommandSourceStack source, ServerPlayer target, String trailId) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                // Validate trail ID length (DoS protection)
                if (trailId == null || trailId.length() > TwilightConfig.MAX_COSMETIC_ID_LENGTH.get()) {
                        source.sendFailure(Component.literal("Or, what. Trail ID too long (max "
                                        + TwilightConfig.MAX_COSMETIC_ID_LENGTH.get() + " characters)"));
                        LOGGER.warn("Or, what. Rejected oversized trail ID (length: {}, max: {})",
                                        trailId == null ? 0 : trailId.length(),
                                        TwilightConfig.MAX_COSMETIC_ID_LENGTH.get());
                        return 0;
                }

                var trails = DataUtils.getTrailsData(target);
                if (trails == null) {
                        source.sendFailure(
                                        Component.literal("Or, what. Player " + target.getName().getString()
                                                        + " has no trails data!"));
                        LOGGER.error("Or, what. Cannot unequip trail for {} - trails data not present",
                                        target.getName().getString());
                        return 0;
                }
                // Admin command: Force unequip regardless of source (player selection or
                // external grant)
                ((TrailsData) trails).forceUnequipTrail(trailId);

                // Save to persistent NBT
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_TRAILS, trails.serialize());

                // Sync to all clients
                NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(target.getUUID(), trails.getActiveTrails()));

                // Send feedback only to admin/console (not when player targets self)
                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        source.sendSuccess(
                                        () -> Component
                                                        .literal("Trail '" + trailId + "' unequipped for "
                                                                        + target.getGameProfile().getName()),
                                        true);
                }

                return 1;
        }

        private static int executeEquipEffect(CommandSourceStack source, ServerPlayer target, String effectId,
                        boolean persistent) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                // Validate effect ID length (DoS protection)
                if (effectId == null || effectId.length() > TwilightConfig.MAX_COSMETIC_ID_LENGTH.get()) {
                        source.sendFailure(Component.literal("Or, what. Effect ID too long (max "
                                        + TwilightConfig.MAX_COSMETIC_ID_LENGTH.get() + " characters)"));
                        LOGGER.warn("Or, what. Rejected oversized effect ID (length: {}, max: {})",
                                        effectId == null ? 0 : effectId.length(),
                                        TwilightConfig.MAX_COSMETIC_ID_LENGTH.get());
                        return 0;
                }

                // Validate effect type
                EffectType effectType = EffectType.fromId(effectId);
                if (effectType == null) {
                        source.sendFailure(Component.literal("Invalid effect type: " + effectId));
                        LOGGER.warn("Or, what. Unknown effect type requested: {}", effectId);
                        return 0;
                }

                var effects = DataUtils.getEffectsData(target);
                if (effects == null) {
                        source.sendFailure(
                                        Component.literal("Or, what. Player " + target.getName().getString()
                                                        + " has no effects data!"));
                        LOGGER.error("Or, what. Cannot equip effect for {} - effects data not present",
                                        target.getName().getString());
                        return 0;
                }
                // Optimization: Skip if effect is already active
                if (effects.isEffectActive(effectId)) {
                        LOGGER.debug("Yeah? Well... {} already has effect '{}' active, skipping unnecessary update",
                                        target.getGameProfile().getName(), effectId);
                        return 1;
                }

                // Activate effect with persistence control
                // persistent=true: persists through logout/death (for race mods and admin
                // grants)
                // persistent=false: Temporary admin preview (cleared on logout)
                ((EffectsData) effects).setActiveEffect(effectId, true, persistent);

                // Save to persistent NBT
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_EFFECTS, effects.serialize());

                // Sync to all clients (we only sync active effects now)
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(target.getUUID(), effects.getActiveEffects()));

                // Send feedback only to admin/console (not when player targets self)
                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        String persistMode = persistent ? " (persistent)" : " (temporary)";
                        source.sendSuccess(() -> Component.literal(
                                        "Effect '" + effectId + "' equipped for " + target.getGameProfile().getName()
                                                        + persistMode),
                                        true);
                }

                return 1;
        }

        private static int executeUnequipEffect(CommandSourceStack source, ServerPlayer target, String effectId) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                // Validate effect ID length (DoS protection)
                if (effectId == null || effectId.length() > TwilightConfig.MAX_COSMETIC_ID_LENGTH.get()) {
                        source.sendFailure(Component.literal("Or, what. Effect ID too long (max "
                                        + TwilightConfig.MAX_COSMETIC_ID_LENGTH.get() + " characters)"));
                        LOGGER.warn("Or, what. Rejected oversized effect ID (length: {}, max: {})",
                                        effectId == null ? 0 : effectId.length(),
                                        TwilightConfig.MAX_COSMETIC_ID_LENGTH.get());
                        return 0;
                }

                var effects = DataUtils.getEffectsData(target);
                if (effects == null) {
                        source.sendFailure(
                                        Component.literal("Or, what. Player " + target.getName().getString()
                                                        + " has no effects data!"));
                        LOGGER.error("Or, what. Cannot unequip effect for {} - effects data not present",
                                        target.getName().getString());
                        return 0;
                }
                // Admin command: Force unequip regardless of source (player selection or
                // external grant)
                ((EffectsData) effects).forceUnequipEffect(effectId);

                // Save to persistent NBT
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_EFFECTS, effects.serialize());

                // Sync to all clients
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(target.getUUID(), effects.getActiveEffects()));

                // Send feedback only to admin/console (not when player targets self)
                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        source.sendSuccess(
                                        () -> Component
                                                        .literal("Effect '" + effectId + "' unequipped for "
                                                                        + target.getGameProfile().getName()),
                                        true);
                }

                return 1;
        }

        private static int executeListEffects(CommandSourceStack source) {
                // Admin command - list ALL available effect types
                String[] effectIds = new String[EffectType.values().length];
                int i = 0;
                for (EffectType type : EffectType.values()) {
                        effectIds[i++] = type.getId();
                }

                if (effectIds.length == 0) {
                        source.sendSuccess(() -> Component.literal("No effects registered"), false);
                } else {
                        source.sendSuccess(
                                        () -> Component.literal("Available effects: " + String.join(", ", effectIds)),
                                        false);
                }

                return 1;
        }

        private static int executeReload(CommandSourceStack source) {
                source.sendSuccess(() -> Component.literal("Reloading Twilight Lib..."), true);
                LOGGER.info("Hey, whatcha doing? Admin {} initiated reload command", source.getTextName());

                try {
                        // Reload supporter data from GitHub (force refresh to bypass cache)
                        source.sendSuccess(() -> Component.literal("Fetching supporter data from GitHub..."), false);
                        SupporterService.forceRefresh().thenRun(() -> {
                                source.sendSuccess(() -> Component.literal("✓ Supporter data reloaded successfully!"),
                                                false);
                                LOGGER.info("Gotcha! Now that was more sparkles. Supporter data reloaded via command");

                                // Re-sync cosmetics for all online players
                                var server = source.getServer();
                                if (server != null) {
                                        var playerList = server.getPlayerList();
                                        int syncedPlayers = 0;

                                        for (ServerPlayer player : playerList.getPlayers()) {
                                                syncedPlayers++;
                                                resyncPlayerCosmetics(player);
                                        }

                                        int finalCount = syncedPlayers;
                                        source.sendSuccess(
                                                        () -> Component.literal("✓ Re-synced cosmetics for "
                                                                        + finalCount + " online players"),
                                                        false);
                                        LOGGER.info("Time to change! Re-synced cosmetics for {} online players",
                                                        finalCount);
                                }
                        }).exceptionally(ex -> {
                                source.sendFailure(Component
                                                .literal("✗ Failed to reload supporter data: " + ex.getMessage()));
                                LOGGER.error("Dang! Failed to reload supporter data via command: {}", ex.getMessage());
                                return null;
                        });

                        // Clear and reinitialize entity cache
                        source.sendSuccess(() -> Component.literal("Refreshing entity cache..."), false);
                        synchronized (VALID_LIVING_ENTITIES) {
                                VALID_LIVING_ENTITIES.clear();
                                cacheInitialized = false;
                                cachedRegistrySize = 0;
                        }
                        if (source.getLevel() != null) {
                                initializeEntityCache(source.getLevel());
                                source.sendSuccess(
                                                () -> Component.literal(
                                                                "✓ Entity cache refreshed with "
                                                                                + VALID_LIVING_ENTITIES.size()
                                                                                + " living entities"),
                                                false);
                                LOGGER.info("THAT WAS AWESOME-AWESOME! Right? Entity cache refreshed via command");
                        }

                        source.sendSuccess(() -> Component.literal("Twilight Lib reload complete!"), true);
                        return 1;
                } catch (Exception e) {
                        source.sendFailure(Component.literal("Reload failed: " + e.getMessage()));
                        LOGGER.error("Oh, farn it! Reload command failed: {}", e.getMessage(), e);
                        return 0;
                }
        }

        private static void resyncPlayerCosmetics(ServerPlayer player) {
                mc.sayda.twilight_lib.cosmetics.CosmeticManager.resyncAll(player);
        }

        private static int executeEquipAddon(CommandSourceStack source, ServerPlayer target, String addonId,
                        boolean persistent) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                // Use exists() instead of hasAddon() - server doesn't need to check mod
                // requirements.
                // The client decides whether to render based on its own mod availability.
                // hasAddon() checks ModRequirement.shouldLoad() which fails on dedicated
                // servers
                // where client-side mods like CreRaces aren't installed.
                if (!AddonRegistry.exists(addonId)) {
                        source.sendFailure(Component.literal("Unknown addon: " + addonId));
                        LOGGER.warn("Or, what. Unknown addon requested: {}", addonId);
                        return 0;
                }

                var addons = DataUtils.getAddonsData(target);
                // Optimization: Skip if addon is already active
                if (addons.isAddonActive(addonId)) {
                        LOGGER.debug("Yeah? Well... {} already has addon '{}' active, skipping unnecessary update",
                                        target.getGameProfile().getName(), addonId);
                        return 1;
                }

                // Activate addon with persistence control
                // persistent=true: persists through logout/death (for race mods and admin
                // grants)
                // persistent=false: Temporary admin preview (cleared on logout)
                ((AddonsData) addons).setActiveAddon(addonId, true, persistent);
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_ADDONS, addons.serialize());
                // Sync to all clients
                NetworkHandler.sendAddonsToAll(
                                new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons(),
                                                addons.getExternalGrants(), addons.getAllAddonTints()));
                LOGGER.debug("Time to change! {} activated addon: {} (persistent: {})",
                                target.getGameProfile().getName(), addonId, persistent);

                // Only send feedback if source is NOT the target player (admin, command block,
                // console)
                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        String persistMode = persistent ? " (persistent)" : " (temporary)";
                        source.sendSuccess(
                                        () -> Component.literal(
                                                        "Addon '" + addonId + "' equipped for "
                                                                        + target.getGameProfile().getName()
                                                                        + persistMode),
                                        true);
                }
                return 1;
        }

        private static int executeUnequipAddon(CommandSourceStack source, ServerPlayer target, String addonId) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                var addons = DataUtils.getAddonsData(target);
                // Admin command: Force unequip regardless of source (player selection or
                // external grant)
                ((AddonsData) addons).forceUnequipAddon(addonId);
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_ADDONS, addons.serialize());
                // Sync to all clients
                NetworkHandler.sendAddonsToAll(
                                new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons(),
                                                addons.getExternalGrants(), addons.getAllAddonTints()));
                LOGGER.debug("Time to change! {} deactivated addon: {}", target.getGameProfile().getName(), addonId);

                // Only send feedback if source is NOT the target player (admin, command block,
                // console)
                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        source.sendSuccess(
                                        () -> Component
                                                        .literal("Addon '" + addonId + "' unequipped for "
                                                                        + target.getGameProfile().getName()),
                                        true);
                }
                return 1;
        }

        private static int executeClearAddons(CommandSourceStack source, ServerPlayer target) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                var addons = DataUtils.getAddonsData(target);
                // Admin command: Clear all active addons
                addons.clearActiveAddons();
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_ADDONS, addons.serialize());
                // Sync to all clients
                NetworkHandler.sendAddonsToAll(
                                new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons(),
                                                addons.getExternalGrants(), addons.getAllAddonTints()));
                LOGGER.debug("Time to change! Cleared all active addons for {}", target.getGameProfile().getName());

                // Only send feedback if source is NOT the target player (admin, command block,
                // console)
                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        source.sendSuccess(
                                        () -> Component.literal(
                                                        "All addons cleared for " + target.getGameProfile().getName()),
                                        true);
                }
                return 1;
        }

        private static int executeSetModelVariant(CommandSourceStack source, ServerPlayer target, String variant) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                // Validate variant (should be "steve" or "alex")
                String normalized = variant.toLowerCase();
                if (!normalized.equals("steve") && !normalized.equals("alex")) {
                        source.sendFailure(Component
                                        .literal("Invalid model variant: " + variant + ". Must be 'steve' or 'alex'."));
                        return 0;
                }

                IModelVariant modelVariant = DataUtils.getModelVariantData(target);

                // Optimization: Skip if already set to avoid unnecessary NBT writes and network
                // syncs
                if (modelVariant.hasCustomVariant() && modelVariant.getModelVariant().equals(normalized)) {
                        LOGGER.debug("Yeah? Well... {} already has model variant '{}', skipping unnecessary update",
                                        target.getGameProfile().getName(), normalized);
                        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                                source.sendSuccess(
                                                () -> Component.literal(
                                                                "Model variant '" + normalized + "' set for "
                                                                                + target.getGameProfile().getName()),
                                                true);
                        }
                        return 1;
                }

                try {
                        modelVariant.setModelVariant(normalized);
                        DataUtils.getPersistentData(target).put(TwilightConstants.NBT_MODEL_VARIANT,
                                        modelVariant.serialize());
                        NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(target.getUUID(), modelVariant));
                        LOGGER.debug("Time to change! {} changed model variant to {}",
                                        target.getGameProfile().getName(),
                                        normalized);
                } catch (IllegalArgumentException e) {
                        source.sendFailure(Component.literal("Error setting model variant: " + e.getMessage()));
                        LOGGER.error("How did I?! Uuuughh! Failed to set model variant for {}: {}",
                                        target.getGameProfile().getName(), e.getMessage());
                }

                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        source.sendSuccess(
                                        () -> Component
                                                        .literal("Model variant '" + normalized + "' set for "
                                                                        + target.getGameProfile().getName()),
                                        true);
                }
                return 1;
        }

        private static int executeClearModelVariant(CommandSourceStack source, ServerPlayer target) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                IModelVariant modelVariant = DataUtils.getModelVariantData(target);
                modelVariant.clearCustomVariant();
                DataUtils.getPersistentData(target).remove(TwilightConstants.NBT_MODEL_VARIANT);
                NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(target.getUUID(), modelVariant));
                LOGGER.debug("Time to change! {} cleared custom model variant", target.getGameProfile().getName());

                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                        source.sendSuccess(
                                        () -> Component.literal("Model variant cleared for "
                                                        + target.getGameProfile().getName()),
                                        true);
                }
                return 1;
        }

        private static int executeSetAddonTint(CommandSourceStack source, ServerPlayer target, String addonId,
                        String colorHex) {
                if (target == null) {
                        source.sendFailure(Component
                                        .literal("This command can only be used by players or must specify a target."));
                        return 0;
                }
                // Validate hex color length (DoS protection)
                if (colorHex == null || colorHex.length() > TwilightConfig.MAX_HEX_COLOR_LENGTH.get()) {
                        source.sendFailure(Component.literal("Or, what. Hex color string too long (max "
                                        + TwilightConfig.MAX_HEX_COLOR_LENGTH.get() + " characters)"));
                        LOGGER.warn("Or, what. Rejected oversized hex color string (length: {}, max: {})",
                                        colorHex == null ? 0 : colorHex.length(),
                                        TwilightConfig.MAX_HEX_COLOR_LENGTH.get());
                        return 0;
                }

                // Parse hex color (supports #RRGGBB or RRGGBB format)
                String hexString = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;

                // Validate hex format
                if (!hexString.matches("[0-9A-Fa-f]{6}")) {
                        source.sendFailure(Component
                                        .literal("Invalid color format. Use hex format: #RRGGBB or RRGGBB (e.g., #FF5733 or FF5733)"));
                        return 0;
                }

                try {
                        int color = Integer.parseInt(hexString, 16);

                        // Validate addon ID (unless using "all" option)
                        if (!addonId.equalsIgnoreCase("all") && !AddonRegistry.exists(addonId)) {
                                source.sendFailure(Component.literal("Unknown addon: " + addonId));
                                LOGGER.warn("Or, what. Unknown addon requested for tint: {}", addonId);
                                return 0;
                        }

                        var addons = DataUtils.getAddonsData(target);

                        if (addonId.equalsIgnoreCase("all")) {
                                // Bulk set all active addons
                                java.util.Set<String> activeAddons = addons.getActiveAddons();
                                if (activeAddons.isEmpty()) {
                                        source.sendFailure(Component.literal("No active addons to tint."));
                                        return 0;
                                }

                                for (String activeAddon : activeAddons) {
                                        addons.setAddonTint(activeAddon, color);
                                }

                                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_ADDONS,
                                                addons.serialize());
                                NetworkHandler.sendAddonsToAll(
                                                new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons(),
                                                                addons.getExternalGrants(), addons.getAllAddonTints()));
                                LOGGER.debug("Bulk tint set for {} active addons to #{}", activeAddons.size(),
                                                hexString.toUpperCase());

                                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                                        source.sendSuccess(
                                                        () -> Component.literal("Set tint #" + hexString.toUpperCase()
                                                                        + " for "
                                                                        + activeAddons.size() + " active addon(s) on "
                                                                        + target.getGameProfile().getName()),
                                                        true);
                                }
                        } else {
                                // Single addon tint
                                addons.setAddonTint(addonId, color);
                                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_ADDONS,
                                                addons.serialize());
                                NetworkHandler.sendAddonsToAll(
                                                new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons(),
                                                                addons.getExternalGrants(), addons.getAllAddonTints()));
                                LOGGER.debug("Oooooh! Pretty! {} set tint for addon '{}' to #{}",
                                                target.getGameProfile().getName(),
                                                addonId, hexString.toUpperCase());

                                if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
                                        source.sendSuccess(
                                                        () -> Component.literal("Tint color #" + hexString.toUpperCase()
                                                                        + " set for addon '" + addonId + "' on "
                                                                        + target.getGameProfile().getName()),
                                                        true);
                                }
                        }
                        return 1;
                } catch (NumberFormatException e) {
                        source.sendFailure(Component
                                        .literal("Invalid color format. Use hex format: #RRGGBB or RRGGBB (e.g., #FF5733 or FF5733)"));
                        return 0;
                }
        }
}
