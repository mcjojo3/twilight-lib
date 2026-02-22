package mc.sayda.twilight_lib.network;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncEffectsPacket {
    public static final ResourceLocation ID = new ResourceLocation(TwilightLib.MODID, "sync_effects");

    private final UUID playerId;
    private final Set<String> effects;
    private final boolean spawnEffect;

    public SyncEffectsPacket(UUID playerId, Set<String> effects, boolean spawnEffect) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.effects = effects != null ? effects : java.util.Collections.emptySet();
        this.spawnEffect = spawnEffect;
    }

    public SyncEffectsPacket(UUID playerId, Set<String> effects) {
        this(playerId, effects, false);
    }

    public SyncEffectsPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        // Use a reasonable limit to prevent memory exhaustion
        int maxColl = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_COLLECTION_SIZE.get();
        int maxStr = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_STRING_LENGTH.get();
        this.effects = buf.readCollection(s -> new java.util.HashSet<>(Math.min(s, maxColl)), b -> b.readUtf(maxStr));
        this.spawnEffect = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeCollection(this.effects, FriendlyByteBuf::writeUtf);
        buf.writeBoolean(this.spawnEffect);
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
                var data = DataUtils.getEffectsData(entity);
                if (data != null) {
                    data.syncEquippedFromPacket(this.effects);
                    if (this.spawnEffect) {
                        mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler.scheduleSpawnEffect(entity.getUUID());
                    }
                }
            });
        });
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Set<String> getEffects() {
        return effects;
    }

    public boolean isSpawnEffect() {
        return spawnEffect;
    }
}
