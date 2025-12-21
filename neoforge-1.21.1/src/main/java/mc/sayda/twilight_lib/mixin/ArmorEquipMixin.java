package mc.sayda.twilight_lib.mixin;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent armor equipping based on custom attributes.
 * Allows addons to restrict specific armor slots when cosmetics would conflict.
 */
@Mixin(value = Player.class, remap = false)
public class ArmorEquipMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Inject into setItemSlot to prevent armor equipping when restricted by attributes.
     * Checks allow_helmet, allow_chestplate, allow_leggings, allow_boots attributes.
     * If attribute is 0, prevents that armor type from being equipped.
     *
     * NOTE: This mixin only blocks NEW armor from being equipped when restricted.
     * It does NOT block removing restricted armor or swapping between items.
     * The ArmorEquipHandler event handles cleanup after swaps/equips.
     *
     * KNOWN EDGE CASE: Swapping two restricted armors may cause event loop
     * (requires both armors to have attribute=0). ArmorEquipHandler needs
     * loop protection for this rare scenario.
     */
    @Inject(
        method = "setItemSlot",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void twilightlib$onSetItemSlot(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // Only check armor slots
        if (!slot.isArmor() || stack.isEmpty()) {
            return;
        }

        // Only check if item is actually armor
        if (!(stack.getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        // Check attribute based on armor type
        // Validate attribute exists to prevent NPE if ModAttributes initialization failed
        var attributeInstance = switch (armorItem.getType()) {
            case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET);
            case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE);
            case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS);
            case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS);
            default -> null;
        };

        if (attributeInstance == null) {
            return; // Allow equipping if attribute missing (fail-safe default)
        }

        double allowAttribute = attributeInstance.getValue();

        // Block ONLY if equipping to empty slot
        // Allow swaps to proceed (ArmorEquipHandler will revert if needed)
        // This prevents armor from vanishing during failed swaps
        ItemStack currentArmor = player.getItemBySlot(slot);
        if (allowAttribute == 0.0 && currentArmor.isEmpty()) {
            ci.cancel();
        }
    }
}
