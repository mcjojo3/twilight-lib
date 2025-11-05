package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility methods for command handling.
 * Provides common command logic to avoid duplication across command classes.
 */
public class CommandUtils {

    /**
     * Check if feedback should be sent to the command source.
     * Used to prevent duplicate messages when a player targets themselves.
     *
     * <p>Returns true if:
     * <ul>
     *   <li>Source is not a player (console, command block, etc.)</li>
     *   <li>Source player is different from target player</li>
     * </ul>
     *
     * @param source The command source executing the command
     * @param target The target player being affected by the command
     * @return true if feedback should be sent to the source
     */
    public static boolean shouldSendFeedbackToSource(CommandSourceStack source, ServerPlayer target) {
        try {
            ServerPlayer sourcePlayer = source.getPlayerOrException();
            return !sourcePlayer.getUUID().equals(target.getUUID());
        } catch (CommandSyntaxException e) {
            // Source is not a player (command block, console, etc.)
            return true;
        }
    }

    /**
     * Get the player executing the command, or null if not a player.
     *
     * @param source The command source
     * @return The ServerPlayer executing the command, or null if source is console/command block
     */
    public static ServerPlayer getTargetPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }
}