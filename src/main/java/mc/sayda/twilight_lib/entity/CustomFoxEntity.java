package mc.sayda.twilight_lib.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.Level;

public class CustomFoxEntity extends Fox {
    private final FoxColor foxColor;

    public CustomFoxEntity(EntityType<? extends Fox> type, Level level, FoxColor color) {
        super(type, level);
        this.foxColor = color;
    }

    public FoxColor getFoxColor() {
        return foxColor;
    }

    public enum FoxColor {
        WHITE,
        BLACK,
        BLUE,
        GOLDEN,
        ORANGE,
        PURPLE,
        RED
    }
}