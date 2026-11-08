package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEmitter;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginService;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatState;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Magic Showcase / CharacterData v4. One server effect executor with
 * grant + combat + resource + native application. No damage shortcuts.
 * Never trusts class, target, mana, effect strength or level from C2S.
 */
public final class AbilityService {
    private static final long SYNC_INTERVAL_TICKS = 5L;
    private static final Map<ServerPlayer, AbilityRequestSequenceGate> REQUESTS =
            new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> LAST_ATTEMPT = new WeakHashMap<>();

    private AbilityService() {}

    public static void onLogin(ServerPlayer player) {
        REQUESTS.remove(player);
        LAST_ATTEMPT.remove(player);
        if (player.getAttached(AbilityAttachments.PLAYER) == null) {
            if (CharacterService.get(player).schemaVersion()
                    != CharacterData.CURRENT_SCHEMA_VERSION) return;
            var stats = AbilityModifierAccess.snapshot(player);
            long now = player.level().getGameTime();
            player.setAttached(AbilityAttachments.PLAYER, new ManaLedger(
                    stats.get(PlayerModifierId.MAX_MANA), now, now, Map.of()));
        }
    }

    public static void onPlayerTick(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now % SYNC_INTERVAL_TICKS != 0L
                || CharacterService.get(player).schemaVersion()
                    != CharacterData.CURRENT_SCHEMA_VERSION) return;

        var stats = AbilityModifierAccess.snapshot(player);
        var previous = player.getAttachedOrCreate(AbilityAttachments.PLAYER);
        var updated = AbilityRules.regenerate(previous, now,
                stats.get(PlayerModifierId.MAX_MANA),
                stats.get(PlayerModifierId.MANA_REGENERATION));
        if (Math.abs(previous.mana() - updated.mana()) > 0.001D
                || now < previous.lastUpdateTick()
                || previous.regenReadyTick() != updated.regenReadyTick()
                || !previous.cooldowns().equals(updated.cooldowns())) {
            player.setAttached(AbilityAttachments.PLAYER, updated);
        }
    }

    public static boolean tryCast(ServerPlayer player, String id, int sequence) {
        if (player == null || id == null
                || CharacterService.get(player).schemaVersion()
                    != CharacterData.CURRENT_SCHEMA_VERSION) return false;
        var licenses = SpellLicenseService.heldResearchLicenses(player);
        AbilityGrantPolicy.Grants grants = AbilityGrantPolicy.fromSaved(
                CharacterService.get(player), CharacterOriginService.get(player), licenses);
        if (!AbilityGrantPolicy.executable(grants, id)) return false;

        // The server consumes an attempted sequence even when the actor is
        // busy or low on mana. A reordered old packet can never cast later.
        var gate = REQUESTS.computeIfAbsent(player, ignored -> new AbilityRequestSequenceGate());
        if (!gate.accept(sequence)) return false;
        long now = player.level().getGameTime();
        if (LAST_ATTEMPT.getOrDefault(player, -1L) == now) return false;
        LAST_ATTEMPT.put(player, now);

        boolean actorValid = player.isAlive() && !player.isSpectator()
                && !player.isPassenger();
        CombatState combat = player.getAttachedOrCreate(CombatAttachments.PLAYER);
        boolean actionIdle = player.getAttachedOrCreate(CombatAttachments.TIMELINE).idle()
                && now >= combat.heavyReadyTick();
        boolean effectAllowed = switch (id) {
            case "mkm:renewal" ->
                    player.getHealth() < player.getMaxHealth() - 0.001F;
            case "mkm:arcane_barrier" -> ArcaneWardEffect.canApply(player);
            default -> false;
        };
        if (!actorValid || !actionIdle || !effectAllowed) return false;

        PlayerModifierSnapshot stats = AbilityModifierAccess.snapshot(player);
        var previous = player.getAttachedOrCreate(AbilityAttachments.PLAYER);
        var ability = AbilityCatalog.get(id);
        var paid = AbilityCastPlan.spendIfAllowed(
                ability, actorValid, actionIdle, effectAllowed, previous, now,
                stats.get(PlayerModifierId.MAX_MANA),
                stats.get(PlayerModifierId.MANA_REGENERATION),
                stats.get(PlayerModifierId.MANA_COST)
                        * ("mkm:arcane_barrier".equals(id)
                            ? SpellLicenseService.offClassManaMultiplier(
                                    grants.classId(), licenses.contains(id)) : 1D),
                stats.get(PlayerModifierId.COOLDOWN_RECOVERY),
                stats.get(PlayerModifierId.MANA_REGEN_DELAY));
        if (paid == null) return false;

        // One committed debit/cooldown; particles and MNAE receipt follow
        // ONLY a real successful server-native effect.
        player.setAttached(AbilityAttachments.PLAYER, paid);
        if (AbilityCatalog.RENEWAL.id().equals(id)) {
            float before = player.getHealth();
            float amount = (float) (ability.baseMagnitude()
                    * stats.get(PlayerModifierId.HEALING_POWER)
                    * stats.get(PlayerModifierId.HEALING_RECEIVED));
            if (RenewalEffect.healSelf(player, amount) <= 0F) return false;
            AbilityVisualEmitter.onRenewalApplied(player, before);
            player.sendOverlayMessage(Component.translatable("message.mkm.renewal.cast"));
            return true;
        }

        if ("mkm:arcane_barrier".equals(id)) {
            boolean applied = ArcaneWardEffect.apply(player);
            if (!applied) return false;
            AbilityVisualEmitter.onWardApplied(player, true);
            player.sendOverlayMessage(Component.translatable("message.mkm.arcane_ward.cast"));
            return true;
        }
        return false;
    }
}
