package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.MkmMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** New independent player attachment. Does not alter the existing v4 XP/claim ledger. */
public final class CharacterOriginAttachments {
    public static final AttachmentType<CharacterOriginData> PROFILE = AttachmentRegistry.create(
            MkmMod.id("character_origin"),
            builder -> builder.initializer(CharacterOriginData::new)
                    .persistent(CharacterOriginData.CODEC)
                    .copyOnDeath()
                    .syncWith(CharacterOriginData.STREAM_CODEC,
                            AttachmentSyncPredicate.targetOnly()));

    private CharacterOriginAttachments() {}
    public static void initialize() { PROFILE.identifier(); }
}
