package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginData;
import com.eretenkodaniil.mkm.character.origin.OriginCatalog;
import java.util.Set;

/**
 * Authorization evidence comes only from persisted server Character v4 and
 * Origin v1. Research license is an explicit trusted adapter input (currently
 * empty); never interpret the quest reward receipt ledger as spell licenses.
 */
public final class AbilityGrantPolicy {
    private AbilityGrantPolicy() {}

    public record Grants(String classId, int level, Set<String> researched) {
        public Grants {
            if (level < 1 || level > 100 || researched == null)
                throw new IllegalArgumentException("Invalid grants");
            researched = Set.copyOf(researched);
        }
    }

    public static Grants fromSaved(CharacterData character, CharacterOriginData origin) {
        return fromSaved(character, origin, Set.of());
    }

    /** Only the server may supply validated held-item/research license IDs. */
    public static Grants fromSaved(CharacterData character, CharacterOriginData origin,
            Set<String> serverVerifiedLicenses) {
        if (serverVerifiedLicenses == null) return null;
        if (character == null || character.schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION)
            return null;
        // Preserve QA5 Renewal for legacy/unselected v4 characters.
        if (origin == null || !origin.selected())
            return new Grants("", character.level(), Set.of());
        if (origin.schemaVersion() != CharacterOriginData.SCHEMA_VERSION
                || OriginCatalog.clazz(origin.classId()) == null
                || OriginCatalog.race(origin.raceId()) == null) return null;
        return new Grants(origin.classId(), character.level(), serverVerifiedLicenses);
    }

    public static boolean granted(Grants grants, String id) {
        if (grants == null || !AbilityIds.valid(id)
                || AbilityCatalog.get(id) == null) return false;
        if (AbilityCatalog.RENEWAL.id().equals(id)) return true; // existing player ability
        if ("mkm:mage_ward".equals(id))
            return OriginCatalog.MAGE.id().equals(grants.classId())
                    || grants.researched().contains(id);
        return grants.researched().contains(id);
    }

    public static boolean executable(Grants grants, String id) {
        var def = AbilityCatalog.get(id);
        return granted(grants, id) && SpellProgression.meetsLevel(id, grants.level())
                && def.kind() == AbilityDefinition.Kind.ACTIVE
                && ("mkm:renewal".equals(id) || "mkm:mage_ward".equals(id));
    }
}
