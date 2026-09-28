package io.github.litematicaflex.gui;

import io.github.litematicaflex.config.HudPosition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudLayoutTest {
    @Test void everyCornerUsesMeasuredTextAndMargins() {
        assertEquals(new HudLayout.Point(8,8),HudLayout.origin(HudPosition.TOP_LEFT,320,240,100,3,8,8));
        assertEquals(new HudLayout.Point(212,8),HudLayout.origin(HudPosition.TOP_RIGHT,320,240,100,3,8,8));
        assertEquals(new HudLayout.Point(8,151),HudLayout.origin(HudPosition.BOTTOM_LEFT,320,240,100,3,8,8));
        assertEquals(new HudLayout.Point(212,151),HudLayout.origin(HudPosition.BOTTOM_RIGHT,320,240,100,3,8,8));
    }

    @Test void extremeMarginsRemainOnScreen() {
        var point=HudLayout.origin(HudPosition.BOTTOM_RIGHT,150,90,130,3,500,500);
        assertTrue(point.x()>=0 && point.x()+130<=150);
        assertTrue(point.y()>=0 && point.y()+36<=90);
    }
}
