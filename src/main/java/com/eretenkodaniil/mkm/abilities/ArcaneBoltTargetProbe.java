package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-only, read-only target acquisition prototype for Arcane Bolt.
 *
 * A Combat-owned permission checker is REQUIRED. The first living body in the
 * ray stops targeting even if friendly or invulnerable: no shooting through
 * protected players. Blocks also occlude targets. No damage, mana or cooldown.
 */
public final class ArcaneBoltTargetProbe {
    private static final double BOUNDS_PADDING = 0.1D;
    private static final double EPSILON = 1.0E-6D;

    private ArcaneBoltTargetProbe() {}

    public static Optional<LivingEntity> acquire(
            ServerPlayer caster,
            PlayerModifierSnapshot stats,
            Predicate<LivingEntity> combatPermission) {
        if (caster == null || stats == null || combatPermission == null
                || !caster.isAlive() || caster.isSpectator() || caster.isPassenger()) {
            return Optional.empty();
        }
        double multiplier = stats.get(PlayerModifierId.SPELL_RANGE);
        double range = ArcaneBoltTargetRules.BASE_RANGE_BLOCKS * multiplier;
        if (!Double.isFinite(range) || range <= 0.0D || range > 64.0D) {
            return Optional.empty();
        }

        Vec3 start = caster.getEyePosition();
        Vec3 direction = caster.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(range));
        HitResult block = caster.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double occlusionDistance = block.getType() == HitResult.Type.MISS
                ? range * range
                : start.distanceToSqr(block.getLocation());

        LivingEntity nearest = null;
        double bestDistance = Double.MAX_VALUE;

        // A short, bounded world query occurs only on a requested spell cast,
        // not every player tick. Resolve the closest physical intersection.
        for (Entity entity : caster.level().getEntities(
                caster, caster.getBoundingBox().expandTowards(
                        direction.scale(range)).inflate(1.0D),
                candidate -> candidate instanceof LivingEntity
                        && candidate.isPickable() && candidate.isAlive())) {
            if (!(entity instanceof LivingEntity living)) continue;
            var hit = living.getBoundingBox().inflate(BOUNDS_PADDING).clip(start, end);
            if (hit.isEmpty()) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance <= EPSILON || distance > occlusionDistance + EPSILON
                    || distance >= bestDistance) continue;
            nearest = living;
            bestDistance = distance;
        }
        if (nearest == null) return Optional.empty();

        boolean allowed = combatPermission.test(nearest);
        var facts = new ArcaneBoltTargetRules.TargetFacts(
                caster.isAlive(), nearest.isAlive(), nearest == caster,
                caster.level() == nearest.level(), allowed,
                bestDistance <= occlusionDistance + EPSILON, true, bestDistance);
        return ArcaneBoltTargetRules.eligible(facts, multiplier)
                ? Optional.of(nearest) : Optional.empty();
    }
}
