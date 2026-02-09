package mc.sayda.twilight_lib.mixin;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.minecraft.world.entity.Entity.class)
public abstract class EntityMixin {

    @Invoker("setSharedFlag")
    public abstract void twilight_lib$setSharedFlag(int flag, boolean value);

}
