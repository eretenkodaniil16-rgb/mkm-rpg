package com.eretenkodaniil.mkm.client.gui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class OriginScreenLayoutTest {
    @Test void twoHundredPixelAndStandardGuiScalesKeepControlsOnScreen() {
        for (int[] viewport : new int[][]{{200,200},{320,240},{460,330},{1920,1080}}) {
            var layout = OriginScreenLayout.forViewport(viewport[0], viewport[1]);
            assertTrue(layout.left() >= 0);
            assertTrue(layout.top() >= 0);
            assertTrue(layout.left() + layout.width() <= viewport[0]);
            assertTrue(layout.top() + layout.height() <= viewport[1]);
            assertTrue(layout.firstY() + 20 < layout.secondY() + 20);
            assertTrue(layout.secondY() + 20 < layout.labelY());
            assertTrue(layout.secondDescriptionY() + 10 <= layout.footerY());
            assertTrue(layout.statusY() + 10 <= layout.footerY());
            assertTrue(layout.footerY() + 20 < layout.top() + layout.height());
            assertTrue(layout.raceX() + layout.columnWidth() < layout.classX());
            assertTrue(layout.classX() + layout.columnWidth() < layout.left() + layout.width());
        }
    }
}
