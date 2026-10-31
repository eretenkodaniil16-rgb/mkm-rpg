package com.eretenkodaniil.mkm.client.animation;

import java.util.Map;

/**
 * Maps semantic combat actions to clip IDs. Adding weapons does not alter the
 * packet protocol or hardcode fresh pose switches into Minecraft Mixins.
 */
public record AnimationProfile(String id, Map<String, String> clips) {
    public AnimationProfile {
        if (id == null || !id.matches("mkm:[a-z0-9_./-]+")
                || clips == null || clips.isEmpty() || clips.size() > 128) {
            throw new IllegalArgumentException("Invalid animation profile");
        }
        clips = Map.copyOf(clips);
        for (var entry : clips.entrySet()) {
            if (!entry.getKey().matches("[a-z0-9_]+")
                    || !entry.getValue().matches("mkm:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Invalid action/clip mapping");
            }
        }
    }

    public String clipFor(String action) {
        return clips.get(action);
    }
}
