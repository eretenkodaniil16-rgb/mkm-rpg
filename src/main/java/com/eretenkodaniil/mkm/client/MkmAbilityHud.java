package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.abilities.AbilityAttachments;
import com.eretenkodaniil.mkm.abilities.AbilityCatalog;
import com.eretenkodaniil.mkm.abilities.AbilityRules;
import com.eretenkodaniil.mkm.abilities.ManaLedger;
import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Owner-synced informational display. Authority remains exclusively server-side. */
public final class MkmAbilityHud {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 7;

    private MkmAbilityHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null
                || client.gui.screen() != null || client.player.isSpectator()) return;
        ManaLedger ledger = client.player.getAttached(AbilityAttachments.PLAYER);
        var character = client.player.getAttached(CharacterAttachments.CHARACTER);
        if (ledger == null || character == null) return;

        var stats = PlayerModifierCalculator.calculate(character);
        double maximum = stats.get(PlayerModifierId.MAX_MANA);
        double ratio = maximum > 0 ? Math.clamp(ledger.mana() / maximum, 0D, 1D) : 0D;
        int x = (client.getWindow().getGuiScaledWidth() - WIDTH) / 2;
        int y = Math.max(18, client.getWindow().getGuiScaledHeight() - 113);

        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xC8000000);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xCC1D2033);
        graphics.fill(x, y, x + (int) Math.round(WIDTH * ratio), y + HEIGHT, 0xFF827BD0);
        graphics.text(client.font, Component.literal("MKM Mana  "
                + Math.round(ledger.mana()) + " / " + Math.round(maximum)),
                x, y - 12, 0xFFE7E9EF);

        long now = client.level.getGameTime();
        long remaining = Math.max(0L, ledger.readyTick(AbilityCatalog.RENEWAL.id()) - now);
        double cost = AbilityRules.scaledCost(AbilityCatalog.RENEWAL.baseCost(),
                stats.get(PlayerModifierId.MANA_COST));
        var action = client.player.getAttached(CombatAttachments.TIMELINE);
        String state;
        if (remaining > 0) {
            state = String.format(java.util.Locale.ROOT, "%.1fs", remaining / 20.0D);
        } else if (ledger.mana() + 0.00001D < cost) {
            state = "LOW MANA";
        } else if (client.player.getHealth() >= client.player.getMaxHealth() - 0.001F) {
            state = "FULL HP";
        } else if (action != null && !action.idle()) {
            state = "BUSY";
        } else {
            state = "READY";
        }
        graphics.text(client.font, Component.literal("H: Renewal  " + state),
                x, y + 11, remaining > 0 ? 0xFFC2A7DD : 0xFFE7E9EF);
    }
}
