package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ITrails;
import mc.sayda.twilight_lib.capabilities.ModelVariantProvider;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.capabilities.TrailsData;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main mod class for Twilight Lib - A cosmetic player customization library for Minecraft.
 *
 * <p>Twilight Lib provides a framework for player cosmetics including:
 * <ul>
 *   <li><b>Morphs</b>: Transform players into different entities with matching hitboxes</li>
 *   <li><b>Addons</b>: Visual 3D attachments like tails, wings, and ears</li>
 *   <li><b>Trails</b>: Particle effects that follow player movement</li>
 *   <li><b>Effects</b>: Event-triggered cosmetics like spawn effects and ambient particles</li>
 * </ul>
 *
 * <p><b>Supporter Integration</b>: Cosmetics are tied to Patreon supporter tiers.
 * The mod fetches supporter data from GitHub on startup and grants cosmetics based on tier.
 * Manual cosmetic grants persist independently of supporter status for gifting/admin purposes.
 *
 * <p><b>Thread Safety</b>: This class uses concurrent data structures for delayed sync tasks.
 * The {@link #pendingTasks} map is thread-safe via {@link ConcurrentHashMap}.
 * The {@link #serverTicks} counter uses {@link AtomicLong} for atomic increments during server ticks.
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
     * <p><b>Why delayed sync?</b> When a player logs in, their client-side entity may not be
     * fully loaded yet. Syncing cosmetics immediately can cause visual glitches or lost packets.
     * This queue delays cosmetics sync by a configurable number of ticks (default: 20 ticks = 1 second)
     * to ensure the client is ready to receive and render cosmetic data.
     *
     * <p><b>Thread Safety</b>: Uses ConcurrentHashMap to allow safe concurrent access from
     * multiple server threads (login events and tick events may occur on different threads).
     */
    private static final Map<UUID, DelayedSyncTask> pendingTasks = new ConcurrentHashMap<>();

    /**
     * Server tick counter for delayed task scheduling.
     *
     * <p><b>Thread Safety</b>: Uses AtomicLong for thread-safe increments without synchronization.
     * Incremented once per server tick in {@link #onServerTick(TickEvent.ServerTickEvent)}.
     */
    private static final AtomicLong serverTicks = new AtomicLong(0);

    /**
     * Represents a cosmetics sync task scheduled for future execution.
     *
     * <p>Tasks are immutable after creation to prevent race conditions.
     */
    private static class DelayedSyncTask {
        final UUID playerUUID;
        final long executeAtTick;

        DelayedSyncTask(UUID playerUUID, long executeAtTick) {
            this.playerUUID = playerUUID;
            this.executeAtTick = executeAtTick;
        }
    }

    /**
     * Mod constructor - initializes Twilight Lib and registers all systems.
     *
     * <p><b>Initialization Order</b>:
     * <ol>
     *   <li>Register configuration (accessible before game loads)</li>
     *   <li>Register entities, particles, and attributes on mod bus</li>
     *   <li>Initialize network packet handlers</li>
     *   <li>Attach event listeners for player events (login, respawn, tracking)</li>
     *   <li>Fetch supporter data asynchronously (non-blocking)</li>
     * </ol>
     *
     * <p><b>Why async supporter fetch?</b> The supporter list is fetched from GitHub on startup
     * to avoid blocking server startup. The first player to join will wait for the fetch to complete
     * via {@code SupporterService.fetchSupporters().join()} in {@link #onPlayerLogin}.
     */
    public TwilightLib() {
        LOGGER.info("Yes! This'll be fun! Right? Twilight Lib is loading...");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TwilightConfig.COMMON_CONFIG);

        // Detect loaded mods BEFORE registering cosmetics (critical order!)
        ModRequirement.detectMods();

        ModEntities.register(modBus);
        ModParticles.register(modBus);
        ModAttributes.register(modBus);
        modBus.addListener(this::onRegisterCapabilities);
        modBus.addListener(this::onEntityAttributeModification);
        NetworkHandler.init();

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachEntityCaps);
        MinecraftForge.EVENT_BUS.addListener(TwilightLibCommands::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(CosmeticsCommand::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogout);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerStartTracking);
        MinecraftForge.EVENT_BUS.addListener(TwilightLib::onServerTick);

        // Fetch supporter list on startup (async)
        SupporterService.fetchSupporters();

        LOGGER.info("Hi! My name is Zoe. Twilight Lib loaded successfully.");
    }

    private void onRegisterCapabilities(final RegisterCapabilitiesEvent evt) {
        evt.register(IMorph.class);
        evt.register(IAddons.class);
        evt.register(ITrails.class);
        evt.register(IEffects.class);
        evt.register(IModelVariant.class);
    }

    private void onEntityAttributeModification(final EntityAttributeModificationEvent evt) {
        // Add custom attributes to all living entities (especially players)
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.MINING_PENALTY.get());
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.FOV_MODIFIER.get());
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_HELMET.get());
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_CHESTPLATE.get());
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_LEGGINGS.get());
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_BOOTS.get());
        LOGGER.debug("Come on, this is gonna be fun! Added custom attributes to players.");
    }

    private void attachEntityCaps(final AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof Player) {
            // Attach morph capability
            MorphProvider morphProvider = new MorphProvider();
            evt.addCapability(new ResourceLocation(MODID, "morph"), morphProvider);
            evt.addListener(morphProvider::invalidate);

            // Attach addons capability
            AddonsProvider addonsProvider = new AddonsProvider();
            evt.addCapability(new ResourceLocation(MODID, "addons"), addonsProvider);
            evt.addListener(addonsProvider::invalidate);

            // Attach trails capability
            TrailsProvider trailsProvider = new TrailsProvider();
            evt.addCapability(new ResourceLocation(MODID, "trails"), trailsProvider);
            evt.addListener(trailsProvider::invalidate);

            // Attach effects capability
            EffectsProvider effectsProvider = new EffectsProvider();
            evt.addCapability(new ResourceLocation(MODID, "effects"), effectsProvider);
            evt.addListener(effectsProvider::invalidate);

            // Attach model variant capability
            ModelVariantProvider modelVariantProvider = new ModelVariantProvider();
            evt.addCapability(new ResourceLocation(MODID, "model_variant"), modelVariantProvider);
            evt.addListener(modelVariantProvider::invalidate);
        }
    }

    /**
     * Handles player login - loads persisted cosmetics, syncs with supporter tier, and broadcasts to clients.
     *
     * <p><b>Execution Order (Critical!)</b>:
     * <ol>
     *   <li>Restore cosmetics from persistent NBT (preserves player's previous state)</li>
     *   <li>Wait for supporter data fetch to complete (blocking via {@code join()})</li>
     *   <li>Sync owned cosmetics with current supporter tier (grants/revokes based on tier)</li>
     *   <li>Broadcast cosmetics to all tracking clients immediately</li>
     *   <li>Schedule delayed full sync to logged-in player (ensures client entity is loaded)</li>
     * </ol>
     *
     * <p><b>Why restore BEFORE supporter sync?</b> Supporter tier may have changed since last login.
     * By loading NBT first, we preserve the player's active selection (e.g., active trail choice),
     * then sync ownership with current tier. If they lost access to a cosmetic, it's deactivated
     * but their other selections are preserved.
     *
     * <p><b>Why block on supporter fetch?</b> The first player to join after server startup will
     * wait for the GitHub fetch to complete. Subsequent players use the cached data.
     * This prevents race conditions where cosmetics are granted before supporter data loads.
     *
     * <p><b>Supporter Tier Sync Logic</b>:
     * <ul>
     *   <li><b>Trails</b>: CLEARED and re-granted on each login (prevents tier drift)</li>
     *   <li><b>Addons</b>: Granted additively, removed selectively (preserves admin grants)</li>
     *   <li><b>Effects</b>: Granted additively, removed selectively (preserves admin grants)</li>
     * </ul>
     *
     * @param evt The PlayerLoggedInEvent containing the player entity
     */
    private void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent evt) {
        Player loggedInPlayer = evt.getEntity();
        if (loggedInPlayer.level().isClientSide) return;

        LOGGER.info("I wanna have fun and chat with someone besides myself! Syncing morphs and addons for {}", loggedInPlayer.getGameProfile().getName());

        // Load persisted data from NBT FIRST (before supporter checks modify it)
        CompoundTag persistentData = loggedInPlayer.getPersistentData();

        if (persistentData.contains(TwilightConstants.NBT_MORPH, CompoundTag.TAG_COMPOUND)) {
            loggedInPlayer.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
                morph.deserialize(persistentData.getCompound(TwilightConstants.NBT_MORPH));
                LOGGER.debug("Come on, this is gonna be fun! Restored morph from NBT for {}", loggedInPlayer.getGameProfile().getName());
            });
        }

        if (persistentData.contains(TwilightConstants.NBT_ADDONS, CompoundTag.TAG_COMPOUND)) {
            loggedInPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                addons.deserialize(persistentData.getCompound(TwilightConstants.NBT_ADDONS));
                LOGGER.debug("Come on, this is gonna be fun! Restored {} addons from NBT for {}", addons.getAddons().size(), loggedInPlayer.getGameProfile().getName());
            });
        }

        if (persistentData.contains(TwilightConstants.NBT_TRAILS, CompoundTag.TAG_COMPOUND)) {
            loggedInPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                trails.deserialize(persistentData.getCompound(TwilightConstants.NBT_TRAILS));
                LOGGER.debug("Come on, this is gonna be fun! Restored {} trails from NBT for {}", trails.getTrails().size(), loggedInPlayer.getGameProfile().getName());
            });
        }

        if (persistentData.contains(TwilightConstants.NBT_EFFECTS, CompoundTag.TAG_COMPOUND)) {
            loggedInPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                effects.deserialize(persistentData.getCompound(TwilightConstants.NBT_EFFECTS));
                LOGGER.debug("Come on, this is gonna be fun! Restored {} effects from NBT for {}", effects.getEffects().size(), loggedInPlayer.getGameProfile().getName());
            });
        }

        if (persistentData.contains(TwilightConstants.NBT_MODEL_VARIANT, CompoundTag.TAG_COMPOUND)) {
            loggedInPlayer.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
                modelVariant.deserialize(persistentData.getCompound(TwilightConstants.NBT_MODEL_VARIANT));
                LOGGER.debug("Come on, this is gonna be fun! Restored model variant from NBT for {}", loggedInPlayer.getGameProfile().getName());
            });
        }

        // TODO CRITICAL: This blocking .get() call freezes the entire server during player login!
        // This is a DoS vector - repeated logins can cause server lag for all players.
        // FIX: Replace with non-blocking check using isDone() or thenAcceptAsync() callback
        // See audit issue: "Blocking Call on Login (Lines 290-296)"
        // Ensure supporter data is loaded before checking (waits if fetch is in progress)
        try {
            SupporterService.fetchSupporters().get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.warn("How did I?! Uuuughh! Supporter data fetch timed out after 10 seconds. Proceeding without supporter sync.");
        } catch (Exception e) {
            LOGGER.warn("Shoot! Error waiting for supporter data: {}", e.getMessage());
        }

        // Check supporter status and auto-grant cosmetics (tier unlocks + manual overrides)
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
            loggedInPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                // Sync owned trails with current supporter status (preserves persistent active state)
                Set<String> currentOwned = new java.util.HashSet<>(trails.getTrails());

                // Type-safe cast to access implementation-specific methods
                if (!(trails instanceof TrailsData)) {
                    LOGGER.error("Is this the best physical representation you can manifest? Another mod replaced ITrails capability with incompatible implementation: {}. Expected: TrailsData, Got: {}",
                        trails.getClass().getName(), trails.getClass().getSuperclass().getName());
                    LOGGER.error("Or, what. Skipping trail sync for {} - capability incompatibility detected", loggedInPlayer.getGameProfile().getName());
                    return;
                }
                TrailsData trailsData = (TrailsData) trails;

                // Remove trails no longer granted (ownership only - doesn't affect persistent active)
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

                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());
            });

            // Auto-grant addons (tier unlocks + manual overrides)
            loggedInPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                // Sync owned addons with current supporter status (preserves persistent active state)
                Set<String> currentOwned = new java.util.HashSet<>(addons.getAddons());

                // Type-safe cast to access implementation-specific methods
                if (!(addons instanceof AddonsData)) {
                    LOGGER.error("Is this the best physical representation you can manifest? Another mod replaced IAddons capability with incompatible implementation: {}. Expected: AddonsData, Got: {}",
                        addons.getClass().getName(), addons.getClass().getSuperclass().getName());
                    LOGGER.error("Or, what. Skipping addon sync for {} - capability incompatibility detected", loggedInPlayer.getGameProfile().getName());
                    return;
                }
                AddonsData addonsData = (AddonsData) addons;

                // Remove addons no longer granted (ownership only - doesn't affect persistent active)
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

                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());
            });

            // Auto-grant effects (tier unlocks + manual overrides)
            loggedInPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                // Sync owned effects with current supporter status (preserves persistent active state)
                Set<String> currentOwned = new java.util.HashSet<>(effects.getEffects());

                // Type-safe cast to access implementation-specific methods
                if (!(effects instanceof EffectsData)) {
                    LOGGER.error("Is this the best physical representation you can manifest? Another mod replaced IEffects capability with incompatible implementation: {}. Expected: EffectsData, Got: {}",
                        effects.getClass().getName(), effects.getClass().getSuperclass().getName());
                    LOGGER.error("Or, what. Skipping effect sync for {} - capability incompatibility detected", loggedInPlayer.getGameProfile().getName());
                    return;
                }
                EffectsData effectsData = (EffectsData) effects;

                // Remove effects no longer granted (ownership only - doesn't affect persistent active)
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

                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());
            });

            LOGGER.info("You need more stardust, I could give you some! Auto-granted {} trails, {} addons, {} effects to {}",
                    allTrails.size(), allAddons.size(), allEffects.size(),
                    loggedInPlayer.getGameProfile().getName());
        }

        // Schedule delayed cosmetics sync to allow client entity loading
        long delayTicks = TwilightConfig.LOGIN_SYNC_DELAY_TICKS.get();
        pendingTasks.put(loggedInPlayer.getUUID(), new DelayedSyncTask(loggedInPlayer.getUUID(), serverTicks.get() + delayTicks));

        // Send this player's morph to everyone else
        loggedInPlayer.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(loggedInPlayer.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                // Force dimension refresh to apply morph hitbox immediately
                loggedInPlayer.refreshDimensions();
                // Persist morph state to NBT to prevent data loss on logout
                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_MORPH, morph.serialize());
                LOGGER.info("We are going to be best friends! Player {} logged in with morph: {}", loggedInPlayer.getGameProfile().getName(), rl);
            });
        });

        // Send this player's active addons to everyone else
        loggedInPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(loginAddons -> {
            if (!loginAddons.getActiveAddons().isEmpty()) {
                NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(loggedInPlayer.getUUID(), loginAddons.getActiveAddons(), loginAddons.getAllAddonTints()));
                LOGGER.info("We are going to be best friends! Player {} logged in with {} active addons", loggedInPlayer.getGameProfile().getName(), loginAddons.getActiveAddons().size());
            }
        });

        // Send this player's active trails to everyone else
        loggedInPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(loginTrails -> {
            if (!loginTrails.getActiveTrails().isEmpty()) {
                NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(loggedInPlayer.getUUID(), loginTrails.getActiveTrails()));
                LOGGER.info("We are going to be best friends! Player {} logged in with {} active trails", loggedInPlayer.getGameProfile().getName(), loginTrails.getActiveTrails().size());
            }
        });

        // Send this player's effects to everyone else (trigger spawn effect on login)
        loggedInPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(loggedInPlayer.getUUID(), effects.getActiveEffects(), true));
            }
        });

        // Send this player's model variant to everyone else
        loggedInPlayer.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(loggedInPlayer.getUUID(), modelVariant));
            LOGGER.info("We are going to be best friends! Player {} logged in as {} model variant",
                loggedInPlayer.getGameProfile().getName(), modelVariant.getModelVariant());
        });
    }

    /**
     * Handles player logout - explicitly invalidates all capabilities to prevent memory leaks.
     *
     * <p><b>Why explicit invalidation?</b> Forge capabilities use LazyOptional<T> which can hold
     * references to capability instances. While the {@code AttachCapabilitiesEvent} adds invalidation
     * listeners, they may not trigger reliably on all logout scenarios (crashes, forcekicks, etc.).
     * This explicit cleanup ensures LazyOptionals are properly released.
     *
     * <p><b>Memory Leak Prevention</b>: Without invalidation, LazyOptionals can retain references
     * to capability providers, preventing garbage collection. On long-running servers with many
     * player join/disconnect cycles, this can accumulate and cause memory issues.
     *
     * @param evt The PlayerLoggedOutEvent containing the disconnecting player
     */
    private void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent evt) {
        Player player = evt.getEntity();
        if (player.level().isClientSide) return;

        // Clean up pending sync tasks for this player (prevent memory leak)
        // Using computeIfPresent for atomic removal (prevents race with tick handler)
        UUID playerUUID = player.getUUID();
        pendingTasks.computeIfPresent(playerUUID, (uuid, task) -> {
            LOGGER.debug("Goodbye, my new friend! Cleaning up pending sync task for {} on logout", player.getGameProfile().getName());
            return null; // Returning null removes the entry atomically
        });

        // Force invalidate all capabilities to free LazyOptionals
        player.invalidateCaps();
        LOGGER.debug("Goodbye, my new friend! Cleaned up pending tasks and invalidated capabilities for disconnecting player: {}",
            player.getGameProfile().getName());
    }

    private void onPlayerClone(final PlayerEvent.Clone evt) {
        if (evt.getEntity().level().isClientSide) return;

        // Handle both death and dimension changes
        // Capabilities are copied/invalidated, so we must use persistent NBT data
        CompoundTag oldData = evt.getOriginal().getPersistentData();

        // Restore morph
        if (oldData.contains(TwilightConstants.NBT_MORPH, CompoundTag.TAG_COMPOUND)) {
            CompoundTag morphData = oldData.getCompound(TwilightConstants.NBT_MORPH);
            evt.getEntity().getCapability(MorphProvider.MORPH_CAP).ifPresent(newMorph -> {
                newMorph.deserialize(morphData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring morph from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_MORPH, morphData);
            });
        }

        // Restore addons
        if (oldData.contains(TwilightConstants.NBT_ADDONS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag addonsData = oldData.getCompound(TwilightConstants.NBT_ADDONS);
            evt.getEntity().getCapability(AddonsProvider.ADDONS_CAP).ifPresent(newAddons -> {
                newAddons.deserialize(addonsData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring addons from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_ADDONS, addonsData);
            });
        }

        // Restore trails
        if (oldData.contains(TwilightConstants.NBT_TRAILS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag trailsData = oldData.getCompound(TwilightConstants.NBT_TRAILS);
            evt.getEntity().getCapability(TrailsProvider.TRAILS_CAP).ifPresent(newTrails -> {
                newTrails.deserialize(trailsData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring trails from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_TRAILS, trailsData);
            });
        }

        // Restore effects
        if (oldData.contains(TwilightConstants.NBT_EFFECTS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag effectsData = oldData.getCompound(TwilightConstants.NBT_EFFECTS);
            evt.getEntity().getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(newEffects -> {
                newEffects.deserialize(effectsData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring effects from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_EFFECTS, effectsData);
            });
        }

        // Restore model variant
        if (oldData.contains(TwilightConstants.NBT_MODEL_VARIANT, CompoundTag.TAG_COMPOUND)) {
            CompoundTag modelVariantData = oldData.getCompound(TwilightConstants.NBT_MODEL_VARIANT);
            evt.getEntity().getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(newModelVariant -> {
                newModelVariant.deserialize(modelVariantData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring model variant from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_MODEL_VARIANT, modelVariantData);
            });
        }
    }

    private void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent evt) {
        Player player = evt.getEntity();
        if (player.level().isClientSide) return;

        // Sync morph to client after respawn (when client-side player entity exists)
        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(player.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                player.refreshDimensions();
                // Persist morph state to NBT to prevent data loss
                player.getPersistentData().put(TwilightConstants.NBT_MORPH, morph.serialize());
                LOGGER.debug("Time to change! Player {} respawned as {}", player.getGameProfile().getName(), rl);
            });
        });

        // Sync active addons to client after respawn
        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (!addons.getActiveAddons().isEmpty()) {
                NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons(), addons.getAllAddonTints()));
                LOGGER.debug("Time to change! Player {} respawned with {} active addons", player.getGameProfile().getName(), addons.getActiveAddons().size());
            }
        });

        // Sync active trails to client after respawn
        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            if (!trails.getActiveTrails().isEmpty()) {
                NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.getActiveTrails()));
                LOGGER.debug("Time to change! Player {} respawned with {} active trails", player.getGameProfile().getName(), trails.getActiveTrails().size());
            }
        });

        // Sync effects to client after respawn (trigger spawn effect on respawn)
        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects(), true));
                LOGGER.debug("Time to change! Player {} respawned with {} active effects", player.getGameProfile().getName(), effects.getActiveEffects().size());
            }
        });

        // Sync model variant to client after respawn
        player.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(player.getUUID(), modelVariant));
            LOGGER.debug("Time to change! Player {} respawned as {} model", player.getGameProfile().getName(), modelVariant.getModelVariant());
        });
    }

    /**
     * Handles player dimension change - syncs cosmetics to clients after traveling to a new dimension.
     *
     * <p><b>Why this handler?</b> When a player changes dimensions (e.g., Overworld → Nether),
     * the client needs to be re-synced with all cosmetic data. The {@link #onPlayerClone} event
     * restores data from NBT, but doesn't broadcast to clients. This handler ensures:
     * <ul>
     *   <li>The player's cosmetics are visible in the new dimension</li>
     *   <li>Other players in the new dimension can see the arriving player's cosmetics</li>
     *   <li>Spawn effects trigger on dimension entry</li>
     * </ul>
     *
     * <p><b>Forge 1.20.1 Dimension Change Bug Workaround</b>: In Forge 1.20.1, capabilities are
     * invalidated during dimension changes and not automatically restored. PlayerEvent.Clone only
     * fires for death, not dimension changes. This handler detects missing capabilities and
     * manually restores them from persistent NBT before syncing to clients.
     *
     * @param evt The PlayerChangedDimensionEvent containing the player and dimension info
     */
    private void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent evt) {
        Player player = evt.getEntity();
        if (player.level().isClientSide) return;

        LOGGER.debug("Time to change! Player {} changed dimensions from {} to {}",
            player.getGameProfile().getName(), evt.getFrom(), evt.getTo());

        // Capability providers now automatically recreate LazyOptionals when accessed after invalidation
        // This fixes the Forge 1.20.1 dimension change bug where capabilities were invalidated during travel
        // Simply sync all cosmetics to clients - providers handle the recreation internally

        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(player.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                player.refreshDimensions();
                LOGGER.debug("Time to change! Player {} entered {} as {}", player.getGameProfile().getName(), evt.getTo().location(), rl);
            });
        });

        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (!addons.getActiveAddons().isEmpty()) {
                NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons(), addons.getAllAddonTints()));
                LOGGER.debug("Time to change! Player {} entered {} with {} active addons",
                    player.getGameProfile().getName(), evt.getTo().location(), addons.getActiveAddons().size());
            }
        });

        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            if (!trails.getActiveTrails().isEmpty()) {
                NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.getActiveTrails()));
                LOGGER.debug("Time to change! Player {} entered {} with {} active trails",
                    player.getGameProfile().getName(), evt.getTo().location(), trails.getActiveTrails().size());
            }
        });

        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects(), true));
                LOGGER.debug("Time to change! Player {} entered {} with {} active effects",
                    player.getGameProfile().getName(), evt.getTo().location(), effects.getActiveEffects().size());
            }
        });

        player.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(player.getUUID(), modelVariant));
            LOGGER.debug("Time to change! Player {} entered {} as {} model",
                player.getGameProfile().getName(), evt.getTo().location(), modelVariant.getModelVariant());
        });
    }

    private void onPlayerStartTracking(final PlayerEvent.StartTracking evt) {
        // When player A starts tracking entity B, if B is a player, send B's cosmetics to A
        if (!(evt.getTarget() instanceof ServerPlayer trackedPlayer)) return;
        if (!(evt.getEntity() instanceof ServerPlayer trackingPlayer)) return;
        if (trackingPlayer.level().isClientSide) return;

        // Send the tracked player's cosmetics to the tracking player
        trackedPlayer.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendToPlayer(trackingPlayer, SyncMorphPacket.of(trackedPlayer.getUUID(), Optional.of(rl), morph.isNametagHidden()));
                LOGGER.debug("Here you go! Sent morph {} for {} to tracking player {}",
                    rl, trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
            });
        });

        trackedPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (!addons.getActiveAddons().isEmpty()) {
                NetworkHandler.sendAddonsToPlayer(trackingPlayer, new SyncAddonsPacket(trackedPlayer.getUUID(), addons.getActiveAddons(), addons.getAllAddonTints()));
                LOGGER.debug("Here you go! Sent {} addons for {} to tracking player {}",
                    addons.getActiveAddons().size(), trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
            }
        });

        trackedPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            if (!trails.getActiveTrails().isEmpty()) {
                NetworkHandler.sendTrailsToPlayer(trackingPlayer, new SyncTrailsPacket(trackedPlayer.getUUID(), trails.getActiveTrails()));
                LOGGER.debug("Here you go! Sent {} trails for {} to tracking player {}",
                    trails.getActiveTrails().size(), trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
            }
        });

        trackedPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToPlayer(trackingPlayer, new SyncEffectsPacket(trackedPlayer.getUUID(), effects.getActiveEffects(), false));
                LOGGER.debug("Here you go! Sent {} effects for {} to tracking player {}",
                    effects.getActiveEffects().size(), trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
            }
        });

        trackedPlayer.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
            NetworkHandler.sendModelVariantToPlayer(trackingPlayer, SyncModelVariantPacket.of(trackedPlayer.getUUID(), modelVariant));
            LOGGER.debug("Here you go! Sent model variant {} for {} to tracking player {}",
                modelVariant.getModelVariant(), trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
        });
    }

    /**
     * Server tick handler - processes delayed cosmetics sync tasks.
     *
     * <p><b>Why delayed sync?</b> When a player logs in, their client needs time to:
     * <ol>
     *   <li>Create the client-side player entity</li>
     *   <li>Initialize rendering systems</li>
     *   <li>Load nearby chunks and entities</li>
     * </ol>
     *
     * Sending cosmetics data immediately can cause:
     * <ul>
     *   <li>Lost packets (client not ready to receive)</li>
     *   <li>Visual glitches (rendering systems not initialized)</li>
     *   <li>Missing cosmetics for other players (tracking not established)</li>
     * </ul>
     *
     * <p><b>How it works</b>:
     * <ol>
     *   <li>On player login, schedule a task for {@code currentTick + delay}</li>
     *   <li>Every tick, check if any tasks are ready (current tick >= execute tick)</li>
     *   <li>Send full cosmetics sync to the logged-in player (all other players' cosmetics)</li>
     *   <li>Remove completed task from queue</li>
     * </ol>
     *
     * <p><b>Thread Safety</b>: Uses iterator.remove() to safely remove tasks during iteration.
     * ConcurrentHashMap prevents ConcurrentModificationException if new tasks are added during iteration.
     *
     * <p><b>Performance</b>: Early return if no tasks pending. Iterator allocation only when needed.
     *
     * @param evt The ServerTickEvent (only processes during END phase)
     */
    private static void onServerTick(final TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return; // Only process at end of tick
        long currentTick = serverTicks.incrementAndGet();

        // Process pending delayed sync tasks
        if (pendingTasks.isEmpty()) return;

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
                    LOGGER.debug("While I wait, I will stay happy! Delayed cosmetics sync complete for {}", player.getGameProfile().getName());
                } else {
                    LOGGER.debug("Goodbye, my new friend! Player left before delayed sync could complete");
                }

                // Remove completed task
                iterator.remove();
            }
        }
    }
}