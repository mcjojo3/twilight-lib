package mc.sayda.twilight_lib.mixin;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(AttributeInstance.class)
public interface AttributeInstanceAccessor {
    @Invoker("<init>")
    static AttributeInstance create(net.minecraft.core.Holder<Attribute> attribute,
            Consumer<AttributeInstance> onDirty) {
        throw new AssertionError();
    }
}
