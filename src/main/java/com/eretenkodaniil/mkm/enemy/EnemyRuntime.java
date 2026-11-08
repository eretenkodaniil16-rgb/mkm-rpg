package com.eretenkodaniil.mkm.enemy;

import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.PoiseState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;

/**
 * Load-scoped server tactical memory. Never persisted: Minecraft itself persists
 * entity UUID, attributes, equipment and tags. AI does not force-load chunks.
 */
final class EnemyRuntime {
    private static final double ACQUISITION_RADIUS = 16.0;
    private final PathfinderMob mob;
    private final EnemyDefinition definition;
    private final BlockPos patrolAnchor;
    private final boolean guard;
    private final boolean flanker;
    private final boolean elite;
    private BlockPos lastSeen;
    private long lastSeenTick = Long.MIN_VALUE;
    private double lastX, lastY, lastZ;
    private int stuckTicks;
    private long retreatUntil;
    private long shieldUntil;
    private long pounceReadyTick;
    private long pounceWindupUntil;
    private boolean pounceWindingUp;
    private long nextAllyAlertTick;

    EnemyRuntime(PathfinderMob mob, EnemyDefinition definition) {
        this.mob = mob;
        this.definition = definition;
        this.patrolAnchor = mob.blockPosition();
        this.guard = mob.entityTags().contains(EnemyFactory.GUARD_TAG);
        this.flanker = mob.entityTags().contains(EnemyFactory.FLANK_TAG);
        this.elite = mob.entityTags().contains(EnemyFactory.ELITE_STALKER_TAG);
        this.lastX = mob.getX();
        this.lastY = mob.getY();
        this.lastZ = mob.getZ();
    }

    PathfinderMob mob() { return mob; }

    /** First hit immediately alerts allies; repeats are limited to once per 60 ticks. */
    boolean shouldShareAlert(long now) {
        if (now < nextAllyAlertTick) return false;
        nextAllyAlertTick = now + 60L;
        return true;
    }

    void onAttackedBy(ServerPlayer attacker, long tick) {
        if (!attacker.isAlive() || attacker.isSpectator()) return;
        mob.setTarget(attacker);
        lastSeen = attacker.blockPosition();
        lastSeenTick = tick;
    }

    void tick(ServerLevel level, long now) {
        if (!EnemyTactics.shouldRepath(now, mob.getId(), 5)) return;
        PoiseState poise = mob.getAttached(CombatAttachments.POISE);
        if (poise != null && now < poise.staggeredUntilTick()) {
            mob.getNavigation().stop();
            if (mob.isUsingItem()) mob.stopUsingItem();
            pounceWindingUp = false;
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target instanceof ServerPlayer player
                && (!player.isAlive() || player.isSpectator() || player.distanceToSqr(mob) > 28 * 28)) {
            mob.setTarget(null);
            target = null;
        }
        // Keep a wounded raider from instantly reacquiring during its retreat.
        if (target == null && now >= retreatUntil
                && EnemyTactics.shouldRepath(now, mob.getId(), 20)) {
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
        if (target != null && !visible && !memory) {
            mob.setTarget(null);
            target = null;
        }
        if (EnemyTactics.shouldRepath(now, mob.getId(), 10)) {
            double moved = (mob.getX() - lastX) * (mob.getX() - lastX)
                    + (mob.getY() - lastY) * (mob.getY() - lastY)
                    + (mob.getZ() - lastZ) * (mob.getZ() - lastZ);
            stuckTicks = !mob.getNavigation().isDone() && moved < 0.09
                    ? Math.min(60, stuckTicks + 10) : 0;
            lastX = mob.getX();
            lastY = mob.getY();
            lastZ = mob.getZ();
        }
        double distance = target == null ? Double.MAX_VALUE : mob.distanceToSqr(target);
        double healthRatio = mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
        EnemyTactics.Decision decision = EnemyTactics.decide(
                definition.host(), target != null || memory, visible, healthRatio,
                age, distance, stuckTicks);

        if (definition.host() == EnemyDefinition.Host.SPIDER) {
            spiderTactics(level, now, target, visible, distance);
        } else {
            raiderGuard(level, now, target, visible, distance);
        }

        switch (decision) {
            case RECOVER_PATH -> {
                mob.getNavigation().stop();
                stuckTicks = 0;
                if (lastSeen != null && memory) moveToward(lastSeen, 0.85);
                else patrol(now);
            }
            case PATROL -> {
                if (mob.getTarget() == null && mob.getNavigation().isDone()
                        && EnemyTactics.shouldRepath(now, mob.getId(), 40)) patrol(now);
            }
            case INVESTIGATE -> {
                if (!visible && target != null) mob.setTarget(null);
                if (lastSeen != null && EnemyTactics.shouldRepath(now, mob.getId(), 20))
                    moveToward(lastSeen, 1.0);
            }
            case RETREAT -> {
                if (now >= retreatUntil) retreatUntil = now + 70L;
                mob.setTarget(null);
                if (EnemyTactics.shouldRepath(now, mob.getId(), 20)) {
                    double threatX = target != null && visible ? target.getX()
                            : lastSeen != null ? lastSeen.getX() + 0.5 : patrolAnchor.getX();
                    double threatZ = target != null && visible ? target.getZ()
                            : lastSeen != null ? lastSeen.getZ() + 0.5 : patrolAnchor.getZ();
                    double dx = mob.getX() - threatX, dz = mob.getZ() - threatZ;
                    double length = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                    mob.getNavigation().moveTo(mob.getX() + 6.0 * dx / length,
                            mob.getY(), mob.getZ() + 6.0 * dz / length, 1.12);
                }
            }
            case FLANK -> {
                if (target != null && !pounceWindingUp
                        && EnemyTactics.shouldRepath(now, mob.getId(), 30))
                    orbit(target, 3.0, 1.17);
            }
            case CHASE -> {
                if (flanker && target != null && visible && distance > 9.0
                        && EnemyTactics.shouldRepath(now, mob.getId(), 20)
                        && EnemyService.hasNearbyGuard(level, mob))
                    orbit(target, 2.5, 1.0);
                // Otherwise native melee goal retains ownership of combat and navigation.
            }
            case ATTACK -> { /* Vanilla attack goal, never an animation-triggered hit. */ }
        }
    }

    private void raiderGuard(ServerLevel level, long now, LivingEntity target,
            boolean visible, double distance) {
        if (!guard) return;
        if (mob.isUsingItem() && (now >= shieldUntil || target == null || !visible)) {
            mob.stopUsingItem();
        }
        if (target == null || !visible || distance > 22.0 || now < shieldUntil
                || !mob.getOffhandItem().is(Items.SHIELD)) return;
        if (EnemyTactics.shouldRepath(now, mob.getId(), 80)) {
            shieldUntil = now + 24L;
            mob.startUsingItem(InteractionHand.OFF_HAND);
            level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY() + 1.2,
                    mob.getZ(), 6, 0.15, 0.3, 0.15, 0.02);
        }
    }

