package io.github.litematicaflex.config;

import java.util.*;

/** JSON data model. Publish a copy when editing; render workers only see immutable snapshots. */
public final class RuleProfile {
    public boolean enabled = true;
    public boolean allReplacements = false;
    public boolean blacklistEnabled = true;
    public Set<String> blacklistBlocks = new LinkedHashSet<>();
    public Set<String> blacklistPresets = new LinkedHashSet<>(List.of("redstone","moving","obsidian","containers","copper_amethyst"));
    public boolean allowPlankLogReplacement = false;
    public boolean verification = true;
    public boolean rendering = true;
    public boolean placement = true;
    public boolean materialList = true;
    public boolean strictReview = false;
    public boolean allowCrossShape = false;
    public boolean hardnessMatching = false;
    public boolean showAcceptedOverlay = false;
    public float hardnessTolerance = 0.5f;
    public String selection = "HELD_FIRST";
    public Set<String> enabledGroups = new LinkedHashSet<>();
    public Set<String> ignoredProperties = new LinkedHashSet<>();
    public Set<String> strictBlocks = new LinkedHashSet<>();
    public Set<String> temporaryTargets = new LinkedHashSet<>();
    public Map<String, List<String>> replacements = new LinkedHashMap<>();
    public List<Set<String>> customGroups = new ArrayList<>();

    public RuleProfile copy() {
        RuleProfile p = new RuleProfile();
        p.enabled = enabled; p.verification = verification; p.rendering = rendering;
        p.allReplacements = allReplacements;
        p.blacklistEnabled = blacklistEnabled;
        p.blacklistBlocks = Set.copyOf(blacklistBlocks);
        p.blacklistPresets = Set.copyOf(blacklistPresets);
        p.allowPlankLogReplacement = allowPlankLogReplacement;
        p.placement = placement; p.materialList = materialList; p.strictReview = strictReview;
        p.allowCrossShape = allowCrossShape;
        p.hardnessMatching = hardnessMatching; p.hardnessTolerance = hardnessTolerance;
        p.showAcceptedOverlay = showAcceptedOverlay;
        p.selection = selection;
        p.enabledGroups = Set.copyOf(enabledGroups); p.ignoredProperties = Set.copyOf(ignoredProperties);
        p.strictBlocks = Set.copyOf(strictBlocks);
        p.temporaryTargets = Set.copyOf(temporaryTargets);
        Map<String, List<String>> mappings = new LinkedHashMap<>();
        replacements.forEach((key, value) -> mappings.put(key, List.copyOf(value)));
        p.replacements = Collections.unmodifiableMap(mappings);
        p.customGroups = customGroups.stream().map(Set::copyOf).toList();
        return p;
    }
}
