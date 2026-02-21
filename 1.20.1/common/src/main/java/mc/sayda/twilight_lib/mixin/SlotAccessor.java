package mc.sayda.twilight_lib.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for Slot.container (Forge: "container", Yarn: "inventory").
 * Using @Accessor ensures Loom remaps the field name correctly for both
 * platforms.
 */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Accessor("container")
    Container twilight$getContainer();
}
