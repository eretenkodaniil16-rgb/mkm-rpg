package com.eretenkodaniil.mkm.client.animation.engine;

import java.util.Map;
import java.util.Objects;

/**
 * Mathematical world-space hand/grip socket contract in ModelPart PIXELS.
 * Not yet connected to Minecraft's held-item renderer. Pure golden fixture only.
 */
public final class GripSpaceMath {
    private GripSpaceMath() {}
    public static Transform worldGrip(Skeleton rig, Map<String,Transform> boneOffsets,
                                      String handBone, Transform handSocket,
                                      Transform heldGrip, Transform avatarWorld) {
        Objects.requireNonNull(rig); Objects.requireNonNull(boneOffsets);
        Objects.requireNonNull(handSocket); Objects.requireNonNull(heldGrip);
        Objects.requireNonNull(avatarWorld);
        return avatarWorld.compose(rig.world(handBone,boneOffsets))
                .compose(handSocket).compose(heldGrip);
    }
}
