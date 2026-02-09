package mc.sayda.twilight_lib.compat.legacy.creraces;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Handles communication with CreRaces mod without a hard dependency.
 * Uses reflection to access CreRaces variables and attributes.
 */
public class CreRacesInterop {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CRERACES_MODID = "creraces";
    private static boolean isLoaded = false;

    // Reflection cache (Variables)
    private static Supplier<?> playerVariablesAttachment;
    private static Class<?> playerVariablesClass;
    private static Method syncMethod;
    private static Field isRaceField;
    private static Field raceUpdateField;
    private static Field hasChoosenRaceField;
    private static Field hasChoosenSubRaceField;
    private static Field passiveStacksField;
    private static Field factionField;
    private static Field resourceBarField;
    private static Field raceAbilityToggleField;
    private static Field raceAbilityKeepField;
    private static Field sizeField;
    private static Field raceRankingField;
    private static Field skillSelectField;
    private static Field pocketSizeField;
    private static Field progressionStateField;
    private static Field gStateField;
    private static Field raceStringField;
    private static Field raceStyleField;
    private static Field raceStyle2Field;
    private static Field raceStyle3Field;
    private static Field humanCheckField;
    private static Field showRaceOverlayField;
    private static Field hasPocketField;
    private static Field karmaField;
    private static Field tempValue1Field;
    private static Field tempValue2Field;
    private static Field tempValue3Field;
    private static Field tempValue4Field;
    private static Field dxField;
    private static Field dyField;
    private static Field dzField;
    private static Field pxField;
    private static Field pyField;
    private static Field pzField;
    private static Field passiveCooldownField;
    private static Field ultimateCooldownField;
    private static Field ultimateCooldown2Field;

    // Attribute Holders (Custom CreRaces)
    private static net.minecraft.core.Holder<Attribute> HEIGHT_ATTR;
    private static net.minecraft.core.Holder<Attribute> WIDTH_ATTR;
    private static net.minecraft.core.Holder<Attribute> DEFENSE_ATTR;
    private static net.minecraft.core.Holder<Attribute> MINING_SPEED_ATTR;
    private static net.minecraft.core.Holder<Attribute> JUMP_HEIGHT_ATTR;
    private static net.minecraft.core.Holder<Attribute> FALL_SPEED_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_PASSIVE_CD_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_A1_CD_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_A2_CD_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_A3_CD_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_A4_CD_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_RACE_MANA_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_RACE_RAGE_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_RACE_ENERGY_ATTR;
    private static net.minecraft.core.Holder<Attribute> MAX_RACE_GRIT_ATTR;

