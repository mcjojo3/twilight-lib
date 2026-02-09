package mc.sayda.twilight_lib.supporter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.config.TwilightConfig;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service that fetches and caches supporter data from GitHub
 */

public class SupporterService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUPPORTERS_URL = "https://raw.githubusercontent.com/mcjojo3/twilight-database/main/supporters.json";

    private static final AtomicReference<Map<String, SupporterData>> supporterCache = new AtomicReference<>(
            new ConcurrentHashMap<>());
    private static volatile long lastFetchTime = 0;
    private static final AtomicReference<CompletableFuture<Void>> activeFetch = new AtomicReference<>(null);

    public static CompletableFuture<Void> fetchSupporters() {
        // Check cache validity (convert minutes to milliseconds)
        long currentTime = System.currentTimeMillis();
        long cacheDurationMinutes = 60; // Default fallback
        try {
            cacheDurationMinutes = TwilightConfig.SUPPORTER_CACHE_DURATION_MINUTES.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet
        }
        long cacheDurationMs = cacheDurationMinutes * 60L * 1000L;

        // Return current fetch if valid OR return existing in-progress fetch
        if (currentTime - lastFetchTime < cacheDurationMs && !supporterCache.get().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // Atomic Check-and-Return existing future
        CompletableFuture<Void> currentFetch = activeFetch.get();
        if (currentFetch != null && !currentFetch.isDone()) {
            return currentFetch;
        }

        // Start new fetch
        CompletableFuture<Void> nextFetch = CompletableFuture.runAsync(() -> {
            try {
                boolean primarySuccess = false;
                try {
                    LOGGER.info("Twilight Lib: Refreshing supporter data...");
                    if (fetchFromUrlWithRetries(SUPPORTERS_URL)) {
                        primarySuccess = true;
                        lastFetchTime = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    LOGGER.warn("Twilight Lib: Primary URL failed: {}", e.getMessage());
                }

                if (!primarySuccess) {
                    try {
                        String backupUrl = TwilightConfig.SUPPORTER_BACKUP_URL.get();
                        if (fetchFromUrlWithRetries(backupUrl)) {
                            lastFetchTime = System.currentTimeMillis();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Twilight Lib: Both supporter URLs failed: {}", e.getMessage());
                    }
                }
            } finally {
                activeFetch.set(null); // Clear active future when done
            }
        });

        // Try to set it as the active fetch. If someone else beat us to it, return
        // theirs.
        if (activeFetch.compareAndSet(null, nextFetch)) {
            return nextFetch;
        } else {
            return activeFetch.get();
        }
    }

    /**
     * Fetch supporters from a specific URL with retry logic
     * 
     * @param urlString The URL to fetch from
     * @return true if successful, false otherwise
     */
    private static boolean fetchFromUrlWithRetries(String urlString) {
        int maxRetries = 3; // Default fallback
        int retryDelay = 2000; // Default fallback
        try {
            maxRetries = TwilightConfig.SUPPORTER_FETCH_MAX_RETRIES.get();
            retryDelay = TwilightConfig.SUPPORTER_FETCH_RETRY_DELAY_MS.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet, use defaults
        }

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                LOGGER.info("While I wait, I will stay happy! Retry attempt {}/{} for {}", attempt, maxRetries,
                        urlString);
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            if (fetchFromUrl(urlString)) {
                if (attempt > 0) {
                    LOGGER.info("We are going to be best friends! Retry successful on attempt {}", attempt + 1);
                }
                return true;
            }
        }

        LOGGER.warn("How did I?! Uuuughh! All {} retry attempts failed for {}", maxRetries + 1, urlString);
        return false;
    }

    /**
     * Fetch supporters from a specific URL
     * 
     * @param urlString The URL to fetch from
     * @return true if successful, false otherwise
     */
    private static boolean fetchFromUrl(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(urlString).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            // Use defaults if config not yet loaded
            int connectTimeout = 5000; // Default fallback
            int readTimeout = 5000; // Default fallback
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
                // Optimized: Parse directly from stream to save memory
                try (InputStreamReader isr = new InputStreamReader(conn.getInputStream())) {
                    parseSupportersJsonFromReader(isr);
                    return true;
                }
            } else {
                LOGGER.warn("How did I?! Uuuughh! Failed to fetch from {}. Response code: {}", urlString, responseCode);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Error fetching from {}: {}", urlString, e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Parse the supporters.json content
     *
     * Tier determines supporter status and auto-grants from SupporterRegistry.
     * Cosmetics field contains manual overrides that persist even if tier
     * changes/expires.
     */
    private static void parseSupportersJsonFromReader(InputStreamReader reader) {
        Map<String, SupporterData> newCache = new ConcurrentHashMap<>();
        try {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            // Validate root object exists
            if (root == null) {
                LOGGER.error(
                        "Is this the best physical representation you can manifest? JSON parsing returned null root object");
                return;
            }

            // Validate supporters array exists
            if (!root.has("supporters")) {
                LOGGER.error(
                        "Is this the best physical representation you can manifest? JSON missing 'supporters' array");
                return;
            }

            JsonArray supporters = root.getAsJsonArray("supporters");
            if (supporters == null) {
                LOGGER.error("Is this the best physical representation you can manifest? 'supporters' array is null");
                return;
            }

            for (JsonElement element : supporters) {
                try {
                    // Safety check: prevent unbounded cache growth from malicious/corrupted JSON
                    int maxSupporters = 100000; // Default fallback
                    try {
                        maxSupporters = TwilightConfig.MAX_SUPPORTERS.get();
                    } catch (IllegalStateException ex) {
                        // Config not loaded yet, use default
                    }
                    if (newCache.size() >= maxSupporters) {
                        LOGGER.error(
                                "Or, what. Supporter list exceeds maximum size of {}. Truncating remaining entries to prevent memory exhaustion.",
                                maxSupporters);
                        break; // Stop parsing, use what we have
                    }

                    JsonObject supporter = element.getAsJsonObject();

                    // Validate UUID field exists and is not null
                    if (!supporter.has("uuid") || supporter.get("uuid").isJsonNull()) {
                        LOGGER.warn(
                                "Is this the best physical representation you can manifest? Skipping supporter entry without UUID");
                        continue;
                    }

                    String uuid = validateJsonString(supporter.get("uuid").getAsString(), "uuid");

                    // Validate UUID format
                    try {
                        UUID.fromString(uuid);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn(
                                "Is this the best physical representation you can manifest? Invalid UUID format '{}', skipping supporter entry",
                                uuid);
                        continue;
                    }

                    String name = supporter.has("name")
                            ? validateJsonString(supporter.get("name").getAsString(), "name")
                            : "Unknown";

                    // Tier: null or "none" = not a supporter, but can still have manual cosmetics
                    String tier = supporter.has("tier")
                            ? validateJsonString(supporter.get("tier").getAsString(), "tier")
                            : null;

                    // Validate tier against known tiers
                    if (tier != null && !tier.equals("none")) {
                        Set<String> VALID_TIERS = Set.of("stone", "bronze", "silver", "gold", "platinum");
                        if (!VALID_TIERS.contains(tier.toLowerCase())) {
                            LOGGER.warn("Or, what. Unknown supporter tier '{}' for {}. Treating as no tier.", tier,
                                    name);
                            tier = null;
                        }
                    }

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
                    LOGGER.warn("This will be fine! Things break all the time. Skipping malformed supporter entry: {}",
                            entryError.getMessage());
                }
            }

            // Only update cache if we successfully parsed at least some data
            // Atomic replacement to avoid race conditions
            if (!newCache.isEmpty()) {
                supporterCache.set(newCache);
            } else {
                LOGGER.warn(
                        "Wait... they aren't coming back? Parsed JSON contained no valid supporter entries - keeping old cache");
            }
        } catch (Exception e) {
            // Fatal JSON parsing error - keep old cache intact
            LOGGER.error("How did I?! Uuuughh! Fatal error parsing supporters JSON - keeping old cache", e);
        }
    }

    /**
     * Validate and extract string from JSON with length check
     */
    private static String validateJsonString(String value, String fieldName) {
        int maxLength = 1000;
        try {
            maxLength = TwilightConfig.MAX_JSON_FIELD_LENGTH.get();
        } catch (Exception e) {
        }

        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException("JSON field '" + fieldName + "' too long: " + value.length() + " (max "
                    + maxLength + ")");
        }
        return value;
    }

    /**
     * Helper to convert JsonArray to Set<String>
     */
    private static Set<String> jsonArrayToSet(JsonArray array) {
        Set<String> set = new HashSet<>();
        if (array != null) {
            // Validate array size to prevent memory exhaustion
            int maxSize = 500;
            try {
                maxSize = TwilightConfig.MAX_COSMETIC_LIST_SIZE.get();
            } catch (Exception e) {
            }

            if (array.size() > maxSize) {
                LOGGER.error("Or, what. Cosmetic array too large: {} items (max {})", array.size(), maxSize);
                return set; // Return empty set
            }
            for (JsonElement element : array) {
                String value = element.getAsString();
                set.add(validateJsonString(value, "array element"));
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
