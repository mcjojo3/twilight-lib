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

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final @javax.annotation.Nonnull UUID SENTINEL_UUID = new UUID(0, 0);

    private final UUID playerId;
    private final Set<String> addons;
    private final Set<String> externalGrants;
    private final Map<String, Integer> tints;

    public SyncAddonsPacket(UUID playerId, Set<String> addons, Set<String> externalGrants, Map<String, Integer> tints) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.addons = addons != null ? addons : java.util.Collections.emptySet();
        this.externalGrants = externalGrants != null ? externalGrants : java.util.Collections.emptySet();
        this.tints = tints != null ? tints : java.util.Collections.emptyMap();
    }

    public SyncAddonsPacket(FriendlyByteBuf buf) {
        UUID id;
        try {
            id = buf.readUUID();
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncAddonsPacket: {}", e.getMessage());
            id = SENTINEL_UUID;
        }
        this.playerId = id;
        // Use config values for limits
        int maxColl = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_COLLECTION_SIZE.get();
        int maxStr = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_STRING_LENGTH.get();

        this.addons = buf.readCollection(s -> new java.util.HashSet<>(Math.min(s, maxColl)), b -> b.readUtf(maxStr));
        this.externalGrants = buf.readCollection(s -> new java.util.HashSet<>(Math.min(s, maxColl)),
                b -> b.readUtf(maxStr));

        int tintSize = Math.min(buf.readVarInt(), maxColl);
        this.tints = new HashMap<>();
        for (int i = 0; i < tintSize; i++) {
            this.tints.put(buf.readUtf(maxStr), buf.readInt());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeCollection(this.addons, (b, s) -> b.writeUtf(s != null ? s : ""));
        buf.writeCollection(this.externalGrants, (b, s) -> b.writeUtf(s != null ? s : ""));
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
                try {
                    if (this.playerId.equals(SENTINEL_UUID)) {
                        LOGGER.warn("How did I?! Uuuughh! Received malformed SyncAddonsPacket with invalid UUID");
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

                    var data = DataUtils.getAddonsData(player);
                    if (data == null) {
                        LOGGER.error("How did I?! Uuuughh! Failed to get addons data for player {}", this.playerId);
                        return;
                    }

                    data.syncEquippedFromPacket(this.addons);
                    data.syncExternalGrantsFromPacket(this.externalGrants);
                    data.syncTintsFromPacket(this.tints);

                    LOGGER.debug("Want to see something neat? Synced {} active addons for {}", this.addons.size(),
                            player.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("How did I?! Uuuughh! Failed to sync addons for player {}", this.playerId, e);
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
