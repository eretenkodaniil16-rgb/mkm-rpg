package com.eretenkodaniil.mkm.client.animation;

/** Minecraft-free deterministic gate for the experimental R07 render owner. */
public final class R07ModeGate {
    public enum Choice { C3_FALLBACK, SIX_DERIVED, ARTICULATED_17 }
    public record Context(boolean f8Native,boolean f9Articulated,
            boolean assetValid,boolean layerAvailable,boolean localOwner,
            boolean thirdPerson,boolean firstSwordCombo,
            boolean mainSword,boolean offhandEmpty,boolean armorEmpty) {}
    private R07ModeGate(){}
    public static Choice select(Context c){
        if(c==null)throw new IllegalArgumentException("No render context");
        if(!c.f8Native()||!c.assetValid()||!c.localOwner()||!c.thirdPerson()
                ||!c.firstSwordCombo()||!c.mainSword()
                ||!c.offhandEmpty()||!c.armorEmpty())return Choice.C3_FALLBACK;
        if(!c.f9Articulated())return Choice.SIX_DERIVED;
        return c.layerAvailable()?Choice.ARTICULATED_17:Choice.C3_FALLBACK;
    }
}
