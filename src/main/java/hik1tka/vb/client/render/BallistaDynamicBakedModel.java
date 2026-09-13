package hik1tka.vb.client.render;

import hik1tka.vb.block.WoodMaterial;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.function.Supplier;

/**
 * Обгортка над звичайною (ванільно спеченою) BakedModel балісти.
 * <p>
 * Геометрія береться як є (та сама двобоксова модель з ballista_base.json),
 * але КОЖЕН квад під час рендеру перетекстуровується на "живий" спрайт
 * блока дощок відповідного матеріалу - тобто якщо у людини стоїть ресурс-пак,
 * що міняє текстуру oak_planks, балістра з дубу автоматично теж зміниться,
 * бо ми не зберігаємо картинку, а щоразу питаємо "яка зараз текстура у oak_planks".
 */
public class BallistaDynamicBakedModel implements FabricBakedModel, BakedModel {

    private static final RenderMaterial DEFAULT_MATERIAL =
            RendererAccess.INSTANCE.getRenderer().materialFinder().find();

    private final BakedModel wrapped;

    public BallistaDynamicBakedModel(BakedModel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean isVanillaAdapter() {
        // false = "я сам малюю квади через emitBlockQuads/emitItemQuads,
        // не використовуй мене як звичайну ванільну модель"
        return false;
    }

    // --- Рендер у світі (блок стоїть у чанку) ---

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                                Supplier<Random> randomSupplier, RenderContext context) {
        // ВАЖЛИВО: emitBlockQuads викликається під час чанк-бейкінгу, який Minecraft
        // виконує у ФОНОВОМУ потоці. Пряме blockView.getBlockEntity(pos) тут
        // не потокобезпечне (Fabric Rendering API прямо про це попереджає) і саме
        // через це матеріал і "губився", завжди відкочуючись на дефолтний дуб.
        // Замість цього читаємо заздалегідь підготовлений (у головному потоці)
        // render attachment - див. BallistaBaseBlockEntity#getRenderAttachmentData().
        Identifier material = WoodMaterial.DEFAULT;
        if (blockView instanceof RenderAttachedBlockView attachedView
                && attachedView.getBlockEntityRenderAttachment(pos) instanceof Identifier attached) {
            material = attached;
        }

        Sprite sprite = resolvePlankSprite(material);
        Sprite template = resolvePlankSprite(WoodMaterial.DEFAULT);
        Random random = randomSupplier.get();
        QuadEmitter emitter = context.getEmitter();

        for (Direction direction : DIRECTIONS_AND_NULL) {
            for (BakedQuad quad : wrapped.getQuads(state, direction, random)) {
                reTextureAndEmit(emitter, quad, sprite, template);
            }
        }
    }

    // --- Рендер у руці / інвентарі (просто ItemStack, блока в світі ще немає) ---

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        // У інвентарі BlockEntity не існує - матеріал читаємо прямо з NBT стеку.
        Identifier material = WoodMaterial.readFromStack(stack);

        Sprite sprite = resolvePlankSprite(material);
        Sprite template = resolvePlankSprite(WoodMaterial.DEFAULT);
        Random random = randomSupplier.get();
        QuadEmitter emitter = context.getEmitter();

        for (Direction direction : DIRECTIONS_AND_NULL) {
            for (BakedQuad quad : wrapped.getQuads(null, direction, random)) {
                reTextureAndEmit(emitter, quad, sprite, template);
            }
        }
    }

    private static final Direction[] DIRECTIONS_AND_NULL = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, null
    };

    /**
     * @param sprite   спрайт, яким ХОЧЕМО перемалювати квад (наприклад spruce_planks)
     * @param template спрайт, яким модель БУЛА спечена (oak_planks - той, що стоїть
     *                 у #wood_texture в ballista_base.json). Потрібен, щоб коректно
     *                 "розпакувати" атласні координати квада назад у локальні 0-16.
     */
    private void reTextureAndEmit(QuadEmitter emitter, BakedQuad quad, Sprite sprite, Sprite template) {
        // fromVanilla кладе в квад координати у форматі "точка на спільному атласі гри"
        // (крихітні дроби на кшталт 0.0273) - їх не можна напряму згодовувати у spriteBake,
        // бо той за замовчуванням очікує координати у форматі 0-16 (як у звичайній JSON моделі).
        emitter.fromVanilla(quad, DEFAULT_MATERIAL, quad.getFace());

        float u0 = template.getMinU();
        float u1 = template.getMaxU();
        float v0 = template.getMinV();
        float v1 = template.getMaxV();

        // "Розпаковуємо" атласні координати назад у 0-16 відносно ОРИГІНАЛЬНОЇ (шаблонної)
        // текстури, якою модель була спечена - тобто відновлюємо ту саму форму UV,
        // яка була прописана у самому ballista_base.json.
        for (int i = 0; i < 4; i++) {
            float atlasU = emitter.spriteU(i, 0);
            float atlasV = emitter.spriteV(i, 0);

            float localU = (atlasU - u0) / (u1 - u0) * 16f;
            float localV = (atlasV - v0) / (v1 - v0) * 16f;

            emitter.sprite(i, 0, localU, localV);
        }

        // Тепер координати знову в звичному 0-16 форматі - і spriteBake коректно
        // перемальовує ту саму форму UV на потрібний спрайт (наприклад spruce_planks).
        emitter.spriteBake(sprite, 0);
        emitter.emit();
    }

    /**
     * Дає спрайт (текстуру) блока дощок за його Identifier, беручи його
     * з реальної, вже спеченої моделі цього блока - тобто працює з будь-яким
     * ресурс-паком і з будь-яким модом, що зареєстрував дошки під своїм неймспейсом.
     */
    private static Sprite resolvePlankSprite(Identifier materialId) {
        Block block = Registries.BLOCK.get(materialId);
        if (block == Blocks.AIR) {
            // Невалідний/відсутній матеріал (наприклад пошкоджений NBT) - падаємо на дефолт.
            block = Registries.BLOCK.get(WoodMaterial.DEFAULT);
        }

        BakedModel plankModel = MinecraftClient.getInstance()
                .getBlockRenderManager()
                .getModel(block.getDefaultState());

        return plankModel.getParticleSprite();
    }

    // --- Решта методів BakedModel - просто делегуємо у вихідну модель ---

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
        return wrapped.getQuads(state, face, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return wrapped.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return wrapped.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return wrapped.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return wrapped.isBuiltin();
    }

    @Override
    public Sprite getParticleSprite() {
        return wrapped.getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return wrapped.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return wrapped.getOverrides();
    }
}
