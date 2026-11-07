package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEmitter;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatState;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only activation authority. No client-supplied effect amount, position,
 * resource cost, damage or primary attribute is accepted.
 */
public final class AbilityService {
    private static final long SYNC_INTERVAL_TICKS = 5L;
    private static final Map<ServerPlayer, Long> LAST_REQUEST_TICK = new WeakHashMap<>();

    private AbilityService() {}

    public static void onLogin(ServerPlayer player) {
        LAST_REQUEST_TICK.remove(player);
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
        // Player grants are independent of the AI's opt-in permission.
        // Existing public Renewal remains available before Character Core
        // implements class choice; other catalog entries stay nonexecutable.
        if (!AbilityGrantPolicy.executable(
                AbilityGrantPolicy.Grants.currentPlayer(), id)) return false;

        long now = player.level().getGameTime();
        // Limit malformed/repeated client intents to one attempt per server
        // tick for this actor. Persistent cooldown is the final authority.
        Long last = LAST_REQUEST_TICK.get(player);
        if (last != null && last == now) return false;
        LAST_REQUEST_TICK.put(player, now);
        boolean actorValid = player.isAlive() && !player.isSpectator() && !player.isPassenger();
        CombatState combat = player.getAttachedOrCreate(CombatAttachments.PLAYER);
        boolean actionIdle = player.getAttachedOrCreate(CombatAttachments.TIMELINE).idle()
                && now >= combat.heavyReadyTick();
        boolean injured = player.getHealth() < player.getMaxHealth() - 0.001F;
        if (!actorValid || !actionIdle || !injured) return false;

        PlayerModifierSnapshot stats = stats(player);
        RenewalParameters parameters = RenewalParameters.from(stats);
        ManaLedger previous = player.getAttachedOrCreate(AbilityAttachments.PLAYER);
        ManaLedger paid = RenewalCastPlan.spendIfAllowed(
                actorValid, actionIdle, injured, previous, now, parameters);
        if (paid == null) return false;

        // The same spend plan and native heal effect are used by opt-in AI.
        // Commit before effect so a retransmitted intent cannot double-cast.
        player.setAttached(AbilityAttachments.PLAYER, paid);
        float beforeHealth = player.getHealth();
        float actualHealed = RenewalEffect.healSelf(player, parameters.healing());
        if (actualHealed <= 0F) return false; // no successful APPLIED
        AbilityVisualEmitter.onRenewalApplied(player, beforeHealth);
        player.sendOverlayMessage(Component.translatable("message.mkm.renewal.cast"));
        return true;
    }

    private static PlayerModifierSnapshot stats(ServerPlayer player) {
        return AbilityModifierAccess.snapshot(player);
    }
}
