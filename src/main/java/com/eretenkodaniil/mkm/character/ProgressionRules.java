package com.eretenkodaniil.mkm.character;

/** Deterministic MKM experience and build-point rules; vanilla experience is independent. */
public final class ProgressionRules {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 100;
    public static final int POINTS_PER_LEVEL = 2;
    public static final int ALLOCATION_ATTRIBUTE_CAP = 40;
    public static final long XP_PER_STEP = 100L;

    private ProgressionRules() {}

    /** Cumulative XP: T(L) = 50 * (L - 1) * L; level 100 requires 495000 XP. */
    public static long experienceForLevel(int level) {
        long n = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level)) - 1L;
        return XP_PER_STEP * n * (n + 1L) / 2L;
    }

    public static int levelForExperience(long experience) {
        long xp = Math.max(0L, experience);
        int low = MIN_LEVEL;
        int high = MAX_LEVEL;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (experienceForLevel(mid) <= xp) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    /** Level-earned credits are monotone even if an admin later reduces the XP total. */
    public static int earnedPointsForLevel(int level) {
        return (Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level)) - 1) * POINTS_PER_LEVEL;
    }

    public static long experienceForNextLevel(long experience) {
        int level = levelForExperience(experience);
        return level >= MAX_LEVEL ? experienceForLevel(MAX_LEVEL) : experienceForLevel(level + 1);
    }

    public static boolean isMaxLevel(long experience) {
        return levelForExperience(experience) >= MAX_LEVEL;
    }
}
