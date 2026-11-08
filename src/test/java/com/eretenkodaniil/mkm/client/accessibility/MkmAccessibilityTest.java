package com.eretenkodaniil.mkm.client.accessibility;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MkmAccessibilityTest {
    @Test void allGroupsHaveIndependentPersistedChoices() {
        var a = MkmAccessibility.Options.defaults()
                .cycle(MkmAccessibility.Setting.HUD_SCALE)
                .cycle(MkmAccessibility.Setting.TEXT_SCALE)
                .cycle(MkmAccessibility.Setting.HIGH_CONTRAST)
                .cycle(MkmAccessibility.Setting.DISTINCT_COLORS)
                .cycle(MkmAccessibility.Setting.REDUCED_SHAKE)
                .cycle(MkmAccessibility.Setting.REDUCED_FLASH)
                .cycle(MkmAccessibility.Setting.VFX)
                .cycle(MkmAccessibility.Setting.SIMPLE_INPUT)
                .cycle(MkmAccessibility.Setting.GAMEPAD);
        assertEquals(a, MkmAccessibility.Options.fromProperties(a.toProperties()));
        assertEquals(125, a.hudScale());
        assertEquals(125, a.textScale());
        assertTrue(a.highContrast());
        assertTrue(a.distinctColors());
        assertFalse(a.reducedShake());
        assertFalse(a.reducedFlash());
        assertEquals(MkmAccessibility.Vfx.REDUCED, a.vfx());
        assertTrue(a.simpleInput());
        assertTrue(a.gamepadEnabled());
    }
    @Test void invalidAndUnknownPropertiesFallBackWithoutCrashing() {
        Properties p = new Properties();
        p.setProperty("hudScale", "5000");
        p.setProperty("textScale", "invalid");
        p.setProperty("reducedFlash", "maybe");
        p.setProperty("vfx", "STROBE");
        var saved = MkmAccessibility.Options.fromProperties(p);
        assertEquals(MkmAccessibility.Options.defaults(), saved);
    }
    @Test void cyclesStayBounded() {
        var v = MkmAccessibility.Options.defaults();
        for (int i = 0; i < 4; i++) v = v.cycle(MkmAccessibility.Setting.HUD_SCALE);
        for (int i = 0; i < 4; i++) v = v.cycle(MkmAccessibility.Setting.TEXT_SCALE);
        assertEquals(MkmAccessibility.Options.defaults(), v);
    }
}
