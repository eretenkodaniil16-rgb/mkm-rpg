package com.eretenkodaniil.mkm.abilities;

import net.minecraft.world.entity.LivingEntity;

/**
 * SINGLE server-authoritative native healing effect shared by players and
 * hostile AI. MNAE APPLIED is based on positive actual health delta only.
 * This class never accepts client-target coordinates or damage events.
 */
public final class RenewalEffect {
    private RenewalEffect() {}

    public static float healSelf(LivingEntity actor, float amount) {
        if (actor == null || !actor.isAlive() || !Float.isFinite(amount) || amount <= 0F)
            return 0F;
        float before = actor.getHealth();
        if (!Float.isFinite(before) || before >= actor.getMaxHealth() - 0.001F) return 0F;
        actor.heal(amount);
        float after = actor.getHealth();
        return Float.isFinite(after) && after > before ? after - before : 0F;
    }
}
