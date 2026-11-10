package mc.sayda.twilight_lib.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model variants synced from the server, read by PlayerInfoMixin. Cleared when
 * leaving a world.
 *
 * @author SaydaGames (mc_jojo3)
 */
public class ClientModelVariantCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        // Model variant cache initialization
    }

    /** Player UUID to model variant. Concurrent because packets and rendering run on different threads. */
    private static final Map<UUID, net.minecraft.resources.ResourceLocation> MODEL_VARIANTS = new ConcurrentHashMap<>();

    /** Stores a player's model variant when the sync packet arrives. */
    public static void setModelVariant(UUID playerUUID, net.minecraft.resources.ResourceLocation modelVariant) {
        if (modelVariant == null) {
            MODEL_VARIANTS.remove(playerUUID);
            LOGGER.debug("Want to see something neat? Cleared model variant for player {}", playerUUID);
        } else {
            MODEL_VARIANTS.put(playerUUID, modelVariant);
            LOGGER.debug("Want to see something neat? Cached model variant {} for player {}", modelVariant, playerUUID);
        }
    }

    /** A player's model variant, or null if not cached. */
    public static net.minecraft.resources.ResourceLocation getModelVariant(UUID playerUUID) {
        return MODEL_VARIANTS.get(playerUUID);
    }

    /** Removes a player's entry, e.g. when they disconnect. */
    public static void removePlayer(UUID playerUUID) {
        MODEL_VARIANTS.remove(playerUUID);
        LOGGER.debug("Goodbye, my new friend! Removed model variant cache for player {}", playerUUID);
    }

    /** Clears the cache when leaving a world or server. */
    public static void clear() {
        int size = MODEL_VARIANTS.size();
        MODEL_VARIANTS.clear();
        LOGGER.debug("Goodbye, my new friend! Cleared {} model variants from cache", size);
    }

    /**
     * Gets the number of cached model variants (for debugging).
     *
     * @return The number of players with cached model variants
     */
    public static int getCacheSize() {
        return MODEL_VARIANTS.size();
    }
}
