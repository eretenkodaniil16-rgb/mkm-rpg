package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.client.animation.engine.ArticulatedClipV3;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.Objects;

/**
 * QA-v4-only opt-in client presentation. No server packets, gameplay handlers,
 * persistent attachment or CharacterData write. F8: C3 vs derived six-part
 * comparator; F9: experimental R07 17-bone renderer (requires F8).
 */
public final class ArticulatedMode {
    public enum Mode { C3_QA5, SIX_PART_DERIVED, R07_17 }
    private static boolean nativeEnabled;
    private static boolean articulatedEnabled;
    private static boolean layerReady;
    private static final String RESOURCE="assets/mkm/mnae/r07_mid_guard_cut_v3.json";
    private static String loadError="asset not loaded";
    private ArticulatedMode(){}
    private static final class Assets {
        private static final ArticulatedClipV3 CLIP=load();
    }
    private static ArticulatedClipV3 load() {
        try {
            ArticulatedClipV3 clip=ArticulatedClipV3.readResource(RESOURCE);
            loadError="";
            return clip;
        } catch(RuntimeException e) {
            loadError=e.getClass().getSimpleName()+": "+e.getMessage();
            return null;
        }
    }
    public static boolean nativeEnabled(){return nativeEnabled;}
    public static boolean articulatedEnabled(){return articulatedEnabled;}
    public static boolean flipNative(){return nativeEnabled=!nativeEnabled;}
    public static boolean flipArticulated(){return articulatedEnabled=!articulatedEnabled;}
    public static ArticulatedClipV3 clip(){return Assets.CLIP;}
    public static void markLayerReady(){layerReady=true;}
    public static boolean layerReady(){return layerReady;}
    public static String loadStatus(){return clip()==null?"R07 asset invalid: "+loadError:"R07 asset OK";}

    /** The only action eligible for ANY new cosmetic pose in this slice. */
    public static boolean eligible(AvatarRenderState state,LocalCombatAnimation.Frame frame) {
        Minecraft client=Minecraft.getInstance();
        if(client.player==null||client.level==null||state==null
                ||frame==null||frame.sampledClip()==null
                ||state.id!=client.player.getId()||client.player.isSpectator())return false;
        ActionTimeline timeline=frame.timeline();
        if(timeline==null||timeline.idle()
                ||timeline.kind()!=ActionTimeline.Kind.LIGHT_COMBO
                ||timeline.comboStep()!=1)return false;
        if(client.options.getCameraType().isFirstPerson()
                ||!client.player.getMainHandItem().is(ItemTags.SWORDS)
                ||!client.player.getOffhandItem().isEmpty())return false;
        for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
            if(!client.player.getItemBySlot(slot).isEmpty())return false;
        return true;
    }
    public static Mode mode(AvatarRenderState state,LocalCombatAnimation.Frame frame){
        if(!nativeEnabled||!eligible(state,frame)||clip()==null)return Mode.C3_QA5;
        if(articulatedEnabled)return layerReady?Mode.R07_17:Mode.C3_QA5;
        return Mode.SIX_PART_DERIVED;
    }
    public static boolean active(AvatarRenderState state,LocalCombatAnimation.Frame frame){
        return mode(state,frame)==Mode.R07_17;
    }
    public static String fallback(AvatarRenderState state,LocalCombatAnimation.Frame frame) {
        if(!nativeEnabled)return "F8: C3 QA5";
        if(clip()==null)return "R07_RESOURCE_MISSING";
        if(articulatedEnabled&&!layerReady)return "R07_RENDER_LAYER_MISSING";
        if(!eligible(state,frame))return "R07_CONTEXT_FALLBACK (item/offhand/armor/camera/timeline)";
        return articulatedEnabled?"R07_17_ACTIVE":"R07_SIX_DERIVED_ACTIVE";
    }
}
