package com.eretenkodaniil.mkm.character;

/**
 * Prototype progression curve for the Character Core vertical slice.
 *
 * <p>The curve is deliberately isolated from persistence. Tuning it does not require changing the
 * stored character schema. It is not a final balance commitment.</p>
 */
public final class ProgressionRules {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 100;
    private static final long XP_STEP = 100L;

    private ProgressionRules() {
    }

    /**
     * Total experience required to reach a level.
     * Level 1 = 0 XP, level 2 = 100 XP, level 3 = 300 XP, level 4 = 600 XP.
     */
    public static long experienceForLevel(int level) {
        int normalized = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        long completedLevels = normalized - 1L;
        return XP_STEP * completedLevels * (completedLevels + 1L) / 2L;
    }

    public static int levelForExperience(long experience) {
        long normalizedExperience = Math.max(0L, experience);
        int low = MIN_LEVEL;
        int high = MAX_LEVEL;

        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (experienceForLevel(mid) <= normalizedExperience) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    public static long experienceForNextLevel(long experience) {
        int level = levelForExperience(experience);
        return level >= MAX_LEVEL ? experienceForLevel(MAX_LEVEL) : experienceForLevel(level + 1);
    }

    public static boolean isMaxLevel(long experience) {
        return levelForExperience(experience) >= MAX_LEVEL;
    }
}
