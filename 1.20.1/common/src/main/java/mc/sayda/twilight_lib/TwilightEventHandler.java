package mc.sayda.twilight_lib;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class TwilightEventHandler {

    public static void init() {
    }

    public static boolean onTryToStartFallFlying(Player player) {
        Attribute attrObj = ModAttributes.ELYTRA_FLIGHT.get();
        if (attrObj == null)
            return false;
        var attr = player.getAttribute(attrObj);
        if (attr != null && attr.getValue() > 0) {
            boolean canFly = !player.onGround()
                    && !player.isInWater()
                    && !player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
            if (canFly) {
                player.startFallFlying();
                return true;
            }
        }
        return false;
    }
}
