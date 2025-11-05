package mc.sayda.twilight_lib.supporter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightConstants;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service that fetches and caches supporter data from GitHub
 */

public class SupporterService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUPPORTERS_URL = "https://raw.githubusercontent.com/mcjojo3/twilight-database/main/supporters.json";
    private static final long CACHE_DURATION_MS = 3600000; // 1 hour

    private static volatile Map<String, SupporterData> supporterCache = new ConcurrentHashMap<>();
    private static volatile long lastFetchTime = 0;
    private static final AtomicBoolean fetchInProgress = new AtomicBoolean(false);

    /**
     * Fetch supporters list from GitHub (async, cached)
     */
    public static CompletableFuture<Void> fetchSupporters() {
        // Check cache validity
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFetchTime < CACHE_DURATION_MS && !supporterCache.isEmpty()) {
            LOGGER.debug("Want to see something neat? Using cached supporter data");
            return CompletableFuture.completedFuture(null);
        }

        // Prevent duplicate fetches with atomic compare-and-set
        if (!fetchInProgress.compareAndSet(false, true)) {
            LOGGER.debug("Aaand a skip-skip and a jump-jump! Fetch already in progress, skipping");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            HttpURLConnection conn = null;
            try {
                LOGGER.info("I wonder who's around. Fetching supporter list from GitHub...");
                URL url = new URL(SUPPORTERS_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "TwilightLib-Minecraft-Mod");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder content = new StringBuilder();
                        String line;
                        int maxSizeBytes = TwilightConstants.Supporter.DEFAULT_MAX_JSON_SIZE_MB * 1024 * 1024;  // Convert MB to bytes

                        while ((line = in.readLine()) != null) {
                            content.append(line);

                            // Check size limit to prevent OOM attacks
                            if (content.length() > maxSizeBytes) {
                                LOGGER.error("Oh no! Supporter JSON exceeds size limit of {}MB", TwilightConstants.Supporter.DEFAULT_MAX_JSON_SIZE_MB);
                                return;  // Don't update cache
                            }
                        }

                        parseSupportersJson(content.toString());
                        lastFetchTime = System.currentTimeMillis();
                        LOGGER.info("We are going to be best friends! Successfully fetched {} supporters", supporterCache.size());
                    }
                } else {
                    LOGGER.warn("Are we done in this reality yet? Hello? Hellooo? Failed to fetch supporters list. Response code: {}", responseCode);
                }
            } catch (Exception e) {
                LOGGER.error("How did I?! Uuuughh! Error fetching supporters list", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
                fetchInProgress.set(false);
            }
        });
    }

    /**
     * Parse the supporters.json content
     *
     * Tier determines supporter status and auto-grants from SupporterRegistry.
     * Cosmetics field contains manual overrides that persist even if tier changes/expires.
     */
    private static void parseSupportersJson(String jsonContent) {
        Map<String, SupporterData> newCache = new ConcurrentHashMap<>();

        try {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
            JsonArray supporters = root.getAsJsonArray("supporters");

            for (JsonElement element : supporters) {
                try {
                    JsonObject supporter = element.getAsJsonObject();

                    String uuid = supporter.get("uuid").getAsString();
                    String name = supporter.has("name") ? supporter.get("name").getAsString() : "Unknown";

                    // Tier: null or "none" = not a supporter, but can still have manual cosmetics
                    String tier = supporter.has("tier") ? supporter.get("tier").getAsString() : null;

                    // Parse manual cosmetic overrides (optional field)
                    Set<String> manualTrails = new HashSet<>();
                    Set<String> manualAddons = new HashSet<>();
                    Set<String> manualEffects = new HashSet<>();

                    if (supporter.has("cosmetics")) {
                        JsonObject cosmetics = supporter.getAsJsonObject("cosmetics");
                        if (cosmetics.has("trails")) {
                            manualTrails = jsonArrayToSet(cosmetics.getAsJsonArray("trails"));
                        }
                        if (cosmetics.has("addons")) {
                            manualAddons = jsonArrayToSet(cosmetics.getAsJsonArray("addons"));
                        }
                        if (cosmetics.has("effects")) {
                            manualEffects = jsonArrayToSet(cosmetics.getAsJsonArray("effects"));
                        }
                    }

                    SupporterData data = new SupporterData(uuid, name, tier, manualTrails, manualAddons, manualEffects);
                    newCache.put(uuid, data);
                } catch (Exception entryError) {
                    // Skip malformed entries but continue parsing others
                    LOGGER.warn("Shoot! Skipping malformed supporter entry: {}", entryError.getMessage());
                }
            }

            // Only update cache if we successfully parsed at least some data
            // Atomic replacement instead of clear+putAll to avoid empty cache window
            if (!newCache.isEmpty()) {
                supporterCache = newCache;
            } else {
                LOGGER.warn("Really?! Parsed JSON contained no valid supporter entries - keeping old cache");
            }
        } catch (Exception e) {
            // Fatal JSON parsing error - keep old cache intact
            LOGGER.error("Oh, dung beetles! Fatal error parsing supporters JSON - keeping old cache", e);
        }
    }

    /**
     * Helper to convert JsonArray to Set<String>
     */
    private static Set<String> jsonArrayToSet(JsonArray array) {
        Set<String> set = new HashSet<>();
        if (array != null) {
            for (JsonElement element : array) {
                set.add(element.getAsString());
            }
        }
        return set;
    }

    /**
     * Check if a player UUID is a supporter
     */
    public static boolean isSupporter(String uuid) {
        return supporterCache.containsKey(uuid);
    }

    /**
     * Get supporter data by UUID
     */
    public static Optional<SupporterData> getSupporterData(String uuid) {
        return Optional.ofNullable(supporterCache.get(uuid));
    }

    /**
     * Get all supporters (for debugging/admin commands)
     */
    public static Collection<SupporterData> getAllSupporters() {
        return new ArrayList<>(supporterCache.values());
    }

    /**
     * Force refresh the supporter cache
     */
    public static CompletableFuture<Void> forceRefresh() {
        lastFetchTime = 0;
        return fetchSupporters();
    }
}