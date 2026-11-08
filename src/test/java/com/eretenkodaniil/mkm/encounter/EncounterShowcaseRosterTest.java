package com.eretenkodaniil.mkm.encounter;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Saved legacy skeleton reservations remain valid when new content uses Spider. */
final class EncounterShowcaseRosterTest {
    @Test void spiderEliteRosterDecodesWithoutNewLedgerSchema() {
        var def = new EncounterDefinition("mkm:ford_ambush", "New night ambush",
                "hostile", "minecraft:overworld", List.of("minecraft:forest"),
                "night", 2, 100, List.of(
                    new EncounterDefinition.Spawn("minecraft:husk", "frontline", false),
                    new EncounterDefinition.Spawn("minecraft:husk", "frontline", false),
                    new EncounterDefinition.Spawn("minecraft:spider", "leader", true)));
        var json = EncounterDefinition.CODEC.encodeStart(JsonOps.INSTANCE, def).getOrThrow();
        assertEquals(def, EncounterDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        var members = List.of(
                new EncounterLedger.Member("raider-uuid-a","minecraft:husk","frontline",false,false),
                new EncounterLedger.Member("raider-uuid-b","minecraft:husk","frontline",false,false),
                new EncounterLedger.Member("stalker-uuid","minecraft:spider","leader",true,false));
        var reserved = new EncounterLedger().reserve("mkm:ford_ambush",
                new EncounterLedger.Entry(1,"owner",10,64,10,100,EncounterLedger.RESERVED,members));
        assertEquals(3,reserved.activeMobReservations());
        var restored = EncounterLedger.CODEC.parse(JsonOps.INSTANCE,
                EncounterLedger.CODEC.encodeStart(JsonOps.INSTANCE, reserved).getOrThrow()).getOrThrow();
        assertEquals(3,restored.activeMobReservations());
        var finalPhase = restored.markDeath("mkm:ford_ambush","raider-uuid-a")
                .markDeath("mkm:ford_ambush","raider-uuid-b")
                .markDeath("mkm:ford_ambush","stalker-uuid");
        assertEquals(EncounterLedger.DEFEATED, finalPhase.find("mkm:ford_ambush").phase());
        assertSame(finalPhase,finalPhase.markDeath("mkm:ford_ambush","stalker-uuid"));
    }
}
