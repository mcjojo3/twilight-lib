package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.client.ClientModelVariantCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerInfo.class)
public class PlayerModelVariantMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void twilightlib$overridePlayerSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerInfo playerInfo = (PlayerInfo) (Object) this;
        UUID playerUUID = playerInfo.getProfile().getId();
        PlayerSkin originalSkin = cir.getReturnValue();

        if (originalSkin == null)
            return;

        // Check client player first (for first-person rendering)
        Minecraft minecraftInstance = Minecraft.getInstance();
        if (minecraftInstance.player != null && minecraftInstance.player.getUUID().equals(playerUUID)) {
            var modelVariantData = DataUtils.getModelVariantData(minecraftInstance.player);
            if (modelVariantData != null && modelVariantData.hasCustomVariant()) {
                PlayerSkin.Model newModel = modelVariantData.getVariant()
                        .<Boolean>map(m -> m.isSlim()).<PlayerSkin.Model>map(
                                slim -> slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE)
                        .orElse(originalSkin.model());

                PlayerSkin newSkin = new PlayerSkin(
                        originalSkin.texture(),
                        originalSkin.textureUrl(),
                        originalSkin.capeTexture(),
                        originalSkin.elytraTexture(),
                        newModel,
                        originalSkin.secure());
                cir.setReturnValue(newSkin);
                return;
            }
        }

        // For other players, use client-side cache
        net.minecraft.resources.ResourceLocation cachedVariantId = ClientModelVariantCache.getModelVariant(playerUUID);
        if (cachedVariantId != null) {
            PlayerSkin.Model newModel = mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance()
                    .get(cachedVariantId)
                    .<Boolean>map(m -> m.isSlim()).<PlayerSkin.Model>map(
                            slim -> slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE)
                    .orElse(originalSkin.model());

            PlayerSkin newSkin = new PlayerSkin(
                    originalSkin.texture(),
                    originalSkin.textureUrl(),
                    originalSkin.capeTexture(),
                    originalSkin.elytraTexture(),
                    newModel,
                    originalSkin.secure());
            cir.setReturnValue(newSkin);
        }
    }
}