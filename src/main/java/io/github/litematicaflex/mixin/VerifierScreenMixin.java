package io.github.litematicaflex.mixin;

import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicVerificationResults;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import io.github.litematicaflex.gui.FlexText;
import io.github.litematicaflex.gui.FlexVerifierScreen;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=GuiSchematicVerifier.class,remap=false)
public abstract class VerifierScreenMixin extends GuiListBase<BlockMismatchEntry,WidgetSchematicVerificationResult,WidgetListSchematicVerificationResults> {
    @Shadow @Final private SchematicVerifier verifier;
    protected VerifierScreenMixin(){super(10,60);}

    @ModifyReturnValue(method="getBrowserHeight",at=@At("RETURN"))
    private int flex$reserveButtonRow(int height){return Math.max(40,height-22);}

    @Inject(method="initGui",at=@At("TAIL"))
    private void flex$addDetailButton(CallbackInfo ci) {
        addButton(new ButtonGeneric(10,getScreenHeight()-56,120,18,FlexText.tr("Flex 校验明细")),
            (button,mouse)->GuiBase.openGui(new FlexVerifierScreen((GuiSchematicVerifier)(Object)this,verifier)));
    }
}
