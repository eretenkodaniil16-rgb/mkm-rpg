package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only activation gate. Clients supply only an ability ID, never damage,
 * healing, resources, target coordinates or modified player attributes.
 */
public final class AbilityService {
    private static final long SYNC_INTERVAL_TICKS = 5L;

    private AbilityService() {}

    public static void onLogin(ServerPlayer player) {
        if (player.getAttached(AbilityAttachments.PLAYER) == null) {
            PlayerModifierSnapshot stats = stats(player);
            long now = player.level().getGameTime();
            player.setAttached(AbilityAttachments.PLAYER, new ManaLedger(
                    stats.get(PlayerModifierId.MAX_MANA), now, now,
                    java.util.Map.of()));
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
        // Avoid sending packets just for advancing the accounting timestamp.
        if (Math.abs(previous.mana() - updated.mana()) > 0.001D) {
            player.setAttached(AbilityAttachments.PLAYER, updated);
        }
    }

    public static boolean tryCast(ServerPlayer player, String id) {
        AbilityDefinition ability = AbilityCatalog.get(id);
        // Only Renewal is implemented. Inactive prototypes are not activatable.
        if (ability != AbilityCatalog.RENEWAL || ability.kind() != AbilityDefinition.Kind.ACTIVE
                || ability.targetMode() != AbilityDefinition.TargetMode.SELF
                || ability.effect() != AbilityDefinition.Effect.HEAL
                || ability.resource() != AbilityDefinition.Resource.MANA
                || ability.castTicks() != 0
                || !player.isAlive() || player.isSpectator() || player.isPassenger()
                || !player.getAttachedOrCreate(CombatAttachments.TIMELINE).idle()) {
            return false;
        }

        // Effect precondition is checked before taking any mana or cooldown.
        if (player.getHealth() >= player.getMaxHealth() - 0.001F) return false;

        long now = player.level().getGameTime();
        PlayerModifierSnapshot stats = stats(player);
        double cost = AbilityRules.scaledCost(ability.baseCost(),
                stats.get(PlayerModifierId.MANA_COST));
        ManaLedger previous = player.getAttachedOrCreate(AbilityAttachments.PLAYER);
        ManaLedger current = AbilityRules.regenerate(previous, now,
                stats.get(PlayerModifierId.MAX_MANA),
                stats.get(PlayerModifierId.MANA_REGENERATION));
        ManaLedger paid = AbilityRules.spend(current, ability, now, cost,
                AbilityRules.scaledCooldown(ability.cooldownTicks(),
                        stats.get(PlayerModifierId.COOLDOWN_RECOVERY)),
                AbilityRules.regenDelayTicks(stats.get(PlayerModifierId.MANA_REGEN_DELAY)));
        if (paid == null) return false;

        // Reuses vanilla healing and the existing snapshot; there is no second
        // damage system or invented primary-attribute formula in this domain.
        double amount = ability.baseMagnitude()
                * stats.get(PlayerModifierId.HEALING_POWER)
                * stats.get(PlayerModifierId.HEALING_RECEIVED);
        if (!Double.isFinite(amount) || amount <= 0) return false;
        player.setAttached(AbilityAttachments.PLAYER, paid);
        player.heal((float) amount);
        player.sendOverlayMessage(Component.translatable("message.mkm.renewal.cast"));
        return true;
    }

    private static PlayerModifierSnapshot stats(ServerPlayer player) {
        return PlayerModifierCalculator.calculate(CharacterService.get(player));
    }
}
