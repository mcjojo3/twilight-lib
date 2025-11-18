package mc.sayda.twilight_lib.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.Level;

public class CustomFoxEntity extends Fox {
    private final FoxColor foxColor;
    private volatile boolean forceSleeping = false;

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
        RED
    }
}