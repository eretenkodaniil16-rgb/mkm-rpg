package com.eretenkodaniil.mkm.client.animation.engine;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import com.eretenkodaniil.mkm.client.animation.AnimationLibrary;
import com.eretenkodaniil.mkm.combat.ActionTimelineRules;
class NativeAssetTest {
    @Test void bothDemosAlignExactlyWithAuthoritativeFirstImpact(){
        assertEquals(14,ActionTimelineRules.windupTicks(1)+1+ActionTimelineRules.recoveryTicks(1));
        for(NativeClip clip:new NativeClip[]{NativeDemoLibrary.sword(),NativeDemoLibrary.jab()}){
            assertEquals(14,clip.ticks());
            assertEquals(6,clip.tracks().size());
            double hit=ActionTimelineRules.windupTicks(1)/(double)clip.ticks();
            assertTrue(clip.events().stream().anyMatch(e->e.id().equals("impact")
                    && Math.abs(e.time()-hit)<.000001));
            assertEquals(0,PoseSampler.sample(clip,0).get("body").translation().x(),1e-10);
            assertEquals(0,PoseSampler.sample(clip,1).get("body").translation().x(),1e-10);
            for(int i=0;i<=200;i++){
                var pose=PoseSampler.sample(clip,i/200D);
                assertEquals(6,pose.size());
                for(var t:pose.values()){
                    assertTrue(Double.isFinite(t.rotation().x()));
                    assertTrue(t.scale().x()>.05);
                }
            }
        }
    }
    @Test void existingC3ClipsCanBeImportedWithoutRewritingAssets(){
        var old=AnimationLibrary.loadClasspath().clip("mkm:unarmed/jab");
        NativeClip imported=LegacyC3Importer.convert(old);
        assertEquals(old.id(),imported.id());
        assertEquals(old.durationTicks(),imported.ticks());
        assertEquals(old.tracks().size(),imported.tracks().size());
        assertEquals(old.markers().size(),imported.events().size());
        assertTrue(imported.events().stream().anyMatch(e->e.id().equals("impact")));
    }
    @Test void malformedSchemaIsRejectedWithoutExecutingData(){
        String invalid="{\"format\":2,\"units\":\"blocks\",\"rotation\":\"xyz_radians\"}";
        assertThrows(IllegalArgumentException.class,()->NativeClipLoader.load(
                "assets/mkm/mnae/invalid.json",
                name->new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8))));
        assertThrows(IllegalArgumentException.class,()->NativeClipLoader.read(
                "assets/mkm/mnae/../secrets.json"));
    }
}
