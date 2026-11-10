package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility methods for command handling.
 * Provides common command logic to avoid duplication across command classes.
 */
public class CommandUtils {

    /** False when a player targets themselves, so they don't get the same message twice. */
    public static boolean shouldSendFeedbackToSource(CommandSourceStack source, ServerPlayer target) {
        try {
            ServerPlayer sourcePlayer = source.getPlayerOrException();
            return !sourcePlayer.getUUID().equals(target.getUUID());
        } catch (CommandSyntaxException e) {
            // Source is not a player (command block, console, etc.)
            return true;
        }
    }

    /** The player running the command, or null for console and command blocks. */
    public static ServerPlayer getTargetPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }
}
