package io.github.litematicaflex.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/** Keeps every built-in rule visible in both shipped languages. */
class LanguageResourcesTest {
    private JsonObject read(String path) throws Exception {
        try(var stream=getClass().getResourceAsStream(path)) {
            assertNotNull(stream,path);
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void bothLanguagesCoverEveryPresetAndInterfaceKey() throws Exception {
        var english=read("/assets/litematica_flex/lang/en_us.json");
        var chinese=read("/assets/litematica_flex/lang/zh_cn.json");
        assertEquals(english.keySet(),chinese.keySet());
        for(var entry:english.entrySet()) {
            assertFalse(entry.getValue().getAsString().isBlank(),entry.getKey());
            assertFalse(entry.getValue().getAsString().matches(".*[\\p{IsHan}].*"),entry.getKey());
        }
        var catalogue=read("/assets/litematica_flex/catalogue.json");
        for(var element:catalogue.getAsJsonArray("groups")) {
            var group=element.getAsJsonObject();
            String key="litematica_flex.group."+group.get("id").getAsString();
            assertTrue(english.has(key),key);
            assertEquals(group.get("title").getAsString(),chinese.get(key).getAsString(),key);
        }
    }
}
