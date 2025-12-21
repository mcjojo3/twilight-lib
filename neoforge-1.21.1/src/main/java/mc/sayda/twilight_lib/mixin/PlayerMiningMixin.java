package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to override player mining speed based on custom attributes.
 * Removes vanilla mining penalties when mining_penalty attribute is 0.
 */
@Mixin(Player.class)
public class PlayerMiningMixin {

    // Cache the aqua affinity holder to avoid registry lookup on every getDigSpeed call
    private static volatile net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> AQUA_AFFINITY_CACHE = null;

    /**
     * Inject into getDigSpeed to override mining slowdowns based on custom attribute.
     * Vanilla applies slowdowns for:
     * - Underwater without aqua affinity (configurable multiplier, default 5x)
     * - Not on ground / flight break (configurable multiplier, default 5x)
     * - Both together = default 25x slowdown!
     *
     * If mining_penalty attribute is 0, we remove both slowdowns entirely.
     */
    @Inject(
        method = "getDigSpeed",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private void twilightlib$onGetDigSpeed(BlockState state, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;

        // Validate attribute exists to prevent NPE if ModAttributes initialization failed
        var attributeInstance = player.getAttribute(ModAttributes.MINING_PENALTY);
        if (attributeInstance == null) {
            return; // No mining penalty removal if attribute missing (vanilla behavior)
        }

        double miningPenalty = attributeInstance.getValue();

        if (miningPenalty == 0.0) {
            float currentSpeed = cir.getReturnValue();
            double multiplier = 1.0;

            boolean inWater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
            boolean onGround = player.onGround();

            // Check for aqua affinity using proper 1.21.1 API with cached holder
            // Lazy initialization of the enchantment holder on first use
            if (AQUA_AFFINITY_CACHE == null) {
                net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> enchantmentRegistry =
                    player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                AQUA_AFFINITY_CACHE = enchantmentRegistry.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY);
            }

            // Check if player has aqua affinity on any equipment (typically helmet)
            boolean hasAquaAffinity = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(AQUA_AFFINITY_CACHE, player) > 0;

            // Check if underwater slowdown is active (in water, no aqua affinity)
            if (inWater && !hasAquaAffinity) {
                double waterMultiplier = mc.sayda.twilight_lib.config.TwilightConfig.MINING_WATER_SLOWDOWN_MULTIPLIER.get();
                // Validate config value is reasonable (between 1.0 and 100.0)
                if (waterMultiplier < 1.0 || waterMultiplier > 100.0) {
                    return; // Invalid config, skip modification (keep vanilla behavior)
                }
                multiplier *= waterMultiplier;
            }

            // Check if flight break slowdown is active (not on ground)
            if (!onGround) {
                double flightMultiplier = mc.sayda.twilight_lib.config.TwilightConfig.MINING_FLIGHT_SLOWDOWN_MULTIPLIER.get();
                // Validate config value is reasonable (between 1.0 and 100.0)
                if (flightMultiplier < 1.0 || flightMultiplier > 100.0) {
                    return; // Invalid config, skip modification (keep vanilla behavior)
                }
                multiplier *= flightMultiplier;
            }

            // Restore speed by multiplying back
            if (multiplier > 1.0) {
                cir.setReturnValue((float) (currentSpeed * multiplier));
            }
        }
    }
}
