package com.eretenkodaniil.mkm.registry;

import com.eretenkodaniil.mkm.MkmMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public final class MkmCreativeTabs {
    public static final CreativeModeTab MAIN = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, MkmMod.id("main"),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mkm.main"))
                    .icon(() -> MkmItems.CODEX.getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(MkmItems.CODEX);
                        output.accept(MkmBlocks.AGED_ASHLAR);
                        output.accept(MkmBlocks.MOSSY_ASHLAR);
                        output.accept(MkmBlocks.CRACKED_ASHLAR);
                        output.accept(MkmBlocks.CHARRED_TIMBER);
                        output.accept(MkmBlocks.RUSTED_IRON);
                        output.accept(MkmBlocks.RUNE_SLATE);
                        output.accept(MkmItems.RAIDER_INSIGNIA);
                        output.accept(MkmItems.STALKER_FANG);
                    })
                    .build());

    private MkmCreativeTabs() {}

    public static void initialize() {
        // The static MAIN registration occurs during class initialization.
        // Leave the tab icon supplier lazy; never create ItemStacks here.
    }
}