    @SuppressWarnings("unchecked")
    public static void init() {
        if (!dev.architectury.platform.Platform.isModLoaded(CRERACES_MODID)) {
            return;
        }

        try {
            LOGGER.info("What's good? Twilight Lib: CreRaces detected. Initializing interop bridge...");

            // Get AttachmentType
            Class<?> varsClass = Class.forName("mc.sayda.creraces.network.CreracesModVariables");
            Field attachmentField = varsClass.getField("PLAYER_VARIABLES");
            playerVariablesAttachment = (Supplier<?>) attachmentField.get(null);

            // Get PlayerVariables class and fields
            playerVariablesClass = Class.forName("mc.sayda.creraces.network.CreracesModVariables$PlayerVariables");

            isRaceField = playerVariablesClass.getField("IsRace");
            raceUpdateField = playerVariablesClass.getField("RaceUpdate");
            hasChoosenRaceField = playerVariablesClass.getField("HasChoosenRace");
            hasChoosenSubRaceField = playerVariablesClass.getField("HasChoosenSubRace");
            passiveStacksField = playerVariablesClass.getField("PassiveStacks");
            factionField = playerVariablesClass.getField("Faction");
            resourceBarField = playerVariablesClass.getField("ResourceBar");
            raceAbilityToggleField = playerVariablesClass.getField("RaceAbilityToggle");
            raceAbilityKeepField = playerVariablesClass.getField("RaceAbilityKeep");
            sizeField = playerVariablesClass.getField("Size");
            raceRankingField = playerVariablesClass.getField("RaceRanking");
            skillSelectField = playerVariablesClass.getField("SkillSelect");
            pocketSizeField = playerVariablesClass.getField("PocketSize");
            progressionStateField = playerVariablesClass.getField("progressionState");
            gStateField = playerVariablesClass.getField("gState");
            raceStringField = playerVariablesClass.getField("RaceString");
            raceStyleField = playerVariablesClass.getField("raceStyle");
            raceStyle2Field = playerVariablesClass.getField("raceStyle2");
            raceStyle3Field = playerVariablesClass.getField("raceStyle3");
            humanCheckField = playerVariablesClass.getField("HumanCheck");
            showRaceOverlayField = playerVariablesClass.getField("ShowRaceOverlay");
            hasPocketField = playerVariablesClass.getField("HasPocket");
            karmaField = playerVariablesClass.getField("Karma");
            tempValue1Field = playerVariablesClass.getField("TempValue1");
            tempValue2Field = playerVariablesClass.getField("TempValue2");
            tempValue3Field = playerVariablesClass.getField("TempValue3");
            tempValue4Field = playerVariablesClass.getField("TempValue4");
            dxField = playerVariablesClass.getField("dx");
            dyField = playerVariablesClass.getField("dy");
            dzField = playerVariablesClass.getField("dz");
            pxField = playerVariablesClass.getField("px");
            pyField = playerVariablesClass.getField("py");
            pzField = playerVariablesClass.getField("pz");
            passiveCooldownField = playerVariablesClass.getField("PassiveCooldown");
            ultimateCooldownField = playerVariablesClass.getField("UltimateCooldown");
            ultimateCooldown2Field = playerVariablesClass.getField("UltimateCooldown2");

            syncMethod = playerVariablesClass.getMethod("syncPlayerVariables", Entity.class);

            // Initialize Attribute Holders
            HEIGHT_ATTR = getAttr("height");
            WIDTH_ATTR = getAttr("width");
            DEFENSE_ATTR = getAttr("defense");
            MINING_SPEED_ATTR = getAttr("mining_speed");
            JUMP_HEIGHT_ATTR = getAttr("jump_height");
            FALL_SPEED_ATTR = getAttr("fall_speed");
            MAX_PASSIVE_CD_ATTR = getAttr("max_passive_cd");
            MAX_A1_CD_ATTR = getAttr("max_a1_cd");
            MAX_A2_CD_ATTR = getAttr("max_a2_cd");
            MAX_A3_CD_ATTR = getAttr("max_a3_cd");
            MAX_A4_CD_ATTR = getAttr("max_a4_cd");
            MAX_RACE_MANA_ATTR = getAttr("max_race_mana");
            MAX_RACE_RAGE_ATTR = getAttr("max_race_rage");
            MAX_RACE_ENERGY_ATTR = getAttr("max_race_energy");
            MAX_RACE_GRIT_ATTR = getAttr("max_race_grit");

            isLoaded = true;
            LOGGER.info(
                    "We are going to be best friends! Twilight Lib: CreRaces interop bridge initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Twilight Lib: Failed to initialize CreRaces interop bridge!", e);
        }
    }

