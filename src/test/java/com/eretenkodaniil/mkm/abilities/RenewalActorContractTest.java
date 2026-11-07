package com.eretenkodaniil.mkm.abilities;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenewalActorContractTest {
    private static final RenewalParameters TERMS = new RenewalParameters(
            100D, 1D, 40L, 20D, 60L, 6F);

    @Test void sameSpendAndCooldownForPlayerAndTaggedHostileAi() {
        ManaLedger initial = new ManaLedger(100D, 100L, 100L, Map.of());
        ManaLedger player = RenewalCastPlan.spendIfAllowed(
                true, true, true, initial, 100, TERMS);
        ManaLedger enemy = RenewalCastPlan.spendIfAllowed(
                true, true, true, initial, 100, TERMS);
        assertNotNull(player);
        assertEquals(player, enemy);
        assertEquals(80D, player.mana(), 1E-6);
        assertEquals(160L, player.readyTick("mkm:renewal"));
        // Duplicated C2S packet or repeated AI tick cannot debit or heal twice.
        assertNull(RenewalCastPlan.spendIfAllowed(
                true, true, true, player, 100, TERMS));
        assertNull(RenewalCastPlan.spendIfAllowed(
                true, true, true, player, 120, TERMS));
    }

    @Test void invalidActorBusyFullHealthAndLowManaHaveNoLedgerMutation() {
        ManaLedger original = new ManaLedger(100D, 100L, 100L, Map.of());
        assertNull(RenewalCastPlan.spendIfAllowed(false,true,true,original,100,TERMS));
        assertNull(RenewalCastPlan.spendIfAllowed(true,false,true,original,100,TERMS));
        assertNull(RenewalCastPlan.spendIfAllowed(true,true,false,original,100,TERMS));
        ManaLedger empty = new ManaLedger(0D, 100L, 100L, Map.of());
        assertNull(RenewalCastPlan.spendIfAllowed(true,true,true,empty,100,TERMS));
        assertEquals(Map.of(), original.cooldowns());
        assertEquals(100D, original.mana(), 1E-6);
    }

    @Test void hostileMustHaveRealTargetRangeLineOfSightAndOptIn() {
        var good = new HostileRenewalRules.Facts(
                true, true, true, true, true, true, true, true, true, 49D);
        assertTrue(HostileRenewalRules.allowed(good));
        assertFalse(HostileRenewalRules.allowed(new HostileRenewalRules.Facts(
                false,true,true,true,true,true,true,true,true,49D)));
        assertFalse(HostileRenewalRules.allowed(new HostileRenewalRules.Facts(
                true,true,true,true,true,true,false,true,true,49D)));
        assertFalse(HostileRenewalRules.allowed(new HostileRenewalRules.Facts(
                true,true,true,true,true,false,true,true,true,49D)));
        assertFalse(HostileRenewalRules.allowed(new HostileRenewalRules.Facts(
                true,true,true,true,true,true,true,true,true,257D)));
        assertFalse(HostileRenewalRules.allowed(new HostileRenewalRules.Facts(
                true,true,true,true,true,true,true,true,true,Double.NaN)));
        assertFalse(HostileRenewalRules.allowed(new HostileRenewalRules.Facts(
                true,true,true,true,true,true,true,false,true,49D)));
    }

    @Test void permissionAndRayProofRequiredEvenForGrantedArcaneBolt() {
        var bolt = AbilityCatalog.get("mkm:arcane_bolt");
        assertTrue(AbilityTargetPolicy.allowed(AbilityCatalog.RENEWAL,
                new AbilityTargetPolicy.Evidence(true,true,true,true,
                        false,false,false,0D), 0D)); // SELF: no ray needed
        assertFalse(AbilityTargetPolicy.allowed(bolt,
                new AbilityTargetPolicy.Evidence(true,true,false,true,
                        false,true,true,9D),12D));
        assertFalse(AbilityTargetPolicy.allowed(bolt,
                new AbilityTargetPolicy.Evidence(true,true,false,true,
                        true,false,true,9D),12D));
        assertFalse(AbilityTargetPolicy.allowed(bolt,
                new AbilityTargetPolicy.Evidence(true,true,false,true,
                        true,true,false,9D),12D));
        assertFalse(AbilityTargetPolicy.allowed(bolt,
                new AbilityTargetPolicy.Evidence(true,true,false,false,
                        true,true,true,9D),12D));
        assertTrue(AbilityTargetPolicy.allowed(bolt,
                new AbilityTargetPolicy.Evidence(true,true,false,true,
                        true,true,true,144D),12D));
        assertFalse(AbilityGrantPolicy.executable(
                new AbilityGrantPolicy.Grants(AbilityGrantPolicy.Actor.PLAYER,
                        AbilityArchetypes.MAGE,null,
                        java.util.Set.of(),java.util.Set.of(),java.util.Set.of()),
                bolt.id())); // targeting proof NEVER authorizes damage
    }
}
