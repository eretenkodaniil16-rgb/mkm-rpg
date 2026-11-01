package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.abilities.AbilityIds;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/** Bounded ASCII resource identifier wire encoding (no gameplay authority). */
public final class AbilityIntentCodec {
    public static final int MAX_BYTES = 64;

    private AbilityIntentCodec() {}

    public static String decode(ByteBuf buf) {
        int size = buf.readUnsignedByte();
        if (size < 1 || size > MAX_BYTES) {
            throw new IllegalArgumentException("Invalid ability ID payload length");
        }
        byte[] raw = new byte[size];
        buf.readBytes(raw);
        for (byte b : raw) {
            if (b < 32 || b > 126) throw new IllegalArgumentException("Non-ASCII ability ID");
        }
        String id = new String(raw, StandardCharsets.US_ASCII);
        if (!AbilityIds.valid(id)) throw new IllegalArgumentException("Invalid ability ID");
        return id;
    }

    public static void encode(ByteBuf buf, String id) {
        if (!AbilityIds.valid(id)) {
            throw new IllegalArgumentException("Invalid ability ID");
        }
        byte[] raw = id.getBytes(StandardCharsets.US_ASCII);
        if (raw.length > MAX_BYTES) throw new IllegalArgumentException("Excessive ability ID");
        buf.writeByte(raw.length);
        buf.writeBytes(raw);
    }
}
