package mc.sayda.twilight_lib.entity;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.world.entity.animal.Fox;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// In 1.21.1, bus parameter is deprecated - MOD bus is implied
@EventBusSubscriber(modid = TwilightLib.MODID)
public class ModEntityAttributes {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        LOGGER.info("What's your name? Registering attributes for custom fox entities...");

        // DeferredHolder objects are always present after registration
        // No null check needed - these are registered during mod construction
        AtomicInteger registered = new AtomicInteger(0);
        List.of(
            ModEntities.WHITE_FOX,
            ModEntities.BLACK_FOX,
            ModEntities.BLUE_FOX,
            ModEntities.YELLOW_FOX,
            ModEntities.ORANGE_FOX,
            ModEntities.PURPLE_FOX,
            ModEntities.RED_FOX
        ).forEach(fox -> {
            event.put(fox.get(), Fox.createAttributes().build());
            registered.incrementAndGet();
        });

        LOGGER.info("I like all these things around me! Registered attributes for {} custom fox entities.", registered.get());
    }
}