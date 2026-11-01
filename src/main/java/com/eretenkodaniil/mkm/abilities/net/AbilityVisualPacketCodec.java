package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEvent;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * Strict versioned S2C cosmetic payload. Both IDs are bounded ASCII. No
 * damage, healing amount, target ID, mana or attribute data are transmitted.
 */
public final class AbilityVisualPacketCodec {
    public static final int VERSION = 2;
    // v1 maximum 153 + dimension ID length (one byte + 128 ASCII bytes).
    public static final int MAX_WIRE_BYTES = 282;

    private AbilityVisualPacketCodec() {}

    public static void encode(ByteBuf buf, AbilityVisualEvent event) {
        if (event == null) throw new IllegalArgumentException("Missing cosmetic event");
        buf.writeByte(VERSION);
        AbilityIntentCodec.encode(buf, event.abilityId());
        AbilityIntentCodec.encode(buf, event.visualActionId());
        buf.writeInt(event.actorEntityId());
        buf.writeInt(event.sequence());
        buf.writeByte(event.hand().ordinal());
        buf.writeByte(event.phase().ordinal());
        buf.writeLong(event.serverTick());
        buf.writeInt(event.visualDurationTicks());
        byte[] dimension = event.dimensionId().getBytes(StandardCharsets.US_ASCII);
        buf.writeByte(dimension.length);
        buf.writeBytes(dimension);
    }

    public static AbilityVisualEvent decode(ByteBuf buf) {
        if (buf.readableBytes() > MAX_WIRE_BYTES) {
            throw new IllegalArgumentException("Excessive ability visual packet");
        }
        int version = buf.readUnsignedByte();
        if (version != VERSION) throw new IllegalArgumentException("Unknown ability visual protocol");
        String ability = AbilityIntentCodec.decode(buf);
        String action = AbilityIntentCodec.decode(buf);
        int actorId = buf.readInt();
        int sequence = buf.readInt();
        int handCode = buf.readUnsignedByte();
        int phaseCode = buf.readUnsignedByte();
        if (handCode >= AbilityVisualEvent.Hand.values().length
                || phaseCode >= AbilityVisualEvent.Phase.values().length) {
            throw new IllegalArgumentException("Invalid ability presentation enum");
        }
        long tick = buf.readLong();
        int duration = buf.readInt();
        int dimensionLength = buf.readUnsignedByte();
        if (dimensionLength < 1 || dimensionLength > AbilityVisualEvent.MAX_DIMENSION_ID_BYTES)
            throw new IllegalArgumentException("Invalid dimension identifier length");
        byte[] rawDimension = new byte[dimensionLength];
        buf.readBytes(rawDimension);
        for (byte b : rawDimension) {
            if (b < 32 || b > 126) throw new IllegalArgumentException("Non-ASCII dimension ID");
        }
        String dimension = new String(rawDimension, StandardCharsets.US_ASCII);
        if (buf.isReadable()) {
            throw new IllegalArgumentException("Unexpected trailing ability visual bytes");
        }
        return new AbilityVisualEvent(ability, action, actorId, sequence,
                AbilityVisualEvent.Hand.values()[handCode],
                AbilityVisualEvent.Phase.values()[phaseCode], tick, duration, dimension);
    }
}
