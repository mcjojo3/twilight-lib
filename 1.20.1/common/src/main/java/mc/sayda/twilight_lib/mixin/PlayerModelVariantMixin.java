package mc.sayda.twilight_lib.mixin;

import mc.sayda.twilight_lib.capabilities.DataUtils;
import mc.sayda.twilight_lib.client.ClientModelVariantCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerInfo.class)
public class PlayerModelVariantMixin {

    @Inject(method = { "getModelName", "m_105051_" }, at = @At("RETURN"), cancellable = true, remap = false)
    private void twilightlib$overrideModelName(CallbackInfoReturnable<String> cir) {
        PlayerInfo playerInfo = (PlayerInfo) (Object) this;
        UUID playerUUID = playerInfo.getProfile().getId();

        // Check client player first (for first-person rendering)
        Minecraft minecraftInstance = Minecraft.getInstance();
        if (minecraftInstance.player != null && minecraftInstance.player.getUUID().equals(playerUUID)) {
            var modelVariantData = DataUtils.getModelVariantData(minecraftInstance.player);
            if (modelVariantData != null && modelVariantData.hasCustomVariant()) {
                String newModel = modelVariantData.getVariant()
                        .map(m -> m.isSlim() ? "slim" : "default")
                        .orElse(cir.getReturnValue());
                cir.setReturnValue(newModel);
                return;
            }
        }

        // For other players, use client-side cache
        ResourceLocation cachedVariantId = ClientModelVariantCache.getModelVariant(playerUUID);
        if (cachedVariantId != null) {
            String newModel = mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance()
                    .get(cachedVariantId)
                    .map(m -> m.isSlim() ? "slim" : "default")
                    .orElse(cir.getReturnValue());
            cir.setReturnValue(newModel);
        }
    }
}
