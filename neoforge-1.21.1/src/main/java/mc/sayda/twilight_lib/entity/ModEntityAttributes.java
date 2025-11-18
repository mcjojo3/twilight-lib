package mc.sayda.twilight_lib.entity;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.world.entity.animal.Fox;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

// In 1.21.1, bus parameter is deprecated - MOD bus is implied
@EventBusSubscriber(modid = TwilightLib.MODID)
public class ModEntityAttributes {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // DeferredHolder objects are always present after registration
        // No null check needed - these are registered during mod construction
        List.of(
            ModEntities.WHITE_FOX,
            ModEntities.BLACK_FOX,
            ModEntities.BLUE_FOX,
            ModEntities.YELLOW_FOX,
            ModEntities.ORANGE_FOX,
            ModEntities.PURPLE_FOX,
            ModEntities.RED_FOX
        ).forEach(fox -> event.put(fox.get(), Fox.createAttributes().build()));
    }
}