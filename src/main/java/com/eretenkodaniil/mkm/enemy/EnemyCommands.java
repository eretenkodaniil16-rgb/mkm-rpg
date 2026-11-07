package com.eretenkodaniil.mkm.enemy;

import com.eretenkodaniil.mkm.MkmMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.spider.Spider;

/** Opt-in GM test spawns. Encounter Director will eventually own natural spawn rules. */
public final class EnemyCommands {
    private EnemyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mkmenemy")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("perf").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(EnemyService.performanceLine()), false);
                    return 1;
                }).then(Commands.literal("reset").executes(ctx -> {
                    EnemyService.resetPerformance();
                    ctx.getSource().sendSuccess(() -> Component.literal("MKM Enemy AI telemetry reset."), false);
                    return 1;
                })))
                .then(Commands.literal("spawn")
                        .then(Commands.literal("raider")
                                .executes(ctx -> spawn(ctx.getSource(), EnemyCatalog.ROAD_RAIDER, 1))
                                .then(Commands.argument("level", IntegerArgumentType.integer(
                                        1, EnemyLevelRules.MAX_LEVEL))
                                        .executes(ctx -> spawn(ctx.getSource(), EnemyCatalog.ROAD_RAIDER,
                                                IntegerArgumentType.getInteger(ctx, "level")))))
                        .then(Commands.literal("stalker")
                                .executes(ctx -> spawn(ctx.getSource(), EnemyCatalog.CAVE_STALKER, 1))
                                .then(Commands.argument("level", IntegerArgumentType.integer(
                                        1, EnemyLevelRules.MAX_LEVEL))
                                        .executes(ctx -> spawn(ctx.getSource(), EnemyCatalog.CAVE_STALKER,
                                                IntegerArgumentType.getInteger(ctx, "level")))))));
    }

    private static int spawn(CommandSourceStack source, EnemyDefinition definition, int requestedLevel)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = (ServerLevel) player.level();
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            source.sendFailure(Component.literal("MKM: disable Peaceful difficulty before testing enemies."));
            return 0;
        }
        EnemyLevelRules.Stats stats = EnemyLevelRules.forLevel(definition, requestedLevel);
        BlockPos origin = player.blockPosition();
        int[][] offsets = {{4,0}, {-4,0}, {0,4}, {0,-4}, {4,4}, {-4,4}, {4,-4}, {-4,-4}};
        for (int[] offset : offsets) {
            BlockPos feet = origin.offset(offset[0], 0, offset[1]);
            if (!world.getBlockState(feet.below()).isSolid()) continue;
            PathfinderMob mob = definition.host() == EnemyDefinition.Host.HUSK
                    ? new Husk(EntityTypes.HUSK, world)
                    : new Spider(EntityTypes.SPIDER, world);
            mob.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 0.0F);
            if (!world.noCollision(mob)) continue;
            mob.addTag(definition.persistentTag());
            mob.addTag(EnemyLevelRules.levelTag(stats.level()));
            mob.setCustomName(Component.literal(definition == EnemyCatalog.ROAD_RAIDER
                    ? "MKM Road Raider Lv." + stats.level()
                    : "MKM Cave Stalker Lv." + stats.level()));
            mob.setCustomNameVisible(true);
            mob.setPersistenceRequired();
            setBaseAttribute(mob, Attributes.MAX_HEALTH, stats.health());
            setBaseAttribute(mob, Attributes.ATTACK_DAMAGE, stats.attack());
            setBaseAttribute(mob, Attributes.MOVEMENT_SPEED, stats.speed());
            mob.setHealth((float) stats.health());
            if (world.addFreshEntity(mob)) {
                // ENTITY_LOAD adds only this tagged mob to the local controller index.
                EnemyService.onLoad(mob, world);
                mob.setTarget(player);
                source.sendSuccess(() -> Component.literal(
                        "MKM: spawned " + definition.id() + " level " + stats.level()
                                + " UUID=" + mob.getUUID()), true);
                return 1;
            }
        }
        source.sendFailure(Component.literal("MKM: no safe solid-footing spawn nearby."));
        return 0;
    }

    private static void setBaseAttribute(PathfinderMob mob,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> key, double value) {
        AttributeInstance instance = mob.getAttribute(key);
        if (instance != null) instance.setBaseValue(value);
    }
}
