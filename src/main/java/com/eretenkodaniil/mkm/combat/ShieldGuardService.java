package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Charges stamina ONLY after a real vanilla shield block was confirmed by AFTER_DAMAGE.
 * The shield still performs the normal vanilla block; depleted guard puts that shield
 * on a server-synchronized cooldown and releases its current use animation.
 */
public final class ShieldGuardService {
    private static final ConfirmedBlockLedger<ServerPlayer, DamageSource> LEDGER =
            new ConfirmedBlockLedger<>();

    private ShieldGuardService() {}

    /** Fabric ALLOW_DAMAGE supplies one receipt for each real damage attempt. */
    public static void onDamageAttempt(ServerPlayer defender, DamageSource source) {
        LEDGER.begin(defender, source, defender.level().getGameTime());
    }

    public static void onBlocked(ServerPlayer defender, DamageSource source, float blockedDamage) {
        // Consume BEFORE testing item state so a later callback cannot reuse
        // this attempt, including when the shield was swapped or disabled.
        long now = defender.level().getGameTime();
        if (!LEDGER.consume(defender, source, now)) return;
        if (!Float.isFinite(blockedDamage) || blockedDamage < 0.0F) return;
        ItemStack shield = defender.getUseItem();
        if (!shield.is(Items.SHIELD)) return;

        PlayerModifierSnapshot stats = EquipmentSnapshots.snapshot(
                defender, defender.getAttachedOrCreate(CharacterAttachments.CHARACTER));
        GuardRules.Result result = GuardRules.block(
                defender.getAttachedOrCreate(CombatAttachments.PLAYER),
                now, Math.max(0.0F, blockedDamage),
                stats.get(PlayerModifierId.BLOCK_STABILITY),
                stats.get(PlayerModifierId.STAMINA_COST),
                (float) stats.get(PlayerModifierId.MAX_STAMINA),
                (float) stats.get(PlayerModifierId.STAMINA_REGENERATION),
                Math.max(1L, Math.round(20.0D
                        * stats.get(PlayerModifierId.STAMINA_REGEN_DELAY))),
                GuardCostExperiment.activeFactor());
        defender.setAttached(CombatAttachments.PLAYER, result.state());
        if (result.broken()) {
            // No damage is retroactively invented after vanilla successfully blocked.
            // Future shield uses are disabled for the cooldown duration.
            defender.stopUsingItem();
            defender.getCooldowns().addCooldown(shield, GuardRules.GUARD_BREAK_COOLDOWN_TICKS);
            defender.sendOverlayMessage(Component.literal("MKM  GUARD BROKEN"));
        }
    }

    public static void onDisconnect(ServerPlayer defender) {
        LEDGER.clear(defender);
    }
}
