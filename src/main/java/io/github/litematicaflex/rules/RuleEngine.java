package io.github.litematicaflex.rules;

import io.github.litematicaflex.api.*;
import io.github.litematicaflex.config.RuleProfile;
import java.util.*;

/** Pairwise evaluation: enabling overlapping groups never computes a transitive closure. */
public final class RuleEngine {
    public MatchResult compare(BlockDescription expected, BlockDescription actual, RuleProfile profile) {
        if (expected.id().equals(actual.id()) && expected.properties().equals(actual.properties())) return MatchResult.EXACT;
        if (!profile.enabled || profile.strictReview || expected.air() || actual.air()
                || expected.fluid() || actual.fluid()) return MatchResult.REJECTED;
        if (BlacklistPresets.excludes(profile,expected.id()) || BlacklistPresets.excludes(profile,actual.id())) return MatchResult.REJECTED;
        boolean sameBlock = expected.id().equals(actual.id());
        if (!sameBlock && !profile.allowCrossShape && !expected.shape().equals(actual.shape())) return MatchResult.REJECTED;
        boolean plankLogPair = isPlankLogPair(expected,actual);
        if (plankLogPair && !profile.allowPlankLogReplacement) return MatchResult.REJECTED;
        if (!propertiesMatch(expected, actual, profile)) return MatchResult.REJECTED;
        if (sameBlock) return MatchResult.substitute("ignored_state");

        // Blacklists and common constraints have already been checked; this mode replaces kind rules.
        if (profile.allReplacements) {
            return MatchResult.substitute("all_replacements");
        }

        if(plankLogPair) return MatchResult.substitute("plank_log");

        // Explicit mappings are directional and can deliberately replace functional blocks.
        if (profile.replacements.getOrDefault(expected.id(), List.of()).contains(actual.id())) {
            return MatchResult.substitute("mapping");
        }
        for (Set<String> group : profile.customGroups) {
            if (group.contains(expected.id()) && group.contains(actual.id())) return MatchResult.substitute("custom_group");
        }
        for (String group : expected.groups()) {
            // Legacy broad shape toggles are superseded by the explicit all-replacements switch.
            if(group.startsWith("shape."))continue;
            boolean enabled = profile.enabledGroups.stream().anyMatch(key -> group.equals(key) || group.startsWith(key + "."));
            if (enabled && actual.groups().contains(group)) {
                return MatchResult.substitute(group);
            }
        }
        if (profile.hardnessMatching && expected.hardness() >= 0 && actual.hardness() >= 0
                && Math.abs(expected.hardness() - actual.hardness()) <= profile.hardnessTolerance) {
            return MatchResult.substitute("hardness");
        }
        for (EquivalenceRule rule : FlexApi.INSTANCE.rules()) {
            if (profile.enabledGroups.contains(rule.id()) && rule.matches(expected, actual, profile)) {
                return MatchResult.substitute(rule.id());
            }
        }
        return MatchResult.REJECTED;
    }

    private boolean isPlankLogPair(BlockDescription a,BlockDescription b) {
        return (a.groups().contains("wood.planks") && isLog(b)) || (b.groups().contains("wood.planks") && isLog(a));
    }
    private boolean isLog(BlockDescription b) {
        return b.groups().stream().anyMatch(g -> Set.of("wood.log","wood.wood","wood.stripped_log","wood.stripped_wood").contains(g));
    }

    /** Classifies state errors without authorizing placement or accepting the block as completed. */
    public boolean materialsMatch(BlockDescription expected,BlockDescription actual,RuleProfile profile) {
        if(expected.air() || actual.air() || expected.fluid() || actual.fluid())return false;
        return compare(withoutProperties(expected),withoutProperties(actual),profile).accepted();
    }
    private BlockDescription withoutProperties(BlockDescription b) {
        return new BlockDescription(b.id(),b.shape(),b.groups(),Map.of(),b.hardness(),b.air(),b.fluid(),b.functional());
    }

    private boolean propertiesMatch(BlockDescription expected, BlockDescription actual, RuleProfile profile) {
        Set<String> names = new HashSet<>(expected.properties().keySet());
        names.addAll(actual.properties().keySet());
        for (String name : names) {
            if (profile.ignoredProperties.contains(name)) continue;
            // Directionless substitutes have no axis to preserve; shared properties remain strict.
            if (!expected.id().equals(actual.id()) && (!expected.properties().containsKey(name) || !actual.properties().containsKey(name))) continue;
            if (!Objects.equals(expected.properties().get(name), actual.properties().get(name))) return false;
        }
        return true;
    }
}
