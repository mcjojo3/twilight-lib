package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IModelVariant;
import mc.sayda.twilight_lib.capabilities.ModelVariantProvider;
import mc.sayda.twilight_lib.client.ClientModelVariantCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network packet to sync player model variant (Steve/Alex) from server to client.
 * Sent on login, respawn, and when variant changes.
 */
public class SyncModelVariantPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final UUID playerId;
    private final String modelVariant;  // "steve" or "alex", or null for default
    private final boolean hasCustomVariant;

    public SyncModelVariantPacket(UUID playerId, String modelVariant, boolean hasCustomVariant) {
        this.playerId = playerId;
        this.modelVariant = modelVariant;
        this.hasCustomVariant = hasCustomVariant;
    }

    public static SyncModelVariantPacket of(UUID id, IModelVariant modelVariant) {
        return new SyncModelVariantPacket(
            id,
            modelVariant.getModelVariant(),
            modelVariant.hasCustomVariant()
        );
    }

    public static void encode(SyncModelVariantPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        // Limit string length to 16 chars (sufficient for "steve"/"alex")
        buf.writeUtf(msg.modelVariant, 16);
        buf.writeBoolean(msg.hasCustomVariant);
    }

    public static SyncModelVariantPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        // Limit string length to 16 chars to prevent malicious large strings
        String variant = buf.readUtf(16);
        boolean hasCustom = buf.readBoolean();

        // Whitelist validation - only allow "steve" or "alex" to prevent injection attacks
        if (variant != null && !variant.equals("steve") && !variant.equals("alex")) {
            LOGGER.warn("Or, what. Rejected invalid model variant from network: '{}' (only 'steve' or 'alex' allowed)", variant);
            variant = "steve"; // Default to steve for safety
        }

        return new SyncModelVariantPacket(id, variant, hasCustom);
    }

    public static void handle(SyncModelVariantPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Update client-side cache for mixin to use (works even if entity not loaded yet)
            if (!msg.hasCustomVariant) {
                ClientModelVariantCache.setModelVariant(msg.playerId, null);
            } else {
                ClientModelVariantCache.setModelVariant(msg.playerId, msg.modelVariant);
            }

            var level = Minecraft.getInstance().level;
            if (level == null) {
                LOGGER.warn("Are we done in this reality yet? Cannot sync model variant - level is null");
                return;
            }
            var entity = level.getPlayerByUUID(msg.playerId);
            if (entity == null) {
                LOGGER.debug("I wonder who's around... Player {} not found in level (cached anyway)", msg.playerId);
                return;
            }

            LazyOptional<IModelVariant> cap = entity.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP);
            if (!cap.isPresent()) {
                LOGGER.warn("What's with all the negative waves? Player {} has no model variant capability!", entity.getName().getString());
                return;
            }
            cap.ifPresent(m -> {
                // CLIENT-SIDE CAPABILITY MODIFICATION: This is intentional and safe
                // Server is authoritative and sends sync packets on login/respawn
                // Client capabilities are read-only cache for rendering, no gameplay logic depends on them
                if (!msg.hasCustomVariant) {
                    m.clearCustomVariant();
                } else {
                    m.setModelVariant(msg.modelVariant);
                }
                LOGGER.debug("Time to change! Synced model variant {} for {}", msg.modelVariant, entity.getName().getString());
            });
        });
        ctx.get().setPacketHandled(true);
    }
}