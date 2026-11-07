package com.eretenkodaniil.mkm.integration;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.eretenkodaniil.mkm.quests.FordEncounterRegistry;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

/**
 * Pure 256-owner reservation stress; it models unloaded chunks as ABSENT entity
 * queries. This is NOT a live loaded Minecraft server performance benchmark.
 */
final class MultiOwnerEncounterStressContractTest {
    @Test void reservationsSurviveUnknownChunksAndOnlyExactOwnerDeathRemoves() {
        FordEncounterRegistry registry=new FordEncounterRegistry();
        for(int i=0;i<256;i++){
            String owner="owner-"+i,raider="raider-"+i;
            assertTrue(registry.canRegister(owner));
            registry=registry.register(owner,
                    new FordEncounterRegistry.Entry(raider,"warden",i*32,64,i*16));
        }
        assertEquals(256,registry.byOwner().size());
        var saved=FordEncounterRegistry.CODEC.parse(JsonOps.INSTANCE,
                FordEncounterRegistry.CODEC.encodeStart(JsonOps.INSTANCE,registry)
                        .getOrThrow()).getOrThrow();
        assertEquals(registry,saved);
        for(int i=0;i<256;i++){
            String owner="owner-"+i;
            assertFalse(saved.canRegister(owner));
            assertSame(saved,saved.removeIfMatch(owner,"raider-"+((i+1)%256)));
            assertEquals(i*32,saved.byOwner().get(owner).x());
        }
        var after=saved;
        for(int i=0;i<128;i++){
            after=after.removeIfMatch("owner-"+i,"raider-"+i);
        }
        assertEquals(128,after.byOwner().size());
        for(int i=128;i<256;i++){
            assertEquals("raider-"+i,after.byOwner().get("owner-"+i).raiderUuid());
        }
        assertEquals(256,saved.byOwner().size(),"Persistent save baseline must be immutable");
    }

    @Test void twoOwnersShareRewardIdButNotReceiptsAcrossRestarts(){
        String key="mkm:ford_warden/complete";
        var ownerA=CharacterRewardRules.grantOnce(new CharacterData(),key,75);
        var ownerB=CharacterRewardRules.grantOnce(new CharacterData(),key,100);
        assertEquals(CharacterRewardRules.Status.GRANTED,ownerA.status());
        assertEquals(CharacterRewardRules.Status.GRANTED,ownerB.status());
        assertEquals(75,ownerA.character().experience());
        assertEquals(100,ownerB.character().experience());
        var codec=CharacterData.CODEC.codec();
        var reloadA=codec.parse(JsonOps.INSTANCE,
                codec.encodeStart(JsonOps.INSTANCE,ownerA.character()).getOrThrow()).getOrThrow();
        var reloadB=codec.parse(JsonOps.INSTANCE,
                codec.encodeStart(JsonOps.INSTANCE,ownerB.character()).getOrThrow()).getOrThrow();
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(reloadA,key,100).status());
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(reloadB,key,75).status());
        assertNotEquals(reloadA.experience(),reloadB.experience());
    }
}
