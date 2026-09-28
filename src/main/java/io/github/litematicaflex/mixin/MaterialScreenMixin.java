package io.github.litematicaflex.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import io.github.litematicaflex.gui.FlexText;
import io.github.litematicaflex.gui.GroupedMaterialList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=GuiMaterialList.class,remap=false)
public abstract class MaterialScreenMixin extends GuiListBase<MaterialListEntry,WidgetMaterialListEntry,WidgetListMaterialList> {
    @Unique private boolean flex$grouped;
    protected MaterialScreenMixin(){super(10,44);}

    @Inject(method="createListWidget",at=@At("HEAD"),cancellable=true)
    private void flex$createGroupedTable(int x,int y,CallbackInfoReturnable<WidgetListMaterialList> cir) {
        if(flex$grouped)cir.setReturnValue(new GroupedMaterialList(x,y,getBrowserWidth(),getBrowserHeight(),
            (GuiMaterialList)(Object)this));
    }

    @ModifyReturnValue(method="getBrowserHeight",at=@At("RETURN"))
    private int flex$reserveButtonRow(int height){return Math.max(40,height-22);}

    @Inject(method="initGui",at=@At("TAIL"))
    private void flex$addAlternativeButton(CallbackInfo ci) {
        addButton(new ButtonGeneric(10,getScreenHeight()-56,148,18,
            FlexText.tr(flex$grouped?"替代模式：开":"替代模式：关")),
            (button,mouse) -> {flex$grouped=!flex$grouped;initGui();});
        if(flex$grouped)addButton(new ButtonGeneric(162,getScreenHeight()-56,80,18,FlexText.tr("刷新库存")),
            (button,mouse) -> initGui());
    }
}
