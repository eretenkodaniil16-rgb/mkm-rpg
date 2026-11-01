package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Charges stamina ONLY after a real vanilla shield block was confirmed by AFTER_DAMAGE.
 * The shield still performs the normal vanilla block; depleted guard puts that shield
 * on a server-synchronized cooldown and releases its current use animation.
 */
public final class ShieldGuardService {
    private ShieldGuardService() {}

    public static void onBlocked(ServerPlayer defender, float blockedDamage) {
        ItemStack shield = defender.getUseItem();
        if (!shield.is(Items.SHIELD)) return;

        PlayerModifierSnapshot stats = EquipmentSnapshots.snapshot(
                defender, defender.getAttachedOrCreate(CharacterAttachments.CHARACTER));
        long now = defender.level().getGameTime();
        GuardRules.Result result = GuardRules.block(
                defender.getAttachedOrCreate(CombatAttachments.PLAYER),
                now, Math.max(0.0F, blockedDamage),
                stats.get(PlayerModifierId.BLOCK_STABILITY),
                stats.get(PlayerModifierId.STAMINA_COST),
                (float) stats.get(PlayerModifierId.MAX_STAMINA),
                (float) stats.get(PlayerModifierId.STAMINA_REGENERATION),
                Math.max(1L, Math.round(20.0D
                        * stats.get(PlayerModifierId.STAMINA_REGEN_DELAY))));
        defender.setAttached(CombatAttachments.PLAYER, result.state());
        if (result.broken()) {
            // No damage is retroactively invented after vanilla successfully blocked.
            // Future shield uses are disabled for the cooldown duration.
            defender.stopUsingItem();
            defender.getCooldowns().addCooldown(shield, GuardRules.GUARD_BREAK_COOLDOWN_TICKS);
            defender.sendOverlayMessage(Component.literal("MKM  GUARD BROKEN"));
        }
    }
}
