package io.github.litematicaflex.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationStoreTest {
    @TempDir Path directory;
    private Path file() { return directory.resolve("config/litematica-flex.json"); }

    @Test void oneFileRoundTripsAllPortableData() throws Exception {
        var store=new ConfigurationStore(file());
        var config=store.editable();
        config.global.allReplacements=true;
        config.global.blacklistBlocks.add("minecraft:oak_log");
        config.global.customGroups.add(new LinkedHashSet<>(List.of("minecraft:stone","minecraft:andesite")));
        config.global.replacements.put("minecraft:oak_planks",List.of("minecraft:spruce_planks"));
        config.savedProfiles.put("生存建筑",ProfileLibrary.copy(config.global));
        config.hotkeys.put("打开 Flex 设置","LEFT_ALT,F");
        config.placements.put("example-placement",ProfileLibrary.copy(config.global));
        var region=new FlexConfiguration.RegionOverride();region.placement="example-placement";
        config.regions.add(region);
        store.save();
        var imported=new ConfigurationStore(file());
        assertTrue(imported.load());
        assertTrue(imported.editable().global.allReplacements);
        assertEquals(config.global.blacklistBlocks,imported.editable().global.blacklistBlocks);
        assertEquals(config.global.blacklistPresets,imported.editable().savedProfiles.get("生存建筑").blacklistPresets);
        assertTrue(imported.editable().savedProfiles.get("生存建筑").allReplacements);
        assertEquals(config.global.replacements,imported.editable().global.replacements);
        assertEquals(config.global.customGroups,imported.editable().savedProfiles.get("生存建筑").customGroups);
        assertEquals(config.hotkeys,imported.editable().hotkeys);
        assertEquals(1,imported.editable().placements.size());
        assertEquals(1,imported.editable().regions.size());
        try(var files=Files.list(file().getParent())) { assertEquals(1,files.count()); }
    }

    @Test void badJsonPreservesLastValidRulesAndFile() throws Exception {
        var store=new ConfigurationStore(file());store.save();
        var original=store.editable();
        Files.writeString(file(),"{broken json");
        assertFalse(store.load());
        assertSame(original,store.editable());
        assertNotNull(store.lastError());
        assertThrows(IllegalStateException.class,store::save);
        assertEquals("{broken json",Files.readString(file()));
    }

    @Test void externalEditRequiresReloadAndThenCanSave() throws Exception {
        var store=new ConfigurationStore(file());store.save();
        String external=Files.readString(file()).replace("\"showHud\": true","\"showHud\": false");
        Files.writeString(file(),external);
        assertThrows(IllegalStateException.class,store::save);
        assertTrue(store.load());
        assertFalse(store.editable().showHud);
        store.save();
        assertEquals(external,Files.readString(file().resolveSibling("litematica-flex.json.bak")));
    }

    @Test void existingSchemaLoadsWithoutNewOptionalFields() throws Exception {
        Files.createDirectories(file().getParent());
        Files.writeString(file(),"{\"schemaVersion\":1,\"global\":{\"enabledGroups\":[\"wood.planks\"]}}");
        var store=new ConfigurationStore(file());assertTrue(store.load());
        assertTrue(store.editable().global.enabledGroups.contains("wood.planks"));
        assertFalse(store.editable().global.allReplacements);
        assertNotNull(store.editable().savedProfiles);store.save();
    }

    @Test void invalidNestedProfileIsRejected() throws Exception {
        var store=new ConfigurationStore(file());store.save();
        Files.writeString(file(),Files.readString(file()).replace("\"selection\": \"HELD_FIRST\"","\"selection\": \"UNKNOWN\""));
        assertFalse(store.load());assertThrows(IllegalStateException.class,store::save);
    }

    @Test void namedProfileCopiesAreIndependent() {
        var source=new RuleProfile();source.enabledGroups.add("wood.planks");
        var copy=ProfileLibrary.copy(source);copy.enabledGroups.add("stone.all");
        assertFalse(source.enabledGroups.contains("stone.all"));
    }
    @Test void malformedBlacklistIdPreservesOriginalConfiguration() throws Exception {
        var store=new ConfigurationStore(file());store.save();
        var disk=Files.readString(file());
        store.editable().global.blacklistBlocks.add("Minecraft:Bad ID");
        assertThrows(IllegalArgumentException.class,store::save);
        assertEquals(disk,Files.readString(file()));
    }

}
