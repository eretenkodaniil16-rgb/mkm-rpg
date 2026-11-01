package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEvent;
import io.netty.buffer.ByteBuf;

/**
 * Strict versioned S2C cosmetic payload. Both IDs are bounded ASCII. No
 * damage, healing amount, target ID, mana or attribute data are transmitted.
 */
public final class AbilityVisualPacketCodec {
    public static final int VERSION = 1;
    // 1 version + 2 x (1-byte length + max 64 ASCII bytes)
    // + 2 x int + 2 x byte + long + int.
    public static final int MAX_WIRE_BYTES = 153;

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
        if (buf.isReadable()) {
            throw new IllegalArgumentException("Unexpected trailing ability visual bytes");
        }
        return new AbilityVisualEvent(ability, action, actorId, sequence,
                AbilityVisualEvent.Hand.values()[handCode],
                AbilityVisualEvent.Phase.values()[phaseCode], tick, duration);
    }
}
