package com.eretenkodaniil.mkm.quests;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class FordEncounterRegistryTest {
    private static FordEncounterRegistry.Entry encounter(String id) {
        return new FordEncounterRegistry.Entry(id, "warden-a", 11, 70, -24);
    }

    @Test void singleOwnerCannotReserveTwoRaiders() {
        var empty = new FordEncounterRegistry();
        var one = empty.register("player-a", encounter("raider-a"));
        assertEquals(1, one.byOwner().size());
        assertSame(one, one.register("player-a", encounter("raider-b")));
        assertEquals("raider-a", one.byOwner().get("player-a").raiderUuid());
        assertTrue(one.canRegister("player-b"));
        assertFalse(one.canRegister("player-a"));
    }

    @Test void onePlayersDeathNeverReleasesTheOtherPlayersEnemy() {
        var both = new FordEncounterRegistry()
                .register("player-a", encounter("raider-a"))
                .register("player-b", encounter("raider-b"));
        assertSame(both, both.removeIfMatch("player-b","raider-a"));
        var after = both.removeIfMatch("player-a","raider-a");
        assertEquals(1,after.byOwner().size());
        assertTrue(after.byOwner().containsKey("player-b"));
        assertFalse(after.byOwner().containsKey("player-a"));
        assertSame(after, after.removeIfMatch("player-a","raider-a"));
    }

    @Test void worldRegistryRoundtripKeepsUnloadedEncounterReservation() {
        var original = new FordEncounterRegistry()
                .register("player-a", encounter("raider-a"))
                .register("player-b", encounter("raider-b"));
        var encoded = FordEncounterRegistry.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        var restored = FordEncounterRegistry.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original,restored);
        assertEquals(-24,restored.byOwner().get("player-b").z());
    }
}
