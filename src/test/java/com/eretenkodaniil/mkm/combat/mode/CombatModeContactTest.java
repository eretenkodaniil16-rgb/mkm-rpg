package com.eretenkodaniil.mkm.combat.mode;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.combat.net.CombatModeContact;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

/**
 * Actual event/receipt codec is server-authoritative and immutable. UI/MNAE
 * never turns one confirmed receipt into another attack or damage call.
 */
final class CombatModeContactTest {
    @Test void contactCodecRoundTripAndStrictValidation() {
        var hit = new CombatModeContact(42, (byte) 2, 420L, true, 7.5F);
        var buf = Unpooled.buffer();
        try {
            CombatModeContact.STREAM_CODEC.encode(buf, hit);
            assertEquals(hit, CombatModeContact.STREAM_CODEC.decode(buf));
        } finally { buf.release(); }
        var miss = new CombatModeContact(-1, (byte) 1, 430L, false, 0F);
        var buf2 = Unpooled.buffer();
        try {
            CombatModeContact.STREAM_CODEC.encode(buf2, miss);
            assertEquals(miss, CombatModeContact.STREAM_CODEC.decode(buf2));
        } finally { buf2.release(); }
        assertThrows(IllegalArgumentException.class,
                () -> new CombatModeContact(1, (byte) 3, 10, true, 1F));
        assertThrows(IllegalArgumentException.class,
                () -> new CombatModeContact(-1, (byte) 1, 10, true, 1F));
        assertThrows(IllegalArgumentException.class,
                () -> new CombatModeContact(1, (byte) 1, 10, false, 5F));
        assertThrows(IllegalArgumentException.class,
                () -> new CombatModeContact(1, (byte) 1, 10, true, Float.NaN));
    }
}
