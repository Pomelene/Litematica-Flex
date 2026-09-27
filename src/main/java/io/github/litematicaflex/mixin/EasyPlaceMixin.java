package io.github.litematicaflex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.litematicaflex.runtime.PlacementHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=fi.dy.masa.litematica.util.EasyPlaceUtils.class,remap=false)
public abstract class EasyPlaceMixin {
    @WrapOperation(method={"handleEasyPlace","placementRestrictionInEffect"},at=@At(value="INVOKE",
        target="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState flex$target(Level world,BlockPos pos,Operation<BlockState> original) {
        return PlacementHooks.target(world,pos,original.call(world,pos));
    }
    @WrapOperation(method="handleEasyPlace",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState flex$completed(net.minecraft.client.multiplayer.ClientLevel world,BlockPos pos,Operation<BlockState> original) {
        return PlacementHooks.completed(pos,original.call(world,pos));
    }
    @WrapOperation(method={"handleEasyPlace","placementRestrictionInEffect"},at=@At(value="INVOKE",
        target="Lfi/dy/masa/litematica/materials/MaterialCache;getRequiredBuildItemForState(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack flex$requiredItem(fi.dy.masa.litematica.materials.MaterialCache cache,BlockState selected,Level world,BlockPos pos,Operation<ItemStack> original) {
        return PlacementHooks.requiredItem(cache,selected,pos,() -> original.call(cache,selected,world,pos));
    }
    @WrapOperation(method="handleEasyPlace",at=@At(value="INVOKE",
        target="Lfi/dy/masa/litematica/util/InventoryUtils;schematicWorldPickBlock(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/client/Minecraft;)V"))
    private static void flex$pick(ItemStack stack,BlockPos pos,Level world,net.minecraft.client.Minecraft mc,Operation<Void> original) {
        PlacementHooks.pick(stack,pos,mc,() -> original.call(stack,pos,world,mc));
    }
}
