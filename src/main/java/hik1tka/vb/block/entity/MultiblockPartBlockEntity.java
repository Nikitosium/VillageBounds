package hik1tka.vb.block.entity;

import hik1tka.vb.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class MultiblockPartBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos controllerPos;

    public MultiblockPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_PART, pos, state);
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (controllerPos != null) {
            nbt.putInt("ControllerX", controllerPos.getX());
            nbt.putInt("ControllerY", controllerPos.getY());
            nbt.putInt("ControllerZ", controllerPos.getZ());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("ControllerX")) {
            controllerPos = new BlockPos(nbt.getInt("ControllerX"), nbt.getInt("ControllerY"), nbt.getInt("ControllerZ"));
        } else {
            controllerPos = null;
        }
    }
}