package io.github.litematicaflex.runtime;

import io.github.litematicaflex.config.RuleProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.*;

/** Select one concrete target before Litematica computes the required item and click geometry. */
public final class ReplacementResolver {
    private ReplacementResolver() {}

    public static BlockState forPlacement(BlockState expected, BlockPos pos) {
        RuleProfile profile = FlexRuntime.profile(pos, null);
        Minecraft mc = Minecraft.getInstance();
        if (!FlexRuntime.active(profile, FlexRuntime.Channel.PLACEMENT) || expected.isAir() || mc.player == null) return expected;
        List<ItemStack> inventory=new ArrayList<>();
        for(int slot=0;slot<mc.player.getInventory().getContainerSize();slot++)inventory.add(mc.player.getInventory().getItem(slot));
        return selectFromStacks(expected,pos,profile,List.of(mc.player.getMainHandItem(),mc.player.getOffhandItem()),inventory);
    }

    /** Deterministic selector shared by the game integration and real-item regression tests. */
    public static BlockState selectFromStacks(BlockState expected,BlockPos pos,RuleProfile profile,List<ItemStack> heldStacks,List<ItemStack> inventory) {
        if (!FlexRuntime.active(profile,FlexRuntime.Channel.PLACEMENT) || expected.isAir()) return expected;
        if (profile.selection.equals("HELD_FIRST")) {
            for (ItemStack held : heldStacks) {
                BlockState state = fromStack(expected, held, profile);
                if (state != null) return state;
            }
        }
        BlockState fixed = fixed(expected,pos,profile,FlexRuntime.Channel.PLACEMENT);
        if (fixed != expected) return fixed;
        if (profile.selection.equals("ORIGINAL_FIRST") && inventory.stream().anyMatch(stack -> !stack.isEmpty() && stack.is(expected.getBlock().asItem()))) return expected;
        if (profile.selection.equals("FIXED_ONLY")) return expected;
        for (ItemStack stack:inventory) {
            BlockState candidate = fromStack(expected,stack,profile);
            if (candidate != null) return candidate;
        }
        return expected;
    }

    /** Stable material planning uses fixed mappings only, never today's inventory contents. */
    public static BlockState fixed(BlockState expected, BlockPos pos, RuleProfile profile) {
        return fixed(expected,pos,profile,FlexRuntime.Channel.MATERIALS);
    }

    public static BlockState fixed(BlockState expected, BlockPos pos, RuleProfile profile, FlexRuntime.Channel channel) {
        if (!profile.enabled || profile.strictReview) return expected;
        String id = BuiltInRegistries.BLOCK.getKey(expected.getBlock()).toString();
        for (String target : profile.replacements.getOrDefault(id,List.of())) {
            Identifier key = Identifier.tryParse(target);
            if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) continue;
            BlockState candidate = transfer(expected,BuiltInRegistries.BLOCK.getValue(key).defaultBlockState());
            if (FlexRuntime.matchWithProfile(expected,candidate,profile,channel).accepted()) return candidate;
        }
        return expected;
    }

    private static BlockState fromStack(BlockState expected, ItemStack stack, RuleProfile profile) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return null;
        BlockState candidate = transfer(expected,item.getBlock().defaultBlockState());
        return FlexRuntime.matchWithProfile(expected,candidate,profile,FlexRuntime.Channel.PLACEMENT).accepted() ? candidate : null;
    }

    /** Prefer an empty slot, then a building-block slot; never evict tools or other equipment. */
    public static int safeHotbarSlot(List<ItemStack> hotbar,int selected) {
        for(int i=0;i<Math.min(9,hotbar.size());i++)if(hotbar.get(i).isEmpty())return i;
        if(selected>=0 && selected<hotbar.size() && hotbar.get(selected).getItem() instanceof BlockItem)return selected;
        for(int i=0;i<Math.min(9,hotbar.size());i++)if(hotbar.get(i).getItem() instanceof BlockItem)return i;
        return -1;
    }

    public static BlockState transfer(BlockState expected, BlockState candidate) {
        for (Property<?> property : expected.getProperties()) candidate = copyProperty(expected,candidate,property);
        return candidate;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState expected,BlockState candidate,Property<T> property) {
        if (candidate.hasProperty(property) && property.getPossibleValues().contains(expected.getValue(property))) {
            return candidate.setValue(property,expected.getValue(property));
        }
        return candidate;
    }
}
