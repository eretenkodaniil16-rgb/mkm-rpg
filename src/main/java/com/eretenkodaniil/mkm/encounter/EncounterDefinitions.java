package com.eretenkodaniil.mkm.encounter;

import com.eretenkodaniil.mkm.MkmMod;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

/** Server datapack configuration, validated on initial load and /reload. */
public final class EncounterDefinitions {
    private static volatile EncounterDefinition ford;
    private EncounterDefinitions() {}

    public static EncounterDefinition ford() { return ford; }

    public static void initialize() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override public Identifier getFabricId() {
                        return MkmMod.id("encounter_definitions");
                    }
                    @Override public void reload(ResourceManager manager) {
                        // Never reuse stale config after an invalid datapack reload.
                        ford = null;
                        var file = manager.getResource(MkmMod.id("encounter/ford_ambush.json"));
                        if (file.isEmpty()) {
                            MkmMod.LOGGER.error("Missing datapack encounter: mkm:encounter/ford_ambush.json");
                            return;
                        }
                        try (Reader reader = file.get().openAsReader()) {
                            EncounterDefinition parsed = EncounterDefinition.CODEC
                                    .parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
                            ford = parsed;
                            MkmMod.LOGGER.info("Loaded encounter {} with {} mobs", parsed.id(), parsed.roster().size());
                        } catch (Exception e) {
                            MkmMod.LOGGER.error("Invalid Ford encounter datapack; new starts disabled", e);
                        }
                    }
                });
    }
}
