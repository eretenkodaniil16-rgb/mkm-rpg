package com.eretenkodaniil.mkm.combat.net;

import com.eretenkodaniil.mkm.combat.CombatService;
import com.eretenkodaniil.mkm.combat.mode.CombatModeService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Serverbound actions receive validated intent only. */
public final class CombatNetworking {
    private CombatNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(CombatModeContact.TYPE, CombatModeContact.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CombatModeStatus.TYPE, CombatModeStatus.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CombatModeRequest.TYPE, CombatModeRequest.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CombatStrikeIntent.TYPE, CombatStrikeIntent.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CombatModeRequest.TYPE,
                (payload, context) -> CombatModeService.toggle(context.player(), payload.enabled()));
        ServerPlayNetworking.registerGlobalReceiver(CombatStrikeIntent.TYPE,
                (payload, context) -> CombatModeService.intent(
                        context.player(), payload.phase(), payload.sequence()));
        PayloadTypeRegistry.clientboundPlay().register(HitConfirm.TYPE, HitConfirm.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DodgeRequest.TYPE, DodgeRequest.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DodgeRequest.TYPE,
                (payload, context) -> CombatService.tryDodge(context.player(), payload.direction()));
        PayloadTypeRegistry.serverboundPlay().register(ComboRequest.TYPE, ComboRequest.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ComboRequest.TYPE,
                (payload, context) -> CombatService.tryCombo(context.player(), payload.kind()));
    }
}
