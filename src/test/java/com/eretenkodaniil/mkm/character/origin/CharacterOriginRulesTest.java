package com.eretenkodaniil.mkm.character.origin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class CharacterOriginRulesTest {
    @Test void allFourCombinationsAreValidAndPermanentlyLocked() {
        for (var race : OriginCatalog.racesAll().values()) {
            for (var clazz : OriginCatalog.classesAll().values()) {
                CharacterOriginData before = new CharacterOriginData();
                var result = CharacterOriginRules.select(before, race.id(), clazz.id(), 0);
                assertEquals(CharacterOriginRules.Outcome.SELECTED, result.outcome());
                assertEquals(1, result.data().revision());
                assertEquals(race.id(), result.data().raceId());
                assertEquals(clazz.id(), result.data().classId());
                assertEquals(CharacterOriginRules.Outcome.ALREADY_SELECTED,
                        CharacterOriginRules.select(result.data(), race.id(), clazz.id(), 0).outcome());
                String differentRace = race.id().equals(OriginCatalog.HUMAN.id())
                        ? OriginCatalog.DWARF.id() : OriginCatalog.HUMAN.id();
                assertEquals(CharacterOriginRules.Outcome.LOCKED,
                        CharacterOriginRules.select(result.data(), differentRace, clazz.id(), 1).outcome());
                assertFalse(before.selected(), "Selection must be an immutable replacement");
            }
        }
    }

    @Test void forgedStaleUnknownAndPartialRequestsNeverChangeState() {
        CharacterOriginData empty = new CharacterOriginData();
        assertEquals(CharacterOriginRules.Outcome.STALE,
                CharacterOriginRules.select(empty, "mkm:human", "mkm:warrior", 123).outcome());
        for (String invalid : new String[]{"minecraft:human", "../bad", "mkm:admin", "", "mkm:human/../exploit",
                "mkm:human".repeat(30)}) {
            var result = CharacterOriginRules.select(empty, invalid, "mkm:mage", 0);
            assertEquals(CharacterOriginRules.Outcome.INVALID, result.outcome(), invalid);
            assertSame(empty, result.data());
            var other = CharacterOriginRules.select(empty, "mkm:human", invalid, 0);
            assertEquals(CharacterOriginRules.Outcome.INVALID, other.outcome(), invalid);
        }
        assertEquals(CharacterOriginRules.Outcome.INVALID,
                CharacterOriginRules.select(empty, null, "mkm:warrior", 0).outcome());
        assertEquals(CharacterOriginRules.Outcome.INVALID,
                CharacterOriginRules.select(empty, "mkm:human", null, 0).outcome());
    }

    @Test void forwardVersionAndUnknownSavedIdsFailClosedWithoutErasingThem() {
        CharacterOriginData future = new CharacterOriginData(2, 1, "mkm:human", "mkm:mage");
        assertEquals(CharacterOriginRules.Outcome.UNSUPPORTED,
                CharacterOriginRules.select(future, "mkm:dwarf", "mkm:warrior", 1).outcome());
        CharacterOriginData fromFutureCatalog = new CharacterOriginData(
                1, 1, "mkm:unknown_race", "mkm:unknown_class");
        assertTrue(fromFutureCatalog.selected());
        assertNull(OriginCatalog.race(fromFutureCatalog.raceId()));
        assertEquals(CharacterOriginRules.Outcome.LOCKED,
                CharacterOriginRules.select(fromFutureCatalog, "mkm:human", "mkm:warrior", 1).outcome());
    }
}
