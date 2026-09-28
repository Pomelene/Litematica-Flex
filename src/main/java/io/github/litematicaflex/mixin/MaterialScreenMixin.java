package io.github.litematicaflex.mixin;

import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import io.github.litematicaflex.gui.FlexMaterialScreen;
import io.github.litematicaflex.gui.FlexText;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=GuiMaterialList.class,remap=false)
public abstract class MaterialScreenMixin extends GuiListBase<MaterialListEntry,WidgetMaterialListEntry,WidgetListMaterialList> {
    @Shadow @Final private MaterialListBase materialList;
    protected MaterialScreenMixin(){super(10,44);}

    @ModifyReturnValue(method="getBrowserHeight",at=@At("RETURN"))
    private int flex$reserveButtonRow(int height){return Math.max(40,height-22);}

    @Inject(method="initGui",at=@At("TAIL"))
    private void flex$addAlternativeButton(CallbackInfo ci) {
        addButton(new ButtonGeneric(10,getScreenHeight()-56,128,18,FlexText.tr("显示替代模式")),
            (button,mouse) -> GuiBase.openGui(new FlexMaterialScreen((GuiMaterialList)(Object)this,materialList)));
    }
}
