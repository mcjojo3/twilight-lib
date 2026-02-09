package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ITrails;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.commands.CosmeticsCommand;
import mc.sayda.twilight_lib.commands.TwilightLibCommands;
import mc.sayda.twilight_lib.config.TwilightConfig;
import mc.sayda.twilight_lib.cosmetics.ModRequirement;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.SyncEffectsPacket;
import mc.sayda.twilight_lib.network.SyncModelVariantPacket;
import mc.sayda.twilight_lib.network.SyncMorphPacket;
import mc.sayda.twilight_lib.network.SyncTrailsPacket;
import mc.sayda.twilight_lib.supporter.SupporterService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main mod class for Twilight Lib (Common)
 * Handles initialization and event registration using Architectury API.
 */
public class TwilightLib {
    public static final String MODID = "twilight_lib";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Delayed task scheduler
    private static final Map<UUID, DelayedSyncTask> pendingTasks = new ConcurrentHashMap<>();
    private static final AtomicLong serverTicks = new AtomicLong(0);

    private static class DelayedSyncTask {
        final UUID playerUUID;
        final long executeAtTick;

        DelayedSyncTask(UUID playerUUID, long executeAtTick) {
            this.playerUUID = playerUUID;
            this.executeAtTick = executeAtTick;
        }
    }

