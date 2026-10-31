package com.eretenkodaniil.mkm.registry;

import com.eretenkodaniil.mkm.MkmMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for MKM items.
 *
 * <p>New MKM items should be registered here (or delegated from this domain as the content set
 * grows). The MKM creative tab consumes this registry automatically.</p>
 */
public final class MkmItems {
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MkmMod.MOD_ID);

    /**
     * The first MKM item. It is deliberately simple for now and doubles as the icon/visibility
     * anchor for the dedicated creative tab. Later it can become the in-game RPG codex entry point.
     */
    public static final DeferredItem<Item> CODEX = ITEMS.registerSimpleItem("codex");

    private MkmItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
