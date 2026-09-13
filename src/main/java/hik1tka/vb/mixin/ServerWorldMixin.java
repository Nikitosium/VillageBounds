package hik1tka.vb.mixin;

import hik1tka.vb.VillageSpawnRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void villagebounds$relocateRaiderSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof RaiderEntity raider)) {
            return;
        }

        ServerWorld world = (ServerWorld) (Object) this;
        BlockPos originalPos = entity.getBlockPos();

        if (!VillageSpawnRules.shouldRelocateRaiderSpawn(world, originalPos)) {
            return;
        }

        BlockPos relocated = VillageSpawnRules.findRelocatedRaiderSpawn(world, originalPos);
        if (relocated == null) {
            cir.setReturnValue(false);
            return;
        }

        raider.refreshPositionAndAngles(
                relocated.getX() + 0.5,
                relocated.getY(),
                relocated.getZ() + 0.5,
                raider.getYaw(),
                raider.getPitch()
        );
    }
}