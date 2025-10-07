package mc.sayda.twilight_lib.supporter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that fetches and caches supporter data from GitHub
 */
public class SupporterService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SUPPORTERS_URL = "https://raw.githubusercontent.com/mcjojo3/patreon-supporters/main/supporters.json";
    private static final long CACHE_DURATION_MS = 3600000; // 1 hour

    private static final Map<String, SupporterData> supporterCache = new ConcurrentHashMap<>();
    private static long lastFetchTime = 0;
    private static boolean fetchInProgress = false;

    /**
     * Fetch supporters list from GitHub (async, cached)
     */
    public static CompletableFuture<Void> fetchSupporters() {
        // Check cache validity
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFetchTime < CACHE_DURATION_MS && !supporterCache.isEmpty()) {
            LOGGER.debug("Using cached supporter data");
            return CompletableFuture.completedFuture(null);
        }

        // Prevent duplicate fetches
        if (fetchInProgress) {
            LOGGER.debug("Fetch already in progress, skipping");
            return CompletableFuture.completedFuture(null);
        }

        fetchInProgress = true;

        return CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Fetching supporter list from GitHub...");
                URL url = new URL(SUPPORTERS_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "TwilightLib-Minecraft-Mod");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        content.append(line);
                    }
                    in.close();

                    parseSupportersJson(content.toString());
                    lastFetchTime = System.currentTimeMillis();
                    LOGGER.info("Successfully fetched {} supporters! Thank you for your support! 💜", supporterCache.size());
                } else {
                    LOGGER.warn("Failed to fetch supporters list. Response code: {}", responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                LOGGER.error("Error fetching supporters list: {}", e.getMessage());
            } finally {
                fetchInProgress = false;
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
        try {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(jsonContent, JsonObject.class);
            JsonArray supporters = root.getAsJsonArray("supporters");

            Map<String, SupporterData> newCache = new ConcurrentHashMap<>();

            for (JsonElement element : supporters) {
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
            }

            supporterCache.clear();
            supporterCache.putAll(newCache);
        } catch (Exception e) {
            LOGGER.error("Error parsing supporters JSON: {}", e.getMessage());
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