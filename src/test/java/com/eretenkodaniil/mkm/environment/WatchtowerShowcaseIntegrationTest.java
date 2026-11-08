package com.eretenkodaniil.mkm.environment;

import static org.junit.jupiter.api.Assertions.*;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterRewardRules;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class WatchtowerShowcaseIntegrationTest {
    @Test void reservationAndFinalizationAreVersionedAndFailClosed() {
        var origin = new BlockPos(120, 68, -40);
        var fresh = new WatchtowerSiteRegistry();
        assertTrue(fresh.canReserve(origin));
        var pending = fresh.reserve(origin, 2);
        assertNotSame(fresh, pending);
        assertFalse(pending.canReserve(origin));
        assertFalse(pending.verifiedRune(origin.offset(7, 5, 6)));
        var loadedPending = WatchtowerSiteRegistry.CODEC.parse(JsonOps.INSTANCE,
                WatchtowerSiteRegistry.CODEC.encodeStart(JsonOps.INSTANCE, pending).getOrThrow()).getOrThrow();
        assertEquals(pending, loadedPending);
        var placed = loadedPending.finish(origin);
        assertTrue(placed.verifiedRune(origin.offset(7, 5, 6)));
        assertFalse(placed.verifiedRune(origin.offset(7, 5, 7)));
        assertSame(placed, placed.finish(origin));
        assertSame(placed, placed.reserve(origin, 2));
        assertEquals(1, placed.sites().size());
        assertEquals(2, placed.sites().getFirst().variant());
        assertEquals(placed, WatchtowerSiteRegistry.CODEC.parse(JsonOps.INSTANCE,
                WatchtowerSiteRegistry.CODEC.encodeStart(JsonOps.INSTANCE, placed).getOrThrow()).getOrThrow());
    }

    @Test void singlePlayerRuneReceiptSurvivesSaveReloadAndTwoPlayersStaySeparate(){
        String id = "mkm:showcase/watchtower_rune";
        var a = CharacterRewardRules.grantOnce(new CharacterData(), id, RuneLoreRules.REWARD_XP);
        var b = CharacterRewardRules.grantOnce(new CharacterData(), id, RuneLoreRules.REWARD_XP);
        assertEquals(CharacterRewardRules.Status.GRANTED, a.status());
        assertEquals(CharacterRewardRules.Status.GRANTED, b.status());
        var codec=CharacterData.CODEC.codec();
        var reload=codec.parse(JsonOps.INSTANCE,
                codec.encodeStart(JsonOps.INSTANCE,a.character()).getOrThrow()).getOrThrow();
        assertEquals(25, reload.experience());
        assertEquals(CharacterRewardRules.Status.ALREADY_GRANTED,
                CharacterRewardRules.grantOnce(reload,id,25).status());
        assertEquals(25,b.character().experience());
        assertEquals(0,new CharacterData().experience());
    }

    @Test void allOriginCombinationsHaveRealDistinctClassEffectAndRaceLore() {
        var hw=RuneLoreRules.discover("mkm:human","mkm:warrior");
        var hm=RuneLoreRules.discover("mkm:human","mkm:mage");
        var dw=RuneLoreRules.discover("mkm:dwarf","mkm:warrior");
        var dm=RuneLoreRules.discover("mkm:dwarf","mkm:mage");
        assertEquals(hw.loreKey(),hm.loreKey());
        assertEquals(dw.loreKey(),dm.loreKey());
        assertNotEquals(hw.loreKey(),dw.loreKey());
        assertNotEquals(hw.classKey(),hm.classKey());
        assertFalse(hw.nightVision());
        assertTrue(hm.nightVision());
        assertFalse(dw.nightVision());
        assertTrue(dm.nightVision());
        assertNull(RuneLoreRules.discover("mkm:unrecognized","mkm:warrior"));
        assertNull(RuneLoreRules.discover("mkm:human","mkm:unrecognized"));
    }

    @Test void registryDoesNotPermitDuplicateAnchorsOrOverCapacity(){
        var registry=new WatchtowerSiteRegistry();
        for(int i=0;i<WatchtowerSiteRegistry.MAX_SITES;i++){
            BlockPos at=new BlockPos(i*32,64,0);
            assertTrue(registry.canReserve(at));
            registry=registry.reserve(at,(i%3)+1).finish(at);
        }
        assertEquals(32,registry.sites().size());
        assertFalse(registry.canReserve(new BlockPos(999,64,0)));
        assertFalse(registry.verifiedRune(new BlockPos(999,69,6)));
    }
}
