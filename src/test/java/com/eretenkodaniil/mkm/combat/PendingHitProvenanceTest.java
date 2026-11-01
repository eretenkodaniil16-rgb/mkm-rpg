package com.eretenkodaniil.mkm.combat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class PendingHitProvenanceTest {
    @Test
    void matchesOnlyTheExactDamageSourceOnce() {
        var pending = new PendingHitProvenance<Object, Object>();
        Object target = new Object();
        Object source = new Object();
        pending.begin(target);
        pending.record(target, source, 100L);
        assertTrue(pending.consume(target, source, 100L));
        assertFalse(pending.consume(target, source, 100L));
    }

    @Test
    void differentAttackCannotInheritHeavyMarker() {
        var pending = new PendingHitProvenance<Object, Object>();
        Object target = new Object();
        Object source = new Object();
        pending.record(target, source, 100L);
        assertFalse(pending.consume(target, new Object(), 100L));
        assertFalse(pending.consume(target, source, 100L));
    }

    @Test
    void cancelledAttackIsReplacedByNewDamageAttempt() {
        var pending = new PendingHitProvenance<Object, Object>();
        Object target = new Object();
        pending.record(target, new Object(), 100L);
        pending.begin(target);
        assertFalse(pending.consume(target, new Object(), 101L));
    }

    @Test
    void lateOrBackwardsTimeCallbacksAreDiscarded() {
        var pending = new PendingHitProvenance<Object, Object>();
        Object target = new Object();
        Object source = new Object();
        pending.record(target, source, 100L);
        assertFalse(pending.consume(target, source, 102L));
        pending.record(target, source, 100L);
        assertFalse(pending.consume(target, source, 99L));
        pending.record(target, source, 100L);
        assertTrue(pending.consume(target, source, 101L));
    }

    @Test
    void independentTargetsDoNotInterfere() {
        var pending = new PendingHitProvenance<Object, Object>();
        Object first = new Object();
        Object second = new Object();
        Object source = new Object();
        pending.record(first, source, 100L);
        pending.record(second, source, 100L);
        assertTrue(pending.consume(first, source, 100L));
        assertTrue(pending.consume(second, source, 100L));
    }
}
