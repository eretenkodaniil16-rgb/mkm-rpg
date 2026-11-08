package com.eretenkodaniil.mkm.character.origin.net;

import com.eretenkodaniil.mkm.character.origin.CharacterOriginService;
import com.eretenkodaniil.mkm.character.origin.OriginClassActionService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server-only validation: no client-side XP, item, effect or unlock mutation. */
public final class CharacterOriginNetworking {
    private CharacterOriginNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(
                OriginSelectionRequest.TYPE, OriginSelectionRequest.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                OriginSelectionResult.TYPE, OriginSelectionResult.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                OriginClassActionRequest.TYPE, OriginClassActionRequest.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OriginClassActionRequest.TYPE,
                (payload, context) -> OriginClassActionService.execute(context.player(), payload.intent()));
        ServerPlayNetworking.registerGlobalReceiver(OriginSelectionRequest.TYPE, (payload, context) -> {
            var result = CharacterOriginService.choose(context.player(),
                    payload.raceId(), payload.classId(), payload.expectedRevision());
            ServerPlayNetworking.send(context.player(),
                    new OriginSelectionResult(payload.requestId(), result.outcome()));
        });
    }
}
