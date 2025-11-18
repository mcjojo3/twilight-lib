package mc.sayda.twilight_lib.client;

import mc.sayda.twilight_lib.ModAttributes;
import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

// In 1.21.1, bus parameter is deprecated - game bus is implied for non-mod events
@EventBusSubscriber(modid = TwilightLib.MODID, value = Dist.CLIENT)
public class FOVHandler {
    private static volatile float lastFovModifier = 1.0F;

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOW)
    public static void onFOVModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        double fovModifier = player.getAttributeValue(ModAttributes.FOV_MODIFIER);

        if (fovModifier >= 1.0) {
            // Allow all vanilla FOV changes
            lastFovModifier = event.getNewFovModifier();
            return;
        }

        float currentModifier = event.getNewFovModifier();
        float fovDelta = currentModifier - lastFovModifier;
        float distanceFromNormal = Math.abs(currentModifier - 1.0F);
        float lastDistanceFromNormal = Math.abs(lastFovModifier - 1.0F);

        // Check if FOV is moving towards or away from normal (1.0)
        boolean movingTowardsNormal = distanceFromNormal < lastDistanceFromNormal;

        if (fovModifier <= 0.0) {
            // When disabled: force FOV to 1.0 (completely override Pehkui and other mods)
            event.setNewFovModifier(1.0F);
            lastFovModifier = 1.0F;
        } else {
            // Partial FOV modification: scale changes away from normal
            if (!movingTowardsNormal && Math.abs(fovDelta) > 0.001F) {
                float scaledDelta = fovDelta * (float)fovModifier;
                float newModifier = lastFovModifier + scaledDelta;
                event.setNewFovModifier(newModifier);
                lastFovModifier = newModifier;
            } else {
                lastFovModifier = currentModifier;
            }
        }
    }
}
