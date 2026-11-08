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
                .then(bind)
                .then(Commands.literal("status").executes(c -> status(c.getSource()))));
    }
    private static int setup(CommandSourceStack source,int variant,BlockPos at)
            throws CommandSyntaxException {
        if (WatchtowerPlacementCommands.place(source,variant,at.getX(),at.getY(),at.getZ())!=1)
            return 0;
        if (!ShowcaseService.bindWorld(source.getLevel(),at)) {
            source.sendFailure(net.minecraft.network.chat.Component.literal(
                    "MKM Showcase world site could not be bound after tower placement."));
            return 0;
        }
        // Console operators prepare world geometry; a logged-in GM can place
        // Ilra near the starting location via existing /mkmworld place.
        try {
            ServerPlayer admin=source.getPlayerOrException();
            FordQuestService.placeWarden(admin);
        } catch (CommandSyntaxException noPlayer) {
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "MKM tower bound; log in as GM and place Ilra near the players with /mkmworld place."),
                    true);
        }
        return 1;
    }
    private static int bind(CommandSourceStack source,BlockPos at) {
        var level=source.getLevel();
        boolean ok=ShowcaseService.bindWorld(level,at);
        if (ok) {
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                    "MKM Showcase site bound at "+at+" (world-persistent, no mobs created)"),true);
            return 1;
        }
        source.sendFailure(net.minecraft.network.chat.Component.literal(
                "MKM Showcase bind denied: already configured, wrong dimension or invalid NBT markers."));
        return 0;
    }
    private static int status(CommandSourceStack source) {
        ShowcaseSite site=ShowcaseService.site(source.getLevel());
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "MKM Showcase configured="+site.configured()+" origin="+site.origin()
                +" keeperReserved="+!site.keeperUuid().isBlank()),false);
        return site.configured()?1:0;
    }
}
