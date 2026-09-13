package hik1tka.vb.registry;

import hik1tka.vb.block.entity.BallistaBaseBlockEntity;
import hik1tka.vb.block.entity.MultiblockPartBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<BallistaBaseBlockEntity> BALLISTA_BASE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier("villagebounds", "ballista"),
            FabricBlockEntityTypeBuilder.create(BallistaBaseBlockEntity::new, ModBlocks.BALLISTA_BASE).build()
    );

    public static void registerModBlockEntities() {

        System.out.println("Registering Mod Block Entities for VillageBounds...");
    }

    public static final BlockEntityType<MultiblockPartBlockEntity> MULTIBLOCK_PART = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier("villagebounds", "multiblock_part"),
            FabricBlockEntityTypeBuilder.create(MultiblockPartBlockEntity::new,
                    ModBlocks.SIEGE_WORKBENCH_BASE,
                    ModBlocks.SIEGE_WORKBENCH_BASE_ROPE,
                    ModBlocks.SIEGE_WORKBENCH_BASE_ANVIL
            ).build()
    );
}
