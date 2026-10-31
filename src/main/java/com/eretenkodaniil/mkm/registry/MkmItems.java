package com.eretenkodaniil.mkm.registry;

import com.eretenkodaniil.mkm.MkmMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class MkmItems {
    public static final Item CODEX = Registry.register(
            BuiltInRegistries.ITEM, MkmMod.id("codex"),
            new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MkmMod.id("codex")))));

    private MkmItems() {}

    public static void initialize() {
        // Calling this static method initializes MkmItems and registers CODEX.
        // Do NOT construct an ItemStack here: vanilla item components are not
        // bound until after early Fabric entrypoints have finished.
    }
}
