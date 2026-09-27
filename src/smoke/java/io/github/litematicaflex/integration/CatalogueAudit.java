package io.github.litematicaflex.integration;

import io.github.litematicaflex.api.BlockDescription;
import io.github.litematicaflex.config.RuleProfile;
import io.github.litematicaflex.rules.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.util.*;

/** Exhaustive registered vanilla inventory and state audit; the output is reviewable, not a production check. */
final class CatalogueAudit {
    static void run() throws Exception {
        var catalogue=new BlockCatalogue();var engine=new RuleEngine();
        var blocks=new LinkedHashMap<String,BlockDescription>();
        var inventory=new StringBuilder("id,shape,functional,properties,groups\n");
        int states=0,stateChecks=0;
        for(var block:BuiltInRegistries.BLOCK) {
            var base=catalogue.describe(block.defaultBlockState());blocks.put(base.id(),base);
            inventory.append(csv(base.id())).append(',').append(csv(base.shape())).append(',').append(base.functional()).append(',')
                .append(csv(new TreeMap<>(base.properties()).toString())).append(',').append(csv(new TreeSet<>(base.groups()).toString())).append('\n');
            var p=new RuleProfile();p.allReplacements=true;
            for(var state:block.getStateDefinition().getPossibleStates()) {
                var actual=catalogue.describe(state);states++;
                if(base.properties().equals(actual.properties()) || base.air() || base.fluid() || actual.fluid())continue;
                require(!engine.compare(base,actual,p).accepted(),"State silently accepted: "+base.id()+actual.properties());
                p.ignoredProperties=new HashSet<>(base.properties().keySet());
                require(engine.compare(base,actual,p).accepted()==!BlacklistPresets.excludes(p,base.id()),"Ignored state/blacklist priority failed: "+base.id()+actual.properties());
                p.ignoredProperties=Set.of();stateChecks++;
            }
        }
        for(var preset:BlacklistPresets.ALL)for(String id:preset.blocks())require(blocks.containsKey(id),"Unknown blacklist preset block: "+id);
        var report=new StringBuilder("# Minecraft 26.3 replacement audit\n\n");
        report.append("Registered blocks: ").append(blocks.size()).append("; states: ").append(states)
            .append("; non-default state checks: ").append(stateChecks).append(".\n\n");
        report.append("Each preset is evaluated alone. Shape and state restrictions can still reject a declared member pair.\n\n")
            .append("| Preset | Members | Material pairs accepted | Rejected by shape/protection |\n| --- | ---: | ---: | ---: |\n");
        long presetPairs=0;
        for(var group:catalogue.groups()) {
            var p=new RuleProfile();p.enabledGroups.add(group.id());
            int accepted=0,rejected=0;
            for(String id:group.members())require(blocks.containsKey(id),"Unknown preset block: "+group.id()+" / "+id);
            for(int i=0;i<group.members().size();i++)for(int j=i+1;j<group.members().size();j++) {
                var a=material(blocks.get(group.members().get(i)));var b=material(blocks.get(group.members().get(j)));
                boolean forward=engine.compare(a,b,p).accepted();
                require(forward==engine.compare(b,a,p).accepted(),"Asymmetric preset: "+group.id());
                if(forward)accepted++;else rejected++;presetPairs++;
            }
            report.append('|').append(group.id()).append('|').append(group.members().size()).append('|').append(accepted).append('|').append(rejected).append("|\n");
        }
        var all=new RuleProfile();all.allReplacements=true;
        var descriptions=blocks.values().stream().map(CatalogueAudit::material).toList();
        long allPairs=0,accepted=0;
        for(int i=0;i<descriptions.size();i++)for(int j=i+1;j<descriptions.size();j++) {
            var a=descriptions.get(i);var b=descriptions.get(j);
            boolean result=engine.compare(a,b,all).accepted();allPairs++;
            require(result==engine.compare(b,a,all).accepted(),"Asymmetric all replacement");
            if(result) {
                accepted++;
                require(!a.air()&&!b.air()&&!a.fluid()&&!b.fluid(),"Air/fluid leaked");
                require(!BlacklistPresets.excludes(all,a.id())&&!BlacklistPresets.excludes(all,b.id()),"Blacklist leaked");
                require(a.shape().equals(b.shape()),"Shape boundary leaked");
                var strict=all.copy();strict.strictBlocks=Set.of(a.id());
                require(!engine.compare(a,b,strict).accepted(),"Strict exclusion leaked");
            }
        }
        report.append("\nAll-replacements unordered pairs checked: ").append(allPairs).append("; accepted: ").append(accepted)
            .append(". Preset unordered pairs checked: ").append(presetPairs).append(".\n");
        Path directory=Path.of("build/reports/replacement-audit");Files.createDirectories(directory);
        Files.writeString(directory.resolve("blocks.csv"),inventory);Files.writeString(directory.resolve("audit.md"),report);
        System.out.println("Flex exhaustive audit: "+blocks.size()+" blocks, "+states+" states, "+stateChecks+" state constraints, "+presetPairs+" preset pairs, "+allPairs+" all-mode pairs.");
    }
    private static BlockDescription material(BlockDescription b) {
        return new BlockDescription(b.id(),b.shape(),b.groups(),Map.of(),b.hardness(),b.air(),b.fluid(),b.functional());
    }
    private static String csv(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
