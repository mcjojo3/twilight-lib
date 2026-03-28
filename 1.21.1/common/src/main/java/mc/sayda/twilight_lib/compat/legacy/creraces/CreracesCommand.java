package mc.sayda.twilight_lib.compat.legacy.creraces;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mc.sayda.twilight_lib.capabilities.DataUtils;

import net.minecraft.world.entity.ai.attributes.Attribute;
import dev.architectury.registry.registries.RegistrySupplier;
// import mc.sayda.twilight_lib.compat.CreRacesInterop; // Removed redundant import
import mc.sayda.twilight_lib.network.NetworkHandler;
import mc.sayda.twilight_lib.network.SyncAddonsPacket;
import mc.sayda.twilight_lib.network.SyncModelVariantPacket;
import mc.sayda.twilight_lib.TwilightConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.Optional;

public class CreracesCommand {
    private static final Random RANDOM = new Random();

    // Mapping of lowercase underscored names to numeric IDs
    private static final Map<String, Double> NAME_TO_ID = new LinkedHashMap<>();
    private static final Map<Double, String> ID_TO_NAME = new HashMap<>();

    static {
        registerRace("human", 0.0);
        registerRace("undead", 1.0);
        registerRace("dwarf", 2.0);
        registerRace("fire_dragonborn", 3.1);
        registerRace("water_dragonborn", 3.2);
        registerRace("earth_dragonborn", 3.3);
        registerRace("air_dragonborn", 3.4);
        registerRace("harpy", 4.0);
        registerRace("fairy", 5.0);
        registerRace("day_fairy", 5.1);
        registerRace("night_fairy", 5.2);
        registerRace("spring_fairy", 5.3);
        registerRace("summer_fairy", 5.4);
        registerRace("autumn_fairy", 5.5);
        registerRace("winter_fairy", 5.6);
        registerRace("mermaid", 6.0);
        registerRace("axolotl", 6.6);
        registerRace("elementalist", 7.0);
        registerRace("golem", 8.0);
        registerRace("oread", 9.1);
        registerRace("naiad", 9.2);
        registerRace("dryad", 9.3);
        registerRace("aurai", 9.4);
        registerRace("lycan", 10.0);
        registerRace("giant", 11.0);
        registerRace("elf", 12.1);
        registerRace("velox", 12.2);
        registerRace("ratkin", 13.0);
        registerRace("pixie", 14.1);
        registerRace("troll", 15.0);
        registerRace("orc", 16.0);
        registerRace("kitsune", 17.0);
        registerRace("slime", 19.0);
        registerRace("goblin", 20.0);
    }

    private static void registerRace(String name, double id) {
        NAME_TO_ID.put(name, id);
        ID_TO_NAME.put(id, name);
    }

    private static final SuggestionProvider<CommandSourceStack> RACE_ID_SUGGESTIONS = (context,
            builder) -> SharedSuggestionProvider.suggest(NAME_TO_ID.keySet(), builder);

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("creraces")
                        .requires(src -> src.hasPermission(2))

                        // help
                        .then(Commands.literal("help")
                                .executes(ctx -> executeHelp(ctx.getSource())))

