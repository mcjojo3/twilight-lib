package mc.sayda.twilight_lib.client.tint;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.config.TwilightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bakes a base texture and a "mask" texture into a single tinted texture:
 * mask pixel ALPHA controls how strongly the chosen tint color multiplies
 * into the base pixel at that spot (transparent = untouched, opaque = fully
 * tinted). Alpha rather than brightness/RGB is used deliberately - many image
 * editors leave undefined/black RGB behind on erased (fully transparent)
 * pixels, which would otherwise silently read as "don't tint" regardless of
 * what the artist intended. Painting a mask is then just: draw white where
 * tinting should apply, erase (transparent) where it shouldn't. Results are
 * cached and registered as dynamic textures so normal rendering code can
 * bind them like any other {@link ResourceLocation}.
 */
public final class TintTextureCompositor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_CACHE_SIZE = 64;
    private static final int WHITE = 0xFFFFFF;

    // Keyed by "base|mask|tintHex" -> the registered dynamic texture location.
    // Access-ordered so eviction removes the least recently used composite.
    private static final Map<String, ResourceLocation> CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ResourceLocation> eldest) {
            int max;
            try {
                max = TwilightConfig.MAX_TINT_TEXTURE_CACHE_SIZE.get();
            } catch (Exception e) {
                max = DEFAULT_CACHE_SIZE;
            }
            if (size() > max) {
                Minecraft.getInstance().getTextureManager().release(eldest.getValue());
                return true;
            }
            return false;
        }
    };

    // Prevents log spam when the same broken (base, mask) pair is requested every frame.
    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    // Auto-discovered mask per base texture, or Optional.empty() if none exists.
    // Resolved lazily on first lookup per texture so this is a one-time resource-
    // manager check per addon per session, not a per-frame cost.
    private static final Map<ResourceLocation, Optional<ResourceLocation>> MASK_RESOLUTION_CACHE = new ConcurrentHashMap<>();

    private TintTextureCompositor() {
    }

    /**
     * Auto-discovers a mask for {@code texture} by progressively stripping
     * trailing "_token" segments from its filename and checking whether a
     * same-named file exists under that texture's "mask/" subfolder, e.g.
     * {@code textures/addon/kitsune_ears_black.png} tries, in order:
     * {@code textures/addon/mask/kitsune_ears_black.png}, then
     * {@code textures/addon/mask/kitsune_ears.png}, then
     * {@code textures/addon/mask/kitsune.png} - using whichever exists first.
     * Returns empty if none exist. Results are cached per texture.
     */
    public static Optional<ResourceLocation> resolveMask(ResourceLocation texture) {
        if (texture == null) {
            return Optional.empty();
        }
        return MASK_RESOLUTION_CACHE.computeIfAbsent(texture, TintTextureCompositor::discoverMask);
    }

    private static Optional<ResourceLocation> discoverMask(ResourceLocation texture) {
        var resourceManager = Minecraft.getInstance().getResourceManager();
        String path = texture.getPath();
        int lastSlash = path.lastIndexOf('/');
        String dir = lastSlash >= 0 ? path.substring(0, lastSlash) : "";
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        int dot = fileName.lastIndexOf('.');
        String candidateBase = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot >= 0 ? fileName.substring(dot) : "";

        while (true) {
            ResourceLocation candidate = new ResourceLocation(texture.getNamespace(),
                    dir + "/mask/" + candidateBase + ext);
            if (resourceManager.getResource(candidate).isPresent()) {
                return Optional.of(candidate);
            }
            int underscore = candidateBase.lastIndexOf('_');
            if (underscore < 0) {
                return Optional.empty();
            }
            candidateBase = candidateBase.substring(0, underscore);
        }
    }

    /**
     * Returns a {@link ResourceLocation} to bind for the given base texture with
     * the mask/tint applied. Falls back to {@code base} unchanged if no mask is
     * given, the tint is the default (white, i.e. a no-op), or compositing fails.
     */
    public static synchronized ResourceLocation getOrCreate(ResourceLocation base, ResourceLocation mask,
            int tintRGB) {
        if (base == null) {
            return base;
        }
        int tint = tintRGB & 0x00FFFFFF;
        if (mask == null || tint == WHITE) {
            return base;
        }

        String key = base + "|" + mask + "|" + Integer.toHexString(tint);
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        ResourceLocation composited = composite(base, mask, tint, key);
        if (composited == null) {
            return base;
        }
        CACHE.put(key, composited);
        return composited;
    }

    private static ResourceLocation composite(ResourceLocation base, ResourceLocation mask, int tint, String key) {
        var resourceManager = Minecraft.getInstance().getResourceManager();
        try (InputStream baseStream = resourceManager.open(base);
                InputStream maskStream = resourceManager.open(mask)) {
            try (NativeImage baseImg = NativeImage.read(baseStream); NativeImage maskImg = NativeImage.read(maskStream)) {
                if (baseImg.getWidth() != maskImg.getWidth() || baseImg.getHeight() != maskImg.getHeight()) {
                    warnOnce(key, "Or, what. Tint mask {} ({}x{}) doesn't match base texture {} ({}x{}), skipping mask",
                            mask, maskImg.getWidth(), maskImg.getHeight(), base, baseImg.getWidth(), baseImg.getHeight());
                    return null;
                }

                // NativeImage packs pixels as ABGR (bits 0-7 = R, 8-15 = G, 16-23 = B, 24-31 = A) -
                // NOT the same layout as Twilight Lib's own 0xRRGGBB tint-storage convention.
                // (Mask alpha lives in bits 24-31, read above as maskPixel >> 24.)
                float tr = ((tint >> 16) & 0xFF) / 255.0F;
                float tg = ((tint >> 8) & 0xFF) / 255.0F;
                float tb = (tint & 0xFF) / 255.0F;

                int width = baseImg.getWidth();
                int height = baseImg.getHeight();
                NativeImage result = new NativeImage(width, height, false);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int basePixel = baseImg.getPixelRGBA(x, y);
                        int maskPixel = maskImg.getPixelRGBA(x, y);
                        float strength = ((maskPixel >> 24) & 0xFF) / 255.0F; // mask alpha: opaque = tinted, transparent = untouched

                        int a = (basePixel >> 24) & 0xFF;
                        int b = (basePixel >> 16) & 0xFF;
                        int g = (basePixel >> 8) & 0xFF;
                        int r = basePixel & 0xFF;

                        int newR = lerpChannel(r, tr, strength);
                        int newG = lerpChannel(g, tg, strength);
                        int newB = lerpChannel(b, tb, strength);

                        result.setPixelRGBA(x, y, (a << 24) | (newB << 16) | (newG << 8) | newR);
                    }
                }

                DynamicTexture texture = new DynamicTexture(result);
                texture.upload();

                ResourceLocation dynamicLocation = new ResourceLocation(TwilightLib.MODID,
                        "dynamic/tint/" + hash(key));
                Minecraft.getInstance().getTextureManager().register(dynamicLocation, texture);
                return dynamicLocation;
            }
        } catch (IOException e) {
            warnOnce(key, "Or, what. Failed to composite tint texture (base={}, mask={}): {}", base, mask,
                    e.getMessage());
            return null;
        }
    }

    private static int lerpChannel(int channel, float tintChannel, float strength) {
        float tinted = channel * tintChannel;
        float value = channel + (tinted - channel) * strength;
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static String hash(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashed = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8 && i < hashed.length; i++) {
                sb.append(String.format("%02x", hashed[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(key.hashCode());
        }
    }

    private static void warnOnce(String key, String format, Object... args) {
        if (WARNED_KEYS.add(key)) {
            LOGGER.warn(format, args);
        }
    }

    /**
     * Releases every cached composite. Currently called on disconnect and level
     * unload (see {@code MorphRenderHandler}). Not wired to resource-pack reload -
     * a mask/texture swapped via {@code /reload} won't be picked up until the
     * next disconnect/level unload; call this manually if that's ever needed.
     */
    public static synchronized void clearAll() {
        var textureManager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation rl : CACHE.values()) {
            textureManager.release(rl);
        }
        CACHE.clear();
        WARNED_KEYS.clear();
        MASK_RESOLUTION_CACHE.clear();
    }
}
