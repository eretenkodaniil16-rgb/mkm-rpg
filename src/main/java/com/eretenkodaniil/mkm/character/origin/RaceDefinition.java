package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.List;
import java.util.Map;

/** Immutable, declarative racial origin; no executable behavior or stat mutation. */
public record RaceDefinition(
        String id, String titleKey, List<String> traitIds,
        Map<PlayerModifierId, Double> modifierDeltas) {
    public RaceDefinition {
        OriginIds.require(id);
        if (titleKey == null || titleKey.isBlank()) throw new IllegalArgumentException("Race title required");
        traitIds = OriginIds.immutableIds(traitIds);
        modifierDeltas = OriginModifierRules.checked(modifierDeltas);
    }
}
