package com.eretenkodaniil.mkm.environment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Client-owned and local-only presentation settings. No gameplay fields, server
 * writes, shaderpack download, custom render pipeline or required Iris.
 */
public final class EnvironmentVisualSettings {
    public enum Profile { ECONOMY, STANDARD, CINEMATIC }

    private static volatile Profile profile = Profile.STANDARD;
    private static volatile boolean reducedEffects = false;
    private static Path file;

    private EnvironmentVisualSettings() {}

    public static Profile profile() { return profile; }
    public static boolean reducedEffects() { return reducedEffects; }

    public static synchronized void load(Path gameDirectory) {
        file = gameDirectory.resolve("config").resolve("mkm-environment.properties");
        Properties props = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException | IllegalArgumentException ignored) {
                // Broken client settings are not allowed to prevent joining servers.
            }
        }
        String value = props.getProperty("profile", "standard");
        try {
            profile = Profile.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            profile = Profile.STANDARD;
        }
        reducedEffects = Boolean.parseBoolean(props.getProperty("reducedEffects", "false"));
    }

    public static synchronized Profile cycleProfile() {
        profile = Profile.values()[(profile.ordinal() + 1) % Profile.values().length];
        save();
        return profile;
    }

    public static synchronized boolean toggleReducedEffects() {
        reducedEffects = !reducedEffects;
        save();
        return reducedEffects;
    }

    private static void save() {
        if (file == null) return;
        Properties props = new Properties();
        props.setProperty("profile", profile.name().toLowerCase(Locale.ROOT));
        props.setProperty("reducedEffects", Boolean.toString(reducedEffects));
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "MKM client-only visuals; does not affect gameplay or shaders");
            }
        } catch (IOException ignored) {
            // Read-only game folders leave active settings in-memory for this session.
        }
    }

    public static boolean allowRuneTick() {
        return Boolean.parseBoolean(System.getProperty("mkm.runeParticles", "true"))
                && !(reducedEffects && profile == Profile.ECONOMY);
    }

    public static int runeFrequencyDivisor() {
        if (reducedEffects) return 40;
        return switch (profile) {
            case ECONOMY -> 48;
            case STANDARD -> 12;
            case CINEMATIC -> 7;
        };
    }
}
