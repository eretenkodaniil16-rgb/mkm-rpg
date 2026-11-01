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
        int visualDurationTicks,
        String dimensionId) {

    public static final int MAX_VISUAL_TICKS = 100;
    public static final int MAX_DIMENSION_ID_BYTES = 128;

    public enum Hand { MAIN_HAND, OFF_HAND }
    public enum Phase { STARTED, APPLIED, INTERRUPTED, FINISHED }

    public AbilityVisualEvent {
        if (!AbilityIds.valid(abilityId) || !AbilityIds.valid(visualActionId)
                || actorEntityId <= 0 || sequence <= 0 || hand == null || phase == null
                || !validDimensionId(dimensionId)
                || serverTick < 0 || visualDurationTicks < 0
                || visualDurationTicks > MAX_VISUAL_TICKS) {
            throw new IllegalArgumentException("Invalid ability presentation event");
        }
    }

    public static boolean validDimensionId(String id) {
        if (id == null || id.length() > MAX_DIMENSION_ID_BYTES ||
                !id.matches("[a-z0-9_.-]{1,64}:[a-z0-9_./-]{1,63}")) return false;
        String path = id.substring(id.indexOf(':') + 1);
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) return false;
        }
        return true;
    }
}
