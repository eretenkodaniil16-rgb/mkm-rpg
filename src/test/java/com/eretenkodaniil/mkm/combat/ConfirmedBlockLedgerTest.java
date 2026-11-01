package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ConfirmedBlockLedgerTest {
    @Test void duplicateCallbackWithoutNewAttemptDoesNotDebitTwice() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object();
        Object source = new Object();
        ledger.begin(defender, source, 100);
        assertTrue(ledger.consume(defender, source, 100));
        assertFalse(ledger.consume(defender, source, 100));
        assertFalse(ledger.consume(defender, source, 101));
    }

    @Test void reusedDamageSourceWithNewAttemptIsARealSecondBlock() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object();
        Object reusedSource = new Object();
        ledger.begin(defender, reusedSource, 100);
        assertTrue(ledger.consume(defender, reusedSource, 100));
        ledger.begin(defender, reusedSource, 100);
        assertTrue(ledger.consume(defender, reusedSource, 100));
        ledger.begin(defender, reusedSource, 101);
        assertTrue(ledger.consume(defender, reusedSource, 101));
    }

    @Test void canceledAttemptCannotBlockLaterRealHitFromDifferentSource() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object();
        Object cancelled = new Object();
        Object actual = new Object();
        ledger.begin(defender, cancelled, 100);
        ledger.begin(defender, actual, 101);
        assertFalse(ledger.consume(defender, new Object(), 101));
        assertTrue(ledger.consume(defender, actual, 101));
        assertFalse(ledger.consume(defender, cancelled, 103));
    }

    @Test void differentPlayersAndSourcesRemainIndependent() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object a = new Object(), b = new Object();
        Object source1 = new Object(), source2 = new Object();
        ledger.begin(a, source1, 100);
        ledger.begin(a, source2, 100);
        ledger.begin(b, source1, 100);
        assertTrue(ledger.consume(a, source2, 100));
        assertTrue(ledger.consume(b, source1, 100));
        assertTrue(ledger.consume(a, source1, 100));
        assertFalse(ledger.consume(a, source1, 100));
    }

    @Test void boundsStaleAndMalformedCallbacks() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object player = new Object(), source = new Object();
        assertFalse(ledger.consume(player, source, 100));
        ledger.begin(player, source, 100);
        assertFalse(ledger.consume(player, source, 102));
        ledger.begin(player, source, 100);
        assertFalse(ledger.consume(player, source, 99));
        ledger.begin(player, source, 101);
        assertTrue(ledger.consume(player, source, 102));
        ledger.begin(player, source, 102);
        ledger.clear(player);
        assertFalse(ledger.consume(player, source, 102));
        assertFalse(ledger.consume(player, source, -1));
    }

    @Test void boundedPendingQueueDoesNotPermitDoubleConsumption() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object player = new Object();
        Object source = new Object();
        for (int i = 0; i < 100; i++) {
            ledger.begin(player, new Object(), 100);
        }
        ledger.begin(player, source, 100);
        assertTrue(ledger.consume(player, source, 100));
        assertFalse(ledger.consume(player, source, 100));
    }
}
