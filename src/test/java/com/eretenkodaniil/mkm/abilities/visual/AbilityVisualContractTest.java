package com.eretenkodaniil.mkm.abilities.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityVisualContractTest {
    private static AbilityVisualEvent valid(int seq) {
        return new AbilityVisualEvent("mkm:renewal",
                AbilityVisualPolicy.RENEWAL_ACTION_ID,
                42, seq, AbilityVisualEvent.Hand.MAIN_HAND,
                AbilityVisualEvent.Phase.APPLIED, 650L,
                AbilityVisualPolicy.RENEWAL_VISUAL_TICKS);
    }

    @Test void successfulRenewalOnlyWhenServerActuallyHeals() {
        assertTrue(AbilityVisualPolicy.renewalApplied(true, true, 10F, 16F));
        assertFalse(AbilityVisualPolicy.renewalApplied(true, true, 20F, 20F));
        assertFalse(AbilityVisualPolicy.renewalApplied(true, true, 20F, 19F));
        assertFalse(AbilityVisualPolicy.renewalApplied(false, true, 10F, 16F));
        assertFalse(AbilityVisualPolicy.renewalApplied(true, false, 10F, 16F));
        assertFalse(AbilityVisualPolicy.renewalApplied(true, true, Float.NaN, 16F));
        assertFalse(AbilityVisualPolicy.renewalApplied(true, true, 10F, Float.POSITIVE_INFINITY));
    }

    @Test void appliedEventIsBoundedAndMnaeNeutral() {
        var applied = valid(1);
        assertTrue(AbilityVisualPolicy.allowedForPlayback(applied));
        assertEquals(650, applied.serverTick());
        assertEquals("mkm:renewal", applied.abilityId());
        assertEquals(AbilityVisualEvent.Phase.APPLIED, applied.phase());
        assertThrows(IllegalArgumentException.class,
                () -> new AbilityVisualEvent("mkm:renewal", "mkm:renewal_cast",
                        42, 1, AbilityVisualEvent.Hand.MAIN_HAND,
                        AbilityVisualEvent.Phase.APPLIED, 1, 101));
        assertThrows(IllegalArgumentException.class,
                () -> new AbilityVisualEvent("mkm:renewal", "mkm:renewal_cast",
                        42, 0, AbilityVisualEvent.Hand.MAIN_HAND,
                        AbilityVisualEvent.Phase.APPLIED, 1, 14));
    }

    @Test void eventLifecycleIsDefinedButRenewalDoesNotFakeCastingPhases() {
        for (var phase : AbilityVisualEvent.Phase.values()) {
            var event = new AbilityVisualEvent("mkm:renewal",
                    AbilityVisualPolicy.RENEWAL_ACTION_ID,
                    42, 1, AbilityVisualEvent.Hand.MAIN_HAND, phase, 10L, 14);
            assertEquals(phase == AbilityVisualEvent.Phase.APPLIED,
                    AbilityVisualPolicy.allowedForPlayback(event));
        }
    }

    @Test void invalidActorActionHandOrDurationCannotTriggerRenewalVisual() {
        var wrong = new AbilityVisualEvent("mkm:arcane_bolt",
                AbilityVisualPolicy.RENEWAL_ACTION_ID,
                42, 1, AbilityVisualEvent.Hand.MAIN_HAND,
                AbilityVisualEvent.Phase.APPLIED, 10, 14);
        assertFalse(AbilityVisualPolicy.allowedForPlayback(wrong));
        assertFalse(AbilityVisualPolicy.allowedForPlayback(new AbilityVisualEvent(
                "mkm:renewal", "mkm:different", 42, 1,
                AbilityVisualEvent.Hand.MAIN_HAND, AbilityVisualEvent.Phase.APPLIED, 10, 14)));
        assertFalse(AbilityVisualPolicy.allowedForPlayback(new AbilityVisualEvent(
                "mkm:renewal", AbilityVisualPolicy.RENEWAL_ACTION_ID, 42, 1,
                AbilityVisualEvent.Hand.OFF_HAND, AbilityVisualEvent.Phase.APPLIED, 10, 14)));
        assertFalse(AbilityVisualPolicy.allowedForPlayback(new AbilityVisualEvent(
                "mkm:renewal", AbilityVisualPolicy.RENEWAL_ACTION_ID, 42, 1,
                AbilityVisualEvent.Hand.MAIN_HAND, AbilityVisualEvent.Phase.APPLIED, 10, 15)));
    }

    @Test void clientGateFiltersDuplicatesReorderedAndWrongActor() {
        var gate = new AbilityVisualReceiptGate();
        assertTrue(gate.accept(valid(1), 42));
        assertFalse(gate.accept(valid(1), 42));
        assertFalse(gate.accept(valid(2), 43));
        assertTrue(gate.accept(valid(2), 42));
        assertFalse(gate.accept(valid(1), 42));
        gate.reset();
        assertTrue(gate.accept(valid(1), 42));
    }
}
