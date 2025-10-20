package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.addon.AddonRegistry;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.commands.CommandUtils;
import mc.sayda.twilight_lib.cosmetics.TrailType;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncMorphPacket;
import mc.sayda.twilight_lib.network.SyncTrailsPacket;
import mc.sayda.twilight_lib.network.SyncEffectsPacket;
import mc.sayda.twilight_lib.TwilightConstants;
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

public class TwilightLibCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Cache of valid living entity types to avoid creating test entities repeatedly
    // Note: Cache is invalidated on world unload to detect dynamically registered entities
    // Thread-safe: Synchronized set for concurrent command access
    private static final Set<ResourceLocation> VALID_LIVING_ENTITIES = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean cacheInitialized = false;
    private static volatile int cachedRegistrySize = 0;

    // Suggestion provider for all entity types
    private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTIONS = (context, builder) -> {
        // Initialize or refresh cache if registry changed
        var level = context.getSource().getLevel();
        if (!cacheInitialized || shouldRefreshCache(level)) {
            initializeEntityCache(level);
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
        // For now, only respawn_twilight
        builder.suggest("respawn_twilight");
        return builder.buildFuture();
    };

    // Suggestion provider for addon types
    private static final SuggestionProvider<CommandSourceStack> ADDON_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggest(AddonRegistry.getAllAddonIds(), builder);

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

        synchronized (VALID_LIVING_ENTITIES) {
            // Clear cache if refreshing
            if (cacheInitialized) {
                VALID_LIVING_ENTITIES.clear();
            }

            LOGGER.debug("There are holes in reality. And... in donuts. Initializing entity type cache...");
            for (ResourceLocation rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
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
                    LOGGER.trace("Failed to create test entity for {}: {}", rl, e.getMessage());
                } finally {
                    // Always discard test entity to prevent leak
                    if (testEntity != null) {
                        testEntity.discard();
                    }
                }
            }
            cachedRegistrySize = BuiltInRegistries.ENTITY_TYPE.size();
            cacheInitialized = true;
            LOGGER.debug("This should be fun. Fun~! Entity type cache initialized with {} living entities", VALID_LIVING_ENTITIES.size());
        }
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
                                                return executeMorph(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "entity"));
                                            })
                                            // morph <entity> <target>
                                            .then(Commands.argument("target", EntityArgument.player())
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                        return executeMorph(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "entity"));
                                                    })
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
                                        setMorph(ctx.getSource(), target, Optional.empty());
                                        return 1;
                                    })
                                    // unmorph <target>
                                    .then(Commands.argument("target", EntityArgument.player())
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                setMorph(ctx.getSource(), target, Optional.empty());
                                                return 1;
                                            })
                                    )
                            )

                            // trail ...
                            .then(Commands.literal("trail")
                                    .then(Commands.literal("set")
                                            .then(Commands.argument("trail", StringArgumentType.word())
                                                    .suggests(TRAIL_SUGGESTIONS)
                                                    .executes(ctx -> {
                                                        // Default: self, non-persistent
                                                        ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                        if (target == null) {
                                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                            return 0;
                                                        }
                                                        return executeSetTrail(ctx.getSource(), target, StringArgumentType.getString(ctx, "trail"), false);
                                                    })

                                                    // trail <trail> <target>
                                                    .then(Commands.argument("target", EntityArgument.player())
                                                            .executes(ctx -> {
                                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                return executeSetTrail(ctx.getSource(), target, StringArgumentType.getString(ctx, "trail"), false);
                                                            })

                                                            // trail <trail> <target> <persistent>
                                                            .then(Commands.argument("persistent", BoolArgumentType.bool())
                                                                    .executes(ctx -> {
                                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                                        return executeSetTrail(ctx.getSource(), target, StringArgumentType.getString(ctx, "trail"), BoolArgumentType.getBool(ctx, "persistent"));
                                                                    })
                                                            )
                                                    )
                                            )
                                    )

                                    // trail toggle
                                    .then(Commands.literal("toggle")
                                            .executes(ctx -> {
                                                ServerPlayer target = CommandUtils.getTargetPlayer(ctx.getSource());
                                                if (target == null) {
                                                    ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                    return 0;
                                                }
                                                return executeToggleTrail(ctx.getSource(), target);
                                            })
                                            // trail toggle <target>
                                            .then(Commands.argument("target", EntityArgument.player())
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                        return executeToggleTrail(ctx.getSource(), target);
                                                    })
                                            )
                                    )

                                    // trail list
                                    .then(Commands.literal("list")
                                            .executes(ctx -> executeListTrails(ctx.getSource(), null))
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

                            // reload
                            .then(Commands.literal("reload")
                                    .executes(ctx -> executeReload(ctx.getSource()))
                            )
            );
        }
    }

    private static int executeMorph(CommandSourceStack source, ServerPlayer target, ResourceLocation rl) {
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
                LOGGER.warn("Is this the best physical representation you can manifest? It completely lacks zazz! {}", rl);
                return 0;
            }
        } finally {
            // Always discard test entity to prevent leaks
            testEntity.discard();
        }

        setMorph(source, target, Optional.of(rl));
        LOGGER.debug("Want to see something neat? {} morphed into {}", target.getGameProfile().getName(), rl);
        return 1;
    }

    private static void setMorph(CommandSourceStack source, ServerPlayer target, Optional<ResourceLocation> morph) {
        LazyOptional<IMorph> cap = target.getCapability(MorphProvider.MORPH_CAP);
        cap.ifPresent(m -> {
            m.setEntityType(morph);

            // Save to persistent NBT for death persistence
            if (morph.isPresent()) {
                target.getPersistentData().put(TwilightConstants.NBT_MORPH, m.serialize());
            } else {
                target.getPersistentData().remove(TwilightConstants.NBT_MORPH);
                LOGGER.debug("Paradigm shift time! {} has been unmorphed", target.getGameProfile().getName());
            }

            NetworkHandler.sendMorphToAll(SyncMorphPacket.of(target.getUUID(), morph.orElse(null)));
            target.refreshDimensions();
        });

        // Send feedback only to admin/console (not when player targets self via CreRaces)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            if (morph.isPresent()) {
                source.sendSuccess(() -> Component.literal("Morph '" + morph.get() + "' set for " + target.getGameProfile().getName()), true);
            } else {
                source.sendSuccess(() -> Component.literal("Morph removed for " + target.getGameProfile().getName()), true);
            }
        }
    }

    private static int executeSetTrail(CommandSourceStack source, ServerPlayer target, String trailId, boolean persistent) {
        // Validate trail type
        TrailType trailType = TrailType.fromId(trailId);
        if (trailType == null) {
            source.sendFailure(Component.literal("Invalid trail type: " + trailId));
            return 0;
        }

        target.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            // Use forceSetActiveTrail() with persistence control
            // persistent=true: CreRaces race attributes (survive logout/death)
            // persistent=false: Temporary admin preview (cleared on logout)
            ((mc.sayda.twilight_lib.capabilities.TrailsData) trails).forceSetActiveTrail(trailId, persistent);
            trails.setTrailEnabled(true);

            // Save to persistent NBT
            target.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());

            // Sync to all clients
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(target.getUUID(), trails.serialize()));
        });

        // Send feedback only to admin/console (not when player targets self via CreRaces)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            String persistMode = persistent ? " (persistent)" : " (temporary)";
            source.sendSuccess(() -> Component.literal("Trail '" + trailId + "' set for " + target.getGameProfile().getName() + persistMode), true);
        }

        return 1;
    }

    private static int executeListTrails(CommandSourceStack source, ServerPlayer target) {
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

    private static int executeToggleTrail(CommandSourceStack source, ServerPlayer target) {
        final boolean[] newState = {false};

        target.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            // Toggle trail enabled state
            newState[0] = !trails.isTrailEnabled();
            trails.setTrailEnabled(newState[0]);

            // Save to persistent NBT
            target.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());

            // Sync to all clients
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(target.getUUID(), trails.serialize()));

            LOGGER.debug("Paradigm shift time! Trail {} for {}",
                    newState[0] ? "enabled" : "disabled",
                    target.getGameProfile().getName());
        });

        // Send feedback only to admin/console (not when player targets self via CreRaces)
        String status = newState[0] ? "enabled" : "disabled";
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            source.sendSuccess(() -> Component.literal("Trail " + status + " for " + target.getGameProfile().getName()), true);
        }

        return 1;
    }

    private static int executeEquipEffect(CommandSourceStack source, ServerPlayer target, String effectId, boolean persistent) {
        target.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            // Activate effect with persistence control
            // persistent=true: CreRaces race attributes (survive logout/death)
            // persistent=false: Temporary admin preview (cleared on logout)
            ((mc.sayda.twilight_lib.capabilities.EffectsData) effects).setActiveEffect(effectId, true, persistent);

            // Save to persistent NBT
            target.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());

            // Sync to all clients (we only sync active effects now)
            NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(target.getUUID(), effects.getActiveEffects()));
        });

        // Send feedback only to admin/console (not when player targets self via CreRaces)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            String persistMode = persistent ? " (persistent)" : " (temporary)";
            source.sendSuccess(() -> Component.literal("Effect '" + effectId + "' equipped for " + target.getGameProfile().getName() + persistMode), true);
        }

        return 1;
    }

    private static int executeUnequipEffect(CommandSourceStack source, ServerPlayer target, String effectId) {
        target.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            effects.setActiveEffect(effectId, false);

            // Save to persistent NBT
            target.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());

            // Sync to all clients
            NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(target.getUUID(), effects.getActiveEffects()));
        });

        // Send feedback only to admin/console (not when player targets self via CreRaces)
        if (CommandUtils.shouldSendFeedbackToSource(source, target)) {
            source.sendSuccess(() -> Component.literal("Effect '" + effectId + "' unequipped for " + target.getGameProfile().getName()), true);
        }

        return 1;
    }

    private static int executeListEffects(CommandSourceStack source) {
        // Admin command - list ALL available effect types
        source.sendSuccess(() -> Component.literal("Available effects: respawn_twilight"), false);
        return 1;
    }

    private static int executeReload(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Reloading Twilight Lib..."), true);
        LOGGER.info("Hey, whatcha doing? Admin {} initiated reload command", source.getTextName());

        try {
            // Reload supporter data from GitHub
            source.sendSuccess(() -> Component.literal("Fetching supporter data from GitHub..."), false);
            mc.sayda.twilight_lib.supporter.SupporterService.fetchSupporters().thenRun(() -> {
                source.sendSuccess(() -> Component.literal("✓ Supporter data reloaded successfully!"), false);
                LOGGER.info("Gotcha! Now that was more sparkles. Supporter data reloaded via command");
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

    private static int executeEquipAddon(CommandSourceStack source, ServerPlayer target, String addonId, boolean persistent) {
        if (!AddonRegistry.hasAddon(addonId)) {
            source.sendFailure(Component.literal("Unknown addon: " + addonId));
            LOGGER.warn("Or, what. Unknown addon requested: {}", addonId);
            return 0;
        }

        target.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            // Activate addon with persistence control
            // persistent=true: CreRaces race attributes (survive logout/death)
            // persistent=false: Temporary admin preview (cleared on logout)
            ((mc.sayda.twilight_lib.capabilities.AddonsData) addons).setActiveAddon(addonId, true, persistent);
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
            // Admin command: Force deactivate regardless of ownership
            addons.setActiveAddon(addonId, false);
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
}
