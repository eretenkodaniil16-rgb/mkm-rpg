package com.eretenkodaniil.mkm.client.abilities;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Small one-shot vanilla particle fallback, not an animation engine.
 * MNAE takes over gesture/glow/particles via AbilityVisualInbox listener.
 */
public final class RenewalSparkles {
    private RenewalSparkles() {}

    public static void show(Minecraft client) {
        if (client.level == null || client.player == null) return;
        double x = client.player.getX();
        double y = client.player.getY() + client.player.getBbHeight() * 0.65D;
        double z = client.player.getZ();
        for (int i = 0; i < 8; i++) {
            double a = (2D * Math.PI * i) / 8D;
            double dx = Math.cos(a);
            double dz = Math.sin(a);
            client.level.addParticle(ParticleTypes.END_ROD,
                    x + 0.35D * dx, y + (i % 3) * 0.12D, z + 0.35D * dz,
                    0.012D * dx, 0.028D, 0.012D * dz);
        }
    }
}
