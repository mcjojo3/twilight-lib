package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncMorphPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation ID = new ResourceLocation(TwilightLib.MODID, "sync_morph");

    private final UUID playerId;
    private final Optional<ResourceLocation> entity;
    private final boolean hideNametag;

    public SyncMorphPacket(UUID playerId, Optional<ResourceLocation> entity, boolean hideNametag) {
        this.playerId = java.util.Objects.requireNonNull(playerId, "playerId");
        this.entity = entity != null ? entity : Optional.empty();
        this.hideNametag = hideNametag;
    }

    public SyncMorphPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.entity = buf.readBoolean() ? Optional.ofNullable(buf.readResourceLocation()) : Optional.empty();
        this.hideNametag = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
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
                    var player = context.getPlayer();
                    var level = (player != null) ? player.level()
                            : mc.sayda.twilight_lib.client.ClientAccess.getLevel();
                    if (level == null)
                        return;

                    var entity = level.getPlayerByUUID(this.playerId);
                    if (entity == null)
                        return;

                    IMorph morph = DataUtils.getMorphData(entity);
                    if (morph != null) {
                        Optional<ResourceLocation> validatedEntity = this.entity;
                        if (validatedEntity.isPresent() && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                                .containsKey(validatedEntity.get())) {
                            validatedEntity = Optional.empty();
                        }
                        morph.setEntityType(validatedEntity);
                        morph.setNametagHidden(this.hideNametag);
                        entity.refreshDimensions();
                        LOGGER.debug("Synced morph {} for {}", validatedEntity, entity.getName().getString());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to sync morph for player {}", this.playerId, e);
                }
            });
        });
    }
}
