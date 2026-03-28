package mc.sayda.twilight_lib.cosmetics;

import mc.sayda.twilight_lib.TwilightConstants;
import mc.sayda.twilight_lib.capabilities.*;
import mc.sayda.twilight_lib.network.*;
import mc.sayda.twilight_lib.supporter.SupporterData;
import mc.sayda.twilight_lib.supporter.SupporterService;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Unified manager for granting and syncing player cosmetics.
 * Consolidates logic previously duplicated in TwilightLib and
 * TwilightLibCommands.
 */
public class CosmeticManager {

    /**
     * Re-syncs all cosmetics for a player, including supporter grants and network
     * packets.
     * 
     * @param player The player to sync
     */
    /**
     * Re-syncs all cosmetics for a player.
     *
     * @param player              The player to sync
     * @param triggerSpawnEffects Whether to trigger spawn effects
     *                            (particles/sounds)
     */
    public static void resyncAll(ServerPlayer player, boolean triggerSpawnEffects) {
        if (player == null || player.isRemoved())
            return;

        // 1. Process Supporter Grants
        applySupporterGrants(player);

        // 2. Sync Morph
        IMorph morph = DataUtils.getMorphData(player);
        if (morph != null) {
            NetworkHandler.sendMorphToAll(
                    SyncMorphPacket.of(player.getUUID(), morph.getEntityType(), morph.isNametagHidden()));
            DataUtils.getPersistentData(player).put(TwilightConstants.NBT_MORPH, morph.serialize());
        }

        // 3. Sync Addons
        IAddons addons = DataUtils.getAddonsData(player);
        if (addons != null) {
            NetworkHandler.sendAddonsToAll(
                    new SyncAddonsPacket(player.getUUID(), addons.getActiveAddons(), addons.getExternalGrants(), addons.getAllAddonTints()));
            DataUtils.getPersistentData(player).put(TwilightConstants.NBT_ADDONS, addons.serialize());
        }

        // 4. Sync Trails
        ITrails trails = DataUtils.getTrailsData(player);
        if (trails != null) {
            NetworkHandler.sendTrailsToAll(new SyncTrailsPacket(player.getUUID(), trails.getActiveTrails()));
            DataUtils.getPersistentData(player).put(TwilightConstants.NBT_TRAILS, trails.serialize());
        }

        // 5. Sync Effects
        IEffects effects = DataUtils.getEffectsData(player);
        if (effects != null) {
            NetworkHandler.sendEffectsToAll(
                    new SyncEffectsPacket(player.getUUID(), effects.getActiveEffects(), triggerSpawnEffects));
            DataUtils.getPersistentData(player).put(TwilightConstants.NBT_EFFECTS, effects.serialize());
        }

        // 6. Sync Model Variant
        IModelVariant modelVariant = DataUtils.getModelVariantData(player);
        if (modelVariant != null) {
            NetworkHandler.sendModelVariantToAll(SyncModelVariantPacket.of(player.getUUID(), modelVariant));
            DataUtils.getPersistentData(player).put(TwilightConstants.NBT_MODEL_VARIANT, modelVariant.serialize());
        }

        // 7. Sync All Others to Self (Initial login catch-up)
        NetworkHandler.sendAllMorphsToPlayer(player);
        NetworkHandler.sendAllAddonsToPlayer(player);
        NetworkHandler.sendAllTrailsToPlayer(player);
        NetworkHandler.sendAllEffectsToPlayer(player);
        NetworkHandler.sendAllModelVariantsToPlayer(player);
    }

    public static void resyncAll(ServerPlayer player) {
        resyncAll(player, true);
    }

    /**
     * Checks supporter status and grants/revokes cosmetics based on their
     * tier/manual grants.
     */
    public static void applySupporterGrants(ServerPlayer player) {
        String uuid = player.getStringUUID();
        Optional<SupporterData> supporterData = SupporterService.getSupporterData(uuid);

        if (supporterData.isPresent()) {
            SupporterData data = supporterData.get();
            Set<String> allTrails = data.getAllTrails();
            Set<String> allAddons = data.getAllAddons();
            Set<String> allEffects = data.getAllEffects();

            // Trails
            ITrails trails = DataUtils.getTrailsData(player);
            if (trails instanceof TrailsData trailsData) {
                Set<String> currentOwned = new HashSet<>(trails.getTrails());
                currentOwned.stream().filter(t -> !allTrails.contains(t)).forEach(trailsData::removeTrailOwnership);
                allTrails.stream().filter(t -> !currentOwned.contains(t)).forEach(trails::addTrail);
            }

            // Addons
            IAddons addons = DataUtils.getAddonsData(player);
            if (addons instanceof AddonsData addonsData) {
                Set<String> currentOwned = new HashSet<>(addons.getAddons());
                currentOwned.stream().filter(a -> !allAddons.contains(a)).forEach(addonsData::removeAddonOwnership);
                allAddons.stream().filter(a -> !currentOwned.contains(a)).forEach(addons::addAddon);
            }

            // Effects
            IEffects effects = DataUtils.getEffectsData(player);
            if (effects instanceof EffectsData effectsData) {
                Set<String> currentOwned = new HashSet<>(effects.getEffects());
                currentOwned.stream().filter(e -> !allEffects.contains(e)).forEach(effectsData::removeEffectOwnership);
                allEffects.stream().filter(e -> !currentOwned.contains(e)).forEach(effects::addEffect);
            }
        }
    }
}
