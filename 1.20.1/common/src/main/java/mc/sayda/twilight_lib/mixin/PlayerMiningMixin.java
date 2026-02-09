package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Common Mixin to override player mining speed based on attributes and config.
 * Complements PlayerMixin's redirects by applying configurable multipliers.
 */
@Mixin(Player.class)
public class PlayerMiningMixin {

    @Inject(method = "getDigSpeed", // Forge-added method
            at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void twilightlib$onGetDigSpeed(BlockState state, net.minecraft.core.BlockPos pos,
            CallbackInfoReturnable<Float> cir) {
        twilightlib$onGetDestroySpeed(state, cir);
    }

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void twilightlib$onGetDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;

        // Skip if penalty is not removed
        if (player.getAttributeValue(ModAttributes.getHolder(ModAttributes.MINING_PENALTY)) > 0) {
            return;
        }

        float currentSpeed = cir.getReturnValue();
        double multiplier = 1.0;

        boolean inWater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        boolean onGround = player.onGround();

        boolean hasAquaAffinity = false;
        if (net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY != null) {
            hasAquaAffinity = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY, player) > 0;
        }

        if (inWater && !hasAquaAffinity) {
            double waterMultiplier = mc.sayda.twilight_lib.config.TwilightConfig.MINING_WATER_SLOWDOWN_MULTIPLIER
                    .get();
            if (waterMultiplier >= 1.0 && waterMultiplier <= 100.0) {
                multiplier *= waterMultiplier;
            }
        }

        if (!onGround) {
            double flightMultiplier = mc.sayda.twilight_lib.config.TwilightConfig.MINING_FLIGHT_SLOWDOWN_MULTIPLIER
                    .get();
            if (flightMultiplier >= 1.0 && flightMultiplier <= 100.0) {
                multiplier *= flightMultiplier;
            }
        }

        if (multiplier > 1.0) {
            cir.setReturnValue((float) (currentSpeed * multiplier));
        }
    }
}
