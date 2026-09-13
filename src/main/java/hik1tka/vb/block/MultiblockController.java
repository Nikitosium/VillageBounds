package hik1tka.vb.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public interface MultiblockController {

    void disassemble(World world, BlockPos controllerPos, BlockPos triggeringPos);
}