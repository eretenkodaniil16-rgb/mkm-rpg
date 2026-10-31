package com.eretenkodaniil.mkm.combat;

/**
 * Stable one-byte protocol for directional dodge intent.
 * Calculated from the game's movement input rather than custom key binding state.
 */
public final class DodgeDirection {
    public static final byte FORWARD = 0;
    public static final byte BACKWARD = 1;
    public static final byte LEFT = 2;
    public static final byte RIGHT = 3;
    public static final byte FORWARD_LEFT = 4;
    public static final byte FORWARD_RIGHT = 5;
    public static final byte BACKWARD_LEFT = 6;
    public static final byte BACKWARD_RIGHT = 7;
    public static final byte LAST = BACKWARD_RIGHT;

    private DodgeDirection() {}

    public static byte fromMovement(
            boolean forward, boolean backward, boolean left, boolean right) {
        // Opposing keys cancel, like Minecraft's KeyboardInput.calculateImpulse.
        int longitudinal = Boolean.compare(forward, backward);
        int lateral = Boolean.compare(left, right);

        if (longitudinal > 0) {
            if (lateral > 0) return FORWARD_LEFT;
            if (lateral < 0) return FORWARD_RIGHT;
            return FORWARD;
        }
        if (longitudinal < 0) {
            if (lateral > 0) return BACKWARD_LEFT;
            if (lateral < 0) return BACKWARD_RIGHT;
            return BACKWARD;
        }
        if (lateral > 0) return LEFT;
        if (lateral < 0) return RIGHT;
        // G alone = forward dodge, preserving the previous control.
        return FORWARD;
    }

    public static String label(byte direction) {
        return switch (direction) {
            case FORWARD -> "Forward";
            case BACKWARD -> "Backward";
            case LEFT -> "Left";
            case RIGHT -> "Right";
            case FORWARD_LEFT -> "Forward-left";
            case FORWARD_RIGHT -> "Forward-right";
            case BACKWARD_LEFT -> "Backward-left";
            case BACKWARD_RIGHT -> "Backward-right";
            default -> "Unknown";
        };
    }
}
