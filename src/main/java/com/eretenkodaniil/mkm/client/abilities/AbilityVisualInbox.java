package com.eretenkodaniil.mkm.client.abilities;

import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEvent;
import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualReceiptGate;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

/**
 * One client-only MNAE ingress boundary. Never sends packets or mutates health.
 * MNAE installs a listener and owns authored clips, hand poses and first-person.
 */
public final class AbilityVisualInbox {
    private static final AbilityVisualReceiptGate GATE = new AbilityVisualReceiptGate();
    private static Consumer<AbilityVisualEvent> mnaeListener;
    private static AbilityVisualEvent latest;
    private static Object lastPlayerInstance;
    private static Object lastLevelInstance;

    private AbilityVisualInbox() {}

    public static void installMnaeListener(Consumer<AbilityVisualEvent> listener) {
        mnaeListener = listener;
    }

    public static AbilityVisualEvent latestConfirmed() {
        return latest;
    }

    public static void reset() {
        GATE.reset();
        latest = null;
        lastPlayerInstance = null;
        lastLevelInstance = null;
    }

    /**
     * Prevent a previously confirmed pose from surviving respawn or a level
     * replacement before the next incoming event. No gameplay-tick state machine.
     */
    public static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (lastPlayerInstance != null || lastLevelInstance != null) reset();
            return;
        }
        if ((lastPlayerInstance != null && lastPlayerInstance != client.player)
                || (lastLevelInstance != null && lastLevelInstance != client.level)) {
            reset();
        }
    }

    /** Must run on the client render/game thread, not the networking thread. */
    public static void onServerEvent(Minecraft client, AbilityVisualEvent event) {
        if (client.level == null || client.player == null) return;
        // Respawn/dimension travel may replace the local player or level while
        // retaining a connection. Treat them as a new visual sequence session.
        if (lastPlayerInstance != client.player || lastLevelInstance != client.level) {
            GATE.reset();
            latest = null;
            lastPlayerInstance = client.player;
            lastLevelInstance = client.level;
        }
        if (!GATE.accept(event, client.player.getId())) return;
        latest = event;
        if (mnaeListener != null) {
            // MNAE owns actual pose and duration. Event is cosmetic evidence only.
            mnaeListener.accept(event);
        } else {
            // Lightweight fallback so successful Renewal remains visible before
            // MNAE-3 lands. No custom rig, timeline or gameplay effects.
            RenewalSparkles.show(client);
        }
    }
}
