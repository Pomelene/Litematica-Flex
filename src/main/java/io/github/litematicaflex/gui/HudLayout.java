package io.github.litematicaflex.gui;

import io.github.litematicaflex.config.HudPosition;

/** Computes an anchored position after the final translated text has been measured. */
public final class HudLayout {
    public record Point(int x,int y) {}
    private static final int HOTBAR_CLEARANCE = 45;
    private HudLayout() {}

    public static Point origin(HudPosition position,int screenWidth,int screenHeight,int textWidth,int lineCount,int marginX,int marginY) {
        int height=Math.max(0,lineCount)*12;
        int x=position.right()?screenWidth-marginX-textWidth:marginX;
        int y=position.bottom()?screenHeight-HOTBAR_CLEARANCE-marginY-height:marginY;
        return new Point(Math.clamp(x,0,Math.max(0,screenWidth-textWidth)),
            Math.clamp(y,0,Math.max(0,screenHeight-height)));
    }
}
