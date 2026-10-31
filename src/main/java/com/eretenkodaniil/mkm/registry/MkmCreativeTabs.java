package com.eretenkodaniil.mkm.registry;

import com.eretenkodaniil.mkm.MkmMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

public final class MkmCreativeTabs {
    public static final CreativeModeTab MAIN = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, MkmMod.id("main"),
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mkm.main"))
                    .icon(() -> MkmItems.CODEX.getDefaultInstance())
                    .displayItems((params, output) -> output.accept(MkmItems.CODEX))
                    .build());

    private MkmCreativeTabs() {}

    public static void initialize() {
        MAIN.getDisplayName();
    }
}
