package mc.sayda.twilight_lib.forge;

import mc.sayda.twilight_lib.ModAttributes;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.*;
import mc.sayda.twilight_lib.capabilities.forge.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TwilightLib.MODID)
public class TwilightLibForge {
    public TwilightLibForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        dev.architectury.platform.forge.EventBuses.registerModEventBus(TwilightLib.MODID, modBus);
        TwilightLib.init();

        // Client init must happen in the constructor (before EntityRenderersEvent
        // fires)
        // so Architectury's model layer registration APIs can queue their hooks in
        // time.
        // This mirrors the NeoForge module's approach.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            mc.sayda.twilight_lib.client.TwilightLibClient.init();
        }

        modBus.addListener(this::onRegisterCapabilities);
        modBus.addListener(this::onAttributeModification);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::attachEntityCaps);
    }

    private void onAttributeModification(final EntityAttributeModificationEvent event) {
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.MINING_PENALTY.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.FOV_MODIFIER.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_HELMET.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_CHESTPLATE.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_LEGGINGS.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ALLOW_BOOTS.get());
        event.add(net.minecraft.world.entity.EntityType.PLAYER, ModAttributes.ELYTRA_FLIGHT.get());
    }

    private void onRegisterCapabilities(final RegisterCapabilitiesEvent evt) {
        evt.register(IMorph.class);
        evt.register(IAddons.class);
        evt.register(ITrails.class);
        evt.register(IEffects.class);
        evt.register(IModelVariant.class);
    }

    private void attachEntityCaps(final AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof Player) {
            evt.addCapability(new ResourceLocation(TwilightLib.MODID, "morph"), new MorphProvider());
            evt.addCapability(new ResourceLocation(TwilightLib.MODID, "addons"), new AddonsProvider());
            evt.addCapability(new ResourceLocation(TwilightLib.MODID, "trails"), new TrailsProvider());
            evt.addCapability(new ResourceLocation(TwilightLib.MODID, "effects"), new EffectsProvider());
            evt.addCapability(new ResourceLocation(TwilightLib.MODID, "model_variant"), new ModelVariantProvider());
        }
    }
}
