package com.eretenkodaniil.mkm.environment;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ShowcaseAmbushGateTest {
    @Test void onlyVerifiedDiscoveryNearPlacedTowerCanAuthorizeEncounter() {
        assertTrue(ShowcaseAmbushGate.allowed(true,false,true,true,true,true));
        assertFalse(ShowcaseAmbushGate.allowed(true,false,true,true,false,true));
        assertFalse(ShowcaseAmbushGate.allowed(true,false,true,true,true,false));
        assertFalse(ShowcaseAmbushGate.allowed(true,false,true,false,true,true));
        assertFalse(ShowcaseAmbushGate.allowed(true,false,false,true,true,true));
        assertFalse(ShowcaseAmbushGate.allowed(true,true,true,true,true,true));
        assertFalse(ShowcaseAmbushGate.allowed(false,false,true,true,true,true));
        assertEquals(2304D,ShowcaseAmbushGate.MAX_DISTANCE_SQUARED);
    }
}
