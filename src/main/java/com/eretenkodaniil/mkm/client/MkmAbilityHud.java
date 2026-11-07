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
        var parameters = RenewalParameters.from(stats, character.level());
        double maximum = parameters.maximumMana();
        double ratio = maximum > 0D ? Math.clamp(attached.mana() / maximum, 0D, 1D) : 0D;
        int viewportWidth = client.getWindow().getGuiScaledWidth();
        int viewportHeight = client.getWindow().getGuiScaledHeight();
        if (viewportWidth < 120 || viewportHeight < 140) return;
        ResourceHudLayout layout = ResourceHudLayout.forViewport(viewportWidth, viewportHeight);
        int x = layout.x(), y = layout.manaY(), barWidth = layout.width();

        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + HEIGHT + 1, 0xC8000000);
        graphics.fill(x, y, x + barWidth, y + HEIGHT, 0xCC1D2033);
        graphics.fill(x, y, x + (int) Math.round(barWidth * ratio), y + HEIGHT, 0xFF827BD0);
        String manaLabel = layout.narrow()
                ? "MP " + Math.round(attached.mana()) + " / " + Math.round(maximum)
                : Component.translatable("hud.mkm.mana",
                        Math.round(attached.mana()), Math.round(maximum)).getString();
        graphics.text(client.font, Component.literal(ResourceHudText.fit(
                manaLabel, barWidth, client.font::width)), x, y - 12, 0xFFE7E9EF);

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
        String keyLabel = Component.translatable("hud.mkm.renewal.hotkey",
                MkmClient.RENEWAL.getTranslatedKeyMessage()).getString();
        graphics.text(client.font, Component.literal(ResourceHudText.fit(
                keyLabel, barWidth, client.font::width)), x, y + 11, 0xFFE7E9EF);
        graphics.text(client.font, Component.literal(ResourceHudText.fit(
                status.getString(), barWidth, client.font::width)), x, y + 23,
                result == RenewalEligibility.Result.READY ? 0xFF85D4B0 : 0xFFC2A7DD);
    }
}
