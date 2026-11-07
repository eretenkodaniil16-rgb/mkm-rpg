package com.eretenkodaniil.mkm.encounter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Operator triggers a single authored pilot; players may inspect and claim
 * only their own completed encounter. No client packet can create a mob.
 */
public final class EncounterCommands {
    private EncounterCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkmencounter")
                .then(Commands.literal("start")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> start(c.getSource())))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("claim").executes(c -> claim(c.getSource()))));
    }
    private static int start(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        return EncounterDirector.start(player) ? 1 : 0;
    }
    private static int status(CommandSourceStack src) throws CommandSyntaxException {
        EncounterDirector.status(src.getPlayerOrException());
        return 1;
    }
    private static int claim(CommandSourceStack src) throws CommandSyntaxException {
        return EncounterDirector.claim(src.getPlayerOrException()) ? 1 : 0;
    }
}
