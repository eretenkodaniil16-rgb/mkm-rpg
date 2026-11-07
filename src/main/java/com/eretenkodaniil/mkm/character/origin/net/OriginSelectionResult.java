package com.eretenkodaniil.mkm.character.origin.net;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Reply cannot mutate profile; the owner-only attachment sync remains authoritative. */
public record OriginSelectionResult(int requestId, CharacterOriginRules.Outcome status)
        implements CustomPacketPayload {
    public static final Type<OriginSelectionResult> TYPE = new Type<>(MkmMod.id("origin_selection_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OriginSelectionResult> STREAM_CODEC =
            new StreamCodec<>() {
                @Override public OriginSelectionResult decode(RegistryFriendlyByteBuf buf) {
                    int requestId = buf.readVarInt();
                    int value = buf.readUnsignedByte();
                    var statuses = CharacterOriginRules.Outcome.values();
                    if (value >= statuses.length) throw new IllegalArgumentException("Invalid origin reply");
                    return new OriginSelectionResult(requestId, statuses[value]);
                }
                @Override public void encode(RegistryFriendlyByteBuf buf, OriginSelectionResult reply) {
                    buf.writeVarInt(reply.requestId);
                    buf.writeByte(reply.status.ordinal());
                }
            };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
