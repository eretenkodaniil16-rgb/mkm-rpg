package com.eretenkodaniil.mkm.environment;

/** Contextual exploration clue determined solely by the saved server origin. */
public final class RuneLoreRules {
    public static final String HUMAN = "mkm:human";
    public static final String DWARF = "mkm:dwarf";
    public static final String WARRIOR = "mkm:warrior";
    public static final String MAGE = "mkm:mage";
    public static final long REWARD_XP = 25L;

    private RuneLoreRules() {}

    public record Discovery(String loreKey, String classKey, boolean nightVision) {
        public Discovery {
            if (loreKey == null || classKey == null)
                throw new IllegalArgumentException("Missing rune lore key");
        }
    }

    public static Discovery discover(String raceId, String classId) {
        if (!HUMAN.equals(raceId) && !DWARF.equals(raceId)) return null;
        if (!WARRIOR.equals(classId) && !MAGE.equals(classId)) return null;
        return new Discovery(DWARF.equals(raceId)
                ? "message.mkm.showcase.dwarf" : "message.mkm.showcase.human",
                MAGE.equals(classId) ? "message.mkm.showcase.mage"
                        : "message.mkm.showcase.warrior",
                MAGE.equals(classId));
    }
}
