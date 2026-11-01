package com.eretenkodaniil.mkm.client.animation.engine;
/** Two embedded, verified action assets only. Class initialization is client-scoped. */
public final class NativeDemoLibrary {
    public static final String SWORD="mkm:mnae/sword_cut";
    public static final String JAB="mkm:mnae/straight_punch";
    private NativeDemoLibrary(){}
    private static final class Holder{
        static final NativeClip SWORD_CLIP=NativeClipLoader.read("assets/mkm/mnae/sword_cut.json");
        static final NativeClip JAB_CLIP=NativeClipLoader.read("assets/mkm/mnae/straight_punch.json");
    }
    public static NativeClip sword(){return Holder.SWORD_CLIP;}
    public static NativeClip jab(){return Holder.JAB_CLIP;}
}
