package io.github.litematicaflex.config;

import java.util.*;

public final class FlexConfiguration {
    public int schemaVersion = 1;
    public RuleProfile global = new RuleProfile();
    public Map<String, RuleProfile> placements = new LinkedHashMap<>();
    public List<RegionOverride> regions = new ArrayList<>();
    public Map<String, RuleProfile> savedProfiles = new LinkedHashMap<>();
    public Map<String, String> hotkeys = new LinkedHashMap<>();
    public boolean showHud = true;

    /** Inclusive world coordinates, tied to an explicit placement identity; later entries take priority. */
    public static final class RegionOverride {
        public String placement;
        public String name;
        public int minX, minY, minZ, maxX, maxY, maxZ;
        public RuleProfile profile = new RuleProfile();
        public boolean contains(int x,int y,int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }
}