    public static void init() {
        LOGGER.info("Yes! This'll be fun! Right? Twilight Lib is loading...");

        // Detect loaded mods
        ModRequirement.detectMods();
        LOGGER.info("Twilight Lib: Detected {} active integration mods.",
                mc.sayda.twilight_lib.cosmetics.ModRequirement.getLoadedMods().size());

        // Initialize Registries
        // Registrars are initialized on class load
        mc.sayda.twilight_lib.entity.ModEntities.register();
        mc.sayda.twilight_lib.particle.ModParticles.register();
        mc.sayda.twilight_lib.ModAttributes.register();
        LOGGER.info("Twilight Lib: Core registries initialized.");

        // Initialize Attributes and Events
        mc.sayda.twilight_lib.entity.ModEntityAttributes.register();
        mc.sayda.twilight_lib.TwilightEventHandler.init();

        // Initialize API registries
        mc.sayda.twilight_lib.addon.AddonRegistry.getInstance();
        mc.sayda.twilight_lib.addon.AddonLogicalInit.init();
        mc.sayda.twilight_lib.morph.MorphRegistry.getInstance();
        mc.sayda.twilight_lib.model_variant.ModelVariantRegistry.getInstance();

        // Network
        NetworkHandler.register();
        LOGGER.info("Twilight Lib: Network handler registered.");

        // Register Commands
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
            TwilightLibCommands.registerCommands(dispatcher);
            CosmeticsCommand.registerCommands(dispatcher);
        });

        // Player Events
        PlayerEvent.PLAYER_JOIN.register(TwilightLib::onPlayerLogin);
        PlayerEvent.PLAYER_QUIT.register(TwilightLib::onPlayerLogout);
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wonGame) -> {
            onPlayerClone(oldPlayer, newPlayer, !wonGame);
        });
        PlayerEvent.CHANGE_DIMENSION.register((player, oldLevel, newLevel) -> {
            onPlayerChangedDimension(player, oldLevel, newLevel);
        });

        // Entity Events
        // (StartTracking and PlayerRespawn are handled by platform-specific event
        // handlers due to API differences)

        // Tick Events
        TickEvent.SERVER_POST.register(TwilightLib::onServerTick);

        // Fetch support data (async)
        SupporterService.fetchSupporters();

        LOGGER.info("Hi! My name is Zoe. Twilight Lib initialized (Common).");
    }

    private static void processSupporterCosmetics(ServerPlayer player) {
        if (player == null)
            return;

        SupporterService.fetchSupporters().thenAccept(v -> {
            player.server.execute(() -> {
                if (player.isRemoved())
                    return;

                mc.sayda.twilight_lib.cosmetics.CosmeticManager.resyncAll(player);
                LOGGER.info("Twilight Lib: Processed cosmetics for supporter {}", player.getGameProfile().getName());
            });
        });
    }

    private static void onPlayerLogin(ServerPlayer loggedInPlayer) {
        LOGGER.info("I wanna have fun and chat with someone besides myself! Syncing morphs and addons for {}",
                loggedInPlayer.getGameProfile().getName());

        // Load persisted data from NBT FIRST
        CompoundTag persistentData = DataUtils.getPersistentData(loggedInPlayer);

        if (persistentData.contains(TwilightConstants.NBT_MORPH, CompoundTag.TAG_COMPOUND)) {
            IMorph morph = DataUtils.getMorphData(loggedInPlayer);
            if (morph != null) {
                morph.deserialize(persistentData.getCompound(TwilightConstants.NBT_MORPH));
                LOGGER.debug("Come on, this is gonna be fun! Restored morph from NBT for {}",
                        loggedInPlayer.getGameProfile().getName());
            }
        }

        if (persistentData.contains(TwilightConstants.NBT_ADDONS, CompoundTag.TAG_COMPOUND)) {
            IAddons addons = DataUtils.getAddonsData(loggedInPlayer);
            if (addons != null) {
                addons.deserialize(persistentData.getCompound(TwilightConstants.NBT_ADDONS));
            }
        }

        if (persistentData.contains(TwilightConstants.NBT_TRAILS, CompoundTag.TAG_COMPOUND)) {
            ITrails trails = DataUtils.getTrailsData(loggedInPlayer);
            if (trails != null) {
                trails.deserialize(persistentData.getCompound(TwilightConstants.NBT_TRAILS));
            }
        }

        if (persistentData.contains(TwilightConstants.NBT_EFFECTS, CompoundTag.TAG_COMPOUND)) {
            IEffects effects = DataUtils.getEffectsData(loggedInPlayer);
            if (effects != null) {
                effects.deserialize(persistentData.getCompound(TwilightConstants.NBT_EFFECTS));
            }
        }

        if (persistentData.contains(TwilightConstants.NBT_MODEL_VARIANT, CompoundTag.TAG_COMPOUND)) {
            IModelVariant modelVariant = DataUtils.getModelVariantData(loggedInPlayer);
            if (modelVariant != null) {
                modelVariant.deserialize(persistentData.getCompound(TwilightConstants.NBT_MODEL_VARIANT));
            }
        }

        // Supporter checks (Async, non-blocking)
        processSupporterCosmetics(loggedInPlayer);

        // Schedule sync
        long delayTicks = TwilightConfig.LOGIN_SYNC_DELAY_TICKS.get();
        pendingTasks.put(loggedInPlayer.getUUID(),
                new DelayedSyncTask(loggedInPlayer.getUUID(), serverTicks.get() + delayTicks));

        // Initial Sync (Morph, Addons, Trails, Effects, ModelVariant)
        mc.sayda.twilight_lib.cosmetics.CosmeticManager.resyncAll(loggedInPlayer);
    }

    private static void onPlayerLogout(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        pendingTasks.computeIfPresent(playerUUID, (uuid, task) -> {
            LOGGER.debug("Goodbye, my new friend! Cleaning up pending sync task for {} on logout",
                    player.getGameProfile().getName());
            return null;
        });
    }

    private static void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wasDeath) {
        CompoundTag oldData = DataUtils.getPersistentData(oldPlayer);

        if (oldData.contains(TwilightConstants.NBT_MORPH, CompoundTag.TAG_COMPOUND)) {
            IMorph newMorph = DataUtils.getMorphData(newPlayer);
            if (newMorph != null) {
                newMorph.deserialize(oldData.getCompound(TwilightConstants.NBT_MORPH));
                DataUtils.getPersistentData(newPlayer).put(TwilightConstants.NBT_MORPH,
                        oldData.getCompound(TwilightConstants.NBT_MORPH));
            }
        }

        if (oldData.contains(TwilightConstants.NBT_ADDONS, CompoundTag.TAG_COMPOUND)) {
            IAddons newAddons = DataUtils.getAddonsData(newPlayer);
            if (newAddons != null) {
                newAddons.deserialize(oldData.getCompound(TwilightConstants.NBT_ADDONS));
                DataUtils.getPersistentData(newPlayer).put(TwilightConstants.NBT_ADDONS,
                        oldData.getCompound(TwilightConstants.NBT_ADDONS));
            }
        }

        if (oldData.contains(TwilightConstants.NBT_TRAILS, CompoundTag.TAG_COMPOUND)) {
            ITrails newTrails = DataUtils.getTrailsData(newPlayer);
            if (newTrails != null) {
                newTrails.deserialize(oldData.getCompound(TwilightConstants.NBT_TRAILS));
                DataUtils.getPersistentData(newPlayer).put(TwilightConstants.NBT_TRAILS,
                        oldData.getCompound(TwilightConstants.NBT_TRAILS));
            }
        }

        if (oldData.contains(TwilightConstants.NBT_EFFECTS, CompoundTag.TAG_COMPOUND)) {
            IEffects newEffects = DataUtils.getEffectsData(newPlayer);
            if (newEffects != null) {
                newEffects.deserialize(oldData.getCompound(TwilightConstants.NBT_EFFECTS));
                DataUtils.getPersistentData(newPlayer).put(TwilightConstants.NBT_EFFECTS,
                        oldData.getCompound(TwilightConstants.NBT_EFFECTS));
            }
        }

        if (oldData.contains(TwilightConstants.NBT_MODEL_VARIANT, CompoundTag.TAG_COMPOUND)) {
            IModelVariant newModelVariant = DataUtils.getModelVariantData(newPlayer);
            if (newModelVariant != null) {
                newModelVariant.deserialize(oldData.getCompound(TwilightConstants.NBT_MODEL_VARIANT));
                DataUtils.getPersistentData(newPlayer).put(TwilightConstants.NBT_MODEL_VARIANT,
                        oldData.getCompound(TwilightConstants.NBT_MODEL_VARIANT));
            }
        }
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        LOGGER.debug("Here you go! Syncing all data for respawning player {}", player.getGameProfile().getName());
        mc.sayda.twilight_lib.cosmetics.CosmeticManager.resyncAll(player);
    }

    public static void onStartTracking(ServerPlayer tracker, net.minecraft.world.entity.Entity target) {
        if (!(target instanceof ServerPlayer targetPlayer))
            return;

        // Send target's data to the tracker
        IMorph morph = DataUtils.getMorphData(targetPlayer);
        if (morph != null) {
            NetworkHandler.sendToPlayer(tracker,
                    SyncMorphPacket.of(targetPlayer.getUUID(), morph.getEntityType(), morph.isNametagHidden()));
        }

        IAddons addons = DataUtils.getAddonsData(targetPlayer);
        if (addons != null) {
            NetworkHandler.sendAddonsToPlayer(tracker,
                    new SyncAddonsPacket(targetPlayer.getUUID(), addons.getActiveAddons(),
                            addons.getAllAddonTints()));
        }

        ITrails trails = DataUtils.getTrailsData(targetPlayer);
        if (trails != null) {
            NetworkHandler.sendTrailsToPlayer(tracker,
                    new SyncTrailsPacket(targetPlayer.getUUID(), trails.getActiveTrails()));
        }

        IEffects effects = DataUtils.getEffectsData(targetPlayer);
        if (effects != null) {
            NetworkHandler.sendEffectsToPlayer(tracker,
                    new SyncEffectsPacket(targetPlayer.getUUID(), effects.getActiveEffects(), false));
        }

        IModelVariant mv = DataUtils.getModelVariantData(targetPlayer);
        if (mv != null) {
            NetworkHandler.sendModelVariantToPlayer(tracker, SyncModelVariantPacket.of(targetPlayer.getUUID(), mv));
        }
    }

    private static void onPlayerChangedDimension(ServerPlayer player,
            net.minecraft.resources.ResourceKey<Level> oldLevel, net.minecraft.resources.ResourceKey<Level> newLevel) {
        // Dimension change handled same as respawn for syncing
        mc.sayda.twilight_lib.cosmetics.CosmeticManager.resyncAll(player);
    }

    private static void onServerTick(MinecraftServer server) {
        long currentTick = serverTicks.incrementAndGet();

        if (pendingTasks.isEmpty())
            return;

        Iterator<Map.Entry<UUID, DelayedSyncTask>> iterator = pendingTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DelayedSyncTask> entry = iterator.next();
            DelayedSyncTask task = entry.getValue();

            if (currentTick >= task.executeAtTick) {
                ServerPlayer player = server.getPlayerList().getPlayer(task.playerUUID);
                if (player != null && !player.isRemoved()) {
                    NetworkHandler.sendAllMorphsToPlayer(player);
                    NetworkHandler.sendAllAddonsToPlayer(player);
                    NetworkHandler.sendAllTrailsToPlayer(player);
                    NetworkHandler.sendAllEffectsToPlayer(player);
                    NetworkHandler.sendAllModelVariantsToPlayer(player);
                    LOGGER.debug("Here you go! Delayed cosmetics sync complete for {}",
                            player.getGameProfile().getName());
                }
                iterator.remove();
            }
        }
    }
}
