package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.MkmMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Authoritative SERVER->OWNER contact receipt for the optional sword mode.
 * NOT a request to hit anything: MNAE can observe, never replay damage.
 * kind: 1 quick; 2 charged. targetId -1 means no server-selected target.
 */
public record CombatModeContact(int targetEntityId, byte kind, long serverGameTick,
                                boolean confirmedHealthHit, float actualHealthDamage)
        implements CustomPacketPayload {
    public static final Type<CombatModeContact> TYPE = new Type<>(MkmMod.id("combat_mode_contact"));
    public static final StreamCodec<ByteBuf, CombatModeContact> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public CombatModeContact decode(ByteBuf buf) {
                    return new CombatModeContact(buf.readInt(), buf.readByte(), buf.readLong(),
                            buf.readBoolean(), buf.readFloat());
                }
                @Override public void encode(ByteBuf buf, CombatModeContact value) {
                    buf.writeInt(value.targetEntityId());
                    buf.writeByte(value.kind());
                    buf.writeLong(value.serverGameTick());
                    buf.writeBoolean(value.confirmedHealthHit());
                    buf.writeFloat(value.actualHealthDamage());
                }
            };

    public CombatModeContact {
        if (targetEntityId < -1 || (kind != 1 && kind != 2) || serverGameTick < 0L
                || !Float.isFinite(actualHealthDamage) || actualHealthDamage < 0F
                || (confirmedHealthHit != (actualHealthDamage > 0F))
                || (confirmedHealthHit && targetEntityId < 0)) {
            throw new IllegalArgumentException("Invalid server contact receipt");
        }
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
