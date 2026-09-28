package io.github.litematicaflex.config;

import java.util.*;

public final class FlexConfiguration {
    public int schemaVersion = 1;
    public RuleProfile global = new RuleProfile();
    public Map<String, RuleProfile> savedProfiles = new LinkedHashMap<>();
    public Map<String, String> hotkeys = new LinkedHashMap<>();
    public boolean showHud = true;
}
