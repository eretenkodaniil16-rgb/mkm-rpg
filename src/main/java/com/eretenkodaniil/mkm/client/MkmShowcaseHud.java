package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** A visible, safe guide independent of gameplay packets or damage callbacks. */
public final class MkmShowcaseHud {
    private MkmShowcaseHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null
                || client.player.isSpectator()) return;
        var preferences = ShowcaseAccessibility.state();
        if (!preferences.hudVisible()) return;

        int width = client.getWindow().getGuiScaledWidth();
        int x = Math.max(4, width - 213);
        int y = 8;
        int panelColor = preferences.highContrast() ? 0xF8000000 : 0xB21A2028;
        int titleColor = preferences.highContrast() ? 0xFFFFFFFF : 0xFFEEDAB0;
        int secondary = preferences.highContrast() ? 0xFFFFFF00 : 0xFFA9D3B7;
        graphics.fill(x - 4, y - 3, x + 209, y + 51, panelColor);
        graphics.text(client.font, Component.literal("MKM  |  SHOWCASE RC1"), x, y, titleColor);
        var origin = client.player.getAttached(CharacterOriginAttachments.PROFILE);
        String selected = origin != null && origin.selected() && origin.schemaVersion() == 1
                ? origin.raceId().substring(4) + " / " + origin.classId().substring(4)
                : "K: choose origin + class";
        graphics.text(client.font, Component.literal(selected), x, y + 12, titleColor);
        graphics.text(client.font, Component.literal("Ilra > Watchtower > Ambush"), x, y + 24, secondary);
        String effects = preferences.reducedParticles() ? "VFX LOW" : "VFX ON";
        graphics.text(client.font, Component.literal("O HUD | U Contrast | B " + effects),
                x, y + 36, secondary);
    }
}
