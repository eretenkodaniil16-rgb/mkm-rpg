package com.eretenkodaniil.mkm.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Read-only compatibility advisor; never changes user-installed mods. */
public final class OptimizationCompatibility {
    private OptimizationCompatibility() { }

    public static final List<String> KNOWN_IDS = List.of(
            "sodium", "lithium", "ferritecore", "immediatelyfast", "entityculling",
            "moreculling", "dynamic_fps", "iris", "spark", "chunky", "krypton",
            "c2me", "moonrise", "scalablelux", "modernfix", "vmp");

    public static Report inspect(Set<String> loadedIds) {
        Set<String> loaded = Set.copyOf(loadedIds);
        List<String> detected = KNOWN_IDS.stream().filter(loaded::contains).toList();
        List<String> warnings = new ArrayList<>();
        String chunkBackend = "vanilla";
        if (loaded.contains("moonrise") && loaded.contains("c2me")) {
            chunkBackend = "CONFLICT";
            warnings.add("Moonrise and C2ME both replace chunk internals: remove one before playing.");
        } else if (loaded.contains("moonrise")) {
            chunkBackend = "moonrise";
        } else if (loaded.contains("c2me")) {
            chunkBackend = "c2me";
        }
        if (loaded.contains("modernfix")) {
            warnings.add("Verify ModernFix build explicitly targets Minecraft 26.3; older releases may not.");
        }
        return new Report(chunkBackend, detected, List.copyOf(warnings));
    }

    public record Report(String chunkBackend, List<String> loadedOptimizers, List<String> warnings) {
        public Report {
            loadedOptimizers = List.copyOf(loadedOptimizers);
            warnings = List.copyOf(warnings);
        }
    }
}
