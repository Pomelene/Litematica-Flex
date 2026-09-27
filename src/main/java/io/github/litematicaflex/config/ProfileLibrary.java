package io.github.litematicaflex.config;

import com.google.gson.Gson;

/** Mutable deep copies for named profiles stored inside the single configuration file. */
public final class ProfileLibrary {
    private static final Gson JSON=new Gson();
    private ProfileLibrary() {}
    public static RuleProfile copy(RuleProfile profile) {
        return JSON.fromJson(JSON.toJson(profile),RuleProfile.class);
    }
}
