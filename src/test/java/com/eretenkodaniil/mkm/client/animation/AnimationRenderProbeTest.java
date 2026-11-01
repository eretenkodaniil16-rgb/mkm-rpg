package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimationRenderProbeTest {
    @Test
    void onlyActualRecentRendererCallsCountAsRendered() {
        long tick = 100_000L;
        AnimationRenderProbe.mark(tick, "mkm:sword/light_1");
        assertEquals("RENDERED mkm:sword/light_1", AnimationRenderProbe.status(tick));
        assertEquals("RENDERED mkm:sword/light_1", AnimationRenderProbe.status(tick + 2L));
        assertEquals("NOT RENDERED (use F5)", AnimationRenderProbe.status(tick + 3L));
        AnimationRenderProbe.mark(tick + 10L, "mkm:unarmed/jab");
        assertEquals("RENDERED mkm:unarmed/jab", AnimationRenderProbe.status(tick + 10L));
        assertEquals("NOT RENDERED (use F5)", AnimationRenderProbe.status(tick + 9L));
    }
}
