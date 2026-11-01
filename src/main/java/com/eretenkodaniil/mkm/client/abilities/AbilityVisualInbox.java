package com.eretenkodaniil.mkm.client.abilities;

import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEvent;
import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualLease;
import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualReceiptGate;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

/**
 * Client-only ingress to MNAE: confirmed semantic event and cancellation.
 * No animation player, pose sampler, resource state or gameplay authority.
 */
public final class AbilityVisualInbox {
    private static final AbilityVisualReceiptGate GATE = new AbilityVisualReceiptGate();
    private static Consumer<AbilityVisualEvent> mnaeListener;
    private static Runnable mnaeCancel;
    private static AbilityVisualEvent latest;
    private static long acceptedAtClientTick = -1L;
    private static Object lastPlayerInstance;
    private static Object lastLevelInstance;

    private AbilityVisualInbox() {}

    public static void installMnaeListener(Consumer<AbilityVisualEvent> listener) {
        installMnaeListener(listener, null);
    }

    /**
     * MNAE receives APPLIED for an authored one-shot and a cancel callback when
     * the local scope changes, another presentation supersedes it, or it expires.
     * The renderer owns its actual fade and blending.
     */
    public static void installMnaeListener(
            Consumer<AbilityVisualEvent> listener, Runnable onStop) {
        if (latest != null && mnaeCancel != null) mnaeCancel.run();
        mnaeListener = listener;
        mnaeCancel = onStop;
        latest = null;
        acceptedAtClientTick = -1L;
    }

    public static AbilityVisualEvent latestConfirmed() {
        return latest;
    }

    private static void clearPresentation() {
        if (latest != null && mnaeCancel != null) mnaeCancel.run();
        latest = null;
        acceptedAtClientTick = -1L;
    }

    public static void reset() {
        clearPresentation();
        GATE.reset();
        lastPlayerInstance = null;
        lastLevelInstance = null;
    }

    /** No gameplay timeline: only cancels a cosmetic lease on scope/TTL changes. */
    public static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (lastPlayerInstance != null || lastLevelInstance != null) reset();
            return;
        }
        if ((lastPlayerInstance != null && lastPlayerInstance != client.player)
                || (lastLevelInstance != null && lastLevelInstance != client.level)) {
            reset();
        }
        if (latest != null && acceptedAtClientTick >= 0) {
            if (AbilityVisualLease.expired(
                    acceptedAtClientTick, client.level.getGameTime(), latest.visualDurationTicks())) {
                // Sequence gate is intentionally retained: retransmissions cannot
                // restart a finished visual within the same connection/scope.
                clearPresentation();
            }
        }
    }

    /** Must run on the client game thread, never the networking thread. */
    public static void onServerEvent(Minecraft client, AbilityVisualEvent event) {
        if (client.level == null || client.player == null) return;
        if (lastPlayerInstance != client.player || lastLevelInstance != client.level) {
            reset();
            lastPlayerInstance = client.player;
            lastLevelInstance = client.level;
        }
        String dimensionId = client.level.dimension().identifier().toString();
        if (!GATE.accept(event, client.player.getId(), dimensionId)) return;
        clearPresentation(); // one visual action at a time; MNAE owns any crossfade
        latest = event;
        acceptedAtClientTick = client.level.getGameTime();
        if (mnaeListener != null) {
            mnaeListener.accept(event);
        } else {
            RenewalSparkles.show(client);
        }
    }
}
