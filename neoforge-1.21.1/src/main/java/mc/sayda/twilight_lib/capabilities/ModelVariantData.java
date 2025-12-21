package mc.sayda.twilight_lib.capabilities;

import net.minecraft.nbt.CompoundTag;

/**
 * Implementation of IModelVariant for storing player model variant.
 *
 * <p>Thread-safe implementation using synchronized methods.
 */
public class ModelVariantData implements IModelVariant {
    private String modelVariant = null;  // null = use default (skin-based)

    @Override
    public synchronized String getModelVariant() {
        return modelVariant != null ? modelVariant : "steve";  // Default to steve if not set
    }

    @Override
    public synchronized void setModelVariant(String variant) {
        if (variant == null) {
            this.modelVariant = null;
            return;
        }

        // Validate length before normalizing (prevent log injection and memory issues)
        if (variant.length() > 32) {
            throw new IllegalArgumentException("Model variant too long: " + variant.length() + " chars (max 32)");
        }

        // Normalize to lowercase
        String normalized = variant.toLowerCase();

        // Validate - only accept "steve" or "alex"
        if (!normalized.equals("steve") && !normalized.equals("alex")) {
            throw new IllegalArgumentException("Model variant must be 'steve' or 'alex', got: " + normalized);
        }

        this.modelVariant = normalized;
    }

    @Override
    public synchronized boolean hasCustomVariant() {
        return modelVariant != null;
    }

    @Override
    public synchronized void clearCustomVariant() {
        this.modelVariant = null;
    }

    @Override
    public synchronized CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        if (modelVariant != null) {
            tag.putString("modelVariant", modelVariant);
        }
        return tag;
    }

    @Override
    public synchronized void deserialize(CompoundTag tag) {
        if (tag.contains("modelVariant")) {
            this.modelVariant = tag.getString("modelVariant");
        } else {
            this.modelVariant = null;
        }
    }
}