package io.github.litematicaflex.rules;

import io.github.litematicaflex.api.BlockDescription;
import io.github.litematicaflex.config.RuleProfile;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BlacklistPresetsTest {
    private final RuleEngine engine=new RuleEngine();
    private BlockDescription block(String id,Map<String,String> properties) {
        return new BlockDescription(id,"full",Set.of("test.group"),properties,1,false,false,false);
    }
    @Test void presetsProtectBothEndsInEveryReplacementPath() {
        for(var preset:BlacklistPresets.ALL)for(String id:preset.blocks()) {
            var p=new RuleProfile();p.blacklistPresets=new HashSet<>(Set.of(preset.id()));
            p.allReplacements=true;p.allowPlankLogReplacement=true;p.hardnessMatching=true;
            p.enabledGroups.add("test.group");p.customGroups.add(Set.of(id,"minecraft:stone"));
            p.replacements.put(id,List.of("minecraft:stone"));p.replacements.put("minecraft:stone",List.of(id));
            var protectedBlock=block(id,Map.of());var stone=block("minecraft:stone",Map.of());
            assertFalse(engine.compare(protectedBlock,stone,p).accepted(),id);
            assertFalse(engine.compare(stone,protectedBlock,p).accepted(),id);
            assertTrue(engine.compare(protectedBlock,protectedBlock,p).exact());
            p.allReplacements=false;
            assertFalse(engine.compare(protectedBlock,stone,p).accepted(),id);
        }
    }
    @Test void blacklistPreventsStateIgnoringAndCanBeDisabled() {
        var p=new RuleProfile();p.allReplacements=true;p.ignoredProperties.add("axis");
        p.blacklistBlocks.add("minecraft:oak_log");
        var a=block("minecraft:oak_log",Map.of("axis","y"));var b=block("minecraft:oak_log",Map.of("axis","x"));
        assertFalse(engine.compare(a,b,p).accepted());
        p.blacklistEnabled=false;assertTrue(engine.compare(a,b,p).accepted());
        p.strictBlocks.add("minecraft:oak_log");assertFalse(engine.compare(a,b,p).accepted());
    }
    @Test void defaultsAndCopiesHaveIndependentProtectionLists() {
        var p=new RuleProfile();assertTrue(BlacklistPresets.excludes(p,"minecraft:obsidian"));
        assertTrue(BlacklistPresets.excludes(p,"minecraft:slime_block"));
        assertTrue(BlacklistPresets.excludes(p,"minecraft:redstone_block"));
        assertTrue(BlacklistPresets.excludes(p,"minecraft:red_shulker_box"));
        assertFalse(BlacklistPresets.excludes(p,"minecraft:stone"));
        var copy=p.copy();p.blacklistPresets.clear();p.blacklistBlocks.add("minecraft:stone");
        assertTrue(BlacklistPresets.excludes(copy,"minecraft:obsidian"));
        assertFalse(BlacklistPresets.excludes(copy,"minecraft:stone"));
    }
}
