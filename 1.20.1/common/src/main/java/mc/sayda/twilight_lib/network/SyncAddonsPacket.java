package mc.sayda.twilight_lib.network;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncAddonsPacket {
    public static final ResourceLocation ID = new ResourceLocation(TwilightLib.MODID, "sync_addons");

    private final UUID playerId;
    private final Set<String> addons;
    private final Map<String, Integer> tints;

    public SyncAddonsPacket(UUID playerId, Set<String> addons, Map<String, Integer> tints) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.addons = addons != null ? addons : java.util.Collections.emptySet();
        this.tints = tints != null ? tints : java.util.Collections.emptyMap();
    }

    public SyncAddonsPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        // Use a reasonable limit to prevent memory exhaustion
        int maxColl = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_COLLECTION_SIZE.get();
        int maxStr = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_STRING_LENGTH.get();

        this.addons = buf.readCollection(s -> new java.util.HashSet<>(Math.min(s, maxColl)), b -> b.readUtf(maxStr));
        int tintSize = Math.min(buf.readVarInt(), maxColl);
        this.tints = new HashMap<>();
        for (int i = 0; i < tintSize; i++) {
            this.tints.put(buf.readUtf(maxStr), buf.readInt());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeCollection(this.addons, (b, s) -> b.writeUtf(s != null ? s : ""));
        buf.writeVarInt(this.tints.size());
        this.tints.forEach((k, v) -> {
            buf.writeUtf(k != null ? k : "unknown");
            buf.writeInt(v != null ? v : 0xFFFFFF);
        });
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                var player = context.getPlayer();
                var level = (player != null) ? player.level() : mc.sayda.twilight_lib.client.ClientAccess.getLevel();
                if (level == null)
                    return;
                var entity = level.getPlayerByUUID(this.playerId);
                if (entity == null)
                    return;
                var data = DataUtils.getAddonsData(entity);
                if (data != null) {
                    data.syncEquippedFromPacket(this.addons);
                    data.syncTintsFromPacket(this.tints);
                }
            });
        });
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Set<String> getAddons() {
        return addons;
    }

    public Map<String, Integer> getTints() {
        return tints;
    }
}
