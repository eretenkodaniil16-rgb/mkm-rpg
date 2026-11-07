package com.eretenkodaniil.mkm.performance;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class OptimizationCompatibilityTest {
    @Test void rejectsOverlappingChunkBackends() {
        var r = OptimizationCompatibility.inspect(Set.of("moonrise", "c2me", "lithium"));
        assertEquals("CONFLICT", r.chunkBackend());
        assertEquals(1, r.warnings().size());
        assertTrue(r.warnings().getFirst().contains("Moonrise and C2ME"));
    }

    @Test void compatibleOptimizersDoNotGetDisabled() {
        var r = OptimizationCompatibility.inspect(Set.of("moonrise", "lithium", "sodium", "ferritecore"));
        assertEquals("moonrise", r.chunkBackend());
        assertEquals(0, r.warnings().size());
        assertEquals(3, r.loadedOptimizers().stream().filter(x -> !x.equals("moonrise")).count());
    }

    @Test void absentOptimizationsAreNotInvented() {
        var r = OptimizationCompatibility.inspect(Set.of());
        assertEquals("vanilla", r.chunkBackend());
        assertTrue(r.loadedOptimizers().isEmpty());
    }
}
