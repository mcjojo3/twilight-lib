package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncAddonsPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final UUID playerId;
    private final Set<String> addons;

    public SyncAddonsPacket(UUID playerId, Set<String> addons) {
        this.playerId = playerId;
        this.addons = addons;
    }

    public static void encode(SyncAddonsPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeInt(msg.addons.size());
        for (String addonId : msg.addons) {
            buf.writeUtf(addonId);
        }
    }

    public static SyncAddonsPacket decode(FriendlyByteBuf buf) {
        try {
            UUID id = buf.readUUID();
            int size = buf.readInt();

            // Validate packet size to prevent memory exhaustion attacks
            // Max 1000 addons is reasonable (current registry has ~106)
            if (size < 0 || size > 1000) {
                throw new IllegalArgumentException("Invalid addon packet size: " + size + " (max 1000)");
            }

            Set<String> addons = new HashSet<>();
            for (int i = 0; i < size; i++) {
                String addonId = buf.readUtf();
                if (addonId != null && !addonId.isEmpty()) {
                    addons.add(addonId);
                }
            }
            return new SyncAddonsPacket(id, addons);
        } catch (Exception e) {
            LOGGER.error("Or, what. Failed to decode SyncAddonsPacket: {}", e.getMessage());
            // Return empty packet to prevent crash
            return new SyncAddonsPacket(new UUID(0, 0), new HashSet<>());
        }
    }

    public static void handle(SyncAddonsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Detect malformed packets from decode errors
            if (msg.playerId.equals(new UUID(0, 0))) {
                LOGGER.error("Or, what. Received malformed SyncAddonsPacket with invalid UUID - packet decode failed");
                return;
            }

            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("Are we done in this reality yet? Cannot sync addons - level is null");
                return;
            }
            var entity = level.getPlayerByUUID(msg.playerId);
            if (entity == null) {
                LOGGER.warn("I wonder who's around... Player {} not found in level", msg.playerId);
                return;
            }

            entity.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                // Type-safe cast with validation to prevent crashes in heavily modded environments
                if (!(addons instanceof AddonsData)) {
                    LOGGER.error("Incompatible addons capability implementation for player {}. Expected AddonsData but got {}. " +
                                 "This may be caused by another mod replacing the capability.",
                                 entity.getName().getString(), addons.getClass().getName());
                    return; // Gracefully skip instead of crashing
                }

                // Directly sync equipped addons from server (bypasses ownership validation)
                ((AddonsData) addons).syncEquippedFromPacket(msg.addons);
                LOGGER.debug("Isn't this cool? Synced {} active addons for {}",
                    msg.addons.size(), entity.getName().getString());
            });
        });
        ctx.get().setPacketHandled(true);
    }
}