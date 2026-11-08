package com.eretenkodaniil.mkm.showcase;

import com.eretenkodaniil.mkm.character.origin.CharacterOriginData;
import com.eretenkodaniil.mkm.encounter.EncounterLedger;

/** Pure event contract. Only the actual server performs mutations. */
public final class ShowcaseRules {
    private ShowcaseRules() {}
    public static boolean mayStudy(int perception, CharacterOriginData origin) {
        return perception >= 12 || origin != null && origin.selected()
                && origin.classId().equals("mkm:mage");
    }
    public static String eventId(String owner) { return "mkm:ford_ambush/" + owner; }
    public static String rewardId(String approach) {
        return ShowcaseProgress.DEFEND.equals(approach) ? "mkm:showcase/tower/defend"
                : ShowcaseProgress.STUDY.equals(approach) ? "mkm:showcase/tower/study" : "";
    }
    public static int rewardXp(String approach) {
        return ShowcaseProgress.STUDY.equals(approach) ? 125 : 100;
    }
    public static boolean canResolve(ShowcaseProgress progress, EncounterLedger.Entry event,
                                     String owner) {
        return progress != null && progress.stage().equals(ShowcaseProgress.ACTIVE)
                && event != null && event.ownerId().equals(owner)
                && (event.phase().equals(EncounterLedger.DEFEATED)
                    || event.phase().equals(EncounterLedger.CLAIMED));
    }
}
