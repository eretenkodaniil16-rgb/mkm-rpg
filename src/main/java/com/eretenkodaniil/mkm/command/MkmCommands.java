package com.eretenkodaniil.mkm.command;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import com.eretenkodaniil.mkm.combat.fencing.FencingSchool;
import com.eretenkodaniil.mkm.combat.fencing.FencingSchoolService;
import com.eretenkodaniil.mkm.combat.fencing.FencingSchoolRules;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import net.minecraft.core.registries.BuiltInRegistries;
import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.ProgressionRules;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Minimal diagnostics/admin surface for Character Core.
 *
 * <p>The class uses automatic event-bus subscription so command registration does not depend on
 * manual bootstrap listener wiring.</p>
 */
public final class MkmCommands {
    private MkmCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkm")
                .executes(context -> showHelp(context.getSource()))
                .then(Commands.literal("help")
                        .executes(context -> showHelp(context.getSource())))
                .then(Commands.literal("ping")
                        .executes(context -> ping(context.getSource())))
                .then(Commands.literal("stats")
                        .executes(context -> showStats(context.getSource())))
                .then(Commands.literal("modifiers")
                        .executes(context -> showModifiers(context.getSource())))

                .then(Commands.literal("fencing")
                        .executes(ctx -> showFencing(ctx.getSource()))
                        .then(Commands.literal("none").executes(ctx -> selectFencing(ctx.getSource(), FencingSchool.NONE)))
                        .then(Commands.literal("liechtenauer").executes(ctx -> selectFencing(ctx.getSource(), FencingSchool.LIECHTENAUER)))
                        .then(Commands.literal("fiore").executes(ctx -> selectFencing(ctx.getSource(), FencingSchool.FIORE)))
                        .then(Commands.literal("destreza").executes(ctx -> selectFencing(ctx.getSource(), FencingSchool.DESTREZA))))
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

    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "MKM: /mkm ping, stats, modifiers, fencing [none|liechtenauer|fiore|destreza]. Admin: /mkm xp add <amount>, /mkm xp set <amount>."), false);
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
                        + " | PER: " + data.perception()
                        + " | Points: " + data.availablePoints() + "/" + data.earnedPoints()), false);
        return Command.SINGLE_SUCCESS;
    }


    private static int showModifiers(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerModifierSnapshot modifiers = EquipmentSnapshots.snapshot(player, CharacterService.get(player));

        source.sendSuccess(() -> Component.literal(
                "MKM modifiers | HP " + modifiers.formatted(PlayerModifierId.MAX_HEALTH)
                        + " | Physical " + modifiers.formatted(PlayerModifierId.PHYSICAL_POWER)
                        + " | Melee " + modifiers.formatted(PlayerModifierId.MELEE_DAMAGE)
                        + " | Ranged " + modifiers.formatted(PlayerModifierId.RANGED_DAMAGE)
                        + " | Attack speed " + modifiers.formatted(PlayerModifierId.ATTACK_SPEED)), false);
        source.sendSuccess(() -> Component.literal(
                "Stamina " + modifiers.formatted(PlayerModifierId.MAX_STAMINA)
                        + " | Regen " + modifiers.formatted(PlayerModifierId.STAMINA_REGENERATION)
                        + " | Cost " + modifiers.formatted(PlayerModifierId.STAMINA_COST)
                        + " | Armor penetration " + modifiers.formatted(PlayerModifierId.ARMOR_PENETRATION)), false);
        source.sendSuccess(() -> Component.literal(
                "Physical resist " + modifiers.formatted(PlayerModifierId.PHYSICAL_RESISTANCE)
                        + " | Slash " + modifiers.formatted(PlayerModifierId.SLASHING_RESISTANCE)
                        + " | Pierce " + modifiers.formatted(PlayerModifierId.PIERCING_RESISTANCE)
                        + " | Blunt " + modifiers.formatted(PlayerModifierId.BLUNT_RESISTANCE)
                        + " | Stagger " + modifiers.formatted(PlayerModifierId.STAGGER_POWER)
                        + " | Block stability " + modifiers.formatted(PlayerModifierId.BLOCK_STABILITY)), false);
        source.sendSuccess(() -> Component.literal(
                "Magic " + modifiers.formatted(PlayerModifierId.MAGIC_POWER)
                        + " | Spell damage " + modifiers.formatted(PlayerModifierId.SPELL_DAMAGE)
                        + " | Mana " + modifiers.formatted(PlayerModifierId.MAX_MANA)
                        + " | Mana regen " + modifiers.formatted(PlayerModifierId.MANA_REGENERATION)
                        + " | Magic resistance " + modifiers.formatted(PlayerModifierId.MAGIC_RESISTANCE)
                        + " | Weak point " + modifiers.formatted(PlayerModifierId.WEAK_POINT_DAMAGE)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int showFencing(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var state = FencingSchoolService.state(player);
        long now = player.level().getGameTime();
        String id = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
        boolean active = FencingSchoolRules.effectiveSchool(state,
                EquipmentRules.profile(id, EquipmentRules.Slot.MAIN_HAND).family(), now).isPresent();
        source.sendSuccess(() -> Component.literal("Fencing: " + state.school().id()
                + " | " + (active ? "ACTIVE" : "NOT READY/NOT SWORD")
                + " | warmup " + state.remainingWarmup(now) + "t"
                + " | switch " + state.remainingSwitchCooldown(now) + "t"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int selectFencing(CommandSourceStack source, FencingSchool school)
            throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var result = FencingSchoolService.select(player, school);
        if (!result.applied()) {
            source.sendFailure(Component.literal("Fencing refused: " + result.code()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("School " + school.id()
                + " selected | 20t warmup | 80t switch cooldown"), false);
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
