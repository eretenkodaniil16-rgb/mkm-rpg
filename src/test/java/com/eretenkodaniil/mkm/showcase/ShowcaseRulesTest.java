package com.eretenkodaniil.mkm.showcase;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginData;
import com.eretenkodaniil.mkm.encounter.EncounterLedger;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ShowcaseRulesTest {
    private static final String OWNER_A="b1a00000-0000-4000-8000-000000000001";
    private static final String OWNER_B="b1b00000-0000-4000-8000-000000000002";
    private EncounterLedger.Entry save(String owner,int xp) {
        return new EncounterLedger.Entry(1,owner,80,65,180,xp,EncounterLedger.RESERVED,
                List.of(new EncounterLedger.Member(owner+"-1","minecraft:husk","frontline",false,false),
                        new EncounterLedger.Member(owner+"-2","minecraft:spider","ambusher",false,false),
                        new EncounterLedger.Member(owner+"-3","minecraft:skeleton","leader",true,false)));
    }
    @Test void studyRequiresPerceptionOrActualMageClass() {
        var fresh=new CharacterOriginData();
        var dwarfMage=new CharacterOriginData(1,1,"mkm:dwarf","mkm:mage");
        var humanWarrior=new CharacterOriginData(1,1,"mkm:human","mkm:warrior");
        assertFalse(ShowcaseRules.mayStudy(11,fresh));
        assertFalse(ShowcaseRules.mayStudy(11,humanWarrior));
        assertTrue(ShowcaseRules.mayStudy(12,humanWarrior));
        assertTrue(ShowcaseRules.mayStudy(8,dwarfMage));
    }
    @Test void choiceIsIrreversibleAndReloadedWithoutLegacyQuestMutation() {
        var fresh=new ShowcaseProgress();
        assertSame(fresh,fresh.begin(ShowcaseProgress.STUDY,false));
        var found=fresh.discover();
        var active=found.begin(ShowcaseProgress.STUDY,true);
        assertNotSame(found,active);
        assertEquals(ShowcaseProgress.STUDY,active.approach());
        assertSame(active,active.begin(ShowcaseProgress.DEFEND,true));
        var finished=active.finish();
        assertEquals(ShowcaseProgress.COMPLETED,finished.stage());
        assertSame(finished,finished.finish());
        var json=ShowcaseProgress.CODEC.encodeStart(JsonOps.INSTANCE,finished).getOrThrow();
        assertEquals(finished,ShowcaseProgress.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow());
    }
    @Test void twoOwnersHaveDistinctFightAndIndependentV4RewardReceipts() {
        var aKey=ShowcaseRules.eventId(OWNER_A,ShowcaseProgress.DEFEND);
        var bKey=ShowcaseRules.eventId(OWNER_B,ShowcaseProgress.STUDY);
        var world=new EncounterLedger().reserve(aKey,save(OWNER_A,100))
                .reserve(bKey,save(OWNER_B,125));
        assertEquals(6,world.activeMobReservations());
        assertFalse(world.canReserve(aKey,3));
        assertEquals(2,world.encounters().size());
        for (int i=1;i<=3;i++) world=world.markDeath(aKey,OWNER_A+"-"+i);
        assertEquals(EncounterLedger.DEFEATED,world.find(aKey).phase());
        assertEquals(EncounterLedger.RESERVED,world.find(bKey).phase());
        var a=new ShowcaseProgress().discover().begin(ShowcaseProgress.DEFEND,true);
        assertTrue(ShowcaseRules.canResolve(a,world.find(aKey),OWNER_A));
        assertFalse(ShowcaseRules.canResolve(a,world.find(bKey),OWNER_A));
        var originalA=new CharacterData();
        var originalB=new CharacterData();
        var rewarded=CharacterRewardRules.grantOnce(originalA,ShowcaseRules.rewardId(a.approach()),
                world.find(aKey).rewardXp());
        assertEquals(100L,rewarded.character().experience());
        assertEquals(0L,originalB.experience());
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(rewarded.character(),
                        ShowcaseRules.rewardId(a.approach()),100).status());
        world=world.claim(aKey);
        assertFalse(world.canReserve(aKey,3));
        var encoded=EncounterLedger.CODEC.encodeStart(JsonOps.INSTANCE,world).getOrThrow();
        assertEquals(world,EncounterLedger.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow());
    }
    @Test void separateWorldInstancesNeverShareReservations() {
        String key=ShowcaseRules.eventId(OWNER_A,ShowcaseProgress.DEFEND);
        var worldA=new EncounterLedger().reserve(key,save(OWNER_A,100));
        var worldB=new EncounterLedger();
        assertNull(worldB.find(key));
        assertTrue(worldB.canReserve(key,3));
        assertNotNull(worldA.find(key));
    }
    @Test void siteAndKeeperUuidSurviveServerRestartWithoutDuplicateCreation() {
        var site=new ShowcaseSite().place(new net.minecraft.core.BlockPos(110,65,210));
        var saved=site.withKeeper(UUID.randomUUID().toString());
        assertSame(saved,saved.withKeeper(UUID.randomUUID().toString()));
        assertEquals(new net.minecraft.core.BlockPos(117,70,216),saved.runePos());
        assertEquals(new net.minecraft.core.BlockPos(119,65,242),saved.encounterAnchor());
        var json=ShowcaseSite.CODEC.encodeStart(JsonOps.INSTANCE,saved).getOrThrow();
        assertEquals(saved,ShowcaseSite.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow());
    }
}
