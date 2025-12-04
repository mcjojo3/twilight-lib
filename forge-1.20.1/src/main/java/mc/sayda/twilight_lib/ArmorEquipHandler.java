package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Event handler for armor equipping restrictions based on custom attributes.
 */
@Mod.EventBusSubscriber(modid = TwilightLib.MODID)
public class ArmorEquipHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        // Only check players
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Only check armor being equipped (not removed)
        if (event.getTo().isEmpty() || !(event.getTo().getItem() instanceof ArmorItem armorItem)) {
            return;
        }

        // Check attribute based on armor type
        double allowAttribute = switch (armorItem.getType()) {
            case HELMET -> player.getAttributeValue(ModAttributes.ALLOW_HELMET.get());
            case CHESTPLATE -> player.getAttributeValue(ModAttributes.ALLOW_CHESTPLATE.get());
            case LEGGINGS -> player.getAttributeValue(ModAttributes.ALLOW_LEGGINGS.get());
            case BOOTS -> player.getAttributeValue(ModAttributes.ALLOW_BOOTS.get());
            default -> 1.0;
        };

        // Block equipping if attribute is 0 by returning armor to inventory
        if (allowAttribute == 0.0) {
            // Remove armor from slot and add back to player's inventory
            player.setItemSlot(event.getSlot(), event.getFrom());
            // Give the armor back to the player's inventory
            if (!player.getInventory().add(event.getTo().copy())) {
                // If inventory is full, drop it
                player.drop(event.getTo().copy(), false);
            }
        }
    }
}
