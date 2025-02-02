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

    // Maps a player's UUID to the chosen model name
    private static final Map<UUID, String> PLAYER_MODELS = new HashMap<>();

    public static void registerModel(String name, Supplier<Supplier<EntityModel<AbstractClientPlayer>>> modelSupplier) {
        MODEL_REGISTRY.put(name.toLowerCase(), modelSupplier);
    }

    public static boolean hasModel(String name) {
        return MODEL_REGISTRY.containsKey(name.toLowerCase());
    }

    public static void setPlayerModel(ServerPlayer player, String name) {
        PLAYER_MODELS.put(player.getUUID(), name.toLowerCase());
    }

    public static String getPlayerModel(UUID playerId) {
        return PLAYER_MODELS.get(playerId);
    }

    public static void removePlayerModel(ServerPlayer player) {
        PLAYER_MODELS.remove(player.getUUID());
    }
}
