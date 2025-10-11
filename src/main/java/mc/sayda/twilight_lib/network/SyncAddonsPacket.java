package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
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
        UUID id = buf.readUUID();
        int size = buf.readInt();
        Set<String> addons = new HashSet<>();
        for (int i = 0; i < size; i++) {
            addons.add(buf.readUtf());
        }
        return new SyncAddonsPacket(id, addons);
    }

    public static void handle(SyncAddonsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
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
                addons.clearActiveAddons();
                msg.addons.forEach(addonId -> addons.setActiveAddon(addonId, true));
                LOGGER.debug("Isn't this cool? Synced {} active addons for {}",
                    msg.addons.size(), entity.getName().getString());
            });
        });
        ctx.get().setPacketHandled(true);
    }
}