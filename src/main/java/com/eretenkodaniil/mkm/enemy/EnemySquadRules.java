package com.eretenkodaniil.mkm.enemy;

import java.util.Set;

/**
 * Stable tags determine World-owned ally boundaries; no cross-encounter aggro.
 * Independent GM test prototypes can assist one another only by local proximity.
 */
public final class EnemySquadRules {
    private static final String WORLD_OWNER_PREFIX = "mkm.encounter.owner.";
    private EnemySquadRules() {}

    public static boolean sameSquad(Set<String> actorTags, Set<String> allyTags) {
        if (actorTags == null || allyTags == null) return false;
        boolean worldActor = actorTags.contains(EnemyFactory.ENCOUNTER_TAG);
        boolean worldAlly = allyTags.contains(EnemyFactory.ENCOUNTER_TAG);
        if (worldActor != worldAlly) return false;
        if (!worldActor) return true;
        for (String tag : actorTags) {
            if (tag.startsWith(WORLD_OWNER_PREFIX) && allyTags.contains(tag)) return true;
        }
        return false; // damaged/missing owner tag: fail closed
    }
}
