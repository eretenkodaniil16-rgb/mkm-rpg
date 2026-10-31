package com.eretenkodaniil.mkm.character;

import com.eretenkodaniil.mkm.MkmMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Persisted + owner-synced RPG character data (Fabric 26.3). */
public final class CharacterAttachments {
    public static final AttachmentType<CharacterData> CHARACTER = AttachmentRegistry.create(
            MkmMod.id("character"),
            builder -> builder
                    .initializer(CharacterData::new)
                    .persistent(CharacterData.CODEC.codec())
                    .copyOnDeath()
                    .syncWith(CharacterData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

    private CharacterAttachments() {}

    public static void initialize() {
        // Force eager registration of the attachment type.
        CHARACTER.identifier();
    }
}
