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

    private FordQuestAttachments() {}

    public static void initialize() {
        FORD_QUEST.identifier();
    }
}
