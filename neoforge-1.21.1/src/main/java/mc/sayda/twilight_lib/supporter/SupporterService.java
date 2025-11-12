package mc.sayda.twilight_lib.supporter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.config.TwilightConfig;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service that fetches and caches supporter data from GitHub
 */

public class SupporterService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUPPORTERS_URL = "https://raw.githubusercontent.com/mcjojo3/twilight-database/main/supporters.json";

    private static final AtomicReference<Map<String, SupporterData>> supporterCache = new AtomicReference<>(new ConcurrentHashMap<>());
    private static volatile long lastFetchTime = 0;
    private static final AtomicBoolean fetchInProgress = new AtomicBoolean(false);

    /**
     * Fetch supporters list from GitHub (async, cached)
     */
    public static CompletableFuture<Void> fetchSupporters() {
        // Check cache validity (convert minutes to milliseconds)
        long currentTime = System.currentTimeMillis();
        // Use default if config not yet loaded (during mod initialization)
        long cacheDurationMinutes = TwilightConstants.Supporter.DEFAULT_CACHE_DURATION_MINUTES;
        try {
            cacheDurationMinutes = TwilightConfig.SUPPORTER_CACHE_DURATION_MINUTES.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet, use default
        }
        long cacheDurationMs = cacheDurationMinutes * 60L * 1000L;
        if (currentTime - lastFetchTime < cacheDurationMs && !supporterCache.get().isEmpty()) {
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
                // Use defaults if config not yet loaded
                int connectTimeout = TwilightConstants.Supporter.DEFAULT_CONNECT_TIMEOUT_MS;
                int readTimeout = TwilightConstants.Supporter.DEFAULT_READ_TIMEOUT_MS;
                try {
                    connectTimeout = TwilightConfig.SUPPORTER_CONNECT_TIMEOUT_MS.get();
                    readTimeout = TwilightConfig.SUPPORTER_READ_TIMEOUT_MS.get();
                } catch (IllegalStateException e) {
                    // Config not loaded yet, use defaults
                }
                conn.setConnectTimeout(connectTimeout);
                conn.setReadTimeout(readTimeout);
                conn.setRequestProperty("User-Agent", "TwilightLib-Minecraft-Mod");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (InputStreamReader isr = new InputStreamReader(conn.getInputStream());
                         BufferedReader in = new BufferedReader(isr)) {
                        StringBuilder content = new StringBuilder();
                        String line;

                        // Validate config value to prevent integer overflow and unreasonable memory usage
                        int maxSizeMB = TwilightConstants.Supporter.DEFAULT_MAX_JSON_SIZE_MB;
                        try {
                            maxSizeMB = TwilightConfig.MAX_SUPPORTER_JSON_SIZE.get();
                        } catch (IllegalStateException e) {
                            // Config not loaded yet, use default
                        }
                        if (maxSizeMB <= 0 || maxSizeMB > 100) { // Reasonable max: 100MB for JSON
                            LOGGER.error("Invalid max JSON size: {}MB (must be between 1-100). Using default 10MB", maxSizeMB);
                            maxSizeMB = 10;
                        }
                        long maxSizeBytes = (long) maxSizeMB * 1024L * 1024L;  // Convert MB to bytes (force long arithmetic)

                        while ((line = in.readLine()) != null) {
                            // Check size limit BEFORE appending to prevent OOM attacks
                            if (content.length() + line.length() + 1 > maxSizeBytes) {
                                LOGGER.error("Oh no! Supporter JSON exceeds size limit of {}MB", maxSizeMB);
                                return;  // Don't update cache
                            }
                            content.append(line).append('\n');
                        }

                        parseSupportersJson(content.toString());
                        lastFetchTime = System.currentTimeMillis();
                        LOGGER.info("We are going to be best friends! Successfully fetched {} supporters", supporterCache.get().size());
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

            // Validate root object exists
            if (root == null) {
                LOGGER.error("Really?! JSON parsing returned null root object");
                return;
            }

            // Validate supporters array exists
            if (!root.has("supporters")) {
                LOGGER.error("Oh, dung beetles! JSON missing 'supporters' array");
                return;
            }

            JsonArray supporters = root.getAsJsonArray("supporters");
            if (supporters == null) {
                LOGGER.error("Shoot! 'supporters' array is null");
                return;
            }

            for (JsonElement element : supporters) {
                try {
                    // Safety check: prevent unbounded cache growth from malicious/corrupted JSON
                    int maxSupporters = TwilightConstants.Supporter.DEFAULT_MAX_SUPPORTERS;
                    try {
                        maxSupporters = TwilightConfig.MAX_SUPPORTERS.get();
                    } catch (IllegalStateException ex) {
                        // Config not loaded yet, use default
                    }
                    if (newCache.size() >= maxSupporters) {
                        LOGGER.error("Supporter list exceeds maximum size of {}. Truncating remaining entries to prevent memory exhaustion.", maxSupporters);
                        break; // Stop parsing, use what we have
                    }

                    JsonObject supporter = element.getAsJsonObject();

                    // Validate UUID field exists and is not null
                    if (!supporter.has("uuid") || supporter.get("uuid").isJsonNull()) {
                        LOGGER.warn("Is this the best physical representation you can manifest? Skipping supporter entry without UUID");
                        continue;
                    }

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
            // Atomic replacement to avoid race conditions
            if (!newCache.isEmpty()) {
                supporterCache.set(newCache);
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
        return supporterCache.get().containsKey(uuid);
    }

    /**
     * Get supporter data by UUID
     */
    public static Optional<SupporterData> getSupporterData(String uuid) {
        return Optional.ofNullable(supporterCache.get().get(uuid));
    }

    /**
     * Get all supporters (for debugging/admin commands)
     */
    public static Collection<SupporterData> getAllSupporters() {
        return new ArrayList<>(supporterCache.get().values());
    }

    /**
     * Force refresh the supporter cache
     */
    public static CompletableFuture<Void> forceRefresh() {
        lastFetchTime = 0;
        return fetchSupporters();
    }
}