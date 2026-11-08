package com.eretenkodaniil.mkm.client.accessibility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Client-only, opt-in presentation/accessibility preferences. Never synchronized
 * to the server and never written to CharacterData or gameplay attachments.
 */
public final class MkmAccessibility {
    public enum Setting { HUD_SCALE, TEXT_SCALE, HIGH_CONTRAST, DISTINCT_COLORS,
        REDUCED_SHAKE, REDUCED_FLASH, VFX, SIMPLE_INPUT }
    public enum Vfx { NORMAL, REDUCED, OFF }

    public record Options(int hudScale, int textScale, boolean highContrast,
                          boolean distinctColors, boolean reducedShake,
                          boolean reducedFlash, Vfx vfx, boolean simpleInput) {
        public Options {
            if (!validScale(hudScale) || !validScale(textScale) || vfx == null) {
                throw new IllegalArgumentException("Unsupported MKM accessibility option");
            }
        }
        public static Options defaults() {
            return new Options(100, 100, false, false, true, true, Vfx.NORMAL, false);
        }
        public Options cycle(Setting setting) {
            return switch (setting) {
                case HUD_SCALE -> new Options(nextScale(hudScale), textScale, highContrast, distinctColors,
                        reducedShake, reducedFlash, vfx, simpleInput);
                case TEXT_SCALE -> new Options(hudScale, nextScale(textScale), highContrast, distinctColors,
                        reducedShake, reducedFlash, vfx, simpleInput);
                case HIGH_CONTRAST -> new Options(hudScale, textScale, !highContrast, distinctColors,
                        reducedShake, reducedFlash, vfx, simpleInput);
                case DISTINCT_COLORS -> new Options(hudScale, textScale, highContrast, !distinctColors,
                        reducedShake, reducedFlash, vfx, simpleInput);
                case REDUCED_SHAKE -> new Options(hudScale, textScale, highContrast, distinctColors,
                        !reducedShake, reducedFlash, vfx, simpleInput);
                case REDUCED_FLASH -> new Options(hudScale, textScale, highContrast, distinctColors,
                        reducedShake, !reducedFlash, vfx, simpleInput);
                case VFX -> new Options(hudScale, textScale, highContrast, distinctColors,
                        reducedShake, reducedFlash, Vfx.values()[(vfx.ordinal() + 1) % Vfx.values().length], simpleInput);
                case SIMPLE_INPUT -> new Options(hudScale, textScale, highContrast, distinctColors,
                        reducedShake, reducedFlash, vfx, !simpleInput);
            };
        }
        public Properties toProperties() {
            Properties p = new Properties();
            p.setProperty("schema", "1");
            p.setProperty("hudScale", Integer.toString(hudScale));
            p.setProperty("textScale", Integer.toString(textScale));
            p.setProperty("highContrast", Boolean.toString(highContrast));
            p.setProperty("distinctColors", Boolean.toString(distinctColors));
            p.setProperty("reducedShake", Boolean.toString(reducedShake));
            p.setProperty("reducedFlash", Boolean.toString(reducedFlash));
            p.setProperty("vfx", vfx.name());
            p.setProperty("simpleInput", Boolean.toString(simpleInput));
            return p;
        }
        public static Options fromProperties(Properties p) {
            Options d = defaults();
            int hud = readScale(p.getProperty("hudScale"), d.hudScale);
            int text = readScale(p.getProperty("textScale"), d.textScale);
            Vfx vfx;
            try { vfx = Vfx.valueOf(p.getProperty("vfx", d.vfx.name())); }
            catch (IllegalArgumentException ex) { vfx = d.vfx; }
            return new Options(hud, text,
                    readBoolean(p.getProperty("highContrast"), d.highContrast),
                    readBoolean(p.getProperty("distinctColors"), d.distinctColors),
                    readBoolean(p.getProperty("reducedShake"), d.reducedShake),
                    readBoolean(p.getProperty("reducedFlash"), d.reducedFlash),
                    vfx, readBoolean(p.getProperty("simpleInput"), d.simpleInput));
        }
        private static boolean readBoolean(String value, boolean fallback) {
            if ("true".equalsIgnoreCase(value)) return true;
            if ("false".equalsIgnoreCase(value)) return false;
            return fallback;
        }
        private static int readScale(String value, int fallback) {
            try { int parsed = Integer.parseInt(value); return validScale(parsed) ? parsed : fallback; }
            catch (NumberFormatException ex) { return fallback; }
        }
        private static boolean validScale(int scale) {
            return scale == 75 || scale == 100 || scale == 125 || scale == 150;
        }
        private static int nextScale(int scale) {
            return scale == 75 ? 100 : scale == 100 ? 125 : scale == 125 ? 150 : 75;
        }
    }

    private static Options current = Options.defaults();
    private MkmAccessibility() {}
    public static Options get() { return current; }
    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("mkm-accessibility.properties");
    }
    public static void load() {
        Path path = path();
        if (!Files.isRegularFile(path)) { current = Options.defaults(); return; }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
            current = Options.fromProperties(p);
        } catch (IOException | IllegalArgumentException ex) {
            current = Options.defaults();
            System.err.println("MKM: accessibility settings could not be read: " + ex.getMessage());
        }
    }
    public static void change(Setting setting) {
        Options next = current.cycle(setting);
        Path target = path();
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(temporary)) {
                next.toProperties().store(out, "MKM client accessibility v1 (not server settings)");
            }
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            current = next;
        } catch (IOException ex) {
            System.err.println("MKM: accessibility settings were not saved: " + ex.getMessage());
        }
    }
}
