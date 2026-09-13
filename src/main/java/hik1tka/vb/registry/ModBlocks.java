package hik1tka.vb.registry;

import hik1tka.vb.block.BallistaBaseBlock;
import hik1tka.vb.block.MultiblockPartBlock;
import hik1tka.vb.block.SiegeWorkbenchBlock;
import hik1tka.vb.block.WoodMaterial;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block BALLISTA_BASE=registerBlock("ballista",
            new BallistaBaseBlock (AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));

    public static final Block SIEGE_WORCKBENCH=registerBlock("siege_workbench",
            new SiegeWorkbenchBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));

    private static final String[] VANILLA_WOOD_TYPES = {
            "oak_planks", "spruce_planks", "birch_planks", "jungle_planks",
            "acacia_planks", "dark_oak_planks", "mangrove_planks", "cherry_planks",
            "bamboo_planks", "crimson_planks", "warped_planks"
    };

    public static final Block SIEGE_WORKBENCH_BASE = registerBlock("siege_workbench_base",
            new MultiblockPartBlock(
                    AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque(),
                    Block.createCuboidShape(0, 0, 0, 16, 8, 16)
            ));

    public static final Block SIEGE_WORKBENCH_BASE_ROPE = registerBlock("siege_workbench_base_rope",
            new MultiblockPartBlock(
                    AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque(),
                    Block.createCuboidShape(0, 0, 0, 16, 8, 16)
            ));

    public static final Block SIEGE_WORKBENCH_BASE_ANVIL = registerBlock("siege_workbench_base_anvil",
            new MultiblockPartBlock(
                    AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque(),
                    Block.createCuboidShape(0, 0, 0, 16, 8, 16)
            ));

    private static Block registerBlock(String name, Block block) {
        Identifier id = new Identifier("villagebounds", name);

        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));

        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void registerModBlocks() {
        // Цей рядок просто змушує Java завантажити клас і виконати статичну реєстрацію вище
        System.out.println("Registering Mod Blocks for VillageBounds...");

        // Один Item, але у креативі показуємо його кілька разів - кожен раз
        // з іншим NBT-матеріалом всередині (так само, як vanilla робить
        // з зіллями чи заколдованими книгами - різні стеки одного Item-а).
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(content -> {
            for (String woodType : VANILLA_WOOD_TYPES) {
                Identifier material = new Identifier("minecraft", woodType);
                content.add(WoodMaterial.createStack(BALLISTA_BASE.asItem(), material));
            }
        });
    }

}
