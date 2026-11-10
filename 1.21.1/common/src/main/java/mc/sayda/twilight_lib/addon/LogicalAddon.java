package mc.sayda.twilight_lib.addon;

import java.util.Set;

/** Server-safe addon info: id, required mods and hidden parts. */
public record LogicalAddon(String id, Set<String> modTags, Set<BodyPart> hiddenBodyParts) {
}
