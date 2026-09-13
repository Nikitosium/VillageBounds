package hik1tka.vb.screen;

import hik1tka.vb.item.SettlementProjectItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class DocumentFolderScreenHandler extends ScreenHandler {
    private final ItemStack folderStack;
    private final SimpleInventory inventory;

    // Конструктор для клієнта
    public DocumentFolderScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ItemStack.EMPTY);
    }

    // Основний конструктор
    public DocumentFolderScreenHandler(int syncId, PlayerInventory playerInventory, ItemStack folderStack) {
        super(ModScreenHandlers.DOCUMENT_FOLDER_SCREEN_HANDLER, syncId);
        this.folderStack = folderStack;
        this.inventory = new SimpleInventory(12);

        loadDocuments();

        // Слоти папки
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 5; col++) {
                this.addSlot(new Slot(inventory, row * 4 + col, 44 + col * 18, 20 + row * 18));
            }
        }

        // Слоти гравця
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 103 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 161));
        }
    }

    private void loadDocuments() {
        if (folderStack.hasNbt() && folderStack.getNbt().contains("SavedDocuments", 9)) {
            NbtList nbtList = folderStack.getNbt().getList("SavedDocuments", 10);
            for (int i = 0; i < nbtList.size() && i < 12; i++) {
                inventory.setStack(i, ItemStack.fromNbt(nbtList.getCompound(i)));
            }
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!player.getWorld().isClient) {
            saveDocuments();
            // ВАЖЛИВО: Оновлюємо стак, який тримає гравець,
            // щоб NBT синхронізувався з інвентарем гравця
            player.getStackInHand(player.getActiveHand()).setNbt(folderStack.getNbt());
        }
    }

    private void saveDocuments() {
        NbtCompound nbt = folderStack.getOrCreateNbt();
        NbtList nbtList = new NbtList();
        for (int i = 0; i < 12; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound itemNbt = new NbtCompound();
                stack.writeNbt(itemNbt);
                nbtList.add(itemNbt);
            }
        }
        nbt.put("SavedDocuments", nbtList);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            itemStack = originalStack.copy();
            if (slotIndex < 12) {
                if (!this.insertItem(originalStack, 12, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (originalStack.getItem() instanceof SettlementProjectItem) {
                if (!this.insertItem(originalStack, 0, 12, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (originalStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return itemStack;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        // Якщо слот знаходиться в нашій "папці" (індекси 0-11)
        if (slot.getIndex() < 12) {
            // Дозволяємо тільки якщо предмет є екземпляром вашого класу
            return stack.getItem() instanceof SettlementProjectItem;
        }
        return super.canInsertIntoSlot(stack, slot);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}