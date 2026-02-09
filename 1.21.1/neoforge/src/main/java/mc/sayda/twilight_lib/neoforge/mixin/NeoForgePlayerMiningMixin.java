package mc.sayda.twilight_lib.neoforge.mixin;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge-specific Mixin for player mining speed.
 * Targets getDigSpeed which is the method used by NeoForge for block breaking
 * speed calculation.
 */
@Mixin(Player.class)
public class NeoForgePlayerMiningMixin {

    private static volatile net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> AQUA_AFFINITY_CACHE = null;

    @Inject(method = "getDigSpeed", at = @At("RETURN"), cancellable = true, remap = false // NeoForge/Forge added method
    )
    private void twilightlib$onGetDigSpeed(BlockState state, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;

        // Skip if penalty is not removed (value > 0 means penalty is active/default)
        if (player.getAttributeValue(ModAttributes.getHolder(ModAttributes.MINING_PENALTY)) > 0) {
            return;
        }

        float currentSpeed = cir.getReturnValue();
        double multiplier = 1.0;

        boolean inWater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        boolean onGround = player.onGround();

        if (AQUA_AFFINITY_CACHE == null) {
            try {
                net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> enchantmentRegistry = player
                        .level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                AQUA_AFFINITY_CACHE = enchantmentRegistry
                        .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY);
            } catch (Exception e) {
                return;
            }
        }

        boolean hasAquaAffinity = net.minecraft.world.item.enchantment.EnchantmentHelper
                .getEnchantmentLevel(AQUA_AFFINITY_CACHE, player) > 0;

        if (inWater && !hasAquaAffinity) {
            double waterMultiplier = mc.sayda.twilight_lib.config.TwilightConfig.MINING_WATER_SLOWDOWN_MULTIPLIER.get();
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
