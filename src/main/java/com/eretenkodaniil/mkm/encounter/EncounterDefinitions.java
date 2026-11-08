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

/** Separate, validated server datapacks; legacy Ford encounter stays unchanged. */
public final class EncounterDefinitions {
    private static volatile EncounterDefinition ford;
    private static volatile EncounterDefinition showcase;

    private EncounterDefinitions() {}
    public static EncounterDefinition ford() { return ford; }
    public static EncounterDefinition showcase() { return showcase; }

    private static EncounterDefinition parse(ResourceManager manager, String name, String id) {
        var file=manager.getResource(MkmMod.id("encounter/"+name+".json"));
        if (file.isEmpty()) {
            MkmMod.LOGGER.error("Missing encounter datapack: mkm:encounter/{}.json",name);
            return null;
        }
        try (Reader reader=file.get().openAsReader()) {
            EncounterDefinition result=EncounterDefinition.CODEC.parse(
                    JsonOps.INSTANCE,JsonParser.parseReader(reader)).getOrThrow();
            if (!result.id().equals(id)) throw new IllegalArgumentException(
                    "Datapack resource and definition ID mismatch: "+name+" / "+result.id());
            MkmMod.LOGGER.info("Loaded encounter {} with {} mobs",result.id(),result.roster().size());
            return result;
        } catch (Exception error) {
            MkmMod.LOGGER.error("Invalid encounter datapack: {}. New starts disabled for this type",name,error);
            return null;
        }
    }

    public static void initialize() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override public Identifier getFabricId() {
                        return MkmMod.id("encounter_definitions");
                    }
                    @Override public void onResourceManagerReload(ResourceManager manager) {
                        // Do not retain stale definitions after /reload.
                        ford=null;
                        showcase=null;
                        ford=parse(manager,"ford_ambush","mkm:ford_ambush");
                        showcase=parse(manager,"showcase_watchtower","mkm:showcase_watchtower");
                    }
                });
    }
}
