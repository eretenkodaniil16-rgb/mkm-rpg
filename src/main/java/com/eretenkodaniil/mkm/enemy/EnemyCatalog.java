package com.eretenkodaniil.mkm.enemy;

import java.util.List;
import java.util.Set;

/** Prototype definitions. World owns spawn ecology; this catalog does not spawn on its own. */
public final class EnemyCatalog {
    public static final EnemyDefinition ROAD_RAIDER = new EnemyDefinition(
            "mkm:enemy/road_raider", "mkm.enemy.road_raider.v1",
            EnemyDefinition.Host.HUSK, EnemyDefinition.Family.HUMANOID,
            EnemyDefinition.Role.SKIRMISHER, 1,
            24.0, 4.0, 0.245, 25L, "mkm:rig/humanoid_v1");

    public static final EnemyDefinition CAVE_STALKER = new EnemyDefinition(
            "mkm:enemy/cave_stalker", "mkm.enemy.cave_stalker.v1",
            EnemyDefinition.Host.SPIDER, EnemyDefinition.Family.ARACHNID,
            EnemyDefinition.Role.AMBUSHER, 2,
            20.0, 4.5, 0.315, 35L, "mkm:rig/creature/spider_v1");

    public static final List<EnemyDefinition> PROTOTYPES = List.of(ROAD_RAIDER, CAVE_STALKER);

    private EnemyCatalog() {}

    public static EnemyDefinition byTags(Set<String> tags) {
        if (tags.contains(ROAD_RAIDER.persistentTag())) return ROAD_RAIDER;
        if (tags.contains(CAVE_STALKER.persistentTag())) return CAVE_STALKER;
        return null;
    }

    public static EnemyDefinition byCommand(String name) {
        return switch (name) {
            case "raider" -> ROAD_RAIDER;
            case "stalker" -> CAVE_STALKER;
            default -> null;
        };
    }
}
