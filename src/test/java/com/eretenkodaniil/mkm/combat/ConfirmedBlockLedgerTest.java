package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ConfirmedBlockLedgerTest {
    @Test void duplicateCallbackWithoutNewAttemptDoesNotDebitTwice() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object(), source = new Object();
        ledger.begin(defender, source, 100);
        assertTrue(ledger.consume(defender, source, 100));
        assertFalse(ledger.consume(defender, source, 100));
        assertFalse(ledger.consume(defender, source, 101));
    }

    @Test void realSecondAttemptReusingSourceIsNotSuppressed() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object(), reusedSource = new Object();
        ledger.begin(defender, reusedSource, 100);
        assertTrue(ledger.consume(defender, reusedSource, 100));
        ledger.begin(defender, reusedSource, 100);
        assertTrue(ledger.consume(defender, reusedSource, 100));
        ledger.begin(defender, reusedSource, 101);
        assertTrue(ledger.consume(defender, reusedSource, 101));
    }

    @Test void cancelledAttemptCannotCausePhantomSecondCharge() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object(), sharedSource = new Object();
        ledger.begin(defender, sharedSource, 100); // cancelled: no AFTER_DAMAGE
        ledger.begin(defender, sharedSource, 100); // real second attempt
        assertTrue(ledger.consume(defender, sharedSource, 100));
        assertFalse(ledger.consume(defender, sharedSource, 100));
    }

    @Test void differentSourceReplacesStaleCancelledAttempt() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object defender = new Object(), cancelled = new Object(), real = new Object();
        ledger.begin(defender, cancelled, 100);
        ledger.begin(defender, real, 101);
        assertFalse(ledger.consume(defender, cancelled, 101));
        // Wrong callback intentionally consumes receipt, fail closed.
        ledger.begin(defender, real, 101);
        assertTrue(ledger.consume(defender, real, 101));
        assertFalse(ledger.consume(defender, cancelled, 103));
    }

    @Test void separatePlayersRemainIndependent() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object a = new Object(), b = new Object(), reusedSource = new Object();
        ledger.begin(a, reusedSource, 100);
        ledger.begin(b, reusedSource, 100);
        assertTrue(ledger.consume(a, reusedSource, 100));
        assertTrue(ledger.consume(b, reusedSource, 100));
    }

    @Test void staleTimeAndDisconnectCannotCharge() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object player = new Object(), source = new Object();
        assertFalse(ledger.consume(player, source, 100));
        ledger.begin(player, source, 100);
        assertFalse(ledger.consume(player, source, 102));
        ledger.begin(player, source, 100);
        assertFalse(ledger.consume(player, source, 99));
        ledger.begin(player, source, 100);
        assertTrue(ledger.consume(player, source, 101));
        ledger.begin(player, source, 100);
        ledger.clear(player);
        assertFalse(ledger.consume(player, source, 100));
        assertFalse(ledger.consume(player, source, -1));
    }

    @Test void repeatedCanceledAttemptsUseBoundedSingleReceipt() {
        var ledger = new ConfirmedBlockLedger<Object, Object>();
        Object player = new Object(), source = new Object();
        for (int i = 0; i < 1000; i++) ledger.begin(player, new Object(), 100);
        ledger.begin(player, source, 100);
        assertTrue(ledger.consume(player, source, 100));
        assertFalse(ledger.consume(player, source, 100));
    }
}
