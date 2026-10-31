package com.eretenkodaniil.mkm.character.modifier;

import java.util.Locale;

public enum ModifierUnit {
    FLAT,
    MULTIPLIER,
    FRACTION,
    PER_SECOND,
    SECONDS;

    public String format(double value) {
        return switch (this) {
            case FLAT -> formatNumber(value);
            case MULTIPLIER, FRACTION -> Math.round(value * 100.0D) + "%";
            case PER_SECOND -> formatNumber(value) + "/s";
            case SECONDS -> formatNumber(value) + "s";
        };
    }

    private static String formatNumber(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.005D) {
            return Long.toString(Math.round(rounded));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
