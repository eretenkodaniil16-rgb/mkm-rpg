package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import com.eretenkodaniil.mkm.client.animation.AnimationRenderProbe;
import net.fabricmc.loader.api.FabricLoader;
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
        int vw = client.getWindow().getGuiScaledWidth();
        int vh = client.getWindow().getGuiScaledHeight();
        if (vw < 120 || vh < 140) return;
        ResourceHudLayout layout = ResourceHudLayout.forViewport(vw, vh);
        int x = layout.x(), y = layout.staminaY(), barWidth = layout.width();
        // C3.7 renderer diagnostics are preserved, outside both resource bars.
        if (vh >= 250) {
            String version = FabricLoader.getInstance().getModContainer("mkm")
                    .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
            String heading = ResourceHudText.fit("MKM RPG " + version + " [C3.7]",
                    barWidth, client.font::width);
            graphics.text(client.font, Component.literal(heading),
                    x, layout.manaY() - 24, TEXT);
        }
        CombatState combat = client.player.getAttached(CombatAttachments.PLAYER);
        var character = client.player.getAttached(CharacterAttachments.CHARACTER);
        if (combat == null || character == null) {
            graphics.text(client.font, Component.literal(ResourceHudText.fit(
                    "NO SYNCED COMBAT DATA", barWidth, client.font::width)),
                    x, y - 12, STAMINA_LOW);
            return;
        }

        double max = EquipmentSnapshots.snapshot(client.player, character).get(PlayerModifierId.MAX_STAMINA);
        if (!Double.isFinite(max) || max <= 0) return;
        float ratio = (float) Math.clamp(combat.stamina() / max, 0D, 1D);
        // Keep the model unobscured in F5; diagnostics stay at bottom left.

        graphics.fill(x - 1, y - 1, x + barWidth + 1, y + BAR_HEIGHT + 1, 0xC8000000);
        graphics.fill(x, y, x + barWidth, y + BAR_HEIGHT, STAMINA_BG);
        graphics.fill(x, y, x + Math.round(barWidth * ratio), y + BAR_HEIGHT,
                ratio >= 0.25F ? STAMINA_FILL : STAMINA_LOW);

        String staminaLabel = (layout.narrow() ? "SP " : "MKM ")
                + Math.round(combat.stamina()) + " / " + Math.round(max);
        graphics.text(client.font, Component.literal(ResourceHudText.fit(
                staminaLabel, barWidth, client.font::width)), x, y - 12, TEXT);

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
        }
        graphics.text(client.font, Component.literal(ResourceHudText.fit(
                description, barWidth, client.font::width)), x, y + 12,
                timeline != null && !timeline.idle() ? TEXT : MUTED);
        if (timeline != null && !timeline.idle() && client.level != null) {
            graphics.text(client.font,
                    Component.literal(ResourceHudText.fit(
                            AnimationRenderProbe.status(client.level.getGameTime()),
                            barWidth, client.font::width)),
                    x, y + 24, TEXT);
        }
    }
}
