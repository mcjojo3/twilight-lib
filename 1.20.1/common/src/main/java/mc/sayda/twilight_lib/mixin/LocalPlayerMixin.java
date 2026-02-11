package mc.sayda.twilight_lib.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Reserved for future client-side enhancements (e.g. morph physics, input
 * handling).
 * Currently serves as a placeholder for capability-related state access on the
 * client.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends LivingEntity {

    @Shadow
    public net.minecraft.client.player.Input input;

    @Shadow
    public net.minecraft.client.multiplayer.ClientPacketListener connection;

    protected LocalPlayerMixin(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
    }
}
