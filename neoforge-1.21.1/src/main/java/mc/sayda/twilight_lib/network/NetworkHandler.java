package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.Optional;

public class NetworkHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL = "1";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL);

        // Register client-bound packets
        registrar.playToClient(
            SyncMorphPacket.TYPE,
            SyncMorphPacket.STREAM_CODEC,
            SyncMorphPacket::handle
        );

        registrar.playToClient(
            SyncAddonsPacket.TYPE,
            SyncAddonsPacket.STREAM_CODEC,
            SyncAddonsPacket::handle
        );

        registrar.playToClient(
            SyncTrailsPacket.TYPE,
            SyncTrailsPacket.STREAM_CODEC,
            SyncTrailsPacket::handle
        );

        registrar.playToClient(
            SyncEffectsPacket.TYPE,
            SyncEffectsPacket.STREAM_CODEC,
            SyncEffectsPacket::handle
        );

        registrar.playToClient(
            SyncModelVariantPacket.TYPE,
            SyncModelVariantPacket.STREAM_CODEC,
            SyncModelVariantPacket::handle
        );

        LOGGER.debug("This is the precipice of a new reality! Network payloads registered.");
    }

    public static void sendMorphToAll(SyncMorphPacket pkt) {
        try {
            PacketDistributor.sendToAllPlayers(pkt);
            LOGGER.debug("Here you go! Sending morph packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send morph packet to all players", e);
        }
    }

    public static void sendToPlayer(Player player, SyncMorphPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            PacketDistributor.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("Here you go! Sending morph to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send morph to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllMorphsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Here you go! Syncing all morphs to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            IMorph morph = p.getData(ModAttachments.MORPH);
            if (morph == null) {
                LOGGER.warn("How did I?! Uuuughh! Failed to get morph data for player {}", p.getUUID());
                continue;
            }
            morph.getEntityType().ifPresent(rl ->
                sendToPlayer(recipient, SyncMorphPacket.of(p.getUUID(), Optional.of(rl), morph.isNametagHidden()))
            );
        }
    }

    // Addon packet methods
    public static void sendAddonsToAll(SyncAddonsPacket pkt) {
        try {
            PacketDistributor.sendToAllPlayers(pkt);
            LOGGER.debug("Here you go! Sending addons packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send addons packet to all players", e);
        }
    }

    public static void sendAddonsToPlayer(Player player, SyncAddonsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            PacketDistributor.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("Here you go! Sending addons to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send addons to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllAddonsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Here you go! Syncing all addons to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            var addons = p.getData(ModAttachments.ADDONS);
            if (addons == null) {
                LOGGER.warn("Failed to get addons data for player {}", p.getUUID());
                continue;
            }
            if (!addons.getActiveAddons().isEmpty()) {
                sendAddonsToPlayer(recipient, new SyncAddonsPacket(p.getUUID(), addons.getActiveAddons()));
            }
        }
    }

    // Trails packet methods
    public static void sendTrailsToAll(SyncTrailsPacket pkt) {
        try {
            PacketDistributor.sendToAllPlayers(pkt);
            LOGGER.debug("Here you go! Sending trails packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send trails packet to all players", e);
        }
    }

    public static void sendTrailsToPlayer(Player player, SyncTrailsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            PacketDistributor.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("Here you go! Sending trails to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send trails to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllTrailsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Here you go! Syncing all trails to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            var trails = p.getData(ModAttachments.TRAILS);
            if (trails == null) {
                LOGGER.warn("Failed to get trails data for player {}", p.getUUID());
                continue;
            }
            if (!trails.getActiveTrails().isEmpty()) {
                sendTrailsToPlayer(recipient, new SyncTrailsPacket(p.getUUID(), trails.getActiveTrails()));
            }
        }
    }

    // Effects packet methods
    public static void sendEffectsToAll(SyncEffectsPacket pkt) {
        try {
            PacketDistributor.sendToAllPlayers(pkt);
            LOGGER.debug("Here you go! Sending effects packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send effects packet to all players", e);
        }
    }

    public static void sendEffectsToPlayer(Player player, SyncEffectsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            PacketDistributor.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("Here you go! Sending effects to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send effects to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllEffectsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Here you go! Syncing all effects to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            var effects = p.getData(ModAttachments.EFFECTS);
            if (effects == null) {
                LOGGER.warn("Failed to get effects data for player {}", p.getUUID());
                continue;
            }
            if (!effects.getActiveEffects().isEmpty()) {
                sendEffectsToPlayer(recipient, new SyncEffectsPacket(p.getUUID(), effects.getActiveEffects()));
            }
        }
    }

    // Model Variant packet methods
    public static void sendModelVariantToAll(SyncModelVariantPacket pkt) {
        try {
            PacketDistributor.sendToAllPlayers(pkt);
            LOGGER.debug("Here you go! Sending model variant packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send model variant packet to all players", e);
        }
    }

    public static void sendModelVariantToPlayer(Player player, SyncModelVariantPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            PacketDistributor.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("Here you go! Sending model variant to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send model variant to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllModelVariantsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Here you go! Syncing all model variants to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            var modelVariant = p.getData(ModAttachments.MODEL_VARIANT);
            if (modelVariant == null) {
                LOGGER.warn("Failed to get model variant data for player {}", p.getUUID());
                continue;
            }
            sendModelVariantToPlayer(recipient, SyncModelVariantPacket.of(p.getUUID(), modelVariant));
        }
    }
}
