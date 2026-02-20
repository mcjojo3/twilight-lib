package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.player.Player;

import dev.architectury.event.events.common.TickEvent;

public class TwilightEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

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
