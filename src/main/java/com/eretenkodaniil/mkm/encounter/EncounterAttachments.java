package com.eretenkodaniil.mkm.encounter;

import com.eretenkodaniil.mkm.MkmMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** World-level persistent encounter journal, distinct from FordQuestState. */
public final class EncounterAttachments {
    public static final AttachmentType<EncounterLedger> ENCOUNTERS = AttachmentRegistry.create(
            MkmMod.id("encounter_director_v1"),
            builder -> builder.initializer(EncounterLedger::new).persistent(EncounterLedger.CODEC));

    private EncounterAttachments() {}
    public static void initialize() { ENCOUNTERS.identifier(); }
}
