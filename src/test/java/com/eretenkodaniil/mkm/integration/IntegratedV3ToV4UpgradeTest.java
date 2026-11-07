package com.eretenkodaniil.mkm.integration;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.character.AllocationRules;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.eretenkodaniil.mkm.character.QuestRewardRecovery;
import com.eretenkodaniil.mkm.quests.FordQuestRules;
import com.eretenkodaniil.mkm.quests.FordQuestState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Real PR#30 schema-v3 disk CODEC JSON fixtures, not invented v4 input.
 * No Minecraft world or playerdata NBT is claimed to have been opened.
 */
final class IntegratedV3ToV4UpgradeTest {
    private static final String REWARD = "mkm:ford_warden/complete";
    private static JsonObject old(String variant) {
        String path="/migration/character_v3_0_0_5_fabric_4_"+variant+".json";
        var stream=IntegratedV3ToV4UpgradeTest.class.getResourceAsStream(path);
        assertNotNull(stream,path);
        try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
            return JsonParser.parseReader(reader).getAsJsonObject();
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    private static CharacterData read(JsonObject raw) {
        return CharacterData.CODEC.codec().parse(JsonOps.INSTANCE,raw).getOrThrow();
    }
    private static CharacterData roundTrip(CharacterData data){
        var codec=CharacterData.CODEC.codec();
        return codec.parse(JsonOps.INSTANCE,
                codec.encodeStart(JsonOps.INSTANCE,data).getOrThrow()).getOrThrow();
    }
    private static void equalV3Fields(JsonObject source,CharacterData got) {
        assertEquals(4,got.schemaVersion());
        assertEquals(source.get("experience").getAsLong(),got.experience());
        assertEquals(source.get("strength").getAsInt(),got.strength());
        assertEquals(source.get("dexterity").getAsInt(),got.dexterity());
        assertEquals(source.get("vitality").getAsInt(),got.vitality());
        assertEquals(source.get("endurance").getAsInt(),got.endurance());
        assertEquals(source.get("intelligence").getAsInt(),got.intelligence());
        assertEquals(source.get("willpower").getAsInt(),got.willpower());
        assertEquals(source.get("perception").getAsInt(),got.perception());
        assertEquals(source.get("earned_points").getAsInt(),got.earnedPoints());
        for(int i=0;i<7;i++)
            assertEquals(source.getAsJsonArray("allocated_points").get(i).getAsInt(),
                    got.allocatedPoints().get(i));
        assertEquals(got,roundTrip(got));
        assertTrue(got.claimedRewardIds().isEmpty());
    }
    @Test void allocatedV3PreservesAllCreditsThenClaimsOnceThroughV4Restart() {
        var source=old("allocated");
        CharacterData restored=read(source);
        equalV3Fields(source,restored);
        assertEquals(4000,restored.experience());
        assertEquals(16,restored.earnedPoints());
        assertEquals(14,restored.spentPoints());
        assertEquals(2,restored.availablePoints());
        var alloc=AllocationRules.tryAllocate(restored,new int[]{3,4,1,0,2,3,2},14);
        assertNotNull(alloc);
        assertEquals(15,alloc.spentPoints());
        assertEquals(1,alloc.availablePoints());
        assertNull(AllocationRules.tryAllocate(alloc,new int[]{3,4,1,0,2,3,2},14),
                "Repeated stale allocation cannot buy points twice");
        var paid=CharacterRewardRules.grantOnce(alloc,REWARD,100);
        assertEquals(CharacterRewardRules.Status.GRANTED,paid.status());
        var saved=roundTrip(paid.character());
        assertEquals(4100,saved.experience());
        assertEquals(15,saved.spentPoints());
        assertTrue(saved.hasClaimedReward(REWARD));
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(saved,REWARD,100).status());
    }
    @Test void v3AdminAttributesAndCappedLevelDoNotResetOnMigration(){
        var admin=old("legacy_admin");
        CharacterData withAdminEdit=read(admin);
        equalV3Fields(admin,withAdminEdit);
        assertEquals(63,withAdminEdit.strength());
        assertEquals(2,withAdminEdit.spentPoints());
        assertEquals(2,withAdminEdit.availablePoints());
        var capped=old("max_level");
        var loaded=read(capped);
        equalV3Fields(capped,loaded);
        assertEquals(650000,loaded.experience());
        assertEquals(198,loaded.earnedPoints());
        assertEquals(138,loaded.spentPoints());
        var paid=CharacterRewardRules.grantOnce(loaded,REWARD,75);
        assertEquals(CharacterRewardRules.Status.GRANTED,paid.status());
        assertEquals(0,paid.creditedExperience());
        assertTrue(roundTrip(paid.character()).hasClaimedReward(REWARD));
    }
    @Test void corruptedV3LedgerNeverBecomesFreeUnspentCredits(){
        JsonObject missing=old("allocated");
        missing.remove("allocated_points");
        assertThrows(RuntimeException.class,()->read(missing));
        JsonObject shortLedger=old("allocated");
        shortLedger.getAsJsonArray("allocated_points").remove(0);
        assertThrows(RuntimeException.class,()->read(shortLedger));
    }
    @Test void olderRewardedQuestWithoutReceiptIsNotRepaid(){
        FordQuestState legacy=new FordQuestState(2,FordQuestState.REWARDED,true,0);
        CharacterData migrated=read(old("legacy_admin"));
        assertEquals(QuestRewardRecovery.Decision.MANUAL_REVIEW,
                QuestRewardRecovery.decide(migrated,REWARD,false,true,500L));
        assertEquals(FordQuestState.REWARDED,legacy.stage());
        assertSame(legacy,FordQuestRules.claim(legacy));
        assertEquals(migrated.experience(),roundTrip(migrated).experience());
    }
    @Test void receiptCommittedBeforeQuestStageOnlyFinalizesOnRestart(){
        var initial=read(old("allocated"));
        var ready=FordQuestRules.onOwnedRaiderDefeated(
                FordQuestRules.accept(new FordQuestState()));
        assertEquals(FordQuestState.RETURN_TO_WARDEN,ready.stage());
        var grant=CharacterRewardRules.grantOnce(initial,REWARD,75);
        var afterSavedPlayerRecord=roundTrip(grant.character());
        assertEquals(QuestRewardRecovery.Decision.FINALIZE_QUEST,
                QuestRewardRecovery.decide(afterSavedPlayerRecord,REWARD,true,false,-1));
        var finished=FordQuestRules.claim(ready);
        assertEquals(QuestRewardRecovery.Decision.COMPLETE,
                QuestRewardRecovery.decide(afterSavedPlayerRecord,REWARD,false,true,-1));
        assertEquals(FordQuestState.REWARDED,finished.stage());
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(afterSavedPlayerRecord,REWARD,75).status());
    }
    @Test void futureSchemaBlocksMutationsAndReplays(){
        var v5=old("allocated");
        v5.addProperty("schema_version",5);
        var obj=read(v5);
        assertEquals(5,obj.schemaVersion());
        assertEquals(CharacterRewardRules.Status.INVALID_REQUEST,
                CharacterRewardRules.grantOnce(obj,REWARD,75).status());
        assertThrows(IllegalStateException.class,()->obj.withExperience(5000));
        assertNull(AllocationRules.tryAllocate(obj,new int[]{3,4,1,0,2,3,2},14));
    }
}
