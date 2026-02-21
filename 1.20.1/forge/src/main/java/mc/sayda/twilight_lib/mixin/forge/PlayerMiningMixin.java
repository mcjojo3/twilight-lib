package mc.sayda.twilight_lib.mixin.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import mc.sayda.twilight_lib.ModAttributes;
import mc.sayda.twilight_lib.config.TwilightConfig;

@Mixin(Player.class)
public abstract class PlayerMiningMixin {

    @Inject(method = "getDigSpeed", at = @At("RETURN"), remap = false, cancellable = true)
    private void twilight_lib$modifyDigSpeed(BlockState state, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        float original = cir.getReturnValue();
        Player player = (Player) (Object) this;
        Attribute attrObj = ModAttributes.MINING_PENALTY.get();
        if (attrObj == null)
            return;

        if (player.getAttributeValue(attrObj) > 0) {
            return;
        }

        double multiplier = 1.0;

        boolean inWater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                && !net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(player);
        boolean onGround = player.onGround();

        if (inWater) {
            multiplier *= TwilightConfig.MINING_WATER_SLOWDOWN_MULTIPLIER.get();
        }

        if (!onGround) {
            multiplier *= TwilightConfig.MINING_FLIGHT_SLOWDOWN_MULTIPLIER.get();
        }

        if (multiplier > 1.0) {
            cir.setReturnValue(original * (float) multiplier);
        }
    }
}
