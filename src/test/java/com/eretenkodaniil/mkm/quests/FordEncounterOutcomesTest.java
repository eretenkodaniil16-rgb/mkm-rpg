package com.eretenkodaniil.mkm.quests;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

final class FordEncounterOutcomesTest {
    @Test void offlineDeathIsDurableAndOneShot() {
        FordEncounterOutcomes start = new FordEncounterOutcomes();
        var first = start.recordDeath("raider-a","owner-a");
        assertEquals(1, first.deadRaiders().size());
        assertSame(first, first.recordDeath("raider-a","owner-b"));
        var encoded = FordEncounterOutcomes.CODEC.encodeStart(JsonOps.INSTANCE, first).getOrThrow();
        var loaded = FordEncounterOutcomes.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(first, loaded);
        assertEquals("owner-a", loaded.deadRaiders().get("raider-a"));
        var cleared = loaded.remove("raider-a");
        assertTrue(cleared.deadRaiders().isEmpty());
        assertSame(cleared, cleared.remove("raider-a"));
    }

    @Test void independentPlayersKeepSeparateEncounterRecords() {
        var ledger = new FordEncounterOutcomes()
                .recordDeath("raider-a","player-a")
                .recordDeath("raider-b","player-b");
        assertEquals("player-a",ledger.deadRaiders().get("raider-a"));
        assertEquals("player-b",ledger.deadRaiders().get("raider-b"));
        var remaining = ledger.remove("raider-a");
        assertEquals(1,remaining.deadRaiders().size());
        assertEquals("player-b",remaining.deadRaiders().get("raider-b"));
    }
}
