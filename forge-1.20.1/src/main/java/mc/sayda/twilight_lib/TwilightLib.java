package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.capabilities.IAddons;
import mc.sayda.twilight_lib.capabilities.IEffects;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ITrails;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.commands.CosmeticsCommand;
import mc.sayda.twilight_lib.commands.TwilightLibCommands;
import mc.sayda.twilight_lib.config.TwilightConfig;
import mc.sayda.twilight_lib.entity.ModEntities;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.SyncEffectsPacket;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod(TwilightLib.MODID)
public class TwilightLib {
    public static final String MODID = "twilight_lib";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Delayed task scheduler for proper tick-based delays
    private static final Map<UUID, DelayedSyncTask> pendingTasks = new ConcurrentHashMap<>();
    private static long serverTicks = 0;

    private static class DelayedSyncTask {
        final UUID playerUUID;
        final long executeAtTick;

        DelayedSyncTask(UUID playerUUID, long executeAtTick) {
            this.playerUUID = playerUUID;
            this.executeAtTick = executeAtTick;
        }
    }

    public TwilightLib() {
        LOGGER.info("Yes! This'll be fun! Right? Twilight Lib is loading...");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TwilightConfig.COMMON_CONFIG);

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
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);
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
    }

    private void onEntityAttributeModification(final EntityAttributeModificationEvent evt) {
        // Add custom attributes to all living entities (especially players)
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.MINING_PENALTY.get());
        evt.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.FOV_MODIFIER.get());
        LOGGER.debug("It's something new! Added MINING_PENALTY and FOV_MODIFIER attributes to players.");
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
        }
    }

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
                LOGGER.debug("All the neat things! Restored {} addons from NBT for {}", addons.getAddons().size(), loggedInPlayer.getGameProfile().getName());
            });
        }

        if (persistentData.contains(TwilightConstants.NBT_TRAILS, CompoundTag.TAG_COMPOUND)) {
            loggedInPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                trails.deserialize(persistentData.getCompound(TwilightConstants.NBT_TRAILS));
                LOGGER.debug("Sparkles everywhere! Restored {} trails from NBT for {}", trails.getTrails().size(), loggedInPlayer.getGameProfile().getName());
            });
        }

        if (persistentData.contains(TwilightConstants.NBT_EFFECTS, CompoundTag.TAG_COMPOUND)) {
            loggedInPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                effects.deserialize(persistentData.getCompound(TwilightConstants.NBT_EFFECTS));
                LOGGER.debug("Magic is in the air! Restored {} effects from NBT for {}", effects.getEffects().size(), loggedInPlayer.getGameProfile().getName());
            });
        }

        // Ensure supporter data is loaded before checking (waits if fetch is in progress)
        SupporterService.fetchSupporters().join();  // Block until fetch completes

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
                LOGGER.debug("Tier '{}' grants trails: {}", data.getTier(), allTrails);
            } else {
                LOGGER.info("I have a gift for you! {} has manual cosmetic grants (expired/gift supporter)",
                    loggedInPlayer.getGameProfile().getName());
            }

            // Auto-grant trails (tier unlocks + manual overrides)
            loggedInPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                // Preserve active trail selection and enabled state
                String activeTrail = trails.getActiveTrail();
                boolean trailEnabled = trails.isTrailEnabled();

                // Sync owned trails with current supporter status (preserves admin grants)
                Set<String> currentOwned = new java.util.HashSet<>(trails.getTrails());

                // Remove trails no longer granted (selective removal preserves admin grants)
                for (String trail : currentOwned) {
                    if (!allTrails.contains(trail)) {
                        trails.removeTrail(trail);  // Will also clear active trail if this was it
                    }
                }

                // Add newly granted trails
                for (String trail : allTrails) {
                    if (!currentOwned.contains(trail)) {
                        trails.addTrail(trail);
                    }
                }

                // Restore active trail if still owned, otherwise explicitly clear it
                if (activeTrail != null && trails.hasTrail(activeTrail)) {
                    trails.setActiveTrail(activeTrail);
                } else {
                    trails.setActiveTrail(null); // Clear invalid trail (no longer owned)
                }
                trails.setTrailEnabled(trailEnabled);

                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());
            });

            // Auto-grant addons (tier unlocks + manual overrides)
            loggedInPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                // Sync owned addons with current supporter status (preserves persistent active state)
                Set<String> currentOwned = new java.util.HashSet<>(addons.getAddons());

                // Type-safe cast to access implementation-specific methods
                if (!(addons instanceof AddonsData)) {
                    LOGGER.error("Is this the best physical representation you can manifest? It completely lacks zazz! Unexpected addons capability implementation: {}", addons.getClass());
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
                    LOGGER.error("Is this the best physical representation you can manifest? It completely lacks zazz! Unexpected effects capability implementation: {}", effects.getClass());
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

        // Schedule delayed cosmetics sync (40 ticks = 2 seconds) to allow client entity loading
        pendingTasks.put(loggedInPlayer.getUUID(), new DelayedSyncTask(loggedInPlayer.getUUID(), serverTicks + 40));

        // Send this player's morph to everyone else
        loggedInPlayer.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(loggedInPlayer.getUUID(), rl));
                // Force dimension refresh to apply morph hitbox immediately
                loggedInPlayer.refreshDimensions();
                // Persist morph state to NBT to prevent data loss on logout
                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_MORPH, morph.serialize());
                LOGGER.info("I wanna have fun and chat with someone besides myself! Player {} logged in with morph: {}", loggedInPlayer.getGameProfile().getName(), rl);
            });
        });

        // Send this player's active addons to everyone else
        loggedInPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(loginAddons -> {
            if (!loginAddons.getActiveAddons().isEmpty()) {
                NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(loggedInPlayer.getUUID(), loginAddons.getActiveAddons()));
                LOGGER.info("We're gonna be best friends! Player {} logged in with {} active addons", loggedInPlayer.getGameProfile().getName(), loginAddons.getActiveAddons().size());
            }
        });

        // Send this player's trails to everyone else
        loggedInPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(loggedInPlayer.getUUID(), trails.serialize()));
        });

        // Send this player's effects to everyone else (trigger spawn effect on login)
        loggedInPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(loggedInPlayer.getUUID(), effects.getActiveEffects(), true));
            }
        });
    }

    private void onPlayerClone(final PlayerEvent.Clone evt) {
        if (evt.getEntity().level().isClientSide) return;
        if (!evt.isWasDeath()) return; // Only handle death, not dimension change

        // Capabilities are invalidated on death, so we must use persistent NBT data
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
                LOGGER.debug("Here you go! Restoring addons from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_ADDONS, addonsData);
            });
        }

        // Restore trails
        if (oldData.contains(TwilightConstants.NBT_TRAILS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag trailsData = oldData.getCompound(TwilightConstants.NBT_TRAILS);
            evt.getEntity().getCapability(TrailsProvider.TRAILS_CAP).ifPresent(newTrails -> {
                newTrails.deserialize(trailsData);
                LOGGER.debug("More sparkles, now! Restoring trails from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_TRAILS, trailsData);
            });
        }

        // Restore effects
        if (oldData.contains(TwilightConstants.NBT_EFFECTS, CompoundTag.TAG_COMPOUND)) {
            CompoundTag effectsData = oldData.getCompound(TwilightConstants.NBT_EFFECTS);
            evt.getEntity().getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(newEffects -> {
                newEffects.deserialize(effectsData);
                LOGGER.debug("Yes, more magic! Restoring effects from death.");
                evt.getEntity().getPersistentData().put(TwilightConstants.NBT_EFFECTS, effectsData);
            });
        }
    }

    private void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent evt) {
        Player player = evt.getEntity();
        if (player.level().isClientSide) return;

        // Sync morph to client after respawn (when client-side player entity exists)
        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(player.getUUID(), rl));
                player.refreshDimensions();
                // Persist morph state to NBT to prevent data loss
                player.getPersistentData().put(TwilightConstants.NBT_MORPH, morph.serialize());
                LOGGER.debug("The wheel turns, day becomes night... Player {} respawned as {}", player.getGameProfile().getName(), rl);
            });
        });

        // Sync active addons to client after respawn
        player.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (!addons.getActiveAddons().isEmpty()) {
                NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons()));
                LOGGER.debug("Aaand a skip-skip and a jump-jump! Player {} respawned with {} active addons", player.getGameProfile().getName(), addons.getActiveAddons().size());
            }
        });

        // Sync trails to client after respawn
        player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.serialize()));
            LOGGER.debug("Something good is going to happen. With sparkles! Player {} respawned with trails", player.getGameProfile().getName());
        });

        // Sync effects to client after respawn (trigger spawn effect on respawn)
        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects(), true));
                LOGGER.debug("Aw, this spell is neat! Player {} respawned with {} active effects", player.getGameProfile().getName(), effects.getActiveEffects().size());
            }
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
                NetworkHandler.sendToPlayer(trackingPlayer, SyncMorphPacket.of(trackedPlayer.getUUID(), rl));
                LOGGER.debug("Peek-a-boo! Sent morph {} for {} to tracking player {}",
                    rl, trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
            });
        });

        trackedPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (!addons.getActiveAddons().isEmpty()) {
                NetworkHandler.sendAddonsToPlayer(trackingPlayer, new SyncAddonsPacket(trackedPlayer.getUUID(), addons.getActiveAddons()));
                LOGGER.debug("More friends! Sent {} addons for {} to tracking player {}",
                    addons.getActiveAddons().size(), trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
            }
        });

        trackedPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            NetworkHandler.sendTrailsToPlayer(trackingPlayer, new SyncTrailsPacket(trackedPlayer.getUUID(), trails.serialize()));
        });

        trackedPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToPlayer(trackingPlayer, new SyncEffectsPacket(trackedPlayer.getUUID(), effects.getActiveEffects(), false));
                LOGGER.debug("Magic everywhere! Sent {} effects for {} to tracking player {}",
                    effects.getActiveEffects().size(), trackedPlayer.getGameProfile().getName(), trackingPlayer.getGameProfile().getName());
            }
        });
    }

    private static void onServerTick(final TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return; // Only process at end of tick
        serverTicks++;

        // Process pending delayed sync tasks
        if (pendingTasks.isEmpty()) return;

        MinecraftServer server = evt.getServer();
        Iterator<Map.Entry<UUID, DelayedSyncTask>> iterator = pendingTasks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, DelayedSyncTask> entry = iterator.next();
            DelayedSyncTask task = entry.getValue();

            if (serverTicks >= task.executeAtTick) {
                // Time to execute this task
                ServerPlayer player = server.getPlayerList().getPlayer(task.playerUUID);
                if (player != null && !player.isRemoved()) {
                    NetworkHandler.sendAllMorphsToPlayer(player);
                    NetworkHandler.sendAllAddonsToPlayer(player);
                    NetworkHandler.sendAllTrailsToPlayer(player);
                    NetworkHandler.sendAllEffectsToPlayer(player);
                    LOGGER.debug("Time passes differently here. Delayed cosmetics sync complete for {}", player.getGameProfile().getName());
                } else {
                    LOGGER.debug("Player left before delayed sync could complete");
                }

                // Remove completed task
                iterator.remove();
            }
        }
    }
}