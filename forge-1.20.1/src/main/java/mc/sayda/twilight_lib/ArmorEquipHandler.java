package mc.sayda.twilight_lib;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
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
        // Validate attribute exists to prevent NPE if ModAttributes initialization failed
        var attributeInstance = switch (armorItem.getType()) {
            case HELMET -> player.getAttribute(ModAttributes.ALLOW_HELMET.get());
            case CHESTPLATE -> player.getAttribute(ModAttributes.ALLOW_CHESTPLATE.get());
            case LEGGINGS -> player.getAttribute(ModAttributes.ALLOW_LEGGINGS.get());
            case BOOTS -> player.getAttribute(ModAttributes.ALLOW_BOOTS.get());
            default -> null;
        };

        if (attributeInstance == null) {
            LOGGER.warn("Or, what. Armor attribute not registered for {} - allowing equip by default",
                armorItem.getType());
            return; // Allow equipping if attribute missing (fail-safe default)
        }

        double allowAttribute = attributeInstance.getValue();

        // Block equipping if attribute is 0 by returning armor to inventory
        if (allowAttribute == 0.0) {
            ItemStack armor = event.getTo().copy(); // Copy once to avoid duplicate object creation
            LOGGER.debug("Or, what. Player {} cannot equip {} - restricted by attribute",
                player.getName().getString(), armor.getDisplayName().getString());

            // Remove armor from slot and add back to player's inventory
            player.setItemSlot(event.getSlot(), event.getFrom());
            // Give the armor back to the player's inventory
            if (!player.getInventory().add(armor)) {
                // If inventory is full, drop it
                LOGGER.debug("Yeah? Well... Player {} inventory full, dropping {}",
                    player.getName().getString(), armor.getDisplayName().getString());
                player.drop(armor, false);
            }
        }
    }
}
