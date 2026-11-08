package com.eretenkodaniil.mkm.showcase.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShowcaseChoiceRequest(byte action, long nonce) implements CustomPacketPayload {
    public static final byte CLOSE=0, DEFEND=1, STUDY=2, FINISH=3;
    public static final Type<ShowcaseChoiceRequest> TYPE =
            new Type<>(MkmMod.id("showcase_choice"));
    public static final StreamCodec<ByteBuf,ShowcaseChoiceRequest> CODEC = new StreamCodec<>() {
        @Override public ShowcaseChoiceRequest decode(ByteBuf b) {
            return new ShowcaseChoiceRequest(b.readByte(),b.readLong());
        }
        @Override public void encode(ByteBuf b, ShowcaseChoiceRequest v) {
            b.writeByte(v.action());b.writeLong(v.nonce());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
