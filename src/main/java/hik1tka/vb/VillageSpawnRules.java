package hik1tka.vb;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class VillageSpawnRules {
    public static final double RAIDER_OUTER_BUFFER = 16.0D;

    private static final int OUTWARD_STEP = 4;
    private static final int MAX_OUTWARD_DISTANCE = 160;
    private static final int LOCAL_SEARCH_RADIUS = 12;
    private static final int GROUND_SCAN_VERTICAL = 20;
    private static final int MAX_VERTICAL_LIFT = 12;

    private VillageSpawnRules() {
    }

    public static boolean shouldRelocateRaiderSpawn(ServerWorld world, BlockPos pos) {
        VillageState state = VillageState.getServerState(world);
        return state.isPositionProtected(pos, RAIDER_OUTER_BUFFER);
    }

    public static BlockPos findRelocatedRaiderSpawn(ServerWorld world, BlockPos originalPos) {
        VillageState state = VillageState.getServerState(world);

        VillageData targetVillage = null;
        for (VillageData data : state.getAllVillages()) {
            if (data.getPoints() == null || data.getPoints().size() < 3) {
                continue;
            }

            if (VillageGeometryUtil.isInsideOrNearPolygon(originalPos, data.getPoints(), RAIDER_OUTER_BUFFER)) {
                targetVillage = data;
                break;
            }
        }

        if (targetVillage == null) {
            return originalPos;
        }

        BlockPos base = moveOutsideProtectedZone(targetVillage, originalPos);
        if (base == null) {
            return null;
        }

        BlockPos valid = findNearbyValidGround(world, base, LOCAL_SEARCH_RADIUS);
        if (valid != null && !state.isPositionProtected(valid, RAIDER_OUTER_BUFFER)) {
            return valid;
        }

        return null;
    }

    private static BlockPos moveOutsideProtectedZone(VillageData village, BlockPos originalPos) {
        double centerX = village.getCenter().getX() + 0.5;
        double centerZ = village.getCenter().getZ() + 0.5;
        double startX = originalPos.getX() + 0.5;
        double startZ = originalPos.getZ() + 0.5;

        double dirX = startX - centerX;
        double dirZ = startZ - centerZ;

        double[] normalized = VillageGeometryUtil.normalizeXZ(dirX, dirZ);
        dirX = normalized[0];
        dirZ = normalized[1];

        for (int dist = 0; dist <= MAX_OUTWARD_DISTANCE; dist += OUTWARD_STEP) {
            int x = (int) Math.floor(startX + dirX * dist);
            int z = (int) Math.floor(startZ + dirZ * dist);
            BlockPos candidate = new BlockPos(x, originalPos.getY(), z);

            if (!VillageGeometryUtil.isInsideOrNearPolygon(candidate, village.getPoints(), RAIDER_OUTER_BUFFER)) {
                return candidate;
            }
        }

        return null;
    }

    private static BlockPos findNearbyValidGround(ServerWorld world, BlockPos base, int radius) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r != 0 && Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }

                    BlockPos around = base.add(dx, 0, dz);
                    BlockPos groundBase = findGround(world, around, GROUND_SCAN_VERTICAL);
                    if (groundBase == null) {
                        continue;
                    }

                    BlockPos liftedSpawn = liftSpawnAboveObstacles(world, groundBase, MAX_VERTICAL_LIFT);
                    if (liftedSpawn == null) {
                        continue;
                    }

                    if (isSpawnSpaceValid(world, liftedSpawn)) {
                        return liftedSpawn;
                    }
                }
            }
        }

        return null;
    }

    private static BlockPos findGround(ServerWorld world, BlockPos around, int verticalRange) {
        int startY = around.getY() + verticalRange;
        int endY = around.getY() - verticalRange;

        for (int y = startY; y >= endY; y--) {
            BlockPos solid = new BlockPos(around.getX(), y, around.getZ());
            BlockState solidState = world.getBlockState(solid);

            if (!solidState.blocksMovement()) {
                continue;
            }

            if (solidState.isIn(BlockTags.LEAVES)) {
                continue;
            }

            if (!world.getFluidState(solid).isEmpty()) {
                continue;
            }

            return solid.up();
        }

        return null;
    }

    private static BlockPos liftSpawnAboveObstacles(ServerWorld world, BlockPos startPos, int maxLift) {
        for (int up = 0; up <= maxLift; up++) {
            BlockPos candidate = startPos.up(up);

            if (!isSpawnVolumeClear(world, candidate)) {
                continue;
            }

            return candidate;
        }

        return null;
    }

    private static boolean isSpawnVolumeClear(ServerWorld world, BlockPos pos) {
        return isCellFree(world, pos) && isCellFree(world, pos.up());
    }

    private static boolean isCellFree(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        return state.getCollisionShape(world, pos).isEmpty()
                && world.getFluidState(pos).isEmpty();
    }

    private static boolean isSpawnSpaceValid(ServerWorld world, BlockPos pos) {
        BlockPos ground = pos.down();
        BlockState groundState = world.getBlockState(ground);

        return isSpawnVolumeClear(world, pos)
                && groundState.blocksMovement()
                && !groundState.isIn(BlockTags.LEAVES)
                && world.getFluidState(ground).isEmpty();
    }
}