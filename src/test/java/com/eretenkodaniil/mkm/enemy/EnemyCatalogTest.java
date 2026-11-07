package com.eretenkodaniil.mkm.enemy;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnemyCatalogTest {
    @Test void distinctStableDefinitionsAndTags() {
        assertEquals(2, EnemyCatalog.PROTOTYPES.size());
        Set<String> ids = new HashSet<>();
        Set<String> tags = new HashSet<>();
        for (EnemyDefinition def : EnemyCatalog.PROTOTYPES) {
            assertTrue(ids.add(def.id()));
            assertTrue(tags.add(def.persistentTag()));
            assertSame(def, EnemyCatalog.byTags(Set.of(def.persistentTag())));
            assertTrue(def.rigProfile().startsWith("mkm:rig/"));
        }
        assertNull(EnemyCatalog.byTags(Set.of("mkm.ford.raider.v1")));
        assertNull(EnemyCatalog.byTags(Set.of()));
        assertNotEquals(EnemyCatalog.ROAD_RAIDER.host(), EnemyCatalog.CAVE_STALKER.host());
    }

    @Test void boundedScalingAndPersistentLevel() {
        EnemyLevelRules.Stats base = EnemyLevelRules.forLevel(EnemyCatalog.ROAD_RAIDER, 1);
        EnemyLevelRules.Stats upper = EnemyLevelRules.forLevel(EnemyCatalog.ROAD_RAIDER, 999);
        assertEquals(1, base.level());
        assertEquals(30, upper.level());
        assertTrue(upper.health() > base.health());
        assertTrue(upper.attack() > base.attack());
        assertTrue(upper.xp() > base.xp());
        assertTrue(upper.speed() <= 0.42);
        assertEquals(7, EnemyLevelRules.readLevel(Set.of(EnemyLevelRules.levelTag(7))));
        assertEquals(1, EnemyLevelRules.readLevel(Set.of("some.unrelated.tag")));
        assertThrows(IllegalArgumentException.class, () -> EnemyLevelRules.levelTag(0));
        assertThrows(IllegalArgumentException.class, () -> EnemyLevelRules.levelTag(31));
    }

    @Test void rejectsUnstableIdentifiersAndInvalidStats() {
        EnemyDefinition sample = EnemyCatalog.ROAD_RAIDER;
        assertThrows(IllegalArgumentException.class, () -> new EnemyDefinition(
                "invalid", sample.persistentTag(), sample.host(), sample.family(), sample.role(),
                sample.dangerTier(), sample.baseHealth(), sample.baseAttack(), sample.baseSpeed(),
                sample.baseXp(), sample.rigProfile()));
        assertThrows(IllegalArgumentException.class, () -> new EnemyDefinition(
                sample.id(), sample.persistentTag(), sample.host(), sample.family(), sample.role(),
                sample.dangerTier(), Double.NaN, sample.baseAttack(), sample.baseSpeed(),
                sample.baseXp(), sample.rigProfile()));
    }
}
