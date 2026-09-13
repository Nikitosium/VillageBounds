package hik1tka.vb;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class VillageGeometryUtil {
    private VillageGeometryUtil() {
    }

    public static boolean isInsideOrNearPolygon(BlockPos pos, List<BlockPos> polygon, double outerBuffer) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }

        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;

        if (isPointInsidePolygonXZ(x, z, polygon)) {
            return true;
        }

        double distance = minDistanceToPolygonEdgesXZ(x, z, polygon);
        return distance <= outerBuffer;
    }

    public static boolean isPointInsidePolygonXZ(double x, double z, List<BlockPos> polygon) {
        boolean inside = false;
        int size = polygon.size();

        for (int i = 0, j = size - 1; i < size; j = i++) {
            double xi = polygon.get(i).getX() + 0.5;
            double zi = polygon.get(i).getZ() + 0.5;
            double xj = polygon.get(j).getX() + 0.5;
            double zj = polygon.get(j).getZ() + 0.5;

            boolean intersect = ((zi > z) != (zj > z))
                    && (x < (xj - xi) * (z - zi) / ((zj - zi) + 1.0E-9) + xi);

            if (intersect) {
                inside = !inside;
            }
        }

        return inside;
    }

    public static double minDistanceToPolygonEdgesXZ(double x, double z, List<BlockPos> polygon) {
        double min = Double.MAX_VALUE;

        for (int i = 0; i < polygon.size(); i++) {
            BlockPos a = polygon.get(i);
            BlockPos b = polygon.get((i + 1) % polygon.size());

            double ax = a.getX() + 0.5;
            double az = a.getZ() + 0.5;
            double bx = b.getX() + 0.5;
            double bz = b.getZ() + 0.5;

            double dist = distancePointToSegmentXZ(x, z, ax, az, bx, bz);
            if (dist < min) {
                min = dist;
            }
        }

        return min;
    }

    public static double distancePointToSegmentXZ(double px, double pz, double ax, double az, double bx, double bz) {
        double abx = bx - ax;
        double abz = bz - az;
        double apx = px - ax;
        double apz = pz - az;

        double abLenSq = abx * abx + abz * abz;
        if (abLenSq <= 1.0E-9) {
            double dx = px - ax;
            double dz = pz - az;
            return Math.sqrt(dx * dx + dz * dz);
        }

        double t = (apx * abx + apz * abz) / abLenSq;
        t = Math.max(0.0, Math.min(1.0, t));

        double cx = ax + abx * t;
        double cz = az + abz * t;

        double dx = px - cx;
        double dz = pz - cz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double[] normalizeXZ(double x, double z) {
        double len = Math.sqrt(x * x + z * z);
        if (len <= 1.0E-9) {
            return new double[]{1.0, 0.0};
        }
        return new double[]{x / len, z / len};
    }

    public static double centroidX(List<BlockPos> polygon) {
        double sum = 0.0;
        for (BlockPos p : polygon) {
            sum += p.getX() + 0.5;
        }
        return sum / polygon.size();
    }

    public static double centroidZ(List<BlockPos> polygon) {
        double sum = 0.0;
        for (BlockPos p : polygon) {
            sum += p.getZ() + 0.5;
        }
        return sum / polygon.size();
    }
}