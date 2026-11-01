package com.eretenkodaniil.mkm.client.animation;

import com.eretenkodaniil.mkm.combat.ActionTimeline;
import com.eretenkodaniil.mkm.combat.ActionTimelineRules;
import com.eretenkodaniil.mkm.combat.DodgeDirection;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimationLibraryTest {
    @Test
    void loadsEveryPublishedClipAndBindsServerHitMoments() {
        AnimationLibrary library = AnimationLibrary.loadClasspath();
        assertEquals(15, library.clipCount());
        AnimationProfile sword = library.profile("mkm:one_handed_sword");
        assertNotNull(sword);
        for (int stage = 1; stage <= 3; stage++) {
            AnimationClip clip = library.clip(sword.clipFor("combo_" + stage));
            assertNotNull(clip);
            assertEquals(ActionTimelineRules.windupTicks(stage)
                    + 1 + ActionTimelineRules.recoveryTicks(stage), clip.durationTicks());
            assertEquals(AnimationClip.Layer.FULL_BODY, clip.layer());
            assertTrue(clip.markers().stream().anyMatch(m -> m.name().equals("impact")));
            assertFalse(clip.sample(.5D).isEmpty());
        }
        AnimationProfile unarmed = library.profile(AnimationController.UNARMED_PROFILE);
        assertNotNull(unarmed, "Empty hand must have separate punch animations");
        for (int stage = 1; stage <= 3; stage++) {
            AnimationClip punch = library.clip(unarmed.clipFor("combo_" + stage));
            assertNotNull(punch);
            assertTrue(punch.id().startsWith("mkm:unarmed/"));
            assertEquals(ActionTimelineRules.windupTicks(stage)
                    + 1 + ActionTimelineRules.recoveryTicks(stage), punch.durationTicks());
        }
        assertEquals(AnimationController.UNARMED_PROFILE,
                AnimationController.profileForEmptyMainHand(true));
        assertEquals(AnimationController.FIRST_PROFILE,
                AnimationController.profileForEmptyMainHand(false));
        for (int d = 0; d <= DodgeDirection.LAST; d++) {
            AnimationClip clip = library.clip(sword.clipFor("dodge_" + d));
            assertNotNull(clip);
            assertEquals(AnimationClip.Layer.FULL_BODY, clip.layer());
            assertEquals(10, clip.durationTicks());
        }
    }

    @Test
    void explicitC3ControllerFollowsServerPhaseAndAllowsFallback() {
        AnimationLibrary lib = AnimationLibrary.loadClasspath();
        ActionTimeline first = ActionTimelineRules.beginCombo(new ActionTimeline(), 40);
        AnimationController.Frame early = AnimationController.sample(lib,
                AnimationController.FIRST_PROFILE, first, .5F);
        assertNotNull(early);
        assertEquals("mkm:sword/light_1", early.clipId());
        assertEquals(2.5D/14D, early.localTime(), 0.00001);
        assertNotNull(AnimationController.sample(lib, AnimationController.FIRST_PROFILE,
                ActionTimelineRules.beginDodge(new ActionTimeline(), 40,
                        DodgeDirection.BACKWARD_LEFT), .5F));
        assertNull(AnimationController.sample(lib, "mkm:missing", first, .5F));
    }

    @Test
    void malformedAssetFailsValidationRatherThanBeingPlayed() {
        var realOpener = (java.util.function.Function<String, java.io.InputStream>)
                path -> AnimationLibraryTest.class.getClassLoader().getResourceAsStream(path);
        assertThrows(IllegalArgumentException.class, () -> AnimationLibrary.load(path -> {
            if (path.endsWith("sword/light_1.json")) {
                String invalid = """
                    {"format":1,"id":"mkm:sword/light_1","duration_ticks":10,
                    "layer":"upper_body","tracks":{"right_arm":[
                    {"t":0,"r":[0,0,0]},{"t":1,"r":[0,0,0]}]}}
                    """;
                return new ByteArrayInputStream(invalid.getBytes(StandardCharsets.UTF_8));
            }
            return realOpener.apply(path);
        }));
    }
}
