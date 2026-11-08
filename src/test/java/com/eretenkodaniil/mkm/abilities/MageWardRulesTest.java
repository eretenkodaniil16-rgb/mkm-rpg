package com.eretenkodaniil.mkm.abilities;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class MageWardRulesTest {
    @Test void actualWardUsesSameServerManaLedgerAndNeverDoubleSpendsReplay() {
        var ward = AbilityCatalog.MAGE_WARD;
        assertEquals("mkm:mage_ward", ward.id());
        assertEquals(AbilityDefinition.Effect.BARRIER, ward.effect());
        var before = new ManaLedger(100.0D, 0, 0, java.util.Map.of());
        var paid = AbilityRules.spend(before, ward, 20, 25, 180, 20);
        assertNotNull(paid);
        assertEquals(75, paid.mana(), 1e-9);
        assertEquals(200L, paid.readyTick(ward.id()));
        assertNull(AbilityRules.spend(paid, ward, 20, 25, 180, 20));
        assertNull(AbilityRules.spend(new ManaLedger(20,0,0,java.util.Map.of()),
                ward, 20, 25, 180, 20));
        assertNotNull(AbilityRules.spend(paid, ward, 200, 25, 180, 20));
    }
    @Test void existingPublicRenewalAndCatalogEntriesRemainRegistered() {
        assertSame(AbilityCatalog.RENEWAL, AbilityCatalog.get("mkm:renewal"));
        assertSame(AbilityCatalog.MAGE_WARD, AbilityCatalog.get("mkm:mage_ward"));
        assertNotNull(AbilityCatalog.get("mkm:arcane_barrier"));
        assertNotEquals(AbilityCatalog.MAGE_WARD.id(), AbilityCatalog.RENEWAL.id());
    }
}
