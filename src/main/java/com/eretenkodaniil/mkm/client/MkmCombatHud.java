package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Combat C2 phase and stamina feedback, driven by owner-synced data.
 * Uses 26.3's GUI extraction API; no persistent client-side game state.
 */
public final class MkmCombatHud {
    private static final int BAR_HEIGHT = 7;
    private static final int STAMINA_BG = 0xCC19202A;
    private static final int STAMINA_FILL = 0xFF57B38C;
    private static final int STAMINA_LOW = 0xFFD09B55;
    private static final int TEXT = 0xFFE7E9EF;
    private static final int MUTED = 0xFFA8A9B1;

    private MkmCombatHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null
                || client.player.isSpectator()) {
            return;
        }
        CombatState combat = client.player.getAttached(CombatAttachments.PLAYER);
        var character = client.player.getAttached(CharacterAttachments.CHARACTER);
        if (combat == null || character == null) return;

        double max = PlayerModifierCalculator.calculate(character).get(PlayerModifierId.MAX_STAMINA);
        if (!Double.isFinite(max) || max <= 0) return;
        float ratio = (float) Math.clamp(combat.stamina() / max, 0D, 1D);
        int viewportWidth = client.getWindow().getGuiScaledWidth();
        int viewportHeight = client.getWindow().getGuiScaledHeight();
        if (viewportWidth < 120 || viewportHeight < 140) return;
        ResourceHudLayout layout = ResourceHudLayout.forViewport(viewportWidth, viewportHeight);
        int x = layout.x(), y = layout.staminaY(), barWidth = layout.width();

        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + BAR_HEIGHT + 1, 0xC8000000);
        graphics.fill(x, y, x + barWidth, y + BAR_HEIGHT, STAMINA_BG);
        graphics.fill(x, y, x + Math.round(barWidth * ratio), y + BAR_HEIGHT,
                ratio >= 0.25F ? STAMINA_FILL : STAMINA_LOW);

        graphics.text(client.font, Component.literal(
                (layout.narrow() ? "SP " : "MKM  ") + Math.round(combat.stamina())
                + " / " + Math.round(max)), x, y - 12, TEXT);

        ActionTimeline timeline = client.player.getAttached(CombatAttachments.TIMELINE);
        String description = timeline == null || timeline.idle()
                ? "R: Combo     G: Dodge"
                : switch (timeline.phase()) {
                    case WINDUP -> "COMBO " + timeline.comboStep() + "/3   WINDUP";
                    case ACTIVE -> "COMBO " + timeline.comboStep() + "/3   HIT!";
                    case RECOVERY -> "COMBO " + timeline.comboStep() + "/3   RECOVERY";
                    case DODGING -> "DODGE   RECOVERY";
                    case IDLE -> "R: Combo     G: Dodge";
                };
        if (layout.narrow()) {
            description = timeline == null || timeline.idle()
                    ? "R / G" : "ACT " + timeline.phase().name();
            if (description.length() > 12) description = "BUSY";
        }
        graphics.text(client.font, Component.literal(description), x, y + 12,
                timeline != null && !timeline.idle() ? TEXT : MUTED);
    }
}
