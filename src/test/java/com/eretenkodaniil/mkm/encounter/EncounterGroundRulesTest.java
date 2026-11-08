package com.eretenkodaniil.mkm.encounter;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Make private-building exclusions reviewable without a running world. */
final class EncounterGroundRulesTest {
    @Test void allowsOnlyConservativeNaturalFootingForAuthoredSpawns() {
        assertTrue(EncounterGroundRules.isNaturalSurface(Blocks.GRASS_BLOCK));
        assertTrue(EncounterGroundRules.isNaturalSurface(Blocks.DIRT));
        assertTrue(EncounterGroundRules.isNaturalSurface(Blocks.STONE));
        assertTrue(EncounterGroundRules.isNaturalSurface(Blocks.GRAVEL));
        assertFalse(EncounterGroundRules.isNaturalSurface(Blocks.OAK_PLANKS));
        assertFalse(EncounterGroundRules.isNaturalSurface(Blocks.CHEST));
        assertFalse(EncounterGroundRules.isNaturalSurface(Blocks.BARREL));
        assertFalse(EncounterGroundRules.isNaturalSurface(Blocks.BEDROCK));
    }
}
