package com.eretenkodaniil.mkm.quests;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

final class FordDialogueSessionTest {
    private static final UUID ACTOR = UUID.fromString("a11a11a1-aaaa-4aaa-8aaa-000000000001");
    @Test void capabilityBindsToExactActorEntityStageAndFiniteTime() {
        var s = new FordDialogueSessions.Session(1234L, ACTOR, 87, (byte)0, 100L, 1300L);
        assertTrue(s.matches(1234L,ACTOR,87,(byte)0,100L));
        assertTrue(s.matches(1234L,ACTOR,87,(byte)0,1300L));
        assertFalse(s.matches(1235L,ACTOR,87,(byte)0,100L));
        assertFalse(s.matches(1234L,UUID.randomUUID(),87,(byte)0,100L));
        assertFalse(s.matches(1234L,ACTOR,88,(byte)0,100L));
        assertFalse(s.matches(1234L,ACTOR,87,(byte)1,100L));
        assertFalse(s.matches(1234L,ACTOR,87,(byte)0,99L));
        assertFalse(s.matches(1234L,ACTOR,87,(byte)0,1301L));
    }
    @Test void emptyTokenCannotAuthorizeMutation() {
        var s = new FordDialogueSessions.Session(0L, ACTOR, 1, (byte)0, 0L, 100L);
        assertFalse(s.matches(0L,ACTOR,1,(byte)0,1L));
    }
}
