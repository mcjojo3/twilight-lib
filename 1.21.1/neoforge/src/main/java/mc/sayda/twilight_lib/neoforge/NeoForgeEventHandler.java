package mc.sayda.twilight_lib.neoforge;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = TwilightLib.MODID, bus = EventBusSubscriber.Bus.GAME)
public class NeoForgeEventHandler {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
            TwilightLib.onStartTracking(tracker, target);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TwilightLib.onPlayerRespawn(player);
        }
    }

}
