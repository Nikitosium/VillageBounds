package hik1tka.vb.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import hik1tka.vb.VillageSpawnRules;
import hik1tka.vb.client.VillageBoundsClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VillageBoundsRender {

    private static final int WALL_CENTER_TOLERANCE = 5;
    private static final int WALL_HALF_HEIGHT = 5;

    private static final List<BlockPos> cachedSpecial = new ArrayList<>();
    private static int scanTickCounter = 0;
    private static final int CORNER_SEGMENTS = 16;

    // Пороги для Offset Polygon алгоритму (не завʼязані на знакові евристики,
    // лише на числову стійкість перевірки "майже нуль")
    private static final double CONVEX_EPS = 1e-9;
    private static final double PARALLEL_EPS = 1e-9;

    public static Matrix4f beginRender(WorldRenderContext context, MinecraftClient client) {
        // Оновлюємо лічильник тактів тут, раз на 2 секунди оновлюємо кеш ліжок
        if (scanTickCounter++ >= 40) {
            updateSpecialCache(client);
            scanTickCounter = 0;
        }

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return null;

        Vec3d cameraPos = context.camera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        return matrix;
    }

    public static void endRender(WorldRenderContext context) {
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        MatrixStack matrices = context.matrixStack();
        if (matrices != null) {
            matrices.pop();
        }
    }

    //Метод сканування світу навколо гравця на наявність ліжок та дзвонів
    public static void updateSpecialCache(MinecraftClient client) {
        cachedSpecial.clear();

        BlockPos playerPos = client.player.getBlockPos();
        int radius = 64;

        for (BlockPos pos : BlockPos.iterate(playerPos.add(-radius, -16, -radius), playerPos.add(radius, 16, radius))) {
            BlockState state = client.world.getBlockState(pos);
            if (state.isOf(Blocks.BELL) || state.isIn(BlockTags.BEDS)) {
                cachedSpecial.add(pos.toImmutable());
            }
        }
    }


    //Метод, який бере знайдений кеш ліжок/дзвонів і малює навколо них кольорові кубики
    public static void renderSpecialBlocks(Matrix4f matrix, MinecraftClient client) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : cachedSpecial) {
            if (client.world.getBlockState(pos).isOf(Blocks.BELL)) {
                drawSimpleBox(buffer, matrix, pos, 1.0f, 0.85f, 0.15f, 0.40f);
            } else {
                drawSimpleBox(buffer, matrix, pos, 1.0f, 0.15f, 0.15f, 0.30f);
            }
        }
        tessellator.draw();
    }


    //Рендерер твоїх зелених точок і прозорих стін для проєкту
    public static void renderCurrentProject(Matrix4f matrix, ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList pointsNbt = nbt.getList("Points", NbtElement.COMPOUND_TYPE);

        if (pointsNbt.isEmpty()) {
            return;
        }

        List<BlockPos> points = new ArrayList<>();
        for (int i = 0; i < pointsNbt.size(); i++) {
            points.add(NbtHelper.toBlockPos(pointsNbt.getCompound(i)));
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : points) {
            drawSimpleBox(buffer, matrix, pos, 0.20f, 1.00f, 0.20f, 0.50f);
        }
        tessellator.draw();

        if (points.size() >= 2) {
            WallYRange range = computeWallYRange(points);

            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < points.size(); i++) {
                BlockPos p1 = points.get(i);
                BlockPos p2 = points.get((i + 1) % points.size());
                renderWall(buffer, matrix, p1, p2, range.minY(), range.maxY(), 0.20f, 1.00f, 0.20f, 0.30f);
            }
            tessellator.draw();
        }

        if (nbt.contains("VillageCenter")) {
            BlockPos center = NbtHelper.toBlockPos(nbt.getCompound("VillageCenter"));
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            drawSimpleBox(buffer, matrix, center, 0.20f, 0.60f, 1.00f, 0.45f);
            tessellator.draw();
        }
    }


    //Рендерер уже створеного/збереженого селища
    public static void renderSingleVillage(Matrix4f matrix, VillageBoundsClient.ClientVillage village) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        drawSimpleBox(buffer, matrix, village.center(), 0.20f, 0.60f, 1.00f, 0.35f);
        for (BlockPos pos : village.points()) {
            drawSimpleBox(buffer, matrix, pos, 0.10f, 0.75f, 1.00f, 0.25f);
        }
        tessellator.draw();

        List<BlockPos> points = village.points();
        if (points.size() < 2) {
            return;
        }

        WallYRange range = computeWallYRange(points);

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < points.size(); i++) {
            BlockPos p1 = points.get(i);
            BlockPos p2 = points.get((i + 1) % points.size());
            renderWall(buffer, matrix, p1, p2, range.minY(), range.maxY(), 0.10f, 0.75f, 1.00f, 0.18f);
        }
        tessellator.draw();
    }


    //Рендерер буферних зон (Offset Polygon / Polygon Buffer з Round Join)
    public static void renderBufferZones(Matrix4f matrix) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        for (VillageBoundsClient.ClientVillage village : VillageBoundsClient.CLIENT_VILLAGES) {
            List<BlockPos> rawPoints = village.points();
            if (rawPoints.size() < 3) continue;

            double bufferSize = VillageSpawnRules.RAIDER_OUTER_BUFFER;
            WallYRange range = computeWallYRange(rawPoints);

            // Переносимо вершини у 2D (X,Z) площину
            List<Point2D> points = new ArrayList<>();
            for (BlockPos pos : rawPoints) {
                points.add(Point2D.of(pos));
            }

            // --- 1. Нормалізація орієнтації полігона ---
            // Далі весь алгоритм працює виключно з CW-полігоном, без розгалуження логіки.
            if (GeometryUtil.signedArea(points) > 0) {
                Collections.reverse(points);
            }

            int n = points.size();

            // --- 2. Напрямні вектори ребер, зовнішні нормалі та offset-ребра ---
            Point2D[] direction = new Point2D[n];
            OffsetEdge[] offsetEdges = new OffsetEdge[n];

            for (int i = 0; i < n; i++) {
                Point2D a = points.get(i);
                Point2D b = points.get((i + 1) % n);

                Point2D dir = b.sub(a).normalize();
                // Для CW-полігона зовнішня нормаль отримується поворотом напрямного вектора на -90°
                Point2D outwardNormal = new Point2D(-dir.z(), dir.x());
                Point2D offset = outwardNormal.scale(bufferSize);

                direction[i] = dir;
                offsetEdges[i] = new OffsetEdge(a.add(offset), b.add(offset), dir);
            }

            // --- 3. Класифікація кожної вершини: опукла чи увігнута/пряма ---
            boolean[] convex = new boolean[n];
            Point2D[] jointPoint = new Point2D[n];

            for (int i = 0; i < n; i++) {
                int prev = (i - 1 + n) % n;
                Point2D dPrev = direction[prev];
                Point2D dCurr = direction[i];

                // Від'ємний зовнішній добуток напрямних векторів відповідає повороту
                // за годинниковою стрілкою, тобто опуклому куту для CW-полігона.
                convex[i] = GeometryUtil.cross(dPrev, dCurr) < -CONVEX_EPS;

                if (!convex[i]) {
                    // Увігнутий (або майже прямий) кут: шукаємо перетин offset-прямих,
                    // щоб обрізати сусідні ребра точно у стику без арки.
                    jointPoint[i] = GeometryUtil.lineIntersection(
                            offsetEdges[prev].start(), dPrev,
                            offsetEdges[i].start(), dCurr
                    );
                }
            }

            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            // --- 4. Прямі offset-стіни, обрізані по стикових точках вершин ---
            for (int i = 0; i < n; i++) {
                int nextIndex = (i + 1) % n;

                Point2D startPt = convex[i]
                        ? offsetEdges[i].start()
                        : (jointPoint[i] != null ? jointPoint[i] : offsetEdges[i].start());

                Point2D endPt = convex[nextIndex]
                        ? offsetEdges[i].end()
                        : (jointPoint[nextIndex] != null ? jointPoint[nextIndex] : offsetEdges[i].end());

                renderWallRaw(buffer, matrix,
                        (float) startPt.x(), (float) startPt.z(),
                        (float) endPt.x(), (float) endPt.z(),
                        range.minY(), range.maxY(), 1.0f, 0.9f, 0.2f, 0.25f);
            }

            // --- 5. З'єднання у вершинах: Round Join на опуклих кутах ---
            for (int i = 0; i < n; i++) {
                int prev = (i - 1 + n) % n;
                Point2D arcStart = offsetEdges[prev].end();
                Point2D arcEnd = offsetEdges[i].start();

                if (convex[i]) {
                    // Центр дуги визначається геометрично - це сама вершина полігона,
                    // оскільки обидва offset-ребра віддалені від неї рівно на bufferSize.
                    Point2D center = points.get(i);

                    double angleStart = Math.atan2(arcStart.z() - center.z(), arcStart.x() - center.x());
                    double angleEnd = Math.atan2(arcEnd.z() - center.z(), arcEnd.x() - center.x());

                    renderArc(buffer, matrix, center, bufferSize, angleStart, angleEnd,
                            range.minY(), range.maxY(), 1.0f, 0.9f, 0.2f, 0.25f);
                } else if (jointPoint[i] == null) {
                    // Offset-прямі майже паралельні - перетин не визначений,
                    // з'єднуємо кінці offset-ребер коротким сегментом замість арки.
                    renderWallRaw(buffer, matrix,
                            (float) arcStart.x(), (float) arcStart.z(),
                            (float) arcEnd.x(), (float) arcEnd.z(),
                            range.minY(), range.maxY(), 1.0f, 0.9f, 0.2f, 0.25f);
                }
                // Якщо кут увігнутий і jointPoint визначений - ребра вже точно
                // сходяться у цій точці на кроці 4, додаткове з'єднання не потрібне.
            }

            tessellator.draw();
        }
    }


    // Малює дугу Round Join у CORNER_SEGMENTS сегментах.
    // Напрямок дуги завжди примусово встановлюється за годинниковою стрілкою
    // (від'ємне зростання кута), що є коректним напрямком для опуклого кута CW-полігона,
    // а не результатом порівняння "найкоротшого" шляху.
    private static void renderArc(BufferBuilder buffer, Matrix4f matrix, Point2D center, double radius,
                                   double angleStart, double angleEnd,
                                   float minY, float maxY, float r, float g, float b, float a) {
        double sweepAngle = angleEnd - angleStart;
        while (sweepAngle > 0) sweepAngle -= 2 * Math.PI;
        while (sweepAngle <= -2 * Math.PI) sweepAngle += 2 * Math.PI;

        double angleStep = sweepAngle / CORNER_SEGMENTS;
        for (int j = 0; j < CORNER_SEGMENTS; j++) {
            double a1 = angleStart + j * angleStep;
            double a2 = angleStart + (j + 1) * angleStep;

            float x1 = (float) (center.x() + Math.cos(a1) * radius);
            float z1 = (float) (center.z() + Math.sin(a1) * radius);
            float x2 = (float) (center.x() + Math.cos(a2) * radius);
            float z2 = (float) (center.z() + Math.sin(a2) * radius);

            renderWallRaw(buffer, matrix, x1, z1, x2, z2, minY, maxY, r, g, b, a);
        }
    }
    // Низькорівневий метод малювання стіни за готовими float координатами
    private static void renderWallRaw(BufferBuilder buffer, Matrix4f matrix,
                                      float x1, float z1, float x2, float z2,
                                      float minY, float maxY,
                                      float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, minY, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, minY, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, maxY, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, maxY, z1).color(r, g, b, a).next();
    }


    //Математичний розрахунок висоти стін
    private static WallYRange computeWallYRange(List<BlockPos> points) {
        int rawMinY = points.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int rawMaxY = points.stream().mapToInt(BlockPos::getY).max().orElse(0);

        float centerLineY = (rawMinY + rawMaxY) / 2.0f;

        boolean withinCentralBand = true;
        for (BlockPos pos : points) {
            if (Math.abs(pos.getY() - centerLineY) > WALL_CENTER_TOLERANCE) {
                withinCentralBand = false;
                break;
            }
        }

        if (withinCentralBand) {
            float minY = centerLineY - WALL_HALF_HEIGHT;
            float maxY = centerLineY + WALL_HALF_HEIGHT + 1.0f;
            return new WallYRange(minY, maxY);
        }

        return new WallYRange(rawMinY, rawMaxY + 1.0f);
    }


    //Малює стіну (площину) між 2-ма точками полігону
    private static void renderWall(BufferBuilder buffer, Matrix4f matrix,
                            BlockPos p1, BlockPos p2,
                            float minY, float maxY,
                            float r, float g, float b, float a) {
        float x1 = p1.getX() + 0.5f;
        float z1 = p1.getZ() + 0.5f;
        float x2 = p2.getX() + 0.5f;
        float z2 = p2.getZ() + 0.5f;

        buffer.vertex(matrix, x1, minY, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, minY, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, maxY, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, maxY, z1).color(r, g, b, a).next();
    }


    //Малювання кубика (мб буде змінено)
    private static void drawSimpleBox(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, float r, float g, float b, float a) {
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();
        float x2 = x + 1.0f;
        float y2 = y + 1.0f;
        float z2 = z + 1.0f;

        buffer.vertex(matrix, x, y, z).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y, z).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z).color(r, g, b, a).next();
        buffer.vertex(matrix, x, y2, z).color(r, g, b, a).next();

        buffer.vertex(matrix, x, y, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y, z2).color(r, g, b, a).next();

        buffer.vertex(matrix, x, y, z).color(r, g, b, a).next();
        buffer.vertex(matrix, x, y2, z).color(r, g, b, a).next();
        buffer.vertex(matrix, x, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x, y, z2).color(r, g, b, a).next();

        buffer.vertex(matrix, x2, y, z).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z).color(r, g, b, a).next();
    }


    //Контейнер
    private record WallYRange(float minY, float maxY) {
    }


    // Точка у горизонтальній площині (X, Z), використовується Offset Polygon алгоритмом
    private record Point2D(double x, double z) {

        static Point2D of(BlockPos pos) {
            return new Point2D(pos.getX() + 0.5, pos.getZ() + 0.5);
        }

        Point2D add(Point2D other) {
            return new Point2D(x + other.x, z + other.z);
        }

        Point2D sub(Point2D other) {
            return new Point2D(x - other.x, z - other.z);
        }

        Point2D scale(double s) {
            return new Point2D(x * s, z * s);
        }

        double length() {
            return Math.sqrt(x * x + z * z);
        }

        Point2D normalize() {
            double len = length();
            if (len < 1e-9) {
                return new Point2D(0, 0);
            }
            return new Point2D(x / len, z / len);
        }
    }


    // Offset-ребро: вихідне ребро полігона, зсунуте вздовж зовнішньої нормалі на bufferSize
    private record OffsetEdge(Point2D start, Point2D end, Point2D direction) {
    }


    // Допоміжна обчислювальна геометрія для Offset Polygon (Polygon Buffer) алгоритму
    private static final class GeometryUtil {

        private GeometryUtil() {
        }

        // Знакова площа полігона (формула Гаусса, подвоєна площа не потрібна для визначення знаку).
        // Додатне значення відповідає CCW-обходу, від'ємне - CW-обходу.
        static double signedArea(List<Point2D> pts) {
            double sum = 0;
            int n = pts.size();
            for (int i = 0; i < n; i++) {
                Point2D a = pts.get(i);
                Point2D b = pts.get((i + 1) % n);
                sum += a.x() * b.z() - b.x() * a.z();
            }
            return sum * 0.5;
        }

        // Z-компонента векторного добутку двох 2D-векторів
        static double cross(Point2D a, Point2D b) {
            return a.x() * b.z() - a.z() * b.x();
        }

        // Перетин двох прямих, кожна задана точкою на прямій та напрямним вектором.
        // Повертає null, якщо прямі майже паралельні (перетин не визначений).
        static Point2D lineIntersection(Point2D p1, Point2D d1, Point2D p2, Point2D d2) {
            double denom = cross(d1, d2);
            if (Math.abs(denom) < PARALLEL_EPS) {
                return null;
            }
            Point2D diff = p2.sub(p1);
            double t = cross(diff, d2) / denom;
            return p1.add(d1.scale(t));
        }
    }
}

