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

        double miningPenalty = player.getAttributeValue(ModAttributes.MINING_PENALTY);

        if (miningPenalty == 0.0) {
            float currentSpeed = cir.getReturnValue();
            double multiplier = 1.0;

            boolean inWater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
            boolean onGround = player.onGround();

            // Check for aqua affinity using proper 1.21.1 API
            // Get the enchantment holder from the registry
            boolean hasAquaAffinity = false;
            net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> enchantmentRegistry =
                player.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> aquaAffinityHolder =
                enchantmentRegistry.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY);

            // Check if player has aqua affinity on any equipment (typically helmet)
            hasAquaAffinity = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(aquaAffinityHolder, player) > 0;

            // Check if underwater slowdown is active (in water, no aqua affinity)
            if (inWater && !hasAquaAffinity) {
                multiplier *= mc.sayda.twilight_lib.config.TwilightConfig.MINING_WATER_SLOWDOWN_MULTIPLIER.get();
            }

            // Check if flight break slowdown is active (not on ground)
            if (!onGround) {
                multiplier *= mc.sayda.twilight_lib.config.TwilightConfig.MINING_FLIGHT_SLOWDOWN_MULTIPLIER.get();
            }

            // Restore speed by multiplying back
            if (multiplier > 1.0) {
                cir.setReturnValue((float) (currentSpeed * multiplier));
            }
        }
    }
}