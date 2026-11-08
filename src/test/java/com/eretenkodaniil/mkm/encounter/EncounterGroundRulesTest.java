package com.eretenkodaniil.mkm.encounter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure rule: tests must NOT bootstrap the Minecraft static block registry. */
final class EncounterGroundRulesTest {
    @Test void allowsOnlyConservativeNaturalFootingForAuthoredSpawns() {
        assertTrue(EncounterGroundRules.isNaturalSurface("minecraft:grass_block"));
        assertTrue(EncounterGroundRules.isNaturalSurface("minecraft:dirt"));
        assertTrue(EncounterGroundRules.isNaturalSurface("minecraft:stone"));
        assertTrue(EncounterGroundRules.isNaturalSurface("minecraft:gravel"));
        assertFalse(EncounterGroundRules.isNaturalSurface("minecraft:oak_planks"));
        assertFalse(EncounterGroundRules.isNaturalSurface("minecraft:chest"));
        assertFalse(EncounterGroundRules.isNaturalSurface("minecraft:barrel"));
        assertFalse(EncounterGroundRules.isNaturalSurface("minecraft:bedrock"));
        assertFalse(EncounterGroundRules.isNaturalSurface(null));
    }
}
