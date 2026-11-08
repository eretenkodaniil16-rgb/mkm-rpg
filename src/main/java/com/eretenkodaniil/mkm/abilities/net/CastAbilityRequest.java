package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.abilities.AbilityIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * v2 C2S intention: validated ID plus monotonic per-session request number.
 * NEVER accepts target, result, health, damage, cost, mana or cooldown.
 * Replayed sequence values are rejected by the authoritative server.
 */
public record CastAbilityRequest(String abilityId, int requestSequence)
        implements CustomPacketPayload {
    public static final int VERSION = 2;
    public static final int MAX_WIRE_BYTES = 1 + 1 + AbilityIntentCodec.MAX_BYTES + 4;
    public static final Type<CastAbilityRequest> TYPE = new Type<>(MkmMod.id("cast_ability"));

    public CastAbilityRequest {
        if (!AbilityIds.valid(abilityId) || requestSequence <= 0)
            throw new IllegalArgumentException("Invalid ability cast request");
    }

    public static final StreamCodec<ByteBuf, CastAbilityRequest> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CastAbilityRequest decode(ByteBuf buf) {
            if (buf.readableBytes() > MAX_WIRE_BYTES)
                throw new IllegalArgumentException("Oversized cast intent");
            if (buf.readUnsignedByte() != VERSION)
                throw new IllegalArgumentException("Unsupported cast intent protocol");
            String id = AbilityIntentCodec.decode(buf);
            int sequence = buf.readInt();
            if (buf.isReadable()) throw new IllegalArgumentException("Trailing cast intent bytes");
            return new CastAbilityRequest(id, sequence);
        }

        @Override
        public void encode(ByteBuf buf, CastAbilityRequest value) {
            buf.writeByte(VERSION);
            AbilityIntentCodec.encode(buf, value.abilityId());
            buf.writeInt(value.requestSequence());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
