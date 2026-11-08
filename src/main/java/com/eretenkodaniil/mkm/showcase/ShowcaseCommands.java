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

/** All commands prepare the world for players; story progression requires no commands. */
public final class ShowcaseCommands {
    private ShowcaseCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var setup=Commands.literal("setup");
        var variant=Commands.argument("variant",IntegerArgumentType.integer(1,3));
        var x=Commands.argument("x",IntegerArgumentType.integer(-29999000,29999000));
        var y=Commands.argument("y",IntegerArgumentType.integer(-64,400));
        var z=Commands.argument("z",IntegerArgumentType.integer(-29999000,29999000));
        z.executes(c->setup(c.getSource(),IntegerArgumentType.getInteger(c,"variant"),
                new BlockPos(IntegerArgumentType.getInteger(c,"x"),
                        IntegerArgumentType.getInteger(c,"y"),
                        IntegerArgumentType.getInteger(c,"z"))));
        y.then(z);
        x.then(y);
        variant.then(x);
        setup.then(variant);

        var bind=Commands.literal("bind");
        var bindX=Commands.argument("x",IntegerArgumentType.integer(-29999000,29999000));
        var bindY=Commands.argument("y",IntegerArgumentType.integer(-64,400));
        var bindZ=Commands.argument("z",IntegerArgumentType.integer(-29999000,29999000));
        bindZ.executes(c->bind(c.getSource(),new BlockPos(
                IntegerArgumentType.getInteger(c,"x"),
                IntegerArgumentType.getInteger(c,"y"),
                IntegerArgumentType.getInteger(c,"z"))));
        bindY.then(bindZ);
        bindX.then(bindY);
        bind.then(bindX);
        dispatcher.register(Commands.literal("mkmshowcase")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(setup)
                .then(bind));
    }
    private static int setup(CommandSourceStack source,int variant,BlockPos at)
            throws CommandSyntaxException {
        if (WatchtowerPlacementCommands.place(source,variant,at.getX(),at.getY(),at.getZ())!=1)
            return 0;
        ServerPlayer admin=source.getPlayerOrException();
        if (!ShowcaseService.bind(admin,at)) return 0;
        FordQuestService.placeWarden(admin);
        return 1;
    }
    private static int bind(CommandSourceStack source,BlockPos at)
            throws CommandSyntaxException {
        return ShowcaseService.bind(source.getPlayerOrException(),at)?1:0;
    }
}
