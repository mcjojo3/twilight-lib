package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.TrailsData;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncTrailsPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final UUID playerId;
    private final Set<String> trails;

    public SyncTrailsPacket(UUID playerId, Set<String> trails) {
        this.playerId = playerId;
        this.trails = trails;
    }

    public static void encode(SyncTrailsPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeInt(msg.trails.size());
        for (String trailId : msg.trails) {
            buf.writeUtf(trailId);
        }
    }

    public static SyncTrailsPacket decode(FriendlyByteBuf buf) {
        try {
            UUID id = buf.readUUID();
            int size = buf.readInt();

            // Validate packet size to prevent memory exhaustion attacks
            // Max 1000 trails is reasonable (current registry has ~20)
            if (size < 0 || size > 1000) {
                throw new IllegalArgumentException("Invalid trail packet size: " + size + " (max 1000)");
            }

            Set<String> trails = new HashSet<>();
            for (int i = 0; i < size; i++) {
                String trailId = buf.readUtf();
                if (trailId != null && !trailId.isEmpty()) {
                    trails.add(trailId);
                }
            }
            return new SyncTrailsPacket(id, trails);
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to decode SyncTrailsPacket: {}", e.getMessage());
            // Return empty packet to prevent crash
            return new SyncTrailsPacket(new UUID(0, 0), new HashSet<>());
        }
    }

    public static void handle(SyncTrailsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Detect malformed packets from decode errors
            if (msg.playerId.equals(new UUID(0, 0))) {
                LOGGER.error("This will be fine! Things break all the time. Received malformed SyncTrailsPacket with invalid UUID - packet decode failed");
                return;
            }

            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("Are we done in this reality yet? Cannot sync trails - level is null");
                return;
            }
            var entity = level.getPlayerByUUID(msg.playerId);
            if (entity == null) {
                LOGGER.warn("I wonder who's around... Player {} not found in level", msg.playerId);
                return;
            }

            entity.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                // Type-safe cast with validation to prevent crashes in heavily modded environments
                if (!(trails instanceof TrailsData)) {
                    LOGGER.error("What's with all the negative waves? Incompatible trails capability implementation for player {}. Expected TrailsData but got {}. " +
                                 "This may be caused by another mod replacing the capability.",
                                 entity.getName().getString(), trails.getClass().getName());
                    return; // Gracefully skip instead of crashing
                }

                // Directly sync equipped trails from server (bypasses ownership validation)
                ((TrailsData) trails).syncEquippedFromPacket(msg.trails);
                LOGGER.debug("Time to change! Synced {} active trails for {}",
                    msg.trails.size(), entity.getName().getString());
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
