package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.List;
import java.util.Map;

/**
 * Archetype identity and future unlock metadata. Only modifierDeltas currently affect play.
 * Listed startingAbilityIds are recommendations/contract IDs, NOT automatic executable grants:
 * Ability Core's current Renewal is available publicly and must remain so.
 */
public record ClassDefinition(
        String id, String titleKey, List<String> traitIds,
        List<String> startingAbilityIds, List<String> specializationIds,
        Map<PlayerModifierId, Double> modifierDeltas) {
    public ClassDefinition {
        OriginIds.require(id);
        if (titleKey == null || titleKey.isBlank()) throw new IllegalArgumentException("Class title required");
        traitIds = OriginIds.immutableIds(traitIds);
        startingAbilityIds = OriginIds.immutableIds(startingAbilityIds);
        specializationIds = OriginIds.immutableIds(specializationIds);
        modifierDeltas = OriginModifierRules.checked(modifierDeltas);
    }
}
