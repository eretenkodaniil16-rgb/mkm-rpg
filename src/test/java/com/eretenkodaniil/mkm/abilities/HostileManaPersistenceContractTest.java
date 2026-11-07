package com.eretenkodaniil.mkm.abilities;

import com.mojang.serialization.JsonOps;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression for exhausted tagged mobs: every eligible server scan must
 * persist regenerated ManaLedger even if Renewal cannot yet be afforded.
 */
class HostileManaPersistenceContractTest {
    private static final RenewalParameters MOB_TERMS = new RenewalParameters(
            100D, 1D, 40L, 20D, 60L, 6F);

    @Test void repeatedFailedAttemptsEventuallyRegeneratePastCatchupCap() {
        ManaLedger saved = new ManaLedger(0D, 100L, 100L, Map.of());
        assertNull(RenewalCastPlan.spendIfAllowed(
                true,true,true,saved,400L,MOB_TERMS));
        // Without persisting regen at every scan, each retry is capped at 10
        // mana and the mob can never again afford a 20-mana cast.
        for (long t = 400; t <= 620; t += 20L) {
            saved = AbilityRules.regenerate(saved, t, 100D, 1D);
        }
        assertTrue(saved.mana() >= 20D);
        ManaLedger paid = RenewalCastPlan.spendIfAllowed(
                true,true,true,saved,620L,MOB_TERMS);
        assertNotNull(paid);
        assertTrue(paid.mana() >= 0);
        assertEquals(680L, paid.readyTick(AbilityCatalog.RENEWAL.id()));
    }

    @Test void hostileUsesTheSameVersionedManaCodecWithoutCharacterData() {
        ManaLedger hostile = new ManaLedger(46.5D, 140L, 165L,
                Map.of("mkm:renewal", 200L));
        var encoded = ManaLedgerCodec.PERSISTENT_CODEC.encodeStart(
                JsonOps.INSTANCE, hostile).result().orElseThrow();
        ManaLedger decoded = ManaLedgerCodec.PERSISTENT_CODEC.parse(
                JsonOps.INSTANCE, encoded).result().orElseThrow();
        assertEquals(hostile, decoded);
        assertEquals(200L, decoded.readyTick("mkm:renewal"));
    }
}
