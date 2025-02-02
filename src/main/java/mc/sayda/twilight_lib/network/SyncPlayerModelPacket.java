package mc.sayda.twilight_lib.network;

import java.util.UUID;
import java.util.function.Supplier;
import mc.sayda.twilight_lib.ModelManager;
import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SyncPlayerModelPacket {
    private final UUID playerId;
    private final String modelName;

    public SyncPlayerModelPacket(UUID playerId, String modelName) {
        this.playerId = playerId;
        this.modelName = modelName;
    }

    // Called when encoding the packet for sending.
    public static void encode(SyncPlayerModelPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeUtf(packet.modelName);
    }

    // Called when decoding the packet on reception.
    public static SyncPlayerModelPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String model = buf.readUtf(32767);
        return new SyncPlayerModelPacket(id, model);
    }

    // Handle the packet on the client side.
    public static void handle(SyncPlayerModelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // We are now on the client thread.
            ModelManager.updatePlayerModel(packet.playerId, packet.modelName);
            TwilightLib.LOGGER.info("Received model sync for player {}: {}", packet.playerId, packet.modelName);
        });
        ctx.get().setPacketHandled(true);
    }
}
