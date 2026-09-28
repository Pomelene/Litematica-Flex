package io.github.litematicaflex.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import io.github.litematicaflex.runtime.FlexRuntime;
import io.github.litematicaflex.runtime.MaterialPlanner;
import io.github.litematicaflex.runtime.ReplacementResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Read-only alternative view; never changes Litematica's list entries or availability counts. */
public final class FlexMaterialScreen extends FlexResultList {
    private final GuiMaterialList original;
    private final MaterialListBase materials;
    private List<MaterialPlanner.Row> rows=List.of();
    private final Map<String,String> names=new HashMap<>();

    public FlexMaterialScreen(GuiMaterialList original,MaterialListBase materials) {
        this.original=original;this.materials=materials;
        this.title=FlexText.tr("显示替代模式");
        refresh();
    }

    private static String id(ItemStack stack) {
        if(stack.getItem() instanceof BlockItem block)return BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
        return "item@"+BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private void refresh() {
        names.clear();
        var demands=new ArrayList<MaterialPlanner.Demand>();
        int multiplier=materials.getMultiplier();
        for(var entry:materials.getMaterialsAll()) {
            if(entry.getStack().isEmpty())continue;
            String id=id(entry.getStack());names.putIfAbsent(id,entry.getStack().getHoverName().getString());
            int needed=multiplier==1?entry.getCountMissing():entry.getCountTotal()*multiplier;
            if(needed>0)demands.add(new MaterialPlanner.Demand(id,needed));
        }
        var counts=new LinkedHashMap<String,Integer>();
        var player=Minecraft.getInstance().player;
        if(player!=null) {
            MaterialListUtils.getInventoryItemCounts(player.getInventory()).forEach((type,count) -> {
                var stack=type.getStack();String id=id(stack);counts.merge(id,count,Integer::sum);
                names.putIfAbsent(id,stack.getHoverName().getString());
            });
            var ender=Registry.ENTITY_DATA_REGISTRY.chestTracker().getEnderCache();
            if(Configs.Generic.MATERIAL_LIST_COUNT_ENDER_CACHE.getBooleanValue() && ender!=null) {
                var inventory=ender.toInventory(NbtInventory.DEFAULT_SIZE);
                if(inventory!=null)MaterialListUtils.getInventoryItemCounts(inventory).forEach((type,count) -> {
                    var stack=type.getStack();String id=id(stack);counts.merge(id,count,Integer::sum);
                    names.putIfAbsent(id,stack.getHoverName().getString());
                });
            }
        }
        // Keep any extra availability supplied to Litematica by another mod for original items.
        // Such sources cannot be enumerated as new substitute types without a provider API.
        for(var entry:materials.getMaterialsAll())if(!entry.getStack().isEmpty()) {
            String id=id(entry.getStack());
            counts.merge(id,entry.getCountAvailable(),Math::max);
        }
        var supplies=counts.entrySet().stream().map(e -> new MaterialPlanner.Supply(e.getKey(),e.getValue())).toList();
        rows=MaterialPlanner.plan(demands,supplies,this::accepted);
        setRows(rows.stream().map(row -> {
            String assigned=row.assigned().entrySet().stream()
                .map(e -> names.getOrDefault(e.getKey(),e.getKey())+" ×"+e.getValue())
                .reduce((a,b)->a+", "+b).orElse(FlexText.tr("背包内没有合规材料"));
            Identifier blockId=Identifier.tryParse(row.expected());
            ItemStack icon=blockId!=null && BuiltInRegistries.BLOCK.containsKey(blockId)
                ?new ItemStack(BuiltInRegistries.BLOCK.getValue(blockId).asItem()):ItemStack.EMPTY;
            return new Row(names.getOrDefault(row.expected(),row.expected()),assigned,
                row.covered()+" / "+row.needed(),row.covered()>=row.needed()?0xFF80E0A0:0xFFFFC070,icon);
        }).toList());
    }

    private boolean accepted(String expectedId,String candidateId) {
        if(expectedId.startsWith("item@") || candidateId.startsWith("item@"))return false;
        Identifier a=Identifier.tryParse(expectedId),b=Identifier.tryParse(candidateId);
        if(a==null || b==null || !BuiltInRegistries.BLOCK.containsKey(a) || !BuiltInRegistries.BLOCK.containsKey(b))return false;
        var expected=BuiltInRegistries.BLOCK.getValue(a).defaultBlockState();
        var candidate=ReplacementResolver.transfer(expected,BuiltInRegistries.BLOCK.getValue(b).defaultBlockState());
        return FlexRuntime.matchWithProfile(expected,candidate,FlexRuntime.profile(null,null),FlexRuntime.Channel.MATERIALS).accepted();
    }

    @Override public void initGui() {
        super.initGui();
        int height=getScreenHeight();
        addLabel(10,27,getScreenWidth()-20,12,0xFFB0B0B0,FlexText.tr("只读估算：每件物品仅分配一次；状态和放置协议仍由校验器判断。"));
        addButton(new ButtonGeneric(10,height-25,105,20,FlexText.tr("返回原材料清单")),(button,mouse)->GuiBase.openGui(original));
        addButton(new ButtonGeneric(119,height-25,65,20,FlexText.tr("刷新库存")),(button,mouse)->{refresh();initGui();});
    }
}
