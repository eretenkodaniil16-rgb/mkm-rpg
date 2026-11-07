package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.abilities.AbilityGrantPolicy.Grants;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

/**
 * Opt-in server-only hostile healer prototype. No new Mob AI renderer,
 * no attack/DamageSource, no class/CharacterData fields. Use vanilla /tag
 * to opt ONE existing hostile mob into the prototype.
 *
 * Bounded by 20-tick cadence, 16-block vicinity and a per-player candidate cap.
 * Native monster AI still owns target selection, pathing and all attacks.
 */
public final class HostileRenewalService {
    public static final String OPT_IN_TAG = "mkm_arcane_healer";
    private static final int MAX_CANDIDATES_PER_PLAYER = 32;
    private static final long TICK_INTERVAL = 20L;
    private static final double THREAT_RADIUS = 16.0D;
    private static final RenewalParameters PROTOTYPE_TERMS = new RenewalParameters(
            100.0D, 1.0D, 40L, AbilityCatalog.RENEWAL.baseCost(),
            AbilityCatalog.RENEWAL.cooldownTicks(),
            (float) AbilityCatalog.RENEWAL.baseMagnitude());

    private HostileRenewalService() {}

    public static void onServerTick(MinecraftServer server) {
        if (server.getTickCount() % TICK_INTERVAL != 0) return;
        Set<UUID> processed = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            int inspected = 0;
            for (Monster caster : player.level().getEntitiesOfClass(
                    Monster.class, player.getBoundingBox().inflate(THREAT_RADIUS),
                    mob -> mob.getScoreboardTags().contains(OPT_IN_TAG))) {
                if (++inspected > MAX_CANDIDATES_PER_PLAYER) break;
                if (caster.getTarget() != player || !processed.add(caster.getUUID())) continue;
                tryCast(caster, player);
            }
        }
    }

    private static void tryCast(Monster caster, ServerPlayer target) {
        boolean allowed = HostileRenewalRules.allowed(new HostileRenewalRules.Facts(
                caster.getScoreboardTags().contains(OPT_IN_TAG), true,
                caster.isAlive() && !caster.isPassenger() && !caster.isNoAi(),
                caster.getTarget() == target, target.isAlive() && !target.isSpectator(),
                caster.level() == target.level(),
                caster.hasLineOfSight(target), !caster.isUsingItem(),
                caster.getHealth() < caster.getMaxHealth() - 0.001F,
                caster.distanceToSqr(target)));
        if (!allowed || !AbilityGrantPolicy.executable(
                Grants.taggedHostileRenewal(), AbilityCatalog.RENEWAL.id())) return;

        long now = caster.level().getGameTime();
        ManaLedger previous = caster.getAttachedOrCreate(AbilityAttachments.HOSTILE);
        ManaLedger paid = RenewalCastPlan.spendIfAllowed(
                caster.isAlive(), true, caster.getHealth() < caster.getMaxHealth() - 0.001F,
                previous, now, PROTOTYPE_TERMS);
        if (paid == null) return;

        // Commit resource/cooldown BEFORE one native self-heal. Duplicated
        // per-player scans or repeated AI decisions now fail cooldown.
        caster.setAttached(AbilityAttachments.HOSTILE, paid);
        if (RenewalEffect.healSelf(caster, PROTOTYPE_TERMS.healing()) > 0F) {
            HostileRenewalVisuals.onApplied(caster);
        }
    }
}
