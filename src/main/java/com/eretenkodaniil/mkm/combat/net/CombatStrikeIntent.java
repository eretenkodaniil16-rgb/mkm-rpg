package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 0 PRESS, 1 RELEASE, 2 CANCEL + monotonically increasing client nonce.
 * Server ignores client-selected targets, timing, damage and movement.
 */
public record CombatStrikeIntent(byte phase, int sequence) implements CustomPacketPayload {
    public static final Type<CombatStrikeIntent> TYPE = new Type<>(MkmMod.id("combat_strike_intent"));
    public static final StreamCodec<ByteBuf, CombatStrikeIntent> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public CombatStrikeIntent decode(ByteBuf buf) {
                    return new CombatStrikeIntent(buf.readByte(), buf.readInt());
                }
                @Override public void encode(ByteBuf buf, CombatStrikeIntent value) {
                    buf.writeByte(value.phase());
                    buf.writeInt(value.sequence());
                }
            };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
