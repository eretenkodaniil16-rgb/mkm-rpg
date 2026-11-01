package com.eretenkodaniil.mkm.client.gui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class CharacterSheetLayoutTest {
    @Test
    void standardAndSmallGuiStayInsideViewport() {
        for (int screenWidth : new int[] {200, 240, 320, 360, 640, 1920}) {
            for (int screenHeight : new int[] {200, 220, 232, 240, 300, 320, 480, 1080}) {
                CharacterSheetLayout l = CharacterSheetLayout.forViewport(screenWidth, screenHeight);
                assertTrue(l.left() >= 0);
                assertTrue(l.top() >= 0);
                assertTrue(l.left() + l.width() <= screenWidth);
                assertTrue(l.top() + l.height() <= screenHeight);
                assertTrue(l.attributeButtonTop(6) + 13 <= l.footerTop(), screenWidth + "x" + screenHeight);
                assertTrue(l.attributeTextTop(6) + 9 <= l.footerTop(), screenWidth + "x" + screenHeight);
                assertTrue(l.footerTop() + 20 <= l.top() + l.height());
                if (l.compact()) {
                    assertTrue(l.micro() || l.contentTop() + 53 + 9 <= l.statsTop(),
                            "points line must not collide with attributes");
                }
            }
        }
    }

    @Test
    void conventionalThreeHundredTwentyByTwoFortyShowsXpAndDerivedStats() {
        CharacterSheetLayout l = CharacterSheetLayout.forViewport(320, 240);
        assertTrue(l.compact());
        assertFalse(l.micro());
        assertFalse(l.narrow());
        assertEquals(312, l.width());
        assertEquals(232, l.height());
        assertEquals(4 + 95, l.statsTop());
    }

    @Test
    void tinyViewportSwitchesToEssentialContentOnly() {
        CharacterSheetLayout l = CharacterSheetLayout.forViewport(200, 200);
        assertTrue(l.micro());
        assertTrue(l.narrow());
        assertTrue(l.attributeTextTop(0) > l.top() + 33 + 9);
    }
}
