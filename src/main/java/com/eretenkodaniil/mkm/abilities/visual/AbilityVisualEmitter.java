package com.eretenkodaniil.mkm.abilities.visual;

import com.eretenkodaniil.mkm.abilities.AbilityCatalog;
import com.eretenkodaniil.mkm.abilities.net.AbilityVisualPacket;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The server broadcasts ONLY successful-effect particles, so other tracked
 * players see the same single cast as the owner. Owner gets a dimension-bound
 * semantic MNAE event, but the client does not spawn a duplicate burst.
 */
public final class AbilityVisualEmitter {
    private static final Map<ServerPlayer, Integer> SEQUENCES = new WeakHashMap<>();
    private AbilityVisualEmitter() {}

    public static void onRenewalApplied(ServerPlayer player, float oldHealth) {
        if (!AbilityVisualPolicy.renewalApplied(true, player.isAlive(),
                oldHealth, player.getHealth())) return;
        broadcast(player, AbilityCatalog.RENEWAL.id(),
                AbilityVisualPolicy.RENEWAL_ACTION_ID,
                AbilityVisualPolicy.RENEWAL_VISUAL_TICKS);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + 1.1, player.getZ(),
                10, .36, .65, .36, .03);
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.35, player.getZ(),
                7, .24, .35, .24, .018);
    }

    public static void onWardApplied(ServerPlayer player, boolean changed) {
        if (!AbilityVisualPolicy.wardApplied(true, player.isAlive(), changed)) return;
        broadcast(player, "mkm:arcane_barrier",
                AbilityVisualPolicy.WARD_ACTION_ID,
                AbilityVisualPolicy.WARD_VISUAL_TICKS);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.1, player.getZ(),
                22, .65, .85, .65, .04);
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0, player.getZ(),
                10, .65, .60, .65, .022);
    }

    private static void broadcast(ServerPlayer player, String id, String action, int duration) {
        if (!ServerPlayNetworking.canSend(player, AbilityVisualPacket.TYPE)) return;
        int previous = SEQUENCES.getOrDefault(player, 0);
        if (previous == Integer.MAX_VALUE) return;
        int seq = previous + 1;
        SEQUENCES.put(player, seq);
        ServerPlayNetworking.send(player, new AbilityVisualPacket(
                new AbilityVisualEvent(id, action, player.getId(), seq,
                        AbilityVisualEvent.Hand.MAIN_HAND,
                        AbilityVisualEvent.Phase.APPLIED,
                        player.level().getGameTime(), duration,
                        player.level().dimension().identifier().toString())));
    }
}
