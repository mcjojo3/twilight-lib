package mc.sayda.twilight_lib.network;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncTrailsPacket {
    public static final ResourceLocation ID = new ResourceLocation(TwilightLib.MODID, "sync_trails");

    private static final @javax.annotation.Nonnull UUID SENTINEL_UUID = new UUID(0, 0);
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private final UUID playerId;
    private final Set<String> trails;

    public SyncTrailsPacket(UUID playerId, Set<String> trails) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.trails = trails != null ? trails : java.util.Collections.emptySet();
    }

    public SyncTrailsPacket(FriendlyByteBuf buf) {
        UUID id;
        try {
            id = buf.readUUID();
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncTrailsPacket: {}", e.getMessage());
            id = SENTINEL_UUID;
        }
        this.playerId = id;
        // Use config values for limits
        int maxColl = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_COLLECTION_SIZE.get();
        int maxStr = mc.sayda.twilight_lib.config.TwilightConfig.NETWORK_MAX_STRING_LENGTH.get();
        this.trails = buf.readCollection(s -> new java.util.HashSet<>(Math.min(s, maxColl)), b -> b.readUtf(maxStr));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeCollection(this.trails, (b, s) -> b.writeUtf(s != null ? s : ""));
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        contextSupplier.get().queue(() ->
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> ClientHandler.apply(this)));
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHandler {
        static void apply(SyncTrailsPacket pkt) {
            try {
                if (pkt.playerId.equals(SENTINEL_UUID)) {
                    LOGGER.warn("How did I?! Uuuughh! Received malformed sync_trails packet with invalid UUID");
                    return;
                }

                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                net.minecraft.world.entity.player.Player player = null;

                if (minecraft.player != null && minecraft.player.getUUID().equals(pkt.playerId)) {
                    player = minecraft.player;
                } else if (minecraft.level != null) {
                    player = minecraft.level.getPlayerByUUID(pkt.playerId);
                }

                if (player == null) {
                    LOGGER.warn("Or, what. Player {} not found in level (cached anyway)", pkt.playerId);
                    return;
                }

                var data = DataUtils.getTrailsData(player);
                if (data == null) {
                    LOGGER.error("How did I?! Uuuughh! Failed to get trails data for player {}", pkt.playerId);
                    return;
                }

                data.syncEquippedFromPacket(pkt.trails);
                LOGGER.debug("Want to see something neat? Synced {} active trails for {}", pkt.trails.size(),
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to sync trails for player {}", pkt.playerId, e);
            }
        }
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Set<String> getTrails() {
        return trails;
    }
}
