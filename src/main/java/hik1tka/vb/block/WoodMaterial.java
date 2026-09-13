package hik1tka.vb.block;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;

/**
 * Матеріал балісти зберігається НЕ як enum, а як {@link Identifier} блока дощок
 * (наприклад "minecraft:oak_planks" або "somemod:custom_planks").
 * <p>
 * Завдяки цьому підтримується будь-яка деревина будь-якого мода: єдина умова -
 * блок повинен входити у ванільний тег {@link BlockTags#PLANKS}, до якого практично
 * всі моди додають свої дошки.
 * <p>
 * ВАЖЛИВО: у 1.20.1 ще немає Data Components (вони з'явились у 1.20.5), тому
 * матеріал пишеться у звичайний NBT стеку, а не у ComponentType.
 */
public final class WoodMaterial {

    /** Матеріал за замовчуванням, якщо стек нічого не задає (наприклад видано через /give). */
    public static final Identifier DEFAULT = new Identifier("minecraft", "oak_planks");

    private static final String NBT_KEY = "Material";

    private WoodMaterial() {
    }

    /** Зчитує матеріал зі стеку. Якщо NBT відсутній або невалідний - повертає {@link #DEFAULT}. */
    public static Identifier readFromStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasNbt() || !stack.getNbt().contains(NBT_KEY)) {
            return DEFAULT;
        }
        Identifier parsed = Identifier.tryParse(stack.getNbt().getString(NBT_KEY));
        return parsed != null ? parsed : DEFAULT;
    }

    /** Записує матеріал у NBT стеку. */
    public static void writeToStack(ItemStack stack, Identifier material) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(NBT_KEY, (material != null ? material : DEFAULT).toString());
    }

    /** Створює новий ItemStack переданого предмета з прописаним матеріалом (для дропу / pick block). */
    public static ItemStack createStack(Item item, Identifier material) {
        ItemStack stack = new ItemStack(item);
        writeToStack(stack, material);
        return stack;
    }

    /**
     * Перевіряє, що переданий блок дійсно є "дошками" - у тому числі модовими,
     * якщо вони зареєстровані у тезі #minecraft:planks.
     */
    public static boolean isPlank(Block block) {
        return Registries.BLOCK.getEntry(block).isIn(BlockTags.PLANKS);
    }
}
