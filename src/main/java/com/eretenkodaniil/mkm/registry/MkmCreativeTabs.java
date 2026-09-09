package com.eretenkodaniil.mkm.registry;

import com.eretenkodaniil.mkm.MkmMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Owns the dedicated MKM creative inventory tab.
 *
 * <p>The tab is intentionally wired to {@link MkmItems}; registered MKM items will appear here
 * automatically without each item needing separate creative-tab code.</p>
 */
public final class MkmCreativeTabs {
    public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MkmMod.MOD_ID, "main"));

    private MkmCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MkmCreativeTabs::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(
                MAIN,
                CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.mkm.main"))
                        .icon(() -> new ItemStack(MkmItems.CODEX.get()))
                        .withSearchBar()
                        .displayItems(MkmItems.ITEMS.getEntries())
                        .build()));
    }
}
