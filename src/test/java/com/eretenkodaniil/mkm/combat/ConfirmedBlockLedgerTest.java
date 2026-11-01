package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ConfirmedBlockLedgerTest {
    @Test void duplicateCallbackFromSameBlockDebitsOnlyOnce() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object();
        Object damageSource = new Object();
        assertTrue(ledger.record(defender, damageSource, 100));
        assertFalse(ledger.record(defender, damageSource, 100));
        assertFalse(ledger.record(defender, damageSource, 101));
        assertTrue(ledger.record(defender, damageSource, 102));
    }

    @Test void differentSourcesAndDifferentPlayersCanSpendIndependently() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object one = new Object();
        Object two = new Object();
        Object source = new Object();
        assertTrue(ledger.record(one, source, 100));
        assertTrue(ledger.record(one, new Object(), 100));
        assertTrue(ledger.record(two, source, 100));
        ledger.clear(one);
        assertTrue(ledger.record(one, source, 100));
    }

    @Test void timeRollbackClearsOldIdentityState() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object player = new Object();
        Object source = new Object();
        assertTrue(ledger.record(player, source, 100));
        assertTrue(ledger.record(player, source, 10));
        assertFalse(ledger.record(player, source, -1));
    }
}
