package mc.sayda.twilight_lib.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.Level;

public class CustomFoxEntity extends Fox {
    private final FoxColor foxColor;
    private volatile boolean forceSleeping = false;
    private volatile int tint = 0xFFFFFF;

    public CustomFoxEntity(EntityType<? extends Fox> type, Level level, FoxColor color) {
        super(type, level);
        if (color == null) {
            throw new IllegalArgumentException("FoxColor cannot be null");
        }
        this.foxColor = color;
    }

    public FoxColor getFoxColor() {
        return foxColor;
    }

    /**
     * Force the fox sleeping animation state for morphs.
     * This bypasses the normal fox AI sleeping logic.
     * The renderer will check isSleeping() which now returns forceSleeping.
     */
    public void setForceSleeping(boolean sleeping) {
        this.forceSleeping = sleeping;
    }

    public int getTint() {
        return tint;
    }

    /**
     * Push the player's morph tint onto this proxy each frame. Read by
     * {@link mc.sayda.twilight_lib.client.renderer.CustomFoxRenderer} to bake a
     * masked-tint texture instead of the plain color-variant texture.
     */
    public void setTint(int tint) {
        this.tint = tint & 0x00FFFFFF;
    }

    @Override
    public boolean isSleeping() {
        return forceSleeping || super.isSleeping();
    }

    public enum FoxColor {
        WHITE,
        BLACK,
        BLUE,
        YELLOW,
        ORANGE,
        PURPLE,
        RED,
        GRAY
    }
}