package com.eretenkodaniil.mkm.client;

import java.util.Properties;

/** Local-only client preferences; no combat, C2S input or server state changes. */
public record ShowcaseAccessibilityState(boolean hudVisible, boolean highContrast,
                                         boolean reducedParticles) {
    public static ShowcaseAccessibilityState defaults() {
        return new ShowcaseAccessibilityState(true, false, false);
    }

    public static ShowcaseAccessibilityState read(Properties props) {
        return new ShowcaseAccessibilityState(
                readBoolean(props, "hudVisible", true),
                readBoolean(props, "highContrast", false),
                readBoolean(props, "reducedParticles", false));
    }

    private static boolean readBoolean(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        return raw == null ? fallback : "true".equals(raw) ? true
                : "false".equals(raw) ? false : fallback;
    }

    public Properties properties() {
        var out = new Properties();
        out.setProperty("version", "1");
        out.setProperty("hudVisible", Boolean.toString(hudVisible));
        out.setProperty("highContrast", Boolean.toString(highContrast));
        out.setProperty("reducedParticles", Boolean.toString(reducedParticles));
        return out;
    }

    public ShowcaseAccessibilityState toggleHud() {
        return new ShowcaseAccessibilityState(!hudVisible, highContrast, reducedParticles);
    }
    public ShowcaseAccessibilityState toggleContrast() {
        return new ShowcaseAccessibilityState(hudVisible, !highContrast, reducedParticles);
    }
    public ShowcaseAccessibilityState toggleParticles() {
        return new ShowcaseAccessibilityState(hudVisible, highContrast, !reducedParticles);
    }
}
