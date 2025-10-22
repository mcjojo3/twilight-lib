package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {

    /**
     * Inject into getDigSpeed to override mining slowdowns based on custom attribute.
     * Vanilla applies:
     * - 5x slowdown when underwater without aqua affinity
     * - 5x slowdown when not on ground (flight break)
     * - Both together = 25x slowdown!
     *
     * If mining_penalty attribute is 0, we remove both slowdowns.
     */
    @Inject(
        method = "getDigSpeed",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void twilightlib$onGetDigSpeed(BlockState state, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;

        double miningPenalty = player.getAttributeValue(ModAttributes.MINING_PENALTY.get());

        if (miningPenalty == 0.0) {
            float currentSpeed = cir.getReturnValue();
            int multiplier = 1;

            boolean inWater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
            boolean hasAquaAffinity = net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(player);
            boolean onGround = player.onGround();

            // Check if underwater slowdown is active (in water, no aqua affinity)
            if (inWater && !hasAquaAffinity) {
                multiplier *= 5;
            }

            // Check if flight break slowdown is active (not on ground)
            if (!onGround) {
                multiplier *= 5;
            }

            // Restore speed by multiplying back
            if (multiplier > 1) {
                cir.setReturnValue(currentSpeed * multiplier);
            }
        }
    }
}