package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ITrails;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import mc.sayda.twilight_lib.capabilities.TrailsData;
import mc.sayda.twilight_lib.commands.CosmeticsCommand;
import mc.sayda.twilight_lib.commands.TwilightLibCommands;
import mc.sayda.twilight_lib.config.TwilightConfig;
import mc.sayda.twilight_lib.cosmetics.ModRequirement;
import mc.sayda.twilight_lib.entity.ModEntities;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.SyncEffectsPacket;
import mc.sayda.twilight_lib.network.SyncModelVariantPacket;
import mc.sayda.twilight_lib.network.SyncMorphPacket;
import mc.sayda.twilight_lib.network.SyncTrailsPacket;
import mc.sayda.twilight_lib.particle.ModParticles;
import mc.sayda.twilight_lib.supporter.SupporterData;
import mc.sayda.twilight_lib.supporter.SupporterService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main mod class for Twilight Lib - A cosmetic player customization library for
 * Minecraft.
 *
 * <p>
 * Twilight Lib provides a framework for player cosmetics including:
 * <ul>
 * <li><b>Morphs</b>: Transform players into different entities with matching
 * hitboxes</li>
 * <li><b>Addons</b>: Visual 3D attachments like tails, wings, and ears</li>
 * <li><b>Trails</b>: Particle effects that follow player movement</li>
 * <li><b>Effects</b>: Event-triggered cosmetics like spawn effects and ambient
 * particles</li>
 * </ul>
 *
 * <p>
 * <b>Supporter Integration</b>: Cosmetics are tied to Patreon supporter tiers.
 * The mod fetches supporter data from GitHub on startup and grants cosmetics
 * based on tier.
 * Manual cosmetic grants persist independently of supporter status for
 * gifting/admin purposes.
 *
 * <p>
 * <b>Thread Safety</b>: This class uses concurrent data structures for delayed
 * sync tasks.
 * The {@link #pendingTasks} map is thread-safe via {@link ConcurrentHashMap}.
 * The {@link #serverTicks} counter uses {@link AtomicLong} for atomic
 * increments during server ticks.
 *
 * @author SaydaGames (mc_jojo3)
 * @version 1.0
 */
@Mod(TwilightLib.MODID)
public class TwilightLib {
    public static final String MODID = "twilight_lib";
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Delayed task scheduler for login cosmetics sync.
     *
     * <p>
     * <b>Why delayed sync?</b> When a player logs in, their client-side entity may
     * not be
     * fully loaded yet. Syncing cosmetics immediately can cause visual glitches or
     * lost packets.
     * This queue delays cosmetics sync by a configurable number of ticks (default:
     * 20 ticks = 1 second)
     * to ensure the client is ready to receive and render cosmetic data.
     *
     * <p>
     * <b>Thread Safety</b>: Uses ConcurrentHashMap to allow safe concurrent access
     * from
     * multiple server threads (login events and tick events may occur on different
     * threads).
     */
    private static final Map<UUID, DelayedSyncTask> pendingTasks = new ConcurrentHashMap<>();

    /**
     * Server tick counter for delayed task scheduling.
     *
     * <p>
     * <b>Thread Safety</b>: Uses AtomicLong for thread-safe increments without
     * synchronization.
     * Incremented once per server tick in
     * {@link #onServerTick(ServerTickEvent.Post)}.
     */
    private static final AtomicLong serverTicks = new AtomicLong(0);

    /**
     * Represents a cosmetics sync task scheduled for future execution.
     *
     * <p>
     * Tasks are immutable after creation to prevent race conditions.
     */
    private static class DelayedSyncTask {
        final UUID playerUUID;
        final long executeAtTick;

        DelayedSyncTask(UUID playerUUID, long executeAtTick) {
            this.playerUUID = playerUUID;
            this.executeAtTick = executeAtTick;
        }
    }

    public TwilightLib(IEventBus modBus, ModContainer modContainer) {
        LOGGER.info("Yes! This'll be fun! Right? Twilight Lib is loading...");

        // Register config
        modContainer.registerConfig(ModConfig.Type.COMMON, TwilightConfig.COMMON_CONFIG);

        // Detect loaded mods BEFORE registering cosmetics (critical order!)
        ModRequirement.detectMods();

        ModEntities.register(modBus);
        ModParticles.register(modBus);
        ModAttributes.register(modBus);
        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        modBus.addListener(this::onEntityAttributeModification);
        modBus.addListener(NetworkHandler::register);

        NeoForge.EVENT_BUS.addListener(TwilightLibCommands::registerCommands);
        NeoForge.EVENT_BUS.addListener(CosmeticsCommand::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(this::onPlayerStartTracking);
        NeoForge.EVENT_BUS.addListener(TwilightLib::onServerTick);

        // Fetch supporter list on startup (async)
        SupporterService.fetchSupporters();

        LOGGER.info("Hi! My name is Zoe. Twilight Lib loaded successfully.");
    }

    private void onEntityAttributeModification(final EntityAttributeModificationEvent evt) {
        // Add custom attributes to all living entities (especially players)
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.MINING_PENALTY);
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.FOV_MODIFIER);
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_HELMET);
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_CHESTPLATE);
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_LEGGINGS);
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_BOOTS);
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ELYTRA_FLIGHT);
        LOGGER.debug("Come on, this is gonna be fun! Added custom attributes to players.");
    }

    private void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent evt) {
        Player loggedInPlayer = evt.getEntity();
        if (loggedInPlayer.level().isClientSide)
            return;

        LOGGER.info("I wanna have fun and chat with someone besides myself! Syncing morphs and addons for {}",
                loggedInPlayer.getGameProfile().getName());

        // Load persisted data from NBT FIRST (before supporter checks modify it)
        CompoundTag persistentData = loggedInPlayer.getPersistentData();

        if (persistentData.contains(TwilightConstants.NBT_MORPH, CompoundTag.TAG_COMPOUND)) {
            IMorph morph = loggedInPlayer.getData(ModAttachments.MORPH);
            if (morph != null) {
                morph.deserialize(persistentData.getCompound(TwilightConstants.NBT_MORPH));
                LOGGER.debug("Come on, this is gonna be fun! Restored morph from NBT for {}",
                        loggedInPlayer.getGameProfile().getName());
            }
        }

        if (persistentData.contains(TwilightConstants.NBT_ADDONS, CompoundTag.TAG_COMPOUND)) {
            IAddons addons = loggedInPlayer.getData(ModAttachments.ADDONS);
            if (addons != null) {
                addons.deserialize(persistentData.getCompound(TwilightConstants.NBT_ADDONS));
                LOGGER.debug("Come on, this is gonna be fun! Restored {} addons from NBT for {}",
                        addons.getAddons().size(), loggedInPlayer.getGameProfile().getName());
            }
        }

        if (persistentData.contains(TwilightConstants.NBT_TRAILS, CompoundTag.TAG_COMPOUND)) {
            ITrails trails = loggedInPlayer.getData(ModAttachments.TRAILS);
            if (trails != null) {
                trails.deserialize(persistentData.getCompound(TwilightConstants.NBT_TRAILS));
                LOGGER.debug("Come on, this is gonna be fun! Restored {} trails from NBT for {}",
                        trails.getTrails().size(), loggedInPlayer.getGameProfile().getName());
            }
        }

        if (persistentData.contains(TwilightConstants.NBT_EFFECTS, CompoundTag.TAG_COMPOUND)) {
            IEffects effects = loggedInPlayer.getData(ModAttachments.EFFECTS);
            if (effects != null) {
                effects.deserialize(persistentData.getCompound(TwilightConstants.NBT_EFFECTS));
                LOGGER.debug("Come on, this is gonna be fun! Restored {} effects from NBT for {}",
                        effects.getEffects().size(), loggedInPlayer.getGameProfile().getName());
            }
        }

        if (persistentData.contains(TwilightConstants.NBT_MODEL_VARIANT, CompoundTag.TAG_COMPOUND)) {
            IModelVariant modelVariant = loggedInPlayer.getData(ModAttachments.MODEL_VARIANT);
            if (modelVariant != null) {
                modelVariant.deserialize(persistentData.getCompound(TwilightConstants.NBT_MODEL_VARIANT));
                LOGGER.debug("Come on, this is gonna be fun! Restored model variant from NBT for {}",
                        loggedInPlayer.getGameProfile().getName());
            }
        }

        // TODO CRITICAL: This blocking .get() call freezes the entire server during
        // player login!
        // This is a DoS vector - repeated logins can cause server lag for all players.
        // FIX: Replace with non-blocking check using isDone() or thenAcceptAsync()
        // callback
        // See audit issue: "Blocking Call on Login (Lines 200-207)"
        // Ensure supporter data is loaded before checking (waits if fetch is in
        // progress)
        try {
            SupporterService.fetchSupporters().get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.warn(
                    "How did I?! Uuuughh! Supporter data fetch timed out after 10 seconds. Proceeding without supporter sync.");
        } catch (Exception e) {
            LOGGER.warn("Shoot! Error waiting for supporter data: {}", e.getMessage());
        }

        // Check supporter status and auto-grant cosmetics (tier unlocks + manual
        // overrides)
        String uuid = loggedInPlayer.getStringUUID();
        Optional<SupporterData> supporterData = SupporterService.getSupporterData(uuid);

        if (supporterData.isPresent()) {
            SupporterData data = supporterData.get();

            // Get ALL cosmetics (tier-based + manual overrides)
            Set<String> allTrails = data.getAllTrails();
            Set<String> allAddons = data.getAllAddons();
            Set<String> allEffects = data.getAllEffects();

            // Log supporter status
            if (data.isActiveSupporter()) {
                LOGGER.info("Delightful little world you have... I like it! {} is a {} tier supporter",
                        loggedInPlayer.getGameProfile().getName(), data.getTier());
                LOGGER.debug("I have a gift for you! Tier '{}' grants trails: {}", data.getTier(), allTrails);
            } else {
                LOGGER.info("I have a gift for you! {} has manual cosmetic grants (expired/gift supporter)",
                        loggedInPlayer.getGameProfile().getName());
            }

            // Auto-grant trails (tier unlocks + manual overrides)
            ITrails trails = loggedInPlayer.getData(ModAttachments.TRAILS);

            // Type-safe cast to access implementation-specific methods
            if (trails == null) {
                LOGGER.error("Or, what. Trails attachment is null for player {} - this should never happen!",
                        loggedInPlayer.getGameProfile().getName());
            } else if (!(trails instanceof TrailsData)) {
                LOGGER.error(
                        "Is this the best physical representation you can manifest? Another mod replaced ITrails attachment with incompatible implementation: {}. Expected: TrailsData",
                        trails.getClass().getName());
                LOGGER.error("Or, what. Skipping trail sync for {} - attachment incompatibility detected",
                        loggedInPlayer.getGameProfile().getName());
            } else {
                TrailsData trailsData = (TrailsData) trails;
                // Sync owned trails with current supporter status (preserves persistent active
                // state)
                Set<String> currentOwnedTrails = new java.util.HashSet<>(trails.getTrails());

                // Remove trails no longer granted (ownership only - doesn't affect persistent
                // active)
                for (String trail : currentOwnedTrails) {
                    if (!allTrails.contains(trail)) {
                        trailsData.removeTrailOwnership(trail);
                    }
                }

                // Add newly granted trails
                for (String trail : allTrails) {
                    if (!currentOwnedTrails.contains(trail)) {
                        trails.addTrail(trail);
                    }
                }

                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());
            }

            // Auto-grant addons (tier unlocks + manual overrides)
            IAddons addons = loggedInPlayer.getData(ModAttachments.ADDONS);

            // Type-safe cast to access implementation-specific methods
            if (addons == null) {
                LOGGER.error("Or, what. Addons attachment is null for player {} - this should never happen!",
                        loggedInPlayer.getGameProfile().getName());
            } else if (!(addons instanceof AddonsData)) {
                LOGGER.error(
                        "Is this the best physical representation you can manifest? Another mod replaced IAddons attachment with incompatible implementation: {}. Expected: AddonsData",
                        addons.getClass().getName());
                LOGGER.error("Or, what. Skipping addon sync for {} - attachment incompatibility detected",
                        loggedInPlayer.getGameProfile().getName());
            } else {
                AddonsData addonsData = (AddonsData) addons;
                // Sync owned addons with current supporter status (preserves persistent active
                // state)
                Set<String> currentOwnedAddons = new java.util.HashSet<>(addons.getAddons());

                // Remove addons no longer granted (ownership only - doesn't affect persistent
                // active)
                for (String addon : currentOwnedAddons) {
                    if (!allAddons.contains(addon)) {
                        addonsData.removeAddonOwnership(addon);
                    }
                }

                // Add newly granted addons
                for (String addon : allAddons) {
                    if (!currentOwnedAddons.contains(addon)) {
                        addons.addAddon(addon);
                    }
                }

                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());
            }

            // Auto-grant effects (tier unlocks + manual overrides)
            IEffects effects = loggedInPlayer.getData(ModAttachments.EFFECTS);

            // Type-safe cast to access implementation-specific methods
            if (effects == null) {
                LOGGER.error("Or, what. Effects attachment is null for player {} - this should never happen!",
                        loggedInPlayer.getGameProfile().getName());
            } else if (!(effects instanceof EffectsData)) {
                LOGGER.error(
                        "Is this the best physical representation you can manifest? Another mod replaced IEffects attachment with incompatible implementation: {}. Expected: EffectsData",
                        effects.getClass().getName());
                LOGGER.error("Or, what. Skipping effect sync for {} - attachment incompatibility detected",
                        loggedInPlayer.getGameProfile().getName());
            } else {
                EffectsData effectsData = (EffectsData) effects;
                // Sync owned effects with current supporter status (preserves persistent active
                // state)
                Set<String> currentOwnedEffects = new java.util.HashSet<>(effects.getEffects());

                // Remove effects no longer granted (ownership only - doesn't affect persistent
                // active)
                for (String effect : currentOwnedEffects) {
                    if (!allEffects.contains(effect)) {
                        effectsData.removeEffectOwnership(effect);
                    }
                }

                // Add newly granted effects
                for (String effect : allEffects) {
                    if (!currentOwnedEffects.contains(effect)) {
                        effects.addEffect(effect);
                    }
                }

                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());
            }

            LOGGER.info(
                    "You need more stardust, I could give you some! Auto-granted {} trails, {} addons, {} effects to {}",
                    allTrails.size(), allAddons.size(), allEffects.size(),
                    loggedInPlayer.getGameProfile().getName());
        }

        // Schedule delayed cosmetics sync to allow client entity loading
        long delayTicks = TwilightConfig.LOGIN_SYNC_DELAY_TICKS.get();
        pendingTasks.put(loggedInPlayer.getUUID(),
                new DelayedSyncTask(loggedInPlayer.getUUID(), serverTicks.get() + delayTicks));

        // Send this player's morph to everyone else
        IMorph morph = loggedInPlayer.getData(ModAttachments.MORPH);
        if (morph != null) {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendMorphToAll(
                        SyncMorphPacket.of(loggedInPlayer.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                // Force dimension refresh to apply morph hitbox immediately
                loggedInPlayer.refreshDimensions();
                // Persist morph state to NBT to prevent data loss on logout
                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_MORPH, morph.serialize());
                LOGGER.info("We are going to be best friends! Player {} logged in with morph: {}",
                        loggedInPlayer.getGameProfile().getName(), rl);
            });
        }

        // Send this player's active addons to everyone else
        IAddons loginAddons = loggedInPlayer.getData(ModAttachments.ADDONS);
        if (loginAddons != null && !loginAddons.getActiveAddons().isEmpty()) {
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(loggedInPlayer.getUUID(), loginAddons.getActiveAddons(),
                    loginAddons.getAllAddonTints()));
            LOGGER.info("We are going to be best friends! Player {} logged in with {} active addons",
                    loggedInPlayer.getGameProfile().getName(), loginAddons.getActiveAddons().size());
        }

        // Send this player's active trails to everyone else
        ITrails loginTrails = loggedInPlayer.getData(ModAttachments.TRAILS);
        if (loginTrails != null && !loginTrails.getActiveTrails().isEmpty()) {
            NetworkHandler
                    .sendTrailsToAll(new SyncTrailsPacket(loggedInPlayer.getUUID(), loginTrails.getActiveTrails()));
            LOGGER.info("We are going to be best friends! Player {} logged in with {} active trails",
                    loggedInPlayer.getGameProfile().getName(), loginTrails.getActiveTrails().size());
        }

        // Send this player's effects to everyone else (trigger spawn effect on login)
        IEffects loginEffects = loggedInPlayer.getData(ModAttachments.EFFECTS);
        if (loginEffects != null && !loginEffects.getActiveEffects().isEmpty()) {
            NetworkHandler.sendEffectsToAll(
                    new SyncEffectsPacket(loggedInPlayer.getUUID(), loginEffects.getActiveEffects(), true));
        }

        // Send this player's model variant to everyone else
        IModelVariant loginModelVariant = loggedInPlayer.getData(ModAttachments.MODEL_VARIANT);
        if (loginModelVariant != null) {
            NetworkHandler
                    .sendModelVariantToAll(SyncModelVariantPacket.of(loggedInPlayer.getUUID(), loginModelVariant));
            LOGGER.info("We are going to be best friends! Player {} logged in as {} model variant",
                    loggedInPlayer.getGameProfile().getName(), loginModelVariant.getModelVariant());
        }
    }

    private void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent evt) {
        Player player = evt.getEntity();
        if (player.level().isClientSide)
            return;

        // Clean up pending sync tasks for this player (prevent memory leak)
        // Using computeIfPresent for atomic removal (prevents race with tick handler)
        UUID playerUUID = player.getUUID();
        pendingTasks.computeIfPresent(playerUUID, (uuid, task) -> {
            LOGGER.debug("Goodbye, my new friend! Cleaning up pending sync task for {} on logout",
                    player.getGameProfile().getName());
            return null; // Returning null removes the entry atomically
        });
    }

    private void onPlayerClone(final PlayerEvent.Clone evt) {
        if (evt.getEntity().level().isClientSide)
            return;

        // Handle both death and dimension changes
        // In NeoForge, attachments with copyOnDeath() are automatically copied.
        // However, we still need to preserve persistent NBT data for consistency.
        CompoundTag oldData = evt.getOriginal().getPersistentData();

        // Restore morph (attachment is already copied via copyOnDeath())
        if (oldData.contains(TwilightConstants.NBT_MORPH, CompoundTag.TAG_COMPOUND)) {
            CompoundTag morphData = oldData.getCompound(TwilightConstants.NBT_MORPH);
            IMorph newMorph = evt.getEntity().getData(ModAttachments.MORPH);
            if (newMorph != null) {
                newMorph.deserialize(morphData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring morph from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_MORPH, morphData);
            }
        }

        // Restore addons (attachment is already copied via copyOnDeath())
        if (oldData.contains(TwilightConstants.NBT_ADDONS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag addonsData = oldData.getCompound(TwilightConstants.NBT_ADDONS);
            IAddons newAddons = evt.getEntity().getData(ModAttachments.ADDONS);
            if (newAddons != null) {
                newAddons.deserialize(addonsData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring addons from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_ADDONS, addonsData);
            }
        }

        // Restore trails (attachment is already copied via copyOnDeath())
        if (oldData.contains(TwilightConstants.NBT_TRAILS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag trailsData = oldData.getCompound(TwilightConstants.NBT_TRAILS);
            ITrails newTrails = evt.getEntity().getData(ModAttachments.TRAILS);
            if (newTrails != null) {
                newTrails.deserialize(trailsData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring trails from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_TRAILS, trailsData);
            }
        }

        // Restore effects (attachment is already copied via copyOnDeath())
        if (oldData.contains(TwilightConstants.NBT_EFFECTS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag effectsData = oldData.getCompound(TwilightConstants.NBT_EFFECTS);
            IEffects newEffects = evt.getEntity().getData(ModAttachments.EFFECTS);
            if (newEffects != null) {
                newEffects.deserialize(effectsData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring effects from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_EFFECTS, effectsData);
            }
        }

        // Restore model variant (attachment is already copied via copyOnDeath())
        if (oldData.contains(TwilightConstants.NBT_MODEL_VARIANT, CompoundTag.TAG_COMPOUND)) {
            CompoundTag modelVariantData = oldData.getCompound(TwilightConstants.NBT_MODEL_VARIANT);
            IModelVariant newModelVariant = evt.getEntity().getData(ModAttachments.MODEL_VARIANT);
            if (newModelVariant != null) {
                newModelVariant.deserialize(modelVariantData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring model variant from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_MODEL_VARIANT, modelVariantData);
            }
        }
    }

    private void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent evt) {
        Player player = evt.getEntity();
        if (player.level().isClientSide)
            return;

        // Sync morph to client after respawn (when client-side player entity exists)
        IMorph morph = player.getData(ModAttachments.MORPH);
        if (morph != null) {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler
                        .sendMorphToAll(SyncMorphPacket.of(player.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                player.refreshDimensions();
                // Persist morph state to NBT to prevent data loss
                player.getPersistentData().put(TwilightConstants.NBT_MORPH, morph.serialize());
                LOGGER.debug("Time to change! Player {} respawned as {}", player.getGameProfile().getName(), rl);
            });
        }

        // Sync active addons to client after respawn
        IAddons respawnAddons = player.getData(ModAttachments.ADDONS);
        if (respawnAddons != null && !respawnAddons.getActiveAddons().isEmpty()) {
            NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(player.getUUID(), respawnAddons.getActiveAddons(),
                    respawnAddons.getAllAddonTints()));
            LOGGER.debug("Time to change! Player {} respawned with {} active addons", player.getGameProfile().getName(),
                    respawnAddons.getActiveAddons().size());
        }

        // Sync active trails to client after respawn
        ITrails respawnTrails = player.getData(ModAttachments.TRAILS);
        if (respawnTrails != null && !respawnTrails.getActiveTrails().isEmpty()) {
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), respawnTrails.getActiveTrails()));
            LOGGER.debug("Time to change! Player {} respawned with {} active trails", player.getGameProfile().getName(),
                    respawnTrails.getActiveTrails().size());
        }

        // Sync effects to client after respawn (trigger spawn effect on respawn)
        IEffects respawnEffects = player.getData(ModAttachments.EFFECTS);
        if (respawnEffects != null && !respawnEffects.getActiveEffects().isEmpty()) {
            NetworkHandler
                    .sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), respawnEffects.getActiveEffects(), true));
            LOGGER.debug("Time to change! Player {} respawned with {} active effects",
                    player.getGameProfile().getName(), respawnEffects.getActiveEffects().size());
        }

        // Sync model variant to client after respawn
        IModelVariant modelVariant = player.getData(ModAttachments.MODEL_VARIANT);
        if (modelVariant != null) {
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(player.getUUID(), modelVariant));
            LOGGER.debug("Time to change! Player {} respawned as {} model", player.getGameProfile().getName(),
                    modelVariant.getModelVariant());
        }
    }

    /**
     * Handles player dimension change - syncs cosmetics to clients after traveling
     * to a new dimension.
     *
     * <p>
     * <b>Why this handler?</b> When a player changes dimensions (e.g., Overworld →
     * Nether),
     * the client needs to be re-synced with all cosmetic data. The
     * {@link #onPlayerClone} event
     * restores data from NBT, but doesn't broadcast to clients. This handler
     * ensures:
     * <ul>
     * <li>The player's cosmetics are visible in the new dimension</li>
     * <li>Other players in the new dimension can see the arriving player's
     * cosmetics</li>
     * <li>Spawn effects trigger on dimension entry</li>
     * </ul>
     *
     * <p>
     * <b>Why similar to respawn?</b> Dimension changes and respawns have similar
     * sync requirements:
     * both involve a player entity being recreated/repositioned, requiring full
     * cosmetic re-sync.
     *
     * @param evt The PlayerChangedDimensionEvent containing the player and
     *            dimension info
     */
    private void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent evt) {
        Player player = evt.getEntity();
        if (player.level().isClientSide)
            return;

        LOGGER.debug("Time to change! Player {} changed dimensions from {} to {}",
                player.getGameProfile().getName(), evt.getFrom(), evt.getTo());

        // Sync morph to client after dimension change
        IMorph morph = player.getData(ModAttachments.MORPH);
        if (morph != null) {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler
                        .sendMorphToAll(SyncMorphPacket.of(player.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                player.refreshDimensions();
                // Persist morph state to NBT to prevent data loss
                player.getPersistentData().put(TwilightConstants.NBT_MORPH, morph.serialize());
                LOGGER.debug("Time to change! Player {} entered {} as {}", player.getGameProfile().getName(),
                        evt.getTo().location(), rl);
            });
        }

        // Sync active addons to client after dimension change
        IAddons addons = player.getData(ModAttachments.ADDONS);
        if (addons != null && !addons.getActiveAddons().isEmpty()) {
            NetworkHandler.sendAddonsToAll(
                    new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons(), addons.getAllAddonTints()));
            LOGGER.debug("Time to change! Player {} entered {} with {} active addons",
                    player.getGameProfile().getName(), evt.getTo().location(), addons.getActiveAddons().size());
        }

        // Sync active trails to client after dimension change
        ITrails trails = player.getData(ModAttachments.TRAILS);
        if (trails != null && !trails.getActiveTrails().isEmpty()) {
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.getActiveTrails()));
            LOGGER.debug("Time to change! Player {} entered {} with {} active trails",
                    player.getGameProfile().getName(), evt.getTo().location(), trails.getActiveTrails().size());
        }

        // Sync effects to client after dimension change (trigger spawn effect on
        // dimension entry)
        IEffects effects = player.getData(ModAttachments.EFFECTS);
        if (effects != null && !effects.getActiveEffects().isEmpty()) {
            NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects(), true));
            LOGGER.debug("Time to change! Player {} entered {} with {} active effects",
                    player.getGameProfile().getName(), evt.getTo().location(), effects.getActiveEffects().size());
        }

        // Sync model variant to client after dimension change
        IModelVariant modelVariant = player.getData(ModAttachments.MODEL_VARIANT);
        if (modelVariant != null) {
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(player.getUUID(), modelVariant));
            LOGGER.debug("Time to change! Player {} entered {} as {} model",
                    player.getGameProfile().getName(), evt.getTo().location(), modelVariant.getModelVariant());
        }
    }

    private void onPlayerStartTracking(final PlayerEvent.StartTracking evt) {
        // When player A starts tracking entity B, if B is a player, send B's cosmetics
        // to A
        if (!(evt.getTarget() instanceof ServerPlayer trackedPlayer))
            return;
        if (!(evt.getEntity() instanceof ServerPlayer trackingPlayer))
            return;
        if (trackingPlayer.level().isClientSide)
            return;

        // Send the tracked player's cosmetics to the tracking player
        IMorph morph = trackedPlayer.getData(ModAttachments.MORPH);
        if (morph != null) {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendToPlayer(trackingPlayer,
                        SyncMorphPacket.of(trackedPlayer.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                LOGGER.debug("Here you go! Sent morph {} for {} to tracking player {}",
                        rl, trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
            });
        }

        IAddons addons = trackedPlayer.getData(ModAttachments.ADDONS);
        if (addons != null && !addons.getActiveAddons().isEmpty()) {
            NetworkHandler.sendAddonsToPlayer(trackingPlayer,
                    new SyncAddonsPacket(trackedPlayer.getUUID(), addons.getActiveAddons(), addons.getAllAddonTints()));
            LOGGER.debug("Here you go! Sent {} addons for {} to tracking player {}",
                    addons.getActiveAddons().size(), trackedPlayer.getGameProfile().getName(),
                    trackingPlayer.getGameProfile().getName());
        }

        ITrails trails = trackedPlayer.getData(ModAttachments.TRAILS);
        if (trails != null && !trails.getActiveTrails().isEmpty()) {
            NetworkHandler.sendTrailsToPlayer(trackingPlayer,
                    new SyncTrailsPacket(trackedPlayer.getUUID(), trails.getActiveTrails()));
            LOGGER.debug("Here you go! Sent {} trails for {} to tracking player {}",
                    trails.getActiveTrails().size(), trackedPlayer.getGameProfile().getName(),
                    trackingPlayer.getGameProfile().getName());
        }

        IEffects effects = trackedPlayer.getData(ModAttachments.EFFECTS);
        if (effects != null && !effects.getActiveEffects().isEmpty()) {
            NetworkHandler.sendEffectsToPlayer(trackingPlayer,
                    new SyncEffectsPacket(trackedPlayer.getUUID(), effects.getActiveEffects(), false));
            LOGGER.debug("Here you go! Sent {} effects for {} to tracking player {}",
                    effects.getActiveEffects().size(), trackedPlayer.getGameProfile().getName(),
                    trackingPlayer.getGameProfile().getName());
        }

        IModelVariant modelVariant = trackedPlayer.getData(ModAttachments.MODEL_VARIANT);
        if (modelVariant != null) {
            NetworkHandler.sendModelVariantToPlayer(trackingPlayer,
                    SyncModelVariantPacket.of(trackedPlayer.getUUID(), modelVariant));
            LOGGER.debug("Here you go! Sent model variant {} for {} to tracking player {}",
                    modelVariant.getModelVariant(), trackedPlayer.getGameProfile().getName(),
                    trackingPlayer.getGameProfile().getName());
        }
    }

    private static void onServerTick(final ServerTickEvent.Post evt) {
        long currentTick = serverTicks.incrementAndGet();

        // Process pending delayed sync tasks
        if (pendingTasks.isEmpty())
            return;

        MinecraftServer server = evt.getServer();
        if (server == null) {
            LOGGER.warn("Or, what. Cannot process delayed sync tasks - server is null");
            return;
        }
        Iterator<Map.Entry<UUID, DelayedSyncTask>> iterator = pendingTasks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, DelayedSyncTask> entry = iterator.next();
            DelayedSyncTask task = entry.getValue();

            if (currentTick >= task.executeAtTick) {
                // Time to execute this task
                ServerPlayer player = server.getPlayerList().getPlayer(task.playerUUID);
                if (player != null && !player.isRemoved()) {
                    NetworkHandler.sendAllMorphsToPlayer(player);
                    NetworkHandler.sendAllAddonsToPlayer(player);
                    NetworkHandler.sendAllTrailsToPlayer(player);
                    NetworkHandler.sendAllEffectsToPlayer(player);
                    NetworkHandler.sendAllModelVariantsToPlayer(player);
                    LOGGER.debug("While I wait, I will stay happy! Delayed cosmetics sync complete for {}",
                            player.getGameProfile().getName());
                } else {
                    LOGGER.debug("Goodbye, my new friend! Player left before delayed sync could complete");
                }

                // Remove completed task
                iterator.remove();
            }
        }
    }
}