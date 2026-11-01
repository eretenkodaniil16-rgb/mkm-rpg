package com.eretenkodaniil.mkm.abilities;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityRulesTest {
    @Test void catalogDefinitionsAreStable() {
        assertEquals(5, AbilityCatalog.all().size());
        assertSame(AbilityCatalog.RENEWAL, AbilityCatalog.get("mkm:renewal"));
        assertEquals(AbilityDefinition.TargetMode.SELF, AbilityCatalog.RENEWAL.targetMode());
        assertEquals(AbilityDefinition.Kind.PASSIVE, AbilityCatalog.get("mkm:iron_will").kind());
        assertNull(AbilityCatalog.get("mkm:forged_spell"));
    }

    @Test void invalidDefinitionsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AbilityDefinition(
                "bad:id", AbilityDefinition.Kind.ACTIVE, AbilityDefinition.TargetMode.SELF,
                AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.HEAL, 10, 0, 30, 5));
        assertThrows(IllegalArgumentException.class, () -> new AbilityDefinition(
                "mkm:cheat", AbilityDefinition.Kind.PASSIVE, AbilityDefinition.TargetMode.NONE,
                AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.MODIFIER, 10, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new AbilityDefinition(
                "mkm:negative", AbilityDefinition.Kind.ACTIVE, AbilityDefinition.TargetMode.SELF,
                AbilityDefinition.Resource.MANA, AbilityDefinition.Effect.HEAL, 10, 0, 0, Double.NaN));
    }

    @Test void sharedModifierInputsScaleCostsAndCooldowns() {
        assertEquals(15D, AbilityRules.scaledCost(20D, 0.75D), 1e-9);
        assertEquals(30L, AbilityRules.scaledCooldown(60, 2D));
        assertEquals(120L, AbilityRules.scaledCooldown(60, 0.5D));
        assertEquals(40L, AbilityRules.regenDelayTicks(2D));
        assertThrows(IllegalArgumentException.class,
                () -> AbilityRules.scaledCost(20D, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> AbilityRules.scaledCooldown(60, 0D));
    }

    @Test void regenerationRespectsDelayMaximumAndTickOrder() {
        ManaLedger state = new ManaLedger(10D, 100L, 120L, Map.of());
        assertEquals(10D, AbilityRules.regenerate(state, 115L, 100D, 5D).mana(), 1e-9);
        assertEquals(15D, AbilityRules.regenerate(state, 140L, 100D, 5D).mana(), 1e-9);
        assertEquals(12D, AbilityRules.regenerate(state, 10_000L, 12D, 50D).mana(), 1e-9);
        assertEquals(10D, AbilityRules.regenerate(state, 99L, 100D, 5D).mana(), 1e-9);
    }

    @Test void acceptedCastPaysExactlyOnceAndReservesCooldown() {
        ManaLedger ready = new ManaLedger(50D, 100L, 100L, Map.of());
        ManaLedger paid = AbilityRules.spend(ready, AbilityCatalog.RENEWAL, 100L, 20D, 60L, 40L);
        assertNotNull(paid);
        assertEquals(30D, paid.mana(), 1e-9);
        assertEquals(160L, paid.readyTick("mkm:renewal"));
        assertEquals(140L, paid.regenReadyTick());
        assertNull(AbilityRules.spend(paid, AbilityCatalog.RENEWAL, 100L, 20D, 60L, 40L));
        assertNull(AbilityRules.spend(paid, AbilityCatalog.RENEWAL, 159L, 20D, 60L, 40L));
        ManaLedger again = AbilityRules.spend(paid, AbilityCatalog.RENEWAL, 160L, 20D, 60L, 40L);
        assertNotNull(again);
        assertEquals(10D, again.mana(), 1e-9);
        assertEquals(220L, again.readyTick("mkm:renewal"));
    }

    @Test void insufficientManaNeverStartsCooldown() {
        ManaLedger low = new ManaLedger(19.99D, 10L, 10L, Map.of());
        assertNull(AbilityRules.spend(low, AbilityCatalog.RENEWAL, 20L, 20D, 60L, 40L));
        assertEquals(0L, low.readyTick("mkm:renewal"));
        assertEquals(19.99D, low.mana(), 1e-9);
    }

    @Test void noninstantSpellsAndPassivesAreNotExecutable() {
        ManaLedger full = new ManaLedger(100D, 10L, 10L, Map.of());
        assertNull(AbilityRules.spend(full, AbilityCatalog.get("mkm:arcane_bolt"), 20L, 10D, 20L, 10L));
        assertNull(AbilityRules.spend(full, AbilityCatalog.get("mkm:iron_will"), 20L, 0D, 0L, 0L));
    }

    @Test void ledgerDefensiveCopyAndCapacity() {
        Map<String, Long> input = new HashMap<>();
        input.put("mkm:renewal", 99L);
        ManaLedger state = new ManaLedger(100D, 10L, 10L, input);
        input.clear();
        assertEquals(99L, state.readyTick("mkm:renewal"));
        assertThrows(UnsupportedOperationException.class, () -> state.cooldowns().put("mkm:renewal", 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new ManaLedger(Double.POSITIVE_INFINITY, 0, 0, Map.of()));
        Map<String, Long> many = new HashMap<>();
        for (int i = 0; i <= ManaLedger.MAX_COOLDOWNS; i++) many.put("mkm:test_" + i, (long) i);
        assertThrows(IllegalArgumentException.class, () -> new ManaLedger(5, 0, 0, many));
    }
}
