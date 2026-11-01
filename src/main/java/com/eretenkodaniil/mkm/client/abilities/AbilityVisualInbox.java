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
    }

    /** Must run on the client render/game thread, not the networking thread. */
    public static void onServerEvent(Minecraft client, AbilityVisualEvent event) {
        if (client.level == null || client.player == null
                || !GATE.accept(event, client.player.getId())) return;
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
