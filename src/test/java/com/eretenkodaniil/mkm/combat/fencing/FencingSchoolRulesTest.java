package com.eretenkodaniil.mkm.combat.fencing;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import org.junit.jupiter.api.Test;

/** Deterministic server-tick selection and no-mid-hit posture swaps. */
final class FencingSchoolRulesTest {
    private static final EquipmentRules.Family SWORD = EquipmentRules.Family.SWORD;

    @Test void allThreeSchoolsHaveDistinctReadOnlyMnaeGuardsAndImmutableDeltas() {
        assertEquals("mkm:guard/vom_tag", FencingSchool.LIECHTENAUER.visualGuardCue());
        assertEquals("mkm:guard/porta_di_ferro", FencingSchool.FIORE.visualGuardCue());
        assertEquals("mkm:guard/angulo_recto", FencingSchool.DESTREZA.visualGuardCue());
        assertTrue(FencingSchool.NONE.contribution().isEmpty());
        for (FencingSchool school : new FencingSchool[]{
                FencingSchool.LIECHTENAUER, FencingSchool.FIORE, FencingSchool.DESTREZA}) {
            assertTrue(school.contribution().isPresent());
            assertFalse(school.additiveDeltas().isEmpty());
            assertEquals(school, FencingSchool.fromId(school.id()).orElseThrow());
            assertTrue(school.visualGuardCue().startsWith("mkm:guard/"));
            assertThrows(UnsupportedOperationException.class, () ->
                    school.additiveDeltas().clear());
        }
        assertTrue(FencingSchool.fromId("unknown").isEmpty());
    }

    @Test void warmupCooldownAndIdleGateRejectInstantStatSwapping() {
        FencingSchoolState start = new FencingSchoolState();
        var chosen = FencingSchoolRules.select(
                start, FencingSchool.LIECHTENAUER, SWORD, true, 100L);
        assertEquals(FencingSchoolRules.ResultCode.APPLIED, chosen.code());
        var state = chosen.state();
        assertFalse(state.activeAt(119L));
        assertTrue(state.activeAt(120L));
        assertEquals(20L, state.remainingWarmup(100L));
        assertEquals(80L, state.remainingSwitchCooldown(100L));
        assertEquals(FencingSchool.LIECHTENAUER,
                FencingSchoolRules.effectiveSchool(state, SWORD, 120L).orElseThrow());
        assertEquals("mkm:guard/vom_tag",
                FencingSchoolRules.visualGuardCue(state, SWORD, 120L).orElseThrow());
        assertTrue(FencingSchoolRules.visualGuardCue(state, SWORD, 119L).isEmpty());

        var busy = FencingSchoolRules.select(state, FencingSchool.FIORE,
                SWORD, false, 180L);
        assertEquals(FencingSchoolRules.ResultCode.ACTION_BUSY, busy.code());
        assertSame(state, busy.state());
        var tooSoon = FencingSchoolRules.select(state, FencingSchool.FIORE,
                SWORD, true, 130L);
        assertEquals(FencingSchoolRules.ResultCode.SWITCH_COOLDOWN, tooSoon.code());
        assertSame(state, tooSoon.state());
        var fiore = FencingSchoolRules.select(state, FencingSchool.FIORE, SWORD, true, 180L);
        assertTrue(fiore.applied());
        assertFalse(fiore.state().activeAt(199L));
        assertTrue(fiore.state().activeAt(200L));
        assertEquals(FencingSchool.FIORE,
                FencingSchoolRules.effectiveSchool(fiore.state(), SWORD, 200L).orElseThrow());
    }

    @Test void incompatibleGearCannotActivateSchoolOrProvideVisualCue() {
        for (String main : new String[]{
                "minecraft:air", "minecraft:iron_axe", "minecraft:mace",
                "minecraft:iron_pickaxe", "minecraft:shield", "minecraft:bow"}) {
            EquipmentRules.Family family = EquipmentRules.presentationFamily(main);
            var rejected = FencingSchoolRules.select(new FencingSchoolState(),
                    FencingSchool.DESTREZA, family, true, 400L);
            assertEquals(FencingSchoolRules.ResultCode.REQUIRES_MAINHAND_SWORD,
                    rejected.code(), main);
            assertTrue(FencingSchoolRules.effectiveSchool(
                    new FencingSchoolState(FencingSchool.FIORE, 100, 180),
                    family, 500).isEmpty(), main);
            assertTrue(FencingSchoolRules.visualGuardCue(
                    new FencingSchoolState(FencingSchool.FIORE, 100, 180),
                    family, 500).isEmpty(), main);
        }
        // Mainhand air, offhand sword is NOT a mainhand fencing school.
        var empty = EquipmentRules.presentationFamily("minecraft:air");
        assertEquals(FencingSchoolRules.ResultCode.REQUIRES_MAINHAND_SWORD,
                FencingSchoolRules.select(new FencingSchoolState(), FencingSchool.FIORE,
                        empty, true, 100L).code());
    }

    @Test void sameSelectionCannotResetCooldownAndDisarmRequiresIdle() {
        var initial = FencingSchoolRules.select(new FencingSchoolState(),
                FencingSchool.DESTREZA, SWORD, true, 100).state();
        assertEquals(FencingSchoolRules.ResultCode.ALREADY_SELECTED,
                FencingSchoolRules.select(initial, FencingSchool.DESTREZA,
                        SWORD, true, 200).code());
        assertEquals(FencingSchoolRules.ResultCode.SWITCH_COOLDOWN,
                FencingSchoolRules.select(initial, FencingSchool.NONE,
                        EquipmentRules.Family.NONE, true, 150).code());
        assertEquals(FencingSchoolRules.ResultCode.ACTION_BUSY,
                FencingSchoolRules.select(initial, FencingSchool.NONE,
                        EquipmentRules.Family.NONE, false, 200).code());
        var off = FencingSchoolRules.select(initial, FencingSchool.NONE,
                EquipmentRules.Family.NONE, true, 200).state();
        assertEquals(FencingSchool.NONE, off.school());
        assertFalse(off.activeAt(500L));
        assertEquals(80L, off.remainingSwitchCooldown(200L));
        assertTrue(FencingSchoolRules.effectiveSchool(off, SWORD, 500L).isEmpty());
    }

    @Test void malformedOrRollbackTicksCannotForgeActiveBonus() {
        var start = new FencingSchoolState();
        assertEquals(FencingSchoolRules.ResultCode.INVALID_TICK,
                FencingSchoolRules.select(start, FencingSchool.FIORE, SWORD,
                        true, -1L).code());
        assertThrows(IllegalArgumentException.class,
                () -> new FencingSchoolState(FencingSchool.FIORE, -1L, 20L));
        assertThrows(IllegalArgumentException.class,
                () -> new FencingSchoolState(FencingSchool.FIORE, 100L, 99L));
        var max = FencingSchoolRules.select(start, FencingSchool.FIORE,
                SWORD, true, Long.MAX_VALUE - 1L);
        assertTrue(max.applied());
        assertEquals(Long.MAX_VALUE, max.state().effectiveFromTick());
        assertEquals(Long.MAX_VALUE, max.state().nextSwitchTick());
        assertFalse(max.state().activeAt(Long.MAX_VALUE - 1L));
        assertTrue(FencingSchoolRules.effectiveSchool(
                max.state(), EquipmentRules.Family.AXE, Long.MAX_VALUE).isEmpty());
    }
}
