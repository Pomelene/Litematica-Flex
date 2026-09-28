package io.github.litematicaflex.mixin;

import io.github.litematicaflex.api.VerificationSummary;
import io.github.litematicaflex.runtime.FlexRuntime;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.ItemUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(value=SchematicVerifier.class, remap=false)
public abstract class VerifierMixin implements VerificationSummary {
    @Shadow private SchematicPlacement schematicPlacement;
    @Shadow private ClientLevel worldClient;
    @Shadow private Object2IntOpenHashMap<BlockState> correctStateCounts;
    @Shadow private int correctStatesCount;
    @Shadow @Final private com.google.common.collect.ArrayListMultimap<org.apache.commons.lang3.tuple.Pair<BlockState,BlockState>,BlockPos> wrongStatesPositions;
    @Shadow @Final private it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap<BlockPos,SchematicVerifier.BlockMismatch> blockMismatches;
    @Shadow @Final private HashSet<org.apache.commons.lang3.tuple.Pair<BlockState,BlockState>> ignoredMismatches;
    @Shadow private int clientBlocks;
    @Shadow private Set<BlockPos> recheckQueue;
    @Unique private final Map<BlockPos,BlockState> flex$accepted = new HashMap<>();
    @Unique private final Set<BlockPos> flex$exact = new HashSet<>();
    @Unique private final Set<BlockPos> flex$temporary = new HashSet<>();
    @Unique private final Set<BlockPos> flex$stateErrors = new HashSet<>();
    @Unique private final Map<BlockPos,String> flex$reasons = new HashMap<>();

    @Inject(method="checkBlockStates",at=@At("HEAD"),cancellable=true)
    private void flex$compare(int x,int y,int z,BlockState expected,BlockState actual,CallbackInfo ci) {
        BlockPos pos = new BlockPos(x,y,z);
        flex$stateErrors.remove(pos);
        var result = FlexRuntime.match(expected,actual,pos,FlexRuntime.placementKey(schematicPlacement),FlexRuntime.Channel.VERIFICATION);
        if (!result.accepted() && FlexRuntime.materialMatches(expected,actual,pos,FlexRuntime.placementKey(schematicPlacement),FlexRuntime.Channel.VERIFICATION)) {
            if(expected.getBlock()!=actual.getBlock())flex$stateErrors.add(pos);
            var pair=org.apache.commons.lang3.tuple.Pair.of(expected,actual);
            if(!ignoredMismatches.contains(pair)) {
                wrongStatesPositions.put(pair,pos);
                blockMismatches.put(pos,new SchematicVerifier.BlockMismatch(SchematicVerifier.MismatchType.WRONG_STATE,expected,actual,1));
            }
            ci.cancel();
            return;
        }
        if (result.accepted() && !expected.isAir()) {
            flex$accepted.put(pos,actual);
            if (result.exact()) flex$exact.add(pos); else flex$exact.remove(pos);
            if (result.reason().equals("temporary")) flex$temporary.add(pos); else flex$temporary.remove(pos);
            if(result.exact())flex$reasons.remove(pos);else flex$reasons.put(pos,result.reason());
            if (!result.exact()) {
                ItemUtils.setItemForBlock(worldClient,pos,actual);
                correctStateCounts.addTo(actual,1);
                correctStatesCount++;
                ci.cancel();
            }
        }
    }

    @Inject(method="clearData",at=@At("HEAD"))
    private void flex$reset(CallbackInfo ci) { flex$accepted.clear(); flex$exact.clear(); flex$temporary.clear(); flex$stateErrors.clear(); flex$reasons.clear(); }

    // Upstream only queues mismatched blocks. Accepted substitutes must also be rechecked after edits.
    @Inject(method="markBlockChanged",at=@At("HEAD"))
    private void flex$changed(BlockPos pos,CallbackInfo ci) {
        flex$stateErrors.remove(pos);
        flex$reasons.remove(pos);
        if (!((SchematicVerifier)(Object)this).isFinished()) return;
        BlockState previous = flex$accepted.remove(pos);
        if (previous == null) return;
        flex$exact.remove(pos);
        flex$temporary.remove(pos);
        correctStatesCount--;
        correctStateCounts.addTo(previous,-1);
        if (worldClient.getBlockState(pos).isAir()) clientBlocks--;
        recheckQueue.add(pos.immutable());
    }

    @Override public int flexExactCount() { return flex$exact.size(); }
    @Override public int flexTemporaryCount() { return flex$temporary.size(); }
    @Override public int flexSubstitutionCount() { return flex$accepted.size()-flex$exact.size()-flex$temporary.size(); }
    @Override public int flexSubstitutionStateErrors(){return flex$stateErrors.size();}
    @Override public Map<String,Integer> flexReasons() {
        Map<String,Integer> result=new TreeMap<>();
        flex$reasons.values().forEach(reason -> result.merge(reason,1,Integer::sum));
        return Map.copyOf(result);
    }
}
