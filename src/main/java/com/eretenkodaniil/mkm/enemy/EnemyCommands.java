package com.eretenkodaniil.mkm.enemy;

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

/** Opt-in GM test spawns; no autonomous worldgen or replacement spawn ecology. */
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
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())))
                .then(Commands.literal("stress")
                        .then(Commands.literal("20").executes(ctx -> stress(ctx.getSource(), 20)))
                        .then(Commands.literal("40").executes(ctx -> stress(ctx.getSource(), 40)))
                        .then(Commands.literal("80").executes(ctx -> stress(ctx.getSource(), 80))))
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

    private static boolean allowed(CommandSourceStack source, ServerLevel world) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) return true;
        source.sendFailure(Component.literal("MKM: disable Peaceful difficulty before testing enemies."));
        return false;
    }

    private static int spawn(CommandSourceStack source, EnemyDefinition definition, int level)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = (ServerLevel) player.level();
        if (!allowed(source, world)) return 0;
        BlockPos origin = player.blockPosition();
        int[][] offsets = {{4,0}, {-4,0}, {0,4}, {0,-4}, {4,4}, {-4,4}, {4,-4}, {-4,-4}};
        for (int[] offset : offsets) {
            PathfinderMob mob = create(world, player, definition, level,
                    origin.offset(offset[0], 0, offset[1]));
            if (mob == null) continue;
            source.sendSuccess(() -> Component.literal(
                    "MKM: spawned " + definition.id() + " level " + level
                            + " UUID=" + mob.getUUID()), true);
            return 1;
        }
        source.sendFailure(Component.literal("MKM: no safe solid-footing spawn nearby."));
        return 0;
    }

    /** Single GM command: real 20/40 loaded mobs, native AI, no fake synthetic targets. */
    private static int stress(CommandSourceStack source, int requested)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = (ServerLevel) player.level();
        if (!allowed(source, world)) return 0;
        int spawned = 0;
        BlockPos origin = player.blockPosition();
        int radius = requested >= 80 ? 18 : 12;
        for (int dx = -radius; dx <= radius && spawned < requested; dx += 3) {
            for (int dz = -radius; dz <= radius && spawned < requested; dz += 3) {
                if (dx * dx + dz * dz < 16) continue;
                EnemyDefinition definition = (spawned & 1) == 0
                        ? EnemyCatalog.ROAD_RAIDER : EnemyCatalog.CAVE_STALKER;
                if (create(world, player, definition, 1, origin.offset(dx, 0, dz)) != null)
                    spawned++;
            }
        }
        EnemyService.resetPerformance();
        int result = spawned;
        source.sendSuccess(() -> Component.literal(
                "MKM Enemy stress: spawned " + result + "/" + requested
                        + " loaded hostile prototypes. Telemetry reset; inspect /mkmenemy perf."), true);
        return spawned;
    }

    /** Cleanup does not call death or pay XP/loot. It affects only prototype-tagged mobs. */
    private static int clear(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        int cleared = 0;
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class,
                player.getBoundingBox().inflate(48.0),
                candidate -> EnemyCatalog.byTags(candidate.entityTags()) != null)) {
            mob.discard();
            cleared++;
        }
        int result = cleared;
        source.sendSuccess(() -> Component.literal(
                "MKM Enemy: discarded " + result + " prototypes without rewards."), true);
        return cleared;
    }

    private static PathfinderMob create(ServerLevel world, ServerPlayer player,
            EnemyDefinition definition, int level, BlockPos feet) {
        if (!world.getBlockState(feet.below()).isSolid()) return null;
        PathfinderMob mob = definition.host() == EnemyDefinition.Host.HUSK
                ? new Husk(EntityTypes.HUSK, world)
                : new Spider(EntityTypes.SPIDER, world);
        mob.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 0.0F);
        if (!world.noCollision(mob)) return null;
        EnemyLevelRules.Stats stats = EnemyLevelRules.forLevel(definition, level);
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
        if (!world.addFreshEntity(mob)) return null;
        EnemyService.onLoad(mob, world);
        mob.setTarget(player);
        return mob;
    }

    private static void setBaseAttribute(PathfinderMob mob,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> key, double value) {
        AttributeInstance instance = mob.getAttribute(key);
        if (instance != null) instance.setBaseValue(value);
    }
}
