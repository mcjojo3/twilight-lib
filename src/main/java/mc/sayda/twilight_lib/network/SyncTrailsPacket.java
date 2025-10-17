package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to sync trails data between server and clients
 */
public class SyncTrailsPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final UUID playerUuid;
    private final CompoundTag trailsData;

    public SyncTrailsPacket(UUID playerUuid, CompoundTag trailsData) {
        this.playerUuid = playerUuid;
        this.trailsData = trailsData;
    }

    public static void encode(SyncTrailsPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUuid);
        buf.writeNbt(msg.trailsData);
    }

    public static SyncTrailsPacket decode(FriendlyByteBuf buf) {
        UUID playerUuid = buf.readUUID();
        CompoundTag trailsData = buf.readNbt();
        // Handle null NBT (network error or malformed packet)
        if (trailsData == null) {
            LOGGER.warn("What's with all the negative waves? Received null NBT in SyncTrailsPacket for player {}", playerUuid);
            trailsData = new CompoundTag();
        }
        return new SyncTrailsPacket(playerUuid, trailsData);
    }

    public static void handle(SyncTrailsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Validate packet data
            if (msg.trailsData == null || msg.trailsData.isEmpty()) {
                LOGGER.warn("What's with all the negative waves? Received invalid trail data for player {}", msg.playerUuid);
                return;
            }

            // Client-side handling
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Player player = mc.level.getPlayerByUUID(msg.playerUuid);
                if (player != null) {
                    player.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                        trails.deserialize(msg.trailsData);
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}