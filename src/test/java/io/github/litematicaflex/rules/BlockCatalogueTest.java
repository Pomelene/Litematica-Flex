package io.github.litematicaflex.rules;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Checks the shipped catalogue without booting Minecraft or requiring a graphical environment. */
class BlockCatalogueTest {
    private JsonArray groups() {
        InputStream stream=getClass().getResourceAsStream("/assets/litematica_flex/catalogue.json");
        assertNotNull(stream);
        return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("groups");
    }
    private Set<String> members(String id) {
        for(var element:groups()) {
            var group=element.getAsJsonObject();
            if(group.get("id").getAsString().equals(id)) {
                Set<String> values=new HashSet<>();group.getAsJsonArray("members").forEach(v -> values.add(v.getAsString()));return values;
            }
        }
        fail("Missing group "+id);return Set.of();
    }
    @Test void undyedGlassPanesAndTerracottaAreIncludedByDefault() {
        assertEquals(17,members("color.stained_glass").size());assertTrue(members("color.stained_glass").contains("minecraft:glass"));
        assertEquals(17,members("color.stained_glass_pane").size());assertTrue(members("color.stained_glass_pane").contains("minecraft:glass_pane"));
        assertEquals(17,members("color.terracotta").size());assertTrue(members("color.terracotta").contains("minecraft:terracotta"));
        assertFalse(members("color.stained_glass").contains("minecraft:tinted_glass"));
    }
    @Test void new263FamiliesArePresent() {
        assertTrue(members("wood.planks").contains("minecraft:poplar_planks"));
        assertEquals(16,members("color.wool_stairs").size());assertEquals(16,members("color.concrete_slab").size());
        assertTrue(members("stone.sulfur").contains("minecraft:sulfur_stairs"));
        assertFalse(members("stone.sulfur").contains("minecraft:potent_sulfur"));
    }
    @Test void oxidationAndWaxHaveSeparateMemberships() {
        var oxid=members("copper.oxidation.copper_block.false");
        assertTrue(oxid.contains("minecraft:oxidized_copper"));assertFalse(oxid.contains("minecraft:waxed_oxidized_copper"));
        var wax=members("copper.wax.copper_block.fresh");
        assertEquals(Set.of("minecraft:copper_block","minecraft:waxed_copper_block"),wax);
        assertEquals(8,members("copper.combined.copper_block").size());
    }
    @Test void catalogueIdsAndMembersAreUnique() {
        Set<String> ids=new HashSet<>();
        for(var element:groups()) {
            var group=element.getAsJsonObject();assertTrue(ids.add(group.get("id").getAsString()));
            List<String> entries=new ArrayList<>();group.getAsJsonArray("members").forEach(v -> entries.add(v.getAsString()));
            assertEquals(entries.size(),new HashSet<>(entries).size());assertTrue(entries.size()>1);
        }
    }
    @Test void visibleStonePresetsHaveExplicitShapesAndSeparateBaseFamilies() {
        assertEquals(Set.of("minecraft:stone","minecraft:smooth_stone"),members("stone.stone.full"));
        assertEquals(Set.of("minecraft:cobblestone","minecraft:mossy_cobblestone"),members("stone.cobblestone.full"));
        assertFalse(members("stone.stone.full").contains("minecraft:stone_bricks"));
        assertTrue(members("stone.stone_brick.full").contains("minecraft:stone_bricks"));
        for(var element:groups()) {
            var group=element.getAsJsonObject();
            if(!group.get("category").getAsString().equals("石材") || group.get("id").getAsString().equals("stone.all"))continue;
            String id=group.get("id").getAsString();String shape=id.substring(id.lastIndexOf('.')+1);
            assertTrue(Set.of("full","stairs","slab","wall").contains(shape));
            for(var entry:group.getAsJsonArray("members")) {
                String member=entry.getAsString();
                if(shape.equals("full"))assertFalse(member.endsWith("_stairs")||member.endsWith("_slab")||member.endsWith("_wall"));
                else assertTrue(member.endsWith("_"+shape));
            }
        }
    }
    @Test void stoneAggregateIsPartOfTheStoneCategory() {
        for(var element:groups()) {
            var group=element.getAsJsonObject();
            if(group.get("id").getAsString().equals("stone.all"))assertEquals("石材",group.get("category").getAsString());
        }
    }
}
