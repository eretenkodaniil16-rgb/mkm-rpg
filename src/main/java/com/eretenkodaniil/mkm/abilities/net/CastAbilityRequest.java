package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Minimal cast intent: a bounded ID, never client-authoritative effect parameters. */
public record CastAbilityRequest(String abilityId) implements CustomPacketPayload {
    public static final Type<CastAbilityRequest> TYPE = new Type<>(MkmMod.id("cast_ability"));
    public static final StreamCodec<ByteBuf, CastAbilityRequest> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CastAbilityRequest decode(ByteBuf buf) {
            return new CastAbilityRequest(net.minecraft.network.FriendlyByteBuf.readUtf(buf, 64));
        }

        @Override
        public void encode(ByteBuf buf, CastAbilityRequest value) {
            net.minecraft.network.FriendlyByteBuf.writeUtf(buf, value.abilityId(), 64);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
