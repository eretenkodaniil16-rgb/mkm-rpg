package com.eretenkodaniil.mkm.quests;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/** Server-validated dialogue choices and a permission-gated authored-NPC placement command. */
public final class FordQuestCommands {
    private FordQuestCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkmworld")
                .then(Commands.literal("place")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> place(context.getSource())))
                .then(Commands.literal("talk").executes(context -> execute(context.getSource(), "talk")))
                .then(Commands.literal("leave").executes(context -> execute(context.getSource(), "leave")))
                .then(Commands.literal("inspect").executes(context -> execute(context.getSource(), "inspect")))
                .then(Commands.literal("accept").executes(context -> execute(context.getSource(), "accept")))
                .then(Commands.literal("recover").executes(context -> execute(context.getSource(), "recover")))
                .then(Commands.literal("claim").executes(context -> execute(context.getSource(), "claim")))
                .then(Commands.literal("status").executes(context -> execute(context.getSource(), "status"))));
    }

    private static int place(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return FordQuestService.placeWarden(player) ? 1 : 0;
    }

    private static int execute(CommandSourceStack source, String action) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        switch (action) {
            case "talk" -> FordQuestService.talk(player);
            case "leave" -> FordQuestService.leave(player);
            case "inspect" -> FordQuestService.inspect(player);
            case "accept" -> FordQuestService.accept(player);
            case "recover" -> FordQuestService.recover(player);
            case "claim" -> FordQuestService.claim(player);
            case "status" -> FordQuestService.status(player);
            default -> throw new IllegalArgumentException("Unknown quest action");
        }
        return 1;
    }
}
