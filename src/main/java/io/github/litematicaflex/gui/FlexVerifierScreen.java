package io.github.litematicaflex.gui;

import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import io.github.litematicaflex.api.VerificationSummary;
import io.github.litematicaflex.runtime.FlexRuntime;
import java.util.*;

/** Read-only explanation of Litematica's current verifier result. */
public final class FlexVerifierScreen extends FlexResultList {
    private final GuiSchematicVerifier original;
    private final SchematicVerifier verifier;

    public FlexVerifierScreen(GuiSchematicVerifier original,SchematicVerifier verifier) {
        this.original=original;this.verifier=verifier;
        this.title=FlexText.tr("Flex 校验明细");
    }

    @Override public void initGui() {
        refreshRows();
        super.initGui();
        int y=28,width=getScreenWidth()-20;
        if(!verifier.isActive() && !verifier.isPaused() && !verifier.isFinished()) {
            addLabel(10,y,width,12,0xFFFFC070,FlexText.tr("请先在 Litematica 校验器中启动校验。"));
        }
        addButton(new ButtonGeneric(10,getScreenHeight()-25,145,20,FlexText.tr("返回 Litematica 校验器")),
            (button,mouse)->GuiBase.openGui(original));
        addButton(new ButtonGeneric(159,getScreenHeight()-25,65,20,FlexText.tr("刷新结果")),
            (button,mouse)->initGui());
    }
    private void refreshRows() {
        var summary=(VerificationSummary)verifier;
        if(!verifier.isActive() && !verifier.isPaused() && !verifier.isFinished()) {setRows(List.of());return;}
        var rows=new ArrayList<Row>();
        rows.add(new Row(FlexText.tr("完全一致"),FlexText.tr("与原理图方块及状态一致"),""+summary.flexExactCount(),0xFF80E0A0));
        rows.add(new Row(FlexText.tr("合规替代"),FlexText.tr("符合当前全局替换规则"),""+summary.flexSubstitutionCount(),0xFF80E0A0));
        rows.add(new Row(FlexText.tr("临时占位"),FlexText.tr("已设置为临时材料"),""+summary.flexTemporaryCount(),0xFFFFC070));
        rows.add(new Row(FlexText.tr("替代材料状态不符"),FlexText.tr("材料合规，但朝向、上下位置或其他状态不合规"),""+summary.flexSubstitutionStateErrors(),0xFFFFC070));
        rows.add(new Row(FlexText.tr("其他状态不符"),FlexText.tr("原方块相同，但状态不合规"),""+Math.max(0,verifier.getMismatchedStates()-summary.flexSubstitutionStateErrors()),0xFFFFC070));
        rows.add(new Row(FlexText.tr("不合规方块"),FlexText.tr("当前规则不接受的方块"),""+verifier.getMismatchedBlocks(),0xFFFF8080));
        rows.add(new Row(FlexText.tr("缺失／多余"),FlexText.tr("未放置 / 多放置"),verifier.getMissingBlocks()+" / "+verifier.getExtraBlocks(),0xFFFF8080));
        for(var reason:new TreeMap<>(summary.flexReasons()).entrySet()) {
            String label=FlexRuntime.CATALOGUE.groups().stream().filter(g -> g.id().equals(reason.getKey()))
                .map(FlexText::group).findFirst().orElse(reason.getKey());
            rows.add(new Row(FlexText.tr(label),FlexText.tr("接受原因"),""+reason.getValue(),0xFFB0D8FF));
        }
        setRows(rows);
    }
}
