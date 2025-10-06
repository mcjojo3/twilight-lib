package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.capabilities.PlayerAddonsProvider;
import net.minecraft.resources.ResourceLocation;
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
        LOGGER.debug("This is the precipice of a new reality! Network channel initialized.");
    }

    public static void sendToAll(SyncMorphPacket pkt) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
        LOGGER.debug("Here you go! Sending morph packet to all players.");
    }

    public static void sendToPlayer(Player player, SyncMorphPacket pkt) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
        LOGGER.debug("I'm coming over to say 'Hi!' Sending morph to {}", player.getGameProfile().getName());
    }

    public static void sendAllMorphsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Want to see something neat? Syncing all morphs to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            LazyOptional<IMorph> cap = p.getCapability(MorphProvider.MORPH_CAP);
            cap.ifPresent(m -> m.getEntityType().ifPresent(rl ->
                sendToPlayer(recipient, SyncMorphPacket.of(p.getUUID(), rl))
            ));
        }
    }

    @Deprecated
    public static void broadcastAllMorphs(Level level) {
        if (level == null) return;
        for (Player p : level.players()) {
            LazyOptional<IMorph> cap = p.getCapability(MorphProvider.MORPH_CAP);
            cap.ifPresent(m -> m.getEntityType().ifPresent(rl ->
                sendToAll(SyncMorphPacket.of(p.getUUID(), rl))
            ));
        }
    }

    // Addon packet methods
    public static void sendAddonsToAll(SyncAddonsPacket pkt) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
        LOGGER.debug("Things totally change so they can be the same but also totally different! Sending addons packet to all players.");
    }

    public static void sendAddonsToPlayer(Player player, SyncAddonsPacket pkt) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
        LOGGER.debug("Starlight is an expression of something inside bursting to get out! Sending addons to {}", player.getGameProfile().getName());
    }

    public static void sendAllAddonsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Every day, every season... ends. And begin something new! Syncing all addons to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            p.getCapability(PlayerAddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                if (!addons.getAddons().isEmpty()) {
                    sendAddonsToPlayer(recipient, new SyncAddonsPacket(p.getUUID(), addons.getAddons()));
                }
            });
        }
    }
}
