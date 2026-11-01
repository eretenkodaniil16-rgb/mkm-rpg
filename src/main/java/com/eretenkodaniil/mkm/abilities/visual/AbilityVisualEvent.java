package com.eretenkodaniil.mkm.abilities.visual;

import com.eretenkodaniil.mkm.abilities.AbilityIds;

/**
 * Server-originated, cosmetic-only event contract for MNAE.
 *
 * STARTED, APPLIED, INTERRUPTED and FINISHED describe the gameplay lifecycle,
 * not animation keyframes. Instant Renewal emits only APPLIED after real healing.
 * The client may animate APPLIED for visualDurationTicks independently of the
 * already completed server-side healing transaction.
 */
public record AbilityVisualEvent(
        String abilityId,
        String visualActionId,
        int actorEntityId,
        int sequence,
        Hand hand,
        Phase phase,
        long serverTick,
        int visualDurationTicks) {

    public static final int MAX_VISUAL_TICKS = 100;

    public enum Hand { MAIN_HAND, OFF_HAND }
    public enum Phase { STARTED, APPLIED, INTERRUPTED, FINISHED }

    public AbilityVisualEvent {
        if (!AbilityIds.valid(abilityId) || !AbilityIds.valid(visualActionId)
                || actorEntityId <= 0 || sequence <= 0 || hand == null || phase == null
                || serverTick < 0 || visualDurationTicks < 0
                || visualDurationTicks > MAX_VISUAL_TICKS) {
            throw new IllegalArgumentException("Invalid ability presentation event");
        }
    }
}
