package mc.sayda.twilight_lib.fabric.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class FabricPlayerAttributeMixin {
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void twilight_lib$onCreateAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue()
                .add(ModAttributes.getHolder(ModAttributes.MINING_PENALTY))
                .add(ModAttributes.getHolder(ModAttributes.FOV_MODIFIER))
                .add(ModAttributes.getHolder(ModAttributes.ALLOW_HELMET))
                .add(ModAttributes.getHolder(ModAttributes.ALLOW_CHESTPLATE))
                .add(ModAttributes.getHolder(ModAttributes.ALLOW_LEGGINGS))
                .add(ModAttributes.getHolder(ModAttributes.ALLOW_BOOTS))
                .add(ModAttributes.getHolder(ModAttributes.ELYTRA_FLIGHT));
    }
}
