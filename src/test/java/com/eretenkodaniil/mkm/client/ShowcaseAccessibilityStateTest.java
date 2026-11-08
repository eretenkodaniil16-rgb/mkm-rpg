package com.eretenkodaniil.mkm.client;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Pure client preference behavior, no Minecraft runtime required. */
final class ShowcaseAccessibilityStateTest {
    @Test void defaultsAndThreeIndependentTogglesRemainIndependent() {
        var a=ShowcaseAccessibilityState.defaults();
        assertTrue(a.hudVisible());
        assertFalse(a.highContrast());
        assertFalse(a.reducedParticles());
        var b=a.toggleContrast().toggleParticles();
        assertTrue(b.hudVisible());
        assertTrue(b.highContrast());
        assertTrue(b.reducedParticles());
        assertEquals(a,b.toggleContrast().toggleParticles());
        assertFalse(a.toggleHud().hudVisible());
        assertFalse(a.toggleHud().highContrast());
    }

    @Test void persistentPropertiesRoundtripAndMalformedValuesUseSafeDefaults(){
        var x=new ShowcaseAccessibilityState(false,true,true);
        assertEquals(x,ShowcaseAccessibilityState.read(x.properties()));
        Properties invalid=new Properties();
        invalid.setProperty("hudVisible","whatever");
        invalid.setProperty("highContrast","TRUE"); // strict, fail closed
        invalid.setProperty("reducedParticles","true");
        var parsed=ShowcaseAccessibilityState.read(invalid);
        assertTrue(parsed.hudVisible());
        assertFalse(parsed.highContrast());
        assertTrue(parsed.reducedParticles());
    }
}
