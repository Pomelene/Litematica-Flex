package io.github.litematicaflex.gui;

import com.google.gson.JsonParser;
import net.minecraft.client.resources.language.I18n;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import io.github.litematicaflex.rules.BlockCatalogue;

/** Resolves display text at use time, so Minecraft language changes need no config migration. */
public final class FlexText {
    private static final String PREFIX="litematica_flex.";
    private static final List<String> PHRASES=loadPhrases();
    private FlexText() {}

    private static List<String> loadPhrases() {
        try(var stream=FlexText.class.getResourceAsStream("/assets/litematica_flex/lang/en_us.json")) {
            if(stream==null)throw new IllegalStateException("Missing English translations");
            var entries=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            return entries.keySet().stream().filter(key -> !key.startsWith(PREFIX+"group.")).map(key -> key.substring(PREFIX.length()))
                .sorted(Comparator.comparingInt(String::length).reversed()).toList();
        } catch(Exception e){throw new IllegalStateException("Cannot load Flex translations",e);}
    }

    public static String tr(String text) {
        if(text==null)return null;
        for(String phrase:PHRASES) {
            if(!text.contains(phrase))continue;
            String translated=I18n.get(PREFIX+phrase);
            if(!translated.equals(PREFIX+phrase))text=text.replace(phrase,translated);
        }
        return text;
    }

    public static String group(BlockCatalogue.Group group) {
        String key=PREFIX+"group."+group.id();
        String value=I18n.get(key);
        return value.equals(key)?tr(group.title()):value;
    }
}
