package io.github.litematicaflex.gui;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;

/** Replaces only the visible material rows; Litematica retains the source list and controls. */
public final class GroupedMaterialList extends WidgetListMaterialList {
    private final MaterialListBase materials;
    private final Map<MaterialListEntry,GroupedMaterialData.Row> rows=new IdentityHashMap<>();

    public GroupedMaterialList(int x,int y,int width,int height,GuiMaterialList gui) {
        super(x,y,width,height,gui);
        this.materials=gui.getMaterialList();
    }

    @Override protected Collection<MaterialListEntry> getAllEntries() {
        rows.clear();
        var entries=new ArrayList<MaterialListEntry>();
        for(var row:GroupedMaterialData.build(materials)) {
            entries.add(row.entry());
            rows.put(row.entry(),row);
        }
        return entries;
    }

    @Override protected List<String> getEntryStringsForFilter(MaterialListEntry entry) {
        var row=rows.get(entry);
        if(row==null)return super.getEntryStringsForFilter(entry);
        return List.of(row.title().toLowerCase(Locale.ROOT),row.tooltip().toLowerCase(Locale.ROOT),
            BuiltInRegistries.ITEM.getKey(entry.getStack().getItem()).toString().toLowerCase(Locale.ROOT));
    }

    @Override protected WidgetMaterialListEntry createListEntryWidget(int x,int y,int index,boolean odd,MaterialListEntry entry) {
        return new GroupedEntry(x,y,browserEntryWidth,getBrowserEntryHeightFor(entry),odd,materials,entry,index,this,rows.get(entry));
    }

    private static final class GroupedEntry extends WidgetMaterialListEntry {
        private final MaterialListEntry material;
        private final GroupedMaterialData.Row row;
        private final boolean odd;
        private final WidgetListMaterialList list;
        private final MaterialListBase materials;
        private final int xLeft,yTop,rowWidth,rowHeight;

        GroupedEntry(int x,int y,int width,int height,boolean odd,MaterialListBase materials,
                     MaterialListEntry material,int index,WidgetListMaterialList list,
                     GroupedMaterialData.Row row) {
            super(x,y,width,height,odd,materials,material,index,list);
            this.xLeft=x;this.yTop=y;this.rowWidth=width;this.rowHeight=height;
            this.odd=odd;this.material=material;this.row=row;this.list=list;this.materials=materials;
        }

        @Override protected boolean onMouseClickedImpl(MouseButtonEvent click,boolean doubleClick) {
            if(material!=null)return false; // Synthetic rows cannot trigger the original per-item Ignore action.
            int offset=(int)click.x()-xLeft;
            if(offset<0 || offset>=rowWidth)return false;
            int availableX=rowWidth-66;
            int neededX=availableX-69;
            int totalX=neededX-63;
            var sort=offset>=availableX?MaterialListBase.SortCriteria.COUNT_AVAILABLE:
                offset>=neededX?MaterialListBase.SortCriteria.COUNT_MISSING:
                offset>=totalX?MaterialListBase.SortCriteria.COUNT_TOTAL:MaterialListBase.SortCriteria.NAME;
            materials.setSortCriteria(sort);
            list.refreshEntries();
            return true;
        }

        @Override public void render(GuiContext ctx,int mouseX,int mouseY,boolean selected) {
            RenderUtils.drawRect(ctx,xLeft,yTop,rowWidth,rowHeight,
                isMouseOver(mouseX,mouseY) && material!=null ? 0xA0707070 : odd ? 0xA0101010 : 0xA0303030);
            if(list.getSearchBarWidget().isSearchOpen() && material==null)return;
            int availableX=xLeft+rowWidth-66;
            int neededX=availableX-69;
            int totalX=neededX-63;
            int nameWidth=Math.max(20,totalX-xLeft-28);
            int y=yTop+7;
            if(material==null) {
                drawString(ctx,xLeft+4,y,0xFFFFFFFF,fit(StringUtils.translate("litematica.gui.label.material_list.title.item"),nameWidth));
                drawString(ctx,totalX,y,0xFFFFFFFF,FlexText.tr("总数"));
                drawString(ctx,neededX,y,0xFFFFFFFF,FlexText.tr("待备"));
                drawString(ctx,availableX,y,0xFFFFFFFF,FlexText.tr("已备"));
                return;
            }
            RenderUtils.drawRect(ctx,xLeft+4,yTop+3,16,16,0x20FFFFFF);
            ctx.renderItem(material.getStack(),xLeft+4,yTop+3);
            drawString(ctx,xLeft+24,y,0xFFFFFFFF,fit(row.title(),nameWidth));
            drawString(ctx,totalX,y,0xFFFFFFFF,String.valueOf(material.getCountTotal()));
            drawString(ctx,neededX,y,0xFFFFFFFF,String.valueOf(material.getCountMissing()));
            int color=material.getCountAvailable()>=material.getCountMissing()?0xFF80E0A0:0xFFFF8080;
            drawString(ctx,availableX,y,color,String.valueOf(material.getCountAvailable()));
        }

        private static String fit(String value,int maxWidth) {
            if(StringUtils.getStringWidth(value)<=maxWidth)return value;
            int end=value.length();
            while(end>0 && StringUtils.getStringWidth(value.substring(0,end)+"…")>maxWidth)
                end=value.offsetByCodePoints(end,-1);
            return value.substring(0,end)+"…";
        }

        @Override public void postRenderHovered(GuiContext ctx,int mouseX,int mouseY,boolean selected) {
            if(row!=null && isMouseOver(mouseX,mouseY)) {
                var lines=new ArrayList<String>();
                lines.add(row.title());
                lines.addAll(Arrays.asList(row.tooltip().split("\\n")));
                RenderUtils.drawHoverText(ctx,mouseX,mouseY,lines);
            }
        }
    }
}
