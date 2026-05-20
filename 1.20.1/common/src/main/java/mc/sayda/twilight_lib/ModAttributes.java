package mc.sayda.twilight_lib;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import dev.architectury.registry.registries.Registrar;

/**
 * Central registry for mod-added attributes.
 * 
 * TODO: Implement a fix for dispensers bypassing armor equipping restrictions.
 * Dispensers currently use ArmorItem.dispenseArmor which bypasses standard
 * inventory hooks.
 */
public class ModAttributes {
        public static final Registrar<Attribute> ATTRIBUTES = dev.architectury.registry.registries.RegistrarManager
                        .get(TwilightLib.MODID).get(net.minecraft.core.registries.Registries.ATTRIBUTE);

        public static final dev.architectury.registry.registries.RegistrySupplier<Attribute> MINING_PENALTY = ATTRIBUTES
                        .register(new net.minecraft.resources.ResourceLocation(TwilightLib.MODID,
                                        "mining_penalty"),
                                        () -> new RangedAttribute("attribute.name.twilight_lib.mining_penalty", 1.0,
                                                        0.0, 1.0)
                                                        .setSyncable(true));

        public static final dev.architectury.registry.registries.RegistrySupplier<Attribute> FOV_MODIFIER = ATTRIBUTES
                        .register(new net.minecraft.resources.ResourceLocation(TwilightLib.MODID,
                                        "fov_modifier"),
                                        () -> new RangedAttribute("attribute.name.twilight_lib.fov_modifier", 1.0, 0.0,
                                                        1.0)
                                                        .setSyncable(true));

        public static final dev.architectury.registry.registries.RegistrySupplier<Attribute> ALLOW_HELMET = ATTRIBUTES
                        .register(new net.minecraft.resources.ResourceLocation(TwilightLib.MODID,
                                        "allow_helmet"),
                                        () -> new RangedAttribute("attribute.name.twilight_lib.allow_helmet", 1.0, 0.0,
                                                        1.0)
                                                        .setSyncable(true));

        public static final dev.architectury.registry.registries.RegistrySupplier<Attribute> ALLOW_CHESTPLATE = ATTRIBUTES
                        .register(new net.minecraft.resources.ResourceLocation(TwilightLib.MODID,
                                        "allow_chestplate"),
                                        () -> new RangedAttribute("attribute.name.twilight_lib.allow_chestplate", 1.0,
                                                        0.0, 1.0)
                                                        .setSyncable(true));

        public static final dev.architectury.registry.registries.RegistrySupplier<Attribute> ALLOW_LEGGINGS = ATTRIBUTES
                        .register(new net.minecraft.resources.ResourceLocation(TwilightLib.MODID,
                                        "allow_leggings"),
                                        () -> new RangedAttribute("attribute.name.twilight_lib.allow_leggings", 1.0,
                                                        0.0, 1.0)
                                                        .setSyncable(true));

        public static final dev.architectury.registry.registries.RegistrySupplier<Attribute> ALLOW_BOOTS = ATTRIBUTES
                        .register(new net.minecraft.resources.ResourceLocation(TwilightLib.MODID,
                                        "allow_boots"),
                                        () -> new RangedAttribute("attribute.name.twilight_lib.allow_boots", 1.0, 0.0,
                                                        1.0)
                                                        .setSyncable(true));

        public static final dev.architectury.registry.registries.RegistrySupplier<Attribute> ELYTRA_FLIGHT = ATTRIBUTES
                        .register(new net.minecraft.resources.ResourceLocation(TwilightLib.MODID,
                                        "elytra_flight"),
                                        () -> new RangedAttribute("attribute.name.twilight_lib.elytra_flight", 0.0, 0.0,
                                                        1024.0)
                                                        .setSyncable(true));

        public static @org.jetbrains.annotations.NotNull net.minecraft.core.Holder<Attribute> getHolder(
                        dev.architectury.registry.registries.RegistrySupplier<Attribute> supplier) {
                return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(supplier.get());
        }

        public static void register() {
                // Trigger class loading and log registration
                com.mojang.logging.LogUtils.getLogger().info("Yes! This'll be fun! Registering Twilight Attributes...");
                ATTRIBUTES.getClass(); // Force static init
        }
}