    private void spiderTactics(ServerLevel level, long now, LivingEntity target,
            boolean visible, double distance) {
        if (target == null || !visible) {
            pounceWindingUp = false;
            return;
        }
        // Native block-light value; the Stalker does not track any scent or sound.
        // Bright torch-lit locations suppress the ambush leap and encourage circling.
        int blockLight = level.getBrightness(LightLayer.BLOCK, mob.blockPosition());
        EnemyService.recordLightSample();
        boolean shadow = EnemyShowcaseRules.shadowEnough(blockLight);
        if (!shadow && !pounceWindingUp && distance >= 9.0
                && EnemyTactics.shouldRepath(now, mob.getId(), 30)) {
            orbit(target, 4.5, 0.95);
            return;
        }

        if (pounceWindingUp) {
            if (now < pounceWindupUntil) {
                level.sendParticles(elite ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
                        mob.getX(), mob.getY() + 0.65, mob.getZ(),
                        elite ? 5 : 3, 0.45, 0.2, 0.45, 0.02);
                if (elite && now + 10 >= pounceWindupUntil
                        && EnemyTactics.shouldRepath(now, mob.getId(), 10))
                    orbit(target, 1.6, 1.08); // short visible feint
                return;
            }
            pounceWindingUp = false;
            if (shadow && distance >= 6.25 && distance <= 100.0) {
                // Velocity only: native attack, shields, hit immunity and Combat
                // remain authoritative. The leap does not deal direct damage.
                double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
                double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                mob.push(dx / len * (elite ? 0.90 : 0.68), elite ? 0.24 : 0.15,
                        dz / len * (elite ? 0.90 : 0.68));
                EnemyService.recordPounce();
            }
            return;
        }
        if (!EnemyShowcaseRules.pouncePossible(visible, blockLight, distance,
                now < pounceReadyTick)) return;
        if (EnemyTactics.shouldRepath(now, mob.getId(), 20)) {
            pounceReadyTick = now + (elite ? 110L : 90L);
            pounceWindupUntil = now + (elite ? 20L : 15L);
            pounceWindingUp = true;
            mob.getNavigation().stop();
            level.playSound(null, mob.blockPosition(), SoundEvents.SPIDER_AMBIENT,
                    SoundSource.HOSTILE, 0.8F, elite ? 0.55F : 1.25F);
            level.sendParticles(elite ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + 0.65, mob.getZ(),
                    14, 0.55, 0.35, 0.55, 0.04);
        }
    }

    private ServerPlayer acquireNearbyPlayer(ServerLevel level) {
        EnemyService.recordTargetQuery();
        ServerPlayer nearest = null;
        double best = ACQUISITION_RADIUS * ACQUISITION_RADIUS;
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

    private void orbit(LivingEntity target, double lateral, double speed) {
        double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
        double length = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
        double side = (mob.getId() & 1) == 0 ? 1.0 : -1.0;
        mob.getNavigation().moveTo(target.getX() - side * dz / length * lateral,
                target.getY(), target.getZ() + side * dx / length * lateral, speed);
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
