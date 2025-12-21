package mc.sayda.twilight_lib.entity;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.world.entity.animal.Fox;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = TwilightLib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        LOGGER.info("What's your name? Registering attributes for custom fox entities...");

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
            if (fox != null && fox.isPresent()) {
                event.put(fox.get(), Fox.createAttributes().build());
                registered.incrementAndGet();
            } else {
                LOGGER.error("Or, what. Fox entity not present during attribute registration!");
            }
        });

        LOGGER.info("I like all these things around me! Registered attributes for {} custom fox entities.", registered.get());
    }
}