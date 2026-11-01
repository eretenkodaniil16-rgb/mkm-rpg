package com.eretenkodaniil.mkm.combat;

/**
 * Deterministic phase machine for the first C2 actions.
 * Transition occurs only during a server tick, never from a client clock.
 */
public final class ActionTimelineRules {
    public static final int MAX_COMBO_STEP = 3;
    public static final long COMBO_WINDOW_TICKS = 16L;
    public static final long DODGE_ACTIVE_TICKS = 10L;
    private static final long ACTIVE_TICKS = 1L;

    private ActionTimelineRules() {}

    public static ActionTimeline beginCombo(ActionTimeline current, long now) {
        if (!current.idle()) return null;
        int step = now <= current.comboUntilTick() && current.comboStep() > 0
                && current.comboStep() < MAX_COMBO_STEP
                ? current.comboStep() + 1 : 1;
        return new ActionTimeline(ActionTimeline.Kind.LIGHT_COMBO, ActionTimeline.Phase.WINDUP,
                step, now + windupTicks(step), 0L);
    }

    public static ActionTimeline beginDodge(ActionTimeline current, long now) {
        return beginDodge(current, now, DodgeDirection.FORWARD);
    }

    public static ActionTimeline beginDodge(ActionTimeline current, long now, byte direction) {
        if (!current.idle() || direction < 0 || direction > DodgeDirection.LAST) return null;
        return new ActionTimeline(ActionTimeline.Kind.DODGE, ActionTimeline.Phase.DODGING,
                0, now + DODGE_ACTIVE_TICKS, 0L, direction);
    }

    public static ActionTimeline advance(ActionTimeline current, long now) {
        if (current.idle() || now < current.phaseUntilTick()) return current;
        return switch (current.phase()) {
            case WINDUP -> new ActionTimeline(ActionTimeline.Kind.LIGHT_COMBO,
                    ActionTimeline.Phase.ACTIVE, current.comboStep(),
                    now + ACTIVE_TICKS, 0L);
            case ACTIVE -> new ActionTimeline(ActionTimeline.Kind.LIGHT_COMBO,
                    ActionTimeline.Phase.RECOVERY, current.comboStep(),
                    now + recoveryTicks(current.comboStep()), 0L);
            case RECOVERY -> new ActionTimeline(ActionTimeline.Kind.NONE,
                    ActionTimeline.Phase.IDLE, current.comboStep(),
                    now, now + COMBO_WINDOW_TICKS);
            case DODGING -> new ActionTimeline();
            case IDLE -> current;
        };
    }

    public static long windupTicks(int step) {
        return switch (step) {
            case 1 -> 5L;
            case 2 -> 4L;
            case 3 -> 6L;
            default -> throw new IllegalArgumentException("Invalid combo stage");
        };
    }

    public static long recoveryTicks(int step) {
        return switch (step) {
            case 1 -> 8L;
            case 2 -> 7L;
            case 3 -> 11L;
            default -> throw new IllegalArgumentException("Invalid combo stage");
        };
    }
}
