package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.DataUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.Optional;

public class NetworkHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void registerS2C() {
        // Register client-bound packets
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncMorphPacket.ID, (buf, context) -> {
            var pkt = new SyncMorphPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncAddonsPacket.ID, (buf, context) -> {
            var pkt = new SyncAddonsPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncTrailsPacket.ID, (buf, context) -> {
            var pkt = new SyncTrailsPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncEffectsPacket.ID, (buf, context) -> {
            var pkt = new SyncEffectsPacket(buf);
            pkt.handle(() -> context);
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SyncModelVariantPacket.ID, (buf, context) -> {
            var pkt = new SyncModelVariantPacket(buf);
            pkt.handle(() -> context);
        });

        LOGGER.info("Twilight Lib: Client network payloads registered.");
    }

    public static void sendMorphToAll(SyncMorphPacket pkt) {
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, SyncMorphPacket.ID, pkt::encode);
            }
        }
    }

    // Helper to avoid repetitive buffer creation
    private static void send(ServerPlayer player, net.minecraft.resources.ResourceLocation id,
            java.util.function.Consumer<net.minecraft.network.FriendlyByteBuf> encoder) {
        net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer());
        encoder.accept(buf);
        NetworkManager.sendToPlayer(player, id, buf);
    }

    public static void sendToPlayer(Player player, SyncMorphPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            send(sp, SyncMorphPacket.ID, pkt::encode);
        }
    }

    public static void sendAllMorphsToPlayer(Player recipient) {
        if (recipient.level() == null)
            return;
        for (Player p : recipient.level().players()) {
            IMorph morph = DataUtils.getMorphData(p);
            if (morph != null) {
                morph.getEntityType().ifPresent(rl -> sendToPlayer(recipient,
                        SyncMorphPacket.of(p.getUUID(), Optional.of(rl), morph.isNametagHidden())));
            }
        }
    }

    // Repeat for other types... (omitted for brevity here, but I'll implement them)
    public static void sendAddonsToAll(SyncAddonsPacket pkt) {
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, SyncAddonsPacket.ID, pkt::encode);
            }
        }
    }

    public static void sendAddonsToPlayer(Player player, SyncAddonsPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            send(sp, SyncAddonsPacket.ID, pkt::encode);
        }
    }

    public static void sendAllAddonsToPlayer(Player recipient) {
        if (recipient.level() == null)
            return;
        for (Player p : recipient.level().players()) {
            var addons = DataUtils.getAddonsData(p);
            if (addons != null && !addons.getActiveAddons().isEmpty()) {
                sendAddonsToPlayer(recipient,
                        new SyncAddonsPacket(p.getUUID(), addons.getActiveAddons(), addons.getAllAddonTints()));
            }
        }
    }

    public static void sendTrailsToAll(SyncTrailsPacket pkt) {
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, SyncTrailsPacket.ID, pkt::encode);
            }
        }
    }

    public static void sendTrailsToPlayer(Player player, SyncTrailsPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            send(sp, SyncTrailsPacket.ID, pkt::encode);
        }
    }

    public static void sendAllTrailsToPlayer(Player recipient) {
        if (recipient.level() == null)
            return;
        for (Player p : recipient.level().players()) {
            var trails = DataUtils.getTrailsData(p);
            if (trails != null && !trails.getActiveTrails().isEmpty()) {
                sendTrailsToPlayer(recipient, new SyncTrailsPacket(p.getUUID(), trails.getActiveTrails()));
            }
        }
    }

    public static void sendEffectsToAll(SyncEffectsPacket pkt) {
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, SyncEffectsPacket.ID, pkt::encode);
            }
        }
    }

    public static void sendEffectsToPlayer(Player player, SyncEffectsPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            send(sp, SyncEffectsPacket.ID, pkt::encode);
        }
    }

    public static void sendAllEffectsToPlayer(Player recipient) {
        if (recipient.level() == null)
            return;
        for (Player p : recipient.level().players()) {
            var effects = DataUtils.getEffectsData(p);
            if (effects != null && !effects.getActiveEffects().isEmpty()) {
                sendEffectsToPlayer(recipient, new SyncEffectsPacket(p.getUUID(), effects.getActiveEffects(), false));
            }
        }
    }

    public static void sendModelVariantToAll(SyncModelVariantPacket pkt) {
        var server = dev.architectury.utils.GameInstance.getServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                send(player, SyncModelVariantPacket.ID, pkt::encode);
            }
        }
    }

    public static void sendModelVariantToPlayer(Player player, SyncModelVariantPacket pkt) {
        if (player instanceof ServerPlayer sp) {
            send(sp, SyncModelVariantPacket.ID, pkt::encode);
        }
    }

    public static void sendAllModelVariantsToPlayer(Player recipient) {
        if (recipient.level() == null)
            return;
        for (Player p : recipient.level().players()) {
            var mv = DataUtils.getModelVariantData(p);
            if (mv != null) {
                sendModelVariantToPlayer(recipient, SyncModelVariantPacket.of(p.getUUID(), mv));
            }
        }
    }
}
