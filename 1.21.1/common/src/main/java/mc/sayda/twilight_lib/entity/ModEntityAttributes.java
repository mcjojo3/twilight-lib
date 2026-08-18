package mc.sayda.twilight_lib.entity;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.world.entity.animal.Fox;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// In 1.21.1, bus parameter is deprecated - MOD bus is implied
import dev.architectury.registry.level.entity.EntityAttributeRegistry;

public class ModEntityAttributes {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register() {
        LOGGER.info("What's your name? Registering attributes for custom fox entities...");

        List.of(
                ModEntities.WHITE_FOX,
                ModEntities.BLACK_FOX,
                ModEntities.BLUE_FOX,
                ModEntities.YELLOW_FOX,
                ModEntities.ORANGE_FOX,
                ModEntities.PURPLE_FOX,
                ModEntities.RED_FOX,
                ModEntities.GRAY_FOX
                )
                .forEach(fox -> EntityAttributeRegistry.register(fox, Fox::createAttributes));

        LOGGER.info("I like all these things around me! Registered attributes for the custom fox entities.");
    }
}
