package mc.sayda.twilight_lib.addon;

import java.util.Set;

/**
 * Server-safe metadata for an addon.
 * Contains only logical information required by the server and common code.
 */
public record LogicalAddon(
        String id,
        Set<String> modTags,
        Set<BodyPart> hiddenBodyParts) {
}
