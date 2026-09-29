package io.github.litematicaflex.rules;

import io.github.litematicaflex.config.RuleProfile;
import java.util.*;

/** Explicit protection presets; no name guessing and no transitive matching. */
public final class BlacklistPresets {
    public record Preset(String id,String title,Set<String> blocks) {}
    public static final String COPPER_AMETHYST="copper_amethyst";
    public static final Preset REDSTONE_ALL=preset("redstone","红石核心方块（全部保护）",
        "redstone_block","redstone_wire","repeater","comparator","observer","piston","sticky_piston",
        "piston_head","moving_piston","dispenser","dropper","hopper","redstone_torch","redstone_wall_torch",
        "redstone_lamp","target","note_block");
    public static final List<Preset> REDSTONE_DETAILS=List.of(
        preset("redstone.components","非完整方块元件","redstone_wire","repeater","comparator","piston_head",
            "moving_piston","hopper","redstone_torch","redstone_wall_torch"),
        preset("redstone.block","红石块","redstone_block"),
        preset("redstone.observer","侦测器","observer"),
        preset("redstone.piston","活塞","piston"),
        preset("redstone.sticky_piston","黏性活塞","sticky_piston"),
        preset("redstone.dispenser","发射器","dispenser"),
        preset("redstone.dropper","投掷器","dropper"),
        preset("redstone.lamp","红石灯","redstone_lamp"),
        preset("redstone.target","标靶","target"),
        preset("redstone.note_block","音符盒","note_block")
    );
    public static final List<Preset> ALL=allPresets();
    private BlacklistPresets() {}

    private static List<Preset> allPresets() {
        var presets=new ArrayList<Preset>();
        presets.add(REDSTONE_ALL);
        presets.addAll(REDSTONE_DETAILS);
        presets.add(preset("moving","粘液与蜂蜜","slime_block","honey_block"));
        presets.add(preset("obsidian","黑曜石与哭泣黑曜石","obsidian","crying_obsidian"));
        presets.add(containers());
        presets.add(preset(COPPER_AMETHYST,"铜灯与紫水晶",
            "copper_bulb","exposed_copper_bulb","weathered_copper_bulb","oxidized_copper_bulb",
            "waxed_copper_bulb","waxed_exposed_copper_bulb","waxed_weathered_copper_bulb","waxed_oxidized_copper_bulb",
            "amethyst_block","budding_amethyst","small_amethyst_bud","medium_amethyst_bud","large_amethyst_bud","amethyst_cluster"));
        presets.add(preset("fluid_mechanics","冰、海绵与气泡柱材料","ice","packed_ice","blue_ice","frosted_ice","sponge","wet_sponge","soul_sand","soul_soil","magma_block"));
        presets.add(preset("special","特殊功能方块","tnt","budding_amethyst","creaking_heart","sculk_sensor","calibrated_sculk_sensor","sculk_shrieker","sculk_catalyst","tinted_glass"));
        return List.copyOf(presets);
    }
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
