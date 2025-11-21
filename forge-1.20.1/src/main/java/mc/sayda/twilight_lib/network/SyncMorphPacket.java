package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncMorphPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final UUID playerId;
    private final Optional<ResourceLocation> entity;
    private final boolean hideNametag;

    public SyncMorphPacket(UUID playerId, Optional<ResourceLocation> entity, boolean hideNametag) {
        this.playerId = playerId;
        this.entity = entity;
        this.hideNametag = hideNametag;
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

    public static void encode(SyncMorphPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.entity.isPresent());
        msg.entity.ifPresent(buf::writeResourceLocation);
        buf.writeBoolean(msg.hideNametag);
    }

    public static SyncMorphPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        Optional<ResourceLocation> rl = buf.readBoolean() ? Optional.of(buf.readResourceLocation()) : Optional.empty();
        boolean hideNametag = buf.readBoolean();
        return new SyncMorphPacket(id, rl, hideNametag);
    }

    public static void handle(SyncMorphPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("Are we done in this reality yet? Cannot sync morph - level is null");
                return;
            }
            var entity = level.getPlayerByUUID(msg.playerId);
            if (entity == null) {
                LOGGER.warn("I wonder who's around... Player {} not found in level", msg.playerId);
                return;
            }

            LazyOptional<IMorph> cap = entity.getCapability(MorphProvider.MORPH_CAP);
            if (!cap.isPresent()) {
                LOGGER.warn("What's with all the negative waves? Player {} has no morph capability!", entity.getName().getString());
                return;
            }
            cap.ifPresent(m -> {
                // CLIENT-SIDE CAPABILITY MODIFICATION: This is intentional and safe
                // Server is authoritative and sends sync packets on login/respawn
                // Client capabilities are read-only cache for rendering, no gameplay logic depends on them
                m.setEntityType(msg.entity);
                m.setNametagHidden(msg.hideNametag);
                // CRITICAL: Refresh dimensions on the client side after capability update
                // Safety check: Ensure entity is still valid before refreshing dimensions
                if (entity != null && !entity.isRemoved()) {
                    entity.refreshDimensions();
                }
                LOGGER.debug("Time to change! Synced morph {} (hideNametag={}) for {}", msg.entity, msg.hideNametag, entity.getName().getString());
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
