package com.eretenkodaniil.mkm.abilities.net;

import com.eretenkodaniil.mkm.abilities.AbilityService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Separate ability network boundary; no CombatNetworking mutation. */
public final class AbilityNetworking {
    private AbilityNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(
                AbilityVisualPacket.TYPE, AbilityVisualPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                CastAbilityRequest.TYPE, CastAbilityRequest.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CastAbilityRequest.TYPE,
                (payload, context) -> AbilityService.tryCast(
                        context.player(), payload.abilityId(), payload.requestSequence()));
    }
}
