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
@Mixin(Player.class)
public class ArmorEquipMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Inject into setItemSlot to prevent armor equipping when restricted by attributes.
     * Production injection (SRG name).
     */
    @Inject(
        method = "m_8061_", // SRG name for setItemSlot in 1.20.1
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0 // Optional - works in production
    )
    private void twilightlib$onSetItemSlot_SRG(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        onSetItemSlot(slot, stack, ci);
    }

    /**
     * Development injection (MojMap name).
     */
    @Inject(
        method = "setItemSlot",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0 // Optional - works in dev
    )
    private void twilightlib$onSetItemSlot_MojMap(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        onSetItemSlot(slot, stack, ci);
    }

    /**
     * Shared logic for both setItemSlot injections.
     * Checks allow_helmet, allow_chestplate, allow_leggings, allow_boots attributes.
     * If attribute is 0, prevents that armor type from being equipped.
     */
    private void onSetItemSlot(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
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
        double allowAttribute = 1.0;
        switch (armorItem.getType()) {
            case HELMET -> allowAttribute = player.getAttributeValue(ModAttributes.ALLOW_HELMET.get());
            case CHESTPLATE -> allowAttribute = player.getAttributeValue(ModAttributes.ALLOW_CHESTPLATE.get());
            case LEGGINGS -> allowAttribute = player.getAttributeValue(ModAttributes.ALLOW_LEGGINGS.get());
            case BOOTS -> allowAttribute = player.getAttributeValue(ModAttributes.ALLOW_BOOTS.get());
        }

        // Cancel equipping if attribute is 0
        if (allowAttribute == 0.0) {
            ci.cancel();
        }
    }
}