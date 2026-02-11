package mc.sayda.twilight_lib.mixin.fabric;

import mc.sayda.twilight_lib.fabric.FabricPlayerExtensions;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void twilight_lib$onRestoreFrom(ServerPlayer that, boolean keepEverything, CallbackInfo ci) {
        ((FabricPlayerExtensions) this).twilight_lib$copyFrom(that);
    }
}
