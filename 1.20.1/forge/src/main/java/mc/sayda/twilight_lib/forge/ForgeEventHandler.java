package mc.sayda.twilight_lib.forge;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightLib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEventHandler {

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player) {
            Float eyeHeight = mc.sayda.twilight_lib.TwilightEventHandler.getMorphEyeHeight(player, event.getPose());
            if (eyeHeight != null) {
                event.setNewEyeHeight(eyeHeight);
            }
        }
    }

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

            // Architectury's event also fires, but this native one ensures Forge capsules
            // are ready
            mc.sayda.twilight_lib.capabilities.DataUtils.getMorphData(newPlayer).deserialize(
                    mc.sayda.twilight_lib.capabilities.DataUtils.getMorphData(oldPlayer).serialize());
            mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(newPlayer).deserialize(
                    mc.sayda.twilight_lib.capabilities.DataUtils.getAddonsData(oldPlayer).serialize());
            mc.sayda.twilight_lib.capabilities.DataUtils.getTrailsData(newPlayer).deserialize(
                    mc.sayda.twilight_lib.capabilities.DataUtils.getTrailsData(oldPlayer).serialize());
            mc.sayda.twilight_lib.capabilities.DataUtils.getEffectsData(newPlayer).deserialize(
                    mc.sayda.twilight_lib.capabilities.DataUtils.getEffectsData(oldPlayer).serialize());
            mc.sayda.twilight_lib.capabilities.DataUtils.getModelVariantData(newPlayer).deserialize(
                    mc.sayda.twilight_lib.capabilities.DataUtils.getModelVariantData(oldPlayer).serialize());

            TwilightLib.LOGGER.debug("Twilight Lib: (Forge) Cloned persistent data and capabilities for {}",
                    newPlayer.getGameProfile().getName());
        }
    }
}
