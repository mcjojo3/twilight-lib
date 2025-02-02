package mc.sayda.twilight_lib.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import mc.sayda.twilight_lib.ModelManager;

public class TwilightLibCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("twilightlib")
                .then(Commands.literal("model")
                        .then(Commands.literal("set")
                                .then(Commands.argument("model_name", StringArgumentType.word())
                                        .executes(context -> {
                                            String modelName = StringArgumentType.getString(context, "model_name");
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            if (ModelManager.hasModel(modelName)) {
                                                ModelManager.setPlayerModel(player, modelName);
                                                context.getSource().sendSuccess(() -> Component.literal("Model set to " + modelName), true);
                                            } else {
                                                context.getSource().sendFailure(Component.literal("No such model: " + modelName));
                                            }
                                            return 1;
                                        })))
                        .then(Commands.literal("remove")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ModelManager.removePlayerModel(player);
                                    context.getSource().sendSuccess(() -> Component.literal("Model removed"), true);
                                    return 1;
                                }))));
    }
}
