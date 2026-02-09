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

    private static volatile net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> AQUA_AFFINITY_CACHE = null;
    private static boolean AQUA_AFFINITY_FAILED = false;

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

        if (AQUA_AFFINITY_CACHE == null && !AQUA_AFFINITY_FAILED) {
            try {
                net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> enchantmentRegistry = player
                        .level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                AQUA_AFFINITY_CACHE = enchantmentRegistry
                        .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY);
            } catch (Exception e) {
                AQUA_AFFINITY_FAILED = true;
                return;
            }
        }

        boolean hasAquaAffinity = net.minecraft.world.item.enchantment.EnchantmentHelper
                .getEnchantmentLevel(AQUA_AFFINITY_CACHE, player) > 0;

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
