package io.github.litematicaflex.gui;

import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** The same scrollable, searchable row pattern used by Litematica's own browsers. */
abstract class FlexResultList extends GuiListBase<FlexResultList.Row, FlexResultList.Entry, FlexResultList.Browser> {
    record Row(String name, String detail, String amount, int color, ItemStack icon) {
        Row(String name, String detail, String amount, int color) { this(name, detail, amount, color, ItemStack.EMPTY); }
    }

    private List<Row> rows = List.of();
    protected FlexResultList() { super(10, 46); }
    protected void setRows(List<Row> rows) {
        this.rows = List.copyOf(rows);
    }
    @Override protected int getBrowserWidth() { return Math.max(80, getScreenWidth() - 20); }
    @Override protected int getBrowserHeight() { return Math.max(42, getScreenHeight() - 104); }
    @Override protected Browser createListWidget(int x, int y) { return new Browser(x, y, getBrowserWidth(), getBrowserHeight(), this); }

    static final class Browser extends WidgetListBase<Row, Entry> {
        private final FlexResultList screen;
        Browser(int x, int y, int width, int height, FlexResultList screen) {
            super(x, y, width, height, null);
            this.screen = screen;
            this.browserEntryHeight = 30;
            this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT);
            this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
        }
        @Override protected Collection<Row> getAllEntries() { return screen.rows; }
        @Override protected List<String> getEntryStringsForFilter(Row row) {
            return List.of(row.name().toLowerCase(Locale.ROOT), row.detail().toLowerCase(Locale.ROOT));
        }
        @Override protected Entry createListEntryWidget(int x, int y, int index, boolean odd, Row row) {
            return new Entry(x, y, browserEntryWidth, browserEntryHeight, odd, row, index);
        }
    }

    static final class Entry extends WidgetListEntryBase<Row> {
        private final boolean odd;
        Entry(int x, int y, int width, int height, boolean odd, Row row, int index) {
            super(x, y, width, height, row, index);
            this.odd = odd;
        }
        @Override public boolean canSelectAt(MouseButtonEvent click) { return false; }
        @Override public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
            RenderUtils.drawRect(ctx, x, y, width, height, isMouseOver(mouseX, mouseY) ? 0xA0707070 : odd ? 0xA0101010 : 0xA0303030);
            int left = x + 5;
            if (!entry.icon().isEmpty()) {
                RenderUtils.drawRect(ctx, left, y + 6, 16, 16, 0x20FFFFFF);
                ctx.renderItem(entry.icon(), left, y + 6);
                left += 21;
            }
            int amountX = Math.max(left + 70, x + width - 86);
            drawString(ctx, left, y + 5, 0xFFFFFFFF, fit(entry.name(), amountX - left - 5));
            drawString(ctx, amountX, y + 5, entry.color(), entry.amount());
            drawString(ctx, left, y + 18, 0xFFB0B0B0, fit(entry.detail(), x + width - left - 7));
            super.render(ctx, mouseX, mouseY, selected);
        }
        private static String fit(String value, int maxWidth) {
            if (StringUtils.getStringWidth(value) <= maxWidth) return value;
            int end = value.length();
            while (end > 0 && StringUtils.getStringWidth(value.substring(0, end) + "…") > maxWidth)
                end = value.offsetByCodePoints(end, -1);
            return value.substring(0, end) + "…";
        }
        @Override public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected) {
            if (isMouseOver(mouseX, mouseY)) RenderUtils.drawHoverText(ctx, mouseX, mouseY, List.of(entry.name(), entry.detail()));
        }
    }
}
