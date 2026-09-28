package io.github.litematicaflex.mixin;

import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicVerificationResults;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicVerificationResult;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.gui.GuiListBase;
import io.github.litematicaflex.api.VerificationSummary;
import net.minecraft.client.resources.language.I18n;
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
    private void flex$addInlineSummary(CallbackInfo ci) {
        if(!verifier.isActive() && !verifier.isPaused() && !verifier.isFinished())return;
        var summary=(VerificationSummary)verifier;
        String line=I18n.get("litematica_flex.verifier_summary",summary.flexExactCount(),
            summary.flexSubstitutionCount(),summary.flexSubstitutionStateErrors());
        addLabel(12,getScreenHeight()-56,getScreenWidth()-24,12,0xFFB0D8FF,line);
    }
}
