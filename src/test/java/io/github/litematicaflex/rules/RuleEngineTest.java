package io.github.litematicaflex.rules;

import io.github.litematicaflex.api.BlockDescription;
import io.github.litematicaflex.config.RuleProfile;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {
    private final RuleEngine engine=new RuleEngine();
    private BlockDescription block(String id,String shape,String... groups) {
        return new BlockDescription(id,shape,Set.of(groups),Map.of(),1.5f,false,false,false);
    }
    private BlockDescription state(BlockDescription block,Map<String,String> properties) {
        return new BlockDescription(block.id(),block.shape(),block.groups(),properties,block.hardness(),false,false,block.functional());
    }
    @Test void woodAndStoneStaySeparateWhenBothEnabled() {
        RuleProfile p=new RuleProfile(); p.enabledGroups.addAll(List.of("wood","stone"));
        var oak=block("oak","full","wood.planks","shape.full");
        var spruce=block("spruce","full","wood.planks","shape.full");
        var stone=block("stone","full","stone.all","shape.full");
        var andesite=block("andesite","full","stone.all","shape.full");
        assertTrue(engine.compare(oak,spruce,p).accepted());
        assertTrue(engine.compare(stone,andesite,p).accepted());
        assertFalse(engine.compare(oak,stone,p).accepted());
    }
    @Test void fullShapeExplicitlyBridgesMaterials() {
        RuleProfile p=new RuleProfile(); p.allReplacements=true;
        assertTrue(engine.compare(block("oak","full","wood.planks","shape.full"),block("stone","full","stone.all","shape.full"),p).accepted());
        assertFalse(engine.compare(block("oak","full","shape.full"),block("slab","slab","shape.slab"),p).accepted());
    }
    @Test void stairsBridgeButPreserveDirection() {
        RuleProfile p=new RuleProfile(); p.allReplacements=true;
        var oak=block("oak_stairs","stairs","shape.stairs");
        var stone=block("stone_stairs","stairs","shape.stairs");
        assertTrue(engine.compare(state(oak,Map.of("facing","north")),state(stone,Map.of("facing","north")),p).accepted());
        assertFalse(engine.compare(state(oak,Map.of("facing","north")),state(stone,Map.of("facing","south")),p).accepted());
    }
    @Test void overlappingGroupsDoNotCreateTransitiveMatches() {
        RuleProfile p=new RuleProfile(); p.enabledGroups.addAll(List.of("first","second"));
        var a=block("a","full","first");var b=block("b","full","first","second");var c=block("c","full","second");
        assertTrue(engine.compare(a,b,p).accepted());assertTrue(engine.compare(b,c,p).accepted());
        assertFalse(engine.compare(a,c,p).accepted());
    }
    @Test void mappingsAreDirectional() {
        RuleProfile p=new RuleProfile();p.replacements.put("a",List.of("b"));
        assertTrue(engine.compare(block("a","full"),block("b","full"),p).accepted());
        assertFalse(engine.compare(block("b","full"),block("a","full"),p).accepted());
    }
    @Test void strictBlockOverridesMappingsAndGroups() {
        RuleProfile p=new RuleProfile();p.strictBlocks.add("a");p.allReplacements=true;p.replacements.put("a",List.of("b"));
        assertFalse(engine.compare(block("a","full","shape.full"),block("b","full","shape.full"),p).accepted());
    }
    @Test void airAndFluidsCannotBecomeBuildingMaterials() {
        RuleProfile p=new RuleProfile();p.allReplacements=true;
        var stone=block("stone","full","shape.full");
        var air=new BlockDescription("air","full",Set.of("shape.full"),Map.of(),0,true,false,false);
        var water=new BlockDescription("water","full",Set.of("shape.full"),Map.of(),0,false,true,false);
        assertFalse(engine.compare(stone,air,p).accepted());assertFalse(engine.compare(stone,water,p).accepted());
    }
    @Test void waterloggedCanBeIgnoredWithoutIgnoringSlabPosition() {
        RuleProfile p=new RuleProfile();p.enabledGroups.add("wood.slab");p.ignoredProperties.add("waterlogged");
        var oak=block("oak_slab","slab","wood.slab");var spruce=block("spruce_slab","slab","wood.slab");
        var a=state(oak,Map.of("type","top","waterlogged","true"));
        assertTrue(engine.compare(a,state(spruce,Map.of("type","top","waterlogged","false")),p).accepted());
        assertFalse(engine.compare(a,state(spruce,Map.of("type","bottom","waterlogged","false")),p).accepted());
    }
    @Test void hardnessIsPairwiseAndRejectsUnbreakable() {
        RuleProfile p=new RuleProfile();p.hardnessMatching=true;p.hardnessTolerance=0.5f;
        var a=new BlockDescription("a","full",Set.of(),Map.of(),1,false,false,false);
        var b=new BlockDescription("b","full",Set.of(),Map.of(),1.4f,false,false,false);
        var c=new BlockDescription("c","full",Set.of(),Map.of(),1.8f,false,false,false);
        var bedrock=new BlockDescription("bedrock","full",Set.of(),Map.of(),-1,false,false,false);
        assertTrue(engine.compare(a,b,p).accepted());assertTrue(engine.compare(b,c,p).accepted());
        assertFalse(engine.compare(a,c,p).accepted());assertFalse(engine.compare(a,bedrock,p).accepted());
    }
    @Test void broadShapesProtectFunctionalBlocksUnlessOptedIn() {
        RuleProfile p=new RuleProfile();p.allReplacements=true;
        var a=block("minecraft:stone","full","shape.full");
        var b=new BlockDescription("minecraft:observer","full",Set.of("shape.full"),Map.of(),1.5f,false,false,true);
        assertFalse(engine.compare(a,b,p).accepted());p.blacklistEnabled=false;assertTrue(engine.compare(a,b,p).accepted());
    }
    @Test void strictReviewPreservesExactButRejectsSubstitutions() {
        RuleProfile p=new RuleProfile();p.strictReview=true;p.enabledGroups.add("wood.planks");
        var a=block("oak","full","wood.planks");var b=block("spruce","full","wood.planks");
        assertEquals(MatchResult.EXACT,engine.compare(a,a,p));assertFalse(engine.compare(a,b,p).accepted());
    }
    @Test void allReplacementsOverrideWithoutErasingCategoryChoices() {
        var p=new RuleProfile();p.enabledGroups.add("wood.planks");
        var oak=block("oak","full","wood.planks");
        var spruce=block("spruce","full","wood.planks");
        var stone=block("stone","full","stone.all");
        assertTrue(engine.compare(oak,spruce,p).accepted());
        assertFalse(engine.compare(oak,stone,p).accepted());
        p.allReplacements=true;
        assertTrue(engine.compare(oak,stone,p).accepted());
        assertEquals(Set.of("wood.planks"),p.enabledGroups);
        assertTrue(p.copy().allReplacements);
        p.allReplacements=false;
        assertTrue(engine.compare(oak,spruce,p).accepted());
        assertFalse(engine.compare(oak,stone,p).accepted());
    }
    @Test void broadReplaceDoesNotOverrideStrictBlocksOrStateConstraints() {
        var p=new RuleProfile();p.allReplacements=true;p.allReplacements=true;
        var a=state(block("oak","stairs","shape.stairs"),Map.of("half","top"));
        var b=state(block("stone","stairs","shape.stairs"),Map.of("half","bottom"));
        assertFalse(engine.compare(a,b,p).accepted());
        p.ignoredProperties.add("half");assertTrue(engine.compare(a,b,p).accepted());
        p.strictBlocks.add("stone");assertFalse(engine.compare(a,b,p).accepted());
    }
    @Test void replacingPresetsKeepsExplicitCustomMappings() {
        var p=new RuleProfile();p.replacements.put("a",List.of("b"));
        assertTrue(engine.compare(block("a","full"),block("b","full"),p).accepted());
    }
    @Test void plankLogReplacementRequiresExplicitOptInEvenInAllMode() {
        var p=new RuleProfile();p.allReplacements=true;
        var plank=block("oak_planks","full","wood.planks");
        var log=state(block("stripped_spruce_log","full","wood.stripped_log"),Map.of("axis","x"));
        assertFalse(engine.compare(plank,log,p).accepted());
        assertFalse(engine.compare(log,plank,p).accepted());
        p.allowPlankLogReplacement=true;
        assertTrue(engine.compare(plank,log,p).accepted());
        p.allReplacements=false;
        assertTrue(engine.compare(log,plank,p).accepted());
        assertTrue(p.copy().allowPlankLogReplacement);
    }
    @Test void compatibleLogWithWrongAxisIsOnlyAMaterialMatch() {
        var p=new RuleProfile();p.allReplacements=true;
        var a=state(block("stripped_oak_log","full","wood.stripped_log"),Map.of("axis","y"));
        var b=state(block("stripped_spruce_log","full","wood.stripped_log"),Map.of("axis","x"));
        assertFalse(engine.compare(a,b,p).accepted());
        assertTrue(engine.materialsMatch(a,b,p));
        p.strictBlocks.add("stripped_spruce_log");
        assertFalse(engine.materialsMatch(a,b,p));
    }

}
