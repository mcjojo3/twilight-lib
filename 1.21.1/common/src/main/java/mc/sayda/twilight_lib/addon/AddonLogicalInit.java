package mc.sayda.twilight_lib.addon;

import java.util.Set;

/**
 * Handles the registration of addon metadata (logical registration) that is
 * safe for both
 * client and server. This ensures that the server is aware of all valid addon
 * IDs
 * without requiring client-only model classes to be loaded.
 */
public class AddonLogicalInit {
    private static boolean initialized = false;

    public static void init() {
        if (initialized)
            return;
        initialized = true;

        // Register built-in addons logical metadata
        // Mod requirements (modTags) and hidden body parts should be defined here
        // as the "Single Source of Truth".

        register("tiara", Set.of());
        register("succubus_wings", Set.of());
        register("beanie", Set.of());

        // Nymph models
        register("dryad_body", Set.of("creraces"));
        register("oread_body", Set.of("creraces"));
        register("naiad_body", Set.of("creraces"));
        register("aurai_body", Set.of("creraces"));
        register("moss", Set.of());

        register("golden_laurel", Set.of());
        register("moonlit_tiara", Set.of());
        register("teemo_hat", Set.of());
        register("chest", Set.of());

        // Wings
        for (int i = 1; i <= 4; i++) {
            register("wings_" + i, Set.of("creraces"));
        }
        register("wings_pixie", Set.of("creraces"));
        register("wings_flandre", Set.of());

        // Harpy set
        Set<BodyPart> harpyLegsHidden = Set.of(BodyPart.RIGHT_LEG, BodyPart.LEFT_LEG, BodyPart.LEFT_PANTS,
                BodyPart.RIGHT_PANTS);
        register("harpy_legs", Set.of("creraces"), harpyLegsHidden);
        register("harpy_thighs", Set.of("creraces"));
        register("harpy_wings", Set.of("creraces"));
        register("harpy_legs_alt", Set.of("creraces"), harpyLegsHidden);
        register("harpy_thighs_alt", Set.of("creraces"));

        register("short_torso", Set.of(), Set.of(BodyPart.BODY, BodyPart.JACKET));
        register("opaque_body", Set.of());
        register("slime_body", Set.of("creraces"));

        // Kitsune set registration
        registerKitsuneLogical();
    }

    private static void registerKitsuneLogical() {
        String[] colors = mc.sayda.twilight_lib.TwilightConstants.Addon.KITSUNE_COLORS;
        Set<String> kitsuneModTags = Set.of("creraces");

        for (String color : colors) {
            register("kitsune_ears_" + color, kitsuneModTags);
            register("kitsune_ears_" + color + "_alt", kitsuneModTags);
            register("kitsune_snout_" + color, kitsuneModTags);

            for (int i = 1; i <= 9; i++) {
                register("kitsune_tails_" + i + "_" + color, kitsuneModTags);
                if (i >= 3 && i <= 5) {
                    register("kitsune_tails_" + i + "_" + color + "_alt", kitsuneModTags);
                }
            }
        }
    }

    private static void register(String id, Set<String> modTags) {
        AddonRegistry.registerLogical(id, modTags, Set.of());
    }

    private static void register(String id, Set<String> modTags, Set<BodyPart> hiddenBodyParts) {
        AddonRegistry.registerLogical(id, modTags, hiddenBodyParts);
    }
}
