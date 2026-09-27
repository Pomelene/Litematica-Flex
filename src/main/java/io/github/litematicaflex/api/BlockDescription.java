package io.github.litematicaflex.api;

import java.util.Map;
import java.util.Set;

/** Immutable, Minecraft-independent input for rules and third-party extensions. */
public record BlockDescription(String id, String shape, Set<String> groups,
                               Map<String, String> properties, float hardness,
                               boolean air, boolean fluid, boolean functional) {
    public BlockDescription {
        groups = Set.copyOf(groups);
        properties = Map.copyOf(properties);
    }
}
