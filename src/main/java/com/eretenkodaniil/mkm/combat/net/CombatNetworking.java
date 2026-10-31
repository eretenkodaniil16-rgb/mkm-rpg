package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.combat.CombatService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Serverbound actions receive validated intent only. */
public final class CombatNetworking {
    private CombatNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(DodgeRequest.TYPE, DodgeRequest.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DodgeRequest.TYPE,
                (payload, context) -> CombatService.tryDodge(context.player(), payload.direction()));
        PayloadTypeRegistry.serverboundPlay().register(ComboRequest.TYPE, ComboRequest.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ComboRequest.TYPE,
                (payload, context) -> CombatService.tryCombo(context.player(), payload.kind()));
    }
}
