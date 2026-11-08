package com.eretenkodaniil.mkm.showcase.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** View is informational: server independently revalidates origin, stage and all choices. */
public record ShowcaseDecisionView(int x, int z, byte mode, boolean studyEligible,
                                   byte branch, long nonce) implements CustomPacketPayload {
    public static final byte SELECT=1, RESOLVE=2;
    public static final Type<ShowcaseDecisionView> TYPE =
            new Type<>(MkmMod.id("showcase_decision"));
    public static final StreamCodec<ByteBuf,ShowcaseDecisionView> CODEC = new StreamCodec<>() {
        @Override public ShowcaseDecisionView decode(ByteBuf b) {
            return new ShowcaseDecisionView(b.readInt(),b.readInt(),b.readByte(),
                    b.readBoolean(),b.readByte(),b.readLong());
        }
        @Override public void encode(ByteBuf b, ShowcaseDecisionView v) {
            b.writeInt(v.x());b.writeInt(v.z());b.writeByte(v.mode());
            b.writeBoolean(v.studyEligible());b.writeByte(v.branch());b.writeLong(v.nonce());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
