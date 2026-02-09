package mc.sayda.twilight_lib.client.model;

import net.minecraft.client.model.geom.ModelPart;

import java.util.Optional;

/**
 * Interface for addon models to expose their body parts for syncing with player model.
 * Body parts return Optional to gracefully handle missing parts - most addons won't use all parts.
 */
public interface IAddonModel {
    default Optional<ModelPart> getHead() { return Optional.empty(); }
    default Optional<ModelPart> getBody() { return Optional.empty(); }
    default Optional<ModelPart> getRightArm() { return Optional.empty(); }
    default Optional<ModelPart> getLeftArm() { return Optional.empty(); }
    default Optional<ModelPart> getRightLeg() { return Optional.empty(); }
    default Optional<ModelPart> getLeftLeg() { return Optional.empty(); }
}
