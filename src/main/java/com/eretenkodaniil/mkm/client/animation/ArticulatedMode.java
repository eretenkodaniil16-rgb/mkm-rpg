package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.client.animation.engine.ArticulatedClipV3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;

/** Explicit opt-in MNAE 17-bone experiment; never part of world saves. */
public final class ArticulatedMode {
    private static boolean enabled;
    private static boolean layerReady;
    private static String loadFailure="";
    private static final String RESOURCE="assets/mkm/mnae/r07_mid_guard_cut_v3.json";
    private ArticulatedMode() {}
    private static final class Asset {
        static final ArticulatedClipV3 CLIP=load();
    }
    private static ArticulatedClipV3 load(){
        try{return ArticulatedClipV3.readResource(RESOURCE);}
        catch(RuntimeException ex){
            loadFailure=ex.getMessage()==null ? "invalid R07 export" : ex.getMessage();
            return null;
        }
    }
    public static boolean enabled(){return enabled;}
    /** Registered only after the 17-bone geometry is actually baked. */
    public static void markLayerReady(){layerReady=true;}
    public static boolean layerReady(){return layerReady;}
    public static boolean toggle(){return enabled=!enabled;}
    public static String loadStatus(){return Asset.CLIP==null?"R07 ABSENT: "+loadFailure:"R07 ASSET VERIFIED";}
    public static ArticulatedClipV3 clip(){return Asset.CLIP;}
    public static boolean active(AvatarRenderState state,LocalCombatAnimation.Frame frame){
        Minecraft client=Minecraft.getInstance();
        if(!enabled||!layerReady||!NativeAnimationToggle.enabled()||Asset.CLIP==null
                ||client.player==null||client.level==null||state==null
                ||state.id!=client.player.getId()||frame==null
                ||frame.sampledClip()==null
                ||client.options.getCameraType().isFirstPerson()
                ||frame.selection().renderer()!=WeaponPresentationResolver.Renderer.MNAE
                ||frame.selection().mainFamily()!=WeaponPresentationResolver.Family.SWORD
                ||frame.selection().offFamily()!=WeaponPresentationResolver.Family.EMPTY
                ||frame.timeline().comboStep()!=1)return false;
        // Experimental geometry has no mapped armor skin yet. Avoid 3D overlap.
        for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
            if(!client.player.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }
    public static String fallback(AvatarRenderState state,LocalCombatAnimation.Frame frame){
        if(!enabled)return "R07_DISABLED_F9";
        if(!layerReady)return "R07_LAYER_NOT_REGISTERED";
        if(!NativeAnimationToggle.enabled())return "R07_REQUIRES_MNAE_F8";
        if(Asset.CLIP==null)return "R07_ASSET_INVALID";
        if(frame==null||state==null||frame.sampledClip()==null)return "R07_NO_COMBO";
        if(frame.selection().mainFamily()!=WeaponPresentationResolver.Family.SWORD)
            return "R07_SWORD_ONLY";
        if(frame.selection().offFamily()!=WeaponPresentationResolver.Family.EMPTY)
            return "R07_OFFHAND_NOT_TESTED";
        return "R07_ARMOR_OR_PERSPECTIVE_NOT_TESTED";
    }
}
