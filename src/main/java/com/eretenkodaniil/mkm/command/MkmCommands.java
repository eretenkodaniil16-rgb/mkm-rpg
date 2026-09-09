package com.eretenkodaniil.mkm.command;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.ProgressionRules;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Minimal diagnostics/admin surface for Character Core.
 *
 * <p>The class uses automatic event-bus subscription so command registration does not depend on
 * manual bootstrap listener wiring.</p>
 */
@EventBusSubscriber(modid = MkmMod.MOD_ID)
public final class MkmCommands {
    private MkmCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mkm")
                .executes(context -> showHelp(context.getSource()))
                .then(Commands.literal("help")
                        .executes(context -> showHelp(context.getSource())))
                .then(Commands.literal("ping")
                        .executes(context -> ping(context.getSource())))
                .then(Commands.literal("stats")
                        .executes(context -> showStats(context.getSource())))
                .then(Commands.literal("xp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                        .executes(context -> addExperience(
                                                context.getSource(),
                                                LongArgumentType.getLong(context, "amount")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                        .executes(context -> setExperience(
                                                context.getSource(),
                                                LongArgumentType.getLong(context, "amount")))))));

        MkmMod.LOGGER.info("Registered MKM commands: /mkm, /mkm ping, /mkm stats, /mkm xp ...");
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "MKM commands: /mkm ping, /mkm stats. Admin/cheats: /mkm xp add <amount>, /mkm xp set <amount>."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int ping(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("MKM command system is active."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int showStats(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CharacterData data = CharacterService.get(player);

        String experienceText = ProgressionRules.isMaxLevel(data.experience())
                ? data.experience() + " (MAX level reached)"
                : data.experience() + "/" + ProgressionRules.experienceForNextLevel(data.experience());

        source.sendSuccess(() -> Component.literal(
                "MKM | Level: " + data.level()
                        + " | XP: " + experienceText
                        + " | STR: " + data.strength()
                        + " | DEX: " + data.dexterity()
                        + " | VIT: " + data.vitality()
                        + " | END: " + data.endurance()
                        + " | INT: " + data.intelligence()
                        + " | WIL: " + data.willpower()
                        + " | PER: " + data.perception()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int addExperience(CommandSourceStack source, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CharacterData previous = CharacterService.get(player);
        CharacterData updated = CharacterService.addExperience(player, amount);

        source.sendSuccess(() -> Component.literal(
                "MKM XP: +" + amount
                        + " -> " + updated.experience()
                        + " | Level " + previous.level() + " -> " + updated.level()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setExperience(CommandSourceStack source, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CharacterData updated = CharacterService.setExperience(player, amount);

        source.sendSuccess(() -> Component.literal(
                "MKM XP set to " + updated.experience() + " | Level: " + updated.level()), true);
        return Command.SINGLE_SUCCESS;
    }
}
