package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only activation authority. No client-supplied effect amount, position,
 * resource cost, damage or primary attribute is accepted.
 */
public final class AbilityService {
    private static final long SYNC_INTERVAL_TICKS = 5L;

    private AbilityService() {}

    public static void onLogin(ServerPlayer player) {
        if (player.getAttached(AbilityAttachments.PLAYER) == null) {
            PlayerModifierSnapshot stats = stats(player);
            long now = player.level().getGameTime();
            player.setAttached(AbilityAttachments.PLAYER, new ManaLedger(
                    stats.get(PlayerModifierId.MAX_MANA), now, now, java.util.Map.of()));
        }
    }

    public static void onPlayerTick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now % SYNC_INTERVAL_TICKS != 0L) return;

        PlayerModifierSnapshot stats = stats(player);
        ManaLedger previous = player.getAttachedOrCreate(AbilityAttachments.PLAYER);
        ManaLedger updated = AbilityRules.regenerate(previous, now,
                stats.get(PlayerModifierId.MAX_MANA),
                stats.get(PlayerModifierId.MANA_REGENERATION));

        // The bookkeeping cursor alone does not justify an attachment sync.
        // A backwards clock, however, must persist rebased cooldowns and delays.
        if (Math.abs(previous.mana() - updated.mana()) > 0.001D
                || now < previous.lastUpdateTick()
                || previous.regenReadyTick() != updated.regenReadyTick()
                || !previous.cooldowns().equals(updated.cooldowns())) {
            player.setAttached(AbilityAttachments.PLAYER, updated);
        }
    }

    public static boolean tryCast(ServerPlayer player, String id) {
        // Hard effect allowlist. Other catalog entries are design prototypes.
        if (!AbilityCatalog.RENEWAL.id().equals(id)) return false;

        long now = player.level().getGameTime();
        boolean actorValid = player.isAlive() && !player.isSpectator() && !player.isPassenger();
        CombatState combat = player.getAttachedOrCreate(CombatAttachments.PLAYER);
        boolean actionIdle = player.getAttachedOrCreate(CombatAttachments.TIMELINE).idle()
                && now >= combat.heavyReadyTick();
        boolean injured = player.getHealth() < player.getMaxHealth() - 0.001F;
        if (!actorValid || !actionIdle || !injured) return false;

        PlayerModifierSnapshot stats = stats(player);
        double amount = AbilityCatalog.RENEWAL.baseMagnitude()
                * stats.get(PlayerModifierId.HEALING_POWER)
                * stats.get(PlayerModifierId.HEALING_RECEIVED);
        if (!Double.isFinite(amount) || amount <= 0.0D
                || amount > Float.MAX_VALUE) return false;

        double cost = AbilityRules.scaledCost(AbilityCatalog.RENEWAL.baseCost(),
                stats.get(PlayerModifierId.MANA_COST));
        ManaLedger previous = player.getAttachedOrCreate(AbilityAttachments.PLAYER);
        ManaLedger current = AbilityRules.regenerate(previous, now,
                stats.get(PlayerModifierId.MAX_MANA),
                stats.get(PlayerModifierId.MANA_REGENERATION));
        if (RenewalEligibility.check(actorValid, actionIdle, injured, current, now, cost)
                != RenewalEligibility.Result.READY) return false;

        ManaLedger paid = AbilityRules.spend(
                current, AbilityCatalog.RENEWAL, now, cost,
                AbilityRules.scaledCooldown(AbilityCatalog.RENEWAL.cooldownTicks(),
                        stats.get(PlayerModifierId.COOLDOWN_RECOVERY)),
                AbilityRules.regenDelayTicks(stats.get(PlayerModifierId.MANA_REGEN_DELAY)));
        if (paid == null) return false;

        // Commit exactly once before the native healing effect. Retransmitted
        // packets see the updated cooldown on the next server-side invocation.
        player.setAttached(AbilityAttachments.PLAYER, paid);
        player.heal((float) amount);
        player.sendOverlayMessage(Component.translatable("message.mkm.renewal.cast"));
        return true;
    }

    private static PlayerModifierSnapshot stats(ServerPlayer player) {
        return PlayerModifierCalculator.calculate(CharacterService.get(player));
    }
}
