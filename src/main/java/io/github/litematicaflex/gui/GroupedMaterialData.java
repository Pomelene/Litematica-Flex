package io.github.litematicaflex.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.nbt.NbtInventory;
import io.github.litematicaflex.mixin.MaterialListAccess;
import io.github.litematicaflex.runtime.FlexRuntime;
import io.github.litematicaflex.runtime.MaterialGroupPlanner;
import io.github.litematicaflex.runtime.MaterialPlanner;
import io.github.litematicaflex.runtime.ReplacementResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Read-only adapter for Litematica's own material table. */
final class GroupedMaterialData {
    record Row(MaterialListEntry entry,String title,String detail,String tooltip) {}
    private GroupedMaterialData() {}

    static List<Row> build(MaterialListBase materials) {
        var entries=materials.getMaterialsAll().stream()
            .filter(entry -> !((MaterialListAccess)materials).flex$ignoredEntries().contains(entry))
            .filter(entry -> !entry.getStack().isEmpty()).toList();
        var names=new LinkedHashMap<String,String>();
        var stacks=new LinkedHashMap<String,ItemStack>();
        var demands=new ArrayList<MaterialGroupPlanner.Demand>();
        var ordinary=new ArrayList<Row>();
        int multiplier=materials.getMultiplier();
        for(var entry:entries) {
            ItemStack stack=entry.getStack();
            if(!(stack.getItem() instanceof BlockItem block)) {
                int total=saturatedMultiply(entry.getCountTotal(),multiplier);
                int missing=multiplier==1?entry.getCountMissing():total;
                if(!materials.getHideAvailable() || entry.getCountAvailable()<missing) {
                    ordinary.add(new Row(new MaterialListEntry(stack,total,missing,entry.getCountMismatched(),entry.getCountAvailable()),
                        stack.getHoverName().getString(),"",stack.getHoverName().getString()));
                }
                continue;
            }
            String id=BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
            names.putIfAbsent(id,stack.getHoverName().getString());
            stacks.putIfAbsent(id,stack);
            int total=saturatedMultiply(entry.getCountTotal(),multiplier);
            int missing=multiplier==1?entry.getCountMissing():total;
            demands.add(new MaterialGroupPlanner.Demand(id,total,missing,entry.getCountMismatched()));
        }
        var counts=new LinkedHashMap<String,Integer>();
        var player=Minecraft.getInstance().player;
        if(player!=null) {
            MaterialListUtils.getInventoryItemCounts(player.getInventory()).forEach((type,count)->addStock(type.getStack(),count,counts,names));
            var ender=Registry.ENTITY_DATA_REGISTRY.chestTracker().getEnderCache();
            if(Configs.Generic.MATERIAL_LIST_COUNT_ENDER_CACHE.getBooleanValue() && ender!=null) {
                var inventory=ender.toInventory(NbtInventory.DEFAULT_SIZE);
                if(inventory!=null)MaterialListUtils.getInventoryItemCounts(inventory)
                    .forEach((type,count)->addStock(type.getStack(),count,counts,names));
            }
        }
        // Preserve external providers' higher original-item counts without rewriting Litematica's data.
        for(var entry:entries)if(entry.getStack().getItem() instanceof BlockItem block) {
            String id=BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
            counts.merge(id,entry.getCountAvailable(),Math::max);
        }
        var supplies=counts.entrySet().stream().map(e->new MaterialPlanner.Supply(e.getKey(),e.getValue())).toList();
        var groups=MaterialGroupPlanner.plan(demands,supplies,GroupedMaterialData::accepted);
        var rows=new ArrayList<Row>();
        for(var group:groups) {
            String first=group.members().getFirst();
            String title=names.getOrDefault(first,first);
            if(group.members().size()>1)title=I18n.get("litematica_flex.grouped_name",title,group.members().size()-1);
            var required=group.members().stream().map(id->names.getOrDefault(id,id)).toList();
            var assigned=group.assigned().entrySet().stream()
                .map(e->names.getOrDefault(e.getKey(),e.getKey())+" ×"+e.getValue()).toList();
            String detail=I18n.get("litematica_flex.grouped_members",String.join(", ",required));
            String tooltip=detail+"\n"+String.join(", ",group.members())+"\n"+I18n.get("litematica_flex.grouped_stock",
                assigned.isEmpty()?FlexText.tr("背包内没有合规材料"):String.join(", ",assigned));
            MaterialListEntry display=new MaterialListEntry(stacks.get(first),group.total(),group.missing(),group.mismatched(),group.covered());
            if(!materials.getHideAvailable() || group.covered()<group.missing())rows.add(new Row(display,title,detail,tooltip));
        }
        rows.addAll(ordinary);
        return List.copyOf(rows);
    }

    private static int saturatedMultiply(int left,int right) {
        return (int)Math.min(Integer.MAX_VALUE,(long)left*right);
    }
    private static void addStock(ItemStack stack,int count,Map<String,Integer> counts,Map<String,String> names) {
        if(stack.getItem() instanceof BlockItem block && count>0) {
            String id=BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
            counts.merge(id,count,Integer::sum);
            names.putIfAbsent(id,stack.getHoverName().getString());
        }
    }
    private static boolean accepted(String expectedId,String candidateId) {
        Identifier a=Identifier.tryParse(expectedId),b=Identifier.tryParse(candidateId);
        if(a==null || b==null || !BuiltInRegistries.BLOCK.containsKey(a) || !BuiltInRegistries.BLOCK.containsKey(b))return false;
        var expected=BuiltInRegistries.BLOCK.getValue(a).defaultBlockState();
        var candidate=ReplacementResolver.transfer(expected,BuiltInRegistries.BLOCK.getValue(b).defaultBlockState());
        return FlexRuntime.matchWithProfile(expected,candidate,FlexRuntime.profile(null,null),FlexRuntime.Channel.MATERIALS).accepted();
    }
}
