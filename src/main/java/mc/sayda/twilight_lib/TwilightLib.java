package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

@Mod(TwilightLib.MODID)
public class TwilightLib {
    public static final String MODID = "twilight_lib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TwilightLib() {
        LOGGER.info("Yes! This'll be fun! Right? Twilight Lib is loading...");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TwilightConfig.COMMON_CONFIG);

        ModEntities.register(modBus);
        ModParticles.register(modBus);
        modBus.addListener(this::onRegisterCapabilities);
        NetworkHandler.init();

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachEntityCaps);
        MinecraftForge.EVENT_BUS.addListener(TwilightLibCommands::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(CosmeticsCommand::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);

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

                // Clear and re-grant trails to match current tier
                trails.clearTrails();
                for (String trail : allTrails) {
                    trails.addTrail(trail);
                }

                // Restore active trail if still owned, otherwise explicitly clear it
                if (activeTrail != null && trails.hasTrail(activeTrail)) {
                    trails.setActiveTrail(activeTrail);
                } else {
                    trails.setActiveTrail(null); // Clear invalid trail (stone tier or no longer owned)
                }
                trails.setTrailEnabled(trailEnabled);

                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_TRAILS, trails.serialize());
            });

            // Auto-grant addons (tier unlocks + manual overrides)
            loggedInPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                // Add addons from supporter tier (doesn't remove existing addons or active state)
                for (String addon : allAddons) {
                    addons.addAddon(addon);
                }
                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_ADDONS, addons.serialize());
            });

            // Auto-grant effects (tier unlocks + manual overrides)
            loggedInPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                // Add effects from supporter tier (doesn't remove existing effects or active state)
                for (String effect : allEffects) {
                    effects.addEffect(effect);
                }
                loggedInPlayer.getPersistentData().put(TwilightConstants.NBT_EFFECTS, effects.serialize());
            });

            LOGGER.info("You need more stardust, I could give you some! Auto-granted {} trails, {} addons, {} effects to {}",
                    allTrails.size(), allAddons.size(), allEffects.size(),
                    loggedInPlayer.getGameProfile().getName());
        }

        // Send all existing morphs to the newly logged-in player
        NetworkHandler.sendAllMorphsToPlayer(loggedInPlayer);

        // Send all existing addons to the newly logged-in player
        NetworkHandler.sendAllAddonsToPlayer(loggedInPlayer);

        // Send all existing trails to the newly logged-in player
        NetworkHandler.sendAllTrailsToPlayer(loggedInPlayer);

        // Send all existing effects to the newly logged-in player
        NetworkHandler.sendAllEffectsToPlayer(loggedInPlayer);

        // Send this player's morph to everyone else
        loggedInPlayer.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendMorphToAll(SyncMorphPacket.of(loggedInPlayer.getUUID(), rl));
                // Force dimension refresh to apply morph hitbox immediately
                loggedInPlayer.refreshDimensions();
                LOGGER.info("I wanna have fun and chat with someone besides myself! Player {} logged in with morph: {}", loggedInPlayer.getGameProfile().getName(), rl);
            });
        });

        // Send this player's active addons to everyone else
        loggedInPlayer.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (!addons.getActiveAddons().isEmpty()) {
                NetworkHandler.sendAddonsToAll(new SyncAddonsPacket(loggedInPlayer.getUUID(), addons.getActiveAddons()));
                LOGGER.info("We're gonna be best friends! Player {} logged in with {} active addons", loggedInPlayer.getGameProfile().getName(), addons.getActiveAddons().size());
            }
        });

        // Send this player's trails to everyone else
        loggedInPlayer.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(loggedInPlayer.getUUID(), trails.serialize()));
        });

        // Send this player's effects to everyone else
        loggedInPlayer.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(loggedInPlayer.getUUID(), effects.getActiveEffects()));
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
                LOGGER.debug("Naptime's over! Restoring addons from death.");
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

        // Sync effects to client after respawn
        player.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
            if (!effects.getActiveEffects().isEmpty()) {
                NetworkHandler.sendEffectsToAll(new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects()));
                LOGGER.debug("Aw, this spell is neat! Player {} respawned with {} active effects", player.getGameProfile().getName(), effects.getActiveEffects().size());
            }
        });
    }
}