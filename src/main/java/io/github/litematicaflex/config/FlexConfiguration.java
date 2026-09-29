package io.github.litematicaflex.config;

import java.util.*;

public final class FlexConfiguration {
    public int schemaVersion = 2;
    public RuleProfile global = new RuleProfile();
    public Map<String, RuleProfile> savedProfiles = new LinkedHashMap<>();
    public Map<String, String> hotkeys = new LinkedHashMap<>();
    public String hudPosition = HudPosition.TOP_LEFT.name();
    public int hudOffsetX = 8;
    public int hudOffsetY = 8;
}
