package com.eretenkodaniil.mkm.environment.net;

import com.eretenkodaniil.mkm.enemy.EnemyCatalog;
import com.eretenkodaniil.mkm.enemy.EnemyDefinition;
import com.eretenkodaniil.mkm.environment.EnvironmentVfxBudget;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Bounded S2C visual notifications only, no C2S attack or control surface.
 * User accessibility/performance choices are resolved locally by each client.
 */
public final class EnvironmentVisualNetworking {
    private EnvironmentVisualNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(
                EnvironmentVisualEvent.TYPE, EnvironmentVisualEvent.CODEC);
    }

    public static void ambient(ServerLevel level, PathfinderMob mob, long tick) {
        if (!EnvironmentVfxBudget.ambientDue(tick, mob.getId())) return;
        byte cue = cueFor(mob, false);
        if (cue != 0) broadcast(level, mob, cue);
    }

    public static void confirmedDamage(ServerLevel level, PathfinderMob mob) {
        byte cue = cueFor(mob, true);
        if (cue != 0) broadcast(level, mob, cue);
    }

    private static byte cueFor(PathfinderMob mob, boolean damage) {
        EnemyDefinition d = EnemyCatalog.byTags(mob.entityTags());
        if (d == EnemyCatalog.ROAD_RAIDER) return damage
                ? EnvironmentVfxBudget.RAIDER_HIT : EnvironmentVfxBudget.RAIDER_AMBIENT;
        if (d == EnemyCatalog.CAVE_STALKER) return damage
                ? EnvironmentVfxBudget.STALKER_HIT : EnvironmentVfxBudget.STALKER_AMBIENT;
        return 0;
    }

    private static void broadcast(ServerLevel level, PathfinderMob mob, byte cue) {
        if (!mob.isAlive()) return;
        for (var player : level.players()) {
            if (player.distanceToSqr(mob) <= 32.0D * 32.0D
                    && ServerPlayNetworking.canSend(player, EnvironmentVisualEvent.TYPE)) {
                ServerPlayNetworking.send(player,
                        new EnvironmentVisualEvent(mob.getId(), cue));
            }
        }
    }
}
