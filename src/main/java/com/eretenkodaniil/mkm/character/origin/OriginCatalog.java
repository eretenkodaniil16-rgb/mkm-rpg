package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * First locked proof-of-concept catalog. A datapack-backed validated resolver can replace
 * the lookup layer later, but saved stable IDs and all pure rules must remain unchanged.
 */
public final class OriginCatalog {
    public static final RaceDefinition HUMAN = new RaceDefinition(
            "mkm:human", "screen.mkm.origin.human",
            java.util.List.of("mkm:adaptable"),
            Map.of(PlayerModifierId.STAMINA_REGENERATION, 0.20D,
                    PlayerModifierId.MANA_REGENERATION, 0.10D));
    public static final RaceDefinition DWARF = new RaceDefinition(
            "mkm:dwarf", "screen.mkm.origin.dwarf",
            java.util.List.of("mkm:stoneborn"),
            Map.of(PlayerModifierId.MAX_STAMINA, 4.0D,
                    PlayerModifierId.BLOCK_STABILITY, 0.08D));
    public static final ClassDefinition WARRIOR = new ClassDefinition(
            "mkm:warrior", "screen.mkm.origin.warrior",
            java.util.List.of("mkm:martial_training"),
            java.util.List.of("mkm:warrior_brace"), java.util.List.of("mkm:guardian", "mkm:duelist"),
            Map.of(PlayerModifierId.MELEE_DAMAGE, 0.035D,
                    PlayerModifierId.STAMINA_COST, -0.025D));
    public static final ClassDefinition MAGE = new ClassDefinition(
            "mkm:mage", "screen.mkm.origin.mage",
            java.util.List.of("mkm:arcane_studies"),
            java.util.List.of("mkm:renewal", "mkm:mage_ward"), java.util.List.of("mkm:arcanist", "mkm:warder"),
            Map.of(PlayerModifierId.MAX_MANA, 6.0D,
                    PlayerModifierId.HEALING_POWER, 0.035D));

    private static final Map<String, RaceDefinition> RACES = races();
    private static final Map<String, ClassDefinition> CLASSES = classes();

    private OriginCatalog() {}

    private static Map<String, RaceDefinition> races() {
        Map<String, RaceDefinition> all = new LinkedHashMap<>();
        all.put(HUMAN.id(), HUMAN);
        all.put(DWARF.id(), DWARF);
        return Map.copyOf(all);
    }

    private static Map<String, ClassDefinition> classes() {
        Map<String, ClassDefinition> all = new LinkedHashMap<>();
        all.put(WARRIOR.id(), WARRIOR);
        all.put(MAGE.id(), MAGE);
        return Map.copyOf(all);
    }

    public static RaceDefinition race(String id) {
        return RACES.get(id);
    }
    public static ClassDefinition clazz(String id) {
        return CLASSES.get(id);
    }
    public static Map<String, RaceDefinition> racesAll() { return RACES; }
    public static Map<String, ClassDefinition> classesAll() { return CLASSES; }
}
