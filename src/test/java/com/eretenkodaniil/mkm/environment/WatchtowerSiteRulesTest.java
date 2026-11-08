package com.eretenkodaniil.mkm.environment;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class WatchtowerSiteRulesTest {
    record Cell(int x, int y, int z) {}

    @Test
    void clearSupportedSitePassesWithoutTouchingGround() {
        var probe = new Grid();
        assertTrue(WatchtowerSiteRules.canPlace(probe));
    }

    @Test
    void neverOverwritesPlayerBlockEvenIfItLooksLikeNaturalStone() {
        var probe = new Grid();
        probe.occupied.add(new Cell(7, 5, 6));
        assertFalse(WatchtowerSiteRules.canPlace(probe));
        probe.occupied.clear();
        probe.occupied.add(new Cell(18, 5, 14));
        assertFalse(WatchtowerSiteRules.canPlace(probe));
    }

    @Test
    void rejectsOverheadStructuresAndUnsupportedCliffs() {
        var probe = new Grid();
        probe.occupied.add(new Cell(0, 13, 0));
        assertFalse(WatchtowerSiteRules.canPlace(probe));
        probe.occupied.clear();
        probe.groundY = -5;
        assertFalse(WatchtowerSiteRules.canPlace(probe));
    }

    @Test
    void acceptsSlopeWithinFourBlocksButRejectsUnstableSupports() {
        var probe = new Grid();
        probe.groundY = -4;
        assertTrue(WatchtowerSiteRules.canPlace(probe));
        probe.unstable.add(new Cell(4, -4, 3));
        assertFalse(WatchtowerSiteRules.canPlace(probe));
    }

    private static final class Grid implements WatchtowerSiteRules.Probe {
        int groundY = -1;
        Set<Cell> occupied = new HashSet<>();
        Set<Cell> unstable = new HashSet<>();

        @Override
        public boolean isAir(int x, int y, int z) {
            return y > groundY && !occupied.contains(new Cell(x, y, z));
        }

        @Override
        public boolean sturdyBelow(int x, int y, int z) {
            return y <= groundY && !unstable.contains(new Cell(x, y, z));
        }
    }
}
