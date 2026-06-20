package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record SyncAddonsPacket(UUID playerId, Set<String> addons, Set<String> externalGrants, Map<String, Integer> tints)
        implements CustomPacketPayload {
    public SyncAddonsPacket {
        java.util.Objects.requireNonNull(playerId, "playerId");
        addons = addons != null ? addons : java.util.Collections.emptySet();
        externalGrants = externalGrants != null ? externalGrants : java.util.Collections.emptySet();
        tints = tints != null ? tints : java.util.Collections.emptyMap();
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncAddonsPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_addons"));

    // Sentinel UUID for malformed packets
    private static final @Nonnull UUID SENTINEL_UUID = new UUID(0, 0);

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public @Nonnull UUID decode(@Nonnull ByteBuf buf) {
            try {
                return new UUID(buf.readLong(), buf.readLong());
            } catch (Exception e) {
                LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncAddonsPacket: {}", e.getMessage());
                return SENTINEL_UUID; // Return sentinel on decode error
            }
        }

        @Override
        public void encode(@Nonnull ByteBuf buf, @Nonnull UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final StreamCodec<ByteBuf, SyncAddonsPacket> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC,
            SyncAddonsPacket::playerId,
            // Hardcoded max size (128) instead of
            // TwilightConfig.NETWORK_MAX_COLLECTION_SIZE.get()
            // to avoid NeoForge crash: config values aren't loaded when static fields
            // initialize
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 128),
            SyncAddonsPacket::addons,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 128),
            SyncAddonsPacket::externalGrants,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT, 128),
            SyncAddonsPacket::tints,
            SyncAddonsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncAddonsPacket msg, dev.architectury.networking.NetworkManager.PacketContext context) {
        context.queue(() ->
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> ClientHandler.apply(msg)));
    }

    @Environment(EnvType.CLIENT)
    public static void clientApply(SyncAddonsPacket msg) {
        ClientHandler.apply(msg);
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHandler {
        static void apply(SyncAddonsPacket msg) {
            try {
                if (msg.playerId().equals(SENTINEL_UUID)) {
                    LOGGER.error("How did I?! Uuuughh! Received malformed SyncAddonsPacket with invalid UUID");
                    return;
                }

                net.minecraft.world.entity.player.Player entity = null;
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.player != null && minecraft.player.getUUID().equals(msg.playerId())) {
                    entity = minecraft.player;
                } else if (minecraft.level != null) {
                    entity = minecraft.level.getPlayerByUUID(msg.playerId());
                }
                if (entity == null) {
                    LOGGER.warn("Or, what. Player {} not found in level (cached anyway)", msg.playerId());
                    return;
                }

                var addons = DataUtils.getAddonsData(entity);
                if (addons == null) {
                    LOGGER.error("How did I?! Uuuughh! Failed to get addons data for player {}", msg.playerId());
                    return;
                }

                addons.syncEquippedFromPacket(msg.addons());
                addons.syncExternalGrantsFromPacket(msg.externalGrants());
                addons.syncTintsFromPacket(msg.tints());

                LOGGER.debug("Want to see something neat? Synced {} active addons for {}", msg.addons().size(),
                        entity.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to sync addons for player {}", msg.playerId(), e);
            }
        }
    }
}
