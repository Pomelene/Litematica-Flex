package io.github.litematicaflex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.render.schematic.ChunkCacheSchematic;
import io.github.litematicaflex.runtime.FlexRuntime;
import io.github.litematicaflex.runtime.RenderMatchContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.util.OverlayType;
import fi.dy.masa.malilib.util.data.Color4f;
import io.github.litematicaflex.rules.MatchResult;

@Mixin(value=ChunkRendererSchematicVbo.class,remap=false)
public abstract class ChunkRendererMixin {
    @Shadow protected ChunkCacheSchematic schematicWorldView;
    @Shadow protected ChunkCacheSchematic clientWorldView;

    @Inject(method={"renderBlocksAndOverlay","renderOverlay"},at=@At("HEAD"))
    private void flex$beginRender(CallbackInfo ci){RenderMatchContext.clear();}

    @Inject(method={"renderBlocksAndOverlay","renderOverlay"},at=@At("RETURN"))
    private void flex$endRender(CallbackInfo ci){RenderMatchContext.clear();}

    /** Normalize only renderer-local reads, including neighbors; never modify either world. */
    @WrapOperation(method={"renderBlocksAndOverlay","renderOverlay"},at=@At(value="INVOKE",
            target="Lfi/dy/masa/litematica/render/schematic/ChunkCacheSchematic;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState flex$renderState(ChunkCacheSchematic view,BlockPos pos,Operation<BlockState> original) {
        BlockState actual = original.call(view,pos);
        if (view != clientWorldView) return actual;
        RenderMatchContext.position(pos);
        BlockState expected = schematicWorldView.getBlockState(pos);
        var result = FlexRuntime.match(expected,actual,pos,null,FlexRuntime.Channel.RENDERING);
        return result.accepted() && !FlexRuntime.profile(pos,null).showAcceptedOverlay ? expected : actual;
    }

    @Inject(method="getOverlayType",at=@At("HEAD"),cancellable=true)
    private void flex$acceptedOverlay(BlockState expected,BlockState actual,CallbackInfoReturnable<OverlayType> cir) {
        RenderMatchContext.overlay(null);
        var pos = RenderMatchContext.position();
        var result = FlexRuntime.match(expected,actual,pos,null,FlexRuntime.Channel.RENDERING);
        if (!result.accepted() && FlexRuntime.materialMatches(expected,actual,pos,null,FlexRuntime.Channel.RENDERING)) {
            cir.setReturnValue(OverlayType.WRONG_STATE);
            return;
        }
        if (result.accepted() && !result.exact() && FlexRuntime.profile(pos,null).showAcceptedOverlay) {
            RenderMatchContext.overlay(result);
            cir.setReturnValue(OverlayType.DIFF_BLOCK);
        }
    }

    @Inject(method="getOverlayColor",at=@At("HEAD"),cancellable=true)
    private static void flex$acceptedColor(OverlayType type,CallbackInfoReturnable<Color4f> cir) {
        var result = RenderMatchContext.overlay();
        if (type == OverlayType.DIFF_BLOCK && result != null) {
            cir.setReturnValue(result.reason().equals("temporary")
                    ? new Color4f(1f,0.65f,0.15f,0.35f) : new Color4f(0.2f,0.85f,0.5f,0.3f));
        }
    }
}
