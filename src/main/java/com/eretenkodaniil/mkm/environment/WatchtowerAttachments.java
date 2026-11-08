package com.eretenkodaniil.mkm.environment;

import com.eretenkodaniil.mkm.MkmMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** ServerLevel-only persisted proof, not player-owned, and never sent to a client. */
public final class WatchtowerAttachments {
    public static final AttachmentType<WatchtowerSiteRegistry> SITES =
            AttachmentRegistry.create(MkmMod.id("watchtower_sites_v1"),
                    builder -> builder.initializer(WatchtowerSiteRegistry::new)
                            .persistent(WatchtowerSiteRegistry.CODEC));
    private WatchtowerAttachments() {}
    public static void initialize() { SITES.identifier(); }
}
