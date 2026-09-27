package io.github.litematicaflex.gui;

import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.litematicaflex.api.VerificationSummary;
import io.github.litematicaflex.runtime.FlexRuntime;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.BlockHitResult;

/** Lightweight status and match explanation, without changing the schematic's block information. */
public final class FlexHud implements IRenderer {
    @Override public void onExtractGuiOverlayPost(GuiContext ctx,float partialTicks,ProfilerFiller profiler) {
        var mc=Minecraft.getInstance();
        if(mc.level==null || mc.gui.screen()!=null || !FlexRuntime.STORE.editable().showHud) return;
        var placement=DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        String key=placement==null?null:FlexRuntime.placementKey(placement);
        var position=mc.hitResult instanceof BlockHitResult hit?hit.getBlockPos():mc.player==null?null:mc.player.blockPosition();
        var profile=FlexRuntime.profile(position,key);
        String status=!FlexRuntime.STORE.editable().global.enabled || !profile.enabled?"关闭":
            profile.strictReview || FlexRuntime.STORE.editable().global.strictReview?"严格复核":profile.allReplacements?"全部种类": "按种类 · "+profile.enabledGroups.size()+" 项";
        ctx.drawString(ctx.fontRenderer(),"Flex · "+status+" · "+FlexRuntime.profileSource(position,key)+" · Alt+F",8,8,0xFFB0D8FF,true);
        if(placement!=null) {
            var verifier=placement.getSchematicVerifier();var summary=(VerificationSummary)verifier;
            if(!verifier.isActive() && !verifier.isFinished() && !verifier.isPaused())
                ctx.drawString(ctx.fontRenderer(),"统计未开始：请在 Litematica 校验器中启动校验",8,20,0xFFD0D0D0,true);
            else ctx.drawString(ctx.fontRenderer(),"严格一致 "+summary.flexExactCount()+" / 替代合格 "+summary.flexSubstitutionCount()+" / 临时占位 "+summary.flexTemporaryCount(),8,20,0xFFD0D0D0,true);
        }
        var schematic=SchematicWorldHandler.getSchematicWorld();
        if(schematic!=null && mc.hitResult instanceof BlockHitResult hit) {
            var pos=hit.getBlockPos();var expected=schematic.getBlockState(pos);var actual=mc.level.getBlockState(pos);
            var result=FlexRuntime.match(expected,actual,pos,null,FlexRuntime.Channel.VERIFICATION);
            if(result.accepted() && !result.exact()) {
                String reason=FlexRuntime.CATALOGUE.groups().stream().filter(g -> g.id().equals(result.reason())).map(g -> g.title()).findFirst().orElse(result.reason());
                ctx.drawString(ctx.fontRenderer(),"接受替代："+actual.getBlock().getName().getString()+" → "+expected.getBlock().getName().getString()+"（"+reason+"）",8,32,0xFF80E0A0,true);
            }
        }
    }
}
