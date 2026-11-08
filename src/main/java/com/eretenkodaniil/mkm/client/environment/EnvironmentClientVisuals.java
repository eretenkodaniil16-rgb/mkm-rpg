package com.eretenkodaniil.mkm.client.environment;

import com.eretenkodaniil.mkm.environment.EnvironmentVfxBudget;
import com.eretenkodaniil.mkm.environment.EnvironmentVisualSettings;
import com.eretenkodaniil.mkm.environment.net.EnvironmentVisualEvent;
import com.eretenkodaniil.mkm.environment.net.EnvironmentRuneEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import com.eretenkodaniil.mkm.registry.MkmBlocks;
import net.minecraft.world.entity.Entity;

/**
 * Engine particles only. Per-client configurable, range culled and non-authoritative.
 * No custom core shaders, screen overlays, flashing, camera displacement, or AI writes.
 */
public final class EnvironmentClientVisuals {
    private EnvironmentClientVisuals() {}

    public static void acceptRune(Minecraft client, EnvironmentRuneEvent event) {
        if (client.level == null || client.player == null) return;
        BlockPos pos = new BlockPos(event.x(), event.y(), event.z());
        if (client.player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5)
                > 32.0D * 32.0D || !client.level.getBlockState(pos).is(MkmBlocks.RUNE_SLATE))
            return;
        int count = EnvironmentVfxBudget.particles(
                EnvironmentVisualSettings.profile(),
                EnvironmentVisualSettings.reducedEffects(), EnvironmentVfxBudget.RUNE_PULSE);
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * (3.0 - Math.sqrt(5.0));
            client.level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + .5 + Math.cos(a) * .35,
                    pos.getY() + 1.1 + .08 * (i % 3),
                    pos.getZ() + .5 + Math.sin(a) * .35,
                    Math.cos(a) * .025, .03, Math.sin(a) * .025);
        }
    }

    public static void accept(Minecraft client, EnvironmentVisualEvent event) {
        if (client.level == null || client.player == null
                || !EnvironmentVfxBudget.known(event.cue())) return;
        Entity entity = client.level.getEntity(event.entityId());
        if (entity == null || !entity.isAlive() || client.player.distanceToSqr(entity) > 32 * 32)
            return;
        int count = EnvironmentVfxBudget.particles(
                EnvironmentVisualSettings.profile(),
                EnvironmentVisualSettings.reducedEffects(), event.cue());
        double spread = Math.min(0.72, Math.max(0.30, entity.getBbWidth() * 0.4));
        for (int i = 0; i < count; i++) {
            double theta = (i * 2.399963229728653) + (client.level.getGameTime() % 16) * 0.30;
            double dx = Math.cos(theta), dz = Math.sin(theta);
            double y = entity.getY() + Math.min(entity.getBbHeight() - 0.1,
                    0.55 + (i % 3) * 0.26);
            if (event.cue() == EnvironmentVfxBudget.STALKER_AMBIENT
                    || event.cue() == EnvironmentVfxBudget.STALKER_HIT) {
                client.level.addParticle(ParticleTypes.PORTAL,
                        entity.getX() + dx * spread, y, entity.getZ() + dz * spread,
                        dx * 0.015, 0.025, dz * 0.015);
            } else {
                client.level.addParticle(ParticleTypes.END_ROD,
                        entity.getX() + dx * spread, y, entity.getZ() + dz * spread,
                        dx * 0.014, 0.02, dz * 0.014);
            }
        }
    }
}
