package io.github.litematicaflex.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import fi.dy.masa.litematica.materials.*;
import io.github.litematicaflex.runtime.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value=MaterialListUtils.class,remap=false)
public abstract class MaterialListMixin {
    @WrapOperation(method="createMaterialListFor(Lfi/dy/masa/litematica/schematic/LitematicaSchematic;Ljava/util/Collection;)Ljava/util/List;",at=@At(value="INVOKE",
            target="Lfi/dy/masa/litematica/materials/MaterialListUtils;getMaterialList(Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;Lnet/minecraft/world/entity/player/Player;)Ljava/util/List;"))
    private static List<MaterialListEntry> flex$plannedMaterial(Object2IntOpenHashMap<BlockState> total,Object2IntOpenHashMap<BlockState> missing,
            Object2IntOpenHashMap<BlockState> mismatch,Player player,Operation<List<MaterialListEntry>> original) {
        var profile = FlexRuntime.profile(null,null);
        if (!FlexRuntime.active(profile,FlexRuntime.Channel.MATERIALS)) return original.call(total,missing,mismatch,player);
        return original.call(flex$remap(total,profile),flex$remap(missing,profile),flex$remap(mismatch,profile),player);
    }
    private static Object2IntOpenHashMap<BlockState> flex$remap(Object2IntOpenHashMap<BlockState> input,io.github.litematicaflex.config.RuleProfile profile) {
        Object2IntOpenHashMap<BlockState> output=new Object2IntOpenHashMap<>();
        for(BlockState state:input.keySet()) output.addTo(ReplacementResolver.fixed(state,null,profile),input.getInt(state));
        return output;
    }
}
