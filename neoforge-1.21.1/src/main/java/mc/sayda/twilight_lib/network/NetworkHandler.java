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
            LOGGER.debug("I'm coming over to say 'Hi!' Sending morph to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("What? No! Why isn't he moving anymore? Failed to send morph to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllMorphsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Want to see something neat? Syncing all morphs to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            IMorph morph = p.getData(ModAttachments.MORPH);
            morph.getEntityType().ifPresent(rl ->
                sendToPlayer(recipient, SyncMorphPacket.of(p.getUUID(), rl))
            );
        }
    }

    // Addon packet methods
    public static void sendAddonsToAll(SyncAddonsPacket pkt) {
        try {
            PacketDistributor.sendToAllPlayers(pkt);
            LOGGER.debug("Things totally change so they can be the same but also totally different! Sending addons packet to all players.");
        } catch (Exception e) {
            LOGGER.error("Oh, dung beetles! Failed to send addons packet to all players", e);
        }
    }

    public static void sendAddonsToPlayer(Player player, SyncAddonsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            PacketDistributor.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("Starlight is an expression of something inside bursting to get out! Sending addons to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("Really?! Failed to send addons to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllAddonsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Every day, every season... ends. And begin something new! Syncing all addons to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            var addons = p.getData(ModAttachments.ADDONS);
            if (!addons.getActiveAddons().isEmpty()) {
                sendAddonsToPlayer(recipient, new SyncAddonsPacket(p.getUUID(), addons.getActiveAddons()));
            }
        }
    }

    // Trails packet methods
    public static void sendTrailsToAll(SyncTrailsPacket pkt) {
        try {
            PacketDistributor.sendToAllPlayers(pkt);
            LOGGER.debug("The wheel turns, day becomes night... time to make colors! Sending trails packet to all players.");
        } catch (Exception e) {
            LOGGER.error("Oh, farn it! Failed to send trails packet to all players", e);
        }
    }

    public static void sendTrailsToPlayer(Player player, SyncTrailsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            PacketDistributor.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("Whoo! Sending trails to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("Shoot! Failed to send trails to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllTrailsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("I wanna wanna, go to some place, place place! Syncing all trails to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            var trails = p.getData(ModAttachments.TRAILS);
            sendTrailsToPlayer(recipient, new SyncTrailsPacket(p.getUUID(), trails.serialize()));
        }
    }

    // Effects packet methods
    public static void sendEffectsToAll(SyncEffectsPacket pkt) {
        try {
            PacketDistributor.sendToAllPlayers(pkt);
            LOGGER.debug("Yes, more magic! Sending effects packet to all players.");
        } catch (Exception e) {
            LOGGER.error("Dang! Failed to send effects packet to all players", e);
        }
    }

    public static void sendEffectsToPlayer(Player player, SyncEffectsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            PacketDistributor.sendToPlayer(serverPlayer, pkt);
            LOGGER.debug("Ooh! Oooooh! Sending effects to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("Miss! Failed to send effects to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllEffectsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Aw, this spell is neat! Syncing all effects to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            var effects = p.getData(ModAttachments.EFFECTS);
            if (!effects.getActiveEffects().isEmpty()) {
                sendEffectsToPlayer(recipient, new SyncEffectsPacket(p.getUUID(), effects.getActiveEffects()));
            }
        }
    }
}
