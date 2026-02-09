package mc.sayda.twilight_lib.api.addon;

import net.minecraft.resources.ResourceLocation;

/**
 * Defines the type of an addon (e.g., WINGS, TAIL).
 * Used for grouping and rendering logic.
 */
public record AddonType(ResourceLocation id, String displayName) {
    public static final AddonType WINGS = new AddonType(ResourceLocation.fromNamespaceAndPath("twilight_lib", "wings"),
            "Wings");
    public static final AddonType TAIL = new AddonType(ResourceLocation.fromNamespaceAndPath("twilight_lib", "tail"),
            "Tail");
    public static final AddonType HORN = new AddonType(ResourceLocation.fromNamespaceAndPath("twilight_lib", "horn"),
            "Horn");
    public static final AddonType OTHER = new AddonType(ResourceLocation.fromNamespaceAndPath("twilight_lib", "other"),
            "Other");
}
