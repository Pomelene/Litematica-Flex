package io.github.litematicaflex.rules;

public record MatchResult(boolean accepted, boolean exact, String reason) {
    public static final MatchResult REJECTED = new MatchResult(false, false, "mismatch");
    public static final MatchResult EXACT = new MatchResult(true, true, "exact");
    public static MatchResult substitute(String reason) { return new MatchResult(true, false, reason); }
}
