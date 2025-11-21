package mc.sayda.twilight_lib.network;

import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.capabilities.IMorph;
import mc.sayda.twilight_lib.capabilities.MorphProvider;
import mc.sayda.twilight_lib.capabilities.AddonsProvider;
import mc.sayda.twilight_lib.capabilities.TrailsProvider;
import mc.sayda.twilight_lib.capabilities.EffectsProvider;
import mc.sayda.twilight_lib.capabilities.ModelVariantProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Network packet handler for Twilight Lib - manages client-server synchronization of cosmetics.
 *
 * <p><b>Packet Flow</b>: Server-to-Client only (PLAY_TO_CLIENT direction).
 * <ul>
 *   <li>Server tracks player cosmetic state (authoritative)</li>
 *   <li>Clients receive packets to render cosmetics on other players</li>
 *   <li>No client-to-server packets (prevents cheating/spoofing)</li>
 * </ul>
 *
 * <p><b>Registered Packets</b>:
 * <ul>
 *   <li>{@link SyncMorphPacket}: Syncs a player's active morph to clients</li>
 *   <li>{@link SyncAddonsPacket}: Syncs a player's active addons to clients</li>
 *   <li>{@link SyncTrailsPacket}: Syncs a player's trail configuration to clients</li>
 *   <li>{@link SyncEffectsPacket}: Syncs a player's active effects to clients</li>
 *   <li>{@link SyncModelVariantPacket}: Syncs a player's model variant (Steve/Alex) to clients</li>
 * </ul>
 *
 * <p><b>Protocol Versioning</b>: Protocol version "1" is hardcoded.
 * If packet structure changes, increment PROTOCOL to prevent version mismatches.
 * Mismatched protocol versions will prevent clients from joining.
 *
 * <p><b>Thread Safety</b>: All methods are called from server tick thread.
 * Packet sending is thread-safe via Forge's network system.
 *
 * @author Sayda (MrJojo)
 * @version 1.0
 */
