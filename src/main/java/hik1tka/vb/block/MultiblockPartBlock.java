package hik1tka.vb.block;

import hik1tka.vb.block.entity.MultiblockPartBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Універсальний "заповнювач" мультиблока: сам нічого не вирішує, лише тримає
 * колізію (форма передається в конструктор - різна для різних ролей клітинки)
 * і перенаправляє клік/ламання на головний (controller) блок.
 * Повністю перевикористовуваний для будь-яких майбутніх мультиблоків.
 */
public class MultiblockPartBlock extends Block implements BlockEntityProvider {

    private final VoxelShape shape;

    public MultiblockPartBlock(Settings settings, VoxelShape shape) {
        super(settings);
        this.shape = shape;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockPartBlockEntity(pos, state);
    }

    // --- Клік по частині -> перенаправляємо на контролер (GUI і т.д.) ---

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof MultiblockPartBlockEntity partEntity)) {
            return ActionResult.PASS;
        }
        BlockPos controllerPos = partEntity.getControllerPos();
        if (controllerPos == null) {
            return ActionResult.PASS;
        }
        BlockState controllerState = world.getBlockState(controllerPos);
        BlockHitResult redirectedHit = new BlockHitResult(
                hit.getPos(), hit.getSide(), controllerPos, hit.isInsideBlock());
        return controllerState.onUse(world, player, hand, redirectedHit);
    }

    // --- Гравець НЕ може зламати частину напряму (тільки контролер) ---

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        return player.isCreative() ? super.calcBlockBreakingDelta(state, player, world, pos) : 0.0f;
    }

    // --- Якщо частину все ж прибрали (вибух, creative, поршень) - розібрати все ---

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient
                && world.getBlockEntity(pos) instanceof MultiblockPartBlockEntity partEntity) {
            BlockPos controllerPos = partEntity.getControllerPos();
            if (controllerPos != null
                    && world.getBlockState(controllerPos).getBlock() instanceof MultiblockController controller) {
                controller.disassemble(world, controllerPos, pos);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}