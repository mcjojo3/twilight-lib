package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
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
        Set<String> effects = new HashSet<>();
        for (int i = 0; i < size; i++) {
            effects.add(buf.readUtf());
        }
        return new SyncEffectsPacket(id, effects);
    }

    public static void handle(SyncEffectsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("Twilight fades... Cannot sync effects - level is null");
                return;
            }
            var entity = level.getPlayerByUUID(msg.playerId);
            if (entity == null) {
                LOGGER.warn("Where did they go? Player {} not found in level", msg.playerId);
                return;
            }

            entity.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                effects.clearEffects();
                msg.effects.forEach(effects::addEffect);
                LOGGER.debug("Magic flows! Synced {} effects for {}",
                    msg.effects.size(), entity.getName().getString());
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
