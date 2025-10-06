package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncMorphPacket;
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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TwilightLibCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Cache of valid living entity types to avoid creating test entities repeatedly
    // Note: Cache is invalidated on world unload to detect dynamically registered entities
    private static final Set<ResourceLocation> VALID_LIVING_ENTITIES = new HashSet<>();
    private static boolean cacheInitialized = false;
    private static int cachedRegistrySize = 0;

    // Suggestion provider for all entity types
    private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTIONS = (context, builder) -> {
        // Initialize or refresh cache if registry changed
        var level = context.getSource().getLevel();
        if (!cacheInitialized || shouldRefreshCache(level)) {
            initializeEntityCache(level);
        }
        return SharedSuggestionProvider.suggestResource(VALID_LIVING_ENTITIES.stream(), builder);
    };

    /**
     * Check if entity registry has changed (new mods loaded entities).
     * @return true if cache should be refreshed
     */
    private static boolean shouldRefreshCache(net.minecraft.world.level.Level level) {
        int currentSize = BuiltInRegistries.ENTITY_TYPE.size();
        if (currentSize != cachedRegistrySize) {
            LOGGER.debug("Every system has a weakness. Entity registry changed: {} -> {}", cachedRegistrySize, currentSize);
            return true;
        }
        return false;
    }

    /**
     * Initialize cache of valid living entity types.
     * Called once on first command suggestion to avoid creating test entities on every keystroke.
     * Automatically invalidates and refreshes if registry size changes.
     */
    private static void initializeEntityCache(net.minecraft.world.level.Level level) {
        // Clear cache if refreshing
        if (cacheInitialized) {
            VALID_LIVING_ENTITIES.clear();
            cacheInitialized = false;
        }

        LOGGER.debug("Let's de-encrypt this whole reality! Initializing entity type cache...");
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
            } finally {
                // Always discard test entity to prevent leak
                if (testEntity != null) {
                    testEntity.discard();
                }
            }
        }
        cachedRegistrySize = BuiltInRegistries.ENTITY_TYPE.size();
        cacheInitialized = true;
        LOGGER.debug("Okay, we're in! Entity type cache initialized with {} living entities", VALID_LIVING_ENTITIES.size());
    }

    public static void registerCommands(RegisterCommandsEvent evt) {
        // Register base commands with all aliases
        for (String alias : new String[]{"twilightlib", "tl"}) {
            evt.getDispatcher().register(
                    Commands.literal(alias)
                            .requires(src -> src.hasPermission(2))
                            // morph <entity> - targets executor
                            .then(Commands.literal("morph")
                                    .then(Commands.argument("entity", ResourceLocationArgument.id())
                                            .suggests(ENTITY_SUGGESTIONS)
                                            .executes(ctx -> {
                                                ServerPlayer target = getTargetPlayer(ctx.getSource());
                                                if (target == null) {
                                                    ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                                    return 0;
                                                }
                                                return executeMorph(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "entity"));
                                            })
                                            // morph <entity> <target> - targets specific player
                                            .then(Commands.argument("target", EntityArgument.player())
                                                    .executes(ctx -> {
                                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                        return executeMorph(ctx.getSource(), target, ResourceLocationArgument.getId(ctx, "entity"));
                                                    }))))
                            // unmorph - targets executor
                            .then(Commands.literal("unmorph")
                                    .executes(ctx -> {
                                        ServerPlayer target = getTargetPlayer(ctx.getSource());
                                        if (target == null) {
                                            ctx.getSource().sendFailure(Component.literal("This command can only be used by players or must specify a target."));
                                            return 0;
                                        }
                                        setMorph(ctx.getSource(), target, Optional.empty());
                                        return 1;
                                    })
                                    // unmorph <target> - targets specific player
                                    .then(Commands.argument("target", EntityArgument.player())
                                            .executes(ctx -> {
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                setMorph(ctx.getSource(), target, Optional.empty());
                                                return 1;
                                            })))
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

        boolean isLiving = testEntity instanceof LivingEntity;
        testEntity.discard();

        if (!isLiving) {
            source.sendFailure(Component.literal("Entity must be a LivingEntity."));
            LOGGER.warn("Is this the best physical representation you can manifest? It completely lacks zazz! {}", rl);
            return 0;
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
                target.getPersistentData().put("TwilightLibMorph", m.serialize());
            } else {
                target.getPersistentData().remove("TwilightLibMorph");
                LOGGER.debug("Paradigm shift time! {} has been unmorphed", target.getGameProfile().getName());
            }

            NetworkHandler.sendToAll(SyncMorphPacket.of(target.getUUID(), morph.orElse(null)));
            target.refreshDimensions();
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
            if (morph.isPresent()) {
                source.sendSuccess(() -> Component.literal("Morphed " + target.getGameProfile().getName() + " -> " + morph.get()), true);
            } else {
                source.sendSuccess(() -> Component.literal("Unmorphed " + target.getGameProfile().getName()), true);
            }
        }
    }
}