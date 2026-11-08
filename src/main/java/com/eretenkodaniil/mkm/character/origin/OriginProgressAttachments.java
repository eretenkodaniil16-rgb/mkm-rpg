package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.MkmMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Optional new v1 player attachment. Separate from existing v4 character and origin v1. */
public final class OriginProgressAttachments {
    public static final AttachmentType<OriginProgressData> PROGRESS = AttachmentRegistry.create(
            MkmMod.id("origin_progress"), b -> b.initializer(OriginProgressData::new)
                    .persistent(OriginProgressData.CODEC).copyOnDeath()
                    .syncWith(OriginProgressData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));
    private OriginProgressAttachments() {}
    public static void initialize() { PROGRESS.identifier(); }
}
