package com.eretenkodaniil.mkm.enemy;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class EnemySquadRulesTest {
    @Test void onlySameWorldEncounterOwnerCanAlertOrCover() {
        String world = EnemyFactory.ENCOUNTER_TAG;
        String ownerA = "mkm.encounter.owner.owner-a";
        String ownerB = "mkm.encounter.owner.owner-b";
        var a = Set.of(world, ownerA, EnemyFactory.GUARD_TAG);
        var sameA = Set.of(world, ownerA, EnemyCatalog.ROAD_RAIDER.persistentTag());
        var b = Set.of(world, ownerB);
        assertTrue(EnemySquadRules.sameSquad(a, sameA));
        assertTrue(EnemySquadRules.sameSquad(sameA, a));
        assertFalse(EnemySquadRules.sameSquad(a, b));
        assertFalse(EnemySquadRules.sameSquad(Set.of(world), Set.of(world)));
        assertFalse(EnemySquadRules.sameSquad(a, Set.of(EnemyFactory.GUARD_TAG)));
        assertFalse(EnemySquadRules.sameSquad(Set.of(), a));
        assertTrue(EnemySquadRules.sameSquad(Set.of(EnemyFactory.GUARD_TAG),
                Set.of(EnemyCatalog.ROAD_RAIDER.persistentTag())));
    }
}
