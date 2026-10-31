package com.eretenkodaniil.mkm.character.attribute;

/**
 * Candidate primary attribute vocabulary for the next Character Core iteration.
 *
 * <p>These identifiers are intentionally not persisted yet. The existing STR/DEX/VIT attachment
 * schema remains untouched until the candidate set and mapping are reviewed.</p>
 */
public enum PrimaryAttribute {
    STRENGTH("strength", "STR"),
    DEXTERITY("dexterity", "DEX"),
    VITALITY("vitality", "VIT"),
    ENDURANCE("endurance", "END"),
    INTELLIGENCE("intelligence", "INT"),
    WILLPOWER("willpower", "WIL"),
    PERCEPTION("perception", "PER");

    private final String id;
    private final String abbreviation;

    PrimaryAttribute(String id, String abbreviation) {
        this.id = id;
        this.abbreviation = abbreviation;
    }

    public String id() {
        return id;
    }

    public String abbreviation() {
        return abbreviation;
    }

    public String translationKey() {
        return "attribute.mkm." + id;
    }
}
