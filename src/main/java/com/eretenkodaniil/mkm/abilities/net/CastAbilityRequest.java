package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** An ability ID only. All gameplay effects and eligibility remain server-owned. */
public record CastAbilityRequest(String abilityId) implements CustomPacketPayload {
    public static final Type<CastAbilityRequest> TYPE = new Type<>(MkmMod.id("cast_ability"));
    public static final StreamCodec<ByteBuf, CastAbilityRequest> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CastAbilityRequest decode(ByteBuf buf) {
            return new CastAbilityRequest(AbilityIntentCodec.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, CastAbilityRequest value) {
            AbilityIntentCodec.encode(buf, value.abilityId());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
