package mc.sayda.twilight_lib.client;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for player model variants synced from server.
 *
 * <p><b>Why a cache?</b> Model variant data is stored in capabilities/attachments on the server.
 * When rendering other players on the client, we can't access their server-side capabilities.
 * The server sends {@link mc.sayda.twilight_lib.network.SyncModelVariantPacket} to sync this data,
 * and we store it in this cache for the {@link mc.sayda.twilight_lib.mixin.PlayerInfoMixin} to access.
 *
 * <p><b>Thread Safety</b>: Uses {@link ConcurrentHashMap} because model variants may be synced
 * from network thread while being read from render thread.
 *
 * <p><b>Cache Lifecycle</b>:
 * <ul>
 *   <li><b>Add</b>: When {@link mc.sayda.twilight_lib.network.SyncModelVariantPacket} is received</li>
 *   <li><b>Read</b>: When {@link mc.sayda.twilight_lib.mixin.PlayerInfoMixin} needs model type</li>
 *   <li><b>Clear</b>: When player disconnects or world unloads</li>
 * </ul>
 *
 * <p><b>Memory Management</b>: Cache is automatically cleared on world unload to prevent
 * memory leaks when switching worlds/servers.
 *
 * @author SaydaGames (mc_jojo3)
 * @version 1.0
 */
@EventBusSubscriber(modid = mc.sayda.twilight_lib.TwilightLib.MODID, value = Dist.CLIENT)
public class ClientModelVariantCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Thread-safe cache mapping player UUID to model variant ("steve" or "alex").
     *
     * <p><b>Why ConcurrentHashMap?</b> Network packets arrive on network thread,
     * but rendering happens on render thread. Concurrent access requires thread-safe map.
     */
    private static final Map<UUID, String> MODEL_VARIANTS = new ConcurrentHashMap<>();

    /**
     * Stores a player's model variant in the client-side cache.
     *
     * <p>Called by {@link mc.sayda.twilight_lib.network.SyncModelVariantPacket} handler
     * when the server syncs model variant data to the client.
     *
     * @param playerUUID The UUID of the player
     * @param modelVariant The model variant ("steve" or "alex")
     */
    public static void setModelVariant(UUID playerUUID, String modelVariant) {
        if (modelVariant == null || modelVariant.isEmpty()) {
            MODEL_VARIANTS.remove(playerUUID);
            LOGGER.debug("Cleared model variant for player {}", playerUUID);
        } else {
            MODEL_VARIANTS.put(playerUUID, modelVariant);
            LOGGER.debug("Cached model variant {} for player {}", modelVariant, playerUUID);
        }
    }

    /**
     * Retrieves a player's model variant from the client-side cache.
     *
     * <p>Called by {@link mc.sayda.twilight_lib.mixin.PlayerInfoMixin} when determining
     * which player model to render.
     *
     * @param playerUUID The UUID of the player
     * @return The model variant ("steve" or "alex"), or null if not cached
     */
    public static String getModelVariant(UUID playerUUID) {
        return MODEL_VARIANTS.get(playerUUID);
    }

    /**
     * Removes a player's model variant from the cache.
     *
     * <p>Should be called when a player disconnects to prevent stale data.
     *
     * @param playerUUID The UUID of the player
     */
    public static void removePlayer(UUID playerUUID) {
        MODEL_VARIANTS.remove(playerUUID);
        LOGGER.debug("Removed model variant cache for player {}", playerUUID);
    }

    /**
     * Clears all cached model variants.
     *
     * <p>Should be called when disconnecting from server or unloading world
     * to prevent memory leaks and stale data.
     */
    public static void clear() {
        int size = MODEL_VARIANTS.size();
        MODEL_VARIANTS.clear();
        LOGGER.debug("Cleared {} model variants from cache", size);
    }

    /**
     * Gets the number of cached model variants (for debugging).
     *
     * @return The number of players with cached model variants
     */
    public static int getCacheSize() {
        return MODEL_VARIANTS.size();
    }

    /**
     * Event handler: Clears cache when client disconnects from server.
     * Prevents stale data from persisting across server/world changes.
     */
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut evt) {
        clear();
        LOGGER.debug("Goodbye, my new friend! Cleared model variant cache on disconnect.");
    }

    /**
     * Event handler: Clears cache when level unloads.
     * Prevents memory leaks when switching worlds.
     */
    @SubscribeEvent
    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload evt) {
        clear();
        LOGGER.debug("I hope this world survives... Cleared model variant cache on level unload.");
    }
}
