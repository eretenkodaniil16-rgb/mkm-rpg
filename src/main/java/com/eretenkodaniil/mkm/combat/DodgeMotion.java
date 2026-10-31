package com.eretenkodaniil.mkm.combat;

/**
 * Server-side, deterministic horizontal dodge impulse derived from the player's
 * facing direction and the server-authoritative dodge-distance modifier.
 *
 * <p>The client sends only a directional intent (0 forward, 1 backwards, 2 left,
 * 3 right). No client-supplied velocity or displacement is trusted.</p>
 */
public record DodgeMotion(double x, double z) {
    // One-shot impulse rather than teleportation: vanilla movement/collisions
    // resolve the following ticks. C2 will replace this with animated dodge state.
    public static final double BASE_IMPULSE = 1.25D;
    public static final double MAX_IMPULSE = 2.50D;

    public static DodgeMotion fromFacing(
            double lookX, double lookZ, int direction, double distanceMultiplier) {
        if (direction < 0 || direction > 3
                || !Double.isFinite(lookX) || !Double.isFinite(lookZ)
                || !Double.isFinite(distanceMultiplier) || distanceMultiplier <= 0D) {
            return null;
        }

        double length = Math.hypot(lookX, lookZ);
        if (length < 0.0001D) {
            return null;
        }
        double forwardX = lookX / length;
        double forwardZ = lookZ / length;

        // Local right direction is (-forward.z, forward.x).
        double axisX = switch (direction) {
            case 0 -> forwardX;
            case 1 -> -forwardX;
            case 2 -> forwardZ;
            case 3 -> -forwardZ;
            default -> throw new IllegalArgumentException("Invalid dodge direction");
        };
        double axisZ = switch (direction) {
            case 0 -> forwardZ;
            case 1 -> -forwardZ;
            case 2 -> -forwardX;
            case 3 -> forwardX;
            default -> throw new IllegalArgumentException("Invalid dodge direction");
        };
        double strength = Math.min(MAX_IMPULSE, BASE_IMPULSE * distanceMultiplier);
        return new DodgeMotion(axisX * strength, axisZ * strength);
    }
}
