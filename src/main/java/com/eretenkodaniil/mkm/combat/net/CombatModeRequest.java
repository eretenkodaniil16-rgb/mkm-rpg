package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Only request enabling/disabling optional QA combat mode. */
public record CombatModeRequest(boolean enabled) implements CustomPacketPayload {
    public static final Type<CombatModeRequest> TYPE = new Type<>(MkmMod.id("combat_mode_request"));
    public static final StreamCodec<ByteBuf, CombatModeRequest> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public CombatModeRequest decode(ByteBuf buf) {
                    return new CombatModeRequest(buf.readBoolean());
                }
                @Override public void encode(ByteBuf buf, CombatModeRequest value) {
                    buf.writeBoolean(value.enabled());
                }
            };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
