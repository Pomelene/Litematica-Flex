package io.github.litematicaflex.gui;

import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.litematicaflex.api.VerificationSummary;
import io.github.litematicaflex.runtime.FlexRuntime;
import io.github.litematicaflex.runtime.ReplacementResolver;
import io.github.litematicaflex.config.HudPosition;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayList;

/** Lightweight status and match explanation, without changing the schematic's block information. */
public final class FlexHud implements IRenderer {
    private record Line(String text,int color) {}
    @Override public void onExtractGuiOverlayPost(GuiContext ctx,float partialTicks,ProfilerFiller profiler) {
        var mc=Minecraft.getInstance();
        var config=FlexRuntime.STORE.editable();
        HudPosition anchor=HudPosition.valueOf(config.hudPosition);
        if(mc.level==null || mc.gui.screen()!=null || anchor==HudPosition.OFF) return;
        var lines=new ArrayList<Line>(3);
        var placement=DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
        String key=placement==null?null:FlexRuntime.placementKey(placement);
        var position=mc.hitResult instanceof BlockHitResult hit?hit.getBlockPos():mc.player==null?null:mc.player.blockPosition();
        var profile=FlexRuntime.profile(position,key);
        String status=!config.global.enabled || !profile.enabled?"关闭":
            profile.strictReview || config.global.strictReview?"严格复核":profile.allReplacements?"全部种类": "按种类 · "+profile.enabledGroups.size()+" 项";
        lines.add(new Line(FlexText.tr("Flex · "+status+" · "+FlexRuntime.profileSource(position,key)+" · Alt+F"),0xFFB0D8FF));
        if(placement!=null) {
            var verifier=placement.getSchematicVerifier();var summary=(VerificationSummary)verifier;
            if(!verifier.isActive() && !verifier.isFinished() && !verifier.isPaused())
                lines.add(new Line(FlexText.tr("统计未开始：请在 Litematica 校验器中启动校验"),0xFFD0D0D0));
            else lines.add(new Line(FlexText.tr("严格一致 "+summary.flexExactCount()+" / 替代合格 "+summary.flexSubstitutionCount()+" / 临时占位 "+summary.flexTemporaryCount()),0xFFD0D0D0));
        }
        var schematic=SchematicWorldHandler.getSchematicWorld();
        if(schematic!=null && mc.hitResult instanceof BlockHitResult hit) {
            var pos=hit.getBlockPos();var expected=schematic.getBlockState(pos);var actual=mc.level.getBlockState(pos);
            var result=FlexRuntime.match(expected,actual,pos,null,FlexRuntime.Channel.VERIFICATION);
            if(result.accepted() && !result.exact()) {
                String reason=FlexRuntime.CATALOGUE.groups().stream().filter(g -> g.id().equals(result.reason())).map(FlexText::group).findFirst().orElse(result.reason());
                lines.add(new Line(FlexText.tr("接受替代：")+actual.getBlock().getName().getString()+" → "+expected.getBlock().getName().getString()+"（"+reason+"）",0xFF80E0A0));
            } else if(!result.accepted() && !expected.isAir()) {
                String reason=ReplacementResolver.diagnose(expected,pos);
                if(reason!=null)lines.add(new Line(FlexText.tr("轻松放置："+reason),0xFFFFC070));
            }
        }
        int screenWidth=mc.getWindow().getGuiScaledWidth(),screenHeight=mc.getWindow().getGuiScaledHeight();
        int maxWidth=Math.max(24,screenWidth-16);
        var font=ctx.fontRenderer();
        var visible=new ArrayList<Line>(lines.size());
        for(Line line:lines) {
            String text=line.text();
            if(font.width(text)>maxWidth)text=font.plainSubstrByWidth(text,Math.max(0,maxWidth-font.width("…")))+"…";
            visible.add(new Line(text,line.color()));
        }
        int textWidth=visible.stream().mapToInt(line -> font.width(line.text())).max().orElse(0);
        var origin=HudLayout.origin(anchor,screenWidth,screenHeight,textWidth,visible.size(),config.hudOffsetX,config.hudOffsetY);
        for(int i=0;i<visible.size();i++) {
            Line line=visible.get(i);
            ctx.drawString(font,line.text(),origin.x(),origin.y()+i*12,line.color(),true);
        }
    }
}
