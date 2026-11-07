package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.abilities.AbilityGrantPolicy.Actor;
import com.eretenkodaniil.mkm.abilities.AbilityGrantPolicy.Grants;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpellProgressionTest {
    @Test void spellCirclesUnlockAtExplicitCharacterMilestones() {
        for (int tier = 1; tier <= 10; tier++) {
            int threshold = SpellProgression.minimumLevel(tier);
            assertEquals(tier == 1 ? 1 : (tier - 1) * 10, threshold);
            assertTrue(SpellProgression.maximumTierAtLevel(threshold) >= tier);
            if (tier > 1) {
                assertTrue(SpellProgression.maximumTierAtLevel(threshold - 1) < tier);
            }
        }
        assertEquals(1, SpellProgression.maximumTierAtLevel(1));
        assertEquals(1, SpellProgression.maximumTierAtLevel(9));
        assertEquals(2, SpellProgression.maximumTierAtLevel(10));
        assertEquals(5, SpellProgression.maximumTierAtLevel(49));
        assertEquals(10, SpellProgression.maximumTierAtLevel(90));
        assertEquals(10, SpellProgression.maximumTierAtLevel(100));
        assertThrows(IllegalArgumentException.class,
                () -> SpellProgression.maximumTierAtLevel(101));
        assertThrows(IllegalArgumentException.class,
                () -> SpellProgression.minimumLevel(0));
    }

    @Test void everyAbilityHasSchoolAndStrictTierMetadataButNoExecutorEscalation() {
        assertEquals(AbilityCatalog.all().keySet(), SpellProgression.all().keySet());
        assertEquals(SpellSchool.RESTORATION, SpellProgression.get("mkm:renewal").school());
        assertEquals(1, SpellProgression.get("mkm:renewal").tier());
        assertEquals(SpellSchool.EVOCATION, SpellProgression.get("mkm:arcane_bolt").school());
        assertEquals(SpellSchool.ABJURATION, SpellProgression.get("mkm:arcane_barrier").school());
        assertEquals(SpellSchool.NONE, SpellProgression.get("mkm:iron_will").school());
        assertEquals(0, SpellProgression.get("mkm:stone_resilience").tier());
        assertNull(SpellProgression.get("mkm:unknown"));

        assertFalse(SpellProgression.meetsLevel("mkm:arcane_barrier", 19));
        assertTrue(SpellProgression.meetsLevel("mkm:arcane_barrier", 20));
        assertFalse(SpellProgression.meetsLevel("mkm:repulsion_wave", 29));
        assertTrue(SpellProgression.meetsLevel("mkm:repulsion_wave", 30));
        assertFalse(SpellProgression.meetsLevel("mkm:ancestral_focus", 9));
        assertTrue(SpellProgression.meetsLevel("mkm:ancestral_focus", 10));
        assertFalse(SpellProgression.meetsLevel("mkm:renewal", 0));
        assertFalse(SpellProgression.meetsLevel("mkm:renewal", 101));

        Grants highLevelMage = new Grants(Actor.PLAYER, "mkm:mage",
                null, Set.of(), Set.of(), Set.of(), 100);
        assertTrue(AbilityGrantPolicy.granted(highLevelMage, "mkm:arcane_bolt"));
        assertFalse(AbilityGrantPolicy.executable(highLevelMage, "mkm:arcane_bolt"));
        assertTrue(AbilityGrantPolicy.executable(Grants.currentPlayer(100), "mkm:renewal"));
        assertThrows(IllegalArgumentException.class,
                () -> new Grants(Actor.PLAYER, null, null,
                        Set.of(), Set.of(), Set.of(), 0));
    }

    @Test void metadataRejectsInvalidTierAndSchoolPairs() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpellProfile("mkm:test", SpellSchool.NONE, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SpellProfile("mkm:test", SpellSchool.EVOCATION, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SpellProfile("mkm:test", SpellSchool.NECROMANCY, 11, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new SpellProfile("mkm:test", SpellSchool.ILLUSION, 5, 30));
    }
}
