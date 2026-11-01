package com.eretenkodaniil.mkm.abilities;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManaLedgerCodecTest {
    @Test void versionedResourceStateRoundTripsWithoutLosingCooldowns() {
        ManaLedger saved = new ManaLedger(17.25D, 1_234L, 1_250L,
                Map.of("mkm:renewal", 1_290L, "mkm:second_spell", 1_260L));
        var encoded = ManaLedgerCodec.PERSISTENT_CODEC
                .encodeStart(JsonOps.INSTANCE, saved).result().orElseThrow();
        assertEquals(ManaLedgerCodec.CURRENT_SCHEMA_VERSION,
                encoded.getAsJsonObject().get("schema_version").getAsInt());

        ManaLedger restored = ManaLedgerCodec.PERSISTENT_CODEC
                .parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        assertEquals(saved, restored);
    }

    @Test void preReviewUnversionedSavesDecodeAndUpgrade() {
        JsonObject old = new JsonObject();
        old.addProperty("mana", 21.0D);
        old.addProperty("last_tick", 500L);
        old.addProperty("regen_ready_tick", 550L);
        ManaLedger migrated = ManaLedgerCodec.PERSISTENT_CODEC
                .parse(JsonOps.INSTANCE, old).result().orElseThrow();
        assertEquals(21.0D, migrated.mana(), 1e-9);
        assertEquals(500L, migrated.lastUpdateTick());
        assertEquals(550L, migrated.regenReadyTick());
        assertTrue(migrated.cooldowns().isEmpty());

        var upgraded = ManaLedgerCodec.PERSISTENT_CODEC
                .encodeStart(JsonOps.INSTANCE, migrated).result().orElseThrow();
        assertEquals(1, upgraded.getAsJsonObject().get("schema_version").getAsInt());
    }

    @Test void futureSchemaFailsClosedInsteadOfLosingFutureFields() {
        JsonObject future = new JsonObject();
        future.addProperty("schema_version", 999);
        future.addProperty("mana", 21.0D);
        future.addProperty("last_tick", 500L);
        future.addProperty("regen_ready_tick", 550L);
        try {
            var result = ManaLedgerCodec.PERSISTENT_CODEC.parse(JsonOps.INSTANCE, future);
            assertTrue(result.error().isPresent(), "Unknown schema must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("schema"));
        }
    }
}