    private static net.minecraft.core.Holder<Attribute> getAttr(String name) {
        try {
            return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.fromNamespaceAndPath(CRERACES_MODID, name))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Resets all CreRaces variables and attributes for the given entity.
     * Replaces the functionality of CreRaces' MakeResetProcedure.
     */
    public static void resetRace(Entity entity) {
        if (!isLoaded)
            return;

        try {
            // 1. Data Reset (Variables)
            // Use reflection for entity.getData() to avoid AttachmentType import
            Object vars = null;
            try {
                Method getDataMethod = entity.getClass().getMethod("getData", Supplier.class);
                vars = getDataMethod.invoke(entity, playerVariablesAttachment);
            } catch (Exception e) {
                // Try fallback for older NeoForge or if method name is different in some
                // environments
                LOGGER.debug("How did I?! Uuuughh! Failed to get CreRaces data via reflection: {}", e.getMessage());
            }

            if (vars == null)
                return;

            double oldRace = (double) isRaceField.get(vars);

            isRaceField.set(vars, 0.0);
            raceUpdateField.set(vars, true);
            hasChoosenRaceField.set(vars, false);
            hasChoosenSubRaceField.set(vars, false);
            passiveStacksField.set(vars, 0.0);
            factionField.set(vars, 0.0);
            resourceBarField.set(vars, 0.0);
            raceAbilityToggleField.set(vars, 0.0);
            raceAbilityKeepField.set(vars, 0.0);
            sizeField.set(vars, 1.0);
            raceRankingField.set(vars, 0.0);
            skillSelectField.set(vars, 0.0);
            pocketSizeField.set(vars, 0.0);
            progressionStateField.set(vars, 0.0);
            raceStringField.set(vars, "None");
            raceStyleField.set(vars, "none");
            raceStyle2Field.set(vars, "none");
            raceStyle3Field.set(vars, "none");
            humanCheckField.set(vars, false);
            showRaceOverlayField.set(vars, true);
            hasPocketField.set(vars, false);
            karmaField.set(vars, 0.0);
            tempValue1Field.set(vars, 0.0);
            tempValue2Field.set(vars, 0.0);
            tempValue3Field.set(vars, 0.0);
            tempValue4Field.set(vars, 0.0);
            dxField.set(vars, 0.0);
            dyField.set(vars, 0.0);
            dzField.set(vars, 0.0);
            pxField.set(vars, 0.0);
            pyField.set(vars, 0.0);
            pzField.set(vars, 0.0);
            passiveCooldownField.set(vars, 0.0);
            ultimateCooldownField.set(vars, 0.0);
            ultimateCooldown2Field.set(vars, 0.0);

            syncMethod.invoke(vars, entity);

            // 2. Physical & Attribute Reset
            if (entity instanceof LivingEntity living) {
                // Clear all effects
                living.removeAllEffects();

                // Reset modifiers
                removeCreRacesModifiers(living);

                // Reset base attributes to defaults
                resetBaseAttributes(living, oldRace);

                // Specific Player Reset (Flight, Hunger, etc.)
                if (living instanceof net.minecraft.world.entity.player.Player player) {
                    // Flight reset (Neutralize non-creative flight)
                    if (!player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                    // Hunger & Saturation restoration
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(20.0f);

                    // Close open menus
                    player.closeContainer();
                }

                // Restore health to full
                living.setHealth(living.getMaxHealth());
            }

            // 3. World Interactions
            entity.setNoGravity(false);

            // 4. Robust Scale Reset (Pehkui)
            resetScale(entity);

            // 4. Persistence cleanup
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                DataUtils.getPersistentData(player).putDouble("isRace", 0.0);
            }

        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Error performing total CreRaces reset", e);
        }
    }

    private static void resetBaseAttributes(LivingEntity living, double oldRace) {
        // TODO: Move these race-specific base overrides to a data-driven system in the
        // future
        // Slime Health Override (Race 19.x)
        if (Math.floor(oldRace) == 19.0) {
            setBase(living, Attributes.MAX_HEALTH, 20.0);
        }
        // Orc Damage/Defense Override (Race 16.0)
        else if (oldRace == 16.0) {
            setBase(living, Attributes.ATTACK_DAMAGE, 1.0);
            setBase(living, DEFENSE_ATTR, 1.0);
        }

        // Reset standard Ability/Resource Bases
        setBase(living, MAX_PASSIVE_CD_ATTR, 100.0);
        setBase(living, MAX_A1_CD_ATTR, 100.0);
        setBase(living, MAX_A2_CD_ATTR, 100.0);
        setBase(living, MAX_A3_CD_ATTR, 100.0);
        setBase(living, MAX_A4_CD_ATTR, 100.0);
        setBase(living, MAX_RACE_MANA_ATTR, 500.0);
        setBase(living, MAX_RACE_RAGE_ATTR, 100.0);
        setBase(living, MAX_RACE_ENERGY_ATTR, 200.0);
        setBase(living, MAX_RACE_GRIT_ATTR, 100.0);

        // Reset movement/physical bases to generic defaults
        setBase(living, Attributes.MOVEMENT_SPEED, 0.1);
        setBase(living, HEIGHT_ATTR, 1.0);
        setBase(living, WIDTH_ATTR, 1.0);
    }

    private static void setBase(LivingEntity living, net.minecraft.core.Holder<Attribute> attr, double value) {
        if (attr == null)
            return;
        AttributeInstance instance = living.getAttribute(attr);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static void resetScale(Entity entity) {
        // Use command for Pehkui reset to ensure compatibility with all scale types
        // (height, width, reach, etc.)
        if (!entity.level().isClientSide() && entity.getServer() != null) {
            String cmd = "scale reset @s";
            entity.getServer().getCommands()
                    .performPrefixedCommand(entity.createCommandSourceStack().withPermission(4), cmd);
        }
    }

    public static double getGState(Entity entity) {
        if (!isLoaded)
            return 0.0;
        try {
            // Use reflection for entity.getData()
            Object vars = null;
            try {
                Method getDataMethod = entity.getClass().getMethod("getData", Supplier.class);
                vars = getDataMethod.invoke(entity, playerVariablesAttachment);
            } catch (Exception e) {
            }

            if (vars != null)
                return (double) gStateField.get(vars);
        } catch (Exception e) {
        }
        return 0.0;
    }

    /**
     * Sets the race ID for the given entity and triggers sync/update.
     */
    public static void setRace(Entity entity, double raceId) {
        if (!isLoaded)
            return;

        try {
            // Use reflection for entity.getData()
            Object vars = null;
            try {
                Method getDataMethod = entity.getClass().getMethod("getData", Supplier.class);
                vars = getDataMethod.invoke(entity, playerVariablesAttachment);
            } catch (Exception e) {
            }

            if (vars == null)
                return;

            isRaceField.set(vars, raceId);
            raceUpdateField.set(vars, true);
            hasChoosenRaceField.set(vars, true);

            syncMethod.invoke(vars, entity);
            LOGGER.info("Time to change! Set CreRaces race to {} for {}", raceId, entity.getName().getString());
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Error setting CreRaces race", e);
        }
    }

    private static void removeCreRacesModifiers(LivingEntity entity) {
        removeAttributeModifier(entity, Attributes.BLOCK_INTERACTION_RANGE, "race_reach_block");
        removeAttributeModifier(entity, Attributes.ENTITY_INTERACTION_RANGE, "race_reach_entity");
        removeAttributeModifier(entity, Attributes.MAX_HEALTH, "race_health");
        removeAttributeModifier(entity, Attributes.LUCK, "race_luck");
        removeAttributeModifier(entity, Attributes.MOVEMENT_SPEED, "race_speed");
        removeAttributeModifier(entity, Attributes.MOVEMENT_SPEED, "race_speed_extra");
        removeAttributeModifier(entity, Attributes.ATTACK_DAMAGE, "race_attack");
        removeAttributeModifier(entity, Attributes.ARMOR_TOUGHNESS, "race_armor_toughness");
        removeAttributeModifier(entity, Attributes.ARMOR, "race_armor");
        removeAttributeModifier(entity, Attributes.ATTACK_SPEED, "race_attack_speed");
        removeAttributeModifier(entity, Attributes.ATTACK_KNOCKBACK, "race_knockback");
        removeAttributeModifier(entity, Attributes.STEP_HEIGHT, "race_step");

        // Dynamic attribute lookup for CreRaces custom attributes
        removeCreRacesCustomAttributeModifier(entity, "height", "race_height");
        removeCreRacesCustomAttributeModifier(entity, "height", "race_height_extra");
        removeCreRacesCustomAttributeModifier(entity, "width", "race_width");
        removeCreRacesCustomAttributeModifier(entity, "width", "race_width_extra");
        removeCreRacesCustomAttributeModifier(entity, "defense", "race_defense");
        removeCreRacesCustomAttributeModifier(entity, "mining_speed", "race_mining");
        removeCreRacesCustomAttributeModifier(entity, "fall_speed", "race_fall");
        removeCreRacesCustomAttributeModifier(entity, "jump_height", "race_jump");
    }

    private static void removeAttributeModifier(LivingEntity entity, net.minecraft.core.Holder<Attribute> attribute,
            String modifierName) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(ResourceLocation.fromNamespaceAndPath(CRERACES_MODID, modifierName));
        }
    }

    private static void removeCreRacesCustomAttributeModifier(LivingEntity entity, String attributeName,
            String modifierName) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(CRERACES_MODID, attributeName);
        BuiltInRegistries.ATTRIBUTE.getHolder(rl).ifPresent(holder -> {
            AttributeInstance instance = entity.getAttribute(holder);
            if (instance != null) {
                instance.removeModifier(ResourceLocation.fromNamespaceAndPath(CRERACES_MODID, modifierName));
            }
        });
    }
}
