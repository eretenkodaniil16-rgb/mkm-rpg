package com.eretenkodaniil.mkm.environment;

/**
 * Fail-closed survey for manual only, operator-chosen structure placement.
 * The check does NOT infer whether ground is natural or built by a player;
 * instead, it prohibits replacing ANY existing block in the target volume.
 * Ground below the volume is only used as support, never modified.
 */
public final class WatchtowerSiteRules {
    public static final int WIDTH = 23;
    public static final int HEIGHT = 14;
    public static final int DEPTH = 21; // 19 template + 2 for two-wide approach
    public static final int MAX_SUPPORT_DEPTH = 4;
    private static final int[][] SUPPORTS = {
            {4, 3}, {13, 3}, {4, 12}, {13, 12},
            {16, 10}, {21, 10}, {16, 16}, {21, 16},
            {14, 10}, {15, 11}, {16, 9},
            {8, 17}, {9, 17}, {8, 18}, {9, 18},
            {8, 19}, {9, 19}, {8, 20}, {9, 20}
    };

    private WatchtowerSiteRules() {}

    public interface Probe {
        boolean isAir(int x, int y, int z);
        boolean sturdyBelow(int x, int y, int z);
    }

    /** Every target voxel must be empty; each anchor must touch stable substrate within 4 blocks. */
    public static boolean canPlace(Probe probe) {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = 0; y < HEIGHT; y++) {
                    if (!probe.isAir(x, y, z)) return false;
                }
            }
        }
        for (int[] column : SUPPORTS) {
            boolean supported = false;
            for (int distance = 1; distance <= MAX_SUPPORT_DEPTH; distance++) {
                int below = -distance;
                if (probe.isAir(column[0], below, column[1])) continue;
                if (!probe.sturdyBelow(column[0], below, column[1])) return false;
                supported = true;
                break;
            }
            if (!supported) return false;
        }
        return true;
    }

    public static int[][] supportColumns() {
        return java.util.Arrays.stream(SUPPORTS).map(int[]::clone).toArray(int[][]::new);
    }
}
