package io.github.litematicaflex.gui;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.widgets.*;

/** Keeps MaLiLib controls and disables both value and reset buttons for overridden category rows. */
final class FlexOptionList extends WidgetListConfigOptions {
    static final class LockedBoolean extends ConfigBoolean {
        LockedBoolean(String name,boolean value) {
            this(name,value,"当前处于全部替换模式、总开关关闭或严格复核中，此种类项暂不生效；原设置保留。");
        }
        LockedBoolean(String name,boolean value,String explanation) {
            super(name,value,FlexText.tr(explanation));
            setPrettyName("§8"+FlexText.tr(name));
        }
    }
    FlexOptionList(int x,int y,int width,int height,int configWidth,boolean keySearch,GuiConfigsBase gui) {
        super(x,y,width,height,configWidth,0,keySearch,gui);
    }
    @Override protected WidgetConfigOption createListEntryWidget(int x,int y,int index,boolean selected,GuiConfigsBase.ConfigOptionWrapper wrapper) {
        return new LockedRow(x,y,browserEntryWidth,browserEntryHeight,maxLabelWidth,configWidth,wrapper,index,parent,this);
    }
    private static final class LockedRow extends WidgetConfigOption {
        private final boolean locked;
        LockedRow(int x,int y,int width,int height,int labelWidth,int configWidth,GuiConfigsBase.ConfigOptionWrapper wrapper,int index,GuiConfigsBase gui,WidgetListConfigOptionsBase<?,?> list) {
            super(x,y,width,height,labelWidth,configWidth,wrapper,index,gui,list);
            locked=wrapper.getConfig() instanceof LockedBoolean;
            if(locked)for(var widget:subWidgets)if(widget instanceof ButtonBase button)button.setEnabled(false);
        }
        @Override public boolean onMouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean doubleClick) {
            return !locked && super.onMouseClicked(event,doubleClick);
        }
    }
}
