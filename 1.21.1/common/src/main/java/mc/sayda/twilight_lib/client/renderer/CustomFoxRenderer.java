package mc.sayda.twilight_lib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import mc.sayda.twilight_lib.TwilightLib;
import mc.sayda.twilight_lib.entity.CustomFoxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.client.renderer.entity.layers.FoxHeldItemLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Fox;
import org.slf4j.Logger;

public class CustomFoxRenderer extends FoxRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation WHITE_FOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "textures/entity/fox/white_fox.png");
    private static final ResourceLocation BLACK_FOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "textures/entity/fox/black_fox.png");
    private static final ResourceLocation BLUE_FOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "textures/entity/fox/blue_fox.png");
    private static final ResourceLocation YELLOW_FOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "textures/entity/fox/yellow_fox.png");
    private static final ResourceLocation ORANGE_FOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "textures/entity/fox/orange_fox.png");
    private static final ResourceLocation PURPLE_FOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "textures/entity/fox/purple_fox.png");
    private static final ResourceLocation RED_FOX_TEXTURE = ResourceLocation.fromNamespaceAndPath(TwilightLib.MODID, "textures/entity/fox/red_fox.png");

    public CustomFoxRenderer(EntityRendererProvider.Context context) {
        super(context);

        // Remove the vanilla fox held item layer and add our custom one with rotation
        this.layers.removeIf(layer -> layer instanceof FoxHeldItemLayer);
        this.addLayer(new CustomFoxHeldItemLayer(this, context.getItemInHandRenderer()));
        LOGGER.debug("Want to see something neat? Initialized CustomFoxRenderer with custom item rotation layer");
    }

    @Override
    public ResourceLocation getTextureLocation(Fox fox) {
        if (fox instanceof CustomFoxEntity customFox) {
            var color = customFox.getFoxColor();
            // Null check: fallback to default texture if color is null
            if (color == null) {
                return super.getTextureLocation(fox);
            }
            return switch (color) {
                case WHITE -> WHITE_FOX_TEXTURE;
                case BLACK -> BLACK_FOX_TEXTURE;
                case BLUE -> BLUE_FOX_TEXTURE;
                case YELLOW -> YELLOW_FOX_TEXTURE;
                case ORANGE -> ORANGE_FOX_TEXTURE;
                case PURPLE -> PURPLE_FOX_TEXTURE;
                case RED -> RED_FOX_TEXTURE;
            };
        }
        return super.getTextureLocation(fox);
    }

    @Override
    public void render(Fox fox, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        super.render(fox, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}