package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A single bounded ASCII ability ID. No client-supplied heal, target,
 * cooldown, mana or player statistics.
 */
public record CastAbilityRequest(String abilityId) implements CustomPacketPayload {
    public static final Type<CastAbilityRequest> TYPE = new Type<>(MkmMod.id("cast_ability"));
    public static final StreamCodec<ByteBuf, CastAbilityRequest> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CastAbilityRequest decode(ByteBuf buf) {
            int size = buf.readUnsignedByte();
            if (size == 0 || size > 64) throw new IllegalArgumentException("Invalid cast payload length");
            byte[] raw = new byte[size];
            buf.readBytes(raw);
            for (byte b : raw) {
                if (b < 32 || b > 126) throw new IllegalArgumentException("Non-ASCII ability ID");
            }
            return new CastAbilityRequest(new String(raw, StandardCharsets.US_ASCII));
        }

        @Override
        public void encode(ByteBuf buf, CastAbilityRequest value) {
            if (value.abilityId() == null || !value.abilityId().matches("mkm:[a-z0-9_./-]{1,54}")) {
                throw new IllegalArgumentException("Invalid cast ability identifier");
            }
            byte[] raw = value.abilityId().getBytes(StandardCharsets.US_ASCII);
            if (raw.length > 64) throw new IllegalArgumentException("Excessive cast payload");
            buf.writeByte(raw.length);
            buf.writeBytes(raw);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
