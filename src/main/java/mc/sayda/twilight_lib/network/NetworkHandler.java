package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.Optional;

public class NetworkHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TwilightLib.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    private static int index = 0;

    public static void init() {
        CHANNEL.registerMessage(
                index++, SyncMorphPacket.class,
                SyncMorphPacket::encode, SyncMorphPacket::decode, SyncMorphPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                index++, SyncAddonsPacket.class,
                SyncAddonsPacket::encode, SyncAddonsPacket::decode, SyncAddonsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                index++, SyncTrailsPacket.class,
                SyncTrailsPacket::encode, SyncTrailsPacket::decode, SyncTrailsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                index++, SyncEffectsPacket.class,
                SyncEffectsPacket::encode, SyncEffectsPacket::decode, SyncEffectsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        LOGGER.debug("This is the precipice of a new reality! Network channel initialized.");
    }

    public static void sendMorphToAll(SyncMorphPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("Here you go! Sending morph packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send morph packet to all players", e);
        }
    }

    public static void sendToPlayer(Player player, SyncMorphPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
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
            LazyOptional<IMorph> cap = p.getCapability(MorphProvider.MORPH_CAP);
            cap.ifPresent(m -> m.getEntityType().ifPresent(rl ->
                sendToPlayer(recipient, SyncMorphPacket.of(p.getUUID(), rl))
            ));
        }
    }

    // Addon packet methods
    public static void sendAddonsToAll(SyncAddonsPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("Things totally change so they can be the same but also totally different! Sending addons packet to all players.");
        } catch (Exception e) {
            LOGGER.error("Oh, dung beetles! Failed to send addons packet to all players", e);
        }
    }

    public static void sendAddonsToPlayer(Player player, SyncAddonsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
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
            p.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                if (!addons.getActiveAddons().isEmpty()) {
                    sendAddonsToPlayer(recipient, new SyncAddonsPacket(p.getUUID(), addons.getActiveAddons()));
                }
            });
        }
    }

    // Trails packet methods
    public static void sendTrailsToAll(SyncTrailsPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("The wheel turns, day becomes night... time to make colors! Sending trails packet to all players.");
        } catch (Exception e) {
            LOGGER.error("Oh, farn it! Failed to send trails packet to all players", e);
        }
    }

    public static void sendTrailsToPlayer(Player player, SyncTrailsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
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
            p.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                sendTrailsToPlayer(recipient, new SyncTrailsPacket(p.getUUID(), trails.serialize()));
            });
        }
    }

    // Effects packet methods
    public static void sendEffectsToAll(SyncEffectsPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("Yes, more magic! Sending effects packet to all players.");
        } catch (Exception e) {
            LOGGER.error("Dang! Failed to send effects packet to all players", e);
        }
    }

    public static void sendEffectsToPlayer(Player player, SyncEffectsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
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
            p.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                if (!effects.getActiveEffects().isEmpty()) {
                    sendEffectsToPlayer(recipient, new SyncEffectsPacket(p.getUUID(), effects.getActiveEffects()));
                }
            });
        }
    }
}
