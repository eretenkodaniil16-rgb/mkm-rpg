package com.eretenkodaniil.mkm.abilities;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityBoundaryRegressionTest {
    private static final AbilityDefinition SECOND_SPELL = new AbilityDefinition(
            "mkm:second_spell", AbilityDefinition.Kind.ACTIVE, AbilityDefinition.TargetMode.SELF,
            AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.HEAL,
            5.0D, 0, 10, 1.0D);

    @Test void aWorldClockRollbackPreservesRemainingCooldownAndDelay() {
        ManaLedger saved = new ManaLedger(27D, 1_000L, 1_040L,
                Map.of("mkm:renewal", 1_060L, "mkm:second_spell", 900L));
        ManaLedger normalized = AbilityRules.reconcileClock(saved, 100L);
        assertEquals(100L, normalized.lastUpdateTick());
        assertEquals(140L, normalized.regenReadyTick());
        assertEquals(160L, normalized.readyTick("mkm:renewal"));
        assertFalse(normalized.cooldowns().containsKey("mkm:second_spell"));
        assertEquals(27D, AbilityRules.regenerate(normalized, 139L, 100D, 5D).mana(), 1e-9);
        assertEquals(32D, AbilityRules.regenerate(normalized, 160L, 100D, 5D).mana(), 1e-9);
        assertNull(AbilityRules.spend(normalized, AbilityCatalog.RENEWAL, 159L, 20D, 60L, 40L));
        assertNotNull(AbilityRules.spend(normalized, AbilityCatalog.RENEWAL, 160L, 20D, 60L, 40L));
    }

    @Test void clockRollbackWithAlreadyExpiredDeadlineDoesNotReopenCooldown() {
        ManaLedger saved = new ManaLedger(100D, 10_000L, 9_000L,
                Map.of("mkm:renewal", 9_000L));
        ManaLedger normalized = AbilityRules.reconcileClock(saved, 50L);
        assertEquals(0L, normalized.readyTick("mkm:renewal"));
        assertEquals(50L, normalized.regenReadyTick());
        assertNotNull(AbilityRules.spend(normalized, AbilityCatalog.RENEWAL, 50L, 20D, 60L, 40L));
    }

    @Test void staleCooldownEntriesDoNotExhaustFixedCapacity() {
        Map<String, Long> full = new HashMap<>();
        for (int i = 0; i < ManaLedger.MAX_COOLDOWNS; i++) full.put("mkm:expired_" + i, 50L);
        ManaLedger state = new ManaLedger(100D, 100L, 100L, full);
        ManaLedger paid = AbilityRules.spend(state, AbilityCatalog.RENEWAL,
                100L, 20D, 60L, 40L);
        assertNotNull(paid);
        assertEquals(1, paid.cooldowns().size());
        assertEquals(160L, paid.readyTick("mkm:renewal"));
    }

    @Test void activeCooldownsCannotExceedCapacity() {
        Map<String, Long> full = new HashMap<>();
        for (int i = 0; i < ManaLedger.MAX_COOLDOWNS; i++) full.put("mkm:active_" + i, 200L);
        ManaLedger state = new ManaLedger(100D, 100L, 100L, full);
        assertNull(AbilityRules.spend(state, AbilityCatalog.RENEWAL,
                100L, 20D, 60L, 40L));
        assertEquals(100D, state.mana(), 1e-9);
    }

    @Test void twoAbilitiesMaintainIndependentDeadlinesAndOneResourcePool() {
        ManaLedger fresh = new ManaLedger(100D, 20L, 20L, Map.of());
        ManaLedger first = AbilityRules.spend(fresh, AbilityCatalog.RENEWAL,
                20L, 20D, 60L, 40L);
        assertNotNull(first);
        ManaLedger second = AbilityRules.spend(first, SECOND_SPELL,
                20L, 5D, 10L, 40L);
        assertNotNull(second);
        assertEquals(75D, second.mana(), 1e-9);
        assertEquals(80L, second.readyTick(AbilityCatalog.RENEWAL.id()));
        assertEquals(30L, second.readyTick(SECOND_SPELL.id()));
        assertNull(AbilityRules.spend(second, SECOND_SPELL, 20L, 5D, 10L, 40L));
    }

    @Test void maximumManaChangeClampsExistingStateWithoutFreeFill() {
        ManaLedger old = new ManaLedger(150D, 50L, 50L, Map.of());
        assertEquals(80D, AbilityRules.regenerate(old, 60L, 80D, 1D).mana(), 1e-9);
        assertEquals(150.5D, AbilityRules.regenerate(old, 60L, 200D, 1D).mana(), 1e-9);
    }

    @Test void costAndDelayRejectNonFiniteOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> AbilityRules.scaledCost(Double.MAX_VALUE, 2D));
        assertThrows(IllegalArgumentException.class,
                () -> AbilityRules.regenDelayTicks(Double.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> AbilityRules.regenerate(new ManaLedger(), 10L, 100D, Double.POSITIVE_INFINITY));
        assertNull(AbilityRules.spend(new ManaLedger(), AbilityCatalog.RENEWAL,
                10L, Double.NaN, 60L, 40L));
        assertThrows(IllegalArgumentException.class,
                () -> AbilityRules.reconcileClock(new ManaLedger(), -1L));
    }

    @Test void availabilityRejectsBusyFullHealthAndDuplicatePacketBeforeSpend() {
        ManaLedger ready = new ManaLedger(100D, 100L, 100L, Map.of());
        assertEquals(RenewalEligibility.Result.INVALID_ACTOR,
                RenewalEligibility.check(false, true, true, ready, 100L, 20D));
        assertEquals(RenewalEligibility.Result.BUSY,
                RenewalEligibility.check(true, false, true, ready, 100L, 20D));
        assertEquals(RenewalEligibility.Result.FULL_HEALTH,
                RenewalEligibility.check(true, true, false, ready, 100L, 20D));
        assertEquals(RenewalEligibility.Result.READY,
                RenewalEligibility.check(true, true, true, ready, 100L, 20D));
        ManaLedger paid = AbilityRules.spend(ready, AbilityCatalog.RENEWAL,
                100L, 20D, 60L, 40L);
        assertNotNull(paid);
        assertEquals(RenewalEligibility.Result.COOLDOWN,
                RenewalEligibility.check(true, true, true, paid, 100L, 20D));
        assertEquals(RenewalEligibility.Result.LOW_MANA,
                RenewalEligibility.check(true, true, true,
                        new ManaLedger(19D, 100L, 100L, Map.of()), 100L, 20D));
        assertNull(AbilityRules.spend(paid, AbilityCatalog.RENEWAL,
                100L, 20D, 60L, 40L));
    }
}
