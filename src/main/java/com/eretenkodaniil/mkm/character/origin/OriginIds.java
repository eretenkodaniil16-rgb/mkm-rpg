package com.eretenkodaniil.mkm.character.origin;

import java.util.HashSet;
import java.util.List;

/** Stable first-party IDs. Never derive identifiers from a localized title. */
public final class OriginIds {
    public static final int MAX_ID_LENGTH = 64;
    private OriginIds() {}

    public static boolean valid(String id) {
        if (id == null || !id.startsWith("mkm:") || id.length() > MAX_ID_LENGTH) return false;
        String path = id.substring(4);
        return !path.isEmpty() && path.matches("[a-z0-9_-]+(?:/[a-z0-9_-]+)*");
    }

    public static void require(String id) {
        if (!valid(id)) throw new IllegalArgumentException("Invalid MKM origin ID: " + id);
    }

    static List<String> immutableIds(List<String> ids) {
        if (ids == null || ids.size() > 32) throw new IllegalArgumentException("Invalid feature list");
        List<String> copy = List.copyOf(ids);
        if (new HashSet<>(copy).size() != copy.size()) throw new IllegalArgumentException("Duplicate feature");
        for (String id : copy) require(id);
        return copy;
    }
}