                        // reset <target>
                        .then(Commands.literal("reset")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> executeReset(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"))))
                                .executes(ctx -> {
                                    ServerPlayer target = ctx.getSource().getPlayer();
                                    if (target == null) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("Target required for non-player execution."));
                                        return 0;
                                    }
                                    return executeReset(ctx.getSource(), target);
                                }))

                        // setrace <target> <race>
                        .then(Commands.literal("setrace")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("race", StringArgumentType.word())
                                                .suggests(RACE_ID_SUGGESTIONS)
                                                .executes(ctx -> executeSetRace(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "race"))))))

                        // setrandom <target>
                        .then(Commands.literal("setrandom")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> executeSetRandom(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"))))
                                .executes(ctx -> {
                                    ServerPlayer target = ctx.getSource().getPlayer();
                                    if (target == null) {
                                        ctx.getSource().sendFailure(
                                                Component.literal("Target required for non-player execution."));
                                        return 0;
                                    }
                                    return executeSetRandom(ctx.getSource(), target);
                                })));
    }

    private static int executeHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("--- CreRaces Modernized Guide ---"), false);
        source.sendSuccess(() -> Component.literal("- /creraces reset <player> ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("(Resets race & attributes)").withStyle(ChatFormatting.DARK_GRAY)), false);
        source.sendSuccess(
                () -> Component.literal("- /creraces setrace <player> <name/ID> ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("(Sets specific race)").withStyle(ChatFormatting.DARK_GRAY)),
                false);
        source.sendSuccess(() -> Component.literal("- /creraces setrandom <player> ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("(Assigns a random race)").withStyle(ChatFormatting.DARK_GRAY)), false);
        source.sendSuccess(() -> Component.literal("- /creraces help ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("(Shows this list)").withStyle(ChatFormatting.DARK_GRAY)), false);
        return 1;
    }

    private static int executeReset(CommandSourceStack source, ServerPlayer target) {
        if (!CreRacesInterop.isLoaded()) {
            source.sendFailure(Component.literal("CreRaces is not loaded!"));
            return 0;
        }

        performFullReset(target);

        source.sendSuccess(() -> Component.literal("Reset race for ")
                .append(Component.literal(target.getGameProfile().getName())), true);
        return 1;
    }

    /**
     * Performs a comprehensive reset of all CreRaces and Twilight Lib player state.
     * Centralizes the logic to ensure consistency between manual resets and race
     * changes.
     */
    private static void performFullReset(ServerPlayer target) {
        // Check gState before reset for specific logic replication
        boolean wasGState1 = CreRacesInterop.getGState(target) == 1.0;

        // 1. Reset CreRaces Variables & Attributes (Reflection)
        CreRacesInterop.resetRace(target);

        // 2. Clear Addons & Tints
        var addons = DataUtils.getAddonsData(target);
        if (addons != null) {
            addons.clearActiveAddons();
            // Reset tints to FFFFFF (White)
            addons.getAllAddonTints().keySet().forEach(slot -> addons.setAddonTint(slot, 0xFFFFFF));

            DataUtils.getPersistentData(target).put(TwilightConstants.NBT_ADDONS, addons.serialize());
            NetworkHandler.sendAddonsToAll(
                    new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons(), addons.getExternalGrants(), addons.getAllAddonTints()));
        }

        // 3. Clear Model Variant
        var modelVariant = DataUtils.getModelVariantData(target);
        if (modelVariant != null) {
            modelVariant.setVariant(Optional.empty());
            DataUtils.getPersistentData(target).remove(TwilightConstants.NBT_MODEL_VARIANT);
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(target.getUUID(), modelVariant));
        }

        // 4. Unmorph
        var morph = DataUtils.getMorphData(target);
        if (morph != null && morph.getEntityType().isPresent()) {
            morph.setEntityType(Optional.empty());
        }

        // 5. Reset Twilight Lib Attributes
        setAttr(target, mc.sayda.twilight_lib.ModAttributes.MINING_PENALTY, 1.0);
        setAttr(target, mc.sayda.twilight_lib.ModAttributes.FOV_MODIFIER, 1.0);
        setAttr(target, mc.sayda.twilight_lib.ModAttributes.ALLOW_HELMET, 1.0);
        setAttr(target, mc.sayda.twilight_lib.ModAttributes.ALLOW_CHESTPLATE, 1.0);
        setAttr(target, mc.sayda.twilight_lib.ModAttributes.ALLOW_LEGGINGS, 1.0);
        setAttr(target, mc.sayda.twilight_lib.ModAttributes.ALLOW_BOOTS, 1.0);
        setAttr(target, mc.sayda.twilight_lib.ModAttributes.ELYTRA_FLIGHT, 0.0);

        // 6. Special GState Logic (Steve/Alex & Chest Addon)
        if (wasGState1) {
            // Re-apply specific model and addon if it was previously in gState 1
            if (modelVariant != null) {
                modelVariant.setModelVariant("alex");
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_MODEL_VARIANT, modelVariant.serialize());
                NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(target.getUUID(), modelVariant));
            }
            if (addons != null) {
                addons.setActiveAddon("chest", true);
                DataUtils.getPersistentData(target).put(TwilightConstants.NBT_ADDONS, addons.serialize());
                NetworkHandler.sendAddonsToAll(
                        new SyncAddonsPacket(target.getUUID(), addons.getActiveAddons(), addons.getExternalGrants(), addons.getAllAddonTints()));
            }
        }
    }

    private static void setAttr(ServerPlayer player,
            RegistrySupplier<Attribute> holder,
            double value) {
        var inst = player.getAttribute(mc.sayda.twilight_lib.ModAttributes.getHolder(holder));
        if (inst != null) {
            inst.setBaseValue(value);
        }
    }

    private static int executeSetRace(CommandSourceStack source, ServerPlayer target, String raceInput) {
        if (!CreRacesInterop.isLoaded()) {
            source.sendFailure(Component.literal("CreRaces is not loaded!"));
            return 0;
        }

        double raceId;
        String raceName;

        // Try lookup by name
        if (NAME_TO_ID.containsKey(raceInput.toLowerCase())) {
            raceId = NAME_TO_ID.get(raceInput.toLowerCase());
            raceName = raceInput.toLowerCase();
        } else {
            // Try parse as ID
            try {
                raceId = Double.parseDouble(raceInput);
                if (!ID_TO_NAME.containsKey(raceId)) {
                    source.sendFailure(
                            Component.literal("Invalid Race ID: " + raceInput).withStyle(ChatFormatting.RED));
                    return 0;
                }
                raceName = ID_TO_NAME.get(raceId);
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("Unknown race: " + raceInput).withStyle(ChatFormatting.RED));
                return 0;
            }
        }

        // CRITICAL: Perform full reset before setting the new race to ensure clean
        // transitions and that attributes/cosmetics are properly handled.
        performFullReset(target);
        CreRacesInterop.setRace(target, raceId);

        source.sendSuccess(() -> Component.literal("Set race to ")
                .append(Component.literal(raceName))
                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(raceId)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" for "))
                .append(Component.literal(target.getGameProfile().getName())), true);
        return 1;
    }

    private static int executeSetRandom(CommandSourceStack source, ServerPlayer target) {
        if (!CreRacesInterop.isLoaded()) {
            source.sendFailure(Component.literal("CreRaces is not loaded!"));
            return 0;
        }

        List<String> names = new ArrayList<>(NAME_TO_ID.keySet());
        String randomName = names.get(RANDOM.nextInt(names.size()));
        return executeSetRace(source, target, randomName);
    }
}
