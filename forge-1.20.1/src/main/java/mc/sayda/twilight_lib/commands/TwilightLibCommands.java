package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.capabilities.TrailsData;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import mc.sayda.twilight_lib.capabilities.ModelVariantProvider;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.commands.CommandUtils;
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
import mc.sayda.twilight_lib.supporter.SupporterData;
import mc.sayda.twilight_lib.supporter.SupporterService;
import net.minecraft.ChatFormatting;
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
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Admin commands for Twilight Lib - server operator tools for managing player cosmetics.
 *
 * <p><b>Permission Level</b>: Level 2 (operators only).
 * These commands allow admins to:
 * <ul>
 *   <li>Morph players into any LivingEntity</li>
 *   <li>Grant trails, addons, and effects to players</li>
 *   <li>Toggle persistent vs temporary cosmetic grants</li>
 *   <li>Reload supporter data from GitHub</li>
 * </ul>
 *
 * <p><b>Thread Safety</b>: Entity cache uses synchronized collections and volatile flags
 * to handle concurrent access from multiple command threads.
 *
 * <p><b>Performance Optimization</b>: Entity type suggestions are cached to avoid
 * creating test entities on every keystroke. Cache is invalidated when:
 * <ul>
 *   <li>Registry size changes (new mods loaded entities)</li>
 *   <li>Reload command is executed</li>
 * </ul>
 *
 * @author SaydaGames (mc_jojo3)
 * @version 1.0
 */
