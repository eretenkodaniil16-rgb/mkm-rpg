package com.eretenkodaniil.mkm.character.modifier;

import com.eretenkodaniil.mkm.character.CharacterData;

public final class AttributeScaling {
    private static final double SOFT_CAP_SPAN = 30.0D;

    private AttributeScaling() {
    }

    public static double effectiveDelta(int rawAttributeValue) {
        int clamped = Math.max(
                CharacterData.MIN_ATTRIBUTE,
                Math.min(CharacterData.MAX_ATTRIBUTE, rawAttributeValue));
        int delta = clamped - CharacterData.DEFAULT_ATTRIBUTE;
        if (delta == 0) {
            return 0.0D;
        }

        double magnitude = Math.abs((double) delta);
        double effectiveMagnitude = SOFT_CAP_SPAN * magnitude / (SOFT_CAP_SPAN + magnitude);
        return Math.copySign(effectiveMagnitude, delta);
    }
}
