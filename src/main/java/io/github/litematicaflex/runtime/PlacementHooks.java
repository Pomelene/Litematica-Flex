package io.github.litematicaflex.runtime;

import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Shared bridge for Litematica's legacy and rewritten easy-place paths. */
public final class PlacementHooks {
    private PlacementHooks() {}
    public static BlockState target(Level world,BlockPos pos,BlockState state) {
        return world==SchematicWorldHandler.getSchematicWorld()?ReplacementResolver.forPlacement(state,pos):state;
    }
    public static BlockState completed(BlockPos pos,BlockState actual) {
        var schematic=SchematicWorldHandler.getSchematicWorld();
        if(schematic==null)return actual;
        var expected=schematic.getBlockState(pos);
        return FlexRuntime.match(expected,actual,pos,null,FlexRuntime.Channel.PLACEMENT).accepted()
            ?ReplacementResolver.forPlacement(expected,pos):actual;
    }
    public static ItemStack requiredItem(MaterialCache cache,BlockState selected,BlockPos pos,Supplier<ItemStack> original) {
        var schematic=SchematicWorldHandler.getSchematicWorld();
        if(schematic!=null && selected.getBlock()!=schematic.getBlockState(pos).getBlock())return cache.getRequiredBuildItemForState(selected);
        return original.get();
    }
    public static void pick(ItemStack stack,BlockPos pos,Minecraft mc,Runnable original) {
        original.run();
        if(mc.player==null || mc.gameMode==null || mc.player.hasInfiniteMaterials() || stack.isEmpty())return;
        var schematic=SchematicWorldHandler.getSchematicWorld();
        if(schematic==null || stack.is(schematic.getBlockState(pos).getBlock().asItem()))return;
        if(!FlexRuntime.active(FlexRuntime.profile(pos,null),FlexRuntime.Channel.PLACEMENT))return;
        if(EntityUtils.getUsedHandForItem(mc.player,stack)!=null)return;
        var inv=mc.player.getInventory();int source=inv.findSlotMatchingItem(stack);
        if(source<0)return;
        if(source<9){inv.setSelectedSlot(source);return;}
        var hotbar=new ArrayList<ItemStack>();for(int i=0;i<9;i++)hotbar.add(inv.getItem(i));
        int target=ReplacementResolver.safeHotbarSlot(hotbar,inv.getSelectedSlot());
        if(target<0)return;
        inv.setSelectedSlot(target);
        fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(),mc);
    }
}
