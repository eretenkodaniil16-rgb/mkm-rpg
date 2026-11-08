package com.eretenkodaniil.mkm.character.origin;

import static org.junit.jupiter.api.Assertions.*;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Compare against the exact v3 field layout from 0.0.5-fabric.4 synthetic fixtures.
 * Origin M5 is OPTIONAL; it must never require rewriting established character credits.
 */
final class OriginM5LegacyUpgradeTest {
    @Test void oldV3DataAndBuildCountersSurviveWhileNewOriginAndMentorRemainUnset() throws Exception {
        for (String variant : new String[]{"allocated","max_level","legacy_admin"}) {
            String name = "/migration/character_v3_0_0_5_fabric_4_" + variant + ".json";
            try (var stream = getClass().getResourceAsStream(name)) {
                assertNotNull(stream, name);
                var source = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                CharacterData after = CharacterData.CODEC.codec().parse(JsonOps.INSTANCE, source)
                        .result().orElseThrow();
                assertEquals(4, after.schemaVersion());
                assertEquals(source.get("experience").getAsLong(), after.experience());
                assertEquals(source.get("strength").getAsInt(), after.strength());
                assertEquals(source.get("earned_points").getAsInt(), after.earnedPoints());
                assertEquals(7, after.allocatedPoints().size());
                for (int i = 0; i < 7; i++)
                    assertEquals(source.getAsJsonArray("allocated_points").get(i).getAsInt(),
                            after.allocatedPoints().get(i));
                var persisted = CharacterData.CODEC.codec().parse(JsonOps.INSTANCE,
                        CharacterData.CODEC.codec().encodeStart(JsonOps.INSTANCE, after)
                                .result().orElseThrow()).result().orElseThrow();
                assertEquals(after, persisted);
                assertFalse(new CharacterOriginData().selected());
                assertTrue(new OriginProgressData().unlockedNodes().isEmpty());
                CharacterOriginData selected = CharacterOriginRules.select(
                        new CharacterOriginData(), "mkm:human", "mkm:mage", 0).data();
                assertEquals(source.get("experience").getAsLong(), after.experience());
                assertTrue(selected.selected());
            }
        }
    }
}
