package com.eretenkodaniil.mkm.encounter;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Distinct data-driven ecology cannot rewrite the original M1 Ford GM pilot. */
final class ShowcaseEncounterDefinitionTest {
    private static final String JSON = """
            {"id":"mkm:showcase_watchtower","title":"Стражи башни",
             "category":"hostile","dimension":"minecraft:overworld",
             "allowed_biomes":["minecraft:plains","minecraft:forest"],
             "time":"night","danger":2,"reward_xp":100,
             "roster":[
                {"entity":"minecraft:husk","role":"frontline","elite":false},
                {"entity":"minecraft:spider","role":"ambusher","elite":false},
                {"entity":"minecraft:skeleton","role":"leader","elite":true}
             ]}
            """;

    @Test void twoRealAiFamiliesOneEliteWithTimeAndBiomeGates() {
        var loaded=EncounterDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(JSON)).getOrThrow();
        assertEquals("mkm:showcase_watchtower",loaded.id());
        assertTrue(loaded.accepts("minecraft:forest",13000));
        assertFalse(loaded.accepts("minecraft:forest",6000));
        assertFalse(loaded.accepts("minecraft:desert",19000));
        assertTrue(loaded.roster().stream().anyMatch(
                mob->mob.entityId().equals("minecraft:spider")&&mob.role().equals("ambusher")));
        var encoded=EncounterDefinition.CODEC.encodeStart(JsonOps.INSTANCE,loaded).getOrThrow();
        assertEquals(loaded,EncounterDefinition.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
    }
    @Test void cannotReplaceShowcaseSpiderWithASecondRaiderOrSpoofElite() {
        var valid=EncounterDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(JSON)).getOrThrow();
        assertThrows(IllegalArgumentException.class,()->new EncounterDefinition(
                valid.id(),valid.title(),valid.category(),valid.dimension(),valid.allowedBiomes(),
                valid.timeWindow(),valid.dangerTier(),valid.rewardXp(),List.of(
                    new EncounterDefinition.Spawn("minecraft:husk","frontline",false),
                    new EncounterDefinition.Spawn("minecraft:husk","frontline",false),
                    new EncounterDefinition.Spawn("minecraft:skeleton","leader",true))));
        assertThrows(IllegalArgumentException.class,()->new EncounterDefinition(
                valid.id(),valid.title(),valid.category(),valid.dimension(),valid.allowedBiomes(),
                valid.timeWindow(),valid.dangerTier(),valid.rewardXp(),List.of(
                    new EncounterDefinition.Spawn("minecraft:husk","frontline",true),
                    new EncounterDefinition.Spawn("minecraft:spider","ambusher",false),
                    new EncounterDefinition.Spawn("minecraft:skeleton","leader",false))));
    }
}
