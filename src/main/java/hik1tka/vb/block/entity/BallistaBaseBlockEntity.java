package hik1tka.vb.block.entity;

import hik1tka.vb.block.WoodMaterial;
import hik1tka.vb.registry.ModBlockEntities;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Зберігає деревний матеріал балісти (Identifier блока дощок) та відповідає за
 * персистенцію (NBT) і синхронізацію сервер -> клієнт.
 * <p>
 * Сама текстура тут НЕ зберігається і НЕ вираховується - лише "сирий" Identifier
 * матеріалу. Перетворення Identifier -> Sprite відбувається на клієнті під час
 * рендеру (окремий крок, ще не реалізований у цьому файлі).
 * <p>
 * Реалізує {@link RenderAttachmentBlockEntity}: під час чанк-бейкінгу (фоновий потік!)
 * НЕ можна напряму викликати {@code blockView.getBlockEntity(pos)} - Fabric Rendering API
 * прямо попереджає, що такий лукап не потокобезпечний і може повертати null/сміття.
 * Замість цього рушій сам, у головному потоці, один раз бере {@link #getRenderAttachmentData()}
 * і кладе цей знімок у {@code RenderAttachedBlockView}, звідки його вже безпечно
 * читати з {@code emitBlockQuads}.
 */
public class BallistaBaseBlockEntity extends BlockEntity implements RenderAttachmentBlockEntity {

    /**
     * Клієнтський "гачок" примусового ре-рендеру конкретної позиції. За замовчуванням
     * no-op (безпечно на сервері, де він ніколи не викликається і НЕ підв'язується).
     * Реальну реалізацію (яка вже лізе у MinecraftClient) підключає ВИКЛЮЧНО клієнтський
     * код - {@code VillageBoundsClient#onInitializeClient()} - щоб common-клас
     * (цей файл, src/main/java) не тримав прямого посилання на клієнтські класи
     * і не падав на dedicated-сервері.
     * <p>
     * Навіщо: без цього хука перемальовка блока в світі залежить від того, чи встигне
     * "випадковий" ре-рендер чанку (напр. від сусіднього оновлення блока) статись ПІСЛЯ
     * того, як дані матеріалу вже дійшли до клієнта. Якщо ні - блок лишається з дефолтним
     * дубом, доки щось інше не змусить чанк перебейкатись (звідси й ефект "по черзі":
     * ставиш другу основу - той-таки випадковий ре-рендер від ЇЇ появи заднім числом
     * підхоплює вже готові дані ПЕРШОЇ, а сама друга ще не встигла засинхронитись).
     * Тепер замість "може пощастить" - ре-рендер конкретної позиції запускається
     * гарантовано ПІСЛЯ {@link #readNbt(NbtCompound)}, тобто одразу як дані стали вірні.
     */
    public static Consumer<BlockPos> CLIENT_RERENDER_HOOK = pos -> {};

    private Identifier material = WoodMaterial.DEFAULT;

    public BallistaBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BALLISTA_BASE, pos, state);
    }

    public Identifier getMaterial() {
        return material;
    }

    /** Викликається логікою блока (place / debug-команди тощо). */
    public void setMaterial(Identifier material) {
        this.material = material == null ? WoodMaterial.DEFAULT : material;
        markDirty();

        // markDirty() сам по собі лише позначає BE для збереження на диск.
        // Щоб клієнт негайно перемалював блок з новою текстурою, потрібно
        // явно розіслати оновлення (тільки з серверної сторони).
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Material", material.toString());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("Material")) {
            Identifier parsed = Identifier.tryParse(nbt.getString("Material"));
            this.material = parsed != null ? parsed : WoodMaterial.DEFAULT;
        } else {
            this.material = WoodMaterial.DEFAULT;
        }

        // Саме тут (а не десь "навмання" пізніше) дані матеріалу вже 100% вірні -
        // тому саме тут і просимо клієнт перебейкати геометрію цієї позиції.
        // На сервері world.isClient == false, тому хук тут ніколи не викликається.
        if (world != null && world.isClient) {
            CLIENT_RERENDER_HOOK.accept(pos);
        }
    }

    // --- Синхронізація сервер -> клієнт ---

    @Nullable
    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        // Повна NBT одразу при завантаженні чанку клієнтом,
        // інакше клієнт бачитиме DEFAULT матеріал до першого markDirty().
        return createNbt();
    }

    // --- RenderAttachmentBlockEntity: потокобезпечний "знімок" для рендер-потоку ---

    @Override
    public Identifier getRenderAttachmentData() {
        // Викликається рушієм у ГОЛОВНОМУ потоці перед чанк-бейкінгом і кладеться
        // у RenderAttachedBlockView. Identifier - immutable, тому передавати його
        // як є у фоновий потік безпечно.
        return material;
    }
}
