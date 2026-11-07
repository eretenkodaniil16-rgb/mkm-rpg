package com.eretenkodaniil.mkm.encounter;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class EncounterDefinitionTest {
    private static final String JSON = """
            {"id":"mkm:ford_ambush","title":"Ночная засада",
             "category":"hostile","dimension":"minecraft:overworld",
             "allowed_biomes":["minecraft:plains","minecraft:forest"],
             "time":"night","danger":2,"reward_xp":100,
             "roster":[
                {"entity":"minecraft:husk","role":"frontline","elite":false},
                {"entity":"minecraft:husk","role":"frontline","elite":false},
                {"entity":"minecraft:skeleton","role":"leader","elite":true}
             ]}
            """;

    @Test void datapackCodecRoundtripAndNightForestFilter() {
        var parsed = EncounterDefinition.CODEC.parse(
                JsonOps.INSTANCE, JsonParser.parseString(JSON)).getOrThrow();
        assertEquals("mkm:ford_ambush",parsed.id());
        assertEquals(3,parsed.roster().size());
        assertTrue(parsed.accepts("minecraft:forest",13000));
        assertTrue(parsed.accepts("minecraft:plains",23000));
        assertFalse(parsed.accepts("minecraft:forest",6000));
        assertFalse(parsed.accepts("minecraft:desert",14000));
        var encoded = EncounterDefinition.CODEC.encodeStart(JsonOps.INSTANCE,parsed).getOrThrow();
        assertEquals(parsed,EncounterDefinition.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
    }

    @Test void refuseUnboundedGroupsUnknownMobAndInvalidRewards() {
        var good = EncounterDefinition.CODEC.parse(
                JsonOps.INSTANCE, JsonParser.parseString(JSON)).getOrThrow();
        assertThrows(IllegalArgumentException.class, () -> new EncounterDefinition(
                good.id(), good.title(), good.category(),good.dimension(),good.allowedBiomes(),
                good.timeWindow(),good.dangerTier(),0,good.roster()));
        assertThrows(IllegalArgumentException.class, () -> new EncounterDefinition(
                good.id(),good.title(),good.category(),good.dimension(),good.allowedBiomes(),
                good.timeWindow(),good.dangerTier(),100,
                List.of(new EncounterDefinition.Spawn("minecraft:wither","leader",true),
                        new EncounterDefinition.Spawn("minecraft:husk","frontline",false),
                        new EncounterDefinition.Spawn("minecraft:husk","frontline",false))));
    }
}
