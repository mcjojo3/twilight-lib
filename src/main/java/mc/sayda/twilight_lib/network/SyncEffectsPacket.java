package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.cosmetics.RespawnEffectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncEffectsPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final UUID playerId;
    private final Set<String> effects;

    public SyncEffectsPacket(UUID playerId, Set<String> effects) {
        this.playerId = playerId;
        this.effects = effects;
    }

    public static void encode(SyncEffectsPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeInt(msg.effects.size());
        for (String effectId : msg.effects) {
            buf.writeUtf(effectId);
        }
    }

    public static SyncEffectsPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        int size = buf.readInt();

        // Validate packet size to prevent memory exhaustion attacks
        // Max 100 effects is reasonable (current implementation has only 1 effect type)
        if (size < 0 || size > 100) {
            throw new IllegalArgumentException("Invalid effects packet size: " + size + " (max 100)");
        }

        Set<String> effects = new HashSet<>();
        for (int i = 0; i < size; i++) {
            effects.add(buf.readUtf());
        }
        return new SyncEffectsPacket(id, effects);
    }

    public static void handle(SyncEffectsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            var level = mc.level;
            if (level == null) {
                LOGGER.warn("Daylight is too bright, the night is too dark! Cannot sync effects - level is null");
                return;
            }
            var entity = level.getPlayerByUUID(msg.playerId);
            if (entity == null) {
                LOGGER.warn("I wonder who's around... Player {} not found in level", msg.playerId);
                return;
            }

            entity.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                // Directly sync equipped effects from server (bypasses ownership validation)
                ((EffectsData) effects).syncEquippedFromPacket(msg.effects);
                LOGGER.debug("Time to change! Synced {} active effects for {}", msg.effects.size(), entity.getName().getString());

                // If any player has respawn_twilight active, trigger spawn effect (visible to all clients)
                if (msg.effects.contains("respawn_twilight")) {
                    LOGGER.debug("C'mon! Try and catch me! Scheduling spawn effect for player {}", entity.getName().getString());
                    RespawnEffectHandler.scheduleSpawnEffect(msg.playerId);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
