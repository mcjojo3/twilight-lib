package mc.sayda.twilight_lib.mixin.fabric.client;

import mc.sayda.twilight_lib.client.renderer.PlayerAddonLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import mc.sayda.twilight_lib.client.renderer.MorphRenderHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model,
            float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void twilight_lib$addLayers(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        this.addLayer(new PlayerAddonLayer(this));
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    public void twilight_lib$render(AbstractClientPlayer entity, float entityYaw, float partialTicks,
            PoseStack matrixStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (MorphRenderHandler.onRenderPlayerPre(entity,
                (PlayerRenderer) (Object) this, partialTicks, matrixStack, buffer, packedLight)) {
            ci.cancel();
        }
    }
}
