package io.github.litematicaflex.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Additive extension registry. Registration is intended for initialization, before worlds load. */
public final class FlexApi {
    public static final FlexApi INSTANCE = new FlexApi();
    private final CopyOnWriteArrayList<EquivalenceRule> rules = new CopyOnWriteArrayList<>();

    private FlexApi() {}

    public void registerRule(EquivalenceRule rule) {
        if (rule.id() == null || rule.id().isBlank()) throw new IllegalArgumentException("Rule ID is required");
        if (rules.stream().anyMatch(existing -> existing.id().equals(rule.id()))) {
            throw new IllegalArgumentException("Duplicate rule ID: " + rule.id());
        }
        rules.add(rule);
    }

    public List<EquivalenceRule> rules() { return List.copyOf(rules); }
}
