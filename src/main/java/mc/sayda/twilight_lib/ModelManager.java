package mc.sayda.twilight_lib;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ModelManager {
    // Maps model names (in lowercase) to suppliers producing an EntityModel<AbstractClientPlayer>
    public static final Map<String, Supplier<Supplier<EntityModel<AbstractClientPlayer>>>> MODEL_REGISTRY = new HashMap<>();

    // Maps a player's UUID to the chosen model name; kept private for encapsulation.
    private static final Map<UUID, String> PLAYER_MODELS = new HashMap<>();

    public static void registerModel(String name, Supplier<Supplier<EntityModel<AbstractClientPlayer>>> modelSupplier) {
        String lowerName = name.toLowerCase();
        MODEL_REGISTRY.put(lowerName, modelSupplier);
        TwilightLib.LOGGER.info("ModelManager: Registered model with key '{}'", lowerName);
    }

    public static boolean hasModel(String name) {
        boolean result = MODEL_REGISTRY.containsKey(name.toLowerCase());
        TwilightLib.LOGGER.debug("ModelManager: hasModel('{}') returns {}", name, result);
        return result;
    }

    public static void setPlayerModel(ServerPlayer player, String name) {
        updatePlayerModel(player.getUUID(), name);
        TwilightLib.LOGGER.info("ModelManager: Set model '{}' for player {}", name, player.getName().getString());
    }

    public static String getPlayerModel(UUID playerId) {
        String model = PLAYER_MODELS.get(playerId);
        TwilightLib.LOGGER.debug("ModelManager: getPlayerModel({}) returns {}", playerId, model);
        return model;
    }

    public static void removePlayerModel(ServerPlayer player) {
        PLAYER_MODELS.remove(player.getUUID());
        TwilightLib.LOGGER.info("ModelManager: Removed model for player {}", player.getName().getString());
    }

    // Public accessor method for updating the mapping.
    public static void updatePlayerModel(UUID playerId, String model) {
        PLAYER_MODELS.put(playerId, model.toLowerCase());
        TwilightLib.LOGGER.info("ModelManager: Updated model for player {} to {}", playerId, model);
    }
}
