package com.eretenkodaniil.mkm.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RpgHudGeometryTest {
    @Test void remainsInsideCommonAndNarrowViewports() {
        int[][] sizes = {{1920,1080},{854,480},{320,240},{200,160}};
        for (int[] size : sizes) for (int scale : new int[]{75,100,125,150}) {
            var g = RpgHudGeometry.forViewport(size[0], size[1], scale);
            assertTrue(g.fits(size[0], size[1]), size[0] + "x" + size[1] + " at " + scale);
            assertTrue(g.scale() > 0);
        }
    }
    @Test void accessibilityScaleIsNotIgnoredOnDesktop() {
        assertEquals(0.75F, RpgHudGeometry.forViewport(800, 600, 75).scale());
        assertEquals(1.5F, RpgHudGeometry.forViewport(800, 600, 150).scale());
    }
}
