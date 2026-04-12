package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncMorphPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation ID = new ResourceLocation(mc.sayda.twilight_lib.TwilightLib.MODID, "sync_morph");

    private static final @javax.annotation.Nonnull UUID SENTINEL_UUID = new UUID(0, 0);

    private final UUID playerId;
    private final Optional<ResourceLocation> entity;
    private final boolean hideNametag;

    public SyncMorphPacket(UUID playerId, Optional<ResourceLocation> entity, boolean hideNametag) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.entity = entity != null ? entity : Optional.empty();
        this.hideNametag = hideNametag;
    }

    public SyncMorphPacket(net.minecraft.network.FriendlyByteBuf buf) {
        UUID id;
        try {
            id = buf.readUUID();
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncMorphPacket: {}", e.getMessage());
            id = SENTINEL_UUID;
        }
        this.playerId = id;
        this.entity = buf.readBoolean() ? Optional.ofNullable(buf.readResourceLocation()) : Optional.empty();
        this.hideNametag = buf.readBoolean();
    }

    public void encode(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeBoolean(this.entity.isPresent());
        this.entity.ifPresent(buf::writeResourceLocation);
        buf.writeBoolean(this.hideNametag);
    }

    public static SyncMorphPacket of(UUID id, ResourceLocation rlOrNull) {
        return new SyncMorphPacket(id, Optional.ofNullable(rlOrNull), false);
    }

    public static SyncMorphPacket of(UUID id, Optional<ResourceLocation> entity) {
        return new SyncMorphPacket(id, entity, false);
    }

    public static SyncMorphPacket of(UUID id, Optional<ResourceLocation> entity, boolean hideNametag) {
        return new SyncMorphPacket(id, entity, hideNametag);
    }

    public void handle(Supplier<dev.architectury.networking.NetworkManager.PacketContext> contextSupplier) {
        var context = contextSupplier.get();
        context.queue(() -> {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                try {
                    if (this.playerId.equals(SENTINEL_UUID)) {
                        LOGGER.warn("How did I?! Uuuughh! Received malformed morph packet with invalid UUID");
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

                    IMorph morph = DataUtils.getMorphData(player);
                    if (morph == null) {
                        LOGGER.error("How did I?! Uuuughh! Failed to get morph data for player {}", this.playerId);
                        return;
                    }

                    Optional<ResourceLocation> validatedEntity = this.entity;
                    if (validatedEntity.isPresent() && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                            .containsKey(validatedEntity.get())) {
                        LOGGER.warn("Or, what. Rejected invalid morph entity from network: '{}'", validatedEntity.get());
                        validatedEntity = Optional.empty();
                    }
                    
                    morph.setEntityType(validatedEntity);
                    morph.setNametagHidden(this.hideNametag);
                    player.refreshDimensions();
                    LOGGER.debug("Want to see something neat? Synced morph {} (hideNametag={}) for {}", validatedEntity,
                            this.hideNametag, player.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("How did I?! Uuuughh! Failed to sync morph for player {}", this.playerId, e);
                }
            });
        });
    }
}
