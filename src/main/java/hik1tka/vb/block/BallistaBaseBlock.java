package hik1tka.vb.block;

import hik1tka.vb.block.entity.BallistaBaseBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Один Block без окремого BlockState на кожну деревину.
 * Матеріал (Identifier дощок) живе виключно у {@link BallistaBaseBlockEntity},
 * а не у BlockState - тому раніше наявний WOOD_TYPE (EnumProperty) прибрано повністю.
 * <p>
 * Цикл: Craft (ItemStack.NBT) -> Place ({@link #onPlaced}) -> BlockEntity ->
 * Break ({@link #onStateReplaced}) -> знову ItemStack з тим самим матеріалом.
 */
public class BallistaBaseBlock extends Block implements BlockEntityProvider {

    // Хітбокс залишається незмінним (6 пікселів у висоту)
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

    public BallistaBaseBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // --- BlockEntityProvider: тут живе матеріал ---

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BallistaBaseBlockEntity(pos, state);
    }

    // --- Craft -> Place: переносимо матеріал зі стеку у BlockEntity ---

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        // onPlaced викликається і на клієнті, і на сервері - BlockEntity
        // достовірно існує лише на сервері, клієнт отримає дані пізніше через sync-пакет.
        if (world.isClient) {
            return;
        }

        if (world.getBlockEntity(pos) instanceof BallistaBaseBlockEntity blockEntity) {
            blockEntity.setMaterial(WoodMaterial.readFromStack(itemStack));
        }
    }

    // Запам'ятовуємо, чи ламає гравець у creative - потрібно для onStateReplaced нижче,
    // бо той хук не отримує PlayerEntity. onBreak викликається ДО onStateReplaced
    // для одного й того ж акту ламання, тому це безпечно (сервер однопотоковий).
    private boolean breakingInCreative = false;

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        breakingInCreative = player.isCreative();
        super.onBreak(world, pos, state, player);
    }

    // --- Break: повертаємо ItemStack із тим самим матеріалом ---

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // state.isOf(newState.getBlock()) == false означає, що блок дійсно зникає
        // (а не просто змінює властивості) - саме тоді потрібно віддати дроп.
        // У creative дропати НЕ треба - раніше сюди falsely потрапляло навіть у creative,
        // бо перевірки player.isCreative() тут не було взагалі.
        if (!state.isOf(newState.getBlock())) {
            if (!world.isClient && !breakingInCreative
                    && world.getBlockEntity(pos) instanceof BallistaBaseBlockEntity blockEntity) {
                Block.dropStack(world, pos, createStackWithMaterial(blockEntity.getMaterial()));
            }
            // Скидаємо прапорець одразу, щоб він не "протікав" на інші зняття блоку,
            // що не є прямим ламанням гравцем (вибух, поршень тощо).
            breakingInCreative = false;
        }

        // BlockEntity видаляється рушієм лише ПІСЛЯ цього виклику,
        // тож дані вище ще гарантовано доступні.
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // --- Pick block (середня кнопка миші у творчому режимі) ---

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof BallistaBaseBlockEntity blockEntity) {
            return createStackWithMaterial(blockEntity.getMaterial());
        }
        return super.getPickStack(world, pos, state);
    }

    private ItemStack createStackWithMaterial(Identifier material) {
        return WoodMaterial.createStack(this.asItem(), material);
    }
}
