package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.combat.ServerSpecialAttackContract.*;
import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;
import org.junit.jupiter.api.Test;

final class ServerSpecialAttackContractTest {
    private static Intent intent() {
        return new Intent("mkm:enemy_guard_strike", Delivery.NATIVE_MELEE,
                AttackPresentationEvent.Hand.MAIN_HAND, EquipmentRules.Family.AXE,
                PhysicalDamageType.SLASHING, 1, 1024L);
    }

    private static ServerEvidence checks(boolean server, boolean target, boolean paid,
                                         boolean once) {
        return new ServerEvidence(server, true, target, true, true, true, paid, once);
    }

    @Test void localAnimationPredictionCannotAuthorizeServerDamage() {
        assertEquals(Decision.PRESENTATION_ONLY, ServerSpecialAttackContract.assess(
                Owner.CLIENT_PREVIEW, intent(), checks(true, true, true, true)));
        assertEquals(Decision.REJECTED, ServerSpecialAttackContract.assess(
                Owner.SERVER_AI, intent(), checks(false, true, true, true)));
        assertEquals(Decision.REJECTED, ServerSpecialAttackContract.assess(
                Owner.SERVER_CLASS, intent(), checks(true, false, true, true)));
        assertEquals(Decision.REJECTED, ServerSpecialAttackContract.assess(
                Owner.SERVER_AI, intent(), checks(true, true, false, true)));
        assertEquals(Decision.REJECTED, ServerSpecialAttackContract.assess(
                Owner.SERVER_AI, intent(), checks(true, true, true, false)));
        assertEquals(Decision.READY_FOR_VANILLA, ServerSpecialAttackContract.assess(
                Owner.SERVER_AI, intent(), checks(true, true, true, true)));
        // READY_FOR_VANILLA is a policy result, NOT a damage-dealing method.
    }

    @Test void serverMintedAttemptIdsRejectReplayAndCrossPlayerLeakage() {
        var ledger = new ServerAttackReceiptGate<Object>();
        Object mobA = new Object(), mobB = new Object();
        assertTrue(ledger.claim(mobA, 10L));
        assertFalse(ledger.claim(mobA, 10L));
        assertFalse(ledger.claim(mobA, 9L));
        assertTrue(ledger.claim(mobA, 11L));
        assertTrue(ledger.claim(mobB, 10L));
        assertFalse(ledger.claim(mobA, 0L));
        ledger.clear(mobA);
        assertTrue(ledger.claim(mobA, 1L));
    }

    @Test void malformedIntentAndCrossAuthorityAreDenied() {
        assertThrows(IllegalArgumentException.class, () ->
                new Intent("not namespaced", Delivery.NATIVE_MELEE,
                        AttackPresentationEvent.Hand.MAIN_HAND, EquipmentRules.Family.SWORD,
                        PhysicalDamageType.SLASHING, 1, 1L));
        assertThrows(IllegalArgumentException.class, () ->
                new Intent("mkm:invalid_step", Delivery.NATIVE_MELEE,
                        AttackPresentationEvent.Hand.MAIN_HAND, EquipmentRules.Family.SWORD,
                        PhysicalDamageType.SLASHING, 100, 1L));
        assertThrows(NullPointerException.class, () ->
                ServerSpecialAttackContract.assess(null, intent(), checks(true,true,true,true)));
    }
}