public class TwilightLibCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Cache of valid LivingEntity types for command suggestions.
     *
     * <p><b>Why cache?</b> Validating entity types requires creating test entities,
     * which is expensive (registry lookups, object allocation, NBT parsing).
     * Caching avoids creating test entities on every keystroke during command autocomplete.
     *
     * <p><b>Thread Safety</b>: Synchronized set allows safe concurrent reads/writes
     * from multiple command threads (operators typing commands simultaneously).
     *
     * <p><b>Invalidation</b>: Cache is cleared when registry size changes or on reload command.
     * This detects mods that dynamically register entities after startup.
     */
    private static final Set<ResourceLocation> VALID_LIVING_ENTITIES = Collections.synchronizedSet(new HashSet<>());

    /**
     * Flag indicating whether entity cache has been populated.
     *
     * <p><b>Thread Safety</b>: Volatile ensures visibility across threads.
     * Double-check locking pattern in {@link #initializeEntityCache} prevents duplicate initialization.
     */
    private static volatile boolean cacheInitialized = false;

    /**
     * Cached size of entity registry when cache was last built.
     *
     * <p><b>Why track size?</b> Detects when new entities are registered (e.g., by mods loaded after startup).
     * If current registry size != cached size, cache is invalidated and rebuilt.
     *
     * <p><b>Thread Safety</b>: Volatile for cross-thread visibility.
     */
    private static volatile int cachedRegistrySize = 0;

    // Suggestion provider for all entity types
    private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTIONS = (context, builder) -> {
        // Initialize or refresh cache if registry changed (synchronized to prevent race condition)
        var level = context.getSource().getLevel();
        synchronized (TwilightLibCommands.class) {
            if (!cacheInitialized || shouldRefreshCache(level)) {
                initializeEntityCache(level);
            }
        }
        return SharedSuggestionProvider.suggestResource(VALID_LIVING_ENTITIES.stream(), builder);
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
    private static final SuggestionProvider<CommandSourceStack> ADDON_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(AddonRegistry.getAllAddonIds(), builder);

    // Suggestion provider for model variants (steve/alex)
    private static final SuggestionProvider<CommandSourceStack> MODEL_VARIANT_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(new String[]{"steve", "alex"}, builder);

    /**
     * Check if entity registry has changed (new mods loaded entities).
     * @return true if cache should be refreshed
     */
    private static boolean shouldRefreshCache(net.minecraft.world.level.Level level) {
        int currentSize = BuiltInRegistries.ENTITY_TYPE.size();
        if (currentSize != cachedRegistrySize) {
            LOGGER.debug("Do not look into the eyes of a god, or fly into its ears. Entity registry changed: {} -> {}", cachedRegistrySize, currentSize);
            return true;
        }
        return false;
    }

    /**
     * Initialize cache of valid living entity types.
     * Called once on first command suggestion to avoid creating test entities on every keystroke.
     * Automatically invalidates and refreshes if registry size changes.
     * Thread-safe with double-check locking pattern.
     */
    private static synchronized void initializeEntityCache(net.minecraft.world.level.Level level) {
        // Double-check pattern to avoid repeated initialization
        if (cacheInitialized && cachedRegistrySize == BuiltInRegistries.ENTITY_TYPE.size()) {
            return;
        }

        // Clear cache if refreshing (no nested synchronization needed - method is already synchronized)
        if (cacheInitialized) {
            VALID_LIVING_ENTITIES.clear();
        }

        LOGGER.debug("There are holes in reality. And... in donuts. Initializing entity type cache...");
        for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            // Safety check: prevent unbounded cache growth
            if (VALID_LIVING_ENTITIES.size() >= TwilightConfig.MAX_ENTITY_CACHE_SIZE.get()) {
                LOGGER.warn("Oh no! Entity cache size limit reached ({}). Stopping cache initialization to prevent memory issues.", TwilightConfig.MAX_ENTITY_CACHE_SIZE.get());
                break;
            }

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
            if (type == null || type == EntityType.PLAYER) continue;

            // Create test entity with proper cleanup
            net.minecraft.world.entity.Entity testEntity = null;
            try {
                testEntity = type.create(level);
                if (testEntity instanceof LivingEntity) {
                    VALID_LIVING_ENTITIES.add(rl);
                }
            } catch (Exception e) {
                // Ignore entities that fail to create
                LOGGER.trace("How did I?! Uuuughh! Failed to create test entity for {}: {}", rl, e.getMessage());
            } finally {
                // Always discard test entity to prevent leak
                if (testEntity != null) {
                    try {
                        testEntity.discard();
                    } catch (Exception e) {
                        LOGGER.warn("How did I?! Uuuughh! Failed to discard test entity for {}: {}", rl, e.getMessage());
                    }
                }
            }
        }
        cachedRegistrySize = BuiltInRegistries.ENTITY_TYPE.size();
        cacheInitialized = true;
        LOGGER.debug("This should be fun. Fun~! Entity type cache initialized with {} living entities", VALID_LIVING_ENTITIES.size());
    }

    public static void registerCommands(RegisterCommandsEvent evt) {
        // Register base commands with all aliases
        for (String alias : new String[]{"twilightlib", "tl"}) {
            evt.getDispatcher().register(
                    Commands.literal(alias)
                            .requires(src -> src.hasPermission(2))

                            // morph <entity>...
                            .then(Commands.literal("morph")
                                    .then(Commands.argument("entity", ResourceLocationArgument.id())
                                            .suggests(ENTITY_SUGGESTIONS)
                                            .executes(ctx -> {
                                                ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                if (target == null) {
                                                    ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                    return 0;
                                                }
                                                return executeMorph(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "entity"), false);
                                            })
                                            // morph <entity> <hidenametag>
                                            .then(Commands.argument("hidenametag", BoolArgumentType.bool())
                                                    .executes(ctx -> {
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                        if (target == null) {
                                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                            return 0;
                                                        }
                                                        return executeMorph(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "entity"), BoolArgumentType.getBool(ctx, "hidenametag"));
                                                    })
                                            )
                                            // morph <entity> <target>
                                            .then(Commands.argument("target", EntityArgument.player())
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                        return executeMorph(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "entity"), false);
                                                    })
                                                    // morph <entity> <target> <hidenametag>
                                                    .then(Commands.argument("hidenametag", BoolArgumentType.bool())
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                return executeMorph(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "entity"), BoolArgumentType.getBool(ctx, "hidenametag"));
                                                            })
                                                    )
                                            )
                                    )
                            )

                            // unmorph
                            .then(Commands.literal("unmorph")
                                    .executes(ctx -> {
                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                        if (target == null) {
                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                            return 0;
                                        }
                                        setMorph(ctx.getSource(), target, Optional.empty(), false);
                                        return 1;
                                    })
                                    // unmorph <target>
                                    .then(Commands.argument("target", EntityArgument.player())
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                setMorph(ctx.getSource(), target, Optional.empty(), false);
                                                return 1;
                                            })
                                    )
                            )

                            // trails ...
                            .then(Commands.literal("trails")
                                    .then(Commands.literal("equip")
                                            .then(Commands.argument("trail", StringArgumentType.word())
                                                    .suggests(TRAIL_SUGGESTIONS)
                                                    .executes(ctx -> {
                                                        // Default: self, non-persistent
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                        if (target == null) {
                                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                            return 0;
                                                        }
                                                        return executeEquipTrail(ctx.getSource(), target, StringArgumentType.getString(ctx, "trail"), false);
                                                    })

                                                    // trails equip <trail> <target>
                                                    .then(Commands.argument("target", EntityArgument.player())
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                return executeEquipTrail(ctx.getSource(), target, StringArgumentType.getString(ctx, "trail"), false);
                                                            })

                                                            // trails equip <trail> <target> <persistent>
                                                            .then(Commands.argument("persistent", BoolArgumentType.bool())
                                                                    .executes(ctx -> {
                                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                        return executeEquipTrail(ctx.getSource(), target, StringArgumentType.getString(ctx, "trail"), BoolArgumentType.getBool(ctx, "persistent"));
                                                                    })
                                                            )
                                                    )
                                            )
                                    )

                                    // trails unequip <trail>
                                    .then(Commands.literal("unequip")
                                            .then(Commands.argument("trail", StringArgumentType.word())
                                                    .suggests(TRAIL_SUGGESTIONS)
                                                    .executes(ctx -> {
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                        if (target == null) {
                                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                            return 0;
                                                        }
                                                        return executeUnequipTrail(ctx.getSource(), target, StringArgumentType.getString(ctx, "trail"));
                                                    })

                                                    // trails unequip <trail> <target>
                                                    .then(Commands.argument("target", EntityArgument.player())
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                return executeUnequipTrail(ctx.getSource(), target, StringArgumentType.getString(ctx, "trail"));
                                                            })
                                                    )
                                            )
                                    )

                                    // trails list
                                    .then(Commands.literal("list")
                                            .executes(ctx -> executeListTrails(ctx.getSource()))
                                    )
                            )

                            // effects ...
                            .then(Commands.literal("effects")
                                    .then(Commands.literal("equip")
                                            .then(Commands.argument("effect", StringArgumentType.word())
                                                    .suggests(EFFECT_SUGGESTIONS)
                                                    .executes(ctx -> {
                                                        // Default: self, non-persistent
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                        if (target == null) {
                                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                            return 0;
                                                        }
                                                        return executeEquipEffect(ctx.getSource(), target, StringArgumentType.getString(ctx, "effect"), false);
                                                    })

                                                    // effects equip <effect> <target>
                                                    .then(Commands.argument("target", EntityArgument.player())
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                return executeEquipEffect(ctx.getSource(), target, StringArgumentType.getString(ctx, "effect"), false);
                                                            })

                                                            // effects equip <effect> <target> <persistent>
                                                            .then(Commands.argument("persistent", BoolArgumentType.bool())
                                                                    .executes(ctx -> {
                                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                        return executeEquipEffect(ctx.getSource(), target, StringArgumentType.getString(ctx, "effect"), BoolArgumentType.getBool(ctx, "persistent"));
                                                                    })
                                                            )
                                                    )
                                            )
                                    )

                                    // effects unequip <effect>
                                    .then(Commands.literal("unequip")
                                            .then(Commands.argument("effect", StringArgumentType.word())
                                                    .suggests(EFFECT_SUGGESTIONS)
                                                    .executes(ctx -> {
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                        if (target == null) {
                                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                            return 0;
                                                        }
                                                        return executeUnequipEffect(ctx.getSource(), target, StringArgumentType.getString(ctx, "effect"));
                                                    })

                                                    // effects unequip <effect> <target>
                                                    .then(Commands.argument("target", EntityArgument.player())
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                return executeUnequipEffect(ctx.getSource(), target, StringArgumentType.getString(ctx, "effect"));
                                                            })
                                                    )
                                            )
                                    )

                                    // effects list
                                    .then(Commands.literal("list")
                                            .executes(ctx -> executeListEffects(ctx.getSource()))
                                    )
                            )

                            // addons ...
                            .then(Commands.literal("addons")
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
                                    .then(Commands.literal("equip")
                                            .then(Commands.argument("addonId", StringArgumentType.string())
                                                    .suggests(ADDON_SUGGESTIONS)
                                                    .executes(ctx -> {
                                                        String addonId = ctx.getArgument("addonId", String.class);
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                        if (target == null) {
                                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                            return 0;
                                                        }
                                                        return executeEquipAddon(ctx.getSource(), target, addonId, false);
                                                    })
                                                    .then(Commands.argument("target", EntityArgument.player())
                                                            .executes(ctx -> {
                                                                String addonId = ctx.getArgument("addonId", String.class);
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                return executeEquipAddon(ctx.getSource(), target, addonId, false);
                                                            })
                                                            .then(Commands.argument("persistent", BoolArgumentType.bool())
                                                                    .executes(ctx -> {
                                                                        String addonId = ctx.getArgument("addonId", String.class);
                                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                        boolean persistent = BoolArgumentType.getBool(ctx, "persistent");
                                                                        return executeEquipAddon(ctx.getSource(), target, addonId, persistent);
                                                                    })
                                                            )
                                                    )
                                            )
                                    )
                                    .then(Commands.literal("unequip")
                                            .then(Commands.argument("addonId", StringArgumentType.string())
                                                    .suggests(ADDON_SUGGESTIONS)
                                                    .executes(ctx -> {
                                                        String addonId = ctx.getArgument("addonId", String.class);
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
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
                                                            })
                                                    )
                                            )
                                    )
                                    .then(Commands.literal("clear")
                                            .executes(ctx -> {
                                                ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
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
                                                    })
                                            )
                                    )
                            )

                            // model ...
                            .then(Commands.literal("model")
                                    .then(Commands.literal("set")
                                            .then(Commands.argument("variant", StringArgumentType.word())
                                                    .suggests(MODEL_VARIANT_SUGGESTIONS)
                                                    .executes(ctx -> {
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                        if (target == null) {
                                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                            return 0;
                                                        }
                                                        return executeSetModelVariant(ctx.getSource(), target, StringArgumentType.getString(ctx, "variant"));
                                                    })
                                                    // model set <variant> <target>
                                                    .then(Commands.argument("target", EntityArgument.player())
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                return executeSetModelVariant(ctx.getSource(), target, StringArgumentType.getString(ctx, "variant"));
                                                            })
                                                    )
                                            )
                                    )
                                    .then(Commands.literal("clear")
                                            .executes(ctx -> {
                                                ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                if (target == null) {
                                                    ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                    return 0;
                                                }
                                                return executeClearModelVariant(ctx.getSource(), target);
                                            })
                                            // model clear <target>
                                            .then(Commands.argument("target", EntityArgument.player())
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                        return executeClearModelVariant(ctx.getSource(), target);
                                                    })
                                            )
                                    )
                            )

                            // reload
                            .then(Commands.literal("reload")
                                    .executes(ctx -> executeReload(ctx.getSource()))
                            )
            );
        }
    }

    private static int executeMorph(CommandSourceStack source, ServerPlayer target, ResourceLocation rl, boolean hideNametag) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);

        if (type == null || type == EntityType.PLAYER) {
            source.sendFailure(Component.literal("Invalid entity type: " + rl));
            LOGGER.warn("There is another reality inside all of us. Maybe not this entity though: {}", rl);
            return 0;
        }

        // Validate by creating a test entity
        var testEntity = type.create(target.serverLevel());
        if (testEntity == null) {
            source.sendFailure(Component.literal("Failed to create entity: " + rl));
            LOGGER.error("Oh, dung beetles! Critical failure creating test entity for type: {}", rl);
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
                LOGGER.error("How did I?! Uuuughh! Failed to discard test entity for {}: {}", rl, e.getMessage());
            }
        }

        setMorph(source, target, Optional.of(rl), hideNametag);
        LOGGER.debug("Want to see something neat? {} morphed into {} (nametag hidden: {})", target.getGameProfile().getName(), rl, hideNametag);
        return 1;
    }

    private static void setMorph(CommandSourceStack source, ServerPlayer target, Optional<ResourceLocation> morph, boolean hideNametag) {
        LazyOptional<IMorph> cap = target.getCapability(MorphProvider.MORPH_CAP);
        cap.ifPresent(m -> {
            // Optimization: Skip if already morphed to this entity
            if (m.getEntityType().equals(morph)) {
                LOGGER.debug("Yeah? Well... {} is already morphed as {}, skipping unnecessary update",
                    target.getGameProfile().getName(), morph.map(ResourceLocation::toString).orElse("none"));
                return;
            }

            m.setEntityType(morph);
            m.setNametagHidden(hideNametag); // Set nametag visibility

            // Save to persistent NBT for death persistence
            if (morph.isPresent()) {
                target.getPersistentData().put(TwilightConstants.NBT_MORPH, m.serialize());
            } else {
                target.getPersistentData().remove(TwilightConstants.NBT_MORPH);
                LOGGER.debug("Paradigm shift time! {} has been unmorphed", target.getGameProfile().getName());
            }

            NetworkHandler.sendMorphToAll(SyncMorphPacket.of(target.getUUID(), morph, hideNametag));
            target.refreshDimensions();
        });

        // Send feedback only to admin/console (not when player targets self)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            if (morph.isPresent()) {
                String nametagStatus = hideNametag ? " (nametag hidden)" : " (nametag visible)";
                source.sendSuccess(() -> Component.literal("Morph '" + morph.get() + "' set for " + target.getGameProfile().getName() + nametagStatus), true);
            } else {
                source.sendSuccess(() -> Component.literal("Morph removed for " + target.getGameProfile().getName()), true);
            }
        }
    }

    private static int executeEquipTrail(CommandSourceStack source, ServerPlayer target, String trailId, boolean persistent) {
        // Validate trail type
        TrailType trailType = TrailType.fromId(trailId);
        if (trailType == null) {
            source.sendFailure(Component.literal("Invalid trail type: " + trailId));
            return 0;
        }

        target.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            // Optimization: Skip if trail is already active
            if (trails.isTrailActive(trailId)) {
                LOGGER.debug("Yeah? Well... {} already has trail '{}' active, skipping unnecessary update",
                    target.getGameProfile().getName(), trailId);
                return;
            }

            // Activate trail with persistence control
            // persistent=true: persists through logout/death (for race mods and admin grants)
            // persistent=false: Temporary admin preview (cleared on logout)
            ((TrailsData) trails).setActiveTrail(trailId, true, persistent);

            // Save to persistent NBT
            target.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());

            // Sync to all clients (we only sync active trails now)
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(target.getUUID(), trails.getActiveTrails()));
        });

        // Send feedback only to admin/console (not when player targets self)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            String persistMode = persistent ? " (persistent)" : " (temporary)";
            source.sendSuccess(() -> Component.literal("Trail '" + trailId + "' equipped for " + target.getGameProfile().getName() + persistMode), true);
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
            source.sendSuccess(() -> Component.literal("Available trails: " + String.join(", ", trailIds)), false);
        }

        return 1;
    }

    private static int executeUnequipTrail(CommandSourceStack source, ServerPlayer target, String trailId) {
        target.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            // Admin command: Force unequip regardless of source (player selection or external grant)
            ((TrailsData) trails).forceUnequipTrail(trailId);

            // Save to persistent NBT
            target.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());

            // Sync to all clients
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(target.getUUID(), trails.getActiveTrails()));
        });

        // Send feedback only to admin/console (not when player targets self)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            source.sendSuccess(() -> Component.literal("Trail '" + trailId + "' unequipped for " + target.getGameProfile().getName()), true);
        }

        return 1;
    }

    private static int executeEquipEffect(CommandSourceStack source, ServerPlayer target, String effectId, boolean persistent) {
        target.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            // Optimization: Skip if effect is already active
            if (effects.isEffectActive(effectId)) {
                LOGGER.debug("Yeah? Well... {} already has effect '{}' active, skipping unnecessary update",
                    target.getGameProfile().getName(), effectId);
                return;
            }

            // Activate effect with persistence control
            // persistent=true: persists through logout/death (for race mods and admin grants)
            // persistent=false: Temporary admin preview (cleared on logout)
            ((EffectsData) effects).setActiveEffect(effectId, true, persistent);

            // Save to persistent NBT
            target.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());

            // Sync to all clients (we only sync active effects now)
            NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(target.getUUID(), effects.getActiveEffects()));
        });

        // Send feedback only to admin/console (not when player targets self)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            String persistMode = persistent ? " (persistent)" : " (temporary)";
            source.sendSuccess(() -> Component.literal("Effect '" + effectId + "' equipped for " + target.getGameProfile().getName() + persistMode), true);
        }

        return 1;
    }

    private static int executeUnequipEffect(CommandSourceStack source, ServerPlayer target, String effectId) {
        target.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            // Admin command: Force unequip regardless of source (player selection or external grant)
            ((EffectsData) effects).forceUnequipEffect(effectId);

            // Save to persistent NBT
            target.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());

            // Sync to all clients
            NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(target.getUUID(), effects.getActiveEffects()));
        });

        // Send feedback only to admin/console (not when player targets self)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            source.sendSuccess(() -> Component.literal("Effect '" + effectId + "' unequipped for " + target.getGameProfile().getName()), true);
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
            source.sendSuccess(() -> Component.literal("Available effects: " + String.join(", ", effectIds)), false);
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
                source.sendSuccess(() -> Component.literal("✓ Supporter data reloaded successfully!"), false);
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
                    source.sendSuccess(() -> Component.literal("✓ Re-synced cosmetics for " + finalCount + " online players"), false);
                    LOGGER.info("Time to change! Re-synced cosmetics for {} online players", finalCount);
                }
            }).exceptionally(ex -> {
                source.sendFailure(Component.literal("✗ Failed to reload supporter data: " + ex.getMessage()));
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
                source.sendSuccess(() -> Component.literal("✓ Entity cache refreshed with " + VALID_LIVING_ENTITIES.size() + " living entities"), false);
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

    /**
     * Re-sync a player's cosmetics based on current supporter data.
     * This mirrors the logic from TwilightLib.onPlayerLogin but for existing players.
     */
    private static void resyncPlayerCosmetics(ServerPlayer player) {
        String uuid = player.getStringUUID();
        Optional<SupporterData> supporterData = SupporterService.getSupporterData(uuid);

        // Sync morph (independent of supporter status - set by commands)
        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(player.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                player.refreshDimensions();
                player.getPersistentData().put(TwilightConstants.NBT_MORPH, morph.serialize());
                LOGGER.debug("Time to change! Re-synced morph {} for {}", rl, player.getGameProfile().getName());
            });
        });

        if (supporterData.isPresent()) {
            SupporterData data = supporterData.get();
            Set<String> allTrails = data.getAllTrails();
            Set<String> allAddons = data.getAllAddons();
            Set<String> allEffects = data.getAllEffects();

            // Sync trails
            player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                Set<String> currentOwned = new HashSet<>(trails.getTrails());
                TrailsData trailsData = (TrailsData) trails;

                // Remove trails no longer granted
                for (String trail : currentOwned) {
                    if (!allTrails.contains(trail)) {
                        trailsData.removeTrailOwnership(trail);
                    }
                }

                // Add newly granted trails
                for (String trail : allTrails) {
                    if (!currentOwned.contains(trail)) {
                        trails.addTrail(trail);
                    }
                }

                player.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());
                if (!trails.getActiveTrails().isEmpty()) {
                    NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.getActiveTrails()));
                }
            });

            // Sync addons
            player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                Set<String> currentOwned = new HashSet<>(addons.getAddons());
                AddonsData addonsData = (AddonsData) addons;

                // Remove addons no longer granted
                for (String addon : currentOwned) {
                    if (!allAddons.contains(addon)) {
                        addonsData.removeAddonOwnership(addon);
                    }
                }

                // Add newly granted addons
                for (String addon : allAddons) {
                    if (!currentOwned.contains(addon)) {
                        addons.addAddon(addon);
                    }
                }

                player.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());
                NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons()));
            });

            // Sync effects
            player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                Set<String> currentOwned = new HashSet<>(effects.getEffects());
                EffectsData effectsData = (EffectsData) effects;

                // Remove effects no longer granted
                for (String effect : currentOwned) {
                    if (!allEffects.contains(effect)) {
                        effectsData.removeEffectOwnership(effect);
                    }
                }

                // Add newly granted effects
                for (String effect : allEffects) {
                    if (!currentOwned.contains(effect)) {
                        effects.addEffect(effect);
                    }
                }

                player.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());
                if (!effects.getActiveEffects().isEmpty()) {
                    NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects()));
                }
            });

            LOGGER.debug("Time to change! Re-synced {} trails, {} addons, {} effects for {}",
                allTrails.size(), allAddons.size(), allEffects.size(), player.getGameProfile().getName());
        }

        // Sync model variant (independent of supporter status - set by commands)
        player.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(player.getUUID(), modelVariant));
            LOGGER.debug("Time to change! Re-synced model variant for {}", player.getGameProfile().getName());
        });
    }

    private static int executeEquipAddon(CommandSourceStack source, ServerPlayer target, String addonId, boolean persistent) {
        if (!AddonRegistry.hasAddon(addonId)) {
            source.sendFailure(Component.literal("Unknown addon: " + addonId));
            LOGGER.warn("Or, what. Unknown addon requested: {}", addonId);
            return 0;
        }

        target.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            // Optimization: Skip if addon is already active
            if (addons.isAddonActive(addonId)) {
                LOGGER.debug("Yeah? Well... {} already has addon '{}' active, skipping unnecessary update",
                    target.getGameProfile().getName(), addonId);
                return;
            }

            // Activate addon with persistence control
            // persistent=true: persists through logout/death (for race mods and admin grants)
            // persistent=false: Temporary admin preview (cleared on logout)
            ((AddonsData) addons).setActiveAddon(addonId, true, persistent);
            target.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());
            // Sync to all clients
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons()));
            LOGGER.debug("Changed your mind about me yet? {} activated addon: {} (persistent: {})", target.getGameProfile().getName(), addonId, persistent);
        });

        // Only send feedback if source is NOT the target player (admin, command block, console)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            String persistMode = persistent ? " (persistent)" : " (temporary)";
            source.sendSuccess(() -> Component.literal("Addon '" + addonId + "' equipped for " + target.getGameProfile().getName() + persistMode), true);
        }
        return 1;
    }

    private static int executeUnequipAddon(CommandSourceStack source, ServerPlayer target, String addonId) {
        target.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            // Admin command: Force unequip regardless of source (player selection or external grant)
            ((AddonsData) addons).forceUnequipAddon(addonId);
            target.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());
            // Sync to all clients
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons()));
            LOGGER.debug("It's so random! {} deactivated addon: {}", target.getGameProfile().getName(), addonId);
        });

        // Only send feedback if source is NOT the target player (admin, command block, console)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            source.sendSuccess(() -> Component.literal("Addon '" + addonId + "' unequipped for " + target.getGameProfile().getName()), true);
        }
        return 1;
    }

    private static int executeClearAddons(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            // Admin command: Clear all active addons
            addons.clearActiveAddons();
            target.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());
            // Sync to all clients
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons()));
            LOGGER.debug("Dusk and dawn are the same. Cleared all active addons for {}", target.getGameProfile().getName());
        });

        // Only send feedback if source is NOT the target player (admin, command block, console)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            source.sendSuccess(() -> Component.literal("All addons cleared for " + target.getGameProfile().getName()), true);
        }
        return 1;
    }

    private static int executeSetModelVariant(CommandSourceStack source, ServerPlayer target, String variant) {
        // Validate variant (should be "steve" or "alex")
        String normalized = variant.toLowerCase();
        if (!normalized.equals("steve") && !normalized.equals("alex")) {
            source.sendFailure(Component.literal("Invalid model variant: " + variant + ". Must be 'steve' or 'alex'."));
            return 0;
        }

        target.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
            // Optimization: Skip if already set to avoid unnecessary NBT writes and network syncs
            if (modelVariant.hasCustomVariant() && modelVariant.getModelVariant().equals(normalized)) {
                LOGGER.debug("Yeah? Well... {} already has model variant '{}', skipping unnecessary update", target.getGameProfile().getName(), normalized);
                return;
            }

            try {
                modelVariant.setModelVariant(normalized);

                // Save to persistent NBT
                target.getPersistentData().put(TwilightConstants.NBT_MODEL_VARIANT, modelVariant.serialize());

                // Sync to all clients
                NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(target.getUUID(), modelVariant));

                LOGGER.debug("Shape-shifter! {} changed model variant to {}", target.getGameProfile().getName(), normalized);
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("Error setting model variant: " + e.getMessage()));
                LOGGER.error("Oh no! Failed to set model variant for {}: {}", target.getGameProfile().getName(), e.getMessage());
            }
        });

        // Send feedback only to admin/console (not when player targets self)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            source.sendSuccess(() -> Component.literal("Model variant '" + normalized + "' set for " + target.getGameProfile().getName()), true);
        }

        return 1;
    }

    private static int executeClearModelVariant(CommandSourceStack source, ServerPlayer target) {
        target.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
            modelVariant.clearCustomVariant();

            // Remove from persistent NBT
            target.getPersistentData().remove(TwilightConstants.NBT_MODEL_VARIANT);

            // Sync to all clients
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(target.getUUID(), modelVariant));

            LOGGER.debug("Back to normal! {} cleared custom model variant", target.getGameProfile().getName());
        });

        // Send feedback only to admin/console (not when player targets self)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            source.sendSuccess(() -> Component.literal("Model variant cleared for " + target.getGameProfile().getName()), true);
        }

        return 1;
    }
}
