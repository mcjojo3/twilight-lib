package mc.sayda.twilight_lib.network;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncTrailsPacket {
    public static final ResourceLocation ID = new ResourceLocation(TwilightLib.MODID, "sync_trails");

    private final UUID playerId;
    private final Set<String> trails;

    public SyncTrailsPacket(UUID playerId, Set<String> trails) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.trails = trails != null ? trails : java.util.Collections.emptySet();
    }

    public SyncTrailsPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        // Use a reasonable limit to prevent memory exhaustion
        int maxColl = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_COLLECTION_SIZE.get();
        int maxStr = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_STRING_LENGTH.get();
        this.trails = buf.readCollection(s -> new java.util.HashSet<>(Math.min(s, maxColl)), b -> b.readUtf(maxStr));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeCollection(this.trails, (b, s) -> b.writeUtf(s != null ? s : ""));
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
                var data = DataUtils.getTrailsData(entity);
                if (data != null) {
                    data.syncEquippedFromPacket(this.trails);
                }
            });
        });
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Set<String> getTrails() {
        return trails;
    }
}
