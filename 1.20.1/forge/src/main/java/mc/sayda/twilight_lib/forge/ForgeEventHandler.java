package mc.sayda.twilight_lib.forge;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightLib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventHandler {

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

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer
                && event.getOriginal() instanceof ServerPlayer oldPlayer) {
            // Forge-specific persistence workaround from .legacy
            // Copy the "Forge Capsule" (getPersistentData) as it's not always preserved
            // during dim change
            CompoundTag oldForgeData = oldPlayer.getPersistentData();
            if (oldForgeData.contains("TwilightPersistentData", 10)) {
                newPlayer.getPersistentData().put("TwilightPersistentData",
                        oldForgeData.getCompound("TwilightPersistentData").copy());
            }

            TwilightLib.LOGGER.debug("Twilight Lib: (Forge) Cloned persistent data for {}",
                    newPlayer.getGameProfile().getName());
        }
    }
}
