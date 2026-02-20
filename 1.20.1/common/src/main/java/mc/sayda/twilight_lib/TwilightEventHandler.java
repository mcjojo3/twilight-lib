package mc.sayda.twilight_lib;

import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;

import dev.architectury.event.events.common.TickEvent;

public class TwilightEventHandler {

    public static void init() {
        TickEvent.PLAYER_POST.register(TwilightEventHandler::onPlayerTick);
    }

    private static void onPlayerTick(Player player) {
        if (player.level().isClientSide)
            return;
        if (!player.isAlive())
            return;

        IMorph morph = DataUtils.getMorphData(player);
        if (morph == null)
            return;
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
