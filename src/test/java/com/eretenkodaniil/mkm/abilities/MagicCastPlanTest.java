package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEvent;
import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualPolicy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagicCastPlanTest {
    static ManaLedger plan(String id,ManaLedger previous,boolean actor,boolean idle,boolean allowed,long now) {
        return AbilityCastPlan.spendIfAllowed(AbilityCatalog.get(id),
                actor,idle,allowed,previous,now,100D,5D,1D,1D,2D);
    }

    @Test void restorationAndAbjurationShareExactlyOneDebitAndCooldown() {
        var initial=new ManaLedger(100D,100L,100L,Map.of());
        var renewal=plan("mkm:renewal",initial,true,true,true,100);
        assertNotNull(renewal);
        assertEquals(80D,renewal.mana(),1e-8);
        assertEquals(160L,renewal.readyTick("mkm:renewal"));
        assertNull(plan("mkm:renewal",renewal,true,true,true,100));
        var ward=plan("mkm:mage_ward",initial,true,true,true,100);
        assertNotNull(ward);
        assertEquals(75D,ward.mana(),1e-8);
        assertEquals(280L,ward.readyTick("mkm:mage_ward"));
        assertNull(plan("mkm:mage_ward",ward,true,true,true,101));
    }

    @Test void invalidActorBusyEffectAlreadyActiveAndNoManaNeverSpend() {
        var full=new ManaLedger(100D,100L,100L,Map.of());
        assertNull(plan("mkm:mage_ward",full,false,true,true,100));
        assertNull(plan("mkm:mage_ward",full,true,false,true,100));
        assertNull(plan("mkm:mage_ward",full,true,true,false,100));
        var empty=new ManaLedger(1D,100L,100L,Map.of());
        assertNull(plan("mkm:mage_ward",empty,true,true,true,100));
        assertFalse(ArcaneWardEffect.eligible(true,true));
        assertTrue(ArcaneWardEffect.eligible(true,false));
        assertEquals(100D,full.mana(),1e-8);
    }

    @Test void spellsWithUnapprovedCastChannelOrDamageStillDenied() {
        var full=new ManaLedger(100D,100L,100L,Map.of());
        assertNull(plan("mkm:arcane_bolt",full,true,true,true,100));
        assertNull(plan("mkm:arcane_barrier",full,true,true,true,100));
        assertNull(plan("mkm:repulsion_wave",full,true,true,true,100));
        assertEquals(8,AbilityCatalog.get("mkm:arcane_bolt").castTicks());
    }

    @Test void visualAllowlistRequiresExactServerAppliedIdActionAndDuration() {
        var renewal=new AbilityVisualEvent("mkm:renewal","mkm:renewal_cast",
                42,1,AbilityVisualEvent.Hand.MAIN_HAND,
                AbilityVisualEvent.Phase.APPLIED,100L,14,"minecraft:overworld");
        var ward=new AbilityVisualEvent("mkm:mage_ward","mkm:arcane_ward_cast",
                42,2,AbilityVisualEvent.Hand.MAIN_HAND,
                AbilityVisualEvent.Phase.APPLIED,101L,20,"minecraft:overworld");
        assertTrue(AbilityVisualPolicy.allowedForPlayback(renewal));
        assertTrue(AbilityVisualPolicy.allowedForPlayback(ward));
        assertFalse(AbilityVisualPolicy.allowedForPlayback(new AbilityVisualEvent(
                ward.abilityId(),ward.visualActionId(),42,3,
                AbilityVisualEvent.Hand.MAIN_HAND,AbilityVisualEvent.Phase.STARTED,
                102L,20,"minecraft:overworld")));
        assertTrue(AbilityVisualPolicy.renewalApplied(true,true,10F,12F));
        assertFalse(AbilityVisualPolicy.renewalApplied(true,true,10F,10F));
        assertFalse(AbilityVisualPolicy.wardApplied(true,true,false));
    }
}
