package com.eretenkodaniil.mkm.client;

/** Pure adaptive geometry. The HUD never extends beyond the GUI viewport. */
public record RpgHudGeometry(int left, int top, float scale) {
    public static RpgHudGeometry forViewport(int width, int height, int preferencePercent) {
        float preferred = Math.clamp(preferencePercent / 100.0F, 0.75F, 1.5F);
        float fit = Math.min((width - 16.0F) / 224.0F, (height - 16.0F) / 118.0F);
        return new RpgHudGeometry(8, 8, Math.max(0.1F, Math.min(preferred, fit)));
    }
    public boolean fits(int viewportWidth, int viewportHeight) {
        return left + 224 * scale <= viewportWidth
                && top + 118 * scale <= viewportHeight;
    }
}
