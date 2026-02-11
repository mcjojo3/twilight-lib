package mc.sayda.twilight_lib.mixin.forge;

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

    @Inject(method = "getModelName", at = @At("RETURN"), cancellable = true)
    private void twilightlib$overrideModelName(CallbackInfoReturnable<String> cir) {
        PlayerInfo playerInfo = (PlayerInfo) (Object) this;
        UUID playerUUID = playerInfo.getProfile().getId();

        // 1. Check client-side cache first (most reliable for network-synced variants)
        ResourceLocation cachedVariantId = ClientModelVariantCache.getModelVariant(playerUUID);
        if (cachedVariantId != null) {
            mc.sayda.twilight_lib.api.model_variant.IModelVariantRegistry.getInstance()
                    .get(cachedVariantId)
                    .ifPresent(m -> cir.setReturnValue(m.isSlim() ? "slim" : "default"));
            return;
        }

        // 2. Fallback to capability (useful for self if cache not yet populated)
        // Note: On Forge, capabilities might be available earlier or differently than
        // Fabric
        Minecraft minecraftInstance = Minecraft.getInstance();
        if (minecraftInstance.player != null && minecraftInstance.player.getUUID().equals(playerUUID)) {
            var modelVariantData = DataUtils.getModelVariantData(minecraftInstance.player);
            if (modelVariantData != null && modelVariantData.hasCustomVariant()) {
                modelVariantData.getVariant()
                        .ifPresent(m -> cir.setReturnValue(m.isSlim() ? "slim" : "default"));
            }
        }
    }
}
