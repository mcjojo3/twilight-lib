package mc.sayda.twilight_lib.addon;

import mc.sayda.twilight_lib.TwilightConstants;
import java.util.Set;

/**
 * Handles logical registration of addons for both server and client.
 * Does not contain any client-specific code or imports.
 */
public class AddonLogicalInit {
    private static boolean initialized = false;

    public static void init() {
        if (initialized)
            return;
        initialized = true;

        // Register built-in addons logical metadata
        register("tiara", Set.of());
        register("succubus_wings", Set.of());
        register("beanie", Set.of());

        // Nymph models
        register("dryad_body", Set.of("creraces"));
        register("oread_body", Set.of("creraces"));
        register("naiad_body", Set.of("creraces"));
        register("aurai_body", Set.of("creraces"));
        register("moss", Set.of());
        register("slime_body", Set.of("creraces"));

        // Hats
        register("golden_laurel", Set.of());
        register("moonlit_tiara", Set.of());
        register("teemo_hat", Set.of());

        // Body replacements
        register("chest", Set.of());
        register("short_torso", Set.of(), Set.of(BodyPart.BODY, BodyPart.JACKET));
        register("opaque_body", Set.of());

        // Wings
        for (int i = 1; i <= 4; i++) {
            register("wings_" + i, Set.of("creraces"));
        }
        register("wings_pixie", Set.of("creraces"));
        register("wings_flandre", Set.of());

        // Harpy parts
        Set<BodyPart> legParts = Set.of(BodyPart.RIGHT_LEG, BodyPart.LEFT_LEG, BodyPart.LEFT_PANTS,
                BodyPart.RIGHT_PANTS);
        register("harpy_legs", Set.of("creraces"), legParts);
        register("harpy_thighs", Set.of("creraces"));
        register("harpy_wings", Set.of("creraces"));
        register("harpy_legs_alt", Set.of("creraces"), legParts);
        register("harpy_thighs_alt", Set.of("creraces"));

        // Kitsune variants
        registerKitsune();
    }

    private static void register(String id, Set<String> modTags) {
        AddonRegistry.registerLogical(id, modTags, Set.of());
    }

    private static void register(String id, Set<String> modTags, Set<BodyPart> hiddenParts) {
        AddonRegistry.registerLogical(id, modTags, hiddenParts);
    }

    private static void registerKitsune() {
        String[] colors = TwilightConstants.Addon.KITSUNE_COLORS;
        Set<String> kitsuneMods = Set.of("creraces");

        for (String color : colors) {
            register("kitsune_ears_" + color, kitsuneMods);
            register("kitsune_ears_" + color + "_alt", kitsuneMods);
            register("kitsune_snout_" + color, kitsuneMods);

            for (int i = 1; i <= 9; i++) {
                register("kitsune_tails_" + i + "_" + color, kitsuneMods);
                if (i >= 3 && i <= 5) {
                    register("kitsune_tails_" + i + "_" + color + "_alt", kitsuneMods);
                }
            }
        }
    }
}
