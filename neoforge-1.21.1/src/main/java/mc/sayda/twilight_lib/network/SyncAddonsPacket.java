package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import mc.sayda.twilight_lib.capabilities.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record SyncAddonsPacket(UUID playerId, Set<String> addons) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncAddonsPacket> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_addons"));

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buf) {
            return new UUID(buf.readLong(), buf.readLong());
        }

        @Override
        public void encode(ByteBuf buf, UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final StreamCodec<ByteBuf, SyncAddonsPacket> STREAM_CODEC = StreamCodec.composite(
        UUID_CODEC,
        SyncAddonsPacket::playerId,
        ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 1000),
        SyncAddonsPacket::addons,
        SyncAddonsPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncAddonsPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("Are we done in this reality yet? Cannot sync addons - level is null");
                return;
            }
            var entity = level.getPlayerByUUID(msg.playerId());
            if (entity == null) {
                LOGGER.warn("I wonder who's around... Player {} not found in level", msg.playerId());
                return;
            }

            var addons = entity.getData(ModAttachments.ADDONS);

            // Type-safe cast with validation to prevent crashes in heavily modded environments
            if (!(addons instanceof AddonsData)) {
                LOGGER.error("Incompatible addons attachment implementation for player {}. Expected AddonsData but got {}. " +
                             "This may be caused by another mod replacing the attachment.",
                             entity.getName().getString(), addons.getClass().getName());
                return; // Gracefully skip instead of crashing
            }

            // Directly sync equipped addons from server (bypasses ownership validation)
            ((AddonsData) addons).syncEquippedFromPacket(msg.addons());
            LOGGER.debug("Isn't this cool? Synced {} active addons for {}",
                msg.addons().size(), entity.getName().getString());
        });
    }
}
