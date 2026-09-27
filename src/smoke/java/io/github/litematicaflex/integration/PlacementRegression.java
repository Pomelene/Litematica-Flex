package io.github.litematicaflex.integration;

import com.google.gson.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.lang.reflect.*;
import java.util.Arrays;

/** Executes the required-item bridge injected into both real Litematica implementations. */
final class PlacementRegression {
    static void run() throws Exception {
        var gson=new GsonBuilder().setExclusionStrategies(new ExclusionStrategy(){
            @Override public boolean shouldSkipField(FieldAttributes field){return true;}
            @Override public boolean shouldSkipClass(Class<?> type){return false;}
        }).create();
        var world=gson.fromJson("{}",FixedWorld.class);world.state=Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        Field worldField=SchematicWorldHandler.class.getDeclaredField("world");worldField.setAccessible(true);
        Object previous=worldField.get(SchematicWorldHandler.INSTANCE);worldField.set(SchematicWorldHandler.INSTANCE,world);
        try {
            var cache=MaterialCache.getInstance();cache.clearCache();
            var brick=Blocks.STONE_BRICK_SLAB.defaultBlockState();
            for(Class<?> target:new Class<?>[]{fi.dy.masa.litematica.util.WorldUtils.class,fi.dy.masa.litematica.util.EasyPlaceUtils.class}) {
                for(String hook:new String[]{"flex$target","flex$completed","flex$requiredItem","flex$pick"}) {
                    if(Arrays.stream(target.getDeclaredMethods()).noneMatch(m -> m.getName().contains(hook)))throw new IllegalStateException("Missing "+hook+" in "+target.getName());
                }
                Method item=Arrays.stream(target.getDeclaredMethods()).filter(m -> m.getName().contains("flex$requiredItem") && m.getParameterCount()==5 && m.getParameterTypes()[0]==MaterialCache.class).findFirst().orElseThrow();item.setAccessible(true);
                int[] originalCalls={0};
                Operation<ItemStack> original=args -> {originalCalls[0]++;return new ItemStack(Blocks.SMOOTH_STONE_SLAB);};
                var selected=(ItemStack)item.invoke(null,cache,brick,world,BlockPos.ZERO,original);
                if(!selected.is(Blocks.STONE_BRICK_SLAB.asItem()) || originalCalls[0]!=0)throw new IllegalStateException("Replacement build item bridge failed: "+target.getName());
                selected=(ItemStack)item.invoke(null,cache,world.state,world,BlockPos.ZERO,original);
                if(!selected.is(Blocks.SMOOTH_STONE_SLAB.asItem()) || originalCalls[0]!=1)throw new IllegalStateException("Original-item lookup was not preserved: "+target.getName());
                System.out.println("Flex actual injected easy-place item bridge passed: "+target.getSimpleName());
            }
        } finally {worldField.set(SchematicWorldHandler.INSTANCE,previous);}
    }
    private static final class FixedWorld extends WorldSchematic {
        private BlockState state;
        private FixedWorld(int unused){super(null,null,null,null);}
        @Override public BlockState getBlockState(BlockPos pos){return state;}
    }
}
