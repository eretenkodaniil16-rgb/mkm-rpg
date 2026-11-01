package com.eretenkodaniil.mkm.quests;

import com.eretenkodaniil.mkm.MkmMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/** Player-owned, persistent quest ledger; no client writes or duplicate XP ledger. */
public final class FordQuestAttachments {
    public static final AttachmentType<FordQuestState> FORD_QUEST = AttachmentRegistry.create(
            MkmMod.id("ford_quest"),
            builder -> builder
                    .initializer(FordQuestState::new)
                    .persistent(FordQuestState.CODEC)
                    .copyOnDeath());

    public static final AttachmentType<FordEncounterOutcomes> ENCOUNTER_OUTCOMES = AttachmentRegistry.create(
            MkmMod.id("ford_encounter_outcomes"),
            builder -> builder
                    .initializer(FordEncounterOutcomes::new)
                    .persistent(FordEncounterOutcomes.CODEC));

    private FordQuestAttachments() {}

    public static void initialize() {
        FORD_QUEST.identifier();
        ENCOUNTER_OUTCOMES.identifier();
    }
}
