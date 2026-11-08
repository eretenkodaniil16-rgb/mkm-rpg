package com.eretenkodaniil.mkm.combat.fencing;

import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import java.util.Map;
import java.util.Optional;

/**
 * Historical inspirations, NOT a claim of a full historical fencing simulation.
 * Read-only stable ids/guard cues for MNAE. Gameplay deltas enter the existing
 * PlayerModifierCalculator only through CombatModifierContributions.
 *
 * All three are SWORD-only. No new weapon family, hitbox, reach or hit event.
 */
public enum FencingSchool {
    NONE("none", "mkm:guard/neutral", Map.of()),
    LIECHTENAUER("liechtenauer", "mkm:guard/vom_tag",
            Map.of(PlayerModifierId.MELEE_DAMAGE, .04D,
                    PlayerModifierId.STAGGER_POWER, .15D,
                    PlayerModifierId.STAMINA_COST, .12D)),
    FIORE("fiore", "mkm:guard/porta_di_ferro",
            Map.of(PlayerModifierId.BLOCK_STABILITY, .20D,
                    PlayerModifierId.STAGGER_RESISTANCE, .10D,
                    PlayerModifierId.MELEE_DAMAGE, -.06D)),
    DESTREZA("destreza", "mkm:guard/angulo_recto",
            Map.of(PlayerModifierId.ARMOR_PENETRATION, .06D,
                    PlayerModifierId.DODGE_COST, -.10D,
                    PlayerModifierId.STAGGER_POWER, -.10D));

    private final String id;
    private final String visualGuardCue;
    private final Map<PlayerModifierId, Double> additiveDeltas;

    FencingSchool(String id, String visualGuardCue, Map<PlayerModifierId, Double> additiveDeltas) {
        this.id = id;
        this.visualGuardCue = visualGuardCue;
        this.additiveDeltas = Map.copyOf(additiveDeltas);
    }

    public String id() {
        return id;
    }

    /** Semantic cue only: MNAE owns the animation, authored guard and grip. */
    public String visualGuardCue() {
        return visualGuardCue;
    }

    public Map<PlayerModifierId, Double> additiveDeltas() {
        return additiveDeltas;
    }


    public static Optional<FencingSchool> fromId(String id) {
        for (FencingSchool school : values()) {
            if (school.id.equals(id)) return Optional.of(school);
        }
        return Optional.empty();
    }
}
