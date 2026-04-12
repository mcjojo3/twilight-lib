package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public record SyncMorphPacket(UUID playerId, Optional<ResourceLocation> entity, boolean hideNametag)
        implements CustomPacketPayload {
    public SyncMorphPacket {
        java.util.Objects.requireNonNull(playerId, "playerId");
        entity = entity != null ? entity : Optional.empty();
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncMorphPacket> TYPE = new CustomPacketPayload.Type<>(
            java.util.Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_morph"), "type_rl"));

    // Sentinel UUID for malformed packets
    private static final @Nonnull UUID SENTINEL_UUID = new UUID(0, 0);

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public @Nonnull UUID decode(@Nonnull ByteBuf buf) {
            try {
                long mostSig = buf.readLong();
                long leastSig = buf.readLong();
                return new UUID(mostSig, leastSig);
            } catch (Exception e) {
                LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncMorphPacket: {}", e.getMessage());
                return SENTINEL_UUID; // Return sentinel on decode error
            }
        }

        @Override
        public void encode(@Nonnull ByteBuf buf, @Nonnull UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final StreamCodec<ByteBuf, SyncMorphPacket> STREAM_CODEC = StreamCodec.composite(
            (StreamCodec<ByteBuf, UUID>) UUID_CODEC,
            SyncMorphPacket::playerId,
            (StreamCodec<ByteBuf, Optional<ResourceLocation>>) ByteBufCodecs.optional((StreamCodec<ByteBuf, ResourceLocation>) ResourceLocation.STREAM_CODEC),
            SyncMorphPacket::entity,
            (StreamCodec<ByteBuf, Boolean>) ByteBufCodecs.BOOL,
            SyncMorphPacket::hideNametag,
            (playerId, entity, hideNametag) -> new SyncMorphPacket(playerId, entity, (boolean) hideNametag));

    public static SyncMorphPacket of(UUID id, ResourceLocation rlOrNull) {
        return new SyncMorphPacket(id, Optional.ofNullable(rlOrNull), false);
    }

    public static SyncMorphPacket of(UUID id, Optional<ResourceLocation> entity) {
        return new SyncMorphPacket(id, entity, false);
    }

    public static SyncMorphPacket of(UUID id, Optional<ResourceLocation> entity, boolean hideNametag) {
        return new SyncMorphPacket(id, entity, hideNametag);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncMorphPacket msg, dev.architectury.networking.NetworkManager.PacketContext context) {
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                try {
                    // Detect malformed packets from decode errors
                    if (msg.playerId().equals(SENTINEL_UUID)) {
                        LOGGER.warn(
                                "How did I?! Uuuughh! Received malformed morph packet with null UUID - packet decode failed");
                        return;
                    }

                    // Try the local player directly first (valid even before entity tracking on
                    // join)
                    net.minecraft.world.entity.player.Player entity = null;
                    net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                    if (minecraft.player != null && minecraft.player.getUUID().equals(msg.playerId())) {
                        entity = minecraft.player;
                    } else if (minecraft.level != null) {
                        entity = minecraft.level.getPlayerByUUID(java.util.Objects.requireNonNull(msg.playerId(), "playerId"));
                    }
                    if (entity == null) {
                        LOGGER.warn("Or, what. Player {} not found in level (cached anyway)", msg.playerId());
                        return;
                    }

                    IMorph morph = DataUtils.getMorphData(entity);
                    if (morph == null) {
                        LOGGER.error("How did I?! Uuuughh! Failed to get morph data for player {}", msg.playerId());
                        return;
                    }
                    // CLIENT-SIDE ATTACHMENT MODIFICATION: This is intentional and safe
                    // Server is authoritative and sends sync packets on login/respawn
                    // Client attachments are read-only cache for rendering, no gameplay logic
                    // depends on them
                    morph.setEntityType(msg.entity());
                    morph.setNametagHidden(msg.hideNametag());
                    // CRITICAL: Refresh dimensions on the client side after attachment update
                    entity.refreshDimensions();
                    LOGGER.debug("Want to see something neat? Synced morph {} (hideNametag={}) for {}", msg.entity(),
                            msg.hideNametag(), entity.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("How did I?! Uuuughh! Failed to sync morph for player {}", msg.playerId(), e);
                }
            });
        });
    }
}
