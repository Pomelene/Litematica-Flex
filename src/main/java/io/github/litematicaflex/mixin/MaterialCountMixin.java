package io.github.litematicaflex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement;
import fi.dy.masa.litematica.world.WorldSchematic;
import io.github.litematicaflex.runtime.*;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksBase;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

@Mixin(value=TaskCountBlocksPlacement.class,remap=false)
public abstract class MaterialCountMixin extends TaskCountBlocksBase {
    @Shadow protected SchematicPlacement schematicPlacement;
    @Shadow protected boolean ignoreState;
    protected MaterialCountMixin(IMaterialList list,String name) { super(list,name); }

    @Inject(method="countAtPosition",at=@At("HEAD"),cancellable=true)
    private void flex$count(BlockPos pos,CallbackInfo ci) {
        var profile=FlexRuntime.profile(pos,FlexRuntime.placementKey(schematicPlacement));
        if (!FlexRuntime.active(profile,FlexRuntime.Channel.MATERIALS)) return;
        BlockState expected=schematicWorld.getBlockState(pos);
        if (!expected.isAir()) {
            BlockState actual=clientWorld.getBlockState(pos);
            BlockState planned=ReplacementResolver.fixed(expected,pos,profile);
            countsTotal.addTo(planned,1);
            boolean accepted=FlexRuntime.match(expected,actual,pos,FlexRuntime.placementKey(schematicPlacement),FlexRuntime.Channel.MATERIALS).accepted();
            boolean excluded=FlexRuntime.excluded(expected,actual,profile);
            if (!accepted && !excluded && ignoreState && expected.getBlock()==actual.getBlock()) accepted=true;
            if (!accepted && !excluded && fi.dy.masa.litematica.config.Configs.Visuals.IGNORE_CROP_AGE.getBooleanValue()) {
                accepted=fi.dy.masa.litematica.util.BlockUtils.areStatesEqualIgnoringAge(expected,actual);
            }
            if (!accepted) {
                countsMissing.addTo(planned,1);
                if (!actual.isAir()) countsMismatch.addTo(planned,1);
            }
        }
        ci.cancel();
    }
}
