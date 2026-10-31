package com.eretenkodaniil.mkm.combat;

/**
 * Server-side, deterministic horizontal dodge impulse derived from the player's
 * facing direction and the server-authoritative dodge-distance modifier.
 *
 * <p>The client sends only a directional intent (0–7, including diagonals).
 * No client-supplied velocity or displacement is trusted.</p>
 */
public record DodgeMotion(double x, double z) {
    // One-shot impulse rather than teleportation: vanilla movement/collisions
    // resolve the following ticks. C2 will replace this with animated dodge state.
    public static final double BASE_IMPULSE = 1.25D;
    public static final double MAX_IMPULSE = 2.50D;

    public static DodgeMotion fromFacing(
            double lookX, double lookZ, int direction, double distanceMultiplier) {
        if (direction < 0 || direction > DodgeDirection.LAST
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
        // Forward, backward, lateral, and diagonals are all relative
        // to the server's view vector. Diagonals are normalized so they
        // never move further than the cardinal directions.
        double along = switch (direction) {
            case DodgeDirection.FORWARD, DodgeDirection.FORWARD_LEFT, DodgeDirection.FORWARD_RIGHT -> 1D;
            case DodgeDirection.BACKWARD, DodgeDirection.BACKWARD_LEFT, DodgeDirection.BACKWARD_RIGHT -> -1D;
            default -> 0D;
        };
        double lateral = switch (direction) {
            case DodgeDirection.LEFT, DodgeDirection.FORWARD_LEFT, DodgeDirection.BACKWARD_LEFT -> 1D;
            case DodgeDirection.RIGHT, DodgeDirection.FORWARD_RIGHT, DodgeDirection.BACKWARD_RIGHT -> -1D;
            default -> 0D;
        };
        double axisX = along * forwardX + lateral * forwardZ;
        double axisZ = along * forwardZ - lateral * forwardX;
        double norm = Math.hypot(axisX, axisZ);
        axisX /= norm;
        axisZ /= norm;
        double strength = Math.min(MAX_IMPULSE, BASE_IMPULSE * distanceMultiplier);
        return new DodgeMotion(axisX * strength, axisZ * strength);
    }
}
