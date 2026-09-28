package io.github.litematicaflex.integration;

import com.google.gson.*;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.render.schematic.ChunkCacheSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.render.IWorldSchematicRenderer;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import java.lang.reflect.*;
import java.util.Arrays;

/** Runs the transformed render read hook against empty chunk caches without GPU or client worlds. */
final class RendererRegression {
    private RendererRegression() {}
    static void run() throws ReflectiveOperationException {
        var type=ChunkRendererSchematicVbo.class;
        var constructor=type.getDeclaredConstructor(WorldSchematic.class,IWorldSchematicRenderer.class);
        constructor.setAccessible(true);
        Object renderer=constructor.newInstance(null,null);
        // Cache constructors require live worlds; these empty fixtures only exercise getBlockState.
        var gson=new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override public boolean shouldSkipField(FieldAttributes field){return true;}
            @Override public boolean shouldSkipClass(Class<?> type){return false;}
        }).create();
        var client=gson.fromJson("{}",ChunkCacheSchematic.class);
        var schematic=gson.fromJson("{}",ChunkCacheSchematic.class);
        Field chunks=ChunkCacheSchematic.class.getDeclaredField("chunkArray");chunks.setAccessible(true);
        chunks.set(client,new LevelChunk[0][0]);chunks.set(schematic,new LevelChunk[0][0]);
        Field clientView=type.getDeclaredField("clientWorldView");clientView.setAccessible(true);clientView.set(renderer,client);
        Field schematicView=type.getDeclaredField("schematicWorldView");schematicView.setAccessible(true);schematicView.set(renderer,schematic);
        Method read=Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().contains("flex$renderState")).findFirst().orElseThrow();
        read.setAccessible(true);
        Operation<BlockState> original=args -> Blocks.STONE.defaultBlockState();
        Object result=read.invoke(renderer,client,BlockPos.ZERO,original);
        if(result!=Blocks.STONE.defaultBlockState())throw new IllegalStateException("Unexpected empty-world substitution");
        var profile=new io.github.litematicaflex.config.RuleProfile();profile.enabledGroups.add("color.wool");
        var catalogue=io.github.litematicaflex.runtime.FlexRuntime.CATALOGUE;
        var white=catalogue.describe(Blocks.WOOL.white().defaultBlockState());
        var red=catalogue.describe(Blocks.WOOL.red().defaultBlockState());
        var matched=new io.github.litematicaflex.rules.RuleEngine().compare(white,red,profile);
        if(!matched.accepted())throw new IllegalStateException("Real wool states did not match: "+white+" / "+red);
        System.out.println("Flex real-state wool substitution matched: "+matched.reason());
        var verifier=new fi.dy.masa.litematica.schematic.verifier.SchematicVerifier();
        Method reset=verifier.getClass().getDeclaredMethod("clearData");reset.setAccessible(true);reset.invoke(verifier);
        var smooth=Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        var brick=Blocks.STONE_BRICK_SLAB.defaultBlockState();
        profile.enabledGroups.clear();profile.enabledGroups.add("stone.stone.slab");profile.enabledGroups.add("stone.stone_brick.slab");
        var engine=new io.github.litematicaflex.rules.RuleEngine();
        if(engine.compare(catalogue.describe(smooth),catalogue.describe(brick),profile).accepted())throw new IllegalStateException("Separate stone families leaked");
        profile.enabledGroups.add("stone.all");
        Field snapshot=io.github.litematicaflex.runtime.FlexRuntime.class.getDeclaredField("snapshot");snapshot.setAccessible(true);
        snapshot.set(null,new io.github.litematicaflex.runtime.ProfileSnapshot(profile.copy()));
        for(var channel:io.github.litematicaflex.runtime.FlexRuntime.Channel.values()) {
            if(!io.github.litematicaflex.runtime.FlexRuntime.match(smooth,brick,BlockPos.ZERO,null,channel).accepted())throw new IllegalStateException("Slab substitution rejected in "+channel);
        }
        var fixedCache=gson.fromJson("{}",FixedCache.class);fixedCache.state=smooth;
        schematicView.set(renderer,fixedCache);
        Operation<BlockState> brickRead=args -> brick;
        if(read.invoke(renderer,client,BlockPos.ZERO,brickRead)!=smooth)throw new IllegalStateException("Accepted slab still rendered as mismatched");
        // Item prototypes are normally bound during resource loading, which this headless fixture skips.
        Blocks.STONE_BRICK_SLAB.asItem().builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
        Blocks.SMOOTH_STONE_SLAB.asItem().builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.EMPTY);
        var stack=new net.minecraft.world.item.ItemStack(Blocks.STONE_BRICK_SLAB);
        var selected=io.github.litematicaflex.runtime.ReplacementResolver.selectFromStacks(smooth,BlockPos.ZERO,profile,java.util.List.of(stack),java.util.List.of(stack));
        if(selected!=brick)throw new IllegalStateException("Held stone brick slab was not selected");
        selected=io.github.litematicaflex.runtime.ReplacementResolver.selectFromStacks(smooth,BlockPos.ZERO,profile,java.util.List.of(),java.util.List.of(stack));
        if(selected!=brick)throw new IllegalStateException("Inventory stone brick slab was not selected");
        var placement=gson.fromJson("{}",fi.dy.masa.litematica.schematic.placement.SchematicPlacement.class);
        Field origin=placement.getClass().getDeclaredField("origin");origin.setAccessible(true);origin.set(placement,BlockPos.ZERO);
        Field placementField=verifier.getClass().getDeclaredField("schematicPlacement");placementField.setAccessible(true);placementField.set(verifier,placement);
        Method check=verifier.getClass().getDeclaredMethod("checkBlockStates",int.class,int.class,int.class,BlockState.class,BlockState.class);check.setAccessible(true);
        check.invoke(verifier,0,0,0,smooth,brick);
        if(((io.github.litematicaflex.api.VerificationSummary)verifier).flexSubstitutionCount()!=1)throw new IllegalStateException("Verifier did not count accepted slab");
        var top=brick.setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP);
        if(engine.compare(catalogue.describe(smooth),catalogue.describe(top),profile).accepted())throw new IllegalStateException("Slab position constraint lost");
        if(engine.compare(catalogue.describe(smooth),catalogue.describe(Blocks.OAK_SLAB.defaultBlockState()),profile).accepted())throw new IllegalStateException("Stone aggregate accepted wood");
        check.invoke(verifier,1,0,0,smooth,top);
        if(verifier.getMismatchedStates()!=1)throw new IllegalStateException("Compatible slab state error not classified as WRONG_STATE");
        Method overlay=type.getDeclaredMethod("getOverlayType",BlockState.class,BlockState.class);overlay.setAccessible(true);
        io.github.litematicaflex.runtime.RenderMatchContext.position(BlockPos.ZERO);
        if(overlay.invoke(renderer,smooth,top)!=fi.dy.masa.litematica.util.OverlayType.WRONG_STATE)throw new IllegalStateException("Compatible slab state error not yellow");
        var woodProfile=new io.github.litematicaflex.config.RuleProfile();woodProfile.allReplacements=true;
        snapshot.set(null,new io.github.litematicaflex.runtime.ProfileSnapshot(woodProfile.copy()));
        var oakLog=Blocks.STRIPPED_OAK_LOG.defaultBlockState();
        var spruceLog=Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,net.minecraft.core.Direction.Axis.X);
        if(overlay.invoke(renderer,oakLog,spruceLog)!=fi.dy.masa.litematica.util.OverlayType.WRONG_STATE)throw new IllegalStateException("Stripped log axis mismatch not yellow");
        if(engine.compare(catalogue.describe(Blocks.OAK_PLANKS.defaultBlockState()),catalogue.describe(oakLog),woodProfile).accepted())throw new IllegalStateException("Planks and log matched by default");
        var hotbar=java.util.List.of(stack,net.minecraft.world.item.ItemStack.EMPTY);
        if(io.github.litematicaflex.runtime.ReplacementResolver.safeHotbarSlot(hotbar,0)!=1)throw new IllegalStateException("Inventory swap did not prefer empty slot");
        // Replace the published rules after a successful normalized read: old acceptance must not survive.
        woodProfile.enabled=false;
        snapshot.set(null,new io.github.litematicaflex.runtime.ProfileSnapshot(woodProfile.copy()));
        if(read.invoke(renderer,client,BlockPos.ZERO,brickRead)!=brick)throw new IllegalStateException("Disabled global mode retained normalized mesh input");
        if(io.github.litematicaflex.runtime.FlexRuntime.match(smooth,brick,BlockPos.ZERO,"test",io.github.litematicaflex.runtime.FlexRuntime.Channel.VERIFICATION).accepted())throw new IllegalStateException("Placement override bypassed global disable");
        woodProfile.enabled=true;woodProfile.allReplacements=true;woodProfile.blacklistBlocks.add("minecraft:stone_brick_slab");
        snapshot.set(null,new io.github.litematicaflex.runtime.ProfileSnapshot(woodProfile.copy()));
        for(var channel:io.github.litematicaflex.runtime.FlexRuntime.Channel.values()) {
            if(io.github.litematicaflex.runtime.FlexRuntime.match(smooth,brick,BlockPos.ZERO,"test",channel).accepted())throw new IllegalStateException("Placement bypassed global blacklist: "+channel);
        }
        if(read.invoke(renderer,client,BlockPos.ZERO,brickRead)!=brick)throw new IllegalStateException("Blacklisted slab still normalized for rendering");
        profile.blacklistBlocks.add("minecraft:stone_brick_slab");
        if(io.github.litematicaflex.runtime.ReplacementResolver.selectFromStacks(smooth,BlockPos.ZERO,profile,java.util.List.of(stack),java.util.List.of(stack))!=smooth)throw new IllegalStateException("Blacklisted inventory item selected");
        var migrated=new io.github.litematicaflex.config.RuleProfile();migrated.enabledGroups.add("wood.all");migrated.enabledGroups.add("stone.all");
        io.github.litematicaflex.runtime.FlexRuntime.normalizeGroups(migrated);
        if(migrated.enabledGroups.contains("wood.all")||!migrated.enabledGroups.contains("stone.shape.slab"))throw new IllegalStateException("Legacy material switch migration failed");
        if(engine.compare(catalogue.describe(Blocks.OAK_LOG.defaultBlockState()),catalogue.describe(Blocks.STRIPPED_OAK_LOG.defaultBlockState()),migrated).accepted())throw new IllegalStateException("Independent wood processing groups merged");
        if(!engine.compare(catalogue.describe(smooth),catalogue.describe(brick),migrated).accepted())throw new IllegalStateException("Stone slab aggregate lost after migration");
        System.out.println("Flex global disable, global blacklist priority, render/selection rejection and independent wood groups passed.");
        System.out.println("Flex stripped log axis and slab state errors classified as yellow; planks/log default separation passed.");
        System.out.println("Flex smooth stone slab -> stone brick slab: render normalization, held/inventory selection, verifier acceptance, shape/wood guards passed.");
        System.out.println("Flex runtime render hook and verifier reset executed successfully.");
    }

    private static final class FixedCache extends ChunkCacheSchematic {
        private BlockState state;
        private FixedCache(int unused){super(null,null,BlockPos.ZERO,0);}
        @Override public BlockState getBlockState(BlockPos pos){return state;}
    }
}
