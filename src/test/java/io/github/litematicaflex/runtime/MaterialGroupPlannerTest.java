package io.github.litematicaflex.runtime;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MaterialGroupPlannerTest {
    @Test void mergedDemandCountsSharedStockOnce() {
        var result=MaterialGroupPlanner.plan(List.of(
            new MaterialGroupPlanner.Demand("slab_a",10,6,1),
            new MaterialGroupPlanner.Demand("slab_b",8,4,0)),
            List.of(new MaterialPlanner.Supply("slab_a",3),new MaterialPlanner.Supply("slab_b",2),new MaterialPlanner.Supply("slab_c",4)),
            (expected,candidate)->expected.startsWith("slab_") && candidate.startsWith("slab_"));
        assertEquals(1,result.size());
        assertEquals(18,result.getFirst().total());
        assertEquals(10,result.getFirst().missing());
        assertEquals(9,result.getFirst().covered());
        assertEquals(9,result.getFirst().assigned().values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test void overlappingRulesDoNotMergeEndpoints() {
        var result=MaterialGroupPlanner.plan(List.of(
            new MaterialGroupPlanner.Demand("wood",1,1,0),
            new MaterialGroupPlanner.Demand("bridge",1,1,0),
            new MaterialGroupPlanner.Demand("stone",1,1,0)),List.of(),
            (a,b)->a.equals(b) || a.equals("bridge") || b.equals("bridge"));
        assertEquals(2,result.size());
        assertEquals(List.of("wood","bridge"),result.getFirst().members());
        assertEquals(List.of("stone"),result.get(1).members());
    }
}
