package mc.sayda.twilight_lib.client;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.screens.Screen;

public class ClientAccess {
    public static Level getLevel() {
        return EnvExecutor.getEnvSpecific(() -> () -> net.minecraft.client.Minecraft.getInstance().level,
                () -> () -> null);
    }

    public static void setScreen(Screen screen) {
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> net.minecraft.client.Minecraft.getInstance().setScreen(screen));
    }

    public static Player getPlayer() {
        return EnvExecutor.getEnvSpecific(() -> () -> net.minecraft.client.Minecraft.getInstance().player,
                () -> () -> null);
    }
}
