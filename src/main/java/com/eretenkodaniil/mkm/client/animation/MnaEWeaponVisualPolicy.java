package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.equipment.EquipmentRules;

/**
 * Read-only Equipment-core classifier. Never changes attack legitimacy or
 * server damage. No generic non-empty-item -> sword classification.
 */
public final class MnaEWeaponVisualPolicy {
    public enum Presentation { SWORD, UNARMED, VANILLA }
    public record Choice(Presentation presentation,String reason){}
    private MnaEWeaponVisualPolicy(){}
    public static Choice select(String main, String off) {
        if(main==null||off==null)
            return new Choice(Presentation.VANILLA,"ITEM_ID_MISSING");
        if(!"minecraft:air".equals(off))
            return new Choice(Presentation.VANILLA,"OFFHAND_OCCUPIED_SAFE_FALLBACK");
        if("minecraft:air".equals(main))
            return new Choice(Presentation.UNARMED,"EMPTY_HAND");
        EquipmentRules.Profile profile=EquipmentRules.profile(main,EquipmentRules.Slot.MAIN_HAND);
        if(profile.family()==EquipmentRules.Family.SWORD)
            return new Choice(Presentation.SWORD,"SWORD_MAINHAND");
        return new Choice(Presentation.VANILLA,
            profile.family()==EquipmentRules.Family.NONE
                ? "UNSUPPORTED_TOOL_OR_ITEM":"UNSUPPORTED_"+profile.family().name());
    }
}
