package io.github.litematicaflex.api;

/** Implemented on Litematica's verifier by the integration mixin. */
public interface VerificationSummary {
    int flexExactCount();
    int flexSubstitutionCount();
    int flexTemporaryCount();
    int flexSubstitutionStateErrors();
    java.util.Map<String,Integer> flexReasons();
}
