package com.eretenkodaniil.mkm.registry;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.environment.RuneSlateBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Six original 16px decorative blocks. No tickable block entities or global renderer. */
public final class MkmBlocks {
    public static final Block AGED_ASHLAR = register("aged_ashlar", false);
    public static final Block MOSSY_ASHLAR = register("mossy_ashlar", false);
    public static final Block CRACKED_ASHLAR = register("cracked_ashlar", false);
    public static final Block CHARRED_TIMBER = register("charred_timber", false);
    public static final Block RUSTED_IRON = register("rusted_iron", false);
    public static final Block RUNE_SLATE = register("rune_slate", true);

    private MkmBlocks() {}

    private static Block register(String path, boolean rune) {
        var id = MkmMod.id(path);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties properties =
                BlockBehaviour.Properties.of().setId(blockKey).strength(2.5F, 6.0F);
        Block block = rune ? new RuneSlateBlock(properties) : new Block(properties);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
        return block;
    }

    public static void initialize() {
        // Force class initialization before registry freeze; never create ItemStacks here.
    }
}
