package io.github.litematicaflex.rules;

import io.github.litematicaflex.config.RuleProfile;
import java.util.*;

/** Explicit protection presets; no name guessing and no transitive matching. */
public final class BlacklistPresets {
    public record Preset(String id,String title,Set<String> blocks) {}
    public static final List<Preset> ALL=List.of(
        preset("redstone","红石核心方块","redstone_block","redstone_wire","repeater","comparator","observer","piston","sticky_piston","piston_head","moving_piston","dispenser","dropper","hopper","redstone_torch","redstone_wall_torch","redstone_lamp","target","note_block"),
        preset("moving","粘液与蜂蜜","slime_block","honey_block"),
        preset("obsidian","黑曜石与哭泣黑曜石","obsidian","crying_obsidian"),
        containers(),
        preset("fluid_mechanics","冰、海绵与气泡柱材料","ice","packed_ice","blue_ice","frosted_ice","sponge","wet_sponge","soul_sand","soul_soil","magma_block"),
        preset("special","特殊功能方块","tnt","budding_amethyst","creaking_heart","sculk_sensor","calibrated_sculk_sensor","sculk_shrieker","sculk_catalyst","tinted_glass")
    );
    private BlacklistPresets() {}
    private static Preset preset(String id,String title,String... names) {
        var ids=new LinkedHashSet<String>();for(String name:names)ids.add("minecraft:"+name);
        return new Preset(id,title,Set.copyOf(ids));
    }
    private static Preset containers() {
        var ids=new LinkedHashSet<String>();
        for(String name:List.of("chest","trapped_chest","ender_chest","barrel","hopper","dispenser","dropper","crafter","furnace","blast_furnace","smoker","brewing_stand","shulker_box"))ids.add("minecraft:"+name);
        for(String color:List.of("white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"))ids.add("minecraft:"+color+"_shulker_box");
        return new Preset("containers","容器与加工设备（含全部潜影盒）",Set.copyOf(ids));
    }
    public static boolean excludes(RuleProfile p,String id) {
        if(p.strictBlocks.contains(id))return true; // Existing strict exclusions always retain their meaning.
        if(!p.blacklistEnabled)return false;
        if(p.blacklistBlocks.contains(id))return true;
        return ALL.stream().anyMatch(preset -> p.blacklistPresets.contains(preset.id()) && preset.blocks().contains(id));
    }
    public static Set<String> effective(RuleProfile p) {
        var result=new TreeSet<>(p.strictBlocks);
        if(p.blacklistEnabled) {
            result.addAll(p.blacklistBlocks);
            for(var preset:ALL)if(p.blacklistPresets.contains(preset.id()))result.addAll(preset.blocks());
        }
        return Set.copyOf(result);
    }
}
