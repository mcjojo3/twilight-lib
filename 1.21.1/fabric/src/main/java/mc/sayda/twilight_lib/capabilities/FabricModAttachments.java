package mc.sayda.twilight_lib.capabilities;

import mc.sayda.twilight_lib.TwilightLib;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Codec;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Fabric-specific data attachment registration using Fabric API.
 */
public class FabricModAttachments {

        private static final Logger LOGGER = LogUtils.getLogger();

        private static <T extends ISerializableData> Codec<T> createCodec(Supplier<T> factory) {
                return CompoundTag.CODEC.xmap(tag -> {
                        T data = factory.get();
                        data.deserialize(tag);
                        return data;
                }, ISerializableData::serialize);
        }

        public static final AttachmentType<MorphData> MORPH = AttachmentRegistry.<MorphData>builder()
                        .initializer(MorphData::new)
                        .persistent(createCodec(MorphData::new))
                        .copyOnDeath()
                        .buildAndRegister(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "morph"));

        public static final AttachmentType<AddonsData> ADDONS = AttachmentRegistry.<AddonsData>builder()
                        .initializer(AddonsData::new)
                        .persistent(createCodec(AddonsData::new))
                        .copyOnDeath()
                        .buildAndRegister(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "addons"));

        public static final AttachmentType<TrailsData> TRAILS = AttachmentRegistry.<TrailsData>builder()
                        .initializer(TrailsData::new)
                        .persistent(createCodec(TrailsData::new))
                        .copyOnDeath()
                        .buildAndRegister(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "trails"));

        public static final AttachmentType<EffectsData> EFFECTS = AttachmentRegistry
                        .<EffectsData>builder()
                        .initializer(EffectsData::new)
                        .persistent(createCodec(EffectsData::new))
                        .copyOnDeath()
                        .buildAndRegister(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "effects"));

        public static final AttachmentType<ModelVariantData> MODEL_VARIANT = AttachmentRegistry
                        .<ModelVariantData>builder()
                        .initializer(ModelVariantData::new)
                        .persistent(createCodec(ModelVariantData::new))
                        .copyOnDeath()
                        .buildAndRegister(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "model_variant"));

        // Persistent NBT Data (Generic)
        public static final AttachmentType<CompoundTag> PERSISTENT_DATA = AttachmentRegistry
                        .<CompoundTag>builder()
                        .initializer(CompoundTag::new)
                        .persistent(CompoundTag.CODEC)
                        .copyOnDeath()
                        .buildAndRegister(ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "persistent_data"));

        public static void init() {
                // Just triggers class loading and registration
                LOGGER.debug("Hi! My name is Zoe. Fabric data attachments registered for Twilight Lib.");
        }
}
