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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record SyncTrailsPacket(UUID playerId, Set<String> trails) implements CustomPacketPayload {
    public SyncTrailsPacket{java.util.Objects.requireNonNull(playerId,"playerId");trails=trails!=null?trails:java.util.Collections.emptySet();}

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncTrailsPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_trails"));
    
    private static final @Nonnull UUID SENTINEL_UUID = new UUID(0, 0);

    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC=new StreamCodec<>(){@Override public @Nonnull UUID decode(@Nonnull ByteBuf buf){try{return new UUID(buf.readLong(),buf.readLong());}catch(Exception e){LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncTrailsPacket: {}",e.getMessage());return SENTINEL_UUID;}}

    @Override public void encode(@Nonnull ByteBuf buf,@Nonnull UUID uuid){buf.writeLong(uuid.getMostSignificantBits());buf.writeLong(uuid.getLeastSignificantBits());}};

    /**
     * Map must be used because ByteBufCodecs.collection results in HashSet but
     * record constructor and getter use Set interface.
     */
    public static final StreamCodec<ByteBuf, SyncTrailsPacket> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC,
            SyncTrailsPacket::playerId,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 128)
                    .map(java.util.function.Function.identity(), set -> new HashSet<>(set)),
            SyncTrailsPacket::trails,
            SyncTrailsPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTrailsPacket msg, dev.architectury.networking.NetworkManager.PacketContext context) {
        context.queue(() ->
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> ClientHandler.apply(msg)));
    }

    @Environment(EnvType.CLIENT)
    public static void clientApply(SyncTrailsPacket msg) {
        ClientHandler.apply(msg);
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHandler {
        static void apply(SyncTrailsPacket msg) {
            try {
                if (msg.playerId().equals(SENTINEL_UUID)) {
                    LOGGER.error("How did I?! Uuuughh! Received malformed SyncTrailsPacket with invalid UUID");
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

                var trails = DataUtils.getTrailsData(entity);
                if (trails == null) {
                    LOGGER.error("How did I?! Uuuughh! Failed to get trails data for player {}", msg.playerId());
                    return;
                }

                trails.syncEquippedFromPacket(msg.trails());
                LOGGER.debug("Want to see something neat? Synced {} active trails for {}", msg.trails().size(),
                        entity.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to sync trails for player {}", msg.playerId(), e);
            }
        }
    }
}
