package io.github.litematicaflex.api;

import io.github.litematicaflex.config.RuleProfile;

/** Register through FlexApi during the litematica-flex entrypoint. Rules never imply transitive equivalence. */
public interface EquivalenceRule {
    String id();
    boolean matches(BlockDescription expected, BlockDescription actual, RuleProfile profile);
}
