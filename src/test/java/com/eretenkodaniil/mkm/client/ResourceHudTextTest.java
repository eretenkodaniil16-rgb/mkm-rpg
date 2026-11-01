package com.eretenkodaniil.mkm.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceHudTextTest {
    private static int fixedPixelWidth(String string) {
        return string.codePointCount(0, string.length()) * 7;
    }

    @Test void fullLabelIsPreservedWhenItFits() {
        assertEquals("MP 80/100", ResourceHudText.fit("MP 80/100", 70,
                ResourceHudTextTest::fixedPixelWidth));
        assertEquals("", ResourceHudText.fit("", 0, ResourceHudTextTest::fixedPixelWidth));
    }

    @Test void localizedLabelsDoNotExceedColumnOrSplitUnicode() {
        String label = "Исцеление готово";
        String trimmed = ResourceHudText.fit(label, 49, ResourceHudTextTest::fixedPixelWidth);
        assertTrue(trimmed.endsWith("\u2026"));
        assertTrue(fixedPixelWidth(trimmed) <= 49);
        assertEquals("Исцеле\u2026", trimmed);

        String symbol = "🗝 Магия";
        String shortened = ResourceHudText.fit(symbol, 21, ResourceHudTextTest::fixedPixelWidth);
        assertEquals("🗝 \u2026", shortened);
        assertEquals(21, fixedPixelWidth(shortened));
    }

    @Test void longReboundKeyCannotObscureCentralF5Model() {
        for (int barWidth : new int[]{40, 76, 136, 148}) {
            String text = ResourceHudText.fit(
                    "CTRL+ALT+PAGE_DOWN: Исцеление", barWidth,
                    ResourceHudTextTest::fixedPixelWidth);
            assertTrue(fixedPixelWidth(text) <= barWidth);
        }
    }

    @Test void tinyWidthProducesEmptyNotOverflowingEllipsis() {
        assertEquals("", ResourceHudText.fit("ABC", 2,
                ResourceHudTextTest::fixedPixelWidth));
        assertThrows(IllegalArgumentException.class,
                () -> ResourceHudText.fit(null, 30, ResourceHudTextTest::fixedPixelWidth));
    }
}
