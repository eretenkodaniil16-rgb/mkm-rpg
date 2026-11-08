package com.eretenkodaniil.mkm.abilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * A real native damage-absorption ward, NOT direct magical damage. The vanilla
 * effect is respected by Minecraft's existing health and Combat damage chain.
 */
public final class ArcaneWardEffect {
    public static final int DURATION_TICKS = 160;
    public static final int AMPLIFIER = 0; // 4 absorption HP, fixed no stacking

    private ArcaneWardEffect() {}

    public static boolean eligible(boolean actorAllowed, boolean alreadyWardProtected) {
        return actorAllowed && !alreadyWardProtected;
    }

    public static boolean canApply(ServerPlayer player) {
        return player != null && player.isAlive()
                && !player.hasEffect(MobEffects.ABSORPTION);
    }

    /** Called only after server resource debit and readiness checks. */
    public static boolean apply(ServerPlayer player) {
        if (!canApply(player)) return false;
        return player.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION, DURATION_TICKS, AMPLIFIER,
                false, true), player);
    }
}
