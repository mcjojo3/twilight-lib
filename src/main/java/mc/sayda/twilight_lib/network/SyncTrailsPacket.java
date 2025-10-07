package mc.sayda.twilight_lib.network;

import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to sync trails data between server and clients
 */
public class SyncTrailsPacket {
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
        return new SyncTrailsPacket(playerUuid, trailsData);
    }

    public static void handle(SyncTrailsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
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