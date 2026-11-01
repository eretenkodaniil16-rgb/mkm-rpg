package com.eretenkodaniil.mkm.quests;

import com.eretenkodaniil.mkm.quests.presentation.NpcVisualSignals;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

final class FordDialogueContractTest {
    @Test void stageChoiceMatrixRejectsReplayAndUnauthorizedAction() {
        assertTrue(FordDialogueRules.permitted((byte)0,(byte)1));
        assertTrue(FordDialogueRules.permitted((byte)0,(byte)2));
        assertTrue(FordDialogueRules.permitted((byte)0,(byte)3));
        assertFalse(FordDialogueRules.permitted((byte)0,(byte)4));
        assertFalse(FordDialogueRules.permitted((byte)0,(byte)5));
        assertTrue(FordDialogueRules.permitted((byte)1,(byte)5));
        assertFalse(FordDialogueRules.permitted((byte)1,(byte)2));
        assertTrue(FordDialogueRules.permitted((byte)2,(byte)4));
        assertFalse(FordDialogueRules.permitted((byte)2,(byte)1));
        assertFalse(FordDialogueRules.permitted((byte)3,(byte)4));
        for (int choice=-128;choice<=127;choice++) {
            assertFalse(FordDialogueRules.permitted((byte)4,(byte)choice));
        }
    }

    @Test void semanticCuesAreExplicitAndCannotMutateAQuest() {
        assertEquals(8,NpcVisualSignals.Cue.values().length);
        assertEquals("mkm:npc/greet",NpcVisualSignals.Cue.GREET.id());
        assertEquals("mkm:npc/death",NpcVisualSignals.Cue.DEATH.id());
        assertTrue(NpcVisualSignals.Cue.DEATH.priority()>NpcVisualSignals.Cue.IDLE.priority());
        assertThrows(IllegalArgumentException.class,()->new NpcVisualSignals.Intent(
                "actor","mkm:unknown/rig",NpcVisualSignals.Cue.TALK,3L,1));
        var events=new ArrayList<NpcVisualSignals.Intent>();
        try {
            NpcVisualSignals.installObserver(events::add);
            NpcVisualSignals.publish(new NpcVisualSignals.Intent(
                    "actor-id",NpcVisualSignals.HUMANOID,NpcVisualSignals.Cue.GREET,12L,1));
            assertEquals(1,events.size());
            assertEquals("mkm:rig/humanoid_v1",events.getFirst().rigProfile());
            FordQuestState start=new FordQuestState();
            assertEquals(FordQuestState.NOT_STARTED,start.stage());
        } finally { NpcVisualSignals.installObserver(x -> {}); }
    }
}
