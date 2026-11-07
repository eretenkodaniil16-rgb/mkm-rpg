package com.eretenkodaniil.mkm.enemy;

import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.PoiseState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;

/**
 * One small, unload-discardable controller per *loaded* tagged enemy.
 * Position and tactical memory are intentionally ephemeral: vanilla saves the
 * entity and its permanent tags/attributes; no unloaded chunk is force-loaded.
 */
final class EnemyRuntime {
    private static final double ACQUISITION_RADIUS = 16.0;
    private final PathfinderMob mob;
    private final EnemyDefinition definition;
    private final BlockPos patrolAnchor;
    private BlockPos lastSeen;
    private long lastSeenTick = Long.MIN_VALUE;
    private double lastX;
    private double lastZ;
    private int stuckTicks;

    EnemyRuntime(PathfinderMob mob, EnemyDefinition definition) {
        this.mob = mob;
        this.definition = definition;
        this.patrolAnchor = mob.blockPosition();
        this.lastX = mob.getX();
        this.lastZ = mob.getZ();
    }

    PathfinderMob mob() { return mob; }

    void onAttackedBy(ServerPlayer attacker, long tick) {
        if (!attacker.isAlive() || attacker.isSpectator()) return;
        mob.setTarget(attacker);
        lastSeen = attacker.blockPosition();
        lastSeenTick = tick;
    }

    void tick(ServerLevel level, long now) {
        // State/expensive pathfinding checks are phased across entities.
        if (!EnemyTactics.shouldRepath(now, mob.getId(), 5)) return;

        PoiseState poise = mob.getAttached(CombatAttachments.POISE);
        if (poise != null && now < poise.staggeredUntilTick()) {
            mob.getNavigation().stop();
            return; // Soft mobility stagger, not a separate attack interrupt.
        }

        LivingEntity target = mob.getTarget();
        if (target instanceof ServerPlayer player
                && (!player.isAlive() || player.isSpectator() || player.distanceToSqr(mob) > 28 * 28)) {
            mob.setTarget(null);
            target = null;
        }
        if (target == null && EnemyTactics.shouldRepath(now, mob.getId(), 20)) {
            target = acquireNearbyPlayer(level);
            if (target != null) mob.setTarget(target);
        }

        boolean visible = target != null && mob.hasLineOfSight(target);
        if (visible) {
            lastSeen = target.blockPosition();
            lastSeenTick = now;
        }
        long age = lastSeenTick == Long.MIN_VALUE
                ? Long.MAX_VALUE : Math.max(0L, now - lastSeenTick);
        boolean memory = lastSeen != null && !EnemyTactics.memoryExpired(definition.host(), age);
        if (target != null && !visible && EnemyTactics.memoryExpired(definition.host(), age)) {
            mob.setTarget(null);
            target = null;
        }

        // Sample position every 10 ticks only while moving. No path-node scan.
        if (EnemyTactics.shouldRepath(now, mob.getId(), 10)) {
            double moved = (mob.getX() - lastX) * (mob.getX() - lastX)
                    + (mob.getZ() - lastZ) * (mob.getZ() - lastZ);
            stuckTicks = !mob.getNavigation().isDone() && moved < 0.09
                    ? Math.min(60, stuckTicks + 10) : 0;
            lastX = mob.getX();
            lastZ = mob.getZ();
        }

        double dist = target == null ? Double.MAX_VALUE : mob.distanceToSqr(target);
        double healthRatio = mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
        EnemyTactics.Decision decision = EnemyTactics.decide(
                definition.host(), target != null || memory, visible,
                healthRatio, age, dist, stuckTicks);

        switch (decision) {
            case RECOVER_PATH -> {
                mob.getNavigation().stop();
                stuckTicks = 0;
                if (lastSeen != null) {
                    moveToward(lastSeen, 0.85);
                } else {
                    patrol(now);
                }
            }
            case PATROL -> {
                if (mob.getTarget() == null && mob.getNavigation().isDone()
                        && EnemyTactics.shouldRepath(now, mob.getId(), 40)) patrol(now);
            }
            case INVESTIGATE -> {
                // Drop vanilla's target when it is no longer visible so it does
                // not cheat by following real player coordinates through walls.
                if (!visible && target != null) mob.setTarget(null);
                if (lastSeen != null && EnemyTactics.shouldRepath(now, mob.getId(), 20)) {
                    moveToward(lastSeen, 1.0);
                }
            }
            case RETREAT -> {
                if (EnemyTactics.shouldRepath(now, mob.getId(), 20)) {
                    double dx = target == null ? mob.getX() - patrolAnchor.getX()
                            : mob.getX() - target.getX();
                    double dz = target == null ? mob.getZ() - patrolAnchor.getZ()
                            : mob.getZ() - target.getZ();
                    double norm = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
                    mob.getNavigation().moveTo(mob.getX() + 6.0 * dx / norm,
                            mob.getY(), mob.getZ() + 6.0 * dz / norm, 1.10);
                }
            }
            case FLANK -> {
                // Spider still uses vanilla Spider attack/leap/climb decisions.
                // This only asks native navigation for a lateral approach.
                if (target != null && EnemyTactics.shouldRepath(now, mob.getId(), 30)) {
                    double dx = target.getX() - mob.getX();
                    double dz = target.getZ() - mob.getZ();
                    double norm = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
                    double side = (mob.getId() & 1) == 0 ? 1.0 : -1.0;
                    mob.getNavigation().moveTo(target.getX() - side * dz / norm * 3.0,
                            target.getY(), target.getZ() + side * dx / norm * 3.0, 1.20);
                }
            }
            case CHASE, ATTACK -> { /* vanilla GoalSelector remains authoritative */ }
        }
    }

    private ServerPlayer acquireNearbyPlayer(ServerLevel level) {
        ServerPlayer nearest = null;
        double best = ACQUISITION_RADIUS * ACQUISITION_RADIUS;
        // A local spatial query once per 20 ticks, NOT a global entity scan.
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                mob.getBoundingBox().inflate(ACQUISITION_RADIUS),
                p -> p.isAlive() && !p.isSpectator())) {
            double distance = mob.distanceToSqr(player);
            if (distance < best && mob.hasLineOfSight(player)) {
                nearest = player;
                best = distance;
            }
        }
        return nearest;
    }

    private void moveToward(BlockPos destination, double speed) {
        mob.getNavigation().moveTo(destination.getX() + 0.5, destination.getY(),
                destination.getZ() + 0.5, speed);
    }

    private void patrol(long now) {
        int direction = ((now / 120L + (mob.getId() & 1)) & 1L) == 0L ? 1 : -1;
        mob.getNavigation().moveTo(patrolAnchor.getX() + direction * 4 + 0.5,
                patrolAnchor.getY(), patrolAnchor.getZ() + 3.5 * direction, 0.82);
    }
}
