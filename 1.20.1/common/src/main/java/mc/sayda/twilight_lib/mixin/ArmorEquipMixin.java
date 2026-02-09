package mc.sayda.twilight_lib.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/**
 * DEPRECATED: Replaced by ArmorItemMixin and ArmorSlotMixin.
 * Kept momentarily to ensure clean migration.
 */
@Mixin(value = Player.class)
public class ArmorEquipMixin {
    // Logic moved to ArmorItemMixin (safe right-click) and ArmorSlotMixin (safe
    // container)
}
