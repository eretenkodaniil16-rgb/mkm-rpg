package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.abilities.AbilityAttachments;
import com.eretenkodaniil.mkm.abilities.AbilityCatalog;
import com.eretenkodaniil.mkm.abilities.AbilityModifierAccess;
import com.eretenkodaniil.mkm.abilities.AbilityRules;
import com.eretenkodaniil.mkm.abilities.RenewalParameters;
import com.eretenkodaniil.mkm.abilities.ManaLedger;
import com.eretenkodaniil.mkm.abilities.RenewalEligibility;
import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Owner-synced information only; never infers cast success or mutates game state.
 * Uses the same pure eligibility rules as the authoritative server.
 */
public final class MkmAbilityHud {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 7;

    private MkmAbilityHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null
                || client.gui.screen() != null || client.player.isSpectator()) return;

        ManaLedger attached = client.player.getAttached(AbilityAttachments.PLAYER);
        var character = client.player.getAttached(CharacterAttachments.CHARACTER);
        var timeline = client.player.getAttached(CombatAttachments.TIMELINE);
        var combat = client.player.getAttached(CombatAttachments.PLAYER);
        if (attached == null || character == null || timeline == null || combat == null) return;

        var stats = AbilityModifierAccess.snapshot(client.player);
        var parameters = RenewalParameters.from(stats);
        double maximum = stats.get(PlayerModifierId.MAX_MANA);
        double ratio = maximum > 0D ? Math.clamp(attached.mana() / maximum, 0D, 1D) : 0D;
        int x = (client.getWindow().getGuiScaledWidth() - WIDTH) / 2;
        int y = Math.max(18, client.getWindow().getGuiScaledHeight() - 113);

        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xC8000000);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xCC1D2033);
        graphics.fill(x, y, x + (int) Math.round(WIDTH * ratio), y + HEIGHT, 0xFF827BD0);
        graphics.text(client.font, Component.translatable("hud.mkm.mana",
                        Math.round(attached.mana()), Math.round(maximum)),
                x, y - 12, 0xFFE7E9EF);

        long now = client.level.getGameTime();
        ManaLedger ledger = AbilityRules.reconcileClock(attached, now);
        long remaining = Math.max(0L, ledger.readyTick(AbilityCatalog.RENEWAL.id()) - now);
        double cost = parameters.cost();
        boolean actorValid = client.player.isAlive() && !client.player.isPassenger();
        boolean actionIdle = timeline.idle() && now >= combat.heavyReadyTick();
        boolean injured = client.player.getHealth() < client.player.getMaxHealth() - 0.001F;
        var result = RenewalEligibility.check(actorValid, actionIdle, injured, ledger, now, cost);
        Component status = switch (result) {
            case READY -> Component.translatable("hud.mkm.renewal.ready");
            case INVALID_ACTOR, BUSY -> Component.translatable("hud.mkm.renewal.busy");
            case FULL_HEALTH -> Component.translatable("hud.mkm.renewal.full_health");
            case LOW_MANA -> Component.translatable("hud.mkm.renewal.low_mana");
            case COOLDOWN -> Component.translatable("hud.mkm.renewal.cooldown",
                    String.format(java.util.Locale.ROOT, "%.1f", remaining / 20.0D));
        };
        graphics.text(client.font, Component.translatable("hud.mkm.renewal.hotkey",
                        MkmClient.RENEWAL.getTranslatedKeyMessage()),
                x, y + 11, 0xFFE7E9EF);
        graphics.text(client.font, status, x + 111, y + 11,
                result == RenewalEligibility.Result.READY ? 0xFF85D4B0 : 0xFFC2A7DD);
    }
}
