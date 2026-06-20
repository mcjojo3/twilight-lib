package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.BiConsumer;

public class NetworkHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Overridden by TwilightLibFabric to use native Fabric networking (Architectury 13.0.8 send path is broken on Fabric).
    public static BiConsumer<ServerPlayer, CustomPacketPayload> PLATFORM_SEND_TO_PLAYER =
            (player, pkt) -> NetworkManager.sendToPlayer(player, pkt);

    public static void register() {
        // On Fabric, native Fabric networking APIs handle registration (TwilightLibFabric/TwilightLibFabricClient).
        // Architectury 13.0.8 has bugs in both registerReceiver and sendToPlayer on Fabric.
        if (!dev.architectury.platform.Platform.isFabric()) {
            registerReceivers();
        }
        LOGGER.info("What's your name? Network payloads registered.");
    }

    private static void registerReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncMorphPacket.TYPE, SyncMorphPacket.STREAM_CODEC,
                SyncMorphPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncAddonsPacket.TYPE, SyncAddonsPacket.STREAM_CODEC,
                SyncAddonsPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncTrailsPacket.TYPE, SyncTrailsPacket.STREAM_CODEC,
                SyncTrailsPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncEffectsPacket.TYPE, SyncEffectsPacket.STREAM_CODEC,
                SyncEffectsPacket::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncModelVariantPacket.TYPE,
                SyncModelVariantPacket.STREAM_CODEC, SyncModelVariantPacket::handle);
    }

    public static void sendMorphToAll(SyncMorphPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                for (var sp : server.getPlayerList().getPlayers()) {
                    PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
                }
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send morph packet to all players", e);
        }
    }

    public static void sendToPlayer(Player player, SyncMorphPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        try {
            PLATFORM_SEND_TO_PLAYER.accept(serverPlayer, pkt);
            LOGGER.debug("We are going to be best friends! Sending morph to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send morph to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllMorphsToPlayer(Player recipient) {
        if (recipient.level() == null) {
            return;
        }
        LOGGER.debug("We are going to be best friends! Syncing all morphs to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) {
                continue;
            }
            IMorph morph = DataUtils.getMorphData(p);
            if (morph != null) {
                morph.getEntityType().ifPresent(rl -> sendToPlayer(recipient,
                        SyncMorphPacket.of(p.getUUID(), Optional.of(rl), morph.isNametagHidden())));
            }
        }
    }

    public static void sendAddonsToAll(SyncAddonsPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                for (var sp : server.getPlayerList().getPlayers()) {
                    PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
                }
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send addons packet", e);
        }
    }

    public static void sendAddonsToPlayer(Player player, SyncAddonsPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
        }
    }

    public static void sendAllAddonsToPlayer(Player recipient) {
        if (recipient.level() == null) {
            return;
        }
        LOGGER.debug("We are going to be best friends! Syncing all addons to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            var addons = DataUtils.getAddonsData(p);
            if (addons != null && !addons.getActiveAddons().isEmpty()) {
                sendAddonsToPlayer(recipient,
                        new SyncAddonsPacket(p.getUUID(), addons.getActiveAddons(), addons.getExternalGrants(),
                                addons.getAllAddonTints()));
            }
        }
    }

    public static void sendTrailsToAll(SyncTrailsPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                for (var sp : server.getPlayerList().getPlayers()) {
                    PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
                }
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send trails packet", e);
        }
    }

    public static void sendTrailsToPlayer(Player player, SyncTrailsPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
        }
    }

    public static void sendAllTrailsToPlayer(Player recipient) {
        if (recipient.level() == null) {
            return;
        }
        LOGGER.debug("We are going to be best friends! Syncing all trails to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            var trails = DataUtils.getTrailsData(p);
            if (trails != null && !trails.getActiveTrails().isEmpty()) {
                sendTrailsToPlayer(recipient, new SyncTrailsPacket(p.getUUID(), trails.getActiveTrails()));
            }
        }
    }

    public static void sendEffectsToAll(SyncEffectsPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                for (var sp : server.getPlayerList().getPlayers()) {
                    PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
                }
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send effects packet", e);
        }
    }

    public static void sendEffectsToPlayer(Player player, SyncEffectsPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
        }
    }

    public static void sendAllEffectsToPlayer(Player recipient) {
        if (recipient.level() == null) {
            return;
        }
        LOGGER.debug("We are going to be best friends! Syncing all effects to {}",
                recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            var effects = DataUtils.getEffectsData(p);
            if (effects != null && !effects.getActiveEffects().isEmpty()) {
                sendEffectsToPlayer(recipient, new SyncEffectsPacket(p.getUUID(), effects.getActiveEffects(), false));
            }
        }
    }

    public static void sendModelVariantToAll(SyncModelVariantPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                for (var sp : server.getPlayerList().getPlayers()) {
                    PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
                }
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send model variant packet", e);
        }
    }

    public static void sendModelVariantToPlayer(Player player, SyncModelVariantPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            PLATFORM_SEND_TO_PLAYER.accept(sp, pkt);
        }
    }

    public static void sendAllModelVariantsToPlayer(Player recipient) {
        if (recipient.level() == null) {
            return;
        }
        LOGGER.debug("We are going to be best friends! Syncing all model variants to {}",
                recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            var mv = DataUtils.getModelVariantData(p);
            if (mv != null) {
                sendModelVariantToPlayer(recipient, SyncModelVariantPacket.of(p.getUUID(), mv));
            }
        }
    }
}