public class NetworkHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Network protocol version.
     *
     * <p><b>IMPORTANT</b>: Increment this when packet structure changes!
     * Mismatched versions will prevent mod from working across client-server.
     */
    private static final String PROTOCOL = "1";

    /**
     * Forge SimpleChannel for packet transmission.
     *
     * <p>Channel name: "twilight_lib:main" (prevents conflicts with other mods).
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TwilightLib.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    /**
     * Packet discriminator index (auto-incremented for each packet type).
     *
     * <p><b>Why track index?</b> Forge requires unique IDs for each packet type.
     * Auto-incrementing prevents ID collisions.
     */
    private static int index = 0;

    /**
     * Initializes the network channel and registers all packet types.
     *
     * <p><b>Registration Order</b>: Order matters! Discriminator IDs must match
     * between client and server. Changing order requires protocol version bump.
     *
     * <p>Called once during mod construction in {@link TwilightLib#TwilightLib()}.
     */
    public static void init() {
        CHANNEL.registerMessage(
                index++, SyncMorphPacket.class,
                SyncMorphPacket::encode, SyncMorphPacket::decode, SyncMorphPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                index++, SyncAddonsPacket.class,
                SyncAddonsPacket::encode, SyncAddonsPacket::decode, SyncAddonsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                index++, SyncTrailsPacket.class,
                SyncTrailsPacket::encode, SyncTrailsPacket::decode, SyncTrailsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                index++, SyncEffectsPacket.class,
                SyncEffectsPacket::encode, SyncEffectsPacket::decode, SyncEffectsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                index++, SyncModelVariantPacket.class,
                SyncModelVariantPacket::encode, SyncModelVariantPacket::decode, SyncModelVariantPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        LOGGER.debug("This is the precipice of a new reality! Network channel initialized.");
    }

    public static void sendMorphToAll(SyncMorphPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("Here you go! Sending morph packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send morph packet to all players", e);
        }
    }

    public static void sendToPlayer(Player player, SyncMorphPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
            LOGGER.debug("Here you go! Sending morph to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send morph to {}", player.getGameProfile().getName(), e);
        }
    }

    /**
     * Sends all players' morphs to a specific recipient (used during delayed login sync).
     *
     * <p><b>When called</b>: After a player logs in, once their client is ready (delayed by ticks).
     * This ensures the recipient sees ALL other players' morphs, even if they were already online.
     *
     * <p><b>Why iterate all players?</b> During login, the player tracking system might not
     * have sent all nearby players yet. This ensures complete cosmetic state.
     *
     * <p><b>Performance</b>: Only sends non-empty morphs. Players without morphs are skipped.
     *
     * @param recipient The player who should receive all morph packets
     */
    public static void sendAllMorphsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Want to see something neat? Syncing all morphs to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            LazyOptional<IMorph> cap = p.getCapability(MorphProvider.MORPH_CAP);
            cap.ifPresent(m -> m.getEntityType().ifPresent(rl ->
                sendToPlayer(recipient, SyncMorphPacket.of(p.getUUID(), Optional.of(rl), m.isNametagHidden()))
            ));
        }
    }

    // Addon packet methods
    public static void sendAddonsToAll(SyncAddonsPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("Here you go! Sending addons packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send addons packet to all players", e);
        }
    }

    public static void sendAddonsToPlayer(Player player, SyncAddonsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
            LOGGER.debug("Here you go! Sending addons to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send addons to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllAddonsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Every day, every season... ends. And begin something new! Syncing all addons to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            p.getCapability(AddonsProvider.ADDONS_CAP).ifPresent(addons -> {
                if (!addons.getActiveAddons().isEmpty()) {
                    sendAddonsToPlayer(recipient, new SyncAddonsPacket(p.getUUID(), addons.getActiveAddons()));
                }
            });
        }
    }

    // Trails packet methods
    public static void sendTrailsToAll(SyncTrailsPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("Here you go! Sending trails packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send trails packet to all players", e);
        }
    }

    public static void sendTrailsToPlayer(Player player, SyncTrailsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
            LOGGER.debug("Here you go! Sending trails to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send trails to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllTrailsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("I wanna wanna, go to some place, place place! Syncing all trails to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            p.getCapability(TrailsProvider.TRAILS_CAP).ifPresent(trails -> {
                if (!trails.getActiveTrails().isEmpty()) {
                    sendTrailsToPlayer(recipient, new SyncTrailsPacket(p.getUUID(), trails.getActiveTrails()));
                }
            });
        }
    }

    // Effects packet methods
    public static void sendEffectsToAll(SyncEffectsPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("Here you go! Sending effects packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send effects packet to all players", e);
        }
    }

    public static void sendEffectsToPlayer(Player player, SyncEffectsPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
            LOGGER.debug("Here you go! Sending effects to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send effects to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllEffectsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Aw, this spell is neat! Syncing all effects to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            p.getCapability(EffectsProvider.EFFECTS_CAP).ifPresent(effects -> {
                if (!effects.getActiveEffects().isEmpty()) {
                    sendEffectsToPlayer(recipient, new SyncEffectsPacket(p.getUUID(), effects.getActiveEffects()));
                }
            });
        }
    }

    // Model Variant packet methods
    public static void sendModelVariantToAll(SyncModelVariantPacket pkt) {
        try {
            CHANNEL.send(PacketDistributor.ALL.noArg(), pkt);
            LOGGER.debug("Here you go! Sending model variant packet to all players.");
        } catch (Exception e) {
            LOGGER.error("How did I?! Uuuughh! Failed to send model variant packet to all players", e);
        }
    }

    public static void sendModelVariantToPlayer(Player player, SyncModelVariantPacket pkt) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), pkt);
            LOGGER.debug("Here you go! Sending model variant to {}", player.getGameProfile().getName());
        } catch (Exception e) {
            LOGGER.warn("How did I?! Uuuughh! Failed to send model variant to {}", player.getGameProfile().getName(), e);
        }
    }

    public static void sendAllModelVariantsToPlayer(Player recipient) {
        if (recipient.level() == null) return;
        LOGGER.debug("Let's see all the different forms! Syncing all model variants to {}", recipient.getGameProfile().getName());
        for (Player p : recipient.level().players()) {
            if (p.level() == null) continue; // Skip players with null level (mid-disconnect)
            p.getCapability(ModelVariantProvider.MODEL_VARIANT_CAP).ifPresent(modelVariant -> {
                sendModelVariantToPlayer(recipient, SyncModelVariantPacket.of(p.getUUID(), modelVariant));
            });
        }
    }
}
