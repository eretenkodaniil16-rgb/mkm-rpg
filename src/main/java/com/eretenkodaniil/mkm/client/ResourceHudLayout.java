package com.eretenkodaniil.mkm.client;

/**
 * Pure geometry shared by stamina and mana HUDs. In F5, the resource bars stay
 * completely on the left half, outside the projected player-model center.
 * Renderers may abbreviate labels when barWidth is small.
 */
public record ResourceHudLayout(int x, int width, int manaY, int staminaY) {
    public static ResourceHudLayout forViewport(int scaledWidth, int scaledHeight) {
        if (scaledWidth < 120 || scaledHeight < 140) {
            throw new IllegalArgumentException("Viewport too small for the paired HUD");
        }
        int x = 8;
        int width = Math.min(148, Math.max(40, scaledWidth / 2 - 24));
        int staminaY = Math.max(88, scaledHeight - 60);
        int manaY = staminaY - 70;
        return new ResourceHudLayout(x, width, manaY, staminaY);
    }

    public boolean narrow() {
        return width < 125;
    }
}
