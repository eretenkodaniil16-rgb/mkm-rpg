package com.eretenkodaniil.mkm.abilities.visual;

import com.eretenkodaniil.mkm.abilities.AbilityCatalog;
import com.eretenkodaniil.mkm.abilities.net.AbilityVisualPacket;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Called only after server-validated healing succeeds. Session-local cosmetic
 * sequencing is independent of persistent mana and cooldown state.
 * Do not expose this as a client-triggerable request or damage application API.
 */
public final class AbilityVisualEmitter {
    private static final Map<ServerPlayer, Integer> SEQUENCES = new WeakHashMap<>();

    private AbilityVisualEmitter() {}

    public static void onRenewalApplied(ServerPlayer player, float beforeHealth) {
        if (!AbilityVisualPolicy.renewalApplied(true, player.isAlive(),
                beforeHealth, player.getHealth())) return;
        if (!ServerPlayNetworking.canSend(player, AbilityVisualPacket.TYPE)) return;

        int sequence = SEQUENCES.getOrDefault(player, 0) + 1;
        if (sequence <= 0) sequence = 1; // avoid wraparound on extremely long sessions
        SEQUENCES.put(player, sequence);
        ServerPlayNetworking.send(player, new AbilityVisualPacket(new AbilityVisualEvent(
                AbilityCatalog.RENEWAL.id(), AbilityVisualPolicy.RENEWAL_ACTION_ID,
                player.getId(), sequence, AbilityVisualEvent.Hand.MAIN_HAND,
                AbilityVisualEvent.Phase.APPLIED, player.level().getGameTime(),
                AbilityVisualPolicy.RENEWAL_VISUAL_TICKS)));
    }
}
