package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import com.eretenkodaniil.mkm.client.animation.AnimationRenderProbe;
import com.eretenkodaniil.mkm.client.animation.ArticulatedMode;
import com.eretenkodaniil.mkm.client.animation.NativeRenderDiagnostics;
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
    private static final int BAR_WIDTH = 148;
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
        int x = 12;
        int y = Math.max(48, client.getWindow().getGuiScaledHeight() - 70);
        String version = FabricLoader.getInstance().getModContainer("mkm")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        graphics.text(client.font, Component.literal("MKM RPG " + version
                        + (ArticulatedMode.nativeEnabled()
                            ? (ArticulatedMode.articulatedEnabled() ? " [R07 17 F9]" : " [R07 6 F8]")
                            : " [C3 QA5 F8]")),
                x, y - 24, TEXT);
        CombatState combat = client.player.getAttached(CombatAttachments.PLAYER);
        var character = client.player.getAttached(CharacterAttachments.CHARACTER);
        if (combat == null || character == null) {
            graphics.text(client.font, Component.literal("NO SYNCED COMBAT DATA"),
                    x, y - 12, STAMINA_LOW);
            return;
        }

        double max = EquipmentSnapshots.snapshot(client.player, character).get(PlayerModifierId.MAX_STAMINA);
        if (!Double.isFinite(max) || max <= 0) return;
        float ratio = (float) Math.clamp(combat.stamina() / max, 0D, 1D);
        // Keep the model unobscured in F5; diagnostics stay at bottom left.

        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xC8000000);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, STAMINA_BG);
        graphics.fill(x, y, x + Math.round(BAR_WIDTH * ratio), y + BAR_HEIGHT,
                ratio >= 0.25F ? STAMINA_FILL : STAMINA_LOW);

        graphics.text(client.font, Component.literal("MKM  " + Math.round(combat.stamina())
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
        graphics.text(client.font, Component.literal(description), x, y + 12,
                timeline != null && !timeline.idle() ? TEXT : MUTED);
        if (timeline != null && !timeline.idle() && client.level != null) {
            graphics.text(client.font,
                    Component.literal(AnimationRenderProbe.status(client.level.getGameTime())),
                    x, y + 24, TEXT);
            graphics.text(client.font,
                    Component.literal(NativeRenderDiagnostics.status(client.level.getGameTime())),
                    x, y + 35, TEXT);
            graphics.text(client.font,
                    Component.literal(ArticulatedMode.loadStatus()),
                    x, y + 46, MUTED);
        }
    }
}
