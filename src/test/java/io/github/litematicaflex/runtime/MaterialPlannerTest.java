package io.github.litematicaflex.runtime;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MaterialPlannerTest {
    @Test void exactItemsHavePriorityAndSharedSubstitutesAreAllocatedOnce() {
        var rows=MaterialPlanner.plan(
            List.of(new MaterialPlanner.Demand("stone",3),new MaterialPlanner.Demand("brick",3)),
            List.of(new MaterialPlanner.Supply("stone",2),new MaterialPlanner.Supply("slab",3)),
            (expected,candidate)->candidate.equals("slab"));
        assertEquals(3,rows.get(0).covered());
        assertEquals(2,rows.get(1).covered());
        assertEquals(2,rows.get(0).assigned().get("stone"));
        assertEquals(1,rows.get(0).assigned().get("slab"));
    }

    @Test void noTransitiveOrUnapprovedSupplyIsAssigned() {
        var rows=MaterialPlanner.plan(List.of(new MaterialPlanner.Demand("wood",2)),
            List.of(new MaterialPlanner.Supply("stone",10)),(a,b)->false);
        assertEquals(0,rows.getFirst().covered());
    }
}
