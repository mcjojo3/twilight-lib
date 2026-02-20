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

            // Architectury's event also fires, but this native one ensures Forge capsules
            // are ready
            mc.sayda.twilight_lib.capabilities.IMorph oldMorph = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getMorphData(oldPlayer);
            mc.sayda.twilight_lib.capabilities.IMorph newMorph = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getMorphData(newPlayer);
            if (oldMorph != null && newMorph != null) {
                newMorph.deserialize(oldMorph.serialize());
            }

            mc.sayda.twilight_lib.capabilities.IAddons oldAddons = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getAddonsData(oldPlayer);
            mc.sayda.twilight_lib.capabilities.IAddons newAddons = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getAddonsData(newPlayer);
            if (oldAddons != null && newAddons != null) {
                newAddons.deserialize(oldAddons.serialize());
            }

            mc.sayda.twilight_lib.capabilities.ITrails oldTrails = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getTrailsData(oldPlayer);
            mc.sayda.twilight_lib.capabilities.ITrails newTrails = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getTrailsData(newPlayer);
            if (oldTrails != null && newTrails != null) {
                newTrails.deserialize(oldTrails.serialize());
            }

            mc.sayda.twilight_lib.capabilities.IEffects oldEffects = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getEffectsData(oldPlayer);
            mc.sayda.twilight_lib.capabilities.IEffects newEffects = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getEffectsData(newPlayer);
            if (oldEffects != null && newEffects != null) {
                newEffects.deserialize(oldEffects.serialize());
            }

            mc.sayda.twilight_lib.capabilities.IModelVariant oldMV = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getModelVariantData(oldPlayer);
            mc.sayda.twilight_lib.capabilities.IModelVariant newMV = mc.sayda.twilight_lib.capabilities.DataUtils
                    .getModelVariantData(newPlayer);
            if (oldMV != null && newMV != null) {
                newMV.deserialize(oldMV.serialize());
            }

            TwilightLib.LOGGER.debug("Twilight Lib: (Forge) Cloned persistent data and capabilities for {}",
                    newPlayer.getGameProfile().getName());
        }
    }
}
