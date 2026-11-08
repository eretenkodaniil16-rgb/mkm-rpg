package com.eretenkodaniil.mkm.showcase;

import com.eretenkodaniil.mkm.environment.WatchtowerPlacementCommands;
import com.eretenkodaniil.mkm.quests.FordQuestService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** All privileged commands are scene preparation, not normal player progression. */
public final class ShowcaseCommands {
    private ShowcaseCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkmshowcase")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("setup")
                        .then(Commands.argument("variant",IntegerArgumentType.integer(1,3))
                                .then(Commands.argument("x",IntegerArgumentType.integer(-29999000,29999000))
                                        .then(Commands.argument("y",IntegerArgumentType.integer(-64,400))
                                                .then(Commands.argument("z",IntegerArgumentType.integer(-29999000,29999000))
                                                        .executes(c->setup(c.getSource(),
                                                                IntegerArgumentType.getInteger(c,"variant"),
                                                                pos(c.getSource(),
                                                                    IntegerArgumentType.getInteger(c,"x"),
                                                                    IntegerArgumentType.getInteger(c,"y"),
                                                                    IntegerArgumentType.getInteger(c,"z")))))))))
                .then(Commands.literal("bind")
                        .then(Commands.argument("x",IntegerArgumentType.integer(-29999000,29999000))
                                .then(Commands.argument("y",IntegerArgumentType.integer(-64,400))
                                        .then(Commands.argument("z",IntegerArgumentType.integer(-29999000,29999000))
                                                .executes(c->bind(c.getSource(),pos(c.getSource(),
                                                        IntegerArgumentType.getInteger(c,"x"),
                                                        IntegerArgumentType.getInteger(c,"y"),
                                                        IntegerArgumentType.getInteger(c,"z"))))))));
    }
    private static BlockPos pos(CommandSourceStack source,int x,int y,int z) {
        return new BlockPos(x,y,z);
    }
    private static int setup(CommandSourceStack source,int variant,BlockPos at) throws CommandSyntaxException {
        if (WatchtowerPlacementCommands.place(source,variant,at.getX(),at.getY(),at.getZ())!=1)
            return 0;
        ServerPlayer admin=source.getPlayerOrException();
        if (!ShowcaseService.bind(admin,at)) return 0;
        // Place Ilra near the operator's current starting point, not at the tower.
        // If an Ilra already exists this is a no-op, keeping authored NPC state.
        FordQuestService.placeWarden(admin);
        return 1;
    }
    private static int bind(CommandSourceStack source,BlockPos at) throws CommandSyntaxException {
        return ShowcaseService.bind(source.getPlayerOrException(),at)?1:0;
    }
}
