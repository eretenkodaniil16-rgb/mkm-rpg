package com.eretenkodaniil.mkm.client;

import java.util.function.ToIntFunction;

/**
 * Pixel-bound labels for compact resource HUDs. The renderer supplies the
 * actual Minecraft font width: never substitute a character-count estimate.
 */
public final class ResourceHudText {
    private static final String ELLIPSIS = "\u2026";

    private ResourceHudText() {}

    public static String fit(String text, int maximumWidth, ToIntFunction<String> measure) {
        if (text == null || measure == null || maximumWidth < 0) {
            throw new IllegalArgumentException("Invalid HUD label bounds");
        }
        if (text.isEmpty() || measure.applyAsInt(text) <= maximumWidth) return text;
        if (measure.applyAsInt(ELLIPSIS) > maximumWidth) return "";

        // Iterate over Unicode code points, not UTF-16 chars: remapped keys
        // and translations may include non-BMP symbols.
        int[] codePoints = text.codePoints().toArray();
        StringBuilder candidate = new StringBuilder();
        String result = ELLIPSIS;
        for (int cp : codePoints) {
            candidate.appendCodePoint(cp);
            String next = candidate + ELLIPSIS;
            if (measure.applyAsInt(next) > maximumWidth) break;
            result = next;
        }
        return result;
    }
}
