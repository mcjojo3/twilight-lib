package mc.sayda.twilight_lib;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES,
            TwilightLib.MODID);

    /**
     * Mining Penalty attribute controls mining speed penalties when not on solid
     * ground.
     * Vanilla applies 5x slowdown when not on ground, and another 5x when
     * underwater (25x total!)
     *
     * 0 = no slowdown (removes both flight break AND underwater mining penalties)
     * 1 = normal slowdown (default vanilla behavior)
     *
     * Note: This overrides BOTH the flight break penalty and underwater mining
     * penalty,
     * even without Aqua Affinity enchantment.
     */
    public static final RegistryObject<Attribute> MINING_PENALTY = ATTRIBUTES.register("mining_penalty",
            () -> new RangedAttribute("attribute.name.twilight_lib.mining_penalty", 1.0, 0.0, 1.0)
                    .setSyncable(true));

    /**
     * FOV Modifier attribute controls whether FOV (Field of View) effects are
     * applied.
     * This includes speed-based FOV changes (sprint FOV, slowness FOV, etc.)
     *
     * 0 = disable all FOV effects (keeps FOV constant)
     * 1 = normal FOV effects (default vanilla behavior)
     *
     * Note: This affects all dynamic FOV changes including sprint, slow falling,
     * speed effects, etc.
     */
    public static final RegistryObject<Attribute> FOV_MODIFIER = ATTRIBUTES.register("fov_modifier",
            () -> new RangedAttribute("attribute.name.twilight_lib.fov_modifier", 1.0, 0.0, 1.0)
                    .setSyncable(true));

    /**
     * Allow Helmet attribute controls whether player can equip helmet armor.
     *
     * 0 = cannot equip helmets
     * 1 = can equip helmets (default vanilla behavior)
     *
     * Note: Used by addons to restrict armor when cosmetics would conflict.
     */
    public static final RegistryObject<Attribute> ALLOW_HELMET = ATTRIBUTES.register("allow_helmet",
            () -> new RangedAttribute("attribute.name.twilight_lib.allow_helmet", 1.0, 0.0, 1.0)
                    .setSyncable(true));

    /**
     * Allow Chestplate attribute controls whether player can equip chestplate
     * armor.
     *
     * 0 = cannot equip chestplates
     * 1 = can equip chestplates (default vanilla behavior)
     *
     * Note: Used by addons to restrict armor when cosmetics would conflict.
     */
    public static final RegistryObject<Attribute> ALLOW_CHESTPLATE = ATTRIBUTES.register("allow_chestplate",
            () -> new RangedAttribute("attribute.name.twilight_lib.allow_chestplate", 1.0, 0.0, 1.0)
                    .setSyncable(true));

    /**
     * Allow Leggings attribute controls whether player can equip leggings armor.
     *
     * 0 = cannot equip leggings
     * 1 = can equip leggings (default vanilla behavior)
     *
     * Note: Used by addons to restrict armor when cosmetics would conflict.
     */
    public static final RegistryObject<Attribute> ALLOW_LEGGINGS = ATTRIBUTES.register("allow_leggings",
            () -> new RangedAttribute("attribute.name.twilight_lib.allow_leggings", 1.0, 0.0, 1.0)
                    .setSyncable(true));

    /**
     * Allow Boots attribute controls whether player can equip boots armor.
     *
     * 0 = cannot equip boots
     * 1 = can equip boots (default vanilla behavior)
     *
     * Note: Used by addons to restrict armor when cosmetics would conflict.
     */
    public static final RegistryObject<Attribute> ALLOW_BOOTS = ATTRIBUTES.register("allow_boots",
            () -> new RangedAttribute("attribute.name.twilight_lib.allow_boots", 1.0, 0.0, 1.0)
                    .setSyncable(true));

    /**
     * Elytra Flight attribute controls whether player can fly like an elytra
     * without wearing one.
     *
     * 0 = disabled (default)
     * 1 = enabled
     */
    public static final RegistryObject<Attribute> ELYTRA_FLIGHT = ATTRIBUTES.register("elytra_flight",
            () -> new RangedAttribute("attribute.name.twilight_lib.elytra_flight", 0.0, 0.0, 1024.0)
                    .setSyncable(true));

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
    }
}