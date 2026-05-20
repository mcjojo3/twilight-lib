package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.player.Player;

public class TwilightEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
    }

    public static boolean onTryToStartFallFlying(Player player) {
        var elytraAttr = ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT);
        if (player.getAttributes().hasAttribute(elytraAttr)) {
            var attr = player.getAttribute(elytraAttr);
            if (attr != null && attr.getValue() > 0) {
                boolean canFly = !player.onGround()
                        && !player.isInWater()
                        && !player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
                if (canFly) {
                    player.startFallFlying();
                    return true;
                }
            }
        }
        return false;
    }
}
