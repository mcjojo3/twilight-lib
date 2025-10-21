package mc.sayda.twilight_lib.addon;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.client.model.addon.KitsuneEarsModel;
import mc.sayda.twilight_lib.client.model.addon.KitsuneSnoutModel;
import mc.sayda.twilight_lib.client.model.addon.KitsuneTailsVariantModel;
import mc.sayda.twilight_lib.client.model.addon.TiaraModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;

/**
 * Initialize and register all built-in addons.
 * Call this during client setup.
 */
public class AddonInit {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void registerAddons() {
        LOGGER.info("I should come here every millennium! Registering built-in addons...");

        AddonRegistry.registerAddon(
                "tiara",                    // ID used in commands
                TiaraModel.LAYER_LOCATION,      // Your model's LAYER_LOCATION
                TiaraModel::createBodyLayer,    // Your model's createBodyLayer method
                TiaraModel::new,
                new ResourceLocation(TwilightLib.MODID, "textures/addon/tiara.png")
        );

        // Register kitsune addons - multiple color variants
        registerKitsuneVariants();

        LOGGER.info("I like all these things around me! {} addons registered.", AddonRegistry.getAllAddonIds().size());
    }

    /**
     * Register all color variants of kitsune parts (ears, snout, and tail variants)
     */
    private static void registerKitsuneVariants() {
        String[] colors = {"white", "black", "blue", "yellow", "orange", "purple", "red"};

        // Define which tails are visible for each variant
        // Standard variants (swapped with alt for 3-5):
        // 1: 1
        // 2: 2, 3
        // 3: 1, 2, 3 (was alt, now standard)
        // 4: 2, 3, 4, 7 (was alt, now standard)
        // 5: 1, 2, 3, 4, 7 (was alt, now standard)
        // 6: 2, 3, 7, 4, 8, 5
        // 7: 1, 2, 3, 4, 5, 7, 8
        // 8: 2, 3, 4, 5, 6, 7, 8, 9
        // 9: 1, 2, 3, 4, 5, 6, 7, 8, 9
        Map<Integer, Set<Integer>> tailVariants = Map.of(
            1, Set.of(1),
            2, Set.of(2, 3),
            3, Set.of(1, 2, 3),
            4, Set.of(2, 3, 4, 7),
            5, Set.of(1, 2, 3, 4, 7),
            6, Set.of(2, 3, 4, 5, 7, 8),
            7, Set.of(1, 2, 3, 4, 5, 7, 8),
            8, Set.of(2, 3, 4, 5, 6, 7, 8, 9),
            9, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9)
        );

        // Alt variants (swapped with standard for 3-5):
        // 1: 1
        // 2: 2, 3
        // 3: 1, 4, 7 (was standard, now alt)
        // 4: 2, 3, 5, 8 (was standard, now alt)
        // 5: 1, 7, 4, 9, 6 (was standard, now alt)
        // 6: 2, 3, 4, 7, 5, 8
        // 7: 1, 2, 3, 4, 7, 5, 8
        // 8: 2, 3, 4, 7, 5, 8, 6, 9
        // 9: 1, 2, 3, 4, 7, 5, 8, 6, 9
        Map<Integer, Set<Integer>> tailVariantsAlt = Map.of(
            1, Set.of(1),
            2, Set.of(2, 3),
            3, Set.of(1, 4, 7),
            4, Set.of(2, 3, 5, 8),
            5, Set.of(1, 4, 6, 7, 9),
            6, Set.of(2, 3, 4, 5, 7, 8),
            7, Set.of(1, 2, 3, 4, 5, 7, 8),
            8, Set.of(2, 3, 4, 5, 6, 7, 8, 9),
            9, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9)
        );

        Map<Integer, ModelLayerLocation> layerLocations = Map.of(
            1, KitsuneTailsVariantModel.LAYER_1_TAIL,
            2, KitsuneTailsVariantModel.LAYER_2_TAIL,
            3, KitsuneTailsVariantModel.LAYER_3_TAIL,
            4, KitsuneTailsVariantModel.LAYER_4_TAIL,
            5, KitsuneTailsVariantModel.LAYER_5_TAIL,
            6, KitsuneTailsVariantModel.LAYER_6_TAIL,
            7, KitsuneTailsVariantModel.LAYER_7_TAIL,
            8, KitsuneTailsVariantModel.LAYER_8_TAIL,
            9, KitsuneTailsVariantModel.LAYER_9_TAIL
        );

        int totalAddons = 0;

        for (String color : colors) {
            // Register ears (standard variant)
            AddonRegistry.registerAddon(
                "kitsune_ears_" + color,
                KitsuneEarsModel.LAYER_LOCATION,
                KitsuneEarsModel::createBodyLayer,
                KitsuneEarsModel::new,
                new ResourceLocation(TwilightLib.MODID, "textures/addon/kitsune_ears_" + color + ".png")
            );
            totalAddons++;

            // Register ears (alt variant - same texture)
            AddonRegistry.registerAddon(
                "kitsune_ears_" + color + "_alt",
                KitsuneEarsModel.LAYER_LOCATION_ALT,
                KitsuneEarsModel::createBodyLayerAlt,
                KitsuneEarsModel::new,
                new ResourceLocation(TwilightLib.MODID, "textures/addon/kitsune_ears_" + color + ".png")
            );
            totalAddons++;

            // Register snout
            AddonRegistry.registerAddon(
                "kitsune_snout_" + color,
                KitsuneSnoutModel.LAYER_LOCATION,
                KitsuneSnoutModel::createBodyLayer,
                KitsuneSnoutModel::new,
                new ResourceLocation(TwilightLib.MODID, "textures/addon/kitsune_snout_" + color + ".png")
            );
            totalAddons++;

            // Register tail variants (1-9 tails, including full 9-tail as the standard)
            for (int tailCount = 1; tailCount <= 9; tailCount++) {
                final int count = tailCount;
                Set<Integer> visibleTails = tailVariants.get(count);
                Set<Integer> visibleTailsAlt = tailVariantsAlt.get(count);
                ModelLayerLocation layerLocation = layerLocations.get(count);

                // Register standard variant
                AddonRegistry.registerAddon(
                    "kitsune_tails_" + count + "_" + color,
                    layerLocation,
                    KitsuneTailsVariantModel::createBodyLayer,
                    root -> new KitsuneTailsVariantModel<>(root, visibleTails),
                    new ResourceLocation(TwilightLib.MODID, "textures/addon/kitsune_tail_" + color + ".png")
                );
                totalAddons++;

                // Register alt variant only if it's different from standard
                // Alt variants that differ: 3, 4, 5
                if (count >= 3 && count <= 5) {
                    AddonRegistry.registerAddon(
                        "kitsune_tails_" + count + "_" + color + "_alt",
                        layerLocation,
                        KitsuneTailsVariantModel::createBodyLayer,
                        root -> new KitsuneTailsVariantModel<>(root, visibleTailsAlt),
                        new ResourceLocation(TwilightLib.MODID, "textures/addon/kitsune_tail_" + color + ".png")
                    );
                    totalAddons++;
                }
            }
        }

        LOGGER.info("There are so many weirdos here... It's awesome! Registered {} kitsune variants (ears + snout + tails + tail variants + alt variants).", totalAddons);
    }
}