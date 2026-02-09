package mc.sayda.twilight_lib.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for player model variants synced from server.
 *
 * <p>
 * <b>Why a cache?</b> Model variant data is stored in capabilities/attachments
 * on the server.
 * When rendering other players on the client, we can't access their server-side
 * capabilities.
 * The server sends {@link mc.sayda.twilight_lib.network.SyncModelVariantPacket}
 * to sync this data,
 * and we store it in this cache for the
 * {@link mc.sayda.twilight_lib.mixin.PlayerInfoMixin} to access.
 *
 * <p>
 * <b>Thread Safety</b>: Uses {@link ConcurrentHashMap} because model variants
 * may be synced
 * from network thread while being read from render thread.
 *
 * <p>
 * <b>Cache Lifecycle</b>:
 * <ul>
 * <li><b>Add</b>: When
 * {@link mc.sayda.twilight_lib.network.SyncModelVariantPacket} is received</li>
 * <li><b>Read</b>: When {@link mc.sayda.twilight_lib.mixin.PlayerInfoMixin}
 * needs model type</li>
 * <li><b>Clear</b>: When player disconnects or world unloads</li>
 * </ul>
 *
 * <p>
 * <b>Memory Management</b>: Cache is automatically cleared on world unload to
 * prevent
 * memory leaks when switching worlds/servers.
 *
 * @author SaydaGames (mc_jojo3)
 * @version 1.0
 */
public class ClientModelVariantCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        // Model variant cache initialization
    }

    /**
     * Thread-safe cache mapping player UUID to model variant ResourceLocation.
     *
     * <p>
     * <b>Why ConcurrentHashMap?</b> Network packets arrive on network thread,
     * but rendering happens on render thread. Concurrent access requires
     * thread-safe map.
     */
    private static final Map<UUID, net.minecraft.resources.ResourceLocation> MODEL_VARIANTS = new ConcurrentHashMap<>();

    /**
     * Stores a player's model variant in the client-side cache.
     *
     * <p>
     * Called by {@link mc.sayda.twilight_lib.network.SyncModelVariantPacket}
     * handler
     * when the server syncs model variant data to the client.
     *
     * @param playerUUID   The UUID of the player
     * @param modelVariant The model variant ResourceLocation
     */
    public static void setModelVariant(UUID playerUUID, net.minecraft.resources.ResourceLocation modelVariant) {
        if (modelVariant == null) {
            MODEL_VARIANTS.remove(playerUUID);
            LOGGER.debug("Time to change! Cleared model variant for player {}", playerUUID);
        } else {
            MODEL_VARIANTS.put(playerUUID, modelVariant);
            LOGGER.debug("Time to change! Cached model variant {} for player {}", modelVariant, playerUUID);
        }
    }

    /**
     * Retrieves a player's model variant from the client-side cache.
     *
     * <p>
     * Called by {@link mc.sayda.twilight_lib.mixin.PlayerInfoMixin} when
     * determining
     * which player model to render.
     *
     * @param playerUUID The UUID of the player
     * @return The model variant ResourceLocation, or null if not cached
     */
    public static net.minecraft.resources.ResourceLocation getModelVariant(UUID playerUUID) {
        return MODEL_VARIANTS.get(playerUUID);
    }

    /**
     * Removes a player's model variant from the cache.
     *
     * <p>
     * Should be called when a player disconnects to prevent stale data.
     *
     * @param playerUUID The UUID of the player
     */
    public static void removePlayer(UUID playerUUID) {
        MODEL_VARIANTS.remove(playerUUID);
        LOGGER.debug("Goodbye, my new friend! Removed model variant cache for player {}", playerUUID);
    }

    /**
     * Clears all cached model variants.
     *
     * <p>
     * Should be called when disconnecting from server or unloading world
     * to prevent memory leaks and stale data.
     */
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
