package io.github.litematicaflex.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import io.github.litematicaflex.gui.FlexText;

/** Screen-edge anchors for the small in-world Flex status overlay. */
public enum HudPosition implements IConfigOptionListEntry {
    TOP_LEFT("左上角"), TOP_RIGHT("右上角"), BOTTOM_LEFT("左下角"), BOTTOM_RIGHT("右下角"), OFF("关闭");

    private final String label;
    HudPosition(String label) { this.label = label; }

    public boolean right() { return this == TOP_RIGHT || this == BOTTOM_RIGHT; }
    public boolean bottom() { return this == BOTTOM_LEFT || this == BOTTOM_RIGHT; }

    @Override public String getStringValue() { return name(); }
    @Override public String getDisplayName() { return FlexText.tr(label); }
    @Override public IConfigOptionListEntry cycle(boolean forward) {
        return values()[Math.floorMod(ordinal() + (forward ? 1 : -1), values().length)];
    }
    @Override public IConfigOptionListEntry fromString(String value) {
        try { return valueOf(value); }
        catch (IllegalArgumentException | NullPointerException ignored) { return TOP_LEFT; }
    }
}
