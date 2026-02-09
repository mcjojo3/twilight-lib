package mc.sayda.twilight_lib.addon;

import java.util.Set;

/**
 * Server-safe representation of an addon containing only logical metadata.
 * This record is safe to use in common code without triggering client-only
 * class loading.
 * 
 * @param id              The unique identifier for the addon.
 * @param modTags         Set of mod IDs required for this addon (empty = always
 *                        load).
 * @param hiddenBodyParts Set of specific body parts to hide when this addon is
 *                        equipped.
 */
public record LogicalAddon(String id, Set<String> modTags, Set<BodyPart> hiddenBodyParts) {
}
