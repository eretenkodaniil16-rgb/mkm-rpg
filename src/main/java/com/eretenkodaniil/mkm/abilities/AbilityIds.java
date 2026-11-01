package com.eretenkodaniil.mkm.abilities;

import java.util.regex.Pattern;

/** Stable, namespace-scoped IDs; rejects path traversal and empty path segments. */
public final class AbilityIds {
    private static final Pattern SHAPE = Pattern.compile("mkm:[a-z0-9_./-]{1,54}");

    private AbilityIds() {}

    public static boolean valid(String id) {
        if (id == null || !SHAPE.matcher(id).matches()) return false;
        String[] segments = id.substring("mkm:".length()).split("/", -1);
        for (String part : segments) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) return false;
        }
        return true;
    }
}
