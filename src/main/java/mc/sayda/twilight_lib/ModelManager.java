package mc.sayda.twilight_lib;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.model.EntityModel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ModelManager {
    // A registry mapping model names to their respective supplier factory.
    // Note: The Supplier returns another Supplier so that instantiation can be deferred until needed.
    public static final Map<String, Supplier<Supplier<EntityModel<Player>>>> MODEL_REGISTRY = new HashMap<>();

    // Mapping of players (by UUID) to the selected model name.
    private static final Map<UUID, String> PLAYER_MODELS = new HashMap<>();

    public static void registerModel(String name, Supplier<Supplier<EntityModel<Player>>> modelSupplier) {
        MODEL_REGISTRY.put(name.toLowerCase(), modelSupplier);
    }

    public static boolean hasModel(String name) {
        return MODEL_REGISTRY.containsKey(name.toLowerCase());
    }

    public static void setPlayerModel(ServerPlayer player, String name) {
        PLAYER_MODELS.put(player.getUUID(), name.toLowerCase());
    }

    public static void removePlayerModel(ServerPlayer player) {
        PLAYER_MODELS.remove(player.getUUID());
    }

    public static String getPlayerModel(UUID playerId) {
        return PLAYER_MODELS.get(playerId);
    }
}
