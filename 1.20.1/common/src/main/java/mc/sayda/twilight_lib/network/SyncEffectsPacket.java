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

    private static final @javax.annotation.Nonnull UUID SENTINEL_UUID = new UUID(0, 0);
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

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
        UUID id;
        try {
            id = buf.readUUID();
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncEffectsPacket: {}", e.getMessage());
            id = SENTINEL_UUID;
        }
        this.playerId = id;
        // Use config values for limits
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
                try {
                    if (this.playerId.equals(SENTINEL_UUID)) {
                        LOGGER.warn("How did I?! Uuuughh! Received malformed sync_effects packet with invalid UUID");
                        return;
                    }

                    net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                    net.minecraft.world.entity.player.Player player = null;

                    if (minecraft.player != null && minecraft.player.getUUID().equals(this.playerId)) {
                        player = minecraft.player;
                    } else if (minecraft.level != null) {
                        player = minecraft.level.getPlayerByUUID(this.playerId);
                    }

                    if (player == null) {
                        LOGGER.warn("Or, what. Player {} not found in level (cached anyway)", this.playerId);
                        return;
                    }

                    var data = DataUtils.getEffectsData(player);
                    if (data == null) {
                        LOGGER.error("How did I?! Uuuughh! Failed to get effects data for player {}", this.playerId);
                        return;
                    }

                    data.syncEquippedFromPacket(this.effects);
                    if (this.spawnEffect) {
                        mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler.scheduleSpawnEffect(player.getUUID());
                    }
                    LOGGER.debug("Want to see something neat? Synced {} active effects for {}", this.effects.size(),
                            player.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("How did I?! Uuuughh! Failed to sync effects for player {}", this.playerId, e);
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
