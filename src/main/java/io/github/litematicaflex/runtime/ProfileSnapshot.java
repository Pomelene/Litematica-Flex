package io.github.litematicaflex.runtime;

import io.github.litematicaflex.config.*;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Immutable world-space view used by render workers. Never consult live placement collections there. */
public record ProfileSnapshot(RuleProfile global, Map<String, RuleProfile> placements,
                              List<Bounds> bounds, List<Region> regions) {
    public record Bounds(String placement, int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        public boolean contains(BlockPos p) {
            return p.getX() >= minX && p.getX() <= maxX && p.getY() >= minY && p.getY() <= maxY && p.getZ() >= minZ && p.getZ() <= maxZ;
        }
    }
    public record Region(Bounds bounds, RuleProfile profile) {}

    public RuleProfile resolve(BlockPos position, String explicitPlacement) {
        String placement = explicitPlacement;
        if (placement == null && position != null) {
            for (Bounds bound : bounds) if (bound.contains(position)) { placement = bound.placement(); break; }
        }
        if (placement != null && position != null) {
            for (int i = regions.size()-1; i >= 0; i--) {
                Region region = regions.get(i);
                if (region.bounds().placement().equals(placement) && region.bounds().contains(position)) return region.profile();
            }
        }
        return placements.getOrDefault(placement == null ? "" : placement, global);
    }
}
