package io.github.litematicaflex.runtime;

import io.github.litematicaflex.config.RuleProfile;
import net.minecraft.core.BlockPos;

/** Immutable rule view shared with render workers. All placements use one global profile. */
public record ProfileSnapshot(RuleProfile global) {
    public RuleProfile resolve(BlockPos position, String placement) { return global; }
}
