package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.HitFeedbackRules;
import com.eretenkodaniil.mkm.client.accessibility.MkmAccessibility;
import com.eretenkodaniil.mkm.combat.net.HitConfirm;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Owner-local cosmetic response to a confirmed server hit. Animation hit
 * markers alone must never trigger this effect (misses and blocks are silent).
 */
public final class HitFeedbackParticles {
    private HitFeedbackParticles() {}

    public static void onConfirmedHit(Minecraft client, HitConfirm confirmation) {
        if (client.level == null || client.player == null
                || !HitFeedbackRules.validConfirmedHit(
                        confirmation.targetEntityId(), confirmation.damageTaken())) return;
        Entity target = client.level.getEntity(confirmation.targetEntityId());
        if (target == null || client.player.distanceToSqr(target) > 48D * 48D) return;

        Vec3 point = target.getBoundingBox().getCenter();
        var visual = MkmAccessibility.get().vfx();
        if (visual == MkmAccessibility.Vfx.OFF) return;
        int count = HitFeedbackRules.particleCount(
                confirmation.damageTaken(), confirmation.staggered());
        if (visual == MkmAccessibility.Vfx.REDUCED) count = Math.min(count, 2);
        if (count <= 0) return;
        // Deterministic radial hit burst is cheaper than a looping effect.
        // No particle sends back damage or alters movement/collision.
        double radius = Math.min(.36D, Math.max(.15D, target.getBbWidth() * .35D));
        for (int i = 0; i < count; i++) {
            double phi = 2D * Math.PI * i / count;
            double dx = Math.cos(phi);
            double dz = Math.sin(phi);
            double rise = (i % 3 - 1) * .09D;
            client.level.addParticle(ParticleTypes.CRIT,
                    point.x + radius * dx, point.y + rise, point.z + radius * dz,
                    dx * .035D, .02D + .008D * (i % 3), dz * .035D);
        }
    }
}
