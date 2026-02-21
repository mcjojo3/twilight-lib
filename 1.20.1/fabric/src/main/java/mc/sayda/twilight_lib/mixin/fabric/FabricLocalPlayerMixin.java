package mc.sayda.twilight_lib.mixin.fabric;

import mc.sayda.twilight_lib.ModAttributes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class FabricLocalPlayerMixin {

    @Unique
    private boolean twilight$redirectIsElytraBody(ItemStack stack, net.minecraft.world.item.Item item) {
        var elytraAttr = ((LivingEntity) (Object) this).getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
        if (item == Items.ELYTRA && elytraAttr != null && elytraAttr.getValue() > 0
                && !((LivingEntity) (Object) this).isInWater()) {
            return true;
        }
        return stack.is(item);
    }

    @Unique
    private boolean twilight$redirectIsFlyEnabledBody(ItemStack stack) {
        var elytraAttr = ((LivingEntity) (Object) this).getAttribute(ModAttributes.ELYTRA_FLIGHT.get());
        if (elytraAttr != null && elytraAttr.getValue() > 0 && !((LivingEntity) (Object) this).isInWater()) {
            return true;
        }
        return net.minecraft.world.item.ElytraItem.isFlyEnabled(stack);
    }

    @SuppressWarnings("null")
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private boolean twilight$redirectIsElytra(ItemStack stack, net.minecraft.world.item.Item item) {
        if (item == net.minecraft.world.item.Items.ELYTRA && twilight$redirectIsFlyEnabledBody(stack)) {
            return true;
        }
        return stack.is(item);
    }

    @SuppressWarnings("null")
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ElytraItem;isFlyEnabled(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean twilight$redirectIsFlyEnabled(ItemStack stack) {
        return twilight$redirectIsFlyEnabledBody(stack);
    }
}
