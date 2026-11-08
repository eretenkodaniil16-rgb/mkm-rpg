package com.eretenkodaniil.mkm.showcase;

import com.eretenkodaniil.mkm.MkmMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Independent v1 story flags preserve Ilra E2.1 and CharacterData v4 byte-for-byte. */
public final class ShowcaseAttachments {
    public static final AttachmentType<ShowcaseProgress> PROGRESS = AttachmentRegistry.create(
            MkmMod.id("showcase_01_progress"), builder -> builder
                    .initializer(ShowcaseProgress::new)
                    .persistent(ShowcaseProgress.CODEC).copyOnDeath());
    public static final AttachmentType<ShowcaseSite> SITE = AttachmentRegistry.create(
            MkmMod.id("showcase_01_site"), builder -> builder
                    .initializer(ShowcaseSite::new).persistent(ShowcaseSite.CODEC));
    private ShowcaseAttachments() {}
    public static void initialize() {
        PROGRESS.identifier();
        SITE.identifier();
    }
}
