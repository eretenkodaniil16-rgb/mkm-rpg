package com.eretenkodaniil.mkm.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceHudLayoutTest {
    @Test void bothBarsStayLeftOfAvatarCenterAtSupportedScales() {
        for (int[] size : new int[][]{{200, 200}, {320, 240}, {480, 270},
                {960, 540}, {1920, 1080}}) {
            ResourceHudLayout layout = ResourceHudLayout.forViewport(size[0], size[1]);
            assertTrue(layout.x() + layout.width() < size[0] / 2);
            assertTrue(layout.staminaY() - (layout.manaY() + 23) >= 40);
            assertTrue(layout.manaY() - 12 >= 0);
            assertTrue(layout.staminaY() + 16 < size[1]);
            assertTrue(layout.width() > 0 && layout.width() <= 148);
        }
    }

    @Test void smallViewportUsesCompactLabels() {
        assertTrue(ResourceHudLayout.forViewport(200, 200).narrow());
        assertFalse(ResourceHudLayout.forViewport(480, 270).narrow());
        assertThrows(IllegalArgumentException.class,
                () -> ResourceHudLayout.forViewport(100, 100));
    }
}
