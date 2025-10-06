package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.commands.TwilightLibCommands;
import mc.sayda.twilight_lib.entity.ModEntities;
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncMorphPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(TwilightLib.MODID)
public class TwilightLib {
    public static final String MODID = "twilight_lib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TwilightLib() {
        LOGGER.info("Yes! This'll be fun! Right? Twilight Lib is loading...");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.register(modBus);
        modBus.addListener(this::onRegisterCapabilities);
        NetworkHandler.init();

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachEntityCaps);
        MinecraftForge.EVENT_BUS.addListener(TwilightLibCommands::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(mc.sayda.twilight_lib.commands.AddonCommands::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);

        LOGGER.info("Hi! My name is Zoe. Twilight Lib loaded successfully.");
    }

    private void onRegisterCapabilities(final RegisterCapabilitiesEvent evt) {
        evt.register(IMorph.class);
        evt.register(mc.sayda.twilight_lib.capabilities.IPlayerAddons.class);
    }

    private void attachEntityCaps(final AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof Player) {
            // Attach morph capability
            MorphProvider morphProvider = new MorphProvider();
            evt.addCapability(new ResourceLocation(MODID, "morph"), morphProvider);
            evt.addListener(morphProvider::invalidate);

            // Attach addons capability
            mc.sayda.twilight_lib.capabilities.PlayerAddonsProvider addonsProvider = new mc.sayda.twilight_lib.capabilities.PlayerAddonsProvider();
            evt.addCapability(new ResourceLocation(MODID, "addons"), addonsProvider);
            evt.addListener(addonsProvider::invalidate);
        }
    }

    private void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent evt) {
        Player loggedInPlayer = evt.getEntity();
        if (loggedInPlayer.level().isClientSide) return;

        LOGGER.info("New friend detected! Syncing morphs and addons for {}", loggedInPlayer.getGameProfile().getName());

        // Send all existing morphs to the newly logged-in player
        NetworkHandler.sendAllMorphsToPlayer(loggedInPlayer);

        // Send all existing addons to the newly logged-in player
        NetworkHandler.sendAllAddonsToPlayer(loggedInPlayer);

        // Send this player's morph to everyone else
        loggedInPlayer.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendToAll(SyncMorphPacket.of(loggedInPlayer.getUUID(), rl));
                // Force dimension refresh to apply morph hitbox immediately
                loggedInPlayer.refreshDimensions();
                LOGGER.info("I wanna have fun and chat with someone besides myself! Player {} logged in with morph: {}", loggedInPlayer.getGameProfile().getName(), rl);
            });
        });

        // Send this player's addons to everyone else
        loggedInPlayer.getCapability(mc.sayda.twilight_lib.capabilities.PlayerAddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (!addons.getAddons().isEmpty()) {
                NetworkHandler.sendAddonsToAll(new mc.sayda.twilight_lib.network.SyncAddonsPacket(loggedInPlayer.getUUID(), addons.getAddons()));
                LOGGER.info("We're gonna be best friends! Player {} logged in with {} addons", loggedInPlayer.getGameProfile().getName(), addons.getAddons().size());
            }
        });
    }

    private void onPlayerClone(final PlayerEvent.Clone evt) {
        if (evt.getEntity().level().isClientSide) return;
        if (!evt.isWasDeath()) return; // Only handle death, not dimension change

        // Capabilities are invalidated on death, so we must use persistent NBT data
        CompoundTag oldData = evt.getOriginal().getPersistentData();

        // Restore morph
        if (oldData.contains("TwilightLibMorph", CompoundTag.TAG_COMPOUND)) {
            CompoundTag morphData = oldData.getCompound("TwilightLibMorph");
            evt.getEntity().getCapability(MorphProvider.MORPH_CAP).ifPresent(newMorph -> {
                newMorph.deserialize(morphData);
                LOGGER.debug("This will be fine! Things break all the time. Restoring morph from death.");
                evt.getEntity().getPersistentData().put("TwilightLibMorph", morphData);
            });
        }

        // Restore addons
        if (oldData.contains("TwilightLibAddons", CompoundTag.TAG_COMPOUND)) {
            CompoundTag addonsData = oldData.getCompound("TwilightLibAddons");
            evt.getEntity().getCapability(mc.sayda.twilight_lib.capabilities.PlayerAddonsProvider.ADDONS_CAP).ifPresent(newAddons -> {
                newAddons.deserialize(addonsData);
                LOGGER.debug("Naptime's over! Restoring addons from death.");
                evt.getEntity().getPersistentData().put("TwilightLibAddons", addonsData);
            });
        }
    }

    private void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent evt) {
        Player player = evt.getEntity();
        if (player.level().isClientSide) return;

        // Sync morph to client after respawn (when client-side player entity exists)
        player.getCapability(MorphProvider.MORPH_CAP).ifPresent(morph -> {
            morph.getEntityType().ifPresent(rl -> {
                NetworkHandler.sendToAll(SyncMorphPacket.of(player.getUUID(), rl));
                player.refreshDimensions();
                LOGGER.debug("The wheel turns, day becomes night... Player {} respawned as {}", player.getGameProfile().getName(), rl);
            });
        });

        // Sync addons to client after respawn
        player.getCapability(mc.sayda.twilight_lib.capabilities.PlayerAddonsProvider.ADDONS_CAP).ifPresent(addons -> {
            if (!addons.getAddons().isEmpty()) {
                NetworkHandler.sendAddonsToAll(new mc.sayda.twilight_lib.network.SyncAddonsPacket(player.getUUID(), addons.getAddons()));
                LOGGER.debug("Aaand a skip-skip and a jump-jump! Player {} respawned with {} addons", player.getGameProfile().getName(), addons.getAddons().size());
            }
        });
    }
}