package mc.sayda.twilight_lib.capabilities;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge 1.21.1 Data Attachments (replaces Forge Capabilities)
 * Attachments are the new way to attach data to entities, chunks, and other holders.
 */
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TwilightLib.MODID);

    /**
     * Morph attachment - stores player's morphed entity type
     */
    public static final Supplier<AttachmentType<MorphData>> MORPH = ATTACHMENT_TYPES.register(
        "morph",
        () -> AttachmentType.builder(() -> new MorphData())
            .serialize(new DataSerializer<>(MorphData::new))
            .copyOnDeath() // Preserve morph on death
            .build()
    );

    /**
     * Addons attachment - stores player's cosmetic addons (tails, wings, etc.)
     */
    public static final Supplier<AttachmentType<AddonsData>> ADDONS = ATTACHMENT_TYPES.register(
        "addons",
        () -> AttachmentType.builder(() -> new AddonsData())
            .serialize(new DataSerializer<>(AddonsData::new))
            .copyOnDeath() // Preserve addons on death
            .build()
    );

    /**
     * Trails attachment - stores player's particle trails
     */
    public static final Supplier<AttachmentType<TrailsData>> TRAILS = ATTACHMENT_TYPES.register(
        "trails",
        () -> AttachmentType.builder(() -> new TrailsData())
            .serialize(new DataSerializer<>(TrailsData::new))
            .copyOnDeath() // Preserve trails on death
            .build()
    );

    /**
     * Effects attachment - stores player's cosmetic effects
     */
    public static final Supplier<AttachmentType<EffectsData>> EFFECTS = ATTACHMENT_TYPES.register(
        "effects",
        () -> AttachmentType.builder(() -> new EffectsData())
            .serialize(new DataSerializer<>(EffectsData::new))
            .copyOnDeath() // Preserve effects on death
            .build()
    );

    /**
     * Type-safe serializer for data classes that implement ISerializableData.
     * Uses proper type bounds instead of instanceof chains for better maintainability and type safety.
     */
    private static class DataSerializer<T extends ISerializableData> implements IAttachmentSerializer<CompoundTag, T> {
        private final Supplier<T> factory;

        public DataSerializer(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T read(net.neoforged.neoforge.attachment.IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            T data = factory.get();
            data.deserialize(tag);
            return data;
        }

        @Override
        public CompoundTag write(T attachment, HolderLookup.Provider provider) {
            return attachment.serialize();
        }
    }
}
