package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.Optional;

public class NetworkHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register() {
        // Register client-bound packets
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                SyncMorphPacket.TYPE,
                SyncMorphPacket.STREAM_CODEC,
                SyncMorphPacket::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                SyncAddonsPacket.TYPE,
                SyncAddonsPacket.STREAM_CODEC,
                SyncAddonsPacket::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                SyncTrailsPacket.TYPE,
                SyncTrailsPacket.STREAM_CODEC,
                SyncTrailsPacket::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                SyncEffectsPacket.TYPE,
                SyncEffectsPacket.STREAM_CODEC,
                SyncEffectsPacket::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                SyncModelVariantPacket.TYPE,
                SyncModelVariantPacket.STREAM_CODEC,
                SyncModelVariantPacket::handle);

        LOGGER.debug("Hi! My name is Zoe. Network payloads registered.");
    }

    public static void sendMorphToAll(SyncMorphPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), pkt);
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send morph packet to all players", e);
        }
    }

    public static void sendToPlayer(Player player, SyncMorphPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        try {
            NetworkManager.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("We are going to be best friends! Sending morph to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send morph to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllMorphsToPlayer(Player recipient) {
        if (recipient.level() == null)
            return;
        LOGGER.debug("We are going to be best friends! Syncing all morphs to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null)
                continue;
            IMorph morph = DataUtils.getMorphData(p);
            if (morph == null) {
                // Warning logged only if significant
                continue;
            }
            morph.getEntityType().ifPresent(rl -> sendToPlayer(recipient,
                    SyncMorphPacket.of(p.getUUID(), Optional.of(rl), morph.isNametagHidden())));
        }
    }

    // Addon packet methods
    public static void sendAddonsToAll(SyncAddonsPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), pkt);
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send addons packet", e);
        }
    }

    // Simplified for plan brevity - repeating pattern for all packet types
    public static void sendAddonsToPlayer(Player player, SyncAddonsPacket pkt) {
        if (player instanceof ServerPlayer sp)
            NetworkManager.sendToPlayer(sp, pkt);
    }

    public static void sendAllAddonsToPlayer(Player recipient) {
        // Same logic as morphs
        if (recipient.level() == null)
            return;
        LOGGER.debug("We are going to be best friends! Syncing all addons to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            var addons = DataUtils.getAddonsData(p);
            if (addons != null && !addons.getActiveAddons().isEmpty()) {
                sendAddonsToPlayer(recipient,
                        new SyncAddonsPacket(p.getUUID(), addons.getActiveAddons(), addons.getExternalGrants(), addons.getAllAddonTints()));
            }
        }
    }

    public static void sendTrailsToAll(SyncTrailsPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), pkt);
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send trails packet", e);
        }
    }

    public static void sendTrailsToPlayer(Player player, SyncTrailsPacket pkt) {
        if (player instanceof ServerPlayer sp)
            NetworkManager.sendToPlayer(sp, pkt);
    }

    public static void sendAllTrailsToPlayer(Player recipient) {
        // Logic...
        if (recipient.level() == null)
            return;
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
                NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), pkt);
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send effects packet", e);
        }
    }

    public static void sendEffectsToPlayer(Player player, SyncEffectsPacket pkt) {
        if (player instanceof ServerPlayer sp)
            NetworkManager.sendToPlayer(sp, pkt);
    }

    public static void sendAllEffectsToPlayer(Player recipient) {
        // Logic
        if (recipient.level() == null)
            return;
        LOGGER.debug("We are going to be best friends! Syncing all effects to {}",
                recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            var effects = DataUtils.getEffectsData(p);
            if (effects != null && !effects.getActiveEffects().isEmpty()) {
                sendEffectsToPlayer(recipient, new SyncEffectsPacket(p.getUUID(), effects.getActiveEffects(), false)); // false
                                                                                                                       // =
                                                                                                                       // no
                                                                                                                       // spawn
                                                                                                                       // effect
                                                                                                                       // on
                                                                                                                       // sync
            }
        }
    }

    public static void sendModelVariantToAll(SyncModelVariantPacket pkt) {
        try {
            var server = dev.architectury.utils.GameInstance.getServer();
            if (server != null) {
                NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), pkt);
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send model variant packet", e);
        }
    }

    public static void sendModelVariantToPlayer(Player player, SyncModelVariantPacket pkt) {
        if (player instanceof ServerPlayer sp)
            NetworkManager.sendToPlayer(sp, pkt);
    }

    public static void sendAllModelVariantsToPlayer(Player recipient) {
        // Logic
        if (recipient.level() == null)
            return;
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
