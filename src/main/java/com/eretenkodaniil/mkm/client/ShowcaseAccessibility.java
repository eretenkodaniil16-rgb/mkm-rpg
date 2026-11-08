package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.MkmMod;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Three remappable keys update local persisted A/B/C preferences.
 * A: high-contrast HUD; B: suppress ambient rune particles; C: keymapping.
 * Camera shake is never added by RC1. Gamepad adapter remains a release blocker.
 */
public final class ShowcaseAccessibility {
    private static final Path CONFIG = FabricLoader.getInstance()
            .getConfigDir().resolve("mkm-showcase-accessibility.properties");
    private static ShowcaseAccessibilityState state = ShowcaseAccessibilityState.defaults();

    private ShowcaseAccessibility() {}

    public static void load() {
        if (Files.isRegularFile(CONFIG)) {
            var data = new Properties();
            try (var input = Files.newInputStream(CONFIG)) {
                data.load(input);
                state = ShowcaseAccessibilityState.read(data);
            } catch (IOException | IllegalArgumentException ex) {
                MkmMod.LOGGER.warn("Unable to read MKM showcase accessibility settings; defaults active", ex);
            }
        }
        // Honour an explicit launch-time particles opt-out even before the first saved toggle.
        if ("false".equalsIgnoreCase(System.getProperty("mkm.runeParticles"))) {
            state = new ShowcaseAccessibilityState(state.hudVisible(), state.highContrast(), true);
        }
        apply();
    }

    public static ShowcaseAccessibilityState state() { return state; }

    public static void toggleHud() { state = state.toggleHud(); persist(); }
    public static void toggleContrast() { state = state.toggleContrast(); persist(); }
    public static void toggleParticles() { state = state.toggleParticles(); persist(); }

    private static void apply() {
        System.setProperty("mkm.runeParticles", Boolean.toString(!state.reducedParticles()));
    }

    private static void persist() {
        apply();
        try {
            Files.createDirectories(CONFIG.getParent());
            Path next = CONFIG.resolveSibling(CONFIG.getFileName() + ".tmp");
            try (var output = Files.newOutputStream(next)) {
                state.properties().store(output, "MKM Showcase RC1 local accessibility; vanilla GUI scale remains configurable");
            }
            try {
                Files.move(next, CONFIG, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(next, CONFIG, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            MkmMod.LOGGER.warn("Could not persist MKM showcase accessibility settings", ex);
        }
    }
}
