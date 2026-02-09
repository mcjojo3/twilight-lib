package mc.sayda.twilight_lib.neoforge.capabilities;

import mc.sayda.twilight_lib.TwilightLib;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import mc.sayda.twilight_lib.capabilities.AddonsData;
import mc.sayda.twilight_lib.capabilities.EffectsData;
import mc.sayda.twilight_lib.capabilities.ISerializableData;
import mc.sayda.twilight_lib.capabilities.ModelVariantData;
import mc.sayda.twilight_lib.capabilities.MorphData;
import mc.sayda.twilight_lib.capabilities.TrailsData;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge 1.21.1 Data Attachments
 */
public class ModAttachments {
        public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
                        .create(NeoForgeRegistries.ATTACHMENT_TYPES, TwilightLib.MODID);

        public static final Supplier<AttachmentType<MorphData>> MORPH = ATTACHMENT_TYPES.register(
                        "morph",
                        () -> AttachmentType.builder(MorphData::new)
                                        .serialize(new DataSerializer<>(MorphData::new))
                                        .copyOnDeath()
                                        .build());

        public static final Supplier<AttachmentType<AddonsData>> ADDONS = ATTACHMENT_TYPES.register(
                        "addons",
                        () -> AttachmentType.builder(AddonsData::new)
                                        .serialize(new DataSerializer<>(AddonsData::new))
                                        .copyOnDeath()
                                        .build());

        public static final Supplier<AttachmentType<TrailsData>> TRAILS = ATTACHMENT_TYPES.register(
                        "trails",
                        () -> AttachmentType.builder(TrailsData::new)
                                        .serialize(new DataSerializer<>(TrailsData::new))
                                        .copyOnDeath()
                                        .build());

        public static final Supplier<AttachmentType<EffectsData>> EFFECTS = ATTACHMENT_TYPES.register(
                        "effects",
                        () -> AttachmentType.builder(EffectsData::new)
                                        .serialize(new DataSerializer<>(EffectsData::new))
                                        .copyOnDeath()
                                        .build());

        public static final Supplier<AttachmentType<ModelVariantData>> MODEL_VARIANT = ATTACHMENT_TYPES.register(
                        "model_variant",
                        () -> AttachmentType.builder(ModelVariantData::new)
                                        .serialize(new DataSerializer<>(ModelVariantData::new))
                                        .copyOnDeath()
                                        .build());

        public static final Supplier<AttachmentType<CompoundTag>> PERSISTENT_DATA = ATTACHMENT_TYPES.register(
                        "persistent_data",
                        () -> AttachmentType.builder(CompoundTag::new)
                                        .serialize(new IAttachmentSerializer<CompoundTag, CompoundTag>() {
                                                @Override
                                                public CompoundTag read(
                                                                net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                                                CompoundTag tag,
                                                                HolderLookup.Provider provider) {
                                                        return tag;
                                                }

                                                @Override
                                                public CompoundTag write(CompoundTag attachment,
                                                                HolderLookup.Provider provider) {
                                                        return attachment;
                                                }
                                        })
                                        .copyOnDeath()
                                        .build());

        private static class DataSerializer<T extends ISerializableData>
                        implements IAttachmentSerializer<CompoundTag, T> {
                private final Supplier<T> factory;

                public DataSerializer(Supplier<T> factory) {
                        this.factory = factory;
                }

                @Override
                public T read(net.neoforged.neoforge.attachment.IAttachmentHolder holder, CompoundTag tag,
                                HolderLookup.Provider provider) {
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
