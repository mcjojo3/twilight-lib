package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.cosmetics.SpawnEffectHandler;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record SyncEffectsPacket(UUID playerId, Set<String> effects, boolean triggerSpawnEffect)
        implements CustomPacketPayload {
    public SyncEffectsPacket {
        java.util.Objects.requireNonNull(playerId, "playerId");
        effects = effects != null ? effects : java.util.Collections.emptySet();
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CustomPacketPayload.Type<SyncEffectsPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "sync_effects"));

    // Sentinel UUID for malformed packets
    private static final @Nonnull UUID SENTINEL_UUID = new UUID(0, 0);

    // Custom UUID codec (encodes as two longs)
    private static final StreamCodec<ByteBuf, UUID> UUID_CODEC = new StreamCodec<>() {
        @Override
        public @Nonnull UUID decode(@Nonnull ByteBuf buf) {
            try {
                return new UUID(buf.readLong(), buf.readLong());
            } catch (Exception e) {
                LOGGER.warn("How did I?! Uuuughh! Failed to decode UUID in SyncEffectsPacket: {}", e.getMessage());
                return SENTINEL_UUID; // Return sentinel on decode error
            }
        }

        @Override
        public void encode(@Nonnull ByteBuf buf, @Nonnull UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    public static final StreamCodec<ByteBuf, SyncEffectsPacket> STREAM_CODEC = StreamCodec.composite(
            UUID_CODEC,
            SyncEffectsPacket::playerId,
            // Hardcoded max size (128) to avoid NeoForge crash from config not being loaded
            // at static init
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, 128),
            SyncEffectsPacket::effects,
            ByteBufCodecs.BOOL,
            SyncEffectsPacket::triggerSpawnEffect,
            SyncEffectsPacket::new);

    // Convenience constructor without trigger flag (defaults to false)
    public SyncEffectsPacket(UUID playerId, Set<String> effects) {
        this(playerId, effects, false);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncEffectsPacket msg, dev.architectury.networking.NetworkManager.PacketContext context) {
        context.queue(() ->
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> ClientHandler.apply(msg)));
    }

    @Environment(EnvType.CLIENT)
    public static void clientApply(SyncEffectsPacket msg) {
        ClientHandler.apply(msg);
    }

    @Environment(EnvType.CLIENT)
    private static final class ClientHandler {
        static void apply(SyncEffectsPacket msg) {
            try {
                if (msg.playerId().equals(SENTINEL_UUID)) {
                    LOGGER.error("How did I?! Uuuughh! Received malformed SyncEffectsPacket with invalid UUID");
                    return;
                }

                net.minecraft.world.entity.player.Player entity = null;
                net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.player != null && minecraft.player.getUUID().equals(msg.playerId())) {
                    entity = minecraft.player;
                } else if (minecraft.level != null) {
                    entity = minecraft.level.getPlayerByUUID(msg.playerId());
                }
                if (entity == null) {
                    LOGGER.warn("Or, what. Player {} not found in level (cached anyway)", msg.playerId());
                    return;
                }

                var effects = DataUtils.getEffectsData(entity);
                if (effects == null) {
                    LOGGER.error("How did I?! Uuuughh! Failed to get effects data for player {}", msg.playerId());
                    return;
                }

                effects.syncEquippedFromPacket(msg.effects());
                LOGGER.debug("Want to see something neat? Synced {} active effects for {}", msg.effects().size(),
                        entity.getName().getString());

                if (msg.triggerSpawnEffect() && !msg.effects().isEmpty()) {
                    LOGGER.debug("More sparkles, now! Scheduling spawn effect for player {}",
                            entity.getName().getString());
                    SpawnEffectHandler.scheduleSpawnEffect(msg.playerId());
                }
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Failed to sync effects for player {}", msg.playerId(), e);
            }
        }
    }
}
