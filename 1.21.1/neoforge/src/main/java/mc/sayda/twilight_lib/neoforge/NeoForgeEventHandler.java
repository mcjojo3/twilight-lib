package mc.sayda.twilight_lib.neoforge;

import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import mc.sayda.twilight_lib.entity.ModEntities;
import net.minecraft.world.entity.animal.Fox;

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

    @SubscribeEvent
    public static void onEntitySize(net.neoforged.neoforge.event.entity.EntityEvent.Size evt) {
        if (evt.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            evt.setNewSize(mc.sayda.twilight_lib.TwilightEventHandler.getMorphDimensions(player, evt.getPose(),
                    evt.getNewSize()));
        }
    }
}

@EventBusSubscriber(modid = TwilightLib.MODID, bus = EventBusSubscriber.Bus.MOD)
class NeoForgeModEventHandler {
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, getHolder(ModAttributes.MINING_PENALTY.get()));
        event.add(EntityType.PLAYER, getHolder(ModAttributes.FOV_MODIFIER.get()));
        event.add(EntityType.PLAYER, getHolder(ModAttributes.ALLOW_HELMET.get()));
        event.add(EntityType.PLAYER, getHolder(ModAttributes.ALLOW_CHESTPLATE.get()));
        event.add(EntityType.PLAYER, getHolder(ModAttributes.ALLOW_LEGGINGS.get()));
        event.add(EntityType.PLAYER, getHolder(ModAttributes.ALLOW_BOOTS.get()));
        event.add(EntityType.PLAYER, getHolder(ModAttributes.ELYTRA_FLIGHT.get()));
    }

    private static Holder<Attribute> getHolder(Attribute attribute) {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }
}
