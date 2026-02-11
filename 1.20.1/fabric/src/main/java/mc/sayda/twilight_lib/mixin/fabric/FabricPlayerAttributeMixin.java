package mc.sayda.twilight_lib.mixin.fabric;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class FabricPlayerAttributeMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void twilight_lib$addAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(ModAttributes.MINING_PENALTY.get())
                .add(ModAttributes.FOV_MODIFIER.get())
                .add(ModAttributes.ALLOW_HELMET.get())
                .add(ModAttributes.ALLOW_CHESTPLATE.get())
                .add(ModAttributes.ALLOW_LEGGINGS.get())
                .add(ModAttributes.ALLOW_BOOTS.get())
                .add(ModAttributes.ELYTRA_FLIGHT.get());
    }
}
