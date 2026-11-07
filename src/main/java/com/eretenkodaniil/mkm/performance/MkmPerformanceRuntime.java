package com.eretenkodaniil.mkm.performance;

import com.eretenkodaniil.mkm.MkmMod;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

/** Passive server-tick diagnostics and read-only third-party compatibility checks. */
public final class MkmPerformanceRuntime {
    private static final TickTimeWindow TICKS = new TickTimeWindow(512);
    private static OptimizationCompatibility.Report compatibility;
    private static boolean registered;

    private MkmPerformanceRuntime() { }

    public static void initialize() {
        if (registered) return;
        registered = true;
        Set<String> installed = OptimizationCompatibility.KNOWN_IDS.stream()
                .filter(id -> FabricLoader.getInstance().isModLoaded(id))
                .collect(Collectors.toUnmodifiableSet());
        compatibility = OptimizationCompatibility.inspect(installed);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> TICKS.clear());
        ServerTickEvents.START_SERVER_TICK.register(server -> TICKS.begin(System.nanoTime()));
        ServerTickEvents.END_SERVER_TICK.register(server -> TICKS.end(System.nanoTime()));
        MkmMod.LOGGER.info("MKM Performance M0: passive tick diagnostics; optimizers {}, chunk backend {}",
                compatibility.loadedOptimizers(), compatibility.chunkBackend());
        for (String warning : compatibility.warnings()) MkmMod.LOGGER.warn("MKM Performance: {}", warning);
    }

    public static TickTimeWindow.Summary tickSummary() { return TICKS.snapshot(); }

    public static OptimizationCompatibility.Report compatibility() {
        if (compatibility == null) throw new IllegalStateException("Performance diagnostics not initialized");
        return compatibility;
    }
}
