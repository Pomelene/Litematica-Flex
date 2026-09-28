package io.github.litematicaflex.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Atomic JSON replacement; invalid files are preserved rather than silently overwritten. */
public final class ConfigurationStore {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private FlexConfiguration editable = new FlexConfiguration();
    private boolean validFile = true;
    private String lastRead;
    private String lastError;

    public ConfigurationStore() { this(FabricLoader.getInstance().getConfigDir().resolve("litematica-flex.json")); }
    public ConfigurationStore(Path path) { this.path = path; }
    public String lastError() { return lastError; }

    public FlexConfiguration editable() { return editable; }
    public Path path() { return path; }

    public boolean load() {
        try {
            if (!Files.exists(path)) { validFile=true; lastRead=null; lastError=null; return true; }
            String contents=Files.readString(path, StandardCharsets.UTF_8);
            JsonObject document = JsonParser.parseString(contents).getAsJsonObject();
            // Older files only had a boolean. Respect a saved "off" choice, then drop that field on save.
            if (document.has("showHud") && !document.has("hudPosition")) {
                document.addProperty("hudPosition", document.get("showHud").getAsBoolean() ? "TOP_LEFT" : "OFF");
            }
            FlexConfiguration loaded = JSON.fromJson(document, FlexConfiguration.class);
            validate(loaded);
            editable = loaded;
            validFile = true;
            lastRead = contents;
            lastError = null;
            return true;
        } catch (RuntimeException | IOException exception) {
            validFile = false;
            lastError = exception.getMessage();
            LoggerFactory.getLogger("litematica-flex").error("Configuration rejected; original file preserved: {}", path, exception);
            return false;
        }
    }

    public void save() {
        if (!validFile) throw new IllegalStateException("Fix the invalid JSON file and reload before saving: " + path);
        validate(editable);
        try {
            String disk = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : null;
            if (!java.util.Objects.equals(disk,lastRead)) {
                throw new IllegalStateException("配置文件已在外部修改，请先重新载入，避免覆盖文件中的更改。");
            }
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            String contents=JSON.toJson(editable);
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            if (disk != null) Files.copy(path,path.resolveSibling(path.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            lastRead=contents;
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    private static void validate(FlexConfiguration config) {
        if (config == null || config.schemaVersion != 1 || config.global == null
                || config.savedProfiles == null || config.hotkeys == null) throw new IllegalArgumentException("Unsupported configuration");
        try { HudPosition.valueOf(config.hudPosition); }
        catch (IllegalArgumentException | NullPointerException e) { throw new IllegalArgumentException("Invalid HUD position",e); }
        if (config.hudOffsetX < 0 || config.hudOffsetX > 500 || config.hudOffsetY < 0 || config.hudOffsetY > 500)
            throw new IllegalArgumentException("Invalid HUD margin");
        config.hotkeys.forEach((key,value) -> { if(key==null || value==null)throw new IllegalArgumentException("Invalid hotkey"); });
        validate(config.global);
        config.savedProfiles.values().forEach(ConfigurationStore::validate);
    }

    private static void validate(RuleProfile profile) {
        if (profile == null || profile.enabledGroups == null || profile.ignoredProperties == null || profile.strictBlocks == null
                || profile.blacklistBlocks == null || profile.blacklistPresets == null || profile.replacements == null || profile.customGroups == null || profile.temporaryTargets == null
                || !java.util.Set.of("HELD_FIRST","ORIGINAL_FIRST","FIXED_ONLY").contains(profile.selection == null ? "" : profile.selection)
                || !Float.isFinite(profile.hardnessTolerance) || profile.hardnessTolerance < 0) {
            throw new IllegalArgumentException("Invalid rule profile");
        }
        for(String id:profile.blacklistBlocks)if(id==null || !id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw new IllegalArgumentException("Invalid blacklist block ID: "+id);
        profile.copy(); // Validates nested collections, including null entries.
    }
}
