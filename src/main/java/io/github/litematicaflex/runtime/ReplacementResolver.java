package io.github.litematicaflex.runtime;

import io.github.litematicaflex.config.RuleProfile;
import io.github.litematicaflex.rules.BlacklistPresets;
import fi.dy.masa.litematica.config.Configs;
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

    /** Explain local selection failures without modifying Litematica's picker or inventory. */
    public static String diagnose(BlockState expected,BlockPos pos) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || expected.isAir())return null;
        if(!Configs.Generic.EASY_PLACE_MODE.getBooleanValue())return "Litematica 轻松放置未开启";
        var profile=FlexRuntime.profile(pos,null);
        if(!profile.enabled)return "Flex 替换总开关已关闭";
        if(profile.strictReview)return "严格复核正在暂停替换";
        if(!profile.placement)return "Flex 未应用于轻松放置";
        String expectedId=BuiltInRegistries.BLOCK.getKey(expected.getBlock()).toString();
        if(BlacklistPresets.excludes(profile,expectedId))return "原理图方块受到黑名单保护";
        var inventory=mc.player.getInventory();
        var selected=forPlacement(expected,pos);
        var required=selected.getBlock().asItem();
        int source=-1;
        for(int slot=0;slot<inventory.getContainerSize();slot++)
            if(inventory.getItem(slot).is(required)){source=slot;break;}
        if(source>=0) {
            if(source>=9) {
                List<ItemStack> hotbar=new ArrayList<>();
                for(int i=0;i<9;i++)hotbar.add(inventory.getItem(i));
                if(safeHotbarSlot(hotbar,inventory.getSelectedSlot())<0)return "快捷栏没有可安全替换的空位";
            }
            return null;
        }
        if(selected!=expected)return "固定映射目标不在随身背包";
        if(profile.selection.equals("FIXED_ONLY"))return "当前只允许固定映射选材";
        boolean stateMismatch=false;
        for(int slot=0;slot<inventory.getContainerSize();slot++) {
            var stack=inventory.getItem(slot);
            if(stack.getItem() instanceof BlockItem item) {
                var candidate=transfer(expected,item.getBlock().defaultBlockState());
                stateMismatch|=FlexRuntime.materialMatches(expected,candidate,pos,null,FlexRuntime.Channel.PLACEMENT)
                    && !FlexRuntime.match(expected,candidate,pos,null,FlexRuntime.Channel.PLACEMENT).accepted();
            }
        }
        if(stateMismatch)return "背包材料符合种类，但状态限制不允许";
        return "随身背包没有合规方块；末影箱缓存不能直接供轻松放置取用";
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
