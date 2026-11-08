package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.client.animation.engine.ArticulatedClipV3;
import com.eretenkodaniil.mkm.combat.ActionTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Purely cosmetic QA-v4 R07 toggle. F8 permits native six-part proxy,
 * F9 additionally permits 17-part R07 on an unarmored local sword player.
 * Idle uses authored Mid Guard so a clip never snaps from vanilla to R07.
 * NEVER writes character attachments or initiates any combat packet.
 */
public final class ArticulatedMode {
    private static boolean enabled;
    private static boolean layerReady;
    private static String loadFailure="";
    private static final String RESOURCE="assets/mkm/mnae/r07_mid_guard_cut_v3.json";
    private ArticulatedMode(){}

    private static final class Asset { static final ArticulatedClipV3 CLIP=load(); }
    private static ArticulatedClipV3 load(){
        try{return ArticulatedClipV3.readResource(RESOURCE);}
        catch(RuntimeException ex){loadFailure=ex.getClass().getSimpleName()+": "+ex.getMessage();return null;}
    }
    public static boolean enabled(){return enabled;}
    public static boolean toggle(){return enabled=!enabled;}
    public static boolean layerReady(){return layerReady;}
    public static void markLayerReady(){layerReady=true;}
    public static void reset(){enabled=false;}
    public static ArticulatedClipV3 clip(){return Asset.CLIP;}
    public static String assetStatus(){return clip()==null?"R07 ASSET INVALID: "+loadFailure:"R07 SOURCE v3 VERIFIED";}
    public static String itemId(ItemStack item){
        return item.isEmpty()?"minecraft:air":BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
    }
    public static MnaEWeaponVisualPolicy.Choice weapon(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null) return new MnaEWeaponVisualPolicy.Choice(
                MnaEWeaponVisualPolicy.Presentation.VANILLA,"NO_PLAYER");
        return MnaEWeaponVisualPolicy.select(itemId(mc.player.getMainHandItem()),
                                             itemId(mc.player.getOffhandItem()));
    }
    public static boolean actionSupported(LocalCombatAnimation.Frame frame){
        if(frame==null)return true; // idle authored Mid Guard
        ActionTimeline timeline=frame.timeline();
        return timeline!=null && timeline.kind()==ActionTimeline.Kind.LIGHT_COMBO
                && timeline.comboStep()==1 && frame.sampledClip()!=null;
    }
    public static boolean candidate(AvatarRenderState state,LocalCombatAnimation.Frame frame) {
        Minecraft mc=Minecraft.getInstance();
        if(!NativeAnimationToggle.enabled()||clip()==null||state==null
                ||mc.player==null||mc.level==null||state.id!=mc.player.getId()
                ||mc.options.getCameraType().isFirstPerson()
                ||weapon().presentation()!=MnaEWeaponVisualPolicy.Presentation.SWORD
                ||!actionSupported(frame)||mc.player.isSpectator()
                ||!mc.player.onGround()||mc.player.isSwimming()||mc.player.isFallFlying())
            return false;
        for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
            if(!mc.player.getItemBySlot(slot).isEmpty())return false;
        return true;
    }
    public static boolean active(AvatarRenderState state,LocalCombatAnimation.Frame frame){
        return enabled && layerReady && candidate(state,frame);
    }
    public static boolean sixPart(AvatarRenderState state,LocalCombatAnimation.Frame frame){
        return !enabled && candidate(state,frame);
    }
    public static double time(LocalCombatAnimation.Frame frame){
        return frame==null||frame.sampledClip()==null
                ? 0D : Math.clamp(frame.sampledClip().localTime(),0D,1D);
    }
    public static String reason(AvatarRenderState state,LocalCombatAnimation.Frame frame){
        Minecraft mc=Minecraft.getInstance();
        if(!NativeAnimationToggle.enabled())return "F8_DISABLED_C3";
        if(clip()==null)return "R07_RESOURCE_MISSING";
        if(mc.player==null||mc.level==null)return "NO_LOCAL_WORLD";
        if(state==null||state.id!=mc.player.getId())return "LOCAL_ONLY_SPECTATOR_PENDING";
        if(mc.options.getCameraType().isFirstPerson())return "FIRST_PERSON_VANILLA";
        var choice=weapon();
        if(choice.presentation()!=MnaEWeaponVisualPolicy.Presentation.SWORD)return choice.reason();
        if(!actionSupported(frame))return "COMBO_2_3_OR_DODGE_FALLBACK";
        for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})
            if(!mc.player.getItemBySlot(slot).isEmpty())return "ARMOR_VANILLA_FALLBACK";
        if(!mc.player.onGround())return "AIRBORNE_VANILLA_FALLBACK";
        if(enabled&&!layerReady)return "R07_RENDER_LAYER_NOT_REGISTERED";
        return enabled ? "R07_17_DRAW" : "MNAE_SIX_PROXY";
    }
}
