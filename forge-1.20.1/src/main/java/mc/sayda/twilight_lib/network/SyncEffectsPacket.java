package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler;
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
    private final boolean triggerSpawnEffect;  // Only true for actual spawn events (login/respawn)

    public SyncEffectsPacket(UUID playerId, Set<String> effects) {
        this(playerId, effects, false);
    }

    public SyncEffectsPacket(UUID playerId, Set<String> effects, boolean triggerSpawnEffect) {
        this.playerId = playerId;
        this.effects = effects;
        this.triggerSpawnEffect = triggerSpawnEffect;
    }

    public static void encode(SyncEffectsPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeInt(msg.effects.size());
        for (String effectId : msg.effects) {
            buf.writeUtf(effectId);
        }
        buf.writeBoolean(msg.triggerSpawnEffect);
    }

    public static SyncEffectsPacket decode(FriendlyByteBuf buf) {
        try {
            UUID id = buf.readUUID();
            int size = buf.readInt();

            // Validate packet size to prevent memory exhaustion attacks
            // Max 100 effects is reasonable (current implementation has only 1 effect type)
            if (size < 0 || size > 100) {
                throw new IllegalArgumentException("Invalid effects packet size: " + size + " (max 100)");
            }

            Set<String> effects = new HashSet<>();
            for (int i = 0; i < size; i++) {
                String effectId = buf.readUtf();
                if (effectId != null && !effectId.isEmpty()) {
                    effects.add(effectId);
                }
            }
            boolean triggerSpawnEffect = buf.readBoolean();
            return new SyncEffectsPacket(id, effects, triggerSpawnEffect);
        } catch (Exception e) {
            LOGGER.error("Oh. Wow. That's like, totally off the mandala. Failed to decode SyncEffectsPacket: {}", e.getMessage());
            // Return empty packet to prevent crash
            return new SyncEffectsPacket(new UUID(0, 0), new HashSet<>(), false);
        }
    }

    public static void handle(SyncEffectsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Detect malformed packets from decode errors
            if (msg.playerId.equals(new UUID(0, 0))) {
                LOGGER.error("This will be fine! Don't worry about it Zoe, things break all the time. Received malformed SyncEffectsPacket with invalid UUID - packet decode failed");
                return;
            }

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

                // Only trigger spawn effect on actual spawn events (login/respawn), not on every sync
                // Check for any spawn effect (spawn_ethereal, spawn_rainbow, etc.)
                if (msg.triggerSpawnEffect && !msg.effects.isEmpty()) {
                    LOGGER.debug("C'mon! Try and catch me! Scheduling spawn effect for player {}", entity.getName().getString());
                    SpawnEffectHandler.scheduleSpawnEffect(msg.playerId);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
