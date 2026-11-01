package com.eretenkodaniil.mkm.abilities.visual;

/**
 * Single owner-only cosmetic stream in M3. Sequence numbers never authorize
 * effects; they only prevent duplicate/reordered visual playback.
 * Clear this state on client join/disconnect; server increments per session.
 */
public final class AbilityVisualReceiptGate {
    private int actorEntityId;
    private int lastSequence;

    public boolean accept(AbilityVisualEvent event, int localPlayerEntityId,
            String localDimensionId) {
        if (event == null || !event.dimensionId().equals(localDimensionId)
                || event.actorEntityId() != localPlayerEntityId
                || localPlayerEntityId <= 0 || !AbilityVisualPolicy.allowedForPlayback(event)) {
            return false;
        }
        if (actorEntityId != localPlayerEntityId) {
            actorEntityId = localPlayerEntityId;
            lastSequence = 0;
        }
        if (event.sequence() <= lastSequence) return false;
        lastSequence = event.sequence();
        return true;
    }

    public void reset() {
        actorEntityId = 0;
        lastSequence = 0;
    }
}
