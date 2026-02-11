package mc.sayda.twilight_lib.neoforge;

import mc.sayda.twilight_lib.TwilightLib;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(mc.sayda.twilight_lib.TwilightLib.MODID)
public class TwilightLibNeoForge {
    public TwilightLibNeoForge(IEventBus modBus) {
        if (modBus == null) {
            throw new IllegalArgumentException("Mod event bus cannot be null");
        }
        mc.sayda.twilight_lib.neoforge.capabilities.ModAttachments.ATTACHMENT_TYPES.register(modBus);
        mc.sayda.twilight_lib.neoforge.config.TwilightLibConfigNeoForge.registerConfig();

        TwilightLib.init();

        modBus.addListener(this::onAttributeModification);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            // We'll call a dedicated early init for common client stuff that doesn't
            // involve late-stage registries (like events/caches)
            mc.sayda.twilight_lib.client.TwilightLibClient.init();
        }
    }

    private void onAttributeModification(
            final net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent event) {
        event.add(net.minecraft.world.entity.EntityType.PLAYER,
                mc.sayda.twilight_lib.ModAttributes.getHolder(mc.sayda.twilight_lib.ModAttributes.MINING_PENALTY));
        event.add(net.minecraft.world.entity.EntityType.PLAYER,
                mc.sayda.twilight_lib.ModAttributes.getHolder(mc.sayda.twilight_lib.ModAttributes.FOV_MODIFIER));
        event.add(net.minecraft.world.entity.EntityType.PLAYER,
                mc.sayda.twilight_lib.ModAttributes.getHolder(mc.sayda.twilight_lib.ModAttributes.ALLOW_HELMET));
        event.add(net.minecraft.world.entity.EntityType.PLAYER,
                mc.sayda.twilight_lib.ModAttributes.getHolder(mc.sayda.twilight_lib.ModAttributes.ALLOW_CHESTPLATE));
        event.add(net.minecraft.world.entity.EntityType.PLAYER,
                mc.sayda.twilight_lib.ModAttributes.getHolder(mc.sayda.twilight_lib.ModAttributes.ALLOW_LEGGINGS));
        event.add(net.minecraft.world.entity.EntityType.PLAYER,
                mc.sayda.twilight_lib.ModAttributes.getHolder(mc.sayda.twilight_lib.ModAttributes.ALLOW_BOOTS));
        event.add(net.minecraft.world.entity.EntityType.PLAYER,
                mc.sayda.twilight_lib.ModAttributes.getHolder(mc.sayda.twilight_lib.ModAttributes.ELYTRA_FLIGHT));
    }
}
